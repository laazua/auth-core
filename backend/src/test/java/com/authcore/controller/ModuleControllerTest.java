package com.authcore.controller;

import com.authcore.config.security.JwtTokenProvider;
import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import com.authcore.mapper.SysModuleMapper;
import com.authcore.mapper.SysPermissionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
 * ModuleController 模块 CRUD 判定用例（modules/001 AC1-5）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SysModuleMapper moduleMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

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
     * Given 种子数据 4 个模块
     * When GET /api/v1/modules?name=用户&status=1
     * Then status=200、total≥1、list 非空、每项含 id/name/code/base_url/description/status
     */
    @Test
    @DisplayName("分页查询支持多条件 name/status")
    void queryModulesWithPaginationAndFilters() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 调用分页查询
        String response = mockMvc.perform(get("/api/v1/modules")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10")
                        .param("name", "用户")
                        .param("status", "1"))
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
            assertNotNull(item.path("baseUrl").asText(), "baseUrl 不应为空");
            assertNotNull(item.path("description").asText(), "description 不应为空");
            assertNotNull(item.path("status").asText(), "status 不应为空");
            assertNotNull(item.path("createdAt").asText(), "createdAt 不应为空");
            assertNotNull(item.path("updatedAt").asText(), "updatedAt 不应为空");
        }
    }

    /**
     * AC1-2: 单模块详情。
     * GET /api/v1/modules/{id} 返回 200，data 含模块详情。
     */
    @Test
    @DisplayName("单模块详情存在返回 VO 不存在返回 404")
    void getModuleByIdExistsAndNotFound() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 存在的模块（假设种子数据中有 id=1）
        String response = mockMvc.perform(get("/api/v1/modules/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var root = mapper.readTree(response);
        assertNotNull(root.path("data").path("name").asText());
        assertNotNull(root.path("data").path("code").asText());
        assertNotNull(root.path("data").path("baseUrl").asText());
        assertNotNull(root.path("data").path("description").asText());
        assertNotNull(root.path("data").path("status").asText());
        assertNotNull(root.path("data").path("createdAt").asText());
        assertNotNull(root.path("data").path("updatedAt").asText());

        // 不存在的模块
        mockMvc.perform(get("/api/v1/modules/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1001));
    }

    /**
     * AC2: 模块下权限级联查询。
     * GET /api/v1/modules/{id}/permissions 返回 200，data 为 List<PermissionVO>，
     * 含该模块下所有权限。
     */
    @Test
    @DisplayName("模块下权限级联查询返回 List<PermissionVO>")
    void queryModulePermissionsCascade() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String response = mockMvc.perform(get("/api/v1/modules/1/permissions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var root = mapper.readTree(response);
        var list = root.path("data");

        assertTrue(list.isArray() && list.size() > 0, "权限列表应非空");

        // 验证每项含必要字段
        for (var item : list) {
            assertNotNull(item.path("id").asText(), "id 不应为空");
            assertNotNull(item.path("moduleId").asText(), "moduleId 不应为空");
            assertNotNull(item.path("name").asText(), "name 不应为空");
            assertNotNull(item.path("code").asText(), "code 不应为空");
            assertNotNull(item.path("description").asText(), "description 不应为空");
            assertNotNull(item.path("createdAt").asText(), "createdAt 不应为空");
            assertNotNull(item.path("updatedAt").asText(), "updatedAt 不应为空");
        }
    }

    /**
     * AC3: 创建模块唯一校验。
     * POST /api/v1/modules {name, code, base_url, description, status} 返回 200，
     * data.id 非空，重复 name 返回 409 code=1301、同 code 返回 409 code=1302。
     */
    @Test
    @DisplayName("创建模块唯一校验 name/code")
    void createModuleUniqueNameAndCode() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String name = unique("新模块");
        String code = unique("NEW_MOD").toUpperCase();

        // 创建新模块
        String createBody = """
                {
                    "name": "%s",
                    "code": "%s",
                    "base_url": "http://new",
                    "description": "测试模块",
                    "status": 1
                }
                """.formatted(name, code);

        String createResponse = mockMvc.perform(post("/api/v1/modules")
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
        Long moduleId = createRoot.path("data").path("id").asLong();
        assertNotNull(moduleId, "id 不应为空");

        // 再次创建同 name 应返回 409 code=1301
        mockMvc.perform(post("/api/v1/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1301))
                .andExpect(jsonPath("$.message").exists());

        // 创建同 code 不同 name 应返回 409 code=1302
        String createBody2 = """
                {
                    "name": "%s",
                    "code": "%s",
                    "base_url": "http://new2",
                    "description": "测试模块2",
                    "status": 1
                }
                """.formatted(unique("another"), code);

        mockMvc.perform(post("/api/v1/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1302))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * AC4: 更新模块 code 不可改。
     * PUT /api/v1/modules/{id} {name, base_url, description, status} 返回 200，
     * name/base_url/description/status 更新，code 不变。
     */
    @Test
    @DisplayName("更新模块 code 不可改")
    void updateModuleCodeImmutable() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 创建测试模块（直接插入）
        Long moduleId = createModuleDirectly(unique("mod_update"), unique("MOD_UPDATE").toUpperCase());

        // 获取原始 code
        String getResponse = mockMvc.perform(get("/api/v1/modules/" + moduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var getRoot = mapper.readTree(getResponse);
        String originalCode = getRoot.path("data").path("code").asText();

        // 更新模块（不含 code）
        String newName = unique("updated_name");
        String updateBody = """
                {
                    "name": "%s",
                    "base_url": "http://updated",
                    "description": "更新后的描述",
                    "status": 0
                }
                """.formatted(newName);

        String updateResponse = mockMvc.perform(put("/api/v1/modules/" + moduleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value(newName))
                .andExpect(jsonPath("$.data.status").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var updateRoot = mapper.readTree(updateResponse);
        // 验证 code 未变
        assertEquals(originalCode, updateRoot.path("data").path("code").asText(), "code 不应改变");
        assertEquals("http://updated", updateRoot.path("data").path("baseUrl").asText(), "base_url 应更新");
        assertEquals("更新后的描述", updateRoot.path("data").path("description").asText(), "description 应更新");
    }

    /**
     * AC5: 删除模块引用保护。
     * DELETE /api/v1/modules/{id} 若 sys_permission 存在则 409 code=1302，否则 200 物理删除。
     */
    @Test
    @DisplayName("删除模块引用保护")
    void deleteModuleWithReferenceProtection() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 创建测试模块（直接插入）
        Long moduleId = createModuleDirectly(unique("del_module"), unique("DEL_MODULE").toUpperCase());

        // 1. 创建权限并关联到该模块 → DELETE /api/v1/modules/{id} → 409 code=1302
        Long permId = createPermissionDirectly(unique("with_perm"), unique("WITH_PERM").toLowerCase() + ":view", moduleId);

        // 尝试删除有权限引用的模块
        mockMvc.perform(delete("/api/v1/modules/" + moduleId)
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1302))
                .andExpect(jsonPath("$.message").exists());

        // 2. 无引用时删除成功 - 创建新模块
        Long moduleIdNoRef = createModuleDirectly(unique("del_module_noref"), unique("DEL_MODULE_NOREF").toUpperCase());

        mockMvc.perform(delete("/api/v1/modules/" + moduleIdNoRef)
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证模块已删除
        mockMvc.perform(get("/api/v1/modules/" + moduleIdNoRef)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1001));
    }

    /**
     * AC1: ModuleController GET /modules/permissions/all 返回权限精简列表。
     * Given 种子数据包含权限
     * When GET /api/v1/modules/permissions/all
     * Then status=200、code=0、data 为非空数组、每项含 id/code/name
     */
    @Test
    @DisplayName("GET /modules/permissions/all 返回权限精简列表")
    void listAllPermissionsReturnsSimpleList() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String response = mockMvc.perform(get("/api/v1/modules/permissions/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var root = mapper.readTree(response);
        var list = root.path("data");

        assertTrue(list.isArray() && list.size() > 0, "权限列表应非空");

        // 验证每项仅含 id、code、name 三字段
        for (var item : list) {
            assertNotNull(item.path("id").asText(), "id 不应为空");
            assertNotNull(item.path("code").asText(), "code 不应为空");
            assertNotNull(item.path("name").asText(), "name 不应为空");
            // 不应包含其他字段
            assertEquals(3, item.size(), "每项应仅含 id/code/name 三字段");
        }
    }
}