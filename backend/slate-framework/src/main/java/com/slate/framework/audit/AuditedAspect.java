// 域/模块: 平台底座/工程规范
// 类型: 审计切面
// 职责: 拦截 @Audited 方法——执行前后采集操作者/双身份/动作/参数摘要/结果/traceId/ip，发布审计事件
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.framework.trace.TraceIdFilter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Optional;

@Aspect
@Component
public class AuditedAspect {

    private static final int PARAMS_LIMIT = 500;

    private final ApplicationEventPublisher eventPublisher;
    private final Optional<AuditorProvider> auditorProvider;
    private final ObjectMapper objectMapper;

    public AuditedAspect(ApplicationEventPublisher eventPublisher,
                         Optional<AuditorProvider> auditorProvider,
                         ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.auditorProvider = auditorProvider;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        boolean success = false;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            publish(joinPoint, audited, success);
        }
    }

    private void publish(ProceedingJoinPoint joinPoint, Audited audited, boolean success) {
        try {
            eventPublisher.publishEvent(new OperationAuditEvent(
                    auditorProvider.flatMap(p -> Optional.ofNullable(p.currentAccountId())).orElse(null),
                    onBehalfOf(),
                    serviceId(),
                    audited.action(),
                    audited.target(),
                    digest(joinPoint.getArgs()),
                    success,
                    MDC.get(TraceIdFilter.MDC_KEY),
                    requestIp()));
        } catch (Exception e) {
            // 审计采集失败不阻断业务
            org.slf4j.LoggerFactory.getLogger(AuditedAspect.class).warn("审计事件发布失败: {}", e.getMessage());
        }
    }

    private String digest(Object[] args) {
        try {
            String json = objectMapper.writeValueAsString(args);
            return json.length() > PARAMS_LIMIT ? json.substring(0, PARAMS_LIMIT) + "…" : json;
        } catch (Exception e) {
            return Arrays.toString(args);
        }
    }

    private String serviceId() {
        return header("X-Service-Id");
    }

    private Long onBehalfOf() {
        String value = header("X-On-Behalf-Of");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String requestIp() {
        return currentRequest().map(req -> {
            String forwarded = req.getHeader("X-Forwarded-For");
            return forwarded == null || forwarded.isBlank() ? req.getRemoteAddr() : forwarded.split(",")[0].trim();
        }).orElse(null);
    }

    private String header(String name) {
        return currentRequest().map(req -> req.getHeader(name)).orElse(null);
    }

    private Optional<jakarta.servlet.http.HttpServletRequest> currentRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest);
    }
}
