// 域/模块: 平台底座/消息中心
// 类型: 契约 DTO
// 职责: 公告视图与创建/更新请求（范围=组织节点引用）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.msg;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AnnouncementDto(Long id, String title, String content, String scopeType, Long scopeOrgId,
                              String status, LocalDateTime publishedAt) {

    /** 发布公告：范围 SCHOOL 全校（orgId 空）；其余范围须有效组织节点（MSG-002） */
    public record CreateRequest(
            @NotBlank @Size(max = 128) String title,
            @NotBlank @Size(max = 4000) String content,
            @NotBlank @Pattern(regexp = "SCHOOL|CAMPUS|SECTION|GRADE|CLASS") String scopeType,
            Long scopeOrgId) {
    }
}
