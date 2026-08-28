package com.authcore.dto.module;

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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}