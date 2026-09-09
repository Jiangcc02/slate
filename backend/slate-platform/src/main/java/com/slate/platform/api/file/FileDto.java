// 域/模块: 平台底座/文件服务
// 类型: 契约 DTO
// 职责: 文件元数据视图
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.file;

import java.time.LocalDateTime;

public record FileDto(Long id, String objectKey, String bizType, Long bizId, String fileName,
                      String contentType, Long sizeBytes, String visibility, Long uploadedBy,
                      LocalDateTime createdAt) {
}
