// 域/模块: 平台底座/工程规范
// 类型: 错误码表（SYS）
// 职责: 框架级兜底错误码——参数校验失败/系统内部错误（api-conventions §4 域码 SYS）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.error;

import com.slate.common.error.ErrorCode;

/**
 * 系统级错误码（域码 SYS，属主=平台底座）。
 * 语义一经发布冻结、只增不改（api-conventions §4）。
 */
public enum SysErrorCode implements ErrorCode {

    /** 参数校验失败（请求体/参数不合法） */
    SYS_001("SYS-001", "参数校验失败"),

    /** 请求方式或媒体类型不支持（框架兜底） */
    SYS_002("SYS-002", "请求方式或媒体类型不支持"),

    /** 字典类型不存在 */
    SYS_003("SYS-003", "字典类型不存在"),

    /** 参数键不存在或作用域冲突 */
    SYS_004("SYS-004", "参数键不存在或作用域冲突"),

    /** 重复请求处理中（同 Idempotency-Key 的首次请求尚未完成，稍后重试取首次结果） */
    SYS_005("SYS-005", "重复请求处理中"),

    /** 系统内部错误（未分类异常兜底） */
    SYS_999("SYS-999", "系统内部错误");

    private final String code;
    private final String message;

    SysErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
