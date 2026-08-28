package com.authcore.dto.permission;

import java.time.LocalDateTime;

/**
 * 权限视图对象（perms/001）。
 */
public record PermissionVO(
        Long id,
        Long moduleId,
        String moduleName,
        String name,
        String code,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}