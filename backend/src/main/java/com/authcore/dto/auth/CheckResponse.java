package com.authcore.dto.auth;

/**
 * 权限校验响应 DTO（auth/005）。
 */
public record CheckResponse(
        boolean hasPermission,
        Long userId,
        String permissionCode
) {
}