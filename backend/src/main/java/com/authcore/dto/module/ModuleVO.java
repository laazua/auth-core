package com.authcore.dto.module;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 模块视图对象（modules/001）。
 */
public record ModuleVO(
        Long id,
        String name,
        String code,
        String baseUrl,
        String description,
        Integer status,
        @JsonProperty("createTime")
        LocalDateTime createdAt,
        @JsonProperty("updateTime")
        LocalDateTime updatedAt
) {
}