package com.authcore.dto.module;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建模块请求体。
 */
public record ModuleCreateDTO(
        @NotBlank(message = "name 不能为空")
        @Size(max = 64, message = "name 长度不得超过 64")
        String name,

        @NotBlank(message = "code 不能为空")
        @Size(max = 64, message = "code 长度不得超过 64")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "code 必须为大写字母开头，仅含大写字母、数字、下划线")
        String code,

        @Size(max = 255, message = "baseUrl 长度不得超过 255")
        String baseUrl,

        @Size(max = 255, message = "description 长度不得超过 255")
        String description,

        Integer status
) {
}