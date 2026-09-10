// 域/模块: 平台底座/工程规范
// 类型: ID 生成器
// 职责: 主链 ID 统一生成器——雪花算法（int64 趋势递增，global-data-model §2 定稿）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.id;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花 ID 生成器：41 位毫秒时间戳 + 10 位机器号 + 12 位序列。
 * 主链实体 ID 由属主域经本生成器产生，他域只引用（global-data-model §2）。
 * workerId 单机部署默认 1；多实例部署时必须为每个实例配置唯一 workerId（0~1023）。
 */
@Component
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1735689600000L; // 2025-01-01T00:00:00Z，ID 时钟起点
    private static final long WORKER_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    /** 时钟回拨容忍窗口：NTP 微调（毫秒~秒级）在此窗口内等待追平而非拒绝服务；超过仍拒绝（保 ID 不重复） */
    private static final long ROLLBACK_TOLERANCE_MS = 5_000L;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(@Value("${slate.id.worker:1}") long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 必须在 0~" + MAX_WORKER_ID + " 之间，当前: " + workerId);
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            timestamp = waitUntil(lastTimestamp);
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    /** 小幅回拨：阻塞等待时钟追平上次发号时刻（容忍窗口内）；超窗口或被中断则拒绝发号 */
    private long waitUntil(long target) {
        long deadline = System.currentTimeMillis() + ROLLBACK_TOLERANCE_MS;
        long current = System.currentTimeMillis();
        while (current < target) {
            if (current > deadline) {
                throw new IllegalStateException("系统时钟回拨超过容忍窗口（%d ms），拒绝生成 ID"
                        .formatted(target - current));
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("时钟回拨等待被中断，拒绝生成 ID", e);
            }
            current = System.currentTimeMillis();
        }
        return current;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
