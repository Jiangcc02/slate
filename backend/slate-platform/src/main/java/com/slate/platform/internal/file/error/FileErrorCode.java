// 域/模块: 平台底座/文件服务
// 类型: 错误码表（FILE）
// 职责: 文件服务错误码——语义与 detail/api/文件服务.md 对齐（api-conventions §4 冻结）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.file.error;

import com.slate.common.error.ErrorCode;

/** 文件服务错误码（域码 FILE，属主=平台底座） */
public enum FileErrorCode implements ErrorCode {

    FILE_001("FILE-001", "文件不存在或已删除"),
    FILE_002("FILE-002", "文件类型不在白名单"),
    FILE_003("FILE-003", "文件大小超限或配额用尽"),
    FILE_004("FILE-004", "文件被业务引用中，禁止删除"),
    FILE_005("FILE-005", "objectKey 登记不一致");

    private final String code;
    private final String message;

    FileErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
