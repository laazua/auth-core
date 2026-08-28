package com.authcore.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置密码请求体（管理员专用）。
 */
public record PasswordResetDTO(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 100, message = "新密码长度必须在 8-100 之间")
        String newPassword
) {
}