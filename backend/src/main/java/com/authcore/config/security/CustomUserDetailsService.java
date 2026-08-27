package com.authcore.config.security;

import com.authcore.entity.SysUser;
import com.authcore.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于 SysUserMapper 实现 UserDetailsService。
 * 查询 sys_user 表，校验 status=1，当前权限码集合留空（后续 sprint 完善）。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;

    /**
     * 构造器注入 SysUserMapper。
     *
     * @param userMapper 用户数据访问器
     */
    public CustomUserDetailsService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
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

        // 当前 sprint 权限码集合留空，auth/003 完善
        return new CustomUserDetails(user, List.of());
    }
}