// 域/模块: 平台底座/认证授权
// 类型: JWT 服务
// 职责: 双 token 签发与校验——access 2h（接口鉴权）/ refresh 7d 滚动（白名单撤销），api-conventions §2 定稿
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.auth.service;

import com.slate.platform.api.auth.TokenPair;
import com.slate.platform.internal.auth.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** 双 token：refresh 白名单存 Redis（key=refresh:{accountId}:{jti}），登出即删=撤销，刷新即旋转 */
@Service
public class TokenService {

    static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_USERNAME = "uname";
    private static final String CLAIM_ROLES = "roles";

    private final StringRedisTemplate redis;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private SecretKey key;

    public TokenService(
            StringRedisTemplate redis,
            @Value("${slate.jwt.access-ttl:PT2H}") Duration accessTtl,
            @Value("${slate.jwt.refresh-ttl:P7D}") Duration refreshTtl,
            @Value("${slate.jwt.secret}") String secret) {
        this.redis = redis;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("slate.jwt.secret 须至少 32 字节（HS256）");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    /** 签发双 token 并登记 refresh 白名单 */
    public TokenPair issue(Long accountId, Long userId, String username, List<String> roleCodes) {
        long now = System.currentTimeMillis();
        String accessToken = Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim(CLAIM_USER_ID, String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, String.join(",", roleCodes))
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTtl.toMillis()))
                .signWith(key)
                .compact();
        String jti = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = Jwts.builder()
                .subject(String.valueOf(accountId))
                .id(jti)
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTtl.toMillis()))
                .signWith(key)
                .compact();
        redis.opsForValue().set(whitelistKey(accountId, jti), "1", refreshTtl);
        return new TokenPair(accessToken, refreshToken, accessTtl.toSeconds(), refreshTtl.toSeconds());
    }

    /** 解析 access token → 登录主体；无效/过期返回 null（由调用方决定 401） */
    public LoginUser parseAccessToken(String accessToken) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(accessToken).getPayload();
            List<String> roles = claims.get(CLAIM_ROLES, String.class) == null
                    ? List.of()
                    : List.of(claims.get(CLAIM_ROLES, String.class).split(","));
            return new LoginUser(
                    Long.valueOf(claims.getSubject()),
                    Long.valueOf(claims.get(CLAIM_USER_ID, String.class)),
                    claims.get(CLAIM_USERNAME, String.class),
                    roles,
                    List.of());   // 权限码由 Filter 按缓存补齐（access token 不携带，避免改权限后长期失效）
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /** 校验并消费 refresh token：白名单内的 jti 才有效，消费即旋转（旧 jti 作废）；
     *  返回 accountId 供调用方重查最新角色后另行签发；无效/已撤销返回 null */
    public Long consumeRefresh(String refreshToken) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(refreshToken).getPayload();
            Long accountId = Long.valueOf(claims.getSubject());
            String whitelistKey = whitelistKey(accountId, claims.getId());
            if (Boolean.TRUE != redis.hasKey(whitelistKey)) {
                return null;   // 已撤销/已旋转/已登出
            }
            redis.delete(whitelistKey);
            return accountId;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** 撤销某账号全部 refresh（改密/禁用场景：scan 前缀删除） */
    public void revokeAll(Long accountId) {
        var keys = redis.keys(REFRESH_KEY_PREFIX + accountId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    /** 撤销单个 refresh（登出） */
    public void revoke(Long accountId, String refreshToken) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(refreshToken).getPayload();
            redis.delete(whitelistKey(accountId, claims.getId()));
        } catch (JwtException | IllegalArgumentException ignored) {
            // 无效 token 撤销无意义，静默
        }
    }

    private String whitelistKey(Long accountId, String jti) {
        return REFRESH_KEY_PREFIX + accountId + ":" + jti;
    }
}
