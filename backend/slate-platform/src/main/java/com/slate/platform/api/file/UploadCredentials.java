// 域/模块: 平台底座/文件服务
// 类型: 契约 DTO
// 职责: 预签名凭证、登记请求、文件视图（契约见 detail/api/文件服务.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.api.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/** 预签名上传凭证：客户端持此 URL 直传对象存储，字节流不经业务单体 */
public record UploadCredentials(String objectKey, String uploadUrl, LocalDateTime expireAt) {

    /** 申请预签名：bizType 决定白名单与大小上限 */
    public record Request(
            @NotBlank @Pattern(regexp = "courseware|image|video") String bizType,
            @NotBlank String fileName,
            @NotNull @Positive Long sizeBytes,
            String contentType) {
    }

    /** 上传完成后的元数据登记 */
    public record RegisterRequest(
            @NotBlank String objectKey,
            @NotBlank @Pattern(regexp = "courseware|image|video") String bizType,
            Long bizId,
            @NotBlank String fileName,
            String contentType,
            @NotNull @Positive Long sizeBytes) {
    }
}
