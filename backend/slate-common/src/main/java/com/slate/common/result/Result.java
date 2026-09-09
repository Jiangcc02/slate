// 域/模块: 平台底座/工程规范
// 类型: 公共契约模型
// 职责: 全系统统一响应结构 { code, message, data, traceId }（api-conventions §3）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.common.result;

import com.slate.common.error.ErrorCode;

/**
 * 统一响应结构：code=0 成功，非 0 为业务错误码；traceId 与日志链路一致、必返。
 */
public class Result<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;

    protected Result() {
    }

    private Result(String code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>("0", "ok", data, null);
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.code(), errorCode.message(), null, null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.code(), message, null, null);
    }

    /** framework 在响应出口补齐链路号，业务代码不填 traceId。 */
    public Result<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public boolean isSuccess() {
        return "0".equals(code);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }
}
