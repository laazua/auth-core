package com.authcore.dto.role;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 角色分页查询参数。
 */
public record RoleQueryDTO(
        @Min(value = 1, message = "page 必须 ≥ 1")
        Integer page,

        @Min(value = 1, message = "size 必须 ≥ 1")
        @Max(value = 100, message = "size 不得超过 100")
        Integer size,

        String name,

        String code,

        Integer status
) {
    public RoleQueryDTO {
        page = page != null ? page : 1;
        size = size != null ? size : 10;
    }
}