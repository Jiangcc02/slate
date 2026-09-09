// 域/模块: 平台底座/工程规范
// 类型: 响应出口包装
// 职责: 把控制器成功返回的裸业务对象统一包成 Result 信封并补齐 traceId（api-conventions §3）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.web;

import com.slate.common.result.Result;
import com.slate.framework.trace.TraceIdFilter;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应出口：业务控制器只返回裸业务对象，信封（code/message/data/traceId）在此补齐。
 * 已是 Result 的（异常处理器出口、显式构造）原样放行；二进制体（文件流）不包。
 */
@RestControllerAdvice(basePackages = "com.slate")
public class ResultWrapAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result<?> result) {
            return result.getTraceId() == null ? result.withTraceId(currentTraceId()) : result;
        }
        if (body instanceof byte[] || body instanceof Resource) {
            return body;
        }
        return Result.ok(body).withTraceId(currentTraceId());
    }

    private String currentTraceId() {
        return MDC.get(TraceIdFilter.MDC_KEY);
    }
}
