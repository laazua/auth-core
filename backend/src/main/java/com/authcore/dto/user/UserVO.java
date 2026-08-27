package com.authcore.dto.user;

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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}