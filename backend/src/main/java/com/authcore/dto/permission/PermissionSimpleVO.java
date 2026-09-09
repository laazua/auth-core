package com.authcore.dto.permission;

import java.util.List;

/**
 * 权限精简视图对象（仅含 id、code、name），用于下拉选项场景。
 */
public record PermissionSimpleVO(
        Long id,
        String code,
        String name
) {
}