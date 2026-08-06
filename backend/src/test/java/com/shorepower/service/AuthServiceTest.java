package com.shorepower.service;

import com.shorepower.common.Result;
import com.shorepower.dto.LoginRequest;
import com.shorepower.dto.RegisterRequest;
import com.shorepower.entity.SysUser;
import com.shorepower.mapper.SysUserMapper;
import com.shorepower.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试：登录校验顺序、禁用账号、密码错误、token 生成。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private SysUserMapper userMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, jwtUtil, passwordEncoder);
    }

    private SysUser user(int status, String role, int tokenVersion) {
        SysUser u = new SysUser();
        u.setId(1L);
        u.setUsername("admin");
        u.setPassword("$2b$12$hash");
        u.setRole(role);
        u.setStatus(status);
        u.setTokenVersion(tokenVersion);
        return u;
    }

    @Test
    void login_success_generatesTokenWithVersion() {
        SysUser u = user(1, "ADMIN", 5);
        when(userMapper.selectOne(any())).thenReturn(u);
        when(passwordEncoder.matches("123456", u.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(1L, "admin", "ADMIN", 5)).thenReturn("jwt-token");

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("123456");
        Result<?> result = authService.login(req);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        verify(jwtUtil).generateToken(1L, "admin", "ADMIN", 5);
    }

    @Test
    void login_disabledAccount_rejected() {
        SysUser u = user(0, "ADMIN", 0);
        when(userMapper.selectOne(any())).thenReturn(u);

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("123456");

        Result<?> result = authService.login(req);
        assertEquals(500, result.getCode());
        verify(jwtUtil, never()).generateToken(any(), any(), any(), anyInt());
    }

    @Test
    void login_wrongPassword_rejectedWithoutGeneratingToken() {
        SysUser u = user(1, "ADMIN", 0);
        when(userMapper.selectOne(any())).thenReturn(u);
        when(passwordEncoder.matches("wrong", u.getPassword())).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrong");

        Result<?> result = authService.login(req);
        assertEquals(500, result.getCode());
        verify(jwtUtil, never()).generateToken(any(), any(), any(), anyInt());
    }

    @Test
    void login_rateLimited_afterFiveFailures() {
        when(userMapper.selectOne(any())).thenReturn(null); // 用户不存在 → 计数失败

        LoginRequest req = new LoginRequest();
        req.setUsername("attacker");
        req.setPassword("x");
        for (int i = 0; i < 5; i++) {
            authService.login(req);
        }
        // 第 6 次应被限流拒绝（code 429），且不再查库
        Result<?> result = authService.login(req);
        assertEquals(429, result.getCode());
        verify(userMapper, times(5)).selectOne(any());
    }

    @Test
    void register_emptyPhoneAndEmail_normalizedToNull() {
        when(userMapper.selectCount(any())).thenReturn(0L);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("123456");
        req.setRealName("新用户");
        req.setPhone("");
        req.setEmail("");

        authService.register(req);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        SysUser saved = captor.getValue();
        // 空字符串必须归一为 NULL，否则与 V7 唯一索引冲突
        assertNull(saved.getPhone());
        assertNull(saved.getEmail());
        assertEquals(0, saved.getTokenVersion());
    }
}
