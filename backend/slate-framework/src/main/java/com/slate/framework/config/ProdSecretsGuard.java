// 域/模块: 平台底座/工程规范
// 类型: 启动校验
// 职责: prod profile 下拒绝以仓库内开发默认密钥启动（密钥永不入库的执行机制，iron-laws L6）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * 生产密钥防线：application.yml 中各密钥均为本地开发提供默认值；
 * 本守卫在 prod profile 激活时校验这些默认值不得原样进入生产，否则拒绝启动（fail-fast）。
 * 生产部署要求：SPRING_PROFILES_ACTIVE=prod + 全部密钥经环境变量注入（见 quickstart.md）。
 */
@Configuration
@Profile("prod")
public class ProdSecretsGuard {

    private final String jwtSecret;
    private final String cryptoKey;
    private final String cryptoHmacKey;
    private final String minioAccessKey;
    private final String minioSecretKey;

    public ProdSecretsGuard(@Value("${slate.jwt.secret:}") String jwtSecret,
                            @Value("${slate.crypto.key:}") String cryptoKey,
                            @Value("${slate.crypto.hmac-key:}") String cryptoHmacKey,
                            @Value("${slate.storage.access-key:}") String minioAccessKey,
                            @Value("${slate.storage.secret-key:}") String minioSecretKey) {
        this.jwtSecret = jwtSecret;
        this.cryptoKey = cryptoKey;
        this.cryptoHmacKey = cryptoHmacKey;
        this.minioAccessKey = minioAccessKey;
        this.minioSecretKey = minioSecretKey;
    }

    @PostConstruct
    public void check() {
        List<String> offenders = new ArrayList<>();
        if ("slate-dev-only-secret-key-32bytes!!".equals(jwtSecret) || jwtSecret.isBlank()) {
            offenders.add("slate.jwt.secret (SLATE_JWT_SECRET)");
        }
        if ("0123456789abcdef0123456789abcdef".equals(cryptoKey) || cryptoKey.isBlank()) {
            offenders.add("slate.crypto.key (SLATE_CRYPTO_KEY)");
        }
        if ("slate-dev-only-hmac-key-32bytes!!".equals(cryptoHmacKey) || cryptoHmacKey.isBlank()) {
            offenders.add("slate.crypto.hmac-key (SLATE_CRYPTO_HMAC_KEY)");
        }
        if ("minioadmin".equals(minioAccessKey) || minioAccessKey.isBlank()) {
            offenders.add("slate.storage.access-key (SLATE_MINIO_USER)");
        }
        if ("minioadmin".equals(minioSecretKey) || minioSecretKey.isBlank()) {
            offenders.add("slate.storage.secret-key (SLATE_MINIO_PASSWORD)");
        }
        if (!offenders.isEmpty()) {
            throw new IllegalStateException(
                    "prod profile 禁止使用开发默认密钥，以下配置必须经环境变量注入真实值：" + offenders);
        }
    }
}
