// 域/模块: 平台底座/消息中心
// 类型: 错误码表（MSG）
// 职责: 消息中心错误码——语义与 detail/api/消息中心.md 对齐（api-conventions §4 冻结）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.msg.error;

import com.slate.common.error.ErrorCode;

/** 消息中心错误码（域码 MSG，属主=平台底座） */
public enum MsgErrorCode implements ErrorCode {

    MSG_001("MSG-001", "消息不存在或无权访问"),
    MSG_002("MSG-002", "公告范围节点无效"),
    MSG_003("MSG-003", "收件人集合为空或含无效账号");

    private final String code;
    private final String message;

    MsgErrorCode(String code, String message) {
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
