package com.authcore.dto.user;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 用户角色分配请求体（users/002）。
 */
public record UserRoleAssignDTO(
        @NotNull(message = "roleIds 不能为空")
        List<Long> roleIds
) {
}