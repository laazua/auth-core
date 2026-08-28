package com.authcore.dto.role;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色权限分配请求体（roles/002）。
 */
public record RolePermissionAssignDTO(
        @NotNull(message = "permissionIds 不能为空")
        List<Long> permissionIds
) {
}