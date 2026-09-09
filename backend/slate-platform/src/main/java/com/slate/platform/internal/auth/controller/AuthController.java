// 域/模块: 平台底座/认证授权
// 类型: 控制器
// 职责: auth 前缀接口——登录/刷新/登出/改密/画像（契约见 docs/design/平台底座/detail/api/认证授权.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.controller;

import com.slate.platform.api.auth.AuthMeResponse;
import com.slate.platform.api.auth.LoginRequest;
import com.slate.platform.api.auth.PasswordChangeRequest;
import com.slate.platform.api.auth.RefreshRequest;
import com.slate.platform.api.auth.TokenPair;
import com.slate.platform.internal.auth.security.LoginUser;
import com.slate.platform.internal.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenPair login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request.username(), request.password(), clientIp(http), http.getHeader("User-Agent"));
    }

    @PostMapping("/tokens/refresh")
    public TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(@AuthenticationPrincipal LoginUser user, @Valid @RequestBody RefreshRequest request) {
        authService.logout(user.accountId(), request.refreshToken());
    }

    @PatchMapping("/password")
    public void changePassword(@AuthenticationPrincipal LoginUser user,
                               @Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(user.accountId(), request.oldPassword(), request.newPassword());
    }

    @GetMapping("/me")
    public AuthMeResponse me(@AuthenticationPrincipal LoginUser user) {
        return authService.me(user.accountId());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
