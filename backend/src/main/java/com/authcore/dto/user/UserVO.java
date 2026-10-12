package com.authcore.dto.user;

import com.authcore.dto.role.RoleVO;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象（users/001；roles 回显字段见 web/046）。
 */
public record UserVO(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        Integer status,
        @JsonProperty("createTime")
        LocalDateTime createdAt,
        @JsonProperty("updateTime")
        LocalDateTime updatedAt,
        List<RoleVO> roles
) {
}