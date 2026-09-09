// 域/模块: 平台底座/启动器
// 类型: 冒烟测试
// 职责: 单体上下文可装配启动（contextLoads）——骨架阶段的最小可运行验收
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.boot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 需要本机 MySQL(3306) 与 Redis(6379)；本地凭证在 application-local.yml（不入库），CI 走环境变量 */
@SpringBootTest
@ActiveProfiles("local")
class SlateApplicationTests {

    @Test
    void 上下文可装配启动() {
    }
}
