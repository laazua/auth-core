package com.authcore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 时间字段 JSON 契约测试（web/039 createTime 三域 / web/040a updateTime 三域 / web/040c me 面）。
 * 前端全站读 createTime/updateTime（users/roles/profile IndexView 与 types/*），
 * 而 VO 输出 createdAt/updatedAt 致对应列/字段恒显空；
 * 本类以 HTTP 层断言三域列表、详情与 /auth/me 的输出键名，驱动各 VO 加方案 B 注解。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TimestampFieldContractTest {

    @Autowired
    private MockMvc mockMvc;

    private static final ObjectMapper JSON = new ObjectMapper();

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

        return JSON.readTree(loginResponse).path("data").path("token").asText();
    }

    private String getListJson(String path, String token) throws Exception {
        return mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /**
     * AC1: 三域列表输出 createTime 且不输出 createdAt。
     * Given 管理员登录（分页第一页由种子数据保证非空）。
     * When GET /users、/roles、/permissions 列表。
     * Then 各域首项含 createTime 键且不含 createdAt 键。
     */
    @Test
    @DisplayName("三域列表输出 createTime 不输出 createdAt")
    void listsOutputCreateTimeForUserRolePermissionDomain() throws Exception {
        // Given
        String token = getAdminToken();

        // When & Then
        for (String path : new String[]{"/api/v1/users", "/api/v1/roles", "/api/v1/permissions"}) {
            String json = getListJson(path, token);
            var records = JSON.readTree(json).path("data").path("records");
            org.junit.jupiter.api.Assertions.assertTrue(records.size() > 0,
                    path + " 分页第一页应非空");
            org.junit.jupiter.api.Assertions.assertTrue(records.get(0).has("createTime"),
                    path + " 列表项应含 createTime 键");
            org.junit.jupiter.api.Assertions.assertFalse(records.get(0).has("createdAt"),
                    path + " 列表项不应含 createdAt 键");
        }
    }

    /**
     * AC2: 三域详情输出 createTime 且不输出 createdAt。
     * Given 管理员登录且列表可取首项 id。
     * When GET /users/{id}、/roles/{id}、/permissions/{id}。
     * Then 各域详情 data 含 createTime 键且不含 createdAt 键。
     */
    @Test
    @DisplayName("三域详情输出 createTime 不输出 createdAt")
    void detailOutputsCreateTimeForUserRolePermissionDomain() throws Exception {
        // Given
        String token = getAdminToken();

        // When & Then
        for (String path : new String[]{"/api/v1/users", "/api/v1/roles", "/api/v1/permissions"}) {
            long id = JSON.readTree(getListJson(path, token))
                    .path("data").path("records").get(0).path("id").asLong();
            mockMvc.perform(get(path + "/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.createTime").exists())
                    .andExpect(jsonPath("$.data.createdAt").doesNotExist());
        }
    }

    /**
     * AC1(web/040a): 三域列表输出 updateTime 且不输出 updatedAt。
     * Given 管理员登录（分页第一页由种子数据保证非空）。
     * When GET /users、/roles、/permissions 列表。
     * Then 各域首项含 updateTime 键且不含 updatedAt 键（与 createTime 键并存）。
     */
    @Test
    @DisplayName("三域列表输出 updateTime 不输出 updatedAt")
    void listsOutputUpdateTimeForUserRolePermissionDomain() throws Exception {
        // Given
        String token = getAdminToken();

        // When & Then
        for (String path : new String[]{"/api/v1/users", "/api/v1/roles", "/api/v1/permissions"}) {
            String json = getListJson(path, token);
            var records = JSON.readTree(json).path("data").path("records");
            org.junit.jupiter.api.Assertions.assertTrue(records.size() > 0,
                    path + " 分页第一页应非空");
            org.junit.jupiter.api.Assertions.assertTrue(records.get(0).has("createTime"),
                    path + " 列表项应含 createTime 键（web/039 既有契约保持）");
            org.junit.jupiter.api.Assertions.assertTrue(records.get(0).has("updateTime"),
                    path + " 列表项应含 updateTime 键");
            org.junit.jupiter.api.Assertions.assertFalse(records.get(0).has("updatedAt"),
                    path + " 列表项不应含 updatedAt 键");
        }
    }

    /**
     * AC2(web/040a): 三域详情输出 updateTime 且不输出 updatedAt。
     * Given 管理员登录且列表可取首项 id。
     * When GET 三域 /{id} 详情。
     * Then 详情 data 含 updateTime 键且不含 updatedAt 键。
     * 注：/auth/me 的 me 面因 MeResponse 内嵌无时间字段的 dto.auth.UserVO（6 参），
     * 需 +2 文件超本单 6 文件熔断，用户裁决拆 040c（见挂起区 2026-09-29 Generator 登记）。
     */
    @Test
    @DisplayName("三域详情输出 updateTime 不输出 updatedAt")
    void detailOutputsUpdateTimeForUserRolePermissionDomain() throws Exception {
        // Given
        String token = getAdminToken();

        // When & Then
        for (String path : new String[]{"/api/v1/users", "/api/v1/roles", "/api/v1/permissions"}) {
            long id = JSON.readTree(getListJson(path, token))
                    .path("data").path("records").get(0).path("id").asLong();
            mockMvc.perform(get(path + "/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.updateTime").exists())
                    .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
        }
    }

    /**
     * AC1(web/040c): /auth/me 的 user 输出 createTime 与 updateTime 且不输出 createdAt/updatedAt。
     * Given 管理员登录。
     * When GET /api/v1/auth/me。
     * Then data.user 含 createTime 与 updateTime（非空 ISO-8601）且不含 createdAt 与 updatedAt
     * —— 修复个人中心创建/更新时间恒空（dto.auth.UserVO 同名异类根因，web/040c）。
     */
    @Test
    @DisplayName("me 的 user 输出 createTime 与 updateTime 不输出 createdAt/updatedAt")
    void meOutputsCreateTimeAndUpdateTime() throws Exception {
        // Given
        String token = getAdminToken();

        // When & Then
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.createTime").exists())
                .andExpect(jsonPath("$.data.user.updateTime").exists())
                .andExpect(jsonPath("$.data.user.createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.user.updatedAt").doesNotExist());
    }
}
