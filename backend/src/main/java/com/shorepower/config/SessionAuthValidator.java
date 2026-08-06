package com.shorepower.config;

import com.shorepower.entity.SysUser;
import com.shorepower.mapper.SysUserMapper;
import com.shorepower.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * WebSocket 会话认证校验（与 HTTP JwtAuthFilter 对齐）。
 *
 * 校验链：Bearer 格式 → JWT 签名/过期 → 用户存在 → 账号启用 → token_version 匹配。
 * 角色一律以数据库当前值为准（不信任 token 中的 role claim），
 * 保证禁用/改角色/改密码后 WS 与 HTTP 行为一致。
 *
 * 校验失败抛 IllegalArgumentException，消息即拒绝原因（前端 ERROR 帧可读）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAuthValidator {

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;
    private final CacheManager cacheManager;

    /** 与 JwtAuthFilter/UserService 共用的认证缓存 */
    private static final String USER_CACHE = "userAuth";

    /**
     * 校验 STOMP CONNECT 的 Authorization 头，返回数据库中的最新用户。
     *
     * @param authHeader 形如 "Bearer {jwt}"
     * @return 校验通过的用户（含最新角色/token_version）
     * @throws IllegalArgumentException 拒绝原因（未提供Token/无效/过期/禁用/版本不匹配/用户不存在）
     */
    public SysUser authenticate(String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("未提供有效的认证Token");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("Token无效或已过期");
        }
        Long userId = jwtUtil.getUserId(token);
        SysUser user = loadUser(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        int tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        if (jwtUtil.getTokenVersion(token) != tokenVersion) {
            throw new IllegalArgumentException("Token已失效，请重新登录");
        }
        return user;
    }

    /** 带短缓存加载用户；禁用/改角色/改密码后由 UserService 清缓存，最快 5 分钟内一致 */
    private SysUser loadUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Token无效或已过期");
        }
        Cache cache = cacheManager.getCache(USER_CACHE);
        if (cache != null) {
            SysUser cached = cache.get(userId, SysUser.class);
            if (cached != null) return cached;
        }
        SysUser user = userMapper.selectById(userId);
        if (user != null && cache != null) {
            cache.put(userId, user);
        }
        return user;
    }
}
