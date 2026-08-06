package com.shorepower.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.shorepower.entity.SysUser;
import com.shorepower.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * UserService 单元测试：token_version 递增规则（角色未变不递增、角色变更递增、改密码递增）。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private SysUserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of("userAuth"));
        cacheManager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).maximumSize(10));
        userService = new UserService(userMapper, passwordEncoder, cacheManager);
    }

    private SysUser user(Long id, String role, int tokenVersion) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setRole(role);
        u.setTokenVersion(tokenVersion);
        u.setPassword("$2b$12$hash");
        u.setStatus(1);
        return u;
    }

    @Test
    void update_roleUnchanged_doesNotIncrementTokenVersion() {
        SysUser existing = user(1L, "USER", 2);
        when(userMapper.selectById(1L)).thenReturn(existing);

        SysUser patch = new SysUser();
        patch.setId(1L);
        patch.setRealName("新名字");
        patch.setRole("USER"); // 角色未变

        userService.update(patch);

        // 未发生认证相关变更：tokenVersion 保持 null（不设置），即不会被递增
        assertNull(patch.getTokenVersion());
    }

    @Test
    void update_roleChanged_incrementsTokenVersion() {
        SysUser existing = user(1L, "USER", 2);
        when(userMapper.selectById(1L)).thenReturn(existing);

        SysUser patch = new SysUser();
        patch.setId(1L);
        patch.setRole("ADMIN"); // 角色变更

        userService.update(patch);

        assertEquals(3, patch.getTokenVersion());
    }

    @Test
    void update_passwordChange_incrementsTokenVersion() {
        SysUser existing = user(1L, "USER", 2);
        when(userMapper.selectById(1L)).thenReturn(existing);

        SysUser patch = new SysUser();
        patch.setId(1L);
        patch.setPassword("newpass1"); // 仅改密码

        userService.update(patch);

        assertEquals(3, patch.getTokenVersion());
    }

    @Test
    void changePassword_incrementsTokenVersion() {
        SysUser existing = user(1L, "USER", 1);
        existing.setPassword("$2b$12$oldhash");
        when(userMapper.selectById(1L)).thenReturn(existing);
        when(passwordEncoder.matches("old", existing.getPassword())).thenReturn(true);

        userService.changePassword(1L, "old", "newpass1");

        assertEquals(2, existing.getTokenVersion());
    }
}
