package com.authcore.dto.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建权限请求体。
 */
public record PermissionCreateDTO(
        @NotBlank(message = "name 不能为空")
        @Size(max = 64, message = "name 长度不得超过 64")
        String name,

        @NotBlank(message = "code 不能为空")
        @Size(max = 64, message = "code 长度不得超过 64")
        @Pattern(regexp = "^[a-z]+:[a-z0-9_:]+$", message = "code 必须符合 模块:操作 格式（小写字母、数字、下划线、冒号）")
        String code,

        @NotNull(message = "module_id 不能为空")
        @JsonProperty("module_id")
        Long moduleId,

        @Size(max = 255, message = "description 长度不得超过 255")
        String description
) {
}