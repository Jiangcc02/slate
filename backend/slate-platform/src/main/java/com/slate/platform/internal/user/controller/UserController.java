// 域/模块: 平台底座/用户中心
// 类型: 控制器
// 职责: users 前缀接口——档案检索/建档开户/更新/生命周期/导入（契约见 detail/api/用户中心.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.user.controller;

import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.audit.Audited;
import com.slate.platform.api.user.ImportResult;
import com.slate.platform.api.user.UserDto;
import com.slate.platform.internal.auth.security.RequirePermission;
import com.slate.platform.internal.user.service.UserImportService;
import com.slate.platform.internal.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserImportService importService;

    public UserController(UserService userService, UserImportService importService) {
        this.userService = userService;
        this.importService = importService;
    }

    @GetMapping
    @RequirePermission("user:read")
    public PageResult<UserDto> search(@RequestParam(required = false) String userType,
                                      @RequestParam(required = false) String realName,
                                      @RequestParam(required = false) String accountStatus,
                                      PageQuery query) {
        return userService.search(userType, realName, accountStatus, query);
    }

    @GetMapping("/{id}")
    @RequirePermission("user:read")
    public UserDto get(@PathVariable Long id, @RequestParam(required = false, defaultValue = "false") boolean reveal) {
        if (reveal) {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof com.slate.platform.internal.auth.security.LoginUser user)
                    || !user.hasPermission("user:lifecycle")) {
                throw new com.slate.common.error.BusinessException(
                        com.slate.platform.internal.user.error.UserErrorCode.USER_001, "明文查看需 user:lifecycle 权限");
            }
        }
        return userService.get(id, reveal);
    }

    @PostMapping
    @RequirePermission("user:write")
    @Audited(action = "user.create", target = "user_profile")
    public UserDto create(@Valid @RequestBody UserDto.CreateRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}")
    @RequirePermission("user:write")
    @Audited(action = "user.update", target = "user_profile")
    public void update(@PathVariable Long id, @Valid @RequestBody UserDto.UpdateRequest request) {
        userService.update(id, request);
    }

    @PostMapping("/{id}/status")
    @RequirePermission("user:lifecycle")
    @Audited(action = "user.lifecycle", target = "account")
    public UserDto.StatusResult lifecycle(@PathVariable Long id,
                                          @Valid @RequestBody UserDto.StatusRequest request) {
        return userService.lifecycle(id, request.action());
    }

    /** 同步导入（一期百级规模；异步任务化随规模需求升级，契约已标注） */
    @PostMapping("/import")
    @RequirePermission("user:import")
    @Audited(action = "user.import", target = "user_profile")
    public ImportResult importStudents(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "2026-2027") String academicYear) throws IOException {
        return importService.importStudents(file.getInputStream(), academicYear);
    }
}
