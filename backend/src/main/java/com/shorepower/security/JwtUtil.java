package com.shorepower.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT (JSON Web Token) 工具类
 *
 * 使用 HMAC-SHA 算法签名（对称密钥）
 *
 * Token 结构：
 *   Header: {"alg": "HS256"}
 *   Payload: {
 *     "sub": username,        // 用户名
 *     "userId": 1,            // 用户ID（自定义字段）
 *     "role": "ADMIN",        // 角色（自定义字段）
 *     "iat": 1716000000,      // 签发时间
 *     "exp": 1716086400       // 过期时间（24小时后）
 *   }
 *   Signature: HMAC-SHA256(base64url(header) + "." + base64url(payload), secret)
 *
 * 密钥来源：JWT_SECRET 环境变量或配置文件 jwt.secret
 * 过期时间：24 小时（86400000ms）
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /** 从配置的密钥字符串生成 HMAC-SHA 密钥对象 */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     *
     * 标准字段：
     *   - sub = username（JWT 标准 subject，Spring Security 默认取此值作为用户名）
     *   - iat = 签发时间
     *   - exp = 过期时间（当前时间 + 24小时）
     *
     * 自定义字段：
     *   - userId：用户数据库ID（用于权限判断和业务操作）
     *   - role：角色 ADMIN/OPERATOR/USER（用于 @PreAuthorize 和路由守卫）
     *
     * @param userId 用户ID（自定义 claim）
     * @param username 用户名（作为 subject）
     * @param role 角色，如 ADMIN/OPERATOR/USER
     * @return 签名的 JWT 字符串
     */
    public String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .subject(username)            // JWT 标准字段 sub
                .claim("userId", userId)      // 自定义字段：用户ID
                .claim("role", role)          // 自定义字段：角色
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())           // HMAC-SHA256 对称签名
                .compact();
    }

    /**
     * 解析 Token（包括签名验证）
     * 先通过 getKey() 还原密钥，验证 HMAC 签名完整性
     * 再提取 claims 载荷（含自定义字段）
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 合法性
     * JwtException 的子类包括：ExpiredJwtException（过期）、MalformedJwtException（格式错误）、
     * SignatureException（签名不匹配）等，统一捕获返回 false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /** 从 Token 中提取用户名（subject 字段） */
    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    /** 从 Token 中提取用户ID（自定义 userId 字段） */
    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    /** 从 Token 中提取角色（自定义 role 字段） */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }
}
