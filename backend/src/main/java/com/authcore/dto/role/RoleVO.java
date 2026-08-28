package com.authcore.dto.role;

import java.time.LocalDateTime;

/**
 * 角色视图对象（roles/001）。
 */
public record RoleVO(
        Long id,
        String name,
        String code,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}