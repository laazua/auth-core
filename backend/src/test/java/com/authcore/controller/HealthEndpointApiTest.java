package com.authcore.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * actuator 健康检查端点 Web 层用例（infra/001 AC2 判定用例）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthEndpointApiTest {

    private final MockMvc mockMvc;

    HealthEndpointApiTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * Given 应用上下文已启动 When 请求 GET /actuator/health Then 返回 200 且 status=UP。
     *
     * @throws Exception MockMvc 调用异常
     */
    @Test
    @DisplayName("GET /actuator/health 返回 200 且 status 为 UP")
    void test_get_actuator_health_返回200且status为UP() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
