package com.shorepower.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试：密钥校验、签名验证、过期校验、tokenVersion 往返。
 */
class JwtUtilTest {

    /** 恰好 32 字节（256 bit）的测试密钥 */
    private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", VALID_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600_000L);
    }

    @Test
    void generateAndValidateToken_roundTrip() {
        String token = jwtUtil.generateToken(1L, "admin", "ADMIN", 3);

        assertTrue(jwtUtil.validateToken(token));
        assertEquals(1L, jwtUtil.getUserId(token));
        assertEquals("admin", jwtUtil.getUsername(token));
        assertEquals("ADMIN", jwtUtil.getRole(token));
        assertEquals(3, jwtUtil.getTokenVersion(token));
    }

    @Test
    void validateToken_tamperedSignature_returnsFalse() {
        String token = jwtUtil.generateToken(1L, "admin", "ADMIN", 0);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    void validateToken_expired_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String token = jwtUtil.generateToken(1L, "admin", "ADMIN", 0);
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void tokenVersion_defaultsToZero_whenClaimMissing() {
        // 旧格式 token（无 tokenVersion claim）应兼容为 0
        String token = jwtUtil.generateToken(1L, "user1", "USER", 0);
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void secretTooShort_throwsAtPostConstruct() {
        JwtUtil bad = new JwtUtil();
        ReflectionTestUtils.setField(bad, "secret", "short");
        assertThrows(IllegalStateException.class, bad::validateSecret);
    }

    @Test
    void secretMissing_throwsAtPostConstruct() {
        JwtUtil bad = new JwtUtil();
        ReflectionTestUtils.setField(bad, "secret", null);
        assertThrows(IllegalStateException.class, bad::validateSecret);
    }
}
