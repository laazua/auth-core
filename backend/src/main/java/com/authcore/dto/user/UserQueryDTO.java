package com.authcore.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 用户分页查询参数。
 */
public record UserQueryDTO(
        @Min(value = 1, message = "page 必须 ≥ 1")
        Integer page,

        @Min(value = 1, message = "size 必须 ≥ 1")
        @Max(value = 100, message = "size 不得超过 100")
        Integer size,

        String username,

        String nickname,

        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "email 格式不正确")
        String email,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone 格式不正确")
        String phone,

        Integer status
) {
    public UserQueryDTO {
        page = page != null ? page : 1;
        size = size != null ? size : 10;
    }
}