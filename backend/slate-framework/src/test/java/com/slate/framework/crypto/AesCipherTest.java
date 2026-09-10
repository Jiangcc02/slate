// 域/模块: 平台底座/工程规范
// 类型: 单元测试
// 职责: AesCipher 原语行为——加密随机性（随机 IV）、HMAC 确定性（等值查询/唯一键的根基）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AesCipherTest {

    private final AesCipher aes = new AesCipher(
            "0123456789abcdef0123456789abcdef", "test-hmac-key-for-unit-tests-only!!");

    @Test
    void 加密随机IV同明文密文不同且均可解密() {
        String first = aes.encrypt("13800001234");
        String second = aes.encrypt("13800001234");
        assertNotEquals(first, second);   // 随机 IV：加密列绝不可用于等值比较
        assertEquals("13800001234", aes.decrypt(first));
        assertEquals("13800001234", aes.decrypt(second));
    }

    @Test
    void 确定性哈希同明文恒同异明文恒异() {
        assertEquals(aes.hmac("13800001234"), aes.hmac("13800001234"));
        assertEquals(aes.hmac("S20260001"), aes.hmac("S20260001"));
        assertNotEquals(aes.hmac("13800001234"), aes.hmac("13800001235"));
        assertTrue(aes.hmac("S20260001").matches("[0-9a-f]{64}"));
    }

    @Test
    void 空值原样通过() {
        assertNull(aes.encrypt(null));
        assertNull(aes.hmac(null));
        assertNull(aes.hmac(""));   // 空串不产哈希（与加密列同约定：空值不建唯一键）
    }
}
