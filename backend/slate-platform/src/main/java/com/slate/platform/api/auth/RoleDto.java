// 域/模块: 平台底座/认证授权
// 类型: 契约 DTO
// 职责: 角色 DTO 与创建/更新请求（roles 前缀管理接口）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 角色视图 */
public record RoleDto(Long id, String code, String name, String type, String remark) {

    /** 创建角色（code 大写字母/数字/下划线；预置角色禁止创建同名） */
    public record CreateRequest(
            @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "角色码须为大写字母开头的字母/数字/下划线") String code,
            @NotBlank @Size(max = 64) String name,
            @Size(max = 255) String remark) {
    }

    /** 更新角色（仅自定义角色，预置角色返回 AUTH-006） */
    public record UpdateRequest(
            @NotBlank @Size(max = 64) String name,
            @Size(max = 255) String remark) {
    }
}
