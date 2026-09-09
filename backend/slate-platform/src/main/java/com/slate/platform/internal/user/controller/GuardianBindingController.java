// 域/模块: 平台底座/用户中心
// 类型: 控制器
// 职责: 家长绑定接口——学生的绑定列表/发起/确认/解绑（契约见 detail/api/用户中心.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.user.controller;

import com.slate.framework.audit.Audited;
import com.slate.platform.api.user.GuardianBindingDto;
import com.slate.platform.internal.auth.security.RequirePermission;
import com.slate.platform.internal.user.service.GuardianBindingService;
import jakarta.validation.Valid;
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
public class GuardianBindingController {

    private final GuardianBindingService bindingService;

    public GuardianBindingController(GuardianBindingService bindingService) {
        this.bindingService = bindingService;
    }

    @GetMapping("/users/{id}/guardians")
    @RequirePermission("user:read")
    public List<GuardianBindingDto> listByStudent(@PathVariable Long id) {
        return bindingService.listByStudent(id);
    }

    @PostMapping("/guardian-bindings")
    @RequirePermission("user:bind:write")
    @Audited(action = "user.guardian.bind", target = "guardian_student")
    public GuardianBindingDto bind(@Valid @RequestBody GuardianBindingDto.BindRequest request) {
        return bindingService.bind(request);
    }

    @PatchMapping("/guardian-bindings/{id}")
    @RequirePermission("user:bind:write")
    @Audited(action = "user.guardian.act", target = "guardian_student")
    public GuardianBindingDto act(@PathVariable Long id,
                                  @Valid @RequestBody GuardianBindingDto.ActionRequest request) {
        return bindingService.act(id, request.action());
    }
}
