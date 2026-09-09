// 域/模块: 平台底座/工程规范
// 类型: 全局异常处理器
// 职责: 把异常转为统一响应 Result（业务异常透传错误码，未分类异常兜底 SYS-999），并补齐 traceId
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.web;

import com.slate.common.error.BusinessException;
import com.slate.common.result.Result;
import com.slate.framework.error.SysErrorCode;
import com.slate.framework.trace.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：所有接口异常的统一出口。
 * 业务异常（BusinessException）返回其错误码；参数校验失败聚合字段信息；其余兜底 SYS-999。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusiness(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getErrorCode().code(), ex.getMessage());
        return Result.<Void>fail(ex.getErrorCode(), ex.getMessage()).withTraceId(currentTraceId());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.<Void>fail(SysErrorCode.SYS_001, detail).withTraceId(currentTraceId());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknown(Exception ex) {
        log.error("系统内部错误", ex);
        return Result.<Void>fail(SysErrorCode.SYS_999).withTraceId(currentTraceId());
    }

    private String currentTraceId() {
        return MDC.get(TraceIdFilter.MDC_KEY);
    }
}
