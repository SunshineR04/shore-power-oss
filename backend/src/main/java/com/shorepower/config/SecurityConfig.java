package com.shorepower.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shorepower.common.Result;
import com.shorepower.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;
import java.util.List;

/**
 * Spring Security 安全配置
 *
 * 认证方式：JWT 无状态 Token 认证（不依赖 Session）
 *
 * URL安全策略：
 *   - 公开接口(permitAll)：登录注册(/api/auth/**)、WebSocket握手(/ws/**)、支付回调、Swagger文档
 *   - Admin专用(hasRole)：/api/admin/**
 *   - 其余全部需要认证
 *
 * 过滤器链：
 *   1. CORS 配置（允许跨域凭据）
 *   2. CSRF 禁用（无状态API不需要）
 *   3. Session 无状态（每次请求独立认证）
 *   4. JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前执行
 *      通过 JwtAuthFilter 解析 Authorization: Bearer {token} 并注入 SecurityContext
 *   5. 方法级别 @PreAuthorize 注解（通过 @EnableMethodSecurity 启用）
 *
 * 注意：WebSocket 的 /ws/** 在HTTP层面是公开的，
 * 但 STOMP CONNECT 帧内部会再做一次 JWT 校验（见 WebSocketConfig）。
 * 这是 Spring WebSocket 安全的标准模式——握手建立连接后，在协议层做认证。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS：允许 Localhost 跨域（开发时前端在 3000 端口）
            .cors(cors -> cors.configurationSource(corsSource()))
            // CSRF 禁用：无状态 REST API 不需要 CSRF 防护（没有 Session Cookie）
            .csrf(csrf -> csrf.disable())
            // 无状态：不创建 HttpSession，每次请求独立认证
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth
                    // 公开路径（无需登录即可访问）
                    .requestMatchers("/api/auth/**").permitAll()             // 登录/注册
                    .requestMatchers("/ws/**").permitAll()                   // WebSocket 握手（STOMP CONNECT 阶段再做 JWT 校验）
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll() // 健康检查（无敏感详情）
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();  // CORS 预检
                // Swagger UI 仅 dev 环境公开（见 SecurityConfig 构造函数中的 profile 判断）
                if (List.of(environment.getActiveProfiles()).contains("dev")) {
                    auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                }
                auth
                    // 角色专用路径：仅 ADMIN 可访问
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // 默认拒绝：所有其他请求都需要认证
                    .anyRequest().authenticated();
            })
            // 在 Spring Security 的 UsernamePasswordAuthenticationFilter 之前插入 JWT 过滤器
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // 统一 401/403 返回 JSON（覆盖过滤器链抛出的异常；方法级 @PreAuthorize 仍由 GlobalExceptionHandler 处理）
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, "未登录或Token已失效")));
                })
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(objectMapper.writeValueAsString(Result.fail(403, "权限不足")));
                })
            );
        return http.build();
    }

    /**
     * 密码编码器（BCrypt 加盐哈希，不可逆）
     * 存储密码哈希值，登录时通过 passwordEncoder.matches() 比对
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 配置
     *
     * 因为前端在 localhost:3000（Vite），后端在 localhost:8088，不同端口算跨域。
     * 使用 allowedOriginPatterns 而非 allowedOrigins，因为 allowCredentials=true
     * 时 allowedOrigins("*") 会被浏览器拒绝。
     *
     * 来源白名单通过 cors.allowed-origins 配置项控制：
     *   - dev: http://localhost:3000,http://localhost:5173
     *   - prod: 通过环境变量 CORS_ORIGINS 指定
     */
    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
