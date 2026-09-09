// 域/模块: 平台底座/工程规范（对外契约）
// 类型: 契约接口
// 职责: 领域事件发布——业务事务内落 outbox（Agent-ready：可观测，铁律 L8）；业务域经本接口外发事件
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.events;

/**
 * 领域事件发布（outbox 模式）：须在业务 @Transactional 内调用——事件行与业务变更同事务提交，
 * 独立投递器轮询外发（失败退避重试，超限死信）；事件契约见 agent-boundary.md。
 */
public interface DomainEventPublisher {

    /**
     * @param eventType      事件类型（如 org.class.member-changed）
     * @param aggregateType  聚合类型（如 org.class）
     * @param aggregateId    聚合 ID
     * @param payload        事件负载（JSON 序列化存储）
     */
    void publish(String eventType, String aggregateType, long aggregateId, Object payload);
}
