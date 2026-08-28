package com.authcore.dto.permission;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 权限分页查询参数。
 */
public record PermissionQueryDTO(
        @Min(value = 1, message = "page 必须 ≥ 1")
        Integer page,

        @Min(value = 1, message = "size 必须 ≥ 1")
        @Max(value = 100, message = "size 不得超过 100")
        Integer size,

        String name,

        String code,

        Long moduleId
) {
    public PermissionQueryDTO {
        page = page != null ? page : 1;
        size = size != null ? size : 10;
    }
}