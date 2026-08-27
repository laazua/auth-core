package com.authcore.dto.user;

import jakarta.validation.constraints.NotNull;

/**
 * 用户状态变更请求体。
 */
public record UserStatusDTO(
        @NotNull(message = "status 不能为空")
        Integer status
) {
}