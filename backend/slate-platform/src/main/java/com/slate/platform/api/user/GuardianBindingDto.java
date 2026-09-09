// 域/模块: 平台底座/用户中心
// 类型: 契约 DTO
// 职责: 家长-学生绑定视图与发起/确认/解绑请求
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 绑定视图：手机号脱敏；confirmed 才允许家校域触达 */
public record GuardianBindingDto(Long id, Long guardianId, String guardianName, String guardianPhone,
                                 Long studentId, String studentName, String relation, Integer isPrimary,
                                 String status) {

    /** 发起绑定：家长手机号 + 学生档案 ID（不存在时自动建家长档） */
    public record BindRequest(
            @NotBlank @Pattern(regexp = "1\\d{10}$", message = "手机号须为 11 位") String phone,
            @NotNull Long studentId,
            @NotBlank @Pattern(regexp = "FATHER|MOTHER|GUARDIAN") String relation,
            Integer isPrimary) {
    }

    /** 确认（confirmed）/ 解绑（unbound，立即生效并通知家校域） */
    public record ActionRequest(@NotBlank @Pattern(regexp = "confirm|unbind") String action) {
    }
}
