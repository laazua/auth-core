package com.authcore.dto.module;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * 更新模块请求体（不含 code）。
 */
public record ModuleUpdateDTO(
        @Size(max = 64, message = "name 长度不得超过 64")
        String name,

        @Size(max = 255, message = "base_url 长度不得超过 255")
        @JsonProperty("base_url")
        String baseUrl,

        @Size(max = 255, message = "description 长度不得超过 255")
        String description,

        Integer status
) {
}