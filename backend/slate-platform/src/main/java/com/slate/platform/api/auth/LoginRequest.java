// 域/模块: 平台底座/认证授权
// 类型: 契约 DTO
// 职责: 登录请求体（契约见 docs/design/平台底座/detail/api/认证授权.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.auth;

import jakarta.validation.constraints.NotBlank;

/** POST /auth/login 请求体 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        /** 预留：登录端标识（admin/student/classroom），一期不强制 */
        String client) {
}
