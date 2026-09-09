// 域/模块: 平台底座/认证授权
// 类型: 审计上下文实现
// 职责: 向 framework 审计切面提供当前登录操作者（LoginUser.accountId）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.security;

import com.slate.framework.audit.AuditorProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditorProvider implements AuditorProvider {

    @Override
    public Long currentAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser user) {
            return user.accountId();
        }
        return null;
    }
}
