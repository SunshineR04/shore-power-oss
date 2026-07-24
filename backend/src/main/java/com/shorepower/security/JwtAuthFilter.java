package com.shorepower.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
 * 继承 OncePerRequestFilter 保证每个请求只过滤一次
 * 在 UsernamePasswordAuthenticationFilter 之前执行
 *
 * 执行流程：
 *   1. 从请求头中提取 Authorization: Bearer {token}
 *   2. 调用 JwtUtil.validateToken() 校验签名和过期时间
 *   3. 验证通过后，从 Token 中提取 userId、username、role
 *   4. 构造 UsernamePasswordAuthenticationToken 注入 SecurityContextHolder
 *   5. Spring Security 后续的 @PreAuthorize 和 .hasRole() 均基于此认证信息
 *
 * 注意：token 无效或过期时不返回 401，而是跳过认证设置，
 * 由后续 Spring Security 的授权检查自然拦截（返回 403）
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 从 HTTP 请求头提取 Authorization 字段
        String header = request.getHeader("Authorization");
        // 检查请求头存在且格式为 "Bearer {token}"
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            // 去掉 "Bearer " 前缀（7个字符），得到原始 token 字符串
            String token = header.substring(7);
            // 校验 JWT 签名和过期时间
            if (jwtUtil.validateToken(token)) {
                // 提取 Token 中的用户信息
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);
                Long userId = jwtUtil.getUserId(token);

                // 构造 Spring Security 认证对象
                // principal = userId（Long类型，Controller 中 auth.getPrincipal() 直接获取）
                // credentials = null（密码已不需要，JWT 证明身份）
                // authorities = ROLE_{role}（匹配 SecurityConfig 中的 hasRole() 和 @PreAuthorize）
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                // details 存入 username，供自定义逻辑使用
                auth.setDetails(username);
                // 注入 SecurityContext，Spring Security 判断为已认证用户
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            // Token 无效时什么也不做——不设认证，后续请求被 .anyRequest().authenticated() 拦截
        }
        // 继续执行过滤器链（无论是否认证通过，都放行到下一个过滤器或 Controller）
        chain.doFilter(request, response);
    }
}
