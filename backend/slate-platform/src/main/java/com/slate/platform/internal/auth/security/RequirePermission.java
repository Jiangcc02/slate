// 域/模块: 平台底座/认证授权
// 类型: 权限注解
// 职责: 接口层注解鉴权（契约：@RequirePermission("sys:role:write")，RBAC 到菜单/按钮级）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 声明接口所需权限码（permission.code）；无权限抛 AUTH-003 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();
}
