// 域/模块: 平台底座/工程规范
// 类型: 加密组件
// 职责: 敏感字段加密（AES-256-GCM，随机 IV 前置）与确定性哈希（HMAC-SHA256，等值查询/唯一键列）——core §7：未成年人敏感字段加密存储
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密列约定：Base64(IV(12B) + 密文)；密钥经 slate.crypto.key 配置（生产环境变量注入，本地默认仅开发用）。
 * 等值查询列约定：hmac() 产出确定性 64 位十六进制哈希，密钥经 slate.crypto.hmac-key 配置（与加密密钥分离）；
 * 同一明文恒定映射，供 WHERE eq 与 UNIQUE KEY 使用——禁止把 hmac 输出当作加密（其为单向，不可解密还原）。
 */
@Component
public class AesCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final SecretKey key;
    private final SecretKey hmacKey;
    private final SecureRandom random = new SecureRandom();

    public AesCipher(@Value("${slate.crypto.key:0123456789abcdef0123456789abcdef}") String keyText,
                     @Value("${slate.crypto.hmac-key:slate-dev-only-hmac-key-32bytes!!}") String hmacKeyText) {
        byte[] keyBytes = keyText.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("slate.crypto.key 须为 32 字节");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
        this.hmacKey = new SecretKeySpec(hmacKeyText.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** 确定性哈希：HMAC-SHA256 → 64 位小写十六进制（同明文恒定，可建唯一键；换 hmac-key 将导致旧哈希全部失配，须回填） */
    public String hmac(String plain) {
        if (plain == null || plain.isEmpty()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] digest = mac.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            char[] out = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                out[i * 2] = HEX[(digest[i] >> 4) & 0xF];
                out[i * 2 + 1] = HEX[digest[i] & 0xF];
            }
            return new String(out);
        } catch (Exception e) {
            throw new IllegalStateException("确定性哈希失败", e);
        }
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return encoded;
        }
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("解密失败", e);
        }
    }

    /** 展示脱敏：保留前 1 后 1（中文姓名场景），短串全掩码 */
    public static String maskName(String name) {
        if (name == null || name.length() < 2) {
            return "***";
        }
        return name.charAt(0) + "*".repeat(Math.max(name.length() - 2, 1)) + name.charAt(name.length() - 1);
    }

    /** 展示脱敏：手机号 138****5678；身份证/学籍号保留后 4 位 */
    public static String maskTail(String value, int keepHead, int keepTail) {
        if (value == null || value.length() <= keepHead + keepTail) {
            return "***";
        }
        return value.substring(0, keepHead) + "*".repeat(value.length() - keepHead - keepTail)
                + value.substring(value.length() - keepTail);
    }
}
