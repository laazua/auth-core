package com.authcore.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建用户请求体。
 */
public record UserCreateDTO(
        @NotBlank(message = "username 不能为空")
        @Size(max = 50, message = "username 长度不得超过 50")
        String username,

        @NotBlank(message = "password 不能为空")
        @Size(min = 8, max = 100, message = "password 长度必须在 8-100 之间")
        String password,

        @Size(max = 50, message = "nickname 长度不得超过 50")
        String nickname,

        @Email(message = "email 格式不正确")
        @Size(max = 100, message = "email 长度不得超过 100")
        String email,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone 格式不正确")
        String phone
) {
}