package com.authcore.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 用户视图对象（auth/004）。
 */
public record UserVO(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        Integer status,
        @JsonProperty("createTime")
        LocalDateTime createdAt,
        @JsonProperty("updateTime")
        LocalDateTime updatedAt
) {
}