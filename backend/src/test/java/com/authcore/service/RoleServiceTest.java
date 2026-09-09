package com.authcore.service;

import com.authcore.common.BusinessException;
import com.authcore.dto.permission.PermissionSimpleVO;
import com.authcore.dto.role.RoleCreateDTO;
import com.authcore.dto.role.RoleQueryDTO;
import com.authcore.dto.role.RoleUpdateDTO;
import com.authcore.dto.role.RoleVO;
import com.authcore.entity.SysRole;
import com.authcore.entity.SysRolePermission;
import com.authcore.entity.SysUserRole;
import com.authcore.mapper.SysRoleMapper;
import com.authcore.mapper.SysRolePermissionMapper;
import com.authcore.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RoleService 业务逻辑判定用例（roles/001 AC1-4）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoleServiceTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    private static final String TEST_PREFIX = "__ut_role_";

    private SysRole createTestRole(String suffix, int status) {
        SysRole role = new SysRole();
        role.setName(TEST_PREFIX + suffix);
        role.setCode(TEST_PREFIX + suffix.toUpperCase());
        role.setStatus(status);
        roleMapper.insert(role);
        return role;
    }

    /**
     * AC1: 分页查询支持多条件。
     * Given 种子数据 ROLE_ADMIN + 测试角色
     * When queryRoles(RoleQueryDTO(page=1, size=10, name="adm", status=1))
     * Then Page<RoleVO> total≥1、list 非空、每项含 id/name/code/status
     */
    @Test
    @DisplayName("分页查询支持多条件")
    void queryRolesWithPaginationAndFilters() {
        // 准备测试数据
        createTestRole("query1", 1);
        createTestRole("query2", 1);
        createTestRole("query3", 0); // 停用角色

        RoleQueryDTO query = new RoleQueryDTO(1, 10, "query", null, 1);
        var page = roleService.queryRoles(query);

        assertEquals(1, page.getCurrent(), "当前页应为 1");
        assertEquals(10, page.getSize(), "每页大小应为 10");
        assertEquals(2, page.getTotal(), "总数应为 2（仅 status=1 且 name 含 query）");
        assertEquals(2, page.getRecords().size(), "记录数应为 2");

        // 验证每条记录字段完整
        for (RoleVO vo : page.getRecords()) {
            assertNotNull(vo.id(), "id 不应为空");
            assertNotNull(vo.name(), "name 不应为空");
            assertNotNull(vo.code(), "code 不应为空");
            assertNotNull(vo.status(), "status 不应为空");
            assertTrue(vo.name().contains("query"), "name 应包含查询关键词");
            assertEquals(1, vo.status(), "status 应为 1");
        }
    }

    /**
     * AC1: 单角色详情：存在返回 VO，不存在抛 1001（复用用户错误码或角色专用）。
     */
    @Test
    @DisplayName("单角色详情存在返回 VO 不存在抛异常")
    void getRoleByIdExistsAndNotFound() {
        SysRole role = createTestRole("detail", 1);

        // 存在
        RoleVO vo = roleService.getRoleById(role.getId());
        assertEquals(role.getId(), vo.id());
        assertEquals(role.getName(), vo.name());
        assertEquals(role.getCode(), vo.code());
        assertEquals(role.getStatus(), vo.status());

        // 不存在
        var ex = assertThrows(BusinessException.class,
                () -> roleService.getRoleById(99999L));
        assertEquals(1001, ex.getCode()); // 复用用户不存在错误码或按规范定义
    }

    /**
     * AC2: 创建角色：name/code 唯一校验（code=1101/1102）、code 规范校验。
     */
    @Test
    @DisplayName("创建角色 name/code 唯一且 code 规范")
    void createRoleUniqueNameAndCode() {
        RoleCreateDTO dto = new RoleCreateDTO(
                TEST_PREFIX + "create",
                "ROLE_TEST_CREATE",
                1
        );

        RoleVO vo = roleService.createRole(dto);
        assertNotNull(vo.id(), "id 不应为空");
        assertEquals(dto.name(), vo.name());
        assertEquals(dto.code(), vo.code());
        assertEquals(1, vo.status(), "默认状态应为 1");

        // 重复 name 应抛 1101
        var exName = assertThrows(BusinessException.class,
                () -> roleService.createRole(dto));
        assertEquals(1101, exName.getCode(), "重复 name 应抛 1101");

        // 重复 code 应抛 1102
        RoleCreateDTO dto2 = new RoleCreateDTO(
                "不同名字",
                "ROLE_TEST_CREATE",
                1
        );
        var exCode = assertThrows(BusinessException.class,
                () -> roleService.createRole(dto2));
        assertEquals(1102, exCode.getCode(), "重复 code 应抛 1102");

        // code 格式不合规（不以 ROLE_ 开头）应抛校验异常
        RoleCreateDTO dto3 = new RoleCreateDTO(
                TEST_PREFIX + "badcode",
                "BAD_CODE",
                1
        );
        var exFormat = assertThrows(BusinessException.class,
                () -> roleService.createRole(dto3));
        assertEquals(400, exFormat.getCode(), "code 格式不合规应抛 400");
    }

    /**
     * AC3: 更新角色：name/status 更新、code 不可改。
     */
    @Test
    @DisplayName("更新角色 name/status 更新 code 不可改")
    void updateRoleCodeImmutable() {
        SysRole role = createTestRole("update", 1);
        String originalCode = role.getCode();

        // 使用唯一名称避免冲突
        String newName = TEST_PREFIX + "updated_" + System.currentTimeMillis();
        RoleUpdateDTO dto = new RoleUpdateDTO(newName, 0);
        RoleVO vo = roleService.updateRole(role.getId(), dto);

        assertEquals(newName, vo.name());
        assertEquals(0, vo.status());
        assertEquals(originalCode, vo.code(), "code 不应改变");

        // 验证数据库 code 未变
        SysRole updated = roleMapper.selectById(role.getId());
        assertEquals(originalCode, updated.getCode(), "数据库 code 不应改变");
    }

    /**
     * AC4: 删除角色：无引用物理删除、有 sys_user_role 引用抛 1103、有 sys_role_permission 引用抛 1104。
     */
    @Test
    @DisplayName("删除角色无引用物理删除 有用户引用抛 1103 有权限引用抛 1104")
    void deleteRoleReferenceProtection() {
        // 无引用角色
        SysRole role1 = createTestRole("delete1", 1);
        roleService.deleteRole(role1.getId());
        assertNull(roleMapper.selectById(role1.getId()), "无引用角色应物理删除");

        // 有 sys_user_role 引用
        SysRole role2 = createTestRole("delete2", 1);
        SysUserRole ur = new SysUserRole();
        ur.setUserId(1L); // admin 用户
        ur.setRoleId(role2.getId());
        userRoleMapper.insert(ur);

        var exUserRef = assertThrows(BusinessException.class,
                () -> roleService.deleteRole(role2.getId()));
        assertEquals(1103, exUserRef.getCode(), "有用户引用应抛 1103");
        assertNotNull(roleMapper.selectById(role2.getId()), "有用户引用角色不应被删除");

        // 有 sys_role_permission 引用
        SysRole role3 = createTestRole("delete3", 1);
        SysRolePermission rp = new SysRolePermission();
        rp.setRoleId(role3.getId());
        rp.setPermissionId(1L); // 任意权限
        rolePermissionMapper.insert(rp);

        var exPermRef = assertThrows(BusinessException.class,
                () -> roleService.deleteRole(role3.getId()));
        assertEquals(1104, exPermRef.getCode(), "有权限引用应抛 1104");
        assertNotNull(roleMapper.selectById(role3.getId()), "有权限引用角色不应被删除");
    }

    /**
     * AC2: listAllPermissions 返回所有权限精简列表。
     * Given 种子数据包含权限
     * When listAllPermissions()
     * Then 返回 List<PermissionSimpleVO> 非空、每项含 id/code/name
     */
    @Test
    @DisplayName("listAllPermissions 返回所有权限精简列表")
    void listAllPermissionsReturnsSimpleList() {
        List<PermissionSimpleVO> permissions = roleService.listAllPermissions();

        assertNotNull(permissions, "权限列表不应为空");
        assertTrue(permissions.size() > 0, "权限列表应非空");

        for (PermissionSimpleVO vo : permissions) {
            assertNotNull(vo.id(), "id 不应为空");
            assertNotNull(vo.code(), "code 不应为空");
            assertNotNull(vo.name(), "name 不应为空");
        }
    }
}