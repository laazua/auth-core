package com.authcore.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V6 种子数据迁移集成判定用例（model/006 AC1-4）。
 *
 * <p>读路径只读断言，不残留测试数据；Flyway 重放幂等性在独立用例中验证。
 */
@ActiveProfiles("test")
class SeedDataIntegrationTest extends AbstractModelIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    /**
     * AC1: V6 迁移首次应用成功且重放幂等。
     */
    @Test
    @DisplayName("迁移_V6首次应用成功且重放幂等")
    void test_迁移_V6首次应用成功且重放幂等() {
        assertEquals(1, countFlywaySuccess("6"), "V6 应已成功应用一次");

        flyway.migrate();

        assertEquals(1, countFlywaySuccess("6"), "重放后不得产生重复版本记录");
    }

    /**
     * AC2: 核心实体数据存在且字段正确。
     */
    @Test
    @DisplayName("核心实体数据存在且字段正确")
    void test_核心实体数据存在且字段正确() {
        // sys_user: admin 用户（种子数据）
        Integer adminCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username = 'admin'", Integer.class);
        assertEquals(1, adminCount, "sys_user 应含 1 行 admin 种子数据");

        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM sys_user WHERE username = 'admin'", String.class);
        assertEquals("admin", username, "用户名应为 admin");

        String password = jdbcTemplate.queryForObject(
                "SELECT password FROM sys_user WHERE username = 'admin'", String.class);
        assertNotNull(password, "密码哈希不应为空");
        assertTrue(password.startsWith("$2a$10$"), "密码应为 BCrypt 哈希");

        String nickname = jdbcTemplate.queryForObject(
                "SELECT nickname FROM sys_user WHERE username = 'admin'", String.class);
        assertEquals("管理员", nickname, "昵称应为 管理员");

        String email = jdbcTemplate.queryForObject(
                "SELECT email FROM sys_user WHERE username = 'admin'", String.class);
        assertEquals("admin@example.com", email, "邮箱应为 admin@example.com");

        String phone = jdbcTemplate.queryForObject(
                "SELECT phone FROM sys_user WHERE username = 'admin'", String.class);
        assertEquals("13800000000", phone, "手机号应为 13800000000");

        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM sys_user WHERE username = 'admin'", Integer.class);
        assertEquals(1, status, "status 应为 1(启用)");

        // sys_module: 4 行
        Integer moduleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_module", Integer.class);
        assertEquals(4, moduleCount, "sys_module 应含 4 行种子数据");

        // 验证模块 code 唯一且符合预期
        Integer userMgmtCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_module WHERE code = 'user_mgmt'", Integer.class);
        assertEquals(1, userMgmtCount, "user_mgmt 模块应存在");

        Integer roleMgmtCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_module WHERE code = 'role_mgmt'", Integer.class);
        assertEquals(1, roleMgmtCount, "role_mgmt 模块应存在");

        Integer permMgmtCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_module WHERE code = 'perm_mgmt'", Integer.class);
        assertEquals(1, permMgmtCount, "perm_mgmt 模块应存在");

        Integer moduleMgmtCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_module WHERE code = 'module_mgmt'", Integer.class);
        assertEquals(1, moduleMgmtCount, "module_mgmt 模块应存在");

        // sys_role: 2 行
        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_role", Integer.class);
        assertEquals(2, roleCount, "sys_role 应含 2 行种子数据");

        Integer adminRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE code = 'ROLE_ADMIN'", Integer.class);
        assertEquals(1, adminRoleCount, "ROLE_ADMIN 角色应存在");

        Integer userRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role WHERE code = 'ROLE_USER'", Integer.class);
        assertEquals(1, userRoleCount, "ROLE_USER 角色应存在");

        // sys_permission: 16 行（4 模块 × 4 权限）
        Integer permCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_permission", Integer.class);
        assertEquals(16, permCount, "sys_permission 应含 16 行种子数据");

        // 验证每个权限的 module_id 指向有效模块
        Integer orphanPermCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_permission p LEFT JOIN sys_module m ON p.module_id = m.id WHERE m.id IS NULL",
                Integer.class);
        assertEquals(0, orphanPermCount, "所有权限的 module_id 必须指向有效模块");
    }

    /**
     * AC3: 关联数据正确。
     */
    @Test
    @DisplayName("关联数据正确")
    void test_关联数据正确() {
        // sys_user_role: 1 行 (admin_user_id, admin_role_id)
        Integer userRoleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user_role", Integer.class);
        assertEquals(1, userRoleCount, "sys_user_role 应恰含 1 行关联数据");

        // 验证关联正确：admin 用户 -> admin 角色
        Integer adminUserRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role ur " +
                "JOIN sys_user u ON ur.user_id = u.id " +
                "JOIN sys_role r ON ur.role_id = r.id " +
                "WHERE u.username = 'admin' AND r.code = 'ROLE_ADMIN'",
                Integer.class);
        assertEquals(1, adminUserRoleCount, "admin 用户应关联 ROLE_ADMIN 角色");

        // sys_role_permission: admin_role -> 16 个权限，user_role -> 4 个只读权限
        Integer adminRolePermCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_permission rp " +
                "JOIN sys_role r ON rp.role_id = r.id " +
                "WHERE r.code = 'ROLE_ADMIN'",
                Integer.class);
        assertEquals(16, adminRolePermCount, "ROLE_ADMIN 应关联全部 16 个权限");

        Integer userRolePermCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_permission rp " +
                "JOIN sys_role r ON rp.role_id = r.id " +
                "WHERE r.code = 'ROLE_USER'",
                Integer.class);
        assertEquals(4, userRolePermCount, "ROLE_USER 应关联 4 个只读权限");

        // 验证 ROLE_USER 只关联 view 类权限
        Integer userRoleViewPermCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_permission rp " +
                "JOIN sys_role r ON rp.role_id = r.id " +
                "JOIN sys_permission p ON rp.permission_id = p.id " +
                "WHERE r.code = 'ROLE_USER' AND p.code LIKE '%:view'",
                Integer.class);
        assertEquals(4, userRoleViewPermCount, "ROLE_USER 的 4 个权限应全为 view 类型");
    }

    /**
     * AC4: admin 密码为有效 BCrypt，可通过 matches 验证。
     */
    @Test
    @DisplayName("admin密码为有效BCrypt")
    void test_admin密码为有效BCrypt() {
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password FROM sys_user WHERE username = 'admin'", String.class);
        assertNotNull(storedHash, "admin 密码哈希不应为空");

        boolean matches = passwordEncoder.matches("admin123456", storedHash);
        assertTrue(matches, "BCryptPasswordEncoder.matches('admin123456', storedHash) 应返回 true");
    }
}