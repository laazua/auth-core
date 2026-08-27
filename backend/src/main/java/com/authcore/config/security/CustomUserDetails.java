package com.authcore.config.security;

import com.authcore.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 自定义 UserDetails 实现，包装 SysUser 与权限码集合。
 */
public class CustomUserDetails implements UserDetails {

    private final SysUser user;
    private final Collection<String> permissionCodes;

    /**
     * 构造器。
     *
     * @param user           系统用户实体
     * @param permissionCodes 权限码集合
     */
    public CustomUserDetails(SysUser user, Collection<String> permissionCodes) {
        this.user = user;
        this.permissionCodes = permissionCodes != null ? permissionCodes : List.of();
    }

    /**
     * @return 权限码转为 SimpleGrantedAuthority 集合
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    /**
     * @return BCrypt 密文
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * @return 登录名
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * 账号是否未过期（恒 true，过期逻辑由 JWT exp 控制）。
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 账号是否未锁定（status=1 视为未锁定）。
     */
    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != null && user.getStatus() == 1;
    }

    /**
     * 凭据是否未过期（恒 true，过期逻辑由 JWT exp 控制）。
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 账号是否启用（status=1）。
     */
    @Override
    public boolean isEnabled() {
        return user.getStatus() != null && user.getStatus() == 1;
    }

    /**
     * @return 底层 SysUser 实体
     */
    public SysUser getUser() {
        return user;
    }

    /**
     * @return 权限码集合
     */
    public Collection<String> getPermissionCodes() {
        return permissionCodes;
    }
}