package com.authcore.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新用户请求体（不含 username 与 password）。
 */
public record UserUpdateDTO(
        @Size(max = 50, message = "nickname 长度不得超过 50")
        String nickname,

        @Email(message = "email 格式不正确")
        @Size(max = 100, message = "email 长度不得超过 100")
        String email,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone 格式不正确")
        String phone
) {
}