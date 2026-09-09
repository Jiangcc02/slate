// 域/模块: 平台底座/工程规范
// 类型: 公共异常模型
// 职责: 业务异常——携带 ErrorCode，由 framework 全局异常处理器转为统一响应
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.common.error;

/**
 * 业务异常：业务代码用它声明失败语义；参数可覆盖码表默认 message。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
