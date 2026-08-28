package com.authcore.dto.role;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新角色请求体（不含 code）。
 */
public record RoleUpdateDTO(
        @Size(max = 64, message = "name 长度不得超过 64")
        String name,

        Integer status
) {
}