// 域/模块: 平台底座/工程规范
// 类型: outbox 发布实现
// 职责: 领域事件落 outbox 表——须在业务事务内调用（同事务提交，杜绝双写不一致）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.framework.id.SnowflakeIdGenerator;
import com.slate.platform.api.events.DomainEventPublisher;
import com.slate.platform.internal.events.entity.DomainEvent;
import com.slate.platform.internal.events.mapper.DomainEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final DomainEventMapper domainEventMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(DomainEventMapper domainEventMapper,
                                SnowflakeIdGenerator idGenerator,
                                ObjectMapper objectMapper) {
        this.domainEventMapper = domainEventMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String eventType, String aggregateType, long aggregateId, Object payload) {
        DomainEvent event = new DomainEvent();
        event.setId(idGenerator.nextId());
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayload(toJson(payload));
        event.setStatus(DomainEvent.PENDING);
        event.setRetryCount(0);
        domainEventMapper.insert(event);
        log.info("领域事件已入 outbox: type={}, aggregate={}/{}, eventId={}",
                eventType, aggregateType, aggregateId, event.getId());
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }
}
