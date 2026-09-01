package com.authcore.test.controller;

import com.authcore.BaseIntegrationTest;
import com.authcore.util.AuthTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 控制器测试示例：使用 MockMvc + 真实库（继承 BaseIntegrationTest）。
 * 对应目录：src/test/java/com/authcore/test/controller/
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ControllerTestExampleSpec extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("控制器测试：健康检查端点无需认证")
    void healthEndpointNoAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("控制器测试：受保护端点需认证")
    void protectedEndpointRequiresAuth() throws Exception {
        // 无 token 访问受保护端点应返回 401
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("控制器测试：携带有效 Token 可访问受保护端点")
    void protectedEndpointWithValidToken() throws Exception {
        // 使用工具类获取有效 token
        String token = AuthTestUtil.obtainBearerToken(mockMvc, "admin", "admin123456");

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", token))
            .andExpect(status().isOk());
    }
}