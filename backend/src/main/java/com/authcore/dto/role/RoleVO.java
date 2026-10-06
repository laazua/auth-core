package com.authcore.dto.role;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 角色视图对象（roles/001）。
 */
public record RoleVO(
        Long id,
        String name,
        String code,
        Integer status,
        @JsonProperty("createTime")
        LocalDateTime createdAt,
        @JsonProperty("updateTime")
        LocalDateTime updatedAt
) {
}