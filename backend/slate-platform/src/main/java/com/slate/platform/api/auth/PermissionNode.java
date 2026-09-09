// 域/模块: 平台底座/认证授权
// 类型: 契约 DTO
// 职责: 权限树节点（GET /permissions，menu/button 两级树，code 全集）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.auth;

import java.util.List;

/** 权限树节点 */
public record PermissionNode(Long id, String code, String name, String type, Integer sort, List<PermissionNode> children) {
}
