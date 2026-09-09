// 域/模块: 平台底座/消息中心
// 类型: 契约 DTO
// 职责: 站内信视图与发送请求（契约见 detail/api/消息中心.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.msg;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** 收件箱条目（readAt 空=未读） */
public record MessageDto(Long id, String title, String content, String refType, Long refId,
                         LocalDateTime createdAt, LocalDateTime readAt) {

    /** 发送站内信（服务间/域内调用为主）：收件人集合 + 标题 + 正文 + 跳转引用 */
    public record SendRequest(
            @NotEmpty List<Long> receiverIds,
            @NotBlank @Size(max = 128) String title,
            @NotBlank @Size(max = 2000) String content,
            @Size(max = 32) String refType,
            Long refId) {
    }
}
