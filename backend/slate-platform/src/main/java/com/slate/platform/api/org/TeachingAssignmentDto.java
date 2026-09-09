// 域/模块: 平台底座/组织架构
// 类型: 契约 DTO
// 职责: 任课关系视图与维护请求（courseId 引用课程域 ID 不复制）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.org;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 任课关系（教师×班级×课程×学年学期） */
public record TeachingAssignmentDto(Long id, Long teacherId, String teacherName,
                                    Long classId, String className,
                                    Long courseId, String academicYear, Integer semester, String subject) {

    /** 建立任课（同教师×班级×课程×学年学期唯一，ORG-004） */
    public record CreateRequest(
            @NotNull Long teacherId,
            @NotNull Long classId,
            @NotNull Long courseId,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{4}") String academicYear,
            @NotNull @Min(1) @Max(2) Integer semester,
            @NotBlank @Size(max = 32) String subject) {
    }
}
