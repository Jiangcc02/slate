// 域/模块: 平台底座/认证授权
// 类型: 控制器
// 职责: roles/permissions 前缀接口——角色管理与授权、权限树（契约见 detail/api/认证授权.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.controller;

import com.slate.platform.api.auth.PermissionNode;
import com.slate.platform.api.auth.RoleDto;
import com.slate.platform.internal.auth.security.RequirePermission;
import com.slate.platform.internal.auth.service.RbacService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RoleController {

    private final RbacService rbacService;

    public RoleController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/roles")
    @RequirePermission("sys:role:read")
    public List<RoleDto> listRoles() {
        return rbacService.listRoles();
    }

    @PostMapping("/roles")
    @RequirePermission("sys:role:write")
    public RoleDto createRole(@Valid @RequestBody RoleDto.CreateRequest request) {
        return rbacService.createRole(request);
    }

    @PatchMapping("/roles/{id}")
    @RequirePermission("sys:role:write")
    public void updateRole(@PathVariable Long id, @Valid @RequestBody RoleDto.UpdateRequest request) {
        rbacService.updateRole(id, request);
    }

    @DeleteMapping("/roles/{id}")
    @RequirePermission("sys:role:write")
    public void deleteRole(@PathVariable Long id) {
        rbacService.deleteRole(id);
    }

    /** 全量替换角色授权（body=权限 ID 集合，空集合=清空） */
    @PostMapping("/roles/{id}/permissions")
    @RequirePermission("sys:role:write")
    public void grantPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        rbacService.grantPermissions(id, permissionIds);
    }

    @GetMapping("/permissions")
    @RequirePermission("sys:role:read")
    public List<PermissionNode> permissionTree() {
        return rbacService.permissionTree();
    }
}
