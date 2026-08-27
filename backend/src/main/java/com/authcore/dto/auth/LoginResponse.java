package com.authcore.dto.auth;

/**
 * 登录响应 DTO。
 */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn
) {
}