package com.authcore.config.aop;

import com.authcore.config.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 日志切面测试。
 * 验证所有控制器接口调用都会输出操作日志（请求入参、耗时、响应结果、异常等）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiLoggingAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
     * 验证 GET 请求会被记录日志（含路径、参数、耗时、响应码）。
     */
    @Test
    @DisplayName("GET 请求记录操作日志")
    void getRequestLogsOperation() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 验证 POST 请求会被记录日志（含请求体、响应体、耗时）。
     */
    @Test
    @DisplayName("POST 请求记录操作日志")
    void postRequestLogsOperation() throws Exception {
        String token = getAdminToken();

        // 使用唯一用户名避免冲突
        String uniqueUsername = "log_test_user_" + System.currentTimeMillis();
        String createBody = """
                {
                    "username": "%s",
                    "password": "Pass1234",
                    "nickname": "日志测试用户",
                    "email": "logtest@test.com",
                    "phone": "13900009999"
                }
                """.formatted(uniqueUsername);

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 验证异常情况也会被记录日志（含异常信息、错误码）。
     */
    @Test
    @DisplayName("异常请求记录操作日志")
    void exceptionLogsOperation() throws Exception {
        String token = getAdminToken();

        // 请求不存在的用户，触发 BusinessException (code=1001)
        mockMvc.perform(get("/api/v1/users/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1001));
    }

    /**
     * 验证认证接口也会被记录日志。
     */
    @Test
    @DisplayName("认证接口记录操作日志")
    void authEndpointLogsOperation() throws Exception {
        String loginBody = """
                {
                    "username": "admin",
                    "password": "admin123456"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}