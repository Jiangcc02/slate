// 域/模块: 平台底座/用户中心
// 类型: 契约 DTO
// 职责: 用户档案视图（默认脱敏）与建档/更新/生命周期请求
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 脱敏视图：姓名 张*三、学籍号尾4、手机号 138****5678（core §7 展示脱敏） */
public record UserDto(Long id, String username, String realName, String userType, String accountStatus,
                      String studentNo, String gradeEntry, String staffNo, String subject, String title,
                      String phone, LocalDateTime createdAt) {

    public record CreateRequest(
            @NotBlank @Pattern(regexp = "STUDENT|TEACHER|GUARDIAN|STAFF") String userType,
            @NotBlank @Size(max = 64) String realName,
            @Size(max = 64) String username,
            @Size(max = 64) String studentNo,
            @Size(max = 16) String gradeEntry,
            @Size(max = 64) String staffNo,
            @Size(max = 32) String subject,
            @Size(max = 64) String title,
            @Pattern(regexp = "^$|1\\d{10}$", message = "手机号须为 11 位") String phone) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 64) String realName,
            @Size(max = 64) String studentNo,
            @Size(max = 16) String gradeEntry,
            @Size(max = 64) String staffNo,
            @Size(max = 32) String subject,
            @Size(max = 64) String title) {
    }

    /** 生命周期动作；重置密码返回的 result 含一次性新密码 */
    public record StatusRequest(@Pattern(regexp = "activate|disable|reset-password") String action) {
    }

    public record StatusResult(String action, String newPassword) {
    }
}
