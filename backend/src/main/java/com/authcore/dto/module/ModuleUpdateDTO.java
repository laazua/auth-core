package com.authcore.dto.module;

import jakarta.validation.constraints.Size;

/**
 * 更新模块请求体（不含 code）。
 */
public record ModuleUpdateDTO(
        @Size(max = 64, message = "name 长度不得超过 64")
        String name,

        @Size(max = 255, message = "baseUrl 长度不得超过 255")
        String baseUrl,

        @Size(max = 255, message = "description 长度不得超过 255")
        String description,

        Integer status
) {
}