package com.authcore.dto.role;

import com.authcore.dto.permission.PermissionVO;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图对象（roles/001）。
 *
 * <p>{@code permissions} 为已分配权限回显槽（web/048b）：详情/列表经
 * {@code sys_role_permission} 批量填充；既有 6 参构造点经重载默认空列表，
 * 对登录/me 等其它域消费者零破坏。</p>
 */
public record RoleVO(
        Long id,
        String name,
        String code,
        Integer status,
        @JsonProperty("createTime")
        LocalDateTime createdAt,
        @JsonProperty("updateTime")
        LocalDateTime updatedAt,
        List<PermissionVO> permissions
) {
    public RoleVO(Long id, String name, String code, Integer status,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, name, code, status, createdAt, updatedAt, List.of());
    }
}
