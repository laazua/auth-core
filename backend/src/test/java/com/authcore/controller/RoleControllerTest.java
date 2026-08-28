package com.authcore.controller;

import com.authcore.config.security.JwtTokenProvider;
import com.authcore.entity.SysRolePermission;
import com.authcore.entity.SysUserRole;
import com.authcore.mapper.SysPermissionMapper;
import com.authcore.mapper.SysRolePermissionMapper;
import com.authcore.mapper.SysUserRoleMapper;
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
 * RoleController 角色 CRUD 判定用例（roles/001 AC1-4）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

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

    private Long createRole(String token, String name, String code) throws Exception {
        String createBody = """
                {
                    "name": "%s",
                    "code": "%s",
                    "status": 1
                }
                """.formatted(name, code);

        String createResponse = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var createRoot = mapper.readTree(createResponse);
        return createRoot.path("data").path("id").asLong();
    }

    private void assignRoleToUser(String token, Long userId, Long roleId) throws Exception {
        String assignBody = """
                {
                    "roleIds": [%d]
                }
                """.formatted(roleId);

        mockMvc.perform(put("/api/v1/users/" + userId + "/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody))
                .andExpect(status().isOk());
    }

    private void clearUserRoles(String token, Long userId) throws Exception {
        String clearBody = """
                {
                    "roleIds": []
                }
                """;

        mockMvc.perform(put("/api/v1/users/" + userId + "/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clearBody))
                .andExpect(status().isOk());
    }

    private void assignPermissionToRole(Long roleId, Long permissionId) {
        SysRolePermission rp = new SysRolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        rolePermissionMapper.insert(rp);
    }

    /**
     * AC1: 分页查询支持多条件。
     * Given 种子数据 admin 角色 + 测试角色
     * When GET /api/v1/roles?name=管&status=1
     * Then status=200、total≥1、list 非空、每项含 id/name/code/status
     */
    @Test
    @DisplayName("分页查询支持多条件")
    void queryRolesWithPaginationAndFilters() throws Exception {
        String token = getAdminToken();

        // 调用分页查询
        String response = mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10")
                        .param("name", "管")
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

        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
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
            assertNotNull(item.path("status").asText(), "status 不应为空");
        }
    }

    /**
     * AC2: 创建角色唯一校验。
     * POST /api/v1/roles {name, code, status} 返回 200，data.id 非空，重复 name/code 返回 409 code=1101/1102。
     */
    @Test
    @DisplayName("创建角色唯一校验 name/code")
    void createRoleUniqueNameAndCode() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String name = unique("newrole");
        String code = "ROLE_" + unique("TEST").toUpperCase();

        // 创建新角色
        String createBody = """
                {
                    "name": "%s",
                    "code": "%s",
                    "status": 1
                }
                """.formatted(name, code);

        String createResponse = mockMvc.perform(post("/api/v1/roles")
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
        Long roleId = createRoot.path("data").path("id").asLong();
        assertNotNull(roleId, "id 不应为空");

        // 再次创建同 name 应返回 409 code=1101
        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1101))
                .andExpect(jsonPath("$.message").exists());

        // 创建同 code 不同 name 应返回 409 code=1102
        String createBody2 = """
                {
                    "name": "%s",
                    "code": "%s",
                    "status": 1
                }
                """.formatted(unique("another"), code);

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1102))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * AC3: 更新角色 code 不可改。
     * PUT /api/v1/roles/{id} {name, status} 返回 200，name/status 更新，code 不变。
     */
    @Test
    @DisplayName("更新角色 code 不可改")
    void updateRoleCodeImmutable() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 先创建一个角色用于更新测试
        String createBody = """
                {
                    "name": "%s",
                    "code": "ROLE_%s",
                    "status": 1
                }
                """.formatted(unique("update_role"), unique("UPDATE").toUpperCase());

        String createResponse = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var createRoot = mapper.readTree(createResponse);
        Long roleId = createRoot.path("data").path("id").asLong();
        String originalCode = createRoot.path("data").path("code").asText();

        // 更新角色（不含 code），使用唯一名称避免冲突
        String newName = unique("updated_name");
        String updateBody = """
                {
                    "name": "%s",
                    "status": 0
                }
                """.formatted(newName);

        String updateResponse = mockMvc.perform(put("/api/v1/roles/" + roleId)
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
    }

    /**
     * AC4: 删除角色引用保护。
     * DELETE /api/v1/roles/{id} 若 sys_user_role 或 sys_role_permission 存在则 409 code=1103/1104，否则 200 物理删除。
     */
    @Test
    @DisplayName("删除角色引用保护")
    void deleteRoleWithReferenceProtection() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        List<Long> createdRoleIds = new ArrayList<>();

        // 1. 创建角色并分配给用户 → DELETE /api/v1/roles/3 → 409 code=1103
        Long roleIdWithUser = createRole(token, unique("user_ref"), "ROLE_" + unique("USER_REF").toUpperCase());
        createdRoleIds.add(roleIdWithUser);

        assignRoleToUser(token, 1L, roleIdWithUser);

        // 尝试删除有用户引用的角色
        mockMvc.perform(delete("/api/v1/roles/" + roleIdWithUser)
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1103))
                .andExpect(jsonPath("$.message").exists());

        // 2. 创建角色并分配权限 → DELETE /api/v1/roles/4 → 409 code=1104
        Long roleIdWithPerm = createRole(token, unique("perm_ref"), "ROLE_" + unique("PERM_REF").toUpperCase());
        createdRoleIds.add(roleIdWithPerm);

        // 直接插入 sys_role_permission（权限 id=1 为 user:view）
        assignPermissionToRole(roleIdWithPerm, 1L);

        // 先清理：移除用户关联
        clearUserRoles(token, 1L);

        // 尝试删除有权限引用的角色
        mockMvc.perform(delete("/api/v1/roles/" + roleIdWithPerm)
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1104))
                .andExpect(jsonPath("$.message").exists());

        // 3. 无引用时删除成功
        Long roleIdNoRef = createRole(token, unique("no_ref"), "ROLE_" + unique("NO_REF").toUpperCase());
        createdRoleIds.add(roleIdNoRef);
        
        // Debug: check permission count for roleIdNoRef
        long permCountBefore = rolePermissionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.authcore.entity.SysRolePermission>()
                        .eq(com.authcore.entity.SysRolePermission::getRoleId, roleIdNoRef));
        System.out.println("DEBUG: perm count for roleIdNoRef before delete: " + permCountBefore);

        // 删除无引用角色
        mockMvc.perform(delete("/api/v1/roles/" + roleIdNoRef)
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证角色已删除
        mockMvc.perform(get("/api/v1/roles/" + roleIdNoRef)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1001));

        // 清理：移除权限引用（用于清理 roleIdWithPerm）
        rolePermissionMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.authcore.entity.SysRolePermission>()
                .eq(com.authcore.entity.SysRolePermission::getRoleId, roleIdWithPerm));

        // 清理：清空其他测试角色的用户关联
        clearUserRoles(token, 1L);

        // 清理：删除创建的其他测试角色（排除已删除的 roleIdNoRef）
        for (Long id : createdRoleIds) {
            if (!id.equals(roleIdNoRef)) {
                mockMvc.perform(delete("/api/v1/roles/" + id)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk());
            }
        }
    }

    /**
     * AC1: 批量分配权限成功（全量替换）。
     * Given 存在的角色与有效权限 IDs
     * When PUT /api/v1/roles/{id}/permissions {permissionIds:[3,4]}
     * Then status=200、sys_role_permission 表中该角色仅关联指定权限
     */
    @Test
    @DisplayName("批量分配权限成功（全量替换）")
    void assignPermissionsBatchReplace() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 创建测试角色
        String roleName = unique("perm_assign_role");
        String roleCode = "ROLE_" + unique("PERM_ASSIGN").toUpperCase();
        Long roleId = createRole(token, roleName, roleCode);

        // 准备请求体：分配权限 3 和 4（sys_permission 种子数据中存在）
        String requestBody = """
                {
                    "permissionIds": [3, 4]
                }
                """;

        // 执行分配
        String response = mockMvc.perform(put("/api/v1/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 验证数据库：sys_role_permission 表中该角色仅有两行 (roleId, 3) 和 (roleId, 4)
        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId);
        List<SysRolePermission> relations = rolePermissionMapper.selectList(queryWrapper);

        assertEquals(2, relations.size(), "角色应仅关联 2 个权限");
        List<Long> permIds = relations.stream().map(SysRolePermission::getPermissionId).sorted().toList();
        assertEquals(List.of(3L, 4L), permIds, "权限 ID 应为 3 和 4");
    }

    /**
     * AC2: 权限不存在返回 400。
     * Given 不存在的 permissionId
     * When PUT /api/v1/roles/{id}/permissions {permissionIds:[999]}
     * Then status=400、code=1201
     */
    @Test
    @DisplayName("权限不存在返回 400 code=1201")
    void assignPermissionsInvalidPermissionReturns400() throws Exception {
        String token = getAdminToken();

        // 创建测试角色
        String roleName = unique("invalid_perm_role");
        String roleCode = "ROLE_" + unique("INVALID_PERM").toUpperCase();
        Long roleId = createRole(token, roleName, roleCode);

        // 准备请求体：包含不存在的权限 ID 999
        String requestBody = """
                {
                    "permissionIds": [999]
                }
                """;

        // 执行分配，期望 400
        mockMvc.perform(put("/api/v1/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1201))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * AC3: 角色不存在返回 404。
     * Given 不存在的 roleId
     * When PUT /api/v1/roles/99999/permissions {permissionIds:[1]}
     * Then status=404、code=1001
     */
    @Test
    @DisplayName("角色不存在返回 404 code=1001")
    void assignPermissionsRoleNotFoundReturns404() throws Exception {
        String token = getAdminToken();

        // 准备请求体：有效权限 ID
        String requestBody = """
                {
                    "permissionIds": [1]
                }
                """;

        // 执行分配，期望 404
        mockMvc.perform(put("/api/v1/roles/99999/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * AC4: 空列表清空权限。
     * Given 角色已有权限关联
     * When PUT /api/v1/roles/{id}/permissions {permissionIds:[]}
     * Then status=200、sys_role_permission 该角色行数为 0
     */
    @Test
    @DisplayName("空列表清空权限")
    void assignPermissionsEmptyListClearsPermissions() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 创建测试角色
        String roleName = unique("clear_perm_role");
        String roleCode = "ROLE_" + unique("CLEAR_PERM").toUpperCase();
        Long roleId = createRole(token, roleName, roleCode);

        // 先手动插入一些权限关联（权限 1 和 2）
        assignPermissionToRole(roleId, 1L);
        assignPermissionToRole(roleId, 2L);

        // 验证初始状态：有 2 个关联
        var queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId);
        long initialCount = rolePermissionMapper.selectCount(queryWrapper);
        assertEquals(2, initialCount, "初始应有 2 个权限关联");

        // 准备请求体：空列表
        String requestBody = """
                {
                    "permissionIds": []
                }
                """;

        // 执行分配（清空）
        mockMvc.perform(put("/api/v1/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证数据库：该角色的关联已清空
        long finalCount = rolePermissionMapper.selectCount(queryWrapper);
        assertEquals(0, finalCount, "清空后权限关联应为 0");
    }
}