package com.authcore.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 角色-权限回显契约（web/048b）。
 *
 * <p>前端权限分配抽屉 {@code PermissionAssignDrawer} 打开时读 {@code role.permissions}
 * 渲染已分配勾选；本类断言详情回显、既有接口零回归与空授权角色空数组形态。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RolePermissionsEchoContractTest {

    private static final String TMP_CODE = "ROLE_REGRESSION_TMP";

    @Autowired
    private MockMvc mockMvc;

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

        var mapper = new ObjectMapper();
        return mapper.readTree(loginResponse).path("data").path("token").asText();
    }

    private long findRoleIdByCode(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .param("code", code)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode records = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .path("data").path("records");
        assertTrue(records.size() > 0, "应存在 code=" + code + " 的角色");
        return records.get(0).path("id").asLong();
    }

    /**
     * AC1: 角色详情权限回显。
     * Given 种子 ROLE_ADMIN 已被 V6/V7 幂等授权
     * When GET /api/v1/roles/{id}
     * Then data.permissions 为数组且含 code=user:view（元素含 id/code/children 键）
     * 修复前：RoleVO 无 permissions 字段 → jsonPath 取值失败。
     */
    @Test
    @DisplayName("角色详情返回已分配权限回显")
    void roleDetailReturnsAssignedPermissionsEcho() throws Exception {
        String token = getAdminToken();
        long roleId = findRoleIdByCode(token, "ROLE_ADMIN");

        mockMvc.perform(get("/api/v1/roles/" + roleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions[?(@.code == 'user:view')]",
                        Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.permissions[?(@.code == 'user:view')].id[0]").isNumber())
                .andExpect(jsonPath("$.data.permissions[?(@.code == 'user:view')].children").isArray());
    }

    /**
     * AC2: 既有分页与新建角色接口零回归。
     * When GET /api/v1/roles 分页 与 POST /api/v1/roles + DELETE 清理
     * Then 分页既有六键不变；创建-删除链路 code=0（不断言 permissions 键，纯护栏）
     */
    @Test
    @DisplayName("既有分页与新建角色接口保持可用")
    void existingPagedAndCreateRoleEndpointsRemainAvailable() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].id").exists())
                .andExpect(jsonPath("$.data.records[0].name").exists())
                .andExpect(jsonPath("$.data.records[0].code").exists())
                .andExpect(jsonPath("$.data.records[0].status").exists())
                .andExpect(jsonPath("$.data.records[0].createTime").exists())
                .andExpect(jsonPath("$.data.records[0].updateTime").exists());

        // 幂等：清理上轮失败残留后创建
        MvcResult residual = mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .param("code", TMP_CODE)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode residualRecords = new ObjectMapper()
                .readTree(residual.getResponse().getContentAsString())
                .path("data").path("records");
        if (residualRecords.size() > 0) {
            mockMvc.perform(delete("/api/v1/roles/" + residualRecords.get(0).path("id").asLong())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        String createBody = """
                {
                    "name": "回归临时角色",
                    "code": "ROLE_REGRESSION_TMP",
                    "status": 1
                }
                """;
        String created = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long newId = new ObjectMapper().readTree(created).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/v1/roles/" + newId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * AC3: 无授权角色回显空数组。
     * Given 除 ROLE_ADMIN 外种子角色无 sys_role_permission 授权
     * When GET /api/v1/roles/{id}
     * Then data.permissions 为空数组（非 null/缺失）
     */
    @Test
    @DisplayName("无授权角色回显空权限数组")
    void roleWithoutGrantEchoesEmptyPermissionsArray() throws Exception {
        String token = getAdminToken();

        MvcResult page = mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode records = new ObjectMapper()
                .readTree(page.getResponse().getContentAsString())
                .path("data").path("records");

        long targetId = -1;
        for (JsonNode row : records) {
            if (!"ROLE_ADMIN".equals(row.path("code").asText())) {
                targetId = row.path("id").asLong();
                break;
            }
        }
        assertTrue(targetId > 0, "应存在非 ROLE_ADMIN 的种子角色");

        mockMvc.perform(get("/api/v1/roles/" + targetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions").isEmpty());
    }
}
