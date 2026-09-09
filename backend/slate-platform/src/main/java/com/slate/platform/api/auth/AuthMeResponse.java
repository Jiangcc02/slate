// 域/模块: 平台底座/认证授权
// 类型: 契约 DTO
// 职责: GET /auth/me 响应——账号画像（角色集/权限码集/组织归属），前端按权限码渲染菜单与按钮
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.auth;

import java.util.List;

/** 当前账号画像；orgId 随组织架构模块接入后回填（当前可空） */
public record AuthMeResponse(
        Long accountId,
        String username,
        String realName,
        List<String> roleCodes,
        List<String> permissionCodes,
        Long orgId) {
}
