// 域/模块: 平台底座/系统管理
// 类型: 配置属性
// 职责: slate.retention 绑定——只增不减数据的保留期与清理任务开关（RetentionWorker / FileRetentionWorker 共用）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 只增不减数据的保留策略（默认值经协调者定稿 2026-09-10）：
 * login-log-days=180 / operation-log-days=365 / read-delivery-days=90 /
 * sent-event-days=30 / deleted-file-days=7 / orphan-file-hours=48。
 * cron 为清理任务统一触发时刻（默认每日 04:30，单线程调度器串行执行）。
 */
@ConfigurationProperties(prefix = "slate.retention")
public record RetentionProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("0 30 4 * * *") String cron,
        @DefaultValue("180") int loginLogDays,
        @DefaultValue("365") int operationLogDays,
        @DefaultValue("90") int readDeliveryDays,
        @DefaultValue("30") int sentEventDays,
        @DefaultValue("7") int deletedFileDays,
        @DefaultValue("48") int orphanFileHours) {
}
