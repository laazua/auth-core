package com.authcore.config.security;

import com.authcore.entity.SysUser;
import com.authcore.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CustomUserDetailsService 用户查询与 status 校验判定用例（auth/001 AC4）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomUserDetailsServiceTest {

    private static final String IT_PREFIX = "__it_auth001_";

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private SysUserMapper userMapper;

    /**
     * AC4: status=1 用户可查询，status=0 抛 UsernameNotFoundException。
     * Given 数据库插入 status=1 与 status=0 用户
     * When loadUserByUsername("enabled_user") / loadUserByUsername("disabled_user")
     * Then 前者返回 UserDetails、后者抛 UsernameNotFoundException
     */
    @Test
    @DisplayName("status=1 用户返回 UserDetails，status=0 抛 UsernameNotFoundException")
    void loadUserByUsernameStatusCheck() {
        // Arrange: 插入启用用户
        SysUser enabledUser = new SysUser();
        enabledUser.setUsername(IT_PREFIX + "enabled");
        enabledUser.setPassword("$2a$10$testHashValueForEnabledUser00000000000000000000000");
        enabledUser.setNickname("启用用户");
        enabledUser.setStatus(1);
        userMapper.insert(enabledUser);

        // Arrange: 插入停用用户
        SysUser disabledUser = new SysUser();
        disabledUser.setUsername(IT_PREFIX + "disabled");
        disabledUser.setPassword("$2a$10$testHashValueForDisabledUser00000000000000000000000");
        disabledUser.setNickname("停用用户");
        disabledUser.setStatus(0);
        userMapper.insert(disabledUser);

        // Act & Assert: 启用用户应返回 UserDetails
        UserDetails enabledDetails = userDetailsService.loadUserByUsername(IT_PREFIX + "enabled");
        assertNotNull(enabledDetails, "启用用户应返回 UserDetails");
        assertEquals(IT_PREFIX + "enabled", enabledDetails.getUsername(), "用户名应匹配");

        // Act & Assert: 停用用户应抛 UsernameNotFoundException
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(IT_PREFIX + "disabled"),
                "停用用户应抛 UsernameNotFoundException");

        // Cleanup: 测试用户名不存在时也应抛异常
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(IT_PREFIX + "nonexistent"),
                "不存在用户应抛 UsernameNotFoundException");
    }
}