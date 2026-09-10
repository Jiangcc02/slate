// 域/模块: 平台底座/工程规范
// 类型: Redis 工具
// 职责: 键空间遍历统一走 SCAN（禁止 KEYS 阻塞命令——O(全键空间) 会卡住 Redis 单线程）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.redis;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

/** 按通配符模式收集键（上限保护）；全部基于 Redis SCAN 增量游标，不阻塞其他命令 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /**
     * SCAN 收集匹配键，最多 maxKeys 个（防意外大集合撑爆内存；键空间语义下单批 COUNT=count）。
     * 返回后由调用方决定批量删除/逐个处理。
     */
    public static List<String> scan(StringRedisTemplate redis, String pattern, long maxKeys) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        List<String> keys = new ArrayList<>();
        redis.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext() && keys.size() < maxKeys) {
                    keys.add(new String(cursor.next()));
                }
            }
            return null;
        });
        return keys;
    }
}
