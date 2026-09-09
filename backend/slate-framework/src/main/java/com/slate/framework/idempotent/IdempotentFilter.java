// 域/模块: 平台底座/工程规范
// 类型: 幂等过滤器
// 职责: 统一幂等（api-conventions §6）——写请求带 Idempotency-Key 头时，同 key 重复请求返回首次结果（Redis 缓存 24h）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.idempotent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.result.Result;
import com.slate.framework.error.SysErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 幂等过滤器：POST/PATCH/DELETE + Idempotency-Key 头 → Redis 抢占；
 * 占到的执行并缓存响应（含状态码/类型/体），未占到的轮询取首次结果，超时返回 409 SYS-005。
 */
@Component
public class IdempotentFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";
    private static final Duration TTL = Duration.ofHours(24);   // 契约定稿（api-conventions §6）
    private static final Logger log = LoggerFactory.getLogger(IdempotentFilter.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public IdempotentFilter(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        boolean writeMethod = HttpMethod.POST.matches(request.getMethod())
                || HttpMethod.PATCH.matches(request.getMethod())
                || HttpMethod.DELETE.matches(request.getMethod());
        if (key == null || key.isBlank() || !writeMethod || request.getRequestURI().contains("/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }
        String cacheKey = "idem:web:" + key;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            replay(response, cached);
            return;
        }
        // 先写占位（空标记），业务完成后由本过滤器回填响应体；占位存在但无响应体=处理中
        String marker = "idem:processing:" + key;
        Boolean acquired = redis.opsForValue().setIfAbsent(marker, "1", TTL);
        if (Boolean.FALSE.equals(acquired)) {
            waitForFirstResult(cacheKey, marker, request, response);
            return;
        }
        ContentCapture capture = new ContentCapture(response);
        try {
            filterChain.doFilter(request, capture);
        } finally {
            String body = capture.getCapturedBodyAsString();
            String entry = capture.getCapturedStatus() + "|" + capture.getContentType() + "|" + body;
            redis.opsForValue().set(cacheKey, entry, TTL);
            redis.delete(marker);
            capture.commitCaptured();
        }
    }

    private void waitForFirstResult(String cacheKey, String marker,
                                    HttpServletRequest request, HttpServletResponse response) throws IOException {
        for (int i = 0; i < 20; i++) {   // 最多等待 2s 取首次结果
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                replay(response, cached);
                return;
            }
            if (Boolean.TRUE != redis.hasKey(marker)) {
                break;   // 首次请求异常退出（未回填缓存），按新请求处理
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(SysErrorCode.SYS_005)));
    }

    private void replay(HttpServletResponse response, String cached) throws IOException {
        int sep1 = cached.indexOf('|');
        int sep2 = cached.indexOf('|', sep1 + 1);
        response.setStatus(Integer.parseInt(cached.substring(0, sep1)));
        String contentType = cached.substring(sep1 + 1, sep2);
        if (contentType != null && !contentType.isBlank()) {
            response.setContentType(contentType);
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HEADER + "-Replayed", "true");
        response.getWriter().write(cached.substring(sep2 + 1));
    }

    /** 捕获响应体（幂等缓存用），结束时一次性写回真实响应 */
    static class ContentCapture extends jakarta.servlet.http.HttpServletResponseWrapper {
        private final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        private String capturedContentType;
        private int capturedStatus = 200;

        ContentCapture(HttpServletResponse delegate) {
            super(delegate);
        }

        @Override
        public void setContentType(String type) {
            this.capturedContentType = type;
            super.setContentType(type);
        }

        @Override
        public String getContentType() {
            return capturedContentType;
        }

        int getCapturedStatus() {
            return capturedStatus;
        }

        @Override
        public void setStatus(int sc) {
            this.capturedStatus = sc;
            super.setStatus(sc);
        }

        @Override
        public jakarta.servlet.ServletOutputStream getOutputStream() {
            return new jakarta.servlet.ServletOutputStream() {
                @Override
                public void write(int b) {
                    buffer.write(b);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
                }
            };
        }

        @Override
        public java.io.PrintWriter getWriter() {
            return new java.io.PrintWriter(new java.io.OutputStreamWriter(buffer, StandardCharsets.UTF_8));
        }

        String getCapturedBodyAsString() {
            return buffer.toString(StandardCharsets.UTF_8);
        }

        /** 把捕获的响应体写回真实响应（业务只写了缓存，真实响应此刻还是空的） */
        void commitCaptured() throws IOException {
            if (buffer.size() > 0) {
                super.setContentLength(buffer.size());
                super.getOutputStream().write(buffer.toByteArray());
                super.getOutputStream().flush();
            }
        }
    }
}
