package com.authcore.service;

import com.authcore.common.BusinessException;
import com.authcore.dto.permission.ModuleVO;
import com.authcore.dto.permission.PermissionCreateDTO;
import com.authcore.dto.permission.PermissionQueryDTO;
import com.authcore.dto.permission.PermissionUpdateDTO;
import com.authcore.dto.permission.PermissionVO;
import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import com.authcore.entity.SysRolePermission;
import com.authcore.mapper.SysModuleMapper;
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.mapper.SysRolePermissionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PermissionService 业务逻辑判定用例（perms/001 AC1-5）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PermissionServiceTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Autowired
    private SysModuleMapper moduleMapper;

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    private static final String TEST_PREFIX = "__ut_perm_";

    private SysModule createTestModule(String suffix) {
        SysModule module = new SysModule();
        module.setName(TEST_PREFIX + "module_" + suffix);
        module.setCode(TEST_PREFIX + "MODULE_" + suffix.toUpperCase());
        module.setStatus(1);
        moduleMapper.insert(module);
        return module;
    }

    private SysPermission createTestPermission(String suffix, Long moduleId) {
        SysPermission permission = new SysPermission();
        permission.setModuleId(moduleId);
        permission.setName(TEST_PREFIX + "perm_" + suffix);
        permission.setCode(TEST_PREFIX + "PERM_" + suffix.toUpperCase());
        permission.setDescription("测试权限描述");
        permissionMapper.insert(permission);
        return permission;
    }

    /**
     * AC1: 分页查询支持多条件。
     * Given 种子数据 14 个权限
     * When queryPermissions(PermissionQueryDTO(page=1, size=10, name="用户", moduleId=1))
     * Then Page<PermissionVO> total≥1、list 非空、每项含 id/name/code/module_id/description
     */
    @Test
    @DisplayName("分页查询支持多条件 name/moduleId")
    void queryPermissionsWithPaginationAndFilters() {
        // 种子数据模块 1 (user_mgmt) 包含 4 个以"用户"开头的权限
        PermissionQueryDTO query = new PermissionQueryDTO(1, 10, "用户", null, 1L);
        var page = permissionService.queryPermissions(query);

        assertEquals(1, page.getCurrent(), "当前页应为 1");
        assertEquals(10, page.getSize(), "每页大小应为 10");
        assertTrue(page.getTotal() >= 1, "总数应 ≥ 1，实际: " + page.getTotal());
        assertTrue(page.getRecords().size() > 0, "记录应非空");

        // 验证每条记录字段完整
        for (PermissionVO vo : page.getRecords()) {
            assertNotNull(vo.id(), "id 不应为空");
            assertNotNull(vo.name(), "name 不应为空");
            assertNotNull(vo.code(), "code 不应为空");
            assertNotNull(vo.moduleId(), "moduleId 不应为空");
            assertNotNull(vo.moduleName(), "moduleName 不应为空");
            assertNotNull(vo.createdAt(), "createdAt 不应为空");
            assertNotNull(vo.updatedAt(), "updatedAt 不应为空");
        }
    }

    /**
     * AC2: 按模块分组查询。
     * When queryPermissionsGroupedByModule()
     * Then Map<ModuleVO, List<PermissionVO>> key 含模块、value 含该模块下权限
     */
    @Test
    @DisplayName("按模块分组查询返回 Map")
    void queryPermissionsGroupedByModule() {
        // 准备测试模块
        SysModule module1 = createTestModule("group1");
        SysModule module2 = createTestModule("group2");

        // 创建测试权限
        createTestPermission("view_1", module1.getId());
        createTestPermission("create_1", module1.getId());
        createTestPermission("view_2", module2.getId());

        Map<ModuleVO, List<PermissionVO>> grouped = permissionService.queryPermissionsGroupedByModule();

        assertNotNull(grouped, "分组结果不应为空");
        assertTrue(grouped.size() >= 2, "应至少包含 2 个模块");

        // 验证模块1有2个权限，模块2有1个权限
        for (Map.Entry<ModuleVO, List<PermissionVO>> entry : grouped.entrySet()) {
            ModuleVO moduleVO = entry.getKey();
            List<PermissionVO> permissions = entry.getValue();

            assertNotNull(moduleVO.id(), "模块 id 不应为空");
            assertNotNull(moduleVO.name(), "模块 name 不应为空");
            assertNotNull(moduleVO.code(), "模块 code 不应为空");

            if (moduleVO.id().equals(module1.getId())) {
                assertEquals(2, permissions.size(), "模块1应有2个权限");
            } else if (moduleVO.id().equals(module2.getId())) {
                assertEquals(1, permissions.size(), "模块2应有1个权限");
            }

            for (PermissionVO vo : permissions) {
                assertEquals(moduleVO.id(), vo.moduleId(), "权限应属于对应模块");
            }
        }
    }

    /**
     * AC3: 单权限详情：存在返回 VO，不存在抛 1001。
     */
    @Test
    @DisplayName("单权限详情存在返回 VO 不存在抛异常")
    void getPermissionByIdExistsAndNotFound() {
        SysModule module = createTestModule("detail");
        SysPermission permission = createTestPermission("detail", module.getId());

        // 存在
        PermissionVO vo = permissionService.getPermissionById(permission.getId());
        assertEquals(permission.getId(), vo.id());
        assertEquals(permission.getName(), vo.name());
        assertEquals(permission.getCode(), vo.code());
        assertEquals(permission.getModuleId(), vo.moduleId());
        assertEquals(module.getName(), vo.moduleName());

        // 不存在
        var ex = assertThrows(BusinessException.class,
                () -> permissionService.getPermissionById(99999L));
        assertEquals(1001, ex.getCode());
    }

    /**
     * AC3: 创建权限：name/code 唯一校验（code=1201/1202）、module_id 必填。
     */
    @Test
    @DisplayName("创建权限 name/code 唯一且 module_id 必填")
    void createPermissionUniqueAndModuleRequired() {
        SysModule module1 = createTestModule("create1");
        SysModule module2 = createTestModule("create2");

        PermissionCreateDTO dto = new PermissionCreateDTO(
                TEST_PREFIX + "create",
                TEST_PREFIX + "CREATE",
                module1.getId(),
                "测试创建权限"
        );

        PermissionVO vo = permissionService.createPermission(dto);
        assertNotNull(vo.id(), "id 不应为空");
        assertEquals(dto.name(), vo.name());
        assertEquals(dto.code(), vo.code());
        assertEquals(dto.moduleId(), vo.moduleId());
        assertEquals(module1.getName(), vo.moduleName());

        // 重复 name 应抛 1201
        var exName = assertThrows(BusinessException.class,
                () -> permissionService.createPermission(dto));
        assertEquals(1201, exName.getCode(), "重复 name 应抛 1201");

        // 重复 code 应抛 1202
        PermissionCreateDTO dto2 = new PermissionCreateDTO(
                "不同名字",
                TEST_PREFIX + "CREATE",
                module1.getId(),
                "测试创建权限"
        );
        var exCode = assertThrows(BusinessException.class,
                () -> permissionService.createPermission(dto2));
        assertEquals(1202, exCode.getCode(), "重复 code 应抛 1202");

        // 缺 module_id 应抛校验异常
        PermissionCreateDTO dto3 = new PermissionCreateDTO(
                TEST_PREFIX + "badmodule",
                TEST_PREFIX + "BADMODULE",
                null,
                "缺 module_id"
        );
        var exModule = assertThrows(BusinessException.class,
                () -> permissionService.createPermission(dto3));
        assertEquals(400, exModule.getCode(), "缺 module_id 应抛 400");

        // 不存在的 module_id 应抛 400
        PermissionCreateDTO dto4 = new PermissionCreateDTO(
                TEST_PREFIX + "badmodule2",
                TEST_PREFIX + "BADMODULE2",
                99999L,
                "不存在的 module_id"
        );
        var exModuleNotFound = assertThrows(BusinessException.class,
                () -> permissionService.createPermission(dto4));
        assertEquals(400, exModuleNotFound.getCode(), "不存在的 module_id 应抛 400");
    }

    /**
     * AC4: 更新权限：name/description/module_id 更新、code 不可改。
     */
    @Test
    @DisplayName("更新权限 name/description/module_id 更新 code 不可改")
    void updatePermissionCodeImmutable() {
        SysModule module1 = createTestModule("update1");
        SysModule module2 = createTestModule("update2");
        SysPermission permission = createTestPermission("update", module1.getId());
        String originalCode = permission.getCode();

        // 使用唯一名称避免冲突
        String newName = TEST_PREFIX + "updated_" + System.currentTimeMillis();
        PermissionUpdateDTO dto = new PermissionUpdateDTO(newName, "新描述", module2.getId());
        PermissionVO vo = permissionService.updatePermission(permission.getId(), dto);

        assertEquals(newName, vo.name());
        assertEquals("新描述", vo.description());
        assertEquals(module2.getId(), vo.moduleId());
        assertEquals(module2.getName(), vo.moduleName());
        assertEquals(originalCode, vo.code(), "code 不应改变");

        // 验证数据库 code 未变
        SysPermission updated = permissionMapper.selectById(permission.getId());
        assertEquals(originalCode, updated.getCode(), "数据库 code 不应改变");
    }

    /**
     * AC5: 删除权限：无引用物理删除、有 sys_role_permission 引用抛 1203。
     */
    @Test
    @DisplayName("删除权限无引用物理删除 有引用抛 1203")
    void deletePermissionReferenceProtection() {
        SysModule module = createTestModule("delete");

        // 无引用权限
        SysPermission perm1 = createTestPermission("delete1", module.getId());
        permissionService.deletePermission(perm1.getId());
        assertNull(permissionMapper.selectById(perm1.getId()), "无引用权限应物理删除");

        // 有 sys_role_permission 引用
        SysPermission perm2 = createTestPermission("delete2", module.getId());
        SysRolePermission rp = new SysRolePermission();
        rp.setRoleId(1L); // admin 角色
        rp.setPermissionId(perm2.getId());
        rolePermissionMapper.insert(rp);

        var exPermRef = assertThrows(BusinessException.class,
                () -> permissionService.deletePermission(perm2.getId()));
        assertEquals(1203, exPermRef.getCode(), "有引用应抛 1203");
        assertNotNull(permissionMapper.selectById(perm2.getId()), "有引用权限不应被删除");
    }
}