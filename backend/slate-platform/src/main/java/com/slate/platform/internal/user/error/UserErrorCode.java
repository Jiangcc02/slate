// 域/模块: 平台底座/用户中心
// 类型: 错误码表（USER）
// 职责: 用户中心错误码——语义与 detail/api/用户中心.md 逐条对齐（api-conventions §4 冻结）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.user.error;

import com.slate.common.error.ErrorCode;

/** 用户中心错误码（域码 USER，属主=平台底座） */
public enum UserErrorCode implements ErrorCode {

    USER_001("USER-001", "用户档案不存在"),
    USER_002("USER-002", "手机号已被其他账号占用"),
    USER_003("USER-003", "学籍号不存在或与姓名不匹配"),
    USER_004("USER-004", "绑定记录不存在或已解绑"),
    USER_005("USER-005", "导入文件格式或模板错误"),
    USER_006("USER-006", "导入行校验失败");

    private final String code;
    private final String message;

    UserErrorCode(String code, String message) {
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
