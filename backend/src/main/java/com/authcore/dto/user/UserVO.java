package com.authcore.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 用户视图对象（users/001）。
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