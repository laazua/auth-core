package com.authcore.service;

import com.authcore.dto.auth.MeResponse;
import com.authcore.dto.auth.RoleVO;
import com.authcore.dto.auth.UserVO;
import com.authcore.entity.SysPermission;
import com.authcore.entity.SysRole;
import com.authcore.entity.SysUser;
import com.authcore.entity.SysUserRole;
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.mapper.SysRoleMapper;
import com.authcore.mapper.SysUserMapper;
import com.authcore.mapper.SysUserRoleMapper;
import com.authcore.mapper.SysRolePermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证业务服务：提供当前用户信息聚合查询（auth/004）。
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    public AuthService(SysUserMapper userMapper,
                       SysUserRoleMapper userRoleMapper,
                       SysRoleMapper roleMapper,
                       SysRolePermissionMapper rolePermissionMapper,
                       SysPermissionMapper permissionMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    /**
     * 获取当前用户完整信息：用户基本信息 + 角色列表 + 权限码集合（去重）。
     *
     * @param userId 用户 ID
     * @return MeResponse 聚合响应
     */
    public MeResponse getCurrentUserInfo(Long userId) {
        // 1. 查用户
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }

        // 2. 查用户关联的角色 IDs
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return new MeResponse(
                    toUserVO(user),
                    List.of(),
                    List.of()
            );
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .toList();

        // 3. 查角色详情
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        List<RoleVO> roleVOs = roles.stream()
                .map(this::toRoleVO)
                .toList();

        // 4. 查角色关联的权限 IDs
        List<com.authcore.entity.SysRolePermission> rolePermissions = rolePermissionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.authcore.entity.SysRolePermission>()
                        .in(com.authcore.entity.SysRolePermission::getRoleId, roleIds)
        );
        if (rolePermissions.isEmpty()) {
            return new MeResponse(
                    toUserVO(user),
                    roleVOs,
                    List.of()
            );
        }

        List<Long> permissionIds = rolePermissions.stream()
                .map(com.authcore.entity.SysRolePermission::getPermissionId)
                .distinct()
                .toList();

        // 5. 查权限详情并提取权限码（去重）
        List<SysPermission> permissions = permissionMapper.selectBatchIds(permissionIds);
        List<String> permissionCodes = permissions.stream()
                .map(SysPermission::getCode)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new java.util.TreeSet<>(String::compareTo)),
                        ArrayList::new
                ));

        return new MeResponse(
                toUserVO(user),
                roleVOs,
                permissionCodes
        );
    }

    /**
     * 校验用户是否拥有指定权限（auth/005）。
     *
     * @param userId         用户 ID
     * @param permissionCode 权限编码
     * @return true 表示拥有权限，false 表示无权限或用户不存在
     */
    public boolean checkPermission(Long userId, String permissionCode) {
        MeResponse me = getCurrentUserInfo(userId);
        if (me == null) {
            return false;
        }
        return me.permissions().stream()
                .anyMatch(p -> permissionCode.equals(p));
    }

    private UserVO toUserVO(SysUser user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus()
        );
    }

    private RoleVO toRoleVO(SysRole role) {
        return new RoleVO(
                role.getId(),
                role.getName(),
                role.getCode(),
                role.getStatus()
        );
    }
}