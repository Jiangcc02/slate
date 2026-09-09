// 域/模块: 平台底座/工程规范
// 类型: 单元测试
// 职责: 雪花 ID 生成器验证——唯一性、趋势递增、机器号隔离、非法 workerId 拒绝
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTest {

    @Test
    void 同一实例批量生成不重复且趋势递增() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        Set<Long> seen = new HashSet<>();
        long prev = 0;
        for (int i = 0; i < 100_000; i++) {
            long id = generator.nextId();
            assertTrue(seen.add(id), "ID 重复: " + id);
            assertTrue(id > prev, "ID 非趋势递增: " + prev + " -> " + id);
            prev = id;
        }
    }

    @Test
    void 不同机器号生成的ID互不冲突() {
        SnowflakeIdGenerator a = new SnowflakeIdGenerator(0);
        SnowflakeIdGenerator b = new SnowflakeIdGenerator(1);
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            assertTrue(seen.add(a.nextId()));
            assertTrue(seen.add(b.nextId()));
        }
    }

    @Test
    void 非法workerId被拒绝() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1024));
    }
}
