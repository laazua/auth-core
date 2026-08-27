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

/**
 * Spring Security 无状态基线配置判定用例（auth/001 AC1-AC3）。
 */
@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "jwt.secret=test-secret-key-for-testing-only-minimum-32-chars"
})
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
     * Then 状态码非 401/403（即 200/400/404 等）
     */
    @Test
    @DisplayName("公开端点 /actuator/health 与 /api/v1/auth/login 无需认证可访问")
    void publicEndpointsAccessibleWithoutAuth() throws Exception {
        // /actuator/health 应返回 200
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        // /api/v1/auth/login 无需认证（登录控制器由 auth/002 实现，当前 404 也满足非 401/403）
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status != 401 && status != 403,
                            "登录端点不应返回 401/403，实际: " + status);
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