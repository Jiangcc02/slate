// 域/模块: 平台底座/工程规范
// 类型: 公共契约接口
// 职责: 错误码契约——格式 <域码>-<三位序号>（api-conventions §4），各域实现自己的码表
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.common.error;

/**
 * 错误码契约：code 一经发布语义冻结、只增不改；message 面向开发者，
 * 面向学生的文案由前端按 code 映射（api-conventions §4）。
 */
public interface ErrorCode {

    /** 格式 <域码>-<三位序号>，如 AUTH-001。 */
    String code();

    /** 面向开发者的默认描述。 */
    String message();
}
