// 域/模块: 平台底座/认证授权
// 类型: 安全模型
// 职责: 登录态主体——账号/档案/角色码/权限码，随 JWT 校验重建于 SecurityContext
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.security;

import java.util.List;

/** 请求级登录主体（由 JwtAuthenticationFilter 依据 access token 重建） */
public record LoginUser(
        Long accountId,
        Long userId,
        String username,
        List<String> roleCodes,
        List<String> permissionCodes) {

    public boolean hasPermission(String code) {
        return permissionCodes.contains(code);
    }
}
