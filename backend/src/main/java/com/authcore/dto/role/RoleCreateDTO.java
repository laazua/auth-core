package com.authcore.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建角色请求体。
 */
public record RoleCreateDTO(
        @NotBlank(message = "name 不能为空")
        @Size(max = 64, message = "name 长度不得超过 64")
        String name,

        @NotBlank(message = "code 不能为空")
        @Size(max = 64, message = "code 长度不得超过 64")
        @Pattern(regexp = "^ROLE_[A-Z0-9_]+$", message = "code 必须以 ROLE_ 开头且仅含大写字母、数字、下划线")
        String code,

        Integer status
) {
    public RoleCreateDTO {
        status = status != null ? status : 1;
    }
}