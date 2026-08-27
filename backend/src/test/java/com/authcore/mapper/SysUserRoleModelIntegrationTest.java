package com.authcore.mapper;

import com.authcore.entity.SysRolePermission;
import com.authcore.entity.SysUserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * sys_user_role / sys_role_permission 数据层集成判定用例，作用于共享开发库 192.168.165.88/authcore（model/005 AC1-4）。
 *
 * <p>写路径全部 @Transactional 回滚隔离，不在库中残留测试数据；读路径只读。
 */
class SysUserRoleModelIntegrationTest extends AbstractModelIntegrationTest {

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    /**
     * 构造可辨识前缀的测试用户-角色关联。
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     * @return 未持久化的实体
     */
    private SysUserRole newUserRole(Long userId, Long roleId) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    /**
     * 构造可辨识前缀的角色-权限关联。
     *
     * @param roleId       角色 ID
     * @param permissionId 权限 ID
     * @return 未持久化的实体
     */
    private SysRolePermission newRolePermission(Long roleId, Long permissionId) {
        SysRolePermission rp = new SysRolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        return rp;
    }

    // ==================== sys_user_role ====================

    /**
     * Given 空缺 V5 When 上下文启动并重放 migrate Then 历史表恰有一条 version=5 成功记录且重放不新增。
     */
    @Test
    @DisplayName("V5 迁移首次应用成功且重放幂等")
    void test_迁移_V5首次应用成功且重放幂等() {
        assertEquals(1, countFlywaySuccess("5"), "V5 应已成功应用一次");

        flyway.migrate();

        assertEquals(1, countFlywaySuccess("5"), "重放后不得产生重复版本记录");
    }

    /**
     * Given V5 已应用 When 查询 information_schema Then sys_user_role 三契约列齐备、NOT NULL 分组含 user_id/role_id、联合唯一索引存在。
     */
    @Test
    @DisplayName("sys_user_role 表结构符合 §3 契约")
    void test_表结构_sys_user_role契约列与约束齐备() {
        int notNullColumns = countColumnsWithNullability("sys_user_role", "NO",
                "id", "user_id", "role_id", "created_at", "updated_at");
        assertEquals(5, notNullColumns, "五列必须为 NOT NULL");
        assertEquals(1, countUniqueIndexes("sys_user_role", "uk_sys_user_role"),
                "user_id+role_id 必须存在联合唯一索引");
    }

    /**
     * Given 合法用户-角色 When 增查改删 Then 主线可用且 createdAt/updatedAt 自动填充非空。
     */
    @Test
    @Transactional
    @DisplayName("增查改删主线 sys_user_role 与时间自动填充")
    void test_增查改删主线_sys_user_role与时间自动填充() {
        SysUserRole created = newUserRole(1L, 1L);
        userRoleMapper.insert(created);
        assertNotNull(created.getId(), "插入后应回填自增主键");

        SysUserRole fetched = userRoleMapper.selectById(created.getId());
        assertEquals(created.getUserId(), fetched.getUserId());
        assertEquals(created.getRoleId(), fetched.getRoleId());
        assertNotNull(fetched.getCreatedAt(), "createdAt 应被自动填充");
        assertNotNull(fetched.getUpdatedAt(), "updatedAt 应被自动填充");

        fetched.setRoleId(2L);
        userRoleMapper.updateById(fetched);
        assertEquals(2L, userRoleMapper.selectById(created.getId()).getRoleId());

        userRoleMapper.deleteById(created.getId());
        assertNull(userRoleMapper.selectById(created.getId()), "物理删除后不应可查");
    }

    /**
     * Given 同事务插入重复 (user_id, role_id) When 再次插入 Then 数据库联合唯一索引触发 DuplicateKeyException。
     */
    @Test
    @Transactional
    @DisplayName("重复 userId_roleId 触发唯一约束异常")
    void test_重复userId_roleId_触发唯一约束异常() {
        userRoleMapper.insert(newUserRole(100L, 100L));
        assertThrows(DuplicateKeyException.class, () -> userRoleMapper.insert(newUserRole(100L, 100L)));
    }

    // ==================== sys_role_permission ====================

    /**
     * Given V5 已应用 When 查询 information_schema Then sys_role_permission 三契约列齐备、NOT NULL 分组含 role_id/permission_id、联合唯一索引存在。
     */
    @Test
    @DisplayName("sys_role_permission 表结构符合 §3 契约")
    void test_表结构_sys_role_permission契约列与约束齐备() {
        int notNullColumns = countColumnsWithNullability("sys_role_permission", "NO",
                "id", "role_id", "permission_id", "created_at", "updated_at");
        assertEquals(5, notNullColumns, "五列必须为 NOT NULL");
        assertEquals(1, countUniqueIndexes("sys_role_permission", "uk_sys_role_permission"),
                "role_id+permission_id 必须存在联合唯一索引");
    }

    /**
     * Given 合法角色-权限 When 增查改删 Then 主线可用且 createdAt/updatedAt 自动填充非空。
     */
    @Test
    @Transactional
    @DisplayName("增查改删主线 sys_role_permission 与时间自动填充")
    void test_增查改删主线_sys_role_permission与时间自动填充() {
        SysRolePermission created = newRolePermission(1L, 1L);
        rolePermissionMapper.insert(created);
        assertNotNull(created.getId(), "插入后应回填自增主键");

        SysRolePermission fetched = rolePermissionMapper.selectById(created.getId());
        assertEquals(created.getRoleId(), fetched.getRoleId());
        assertEquals(created.getPermissionId(), fetched.getPermissionId());
        assertNotNull(fetched.getCreatedAt(), "createdAt 应被自动填充");
        assertNotNull(fetched.getUpdatedAt(), "updatedAt 应被自动填充");

        fetched.setPermissionId(2L);
        rolePermissionMapper.updateById(fetched);
        assertEquals(2L, rolePermissionMapper.selectById(created.getId()).getPermissionId());

        rolePermissionMapper.deleteById(created.getId());
        assertNull(rolePermissionMapper.selectById(created.getId()), "物理删除后不应可查");
    }

    /**
     * Given 同事务插入重复 (role_id, permission_id) When 再次插入 Then 数据库联合唯一索引触发 DuplicateKeyException。
     */
    @Test
    @Transactional
    @DisplayName("重复 roleId_permissionId 触发唯一约束异常")
    void test_重复roleId_permissionId_触发唯一约束异常() {
        rolePermissionMapper.insert(newRolePermission(200L, 200L));
        assertThrows(DuplicateKeyException.class, () -> rolePermissionMapper.insert(newRolePermission(200L, 200L)));
    }

    /**
     * Given Spring 上下文 When 注入关联表 Mapper Then 两个 Mapper 均非空。
     */
    @Test
    @DisplayName("mapper 注入成功")
    void test_mapper注入成功() {
        assertNotNull(userRoleMapper, "SysUserRoleMapper 应被注入");
        assertNotNull(rolePermissionMapper, "SysRolePermissionMapper 应被注入");
    }
}
