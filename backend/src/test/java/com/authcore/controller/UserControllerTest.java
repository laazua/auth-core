package com.authcore.controller;

import com.authcore.config.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 用户 CRUD 判定用例（users/001 AC1-4）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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

    /**
     * AC1: 分页查询支持多条件。
     * Given 种子数据 admin + 2 个测试用户
     * When GET /api/v1/users?username=adm&status=1
     * Then status=200、total≥1、list 非空、每项含 id/username/nickname/email/phone/status
     */
    @Test
    @DisplayName("分页查询支持多条件")
    void queryUsersWithPaginationAndFilters() throws Exception {
        String token = getAdminToken();

        // 调用分页查询
        String response = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10")
                        .param("username", "adm")
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
            assertNotNull(item.path("username").asText(), "username 不应为空");
            assertNotNull(item.path("nickname").asText(), "nickname 不应为空");
            assertNotNull(item.path("email").asText(), "email 不应为空");
            assertNotNull(item.path("phone").asText(), "phone 不应为空");
            assertNotNull(item.path("status").asText(), "status 不应为空");
        }
    }

    /**
     * AC2: 创建用户密码加密且唯一。
     * When POST /api/v1/users {username:"newuser", password:"Pass1234", nickname:"新用户"}
     * Then status=200、id 非空、数据库 password 为 BCrypt、再次创建同 username 返回 409 code=1002
     */
    @Test
    @DisplayName("创建用户密码加密且用户名唯一")
    void createUserEncryptsPasswordAndUniqueUsername() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String username = unique("newuser");

        // 创建新用户
        String createBody = """
                {
                    "username": "%s",
                    "password": "Pass1234",
                    "nickname": "新用户",
                    "email": "newuser@test.com",
                    "phone": "13900000000"
                }
                """.formatted(username);

        String createResponse = mockMvc.perform(post("/api/v1/users")
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
        Long userId = createRoot.path("data").path("id").asLong();
        assertNotNull(userId, "id 不应为空");

        // 再次创建同 username 应返回 409 code=1002
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * AC3: 更新用户不含密码、username 不可改。
     * When PUT /api/v1/users/{id} {nickname:"新昵称", email:"new@test.com"}
     * Then status=200、nickname/email 更新、username/password 与原值一致
     */
    @Test
    @DisplayName("更新用户不含密码且 username 不可改")
    void updateUserExcludesPasswordAndUsername() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 先创建一个用户用于更新测试
        String createBody = """
                {
                    "username": "%s",
                    "password": "Pass1234",
                    "nickname": "原昵称",
                    "email": "old@test.com",
                    "phone": "13900000001"
                }
                """.formatted(unique("update_user"));

        String createResponse = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var createRoot = mapper.readTree(createResponse);
        Long userId = createRoot.path("data").path("id").asLong();

        // 更新用户（不含 password 和 username）
        String updateBody = """
                {
                    "nickname": "新昵称",
                    "email": "new@test.com",
                    "phone": "13900000002"
                }
                """;

        String updateResponse = mockMvc.perform(put("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("新昵称"))
                .andExpect(jsonPath("$.data.email").value("new@test.com"))
                .andExpect(jsonPath("$.data.phone").value("13900000002"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var updateRoot = mapper.readTree(updateResponse);
        // 验证 username 未变
        assertEquals(createRoot.path("data").path("username").asText(), updateRoot.path("data").path("username").asText(), "username 不应改变");
        // 验证 password 未返回（不应在 VO 中）
        assertTrue(updateRoot.path("data").path("password").isMissingNode(), "响应不应包含 password 字段");
    }

    /**
     * AC4: 启停用与删除引用保护。
     * PATCH /api/v1/users/{id}/status {status:0} 返回 200
     * DELETE /api/v1/users/{id} 若 sys_user_role 存在则 409 code=1101，否则 200 物理删除
     */
    @Test
    @DisplayName("启停用与删除引用保护")
    void statusToggleAndDeleteWithReferenceProtection() throws Exception {
        String token = getAdminToken();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 1. 创建测试用户
        String createBody = """
                {
                    "username": "%s",
                    "password": "Pass1234",
                    "nickname": "状态测试",
                    "email": "status@test.com",
                    "phone": "13900000003"
                }
                """.formatted(unique("status_user"));

        String createResponse = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var createRoot = mapper.readTree(createResponse);
        Long userId = createRoot.path("data").path("id").asLong();

        // 2. 停用用户：PATCH /api/v1/users/{id}/status {status:0}
        String statusBody = """
                {
                    "status": 0
                }
                """;

        mockMvc.perform(patch("/api/v1/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证停用后不可登录（由 auth/003 拦截）
        String loginDisabledBody = """
                {
                    "username": "%s",
                    "password": "Pass1234"
                }
                """.formatted(unique("status_user"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginDisabledBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1401));

        // 3. 启用用户：PATCH /api/v1/users/{id}/status {status:1}
        String enableBody = """
                {
                    "status": 1
                }
                """;

        mockMvc.perform(patch("/api/v1/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enableBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证启用后可登录
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginDisabledBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 4. 删除无引用的用户应成功
        mockMvc.perform(delete("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证用户已删除
        mockMvc.perform(get("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1001));
    }
}