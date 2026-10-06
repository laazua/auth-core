package com.authcore.dto.permission;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        @JsonProperty("createTime")
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}