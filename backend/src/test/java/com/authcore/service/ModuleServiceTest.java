package com.authcore.service;

import com.authcore.common.BusinessException;
import com.authcore.dto.module.ModuleCreateDTO;
import com.authcore.dto.module.ModuleQueryDTO;
import com.authcore.dto.module.ModuleUpdateDTO;
import com.authcore.dto.module.ModuleVO;
import com.authcore.dto.permission.PermissionSimpleVO;
import com.authcore.dto.permission.PermissionVO;
import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import com.authcore.mapper.SysModuleMapper;
import com.authcore.mapper.SysPermissionMapper;
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
 * ModuleService 业务逻辑判定用例（modules/001 AC1-5）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ModuleServiceTest {

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private SysModuleMapper moduleMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    private static final String TEST_PREFIX = "__ut_module_";

    private SysModule createTestModule(String suffix) {
        SysModule module = new SysModule();
        module.setName(TEST_PREFIX + "module_" + suffix);
        module.setCode(TEST_PREFIX + "MODULE_" + suffix.toUpperCase());
        module.setBaseUrl("http://test-" + suffix);
        module.setDescription("测试模块描述");
        module.setStatus(1);
        moduleMapper.insert(module);
        return module;
    }

    private SysPermission createTestPermission(String suffix, Long moduleId) {
        SysPermission permission = new SysPermission();
        permission.setModuleId(moduleId);
        permission.setName(TEST_PREFIX + "perm_" + suffix);
        permission.setCode("perm:" + TEST_PREFIX + "PERM_" + suffix.toUpperCase());
        permission.setDescription("测试权限描述");
        permissionMapper.insert(permission);
        return permission;
    }

    /**
     * AC1: 分页查询支持多条件。
     * Given 种子数据 4 个模块
     * When queryModules(ModuleQueryDTO(page=1, size=10, name="用户", status=1))
     * Then Page<ModuleVO> total≥1、list 非空、每项含 id/name/code/base_url/description/status
     */
    @Test
    @DisplayName("分页查询支持多条件 name/status")
    void queryModulesWithPaginationAndFilters() {
        // 种子数据模块 1 (user_mgmt) 包含以"用户"开头的模块
        ModuleQueryDTO query = new ModuleQueryDTO(1, 10, "用户", null, 1);
        var page = moduleService.queryModules(query);

        assertEquals(1, page.getCurrent(), "当前页应为 1");
        assertEquals(10, page.getSize(), "每页大小应为 10");
        assertTrue(page.getTotal() >= 1, "总数应 ≥ 1，实际: " + page.getTotal());
        assertTrue(page.getRecords().size() > 0, "记录应非空");

        // 验证每条记录字段完整
        for (ModuleVO vo : page.getRecords()) {
            assertNotNull(vo.id(), "id 不应为空");
            assertNotNull(vo.name(), "name 不应为空");
            assertNotNull(vo.code(), "code 不应为空");
            assertNotNull(vo.baseUrl(), "baseUrl 不应为空");
            assertNotNull(vo.description(), "description 不应为空");
            assertNotNull(vo.status(), "status 不应为空");
            assertNotNull(vo.createdAt(), "createdAt 不应为空");
            assertNotNull(vo.updatedAt(), "updatedAt 不应为空");
        }
    }

    /**
     * AC1-2: 单模块详情：存在返回 VO，不存在抛 1001。
     */
    @Test
    @DisplayName("单模块详情存在返回 VO 不存在抛异常")
    void getModuleByIdExistsAndNotFound() {
        SysModule module = createTestModule("detail");

        // 存在
        ModuleVO vo = moduleService.getModuleById(module.getId());
        assertEquals(module.getId(), vo.id());
        assertEquals(module.getName(), vo.name());
        assertEquals(module.getCode(), vo.code());
        assertEquals(module.getBaseUrl(), vo.baseUrl());
        assertEquals(module.getDescription(), vo.description());
        assertEquals(module.getStatus(), vo.status());

        // 不存在
        var ex = assertThrows(BusinessException.class,
                () -> moduleService.getModuleById(99999L));
        assertEquals(1001, ex.getCode());
    }

    /**
     * AC2: 模块下权限级联查询。
     * When queryModulePermissions(moduleId)
     * Then List<PermissionVO> 含该模块下所有权限
     */
    @Test
    @DisplayName("模块下权限级联查询返回 List<PermissionVO>")
    void queryModulePermissionsCascade() {
        SysModule module = createTestModule("perm_cascade");

        // 创建测试权限
        createTestPermission("view_1", module.getId());
        createTestPermission("create_1", module.getId());
        createTestPermission("update_1", module.getId());

        List<PermissionVO> permissions = moduleService.queryModulePermissions(module.getId());

        assertNotNull(permissions, "权限列表不应为空");
        assertEquals(3, permissions.size(), "应有 3 个权限");

        for (PermissionVO vo : permissions) {
            assertEquals(module.getId(), vo.moduleId(), "权限应属于对应模块");
            assertNotNull(vo.id(), "id 不应为空");
            assertNotNull(vo.name(), "name 不应为空");
            assertNotNull(vo.code(), "code 不应为空");
            assertNotNull(vo.description(), "description 不应为空");
            assertNotNull(vo.createdAt(), "createdAt 不应为空");
            assertNotNull(vo.updatedAt(), "updatedAt 不应为空");
        }
    }

    /**
     * AC3: 创建模块：name/code 唯一校验（code=1301/1302）、status 默认 1。
     */
    @Test
    @DisplayName("创建模块 name/code 唯一且 status 默认 1")
    void createModuleUniqueNameAndCode() {
        SysModule module1 = createTestModule("create1");

        ModuleCreateDTO dto = new ModuleCreateDTO(
                TEST_PREFIX + "create",
                TEST_PREFIX + "CREATE",
                "http://create",
                "测试创建模块",
                1
        );

        ModuleVO vo = moduleService.createModule(dto);
        assertNotNull(vo.id(), "id 不应为空");
        assertEquals(dto.name(), vo.name());
        assertEquals(dto.code(), vo.code());
        assertEquals(dto.baseUrl(), vo.baseUrl());
        assertEquals(dto.description(), vo.description());
        assertEquals(1, vo.status(), "status 应为 1");

        // 重复 name 应抛 1301
        var exName = assertThrows(BusinessException.class,
                () -> moduleService.createModule(dto));
        assertEquals(1301, exName.getCode(), "重复 name 应抛 1301");

        // 重复 code 应抛 1302
        ModuleCreateDTO dto2 = new ModuleCreateDTO(
                "不同名字",
                TEST_PREFIX + "CREATE",
                "http://create2",
                "测试创建模块2",
                1
        );
        var exCode = assertThrows(BusinessException.class,
                () -> moduleService.createModule(dto2));
        assertEquals(1302, exCode.getCode(), "重复 code 应抛 1302");

        // status 为 null 时应默认为 1
        ModuleCreateDTO dto3 = new ModuleCreateDTO(
                TEST_PREFIX + "create3",
                TEST_PREFIX + "CREATE3",
                "http://create3",
                "测试创建模块3",
                null
        );
        ModuleVO vo3 = moduleService.createModule(dto3);
        assertEquals(1, vo3.status(), "status 为 null 时应默认为 1");
    }

    /**
     * AC4: 更新模块：name/base_url/description/status 更新、code 不可改。
     */
    @Test
    @DisplayName("更新模块 name/base_url/description/status 更新 code 不可改")
    void updateModuleCodeImmutable() {
        SysModule module = createTestModule("update");
        String originalCode = module.getCode();

        // 使用唯一名称避免冲突
        String newName = TEST_PREFIX + "updated_" + System.currentTimeMillis();
        ModuleUpdateDTO dto = new ModuleUpdateDTO(newName, "http://updated", "新描述", 0);
        ModuleVO vo = moduleService.updateModule(module.getId(), dto);

        assertEquals(newName, vo.name());
        assertEquals("http://updated", vo.baseUrl());
        assertEquals("新描述", vo.description());
        assertEquals(0, vo.status());
        assertEquals(originalCode, vo.code(), "code 不应改变");

        // 验证数据库 code 未变
        SysModule updated = moduleMapper.selectById(module.getId());
        assertEquals(originalCode, updated.getCode(), "数据库 code 不应改变");
    }

    /**
     * AC5: 删除模块：无引用物理删除、有 sys_permission 引用抛 1302。
     */
    @Test
    @DisplayName("删除模块无引用物理删除 有权限引用抛 1302")
    void deleteModuleReferenceProtection() {
        SysModule module = createTestModule("delete");

        // 无引用模块
        moduleService.deleteModule(module.getId());
        assertNull(moduleMapper.selectById(module.getId()), "无引用模块应物理删除");

        // 有 sys_permission 引用
        SysModule module2 = createTestModule("delete_ref");
        SysPermission perm = createTestPermission("delete_ref", module2.getId());

        var exPermRef = assertThrows(BusinessException.class,
                () -> moduleService.deleteModule(module2.getId()));
        assertEquals(1302, exPermRef.getCode(), "有权限引用应抛 1302");
        assertNotNull(moduleMapper.selectById(module2.getId()), "有引用模块不应被删除");
    }

    /**
     * AC1: listAllPermissions 返回所有权限精简列表。
     * Given 种子数据包含权限
     * When listAllPermissions()
     * Then 返回 List<PermissionSimpleVO> 非空、每项含 id/code/name
     */
    @Test
    @DisplayName("listAllPermissions 返回所有权限精简列表")
    void listAllPermissionsReturnsSimpleList() {
        List<PermissionSimpleVO> permissions = moduleService.listAllPermissions();

        assertNotNull(permissions, "权限列表不应为空");
        assertTrue(permissions.size() > 0, "权限列表应非空");

        for (PermissionSimpleVO vo : permissions) {
            assertNotNull(vo.id(), "id 不应为空");
            assertNotNull(vo.code(), "code 不应为空");
            assertNotNull(vo.name(), "name 不应为空");
        }
    }
}