// 域/模块: 平台底座/认证授权
// 类型: 服务
// 职责: 登录/刷新/登出/改密/画像——失败锁定（Redis 5次/15min）、登录日志、refresh 滚动与撤销
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.common.error.BusinessException;
import com.slate.platform.api.auth.AuthMeResponse;
import com.slate.platform.api.auth.TokenPair;
import com.slate.platform.internal.auth.entity.Account;
import com.slate.platform.internal.auth.entity.LoginLog;
import com.slate.platform.internal.auth.entity.UserProfile;
import com.slate.platform.internal.auth.error.AuthErrorCode;
import com.slate.platform.internal.auth.mapper.AccountMapper;
import com.slate.platform.internal.auth.mapper.LoginLogMapper;
import com.slate.platform.internal.auth.mapper.UserProfileMapper;
import com.slate.framework.id.SnowflakeIdGenerator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final String FAIL_KEY_PREFIX = "login:fail:";
    private static final int MAX_FAIL = 5;
    private static final Duration LOCK_TTL = Duration.ofMinutes(15);

    private final AccountMapper accountMapper;
    private final UserProfileMapper profileMapper;
    private final LoginLogMapper loginLogMapper;
    private final TokenService tokenService;
    private final RbacService rbacService;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;
    private final StringRedisTemplate redis;

    public AuthService(AccountMapper accountMapper,
                       UserProfileMapper profileMapper,
                       LoginLogMapper loginLogMapper,
                       TokenService tokenService,
                       RbacService rbacService,
                       PasswordEncoder passwordEncoder,
                       SnowflakeIdGenerator idGenerator,
                       StringRedisTemplate redis) {
        this.accountMapper = accountMapper;
        this.profileMapper = profileMapper;
        this.loginLogMapper = loginLogMapper;
        this.tokenService = tokenService;
        this.rbacService = rbacService;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.redis = redis;
    }

    /** 登录：锁定检查 → 凭证校验（错误统一 AUTH-001，不暴露差异）→ 双 token 签发 */
    public TokenPair login(String username, String password, String ip, String userAgent) {
        String failKey = FAIL_KEY_PREFIX + username;
        String fails = redis.opsForValue().get(failKey);
        if (fails != null && Integer.parseInt(fails) >= MAX_FAIL) {
            log(username, null, false, "LOCKED", ip, userAgent);
            throw new BusinessException(AuthErrorCode.AUTH_002);
        }
        Account account = accountMapper.selectOne(
                new LambdaQueryWrapper<Account>().eq(Account::getUsername, username));
        if (account == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            countFail(failKey);
            log(username, account == null ? null : account.getId(), false, "BAD_CREDENTIALS", ip, userAgent);
            throw new BusinessException(AuthErrorCode.AUTH_001);
        }
        if (!"active".equals(account.getStatus())) {
            log(username, account.getId(), false, "DISABLED", ip, userAgent);
            throw new BusinessException(AuthErrorCode.AUTH_002);
        }
        redis.delete(failKey);
        account.setLastLoginAt(LocalDateTime.now());
        accountMapper.updateById(account);
        log(username, account.getId(), true, null, ip, userAgent);
        RbacService.AccountGrants grants = rbacService.loadGrants(account.getId());
        return tokenService.issue(account.getId(), account.getUserId(), account.getUsername(), grants.roleCodes());
    }

    /** 刷新：白名单校验 + 旋转；重查最新角色（token 里角色可能已过期） */
    public TokenPair refresh(String refreshToken) {
        Long accountId = tokenService.consumeRefresh(refreshToken);
        if (accountId == null) {
            throw new BusinessException(AuthErrorCode.AUTH_005);
        }
        Account account = accountMapper.selectById(accountId);
        if (account == null || !"active".equals(account.getStatus())) {
            tokenService.revokeAll(accountId);
            throw new BusinessException(AuthErrorCode.AUTH_005);
        }
        RbacService.AccountGrants grants = rbacService.loadGrants(accountId);
        return tokenService.issue(account.getId(), account.getUserId(), account.getUsername(), grants.roleCodes());
    }

    /** 登出：撤销当前 refresh（白名单删除） */
    public void logout(Long accountId, String refreshToken) {
        tokenService.revoke(accountId, refreshToken);
    }

    /** 修改密码：验旧设新，撤销全部 refresh（全部端下线） */
    public void changePassword(Long accountId, String oldPassword, String newPassword) {
        Account account = requireAccount(accountId);
        if (!passwordEncoder.matches(oldPassword, account.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.AUTH_001);
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountMapper.updateById(account);
        tokenService.revokeAll(accountId);
    }

    /** 当前账号画像（角色/权限码供前端渲染菜单与按钮） */
    public AuthMeResponse me(Long accountId) {
        Account account = requireAccount(accountId);
        UserProfile profile = profileMapper.selectById(account.getUserId());
        RbacService.AccountGrants grants = rbacService.loadGrants(accountId);
        return new AuthMeResponse(
                account.getId(),
                account.getUsername(),
                profile == null ? null : profile.getRealName(),
                grants.roleCodes(),
                grants.permissionCodes(),
                null);   // orgId 随组织架构模块接入后回填
    }

    private Account requireAccount(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(AuthErrorCode.AUTH_004);
        }
        return account;
    }

    private void countFail(String failKey) {
        Long count = redis.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redis.expire(failKey, LOCK_TTL);
        }
    }

    private void log(String username, Long accountId, boolean success, String failReason, String ip, String userAgent) {
        LoginLog entry = new LoginLog();
        entry.setId(idGenerator.nextId());
        entry.setUsername(username);
        entry.setAccountId(accountId);
        entry.setSuccess(success ? 1 : 0);
        entry.setFailReason(failReason);
        entry.setIp(ip);
        entry.setUserAgent(userAgent == null || userAgent.length() > 255 ? null : userAgent);
        loginLogMapper.insert(entry);
    }
}
