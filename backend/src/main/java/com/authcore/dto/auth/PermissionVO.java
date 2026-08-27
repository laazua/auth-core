package com.authcore.dto.auth;

/**
 * 权限视图对象（auth/004）。
 */
public record PermissionVO(
        Long id,
        Long moduleId,
        String name,
        String code,
        String description
) {
}