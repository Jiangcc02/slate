// 域/模块: 平台底座/启动器
// 类型: 应用入口
// 职责: slate 后端唯一启动入口——扫描 com.slate 全部 module 装配为单体
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * slate 后端启动入口。
 * 组件扫描显式声明为 com.slate：common（无组件）+ framework（基础设施件）+ platform（底座域）+ 后续各业务域 module。
 */
@SpringBootApplication(scanBasePackages = "com.slate")
public class SlateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlateApplication.class, args);
    }
}
