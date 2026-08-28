package com.authcore.controller;

import com.authcore.config.security.JwtTokenProvider;
import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import com.authcore.entity.SysRolePermission;
import com.authcore.mapper.SysModuleMapper;
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.mapper.SysRolePermissionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PermissionController 权限 CRUD 判定用例（perms/001 AC1-5）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SysModuleMapper moduleMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Autowired
    private SysRolePermissionMapper rolePermissionMapper;

    private static final String TEST_PREFIX = "ut_" + System.currentTimeMillis() + "_";

    private String unique(String base) {
        return TEST_PREFIX + base;
    }

    private String getAdminToken() throws Exception {
        String loginBody = """
                {
                    "username": "admin",
                    "password": "admin123456"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var root = mapper.readTree(loginResponse);
        return root.path("data").path("token").asText();
    }

    private Long createModuleDirectly(String name, String code) {
        SysModule module = new SysModule();
        module.setName(name);
        module.setCode(code);
        module.setStatus(1);
        moduleMapper.insert(module);
        return module.getId();
    }

    private Long createPermissionDirectly(String name, String code, Long moduleId) {
        SysPermission permission = new SysPermission();
        permission.setModuleId(moduleId);
        permission.setName(name);
        permission.setCode(code);
        permission.setDescription("测试描述");
        permissionMapper.insert(permission);
        return permission.getId();
    }

    /**
     * AC1: 分页查询支持多条件。
     * Given 种子数据 14 个权限 + 测试数据
     * When GET /api/v1/permissions?page=1&size=10&name=用户&module_id=1
     * Then status=200、total≥1、list 非空、每项含 id/name/code/module_id/description
     */
    @Test
    @DisplayName("分页查询支持多条件 name/module_id")
    void queryPermissionsWithPaginationAndFilters() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 调用分页查询
        String response = mockMvc.perform(get("/api/v1/permissions")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10")
                        .param("name", "用户")
                        .param("module_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").exists())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var responseRoot = mapper.readTree(response);
        int total = responseRoot.path("data").path("total").asInt();
        var list = responseRoot.path("data").path("records");

        assertTrue(total >= 1, "total 应 ≥ 1，实际: " + total);
        assertTrue(list.isArray() && list.size() > 0, "records 应非空");

        // 验证每项含必要字段
        for (var item : list) {
            assertNotNull(item.path("id").asText(), "id 不应为空");
            assertNotNull(item.path("name").asText(), "name 不应为空");
            assertNotNull(item.path("code").asText(), "code 不应为空");
            assertNotNull(item.path("moduleId").asText(), "moduleId 不应为空");
            assertNotNull(item.path("moduleName").asText(), "moduleName 不应为空");
        }
    }

    /**
     * AC2: 按模块分组查询。
     * GET /api/v1/permissions/grouped 返回 200，data 为 Map<ModuleVO, List<PermissionVO>>，
     * 每模块下含其权限列表。
     */
    @Test
    @DisplayName("按模块分组查询返回 Map")
    void queryPermissionsGroupedByModule() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String response = mockMvc.perform(get("/api/v1/permissions/grouped")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var responseRoot = mapper.readTree(response);
        var data = responseRoot.path("data");

        assertTrue(data.isObject(), "data 应为 Map 对象");

        // 验证每个模块下有权限列表
        var fields = data.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            var moduleKey = entry.getKey();
            var permissions = entry.getValue();

            // moduleKey 是 ModuleVO 的 toString() 形式，验证权限列表
            assertTrue(permissions.isArray(), "权限列表应为数组");
            for (var perm : permissions) {
                assertTrue(perm.has("id"), "权限应含 id");
                assertTrue(perm.has("name"), "权限应含 name");
                assertTrue(perm.has("code"), "权限应含 code");
                assertTrue(perm.has("moduleId"), "权限应含 moduleId");
                assertTrue(perm.has("moduleName"), "权限应含 moduleName");
            }
        }
    }

    /**
     * AC3: 创建权限唯一校验 + module_id 必填。
     * POST /api/v1/permissions {name, code, module_id, description} 返回 200，
     * data.id 非空，重复 name/code 返回 409 code=1201/1202，缺 module_id 返回 400。
     */
    @Test
    @DisplayName("创建权限唯一校验 name/code + module_id 必填")
    void createPermissionUniqueAndModuleRequired() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 先创建一个测试模块（直接插入）
        Long moduleId = createModuleDirectly(unique("module"), unique("MODULE").toUpperCase());

        String name = unique("newperm");
        String code = "perm:" + unique("test");

        // 创建新权限
        String createBody = """
                {
                    "name": "%s",
                    "code": "%s",
                    "module_id": %d
                }
                """.formatted(name, code, moduleId);

        String createResponse = mockMvc.perform(post("/api/v1/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var createRoot = mapper.readTree(createResponse);
        Long permissionId = createRoot.path("data").path("id").asLong();
        assertNotNull(permissionId, "id 不应为空");

        // 再次创建同 name 应返回 409 code=1201
        mockMvc.perform(post("/api/v1/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1201))
                .andExpect(jsonPath("$.message").exists());

        // 创建同 code 不同 name 应返回 409 code=1202
        String createBody2 = """
                {
                    "name": "%s",
                    "code": "%s",
                    "module_id": %d
                }
                """.formatted(unique("another"), code, moduleId);

        mockMvc.perform(post("/api/v1/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1202))
                .andExpect(jsonPath("$.message").exists());

        // 缺 module_id 返回 400
        String createBody3 = """
                {
                    "name": "%s",
                    "code": "%s"
                }
                """.formatted(unique("nomodule"), "perm:" + unique("nomodule"));

        mockMvc.perform(post("/api/v1/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * AC4: 更新权限 code 不可改。
     * PUT /api/v1/permissions/{id} {name, description, module_id} 返回 200，
     * name/description/module_id 更新，code 不变。
     */
    @Test
    @DisplayName("更新权限 code 不可改")
    void updatePermissionCodeImmutable() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 创建测试模块（直接插入）
        Long moduleId1 = createModuleDirectly(unique("mod1"), unique("MOD1").toUpperCase());
        Long moduleId2 = createModuleDirectly(unique("mod2"), unique("MOD2").toUpperCase());

        // 创建权限用于更新测试（直接插入）
        Long permId = createPermissionDirectly(unique("update_perm"), "perm:" + unique("UPDATE"), moduleId1);

        // 获取原始 code
        String getResponse = mockMvc.perform(get("/api/v1/permissions/" + permId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var getRoot = mapper.readTree(getResponse);
        String originalCode = getRoot.path("data").path("code").asText();

        // 更新权限（不含 code）
        String newName = unique("updated_name");
        String updateBody = """
                {
                    "name": "%s",
                    "description": "更新后的描述",
                    "module_id": %d
                }
                """.formatted(newName, moduleId2);

        String updateResponse = mockMvc.perform(put("/api/v1/permissions/" + permId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value(newName))
                .andExpect(jsonPath("$.data.moduleId").value(moduleId2.intValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var updateRoot = mapper.readTree(updateResponse);
        // 验证 code 未变
        assertEquals(originalCode, updateRoot.path("data").path("code").asText(), "code 不应改变");
    }

    /**
     * AC5: 删除权限引用保护。
     * DELETE /api/v1/permissions/{id} 若 sys_role_permission 存在则 409 code=1203，
     * 否则 200 物理删除。
     */
    @Test
    @DisplayName("删除权限引用保护")
    void deletePermissionWithReferenceProtection() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 创建测试模块（直接插入）
        Long moduleId = createModuleDirectly(unique("del_module"), unique("DEL_MODULE").toUpperCase());

        // 1. 创建权限并分配给角色 → DELETE /api/v1/permissions/{id} → 409 code=1203
        Long permIdWithRole = createPermissionDirectly(unique("with_role"), "perm:" + unique("WITH_ROLE"), moduleId);

        // 分配给 admin 角色 (role_id=1)
        SysRolePermission rp = new SysRolePermission();
        rp.setRoleId(1L);
        rp.setPermissionId(permIdWithRole);
        rolePermissionMapper.insert(rp);

        // 尝试删除有角色引用的权限
        mockMvc.perform(delete("/api/v1/permissions/" + permIdWithRole)
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1203))
                .andExpect(jsonPath("$.message").exists());

        // 2. 无引用时删除成功
        Long permIdNoRef = createPermissionDirectly(unique("no_ref"), "perm:" + unique("NO_REF"), moduleId);

        mockMvc.perform(delete("/api/v1/permissions/" + permIdNoRef)
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证权限已删除
        mockMvc.perform(get("/api/v1/permissions/" + permIdNoRef)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1001));

        // 清理：移除权限引用（用于清理 permIdWithRole）
        rolePermissionMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getPermissionId, permIdWithRole));

        mockMvc.perform(delete("/api/v1/permissions/" + permIdWithRole)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}