package com.shorepower.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.shorepower.entity.SysUser;
import com.shorepower.mapper.SysUserMapper;
import com.shorepower.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SessionAuthValidator 单元测试：
 * WS CONNECT 认证与 HTTP JwtAuthFilter 对齐（token 有效/过期/版本不匹配/禁用/用户不存在/角色取 DB）。
 */
@ExtendWith(MockitoExtension.class)
class SessionAuthValidatorTest {

    private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef";

    @Mock private JwtUtil jwtUtil;
    @Mock private SysUserMapper userMapper;

    private SessionAuthValidator validator;

    @BeforeEach
    void setUp() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of("userAuth"));
        cacheManager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).maximumSize(10));
        validator = new SessionAuthValidator(jwtUtil, userMapper, cacheManager);
    }

    private SysUser user(Long id, int status, String role, int tokenVersion) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername("admin");
        u.setStatus(status);
        u.setRole(role);
        u.setTokenVersion(tokenVersion);
        return u;
    }

    @Test
    void authenticate_success_returnsDbUserWithCurrentRole() {
        when(jwtUtil.validateToken("good-token")).thenReturn(true);
        when(jwtUtil.getUserId("good-token")).thenReturn(1L);
        when(jwtUtil.getTokenVersion("good-token")).thenReturn(2);
        when(userMapper.selectById(1L)).thenReturn(user(1L, 1, "ADMIN", 2));

        SysUser result = validator.authenticate("Bearer good-token");

        assertEquals("ADMIN", result.getRole());
        assertEquals(2, result.getTokenVersion());
    }

    @Test
    void authenticate_missingHeader_rejected() {
        assertThrows(IllegalArgumentException.class, () -> validator.authenticate(null));
        assertThrows(IllegalArgumentException.class, () -> validator.authenticate("Basic abc"));
    }

    @Test
    void authenticate_invalidToken_rejected() {
        when(jwtUtil.validateToken("bad")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> validator.authenticate("Bearer bad"));
    }

    @Test
    void authenticate_userMissing_rejected() {
        when(jwtUtil.validateToken("t")).thenReturn(true);
        when(jwtUtil.getUserId("t")).thenReturn(99L);
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> validator.authenticate("Bearer t"));
    }

    @Test
    void authenticate_disabledUser_rejected() {
        when(jwtUtil.validateToken("t")).thenReturn(true);
        when(jwtUtil.getUserId("t")).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(user(1L, 0, "ADMIN", 0));
        assertThrows(IllegalArgumentException.class, () -> validator.authenticate("Bearer t"));
    }

    @Test
    void authenticate_staleTokenVersion_rejected() {
        when(jwtUtil.validateToken("t")).thenReturn(true);
        when(jwtUtil.getUserId("t")).thenReturn(1L);
        when(jwtUtil.getTokenVersion("t")).thenReturn(1); // token 旧版本
        when(userMapper.selectById(1L)).thenReturn(user(1L, 1, "ADMIN", 2)); // DB 已递增
        assertThrows(IllegalArgumentException.class, () -> validator.authenticate("Bearer t"));
    }
}
