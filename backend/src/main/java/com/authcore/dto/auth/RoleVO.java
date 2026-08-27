package com.authcore.dto.auth;

/**
 * 角色视图对象（auth/004）。
 */
public record RoleVO(
        Long id,
        String name,
        String code,
        Integer status
) {
}