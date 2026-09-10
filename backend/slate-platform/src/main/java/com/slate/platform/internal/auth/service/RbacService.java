// 域/模块: 平台底座/认证授权
// 类型: 服务
// 职责: RBAC 查询与管理——账号角色码/权限码（Redis perm:{accountId} 30min，授权变更失效）、权限树、角色 CRUD 与授权
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.common.error.BusinessException;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.framework.redis.RedisKeys;
import com.slate.platform.api.auth.PermissionNode;
import com.slate.platform.api.auth.RoleDto;
import com.slate.platform.internal.auth.entity.Account;
import com.slate.platform.internal.auth.entity.AccountRole;
import com.slate.platform.internal.auth.entity.Permission;
import com.slate.platform.internal.auth.entity.Role;
import com.slate.platform.internal.auth.entity.RolePermission;
import com.slate.platform.internal.auth.error.AuthErrorCode;
import com.slate.platform.internal.auth.mapper.AccountMapper;
import com.slate.platform.internal.auth.mapper.AccountRoleMapper;
import com.slate.platform.internal.auth.mapper.PermissionMapper;
import com.slate.platform.internal.auth.mapper.RoleMapper;
import com.slate.platform.internal.auth.mapper.RolePermissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RbacService {

    private static final Logger log = LoggerFactory.getLogger(RbacService.class);
    private static final String PERM_KEY_PREFIX = "perm:";
    private static final Duration PERM_TTL = Duration.ofMinutes(30);

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AccountRoleMapper accountRoleMapper;
    private final AccountMapper accountMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final StringRedisTemplate redis;

    public RbacService(RoleMapper roleMapper,
                       PermissionMapper permissionMapper,
                       RolePermissionMapper rolePermissionMapper,
                       AccountRoleMapper accountRoleMapper,
                       AccountMapper accountMapper,
                       SnowflakeIdGenerator idGenerator,
                       StringRedisTemplate redis) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.accountRoleMapper = accountRoleMapper;
        this.accountMapper = accountMapper;
        this.idGenerator = idGenerator;
        this.redis = redis;
    }

    /** @param accountStatus 账号状态随权限缓存下发——禁用/锁定账号在缓存 TTL 内即时拒绝，不再依赖 access token 自然过期 */
    public record AccountGrants(List<String> roleCodes, List<String> permissionCodes, String accountStatus) {
    }

    /**
     * 账号的角色码 + 权限码 + 账号状态（缓存 perm:{accountId}，见 detail/data.md §4；
     * 键值格式 roles|perms|status 逗号分隔，旧格式（无 status）按 active 容错解析，TTL 内自愈）。
     * Redis 故障时降级直查 DB（性能降级、功能不降级）。
     */
    public AccountGrants loadGrants(Long accountId) {
        try {
            String cached = redis.opsForValue().get(PERM_KEY_PREFIX + accountId);
            if (cached != null) {
                String[] parts = cached.split("\\|", -1);
                return new AccountGrants(splitList(parts[0]), splitList(parts[1]),
                        parts.length > 2 ? parts[2] : "active");
            }
        } catch (Exception e) {
            log.warn("权限缓存读取失败，降级直查 DB: accountId={}, error={}", accountId, e.getMessage());
        }
        List<Long> roleIds = accountRoleMapper.selectList(
                        new LambdaQueryWrapper<AccountRole>().eq(AccountRole::getAccountId, accountId))
                .stream().map(AccountRole::getRoleId).toList();
        List<String> roleCodes = roleIds.isEmpty() ? List.of()
                : roleMapper.selectBatchIds(roleIds).stream().map(Role::getCode).toList();
        List<String> permissionCodes = roleIds.isEmpty() ? List.of()
                : rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds))
                .stream().map(RolePermission::getPermissionId).distinct()
                .map(pid -> {
                    Permission p = permissionMapper.selectById(pid);
                    return p == null ? null : p.getCode();
                })
                .filter(code -> code != null)
                .toList();
        Account account = accountMapper.selectById(accountId);
        String status = account == null ? "disabled" : account.getStatus();
        AccountGrants grants = new AccountGrants(roleCodes, permissionCodes, status);
        try {
            redis.opsForValue().set(
                    PERM_KEY_PREFIX + accountId,
                    String.join(",", roleCodes) + "|" + String.join(",", permissionCodes) + "|" + status,
                    PERM_TTL);
        } catch (Exception e) {
            log.warn("权限缓存写入失败（降级运行，靠 TTL 兜底）: accountId={}, error={}", accountId, e.getMessage());
        }
        return grants;
    }

    private List<String> splitList(String joined) {
        return joined.isBlank() ? List.of() : List.of(joined.split(","));
    }

    /** 授权/角色变更后失效全部权限缓存（幂等，可重复调用；SCAN 替代 KEYS） */
    public void evictAllGrants() {
        List<String> keys = RedisKeys.scan(redis, PERM_KEY_PREFIX + "*", 10_000);
        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    /** 权限树（menu/button 两级） */
    public List<PermissionNode> permissionTree() {
        List<Permission> all = permissionMapper.selectList(null);
        all.sort(Comparator.comparing(p -> p.getSort() == null ? 0 : p.getSort()));
        Map<Long, PermissionNode> nodes = new LinkedHashMap<>();
        for (Permission p : all) {
            nodes.put(p.getId(), new PermissionNode(p.getId(), p.getCode(), p.getName(), p.getType(), p.getSort(), new ArrayList<>()));
        }
        List<PermissionNode> roots = new ArrayList<>();
        for (Permission p : all) {
            PermissionNode node = nodes.get(p.getId());
            PermissionNode parent = p.getParentId() == null ? null : nodes.get(p.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    public List<RoleDto> listRoles() {
        return roleMapper.selectList(null).stream()
                .map(r -> new RoleDto(r.getId(), r.getCode(), r.getName(), r.getType(), r.getRemark()))
                .toList();
    }

    @Transactional
    public RoleDto createRole(RoleDto.CreateRequest request) {
        Role exists = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, request.code()));
        if (exists != null) {
            throw new BusinessException(AuthErrorCode.AUTH_006, "角色码已存在: " + request.code());
        }
        Role role = new Role();
        role.setId(idGenerator.nextId());
        role.setCode(request.code());
        role.setName(request.name());
        role.setRemark(request.remark());
        role.setType(Role.TYPE_CUSTOM);
        roleMapper.insert(role);
        return new RoleDto(role.getId(), role.getCode(), role.getName(), role.getType(), role.getRemark());
    }

    @Transactional
    public void updateRole(Long id, RoleDto.UpdateRequest request) {
        Role role = requireCustomRole(id);
        role.setName(request.name());
        role.setRemark(request.remark());
        roleMapper.updateById(role);
        evictAllGrants();
    }

    @Transactional
    public void deleteRole(Long id) {
        requireCustomRole(id);
        roleMapper.deleteById(id);
        evictAllGrants();
    }

    @Transactional
    public void grantPermissions(Long roleId, List<Long> permissionIds) {
        requireCustomRole(roleId);
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        for (Long pid : permissionIds) {
            if (permissionMapper.selectById(pid) == null) {
                throw new BusinessException(AuthErrorCode.AUTH_003, "权限不存在: " + pid);
            }
            RolePermission rp = new RolePermission();
            rp.setId(idGenerator.nextId());
            rp.setRoleId(roleId);
            rp.setPermissionId(pid);
            rolePermissionMapper.insert(rp);
        }
        evictAllGrants();
    }

    private Role requireCustomRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(AuthErrorCode.AUTH_006, "角色不存在");
        }
        if (Role.TYPE_PRESET.equals(role.getType())) {
            throw new BusinessException(AuthErrorCode.AUTH_006);
        }
        return role;
    }
}
