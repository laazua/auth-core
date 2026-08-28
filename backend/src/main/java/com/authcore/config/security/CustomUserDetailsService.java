package com.authcore.config.security;

import com.authcore.entity.SysRole;
import com.authcore.entity.SysUser;
import com.authcore.entity.SysUserRole;
import com.authcore.mapper.SysRoleMapper;
import com.authcore.mapper.SysUserMapper;
import com.authcore.mapper.SysUserRoleMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 SysUserMapper 实现 UserDetailsService。
 * 查询 sys_user 表，校验 status=1，加载角色码作为权限。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    /**
     * 构造器注入。
     *
     * @param userMapper      用户数据访问器
     * @param userRoleMapper  用户角色关联数据访问器
     * @param roleMapper      角色数据访问器
     */
    public CustomUserDetailsService(SysUserMapper userMapper,
                                    SysUserRoleMapper userRoleMapper,
                                    SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    /**
     * 根据用户名加载用户详情。
     *
     * @param username 登录名
     * @return UserDetails 实现
     * @throws UsernameNotFoundException 用户不存在或 status=0
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已停用: " + username);
        }

        // 加载用户角色码作为权限（用于 @PreAuthorize hasRole 校验）
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId())
        );

        List<String> roleCodes = List.of();
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .toList();
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            roleCodes = roles.stream()
                    .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                    .map(SysRole::getCode)
                    .toList();
        }

        // 权限码包含角色码（如 ROLE_ADMIN、ROLE_USER），供 hasRole() 使用
        return new CustomUserDetails(user, roleCodes);
    }
}