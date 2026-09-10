// 域/模块: 平台底座/认证授权
// 类型: 安全过滤器
// 职责: Bearer access token 校验 → 重建 LoginUser（权限码走 RbacService 缓存）→ 注入 SecurityContext
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.security;

import com.slate.framework.idempotent.IdempotentFilter;
import com.slate.platform.internal.auth.service.RbacService;
import com.slate.platform.internal.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** 无 token 或 token 无效时按匿名放行，由授权规则触发 401（AUTH-004） */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final RbacService rbacService;

    public JwtAuthenticationFilter(TokenService tokenService, RbacService rbacService) {
        this.tokenService = tokenService;
        this.rbacService = rbacService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            LoginUser user = tokenService.parseAccessToken(header.substring(7));
            if (user != null) {
                RbacService.AccountGrants grants = rbacService.loadGrants(user.accountId());
                // 账号状态随权限缓存下发：禁用/锁定账号即时 401，不再等 access token 自然过期（≤2h 窗口）
                if ("active".equals(grants.accountStatus())) {
                    LoginUser enriched = new LoginUser(
                            user.accountId(), user.userId(), user.username(),
                            grants.roleCodes(), grants.permissionCodes());
                    List<SimpleGrantedAuthority> authorities = grants.permissionCodes().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(enriched, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    request.setAttribute(IdempotentFilter.ACCOUNT_ATTR, user.accountId());
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
