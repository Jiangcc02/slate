// 域/模块: 平台底座/组织架构
// 类型: 契约 DTO
// 职责: 班级名单视图与入出班请求
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.org;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** 名单条目（含学生摘要——档案归用户中心，此处仅引用展示） */
public record MemberDto(Long membershipId, Long studentId, String studentName,
                        String academicYear, String status,
                        LocalDateTime joinedAt, LocalDateTime leftAt, String leaveReason) {

    /** 入班：学年格式 2026-2027；批量学生 ID */
    public record AddRequest(
            @NotEmpty List<Long> studentIds,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{4}", message = "学年格式应为 2026-2027") String academicYear) {
    }

    /** 出班：必须带原因（异动留痕） */
    public record RemoveRequest(@NotBlank @Size(max = 128) String reason) {
    }
}
