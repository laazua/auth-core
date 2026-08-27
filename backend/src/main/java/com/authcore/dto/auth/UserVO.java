package com.authcore.dto.auth;

/**
 * 用户视图对象（auth/004）。
 */
public record UserVO(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        Integer status
) {
}