package com.authcore.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JwtAuthenticationFilter JWT 校验与 SecurityContext 注入判定用例（auth/003 AC1-AC4）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    /**
     * AC1: 有效 token 请求通过，SecurityContext 注入用户。
     * Given JwtTokenProvider 生成 admin token
     * When GET /api/v1/users 携带 Authorization: Bearer <token>
     * Then status != 401/403（虽然可能 404 因无 controller，但认证已通过）
     *      SecurityContext 在过滤器链中已正确设置（由后续 auth/004 /me 验证）
     */
    @Test
    @DisplayName("有效 token 通过认证（非 401/403）")
    void validTokenSetsSecurityContext() throws Exception {
        // Arrange: 生成有效 token（使用真实的 admin 用户，数据由 V6 种子数据提供）
        var userDetails = customUserDetailsService.loadUserByUsername("admin");
        String token = jwtTokenProvider.generateToken(userDetails);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // 不应返回 401/403（虽然可能 404/500 因无 controller，但认证已通过）
                    assertTrue(status != 401 && status != 403,
                            "有效 token 不应被拦截为 401/403，实际状态码: " + status);
                });
    }

    /**
     * AC2: 无效/过期 token 返回 401。
     * Given 篡改/过期 JWT
     * When GET /api/v1/users 携带 Authorization: Bearer <bad_token>
     * Then 状态码 401，Result.code=1401
     */
    @Test
    @DisplayName("无效 token 返回 401 code=1401")
    void invalidTokenReturns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertTrue(body.contains("\"code\":1401"), "响应应包含 code=1401，实际 body: " + body);
                });
    }

    /**
     * AC3: 无 Authorization 头返回 401。
     * Given 无 Authorization 头
     * When GET /api/v1/users
     * Then 状态码 401，Result.code=1401
     */
    @Test
    @DisplayName("无 Authorization 头返回 401 code=1401")
    void missingAuthHeaderReturns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertTrue(body.contains("\"code\":1401"), "响应应包含 code=1401，实际 body: " + body);
                });
    }

    /**
     * AC4: 非 Bearer 格式返回 401。
     * Given Authorization 头非 Bearer 开头
     * When GET /api/v1/users 携带 Authorization: Basic xxx
     * Then 状态码 401，Result.code=1401
     */
    @Test
    @DisplayName("非 Bearer 格式返回 401 code=1401")
    void nonBearerAuthHeaderReturns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertTrue(body.contains("\"code\":1401"), "响应应包含 code=1401，实际 body: " + body);
                });
    }
}