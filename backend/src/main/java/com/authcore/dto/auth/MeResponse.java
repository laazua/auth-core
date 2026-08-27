package com.authcore.dto.auth;

/**
 * 当前用户完整信息响应（auth/004）。
 */
public record MeResponse(
        UserVO user,
        java.util.List<RoleVO> roles,
        java.util.List<String> permissions
) {
}