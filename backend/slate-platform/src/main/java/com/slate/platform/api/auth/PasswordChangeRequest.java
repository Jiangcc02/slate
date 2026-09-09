// 域/模块: 平台底座/认证授权
// 类型: 契约 DTO
// 职责: 修改密码请求体（PATCH /auth/password，验旧设新，成功后全部端下线）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新密码长度 ≥ 8 */
public record PasswordChangeRequest(
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 8, max = 64, message = "新密码长度须为 8~64 位") String newPassword) {
}
