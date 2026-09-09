// 域/模块: 平台底座/认证授权
// 类型: 契约 DTO
// 职责: 刷新与登出请求体（refreshToken）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.auth;

import jakarta.validation.constraints.NotBlank;

/** POST /auth/tokens/refresh 与 /auth/logout 请求体 */
public record RefreshRequest(@NotBlank(message = "refreshToken 不能为空") String refreshToken) {
}
