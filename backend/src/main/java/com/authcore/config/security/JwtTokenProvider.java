package com.authcore.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * JWT 生成/校验/解析组件（auth/002）。
 * <p>
 * - 使用 HS256 算法
 * - payload 包含：sub(username)、uid(userId)、exp(过期时间)、iat(签发时间)
 * - secret 仅从环境变量注入（JwtProperties）
 * </p>
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expireMillis;

    /**
     * 构造器注入 JwtProperties。
     *
     * @param jwtProperties JWT 配置属性
     */
    public JwtTokenProvider(JwtProperties jwtProperties) {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret 未配置，请设置环境变量");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = (long) jwtProperties.getExpireHours() * 3600 * 1000;
    }

    /**
     * 生成 JWT。
     *
     * @param userDetails 用户详情（需包含 username 与 id）
     * @return JWT 字符串
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMillis);

        String username = userDetails.getUsername();
        Long userId = null;
        if (userDetails instanceof CustomUserDetails custom) {
            userId = custom.getUser().getId();
        }

        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 校验 JWT 是否有效（签名正确且未过期）。
     *
     * @param token JWT 字符串
     * @return true=有效，false=无效或过期
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 过期视为无效
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            // 签名错误、格式错误等
            return false;
        }
    }

    /**
     * 从 JWT 中解析用户名。
     *
     * @param token JWT 字符串
     * @return subject(username)
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    /**
     * 解析 JWT Claims。
     *
     * @param token JWT 字符串
     * @return Claims
     * @throws JwtException 解析失败（签名错误、过期、格式错误等）
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}