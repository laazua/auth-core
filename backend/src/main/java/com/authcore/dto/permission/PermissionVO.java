package com.authcore.dto.permission;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限视图对象（perms/001）。
 *
 * <p>{@code children} 为树形嵌套槽（web/048a）：tree 平铺路径恒为空数组，
 * 既有 8 参构造点经重载默认空列表，对分页/详情消费者零破坏。</p>
 */
public record PermissionVO(
        Long id,
        Long moduleId,
        String moduleName,
        String name,
        String code,
        String description,
        @JsonProperty("createTime")
        LocalDateTime createdAt,
        @JsonProperty("updateTime")
        LocalDateTime updatedAt,
        List<PermissionVO> children
) {
    public PermissionVO(Long id, Long moduleId, String moduleName, String name, String code,
                        String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, moduleId, moduleName, name, code, description, createdAt, updatedAt, List.of());
    }
}