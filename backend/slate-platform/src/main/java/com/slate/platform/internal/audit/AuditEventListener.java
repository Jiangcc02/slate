// 域/模块: 平台底座/系统管理
// 类型: 审计落库监听
// 职责: 监听 framework 审计事件 → 落 operation_log（只追加；落库失败不阻断业务）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.audit;

import com.slate.framework.audit.OperationAuditEvent;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.internal.audit.entity.OperationLog;
import com.slate.platform.internal.audit.mapper.OperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final OperationLogMapper operationLogMapper;
    private final SnowflakeIdGenerator idGenerator;

    public AuditEventListener(OperationLogMapper operationLogMapper, SnowflakeIdGenerator idGenerator) {
        this.operationLogMapper = operationLogMapper;
        this.idGenerator = idGenerator;
    }

    @EventListener
    public void onAudit(OperationAuditEvent event) {
        try {
            OperationLog entry = new OperationLog();
            entry.setId(idGenerator.nextId());
            entry.setAccountId(event.accountId());
            entry.setOnBehalfOf(event.onBehalfOf());
            entry.setServiceId(event.serviceId());
            entry.setAction(event.action());
            entry.setTarget(event.target());
            entry.setParamsDigest(event.paramsDigest());
            entry.setResult(event.success() ? "ok" : "fail");
            entry.setTraceId(event.traceId());
            entry.setIp(event.ip());
            operationLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("审计日志落库失败（不阻断业务）: action={}, error={}", event.action(), e.getMessage());
        }
    }
}
