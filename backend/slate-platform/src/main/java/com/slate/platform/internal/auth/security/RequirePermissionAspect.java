// 域/模块: 平台底座/认证授权
// 类型: 权限切面
// 职责: 拦截 @RequirePermission 方法，按 SecurityContext 中 LoginUser 的权限码集校验，未命中抛 AUTH-003
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.security;

import com.slate.common.error.BusinessException;
import com.slate.platform.internal.auth.error.AuthErrorCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequirePermissionAspect {

    @Before("@annotation(requirePermission)")
    public void check(RequirePermission requirePermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser user)) {
            throw new BusinessException(AuthErrorCode.AUTH_004);
        }
        if (!user.hasPermission(requirePermission.value())) {
            throw new BusinessException(AuthErrorCode.AUTH_003);
        }
    }
}
