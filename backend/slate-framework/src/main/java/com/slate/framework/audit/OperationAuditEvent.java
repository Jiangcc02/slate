// 域/模块: 平台底座/工程规范
// 类型: 审计事件
// 职责: 审计记录载体——由切面发布、由落库实现监听（framework 不依赖存储，方向正确）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.audit;

/** 审计事件：accountId 为主操作者；serviceId/onBehalfOf 为双身份调用（api-conventions §2） */
public record OperationAuditEvent(
        Long accountId,
        Long onBehalfOf,
        String serviceId,
        String action,
        String target,
        String paramsDigest,
        boolean success,
        String traceId,
        String ip) {
}
