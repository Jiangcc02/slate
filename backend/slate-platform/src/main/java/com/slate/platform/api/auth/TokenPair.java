// 域/模块: 平台底座/认证授权
// 类型: 契约 DTO
// 职责: 双 token 响应（access 2h / refresh 7d 滚动，api-conventions §2 定稿）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.auth;

/** 登录/刷新成功响应：token 值 + 有效期（秒），前端据此排期刷新 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn) {
}
