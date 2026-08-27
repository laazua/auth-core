package com.authcore.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 权限校验请求 DTO（auth/005）。
 */
public record CheckRequest(
        @NotNull(message = "用户 ID 不能为空")
        Long userId,

        @NotBlank(message = "权限编码不能为空")
        String permissionCode
) {
}