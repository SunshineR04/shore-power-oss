package com.shorepower.security;

import com.shorepower.entity.SysUser;
import com.shorepower.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * 执行流程：
 *   1. 从请求头中提取 Authorization: Bearer {token}
 *   2. 调用 JwtUtil.validateToken() 校验签名和过期时间
 *   3. 从数据库（带缓存）加载用户，校验：
 *      - 用户存在
 *      - 用户启用（status == 1）
 *      - token 中的 tokenVersion 与数据库一致（修改密码/禁用/改角色后旧 token 失效）
 *   4. 以数据库中的当前角色构造认证信息（不信任 token 中的 role，防止角色变更后旧权限残留）
 *   5. 注入 SecurityContextHolder，供 @PreAuthorize 与 hasRole() 使用
 *
 * 注意：token 无效或不满足上述条件时不设置认证，由 Spring Security 的
 * .anyRequest().authenticated() 自然拦截（返回 401）。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;
    private final CacheManager cacheManager;

    /** 认证状态缓存 key 前缀：userId */
    private static final String USER_CACHE = "userAuth";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserId(token);
                SysUser user = loadUser(userId);
                if (user != null && user.getStatus() != null && user.getStatus() == 1
                        && jwtUtil.getTokenVersion(token) == user.getTokenVersion()) {
                    // 以数据库当前角色为准，避免 token 中角色过期导致越权
                    String role = user.getRole();
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    auth.setDetails(user.getUsername());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }

    /** 带缓存的用户加载；短 TTL 保证禁用/改角色后尽快生效 */
    private SysUser loadUser(Long userId) {
        if (userId == null) return null;
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
