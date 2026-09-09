// 域/模块: 平台底座/认证授权
// 类型: 错误码表（AUTH）
// 职责: 认证授权错误码——语义与 detail/api/认证授权.md 逐条对齐，一经发布冻结（api-conventions §4）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.error;

import com.slate.common.error.ErrorCode;

/** 认证授权错误码（域码 AUTH，属主=平台底座） */
public enum AuthErrorCode implements ErrorCode {

    /** 用户名或密码错误（统一提示，不区分暴露哪个错） */
    AUTH_001("AUTH-001", "用户名或密码错误"),

    /** 账号已锁定（连续失败 5 次锁 15 分钟） */
    AUTH_002("AUTH-002", "账号已锁定，请稍后重试"),

    /** 无权限访问 */
    AUTH_003("AUTH-003", "无权限访问"),

    /** 未登录或 token 无效/过期 */
    AUTH_004("AUTH-004", "未登录或登录态已过期"),

    /** refresh token 无效或已撤销 */
    AUTH_005("AUTH-005", "刷新凭证无效，请重新登录"),

    /** 预置角色不可修改 */
    AUTH_006("AUTH-006", "预置角色不可修改");

    private final String code;
    private final String message;

    AuthErrorCode(String code, String message) {
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
