// 域/模块: 平台底座/系统管理
// 类型: 定时清理任务
// 职责: 只增不减表的保留期清理——登录日志/操作日志/已读消息投递/SENT 领域事件，按 slate.retention 配置执行
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.maintenance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.platform.internal.auth.entity.LoginLog;
import com.slate.platform.internal.auth.mapper.LoginLogMapper;
import com.slate.platform.internal.audit.entity.OperationLog;
import com.slate.platform.internal.audit.mapper.OperationLogMapper;
import com.slate.platform.internal.config.RetentionProperties;
import com.slate.platform.internal.events.entity.DomainEvent;
import com.slate.platform.internal.events.mapper.DomainEventMapper;
import com.slate.platform.internal.msg.entity.Message;
import com.slate.platform.internal.msg.entity.MessageDelivery;
import com.slate.platform.internal.msg.mapper.MessageDeliveryMapper;
import com.slate.platform.internal.msg.mapper.MessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 每日执行（EventDeliveryWorker 之外的第二类定时任务；与 FileRetentionWorker 共用单线程调度器串行运行）。
 * 各清理段独立 try/catch：单段失败不阻断其余段，下轮重试。
 * 已读投递删除后清理零投递的孤儿消息，message/message_delivery 联动收敛。
 */
@Component
public class RetentionWorker {

    private static final Logger log = LoggerFactory.getLogger(RetentionWorker.class);

    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final MessageDeliveryMapper deliveryMapper;
    private final MessageMapper messageMapper;
    private final DomainEventMapper domainEventMapper;
    private final RetentionProperties retention;

    public RetentionWorker(LoginLogMapper loginLogMapper,
                           OperationLogMapper operationLogMapper,
                           MessageDeliveryMapper deliveryMapper,
                           MessageMapper messageMapper,
                           DomainEventMapper domainEventMapper,
                           RetentionProperties retention) {
        this.loginLogMapper = loginLogMapper;
        this.operationLogMapper = operationLogMapper;
        this.deliveryMapper = deliveryMapper;
        this.messageMapper = messageMapper;
        this.domainEventMapper = domainEventMapper;
        this.retention = retention;
    }

    @Scheduled(cron = "${slate.retention.cron:0 30 4 * * *}")
    public void cleanup() {
        if (!retention.enabled()) {
            return;
        }
        try {
            int n = loginLogMapper.delete(new LambdaQueryWrapper<LoginLog>()
                    .lt(LoginLog::getCreatedAt, LocalDateTime.now().minusDays(retention.loginLogDays())));
            if (n > 0) {
                log.info("登录日志清理完成: {} 行（保留 {} 天）", n, retention.loginLogDays());
            }
        } catch (Exception e) {
            log.error("登录日志清理失败: {}", e.getMessage());
        }
        try {
            int n = operationLogMapper.delete(new LambdaQueryWrapper<OperationLog>()
                    .lt(OperationLog::getCreatedAt, LocalDateTime.now().minusDays(retention.operationLogDays())));
            if (n > 0) {
                log.info("操作日志清理完成: {} 行（保留 {} 天）", n, retention.operationLogDays());
            }
        } catch (Exception e) {
            log.error("操作日志清理失败: {}", e.getMessage());
        }
        try {
            int n = deliveryMapper.delete(new LambdaQueryWrapper<MessageDelivery>()
                    .isNotNull(MessageDelivery::getReadAt)
                    .lt(MessageDelivery::getReadAt, LocalDateTime.now().minusDays(retention.readDeliveryDays())));
            int orphanMessages = 0;
            if (n > 0) {
                orphanMessages = messageMapper.delete(new LambdaQueryWrapper<Message>()
                        .notInSql(Message::getId, "SELECT message_id FROM message_delivery"));
            }
            if (n > 0) {
                log.info("已读投递清理完成: {} 行投递 + {} 条孤儿消息（已读保留 {} 天）",
                        n, orphanMessages, retention.readDeliveryDays());
            }
        } catch (Exception e) {
            log.error("消息投递清理失败: {}", e.getMessage());
        }
        try {
            int n = domainEventMapper.delete(new LambdaQueryWrapper<DomainEvent>()
                    .eq(DomainEvent::getStatus, DomainEvent.SENT)
                    .lt(DomainEvent::getSentAt, LocalDateTime.now().minusDays(retention.sentEventDays())));
            if (n > 0) {
                log.info("SENT 领域事件清理完成: {} 行（保留 {} 天；DEAD 事件保留待人工排查）",
                        n, retention.sentEventDays());
            }
        } catch (Exception e) {
            log.error("领域事件清理失败: {}", e.getMessage());
        }
    }
}
