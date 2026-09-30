package com.authcore.controller;

import com.authcore.entity.SysModule;
import com.authcore.mapper.SysModuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 模块 JSON 字段契约测试（web/038c）。
 * 前端提交形状为 camel（ModuleFormDrawer baseUrl）与 createTime（IndexView 列表列），
 * 本类以 HTTP 层断言出入参键名契约，驱动 DTO/VO 修复。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModuleFieldContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysModuleMapper moduleMapper;

    private static final String TEST_PREFIX = "mfc_" + System.currentTimeMillis() + "_";

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
            new com.fasterxml.jackson.databind.ObjectMapper();

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

        return JSON.readTree(loginResponse).path("data").path("token").asText();
    }

    private Long createModuleDirectly(String name, String code) {
        SysModule module = new SysModule();
        module.setName(name);
        module.setCode(code);
        module.setStatus(1);
        moduleMapper.insert(module);
        return module.getId();
    }

    /**
     * AC1: 创建入参 camel baseUrl 正确入库回读。
     * Given 管理员登录。When POST 载荷含 "baseUrl"（同前端 ModuleFormDrawer 提交形状）。
     * Then 200 后 GET 详情 baseUrl 等于提交值（不再被静默丢弃）。
     */
    @Test
    @DisplayName("创建入参 camel baseUrl 正确入库回读")
    void createAcceptsCamelBaseUrlAndReadsBack() throws Exception {
        // Given
        String token = getAdminToken();
        String name = unique("创建契约");
        String code = unique("MFC_CREATE").toUpperCase();
        String createBody = """
                {
                    "name": "%s",
                    "code": "%s",
                    "baseUrl": "http://camel-input",
                    "description": "契约测试模块",
                    "status": 1
                }
                """.formatted(name, code);

        // When
        String createResponse = mockMvc.perform(post("/api/v1/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long moduleId = JSON.readTree(createResponse).path("data").path("id").asLong();

        // Then
        mockMvc.perform(get("/api/v1/modules/" + moduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseUrl").value("http://camel-input"));
    }

    /**
     * AC2: 更新入参 camel baseUrl 正确入库回读。
     * Given 管理员登录且存在模块。When PUT 载荷含 "baseUrl"。
     * Then 200 后详情 baseUrl 等于更新值。
     */
    @Test
    @DisplayName("更新入参 camel baseUrl 正确入库回读")
    void updateAcceptsCamelBaseUrlAndReadsBack() throws Exception {
        // Given
        String token = getAdminToken();
        Long moduleId = createModuleDirectly(unique("更新契约"), unique("MFC_UPDATE").toUpperCase());
        String updateBody = """
                {
                    "name": "%s",
                    "baseUrl": "http://camel-updated",
                    "description": "契约更新描述",
                    "status": 1
                }
                """.formatted(unique("更新契约改"));

        // When
        mockMvc.perform(put("/api/v1/modules/" + moduleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.baseUrl").value("http://camel-updated"));

        // Then
        mockMvc.perform(get("/api/v1/modules/" + moduleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseUrl").value("http://camel-updated"));
    }

    /**
     * AC3: 响应输出 createTime 且不再输出 createdAt。
     * Given 管理员登录。When GET 列表与 GET 详情。
     * Then 列表项与详情均含 createTime 且不含 createdAt 键（前端 IndexView 直读可用）。
     */
    @Test
    @DisplayName("列表与详情输出 createTime 不再输出 createdAt")
    void listAndDetailSerializeCreateTimeNotCreatedAt() throws Exception {
        // Given
        String token = getAdminToken();

        // When: 列表
        mockMvc.perform(get("/api/v1/modules")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].createTime").exists())
                .andExpect(jsonPath("$.data.records[0].createdAt").doesNotExist());

        // Then: 详情
        mockMvc.perform(get("/api/v1/modules/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createTime").exists())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist());
    }
}
