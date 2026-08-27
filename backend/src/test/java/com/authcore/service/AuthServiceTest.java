package com.authcore.service;

import com.authcore.dto.auth.MeResponse;
import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import com.authcore.entity.SysRole;
import com.authcore.entity.SysUser;
import com.authcore.entity.SysUserRole;
import com.authcore.entity.SysRolePermission;
import com.authcore.mapper.SysModuleMapper;
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.mapper.SysRoleMapper;
import com.authcore.mapper.SysUserMapper;
import com.authcore.mapper.SysUserRoleMapper;
import com.authcore.mapper.SysRolePermissionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthService 服务层查询判定用例（auth/004 AC4）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    private static final String IT_PREFIX = "__it_auth004_";

    @Autowired
    private AuthService authService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Autowired
    private SysModuleMapper moduleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    /**
     * AC4: 服务层查询正确性。
     * Given 测试库插入用户、角色、权限、关联数据
     * When service.getCurrentUserInfo(adminId)
     * Then user 不为 null、roles 不为空、permissions 含 "user:view" 等
     */
    @Test
    @DisplayName("服务层正确聚合用户、角色、权限")
    void getCurrentUserInfoAggregatesCorrectly() {
        // Arrange: 创建模块
        SysModule module = new SysModule();
        module.setName(IT_PREFIX + "模块");
        module.setCode(IT_PREFIX + "module");
        module.setBaseUrl("/api/test");
        module.setDescription("测试模块");
        module.setStatus(1);
        moduleMapper.insert(module);

        // Arrange: 创建权限
        SysPermission perm1 = new SysPermission();
        perm1.setModuleId(module.getId());
        perm1.setName("测试查看");
        perm1.setCode("test:view");
        perm1.setDescription("查看测试");
        permissionMapper.insert(perm1);

        SysPermission perm2 = new SysPermission();
        perm2.setModuleId(module.getId());
        perm2.setName("测试新增");
        perm2.setCode("test:create");
        perm2.setDescription("新增测试");
        permissionMapper.insert(perm2);

        // Arrange: 创建角色
        SysRole role = new SysRole();
        role.setName(IT_PREFIX + "角色");
        role.setCode(IT_PREFIX + "ROLE_TEST");
        role.setStatus(1);
        roleMapper.insert(role);

        // Arrange: 角色-权限关联
        SysRolePermission rp1 = new SysRolePermission();
        rp1.setRoleId(role.getId());
        rp1.setPermissionId(perm1.getId());
        rolePermissionMapper.insert(rp1);

        SysRolePermission rp2 = new SysRolePermission();
        rp2.setRoleId(role.getId());
        rp2.setPermissionId(perm2.getId());
        rolePermissionMapper.insert(rp2);

        // Arrange: 创建用户
        SysUser user = new SysUser();
        user.setUsername(IT_PREFIX + "user");
        user.setPassword("$2a$10$testHashValueForTestUser00000000000000000000000");
        user.setNickname("测试用户");
        user.setEmail("test@example.com");
        user.setPhone("13800000001");
        user.setStatus(1);
        userMapper.insert(user);

        // Arrange: 用户-角色关联
        SysUserRole ur = new SysUserRole();
        ur.setUserId(user.getId());
        ur.setRoleId(role.getId());
        userRoleMapper.insert(ur);

        // Act
        MeResponse meResponse = authService.getCurrentUserInfo(user.getId());

        // Assert
        assertNotNull(meResponse, "MeResponse 不应为 null");
        assertNotNull(meResponse.user(), "UserVO 不应为 null");
        assertEquals(user.getId(), meResponse.user().id(), "用户 ID 应匹配");
        assertEquals(user.getUsername(), meResponse.user().username(), "用户名应匹配");
        assertEquals(user.getNickname(), meResponse.user().nickname(), "昵称应匹配");
        assertEquals(user.getEmail(), meResponse.user().email(), "邮箱应匹配");
        assertEquals(user.getPhone(), meResponse.user().phone(), "手机号应匹配");
        assertEquals(user.getStatus(), meResponse.user().status(), "状态应匹配");

        assertNotNull(meResponse.roles(), "角色列表不应为 null");
        assertEquals(1, meResponse.roles().size(), "应包含 1 个角色");
        assertEquals(role.getId(), meResponse.roles().getFirst().id(), "角色 ID 应匹配");
        assertEquals(role.getName(), meResponse.roles().getFirst().name(), "角色名应匹配");
        assertEquals(role.getCode(), meResponse.roles().getFirst().code(), "角色编码应匹配");
        assertEquals(role.getStatus(), meResponse.roles().getFirst().status(), "角色状态应匹配");

        assertNotNull(meResponse.permissions(), "权限列表不应为 null");
        assertEquals(2, meResponse.permissions().size(), "应包含 2 个权限");
        assertTrue(meResponse.permissions().contains("test:view"), "应包含 test:view 权限");
        assertTrue(meResponse.permissions().contains("test:create"), "应包含 test:create 权限");
    }
}