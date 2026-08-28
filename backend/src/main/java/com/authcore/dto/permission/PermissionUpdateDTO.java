package com.authcore.dto.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新权限请求体（不含 code）。
 */
public record PermissionUpdateDTO(
        @Size(max = 64, message = "name 长度不得超过 64")
        String name,

        @Size(max = 255, message = "description 长度不得超过 255")
        String description,

        @NotNull(message = "module_id 不能为空")
        @JsonProperty("module_id")
        Long moduleId
) {
}