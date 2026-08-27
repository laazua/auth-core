package com.authcore.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Spring Security 无状态基线配置判定用例（auth/001 AC1-AC3）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    /**
     * AC1: 公开端点无需认证可访问。
     * Given Spring Boot 测试上下文
     * When GET /actuator/health、POST /api/v1/auth/login
     * Then 状态码非 403（Forbidden），且登录端点返回业务响应而非 Spring Security 认证挑战
     */
    @Test
    @DisplayName("公开端点 /actuator/health 与 /api/v1/auth/login 无需认证可访问")
    void publicEndpointsAccessibleWithoutAuth() throws Exception {
        // /actuator/health 应返回 200
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        // /api/v1/auth/login 无需认证：Spring Security 不应拦截（无 403），控制器可到达（返回业务码 1401 或 200/400）
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    String body = result.getResponse().getContentAsString();
                    // 403 表示被 Spring Security 拦截，不应出现
                    assertTrue(status != 403, "登录端点不应被 Spring Security 拦截 (403)，实际: " + status);
                    // 应返回业务响应（含 code 字段），而非 Spring Security 认证挑战页面
                    assertTrue(body.contains("\"code\""), "登录端点应返回业务响应格式，实际 body: " + body);
                });
    }

    /**
     * AC2: 受保护端点无 token 时返回 401。
     * Given 无 Authorization 头
     * When GET /api/v1/users
     * Then 状态码 401
     */
    @Test
    @DisplayName("受保护端点 /api/v1/users 无 token 返回 401")
    void protectedEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * AC3: BCryptPasswordEncoder 可用。
     * Given 注入 BCryptPasswordEncoder
     * When encode("test123") 生成哈希，matches("test123", hash)
     * Then matches 返回 true
     */
    @Test
    @DisplayName("BCryptPasswordEncoder 编码与校验正常")
    void bCryptEncoderWorks() {
        String raw = "test123";
        String hash = bCryptPasswordEncoder.encode(raw);
        assertTrue(bCryptPasswordEncoder.matches(raw, hash), "BCrypt 校验应通过");
        // strength=10 验证：哈希应以 $2a$10$ 开头
        assertTrue(hash.startsWith("$2a$10$"), "BCrypt strength 应为 10");
    }
}