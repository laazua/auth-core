package com.authcore.dto.permission;

/**
 * 模块视图对象（用于分组查询，perms/001）。
 */
public record ModuleVO(
        Long id,
        String name,
        String code
) {
}