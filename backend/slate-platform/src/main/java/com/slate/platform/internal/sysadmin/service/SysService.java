// 域/模块: 平台底座/系统管理
// 类型: 服务
// 职责: 字典（Redis 缓存 dict:{type} 1h，变更失效，故障降级直查）与参数配置（CAMPUS→GLOBAL 回退解析）；日志查询（登录/操作，只读，缺省时间窗）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.sysadmin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.error.BusinessException;
import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.error.SysErrorCode;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.internal.audit.entity.OperationLog;
import com.slate.platform.internal.audit.mapper.OperationLogMapper;
import com.slate.platform.internal.auth.entity.LoginLog;
import com.slate.platform.internal.auth.mapper.LoginLogMapper;
import com.slate.platform.internal.sysadmin.entity.DictItem;
import com.slate.platform.internal.sysadmin.entity.DictType;
import com.slate.platform.internal.sysadmin.entity.SysConfig;
import com.slate.platform.internal.sysadmin.mapper.DictItemMapper;
import com.slate.platform.internal.sysadmin.mapper.DictTypeMapper;
import com.slate.platform.internal.sysadmin.mapper.SysConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysService {

    private static final Logger log = LoggerFactory.getLogger(SysService.class);
    private static final Duration DICT_TTL = Duration.ofHours(1);
    private static final String DICT_KEY_PREFIX = "dict:";
    private static final String SCOPE_GLOBAL = "GLOBAL";
    private static final String SCOPE_CAMPUS = "CAMPUS";
    /** 日志查询缺省时间窗（天）：from/to 均缺省时只查最近窗口，防全表深扫 */
    private static final int LOG_DEFAULT_WINDOW_DAYS = 30;

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;
    private final SysConfigMapper configMapper;
    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SysService(DictTypeMapper dictTypeMapper,
                      DictItemMapper dictItemMapper,
                      SysConfigMapper configMapper,
                      LoginLogMapper loginLogMapper,
                      OperationLogMapper operationLogMapper,
                      SnowflakeIdGenerator idGenerator,
                      StringRedisTemplate redis,
                      ObjectMapper objectMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
        this.configMapper = configMapper;
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.idGenerator = idGenerator;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    // ── 字典（公开读 + 缓存）──

    public record DictItemView(String value, String label, Integer sort) {
    }

    /** 字典项查询（登录即可，全部登录用户消费；缓存 dict:{type} 1h——detail/data.md §4；Redis 故障降级直查 DB） */
    public List<DictItemView> dictItems(String typeCode) {
        String cacheKey = DICT_KEY_PREFIX + typeCode;
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<DictItemView>>() {
                });
            }
        } catch (Exception e) {
            log.warn("字典缓存读取失败，降级直查 DB: type={}, error={}", typeCode, e.getMessage());
        }
        List<DictItemView> items = dictItemMapper.selectList(new LambdaQueryWrapper<DictItem>()
                        .eq(DictItem::getTypeCode, typeCode)
                        .orderByAsc(DictItem::getSort))
                .stream().map(i -> new DictItemView(i.getValue(), i.getLabel(), i.getSort()))
                .toList();
        try {
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(items), DICT_TTL);
        } catch (Exception e) {
            log.warn("字典缓存写入失败（降级运行）: type={}, error={}", typeCode, e.getMessage());
        }
        return items;
    }

    public List<DictType> dictTypes() {
        return dictTypeMapper.selectList(null);
    }

    public DictType createDictType(String code, String name, String remark) {
        DictType exists = dictTypeMapper.selectOne(new LambdaQueryWrapper<DictType>()
                .eq(DictType::getCode, code));
        if (exists != null) {
            throw new BusinessException(SysErrorCode.SYS_003, "字典类型已存在: " + code);
        }
        DictType type = new DictType();
        type.setId(idGenerator.nextId());
        type.setCode(code);
        type.setName(name);
        type.setRemark(remark);
        try {
            dictTypeMapper.insert(type);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(SysErrorCode.SYS_003, "字典类型已存在: " + code);
        }
        return type;
    }

    /** 字典项新增/更新：变更即失效缓存（失效失败仅告警，靠 TTL 1h 兜底收敛） */
    public void upsertDictItem(String typeCode, String value, String label, Integer sort) {
        if (dictTypeMapper.selectOne(new LambdaQueryWrapper<DictType>()
                .eq(DictType::getCode, typeCode)) == null) {
            throw new BusinessException(SysErrorCode.SYS_003);
        }
        DictItem item = dictItemMapper.selectOne(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getTypeCode, typeCode).eq(DictItem::getValue, value));
        if (item == null) {
            item = new DictItem();
            item.setId(idGenerator.nextId());
            item.setTypeCode(typeCode);
            item.setValue(value);
        }
        item.setLabel(label);
        item.setSort(sort == null ? 0 : sort);
        try {
            if (item.getCreatedAt() == null) {
                dictItemMapper.insert(item);
            } else {
                dictItemMapper.updateById(item);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(SysErrorCode.SYS_003, "字典项已存在: " + value);
        }
        evictDict(typeCode);
    }

    /** 删除即物理删除（变更历史由 @Audited 操作审计留痕） */
    public void deleteDictItem(Long id) {
        DictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(SysErrorCode.SYS_006);
        }
        dictItemMapper.deleteById(id);
        evictDict(item.getTypeCode());
    }

    private void evictDict(String typeCode) {
        try {
            redis.delete(DICT_KEY_PREFIX + typeCode);
        } catch (Exception e) {
            log.warn("字典缓存失效失败（靠 TTL 兜底）: type={}, error={}", typeCode, e.getMessage());
        }
    }

    // ── 参数配置 ──

    /** 参数读取：作用域回退解析（CAMPUS 未命中回落 GLOBAL）；两级均未命中抛 SYS-004。
     *  敏感参数不入本表——运维密钥走环境变量 */
    public String configValue(String key, String scope) {
        String requestedScope = scope == null || scope.isBlank() ? SCOPE_GLOBAL : scope;
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCfgKey, key)
                .eq(SysConfig::getScope, requestedScope));
        if (config == null && !SCOPE_GLOBAL.equals(requestedScope)) {
            config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getCfgKey, key)
                    .eq(SysConfig::getScope, SCOPE_GLOBAL));
        }
        if (config == null) {
            throw new BusinessException(SysErrorCode.SYS_004, "参数不存在: " + key + "@" + requestedScope);
        }
        return config.getCfgValue();
    }

    public List<SysConfig> configs() {
        return configMapper.selectList(null);
    }

    public void upsertConfig(String key, String value, String scope, String remark) {
        String normalizedScope = scope == null || scope.isBlank() ? SCOPE_GLOBAL : scope;
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCfgKey, key).eq(SysConfig::getScope, normalizedScope));
        if (config == null) {
            config = new SysConfig();
            config.setId(idGenerator.nextId());
            config.setCfgKey(key);
            config.setScope(normalizedScope);
        }
        config.setCfgValue(value);
        config.setRemark(remark);
        try {
            if (config.getCreatedAt() == null) {
                configMapper.insert(config);
            } else {
                configMapper.updateById(config);
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(SysErrorCode.SYS_004, "参数已存在: " + key + "@" + normalizedScope);
        }
    }

    public void deleteConfig(Long id) {
        configMapper.deleteById(id);
    }

    // ── 日志查询（只读）──

    /** from/to 均缺省时按最近 30 天窗口查询（时间索引 idx_ll_created 兜底），防无界全表扫描 */
    public PageResult<LoginLog> loginLogs(String username, LocalDateTime from, LocalDateTime to, PageQuery query) {
        LocalDateTime[] window = defaultWindow(from, to);
        Page<LoginLog> result = loginLogMapper.selectPage(
                new Page<>(query.getPage(), query.limitedSize()),
                new LambdaQueryWrapper<LoginLog>()
                        .eq(username != null && !username.isBlank(), LoginLog::getUsername, username)
                        .ge(window[0] != null, LoginLog::getCreatedAt, window[0])
                        .le(window[1] != null, LoginLog::getCreatedAt, window[1])
                        .orderByDesc(LoginLog::getId));
        return new PageResult<>(result.getRecords(), result.getTotal(),
                query.getPage(), query.limitedSize());
    }

    public PageResult<OperationLog> operationLogs(Long accountId, String traceId,
                                                  LocalDateTime from, LocalDateTime to, PageQuery query) {
        LocalDateTime[] window = defaultWindow(from, to);
        Page<OperationLog> result = operationLogMapper.selectPage(
                new Page<>(query.getPage(), query.limitedSize()),
                new LambdaQueryWrapper<OperationLog>()
                        .eq(accountId != null, OperationLog::getAccountId, accountId)
                        .eq(traceId != null && !traceId.isBlank(), OperationLog::getTraceId, traceId)
                        .ge(window[0] != null, OperationLog::getCreatedAt, window[0])
                        .le(window[1] != null, OperationLog::getCreatedAt, window[1])
                        .orderByDesc(OperationLog::getId));
        return new PageResult<>(result.getRecords(), result.getTotal(),
                query.getPage(), query.limitedSize());
    }

    private LocalDateTime[] defaultWindow(LocalDateTime from, LocalDateTime to) {
        if (from == null && to == null) {
            to = LocalDateTime.now();
            from = to.minusDays(LOG_DEFAULT_WINDOW_DAYS);
        }
        return new LocalDateTime[]{from, to};
    }
}
