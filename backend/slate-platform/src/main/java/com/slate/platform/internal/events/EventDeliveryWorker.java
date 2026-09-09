// 域/模块: 平台底座/工程规范
// 类型: 事件投递器
// 职责: outbox 轮询外发——PENDING 事件 POST 到配置回调（失败 2^n 分钟退避，≥5 次死信）；未配置回调时只落表不投递
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.events;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.platform.internal.events.entity.DomainEvent;
import com.slate.platform.internal.events.mapper.DomainEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class EventDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(EventDeliveryWorker.class);
    private static final int MAX_RETRY = 5;
    private static final int BATCH = 50;

    private final DomainEventMapper domainEventMapper;
    private final RestClient restClient;
    private final String webhookUrl;

    public EventDeliveryWorker(DomainEventMapper domainEventMapper,
                               @Value("${slate.events.webhook-url:}") String webhookUrl,
                               RestClient.Builder restClientBuilder) {
        this.domainEventMapper = domainEventMapper;
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.restClient = restClientBuilder.build();
    }

    @Scheduled(fixedDelay = 5000)
    public void deliver() {
        if (webhookUrl.isEmpty()) {
            return;   // 未配置投递目标（Agent 运行时未建）：事件保留 PENDING，机制就绪可观测
        }
        List<DomainEvent> pending = domainEventMapper.selectList(
                new LambdaQueryWrapper<DomainEvent>()
                        .eq(DomainEvent::getStatus, DomainEvent.PENDING)
                        .and(q -> q.isNull(DomainEvent::getNextRetryAt)
                                .or().le(DomainEvent::getNextRetryAt, LocalDateTime.now()))
                        .last("LIMIT " + BATCH));
        for (DomainEvent event : pending) {
            try {
                restClient.post().uri(webhookUrl)
                        .body(Map.of(
                                "eventId", event.getId(),
                                "eventType", event.getEventType(),
                                "aggregateType", event.getAggregateType(),
                                "aggregateId", event.getAggregateId(),
                                "payload", event.getPayload()))
                        .retrieve().toBodilessEntity();
                event.setStatus(DomainEvent.SENT);
                event.setSentAt(LocalDateTime.now());
                domainEventMapper.updateById(event);
            } catch (Exception e) {
                int retry = event.getRetryCount() == null ? 0 : event.getRetryCount() + 1;
                event.setRetryCount(retry);
                if (retry >= MAX_RETRY) {
                    event.setStatus(DomainEvent.DEAD);
                    log.error("领域事件投递超限转死信: id={}, type={}", event.getId(), event.getEventType());
                } else {
                    event.setNextRetryAt(LocalDateTime.now().plusMinutes(1L << retry));   // 2^n 分钟退避
                    log.warn("领域事件投递失败将重试: id={}, retry={}, error={}",
                            event.getId(), retry, e.getMessage());
                }
                domainEventMapper.updateById(event);
            }
        }
    }
}
