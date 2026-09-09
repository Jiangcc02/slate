// 域/模块: 平台底座/系统管理
// 类型: 服务
// 职责: 字典（Redis 缓存 dict:{type} 1h，变更失效）与参数配置；日志查询（登录/操作，只读）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.sysadmin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysService {

    private static final Duration DICT_TTL = Duration.ofHours(1);
    private static final String DICT_KEY_PREFIX = "dict:";

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;
    private final SysConfigMapper configMapper;
    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final StringRedisTemplate redis;

    public SysService(DictTypeMapper dictTypeMapper,
                      DictItemMapper dictItemMapper,
                      SysConfigMapper configMapper,
                      LoginLogMapper loginLogMapper,
                      OperationLogMapper operationLogMapper,
                      SnowflakeIdGenerator idGenerator,
                      StringRedisTemplate redis) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
        this.configMapper = configMapper;
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.idGenerator = idGenerator;
        this.redis = redis;
    }

    // ── 字典（公开读 + 缓存）──

    public record DictItemView(String value, String label, Integer sort) {
    }

    /** 字典项查询（登录即可，全部登录用户消费；缓存 dict:{type} 1h——detail/data.md §4） */
    public List<DictItemView> dictItems(String typeCode) {
        return dictItemMapper.selectList(new LambdaQueryWrapper<DictItem>()
                        .eq(DictItem::getTypeCode, typeCode)
                        .orderByAsc(DictItem::getSort))
                .stream().map(i -> new DictItemView(i.getValue(), i.getLabel(), i.getSort()))
                .toList();
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
        dictTypeMapper.insert(type);
        return type;
    }

    /** 字典项新增/更新：变更即失效缓存 */
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
        if (item.getCreatedAt() == null) {
            dictItemMapper.insert(item);
        } else {
            dictItemMapper.updateById(item);
        }
        evictDict(typeCode);
    }

    public void deleteDictItem(Long id) {
        DictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(SysErrorCode.SYS_003);
        }
        dictItemMapper.deleteById(id);
        evictDict(item.getTypeCode());
    }

    private void evictDict(String typeCode) {
        redis.delete(DICT_KEY_PREFIX + typeCode);
    }

    // ── 参数配置 ──

    /** 参数读取（登录即可；敏感参数不入本表——运维密钥走环境变量） */
    public String configValue(String key, String scope) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getCfgKey, key)
                .eq(SysConfig::getScope, scope == null || scope.isBlank() ? "GLOBAL" : scope));
        return config == null ? null : config.getCfgValue();
    }

    public List<SysConfig> configs() {
        return configMapper.selectList(null);
    }

    public void upsertConfig(String key, String value, String scope, String remark) {
        String normalizedScope = scope == null || scope.isBlank() ? "GLOBAL" : scope;
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
        if (config.getCreatedAt() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
    }

    public void deleteConfig(Long id) {
        configMapper.deleteById(id);
    }

    // ── 日志查询（只读）──

    public PageResult<LoginLog> loginLogs(String username, LocalDateTime from, LocalDateTime to, PageQuery query) {
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<LoginLog>(
                query.getPage(), query.limitedSize());
        var result = loginLogMapper.selectPage(page, new LambdaQueryWrapper<LoginLog>()
                .eq(username != null && !username.isBlank(), LoginLog::getUsername, username)
                .ge(from != null, LoginLog::getCreatedAt, from)
                .le(to != null, LoginLog::getCreatedAt, to)
                .orderByDesc(LoginLog::getId));
        return new PageResult<>(result.getRecords(), result.getTotal(),
                query.getPage(), query.limitedSize());
    }

    public PageResult<OperationLog> operationLogs(Long accountId, String traceId,
                                                  LocalDateTime from, LocalDateTime to, PageQuery query) {
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<OperationLog>(
                query.getPage(), query.limitedSize());
        var result = operationLogMapper.selectPage(page, new LambdaQueryWrapper<OperationLog>()
                .eq(accountId != null, OperationLog::getAccountId, accountId)
                .eq(traceId != null && !traceId.isBlank(), OperationLog::getTraceId, traceId)
                .ge(from != null, OperationLog::getCreatedAt, from)
                .le(to != null, OperationLog::getCreatedAt, to)
                .orderByDesc(OperationLog::getId));
        return new PageResult<>(result.getRecords(), result.getTotal(),
                query.getPage(), query.limitedSize());
    }
}
