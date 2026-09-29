package com.authcore.controller;

import com.authcore.entity.SysModule;
import com.authcore.entity.SysPermission;
import com.authcore.mapper.SysModuleMapper;
import com.authcore.mapper.SysPermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GatewayController 模块（外部服务）统一访问入口判定用例（modules/002 AC1-AC3）。
 *
 * <p>下游 HTTP 由 {@code @MockitoBean RestTemplate} 拦截（仅 mock 外部 HTTP 依赖），
 * 准入裁决走真实 Security 过滤器 + 真实 DB 数据，转发断言在 mock 请求实体上逐项核对。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysModuleMapper moduleMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    @MockitoBean
    private RestTemplate restTemplate;

    private static final String GATEWAY = "/api/v1/gateway/";

    private final String uniquePrefix = "gw" + System.nanoTime() + "_";

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

    private SysModule moduleByCode(String code) {
        return moduleMapper.selectOne(new LambdaQueryWrapper<SysModule>().eq(SysModule::getCode, code));
    }

    private SysModule insertModule(String code, String baseUrl, Integer status) {
        SysModule module = new SysModule();
        module.setName(uniquePrefix + "服务");
        module.setCode(code);
        module.setBaseUrl(baseUrl);
        module.setDescription("gateway modules/002 测试模块");
        module.setStatus(status);
        moduleMapper.insert(module);
        return module;
    }

    private void insertPermission(String code, Long moduleId) {
        SysPermission permission = new SysPermission();
        permission.setModuleId(moduleId);
        permission.setName(uniquePrefix + "权限");
        permission.setCode(code);
        permission.setDescription("gateway modules/002 测试权限");
        permissionMapper.insert(permission);
    }

    /**
     * AC1: 未认证访问网关返回 401 code=1401 且不发起下游调用。
     * Given 无 Authorization / 伪造 Bearer token
     * When GET /api/v1/gateway/user_mgmt/detail
     * Then 两次均 401 + code=1401，下游 RestTemplate 零调用
     */
    @Test
    @DisplayName("未认证访问网关返回 401 code=1401 且不发起下游调用")
    void accessWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(GATEWAY + "user_mgmt/detail"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1401));

        mockMvc.perform(get(GATEWAY + "user_mgmt/detail")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1401));

        verify(restTemplate, never()).exchange(any(RequestEntity.class), eq(byte[].class));
    }

    /**
     * AC2: 通过准入的请求按模块 base_url 转发且响应原样透传（Authorization 不外泄）。
     * Given user_mgmt 模块 base_url=http://service-a:8081/ 且 admin 拥有 user:view
     * When POST /api/v1/gateway/user_mgmt/api/items?page=2（body {"x":1}）
     * Then 下游收到 POST http://service-a:8081/api/items?page=2（保留 Content-Type、无 Authorization、body 原样），
     *      网关把下游 201 + {"id":9} 原样透传
     */
    @Test
    @DisplayName("通过准入的请求按模块 base_url 转发且响应原样透传")
    void forwardsToModuleServiceWhenAuthorized() throws Exception {
        // Given: 种子模块 user_mgmt 指向下游服务地址（测试事务回滚不污染种子数据）
        SysModule module = moduleByCode("user_mgmt");
        module.setBaseUrl("http://service-a:8081/");
        moduleMapper.updateById(module);

        when(restTemplate.exchange(any(RequestEntity.class), eq(byte[].class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":9}".getBytes(StandardCharsets.UTF_8)));

        String token = getAdminToken();

        // When: 经网关访问模块服务
        mockMvc.perform(post(GATEWAY + "user_mgmt/api/items?page=2")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"x\":1}"))
                // Then: 响应状态码与响应体原样透传
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"id\":9}"));

        // Then: 下游收到的转发请求逐项核对
        org.mockito.ArgumentCaptor<RequestEntity> captor =
                org.mockito.ArgumentCaptor.forClass(RequestEntity.class);
        verify(restTemplate).exchange(captor.capture(), eq(byte[].class));
        RequestEntity<?> forwarded = captor.getValue();
        assertEquals(HttpMethod.POST, forwarded.getMethod(), "转发方法应为 POST");
        assertEquals(URI.create("http://service-a:8081/api/items?page=2"),
                forwarded.getUrl(), "目标 URL = base_url 归一 + 剩余路径 + 查询串");
        assertFalse(forwarded.getHeaders().containsKey("Authorization"), "Authorization 不得外泄给外部服务");
        assertEquals(MediaType.APPLICATION_JSON, forwarded.getHeaders().getContentType(), "Content-Type 应保留");
        assertEquals("{\"x\":1}",
                new String((byte[]) forwarded.getBody(), StandardCharsets.UTF_8), "请求体应原样转发");
    }

    /**
     * AC3-①: 用户对该模块无任何有效权限 → 403 code=1403 且不转发。
     * Given 模块启用但其下权限 admin 不持有
     * When GET /api/v1/gateway/{code}/data（admin token）
     * Then 403 + code=1403，下游 RestTemplate 零调用
     */
    @Test
    @DisplayName("准入拒绝：用户对模块无有效权限返回 403 code=1403 且不转发")
    void rejectsWhenUserHasNoModulePermission() throws Exception {
        // Given: 新模块 + admin 不持有的权限
        String code = uniquePrefix + "svc";
        SysModule module = insertModule(code, "http://service-a:8081/", 1);
        insertPermission(uniquePrefix + "perm", module.getId());

        String token = getAdminToken();

        // When/Then
        mockMvc.perform(get(GATEWAY + code + "/data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));
        verify(restTemplate, never()).exchange(any(RequestEntity.class), eq(byte[].class));
    }

    /**
     * AC3-②: 模块停用（其下权限按 §6.5 视为无效）→ 403 code=1403 且不转发。
     * Given role_mgmt status=0 且 admin 持有 role:view
     * When GET /api/v1/gateway/role_mgmt/data（admin token）
     * Then 403 + code=1403，下游 RestTemplate 零调用
     */
    @Test
    @DisplayName("准入拒绝：停用模块返回 403 code=1403 且不转发")
    void rejectsWhenModuleDisabled() throws Exception {
        // Given: 种子模块 role_mgmt 停用（测试事务回滚）
        SysModule module = moduleByCode("role_mgmt");
        module.setStatus(0);
        moduleMapper.updateById(module);

        String token = getAdminToken();

        // When/Then
        mockMvc.perform(get(GATEWAY + "role_mgmt/data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1403));
        verify(restTemplate, never()).exchange(any(RequestEntity.class), eq(byte[].class));
    }

    /**
     * AC3-③: 模块码不存在 → 400 code=1304 且不转发。
     * Given 不存在的模块码
     * When GET /api/v1/gateway/{code}/data（admin token）
     * Then 400 + code=1304，下游 RestTemplate 零调用
     */
    @Test
    @DisplayName("准入拒绝：模块不存在返回 400 code=1304 且不转发")
    void rejectsWhenModuleNotFound() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get(GATEWAY + uniquePrefix + "nope/data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1304));
        verify(restTemplate, never()).exchange(any(RequestEntity.class), eq(byte[].class));
    }

    /**
     * AC3-④: 模块 base_url 未配置 → 400 code=1305 且不转发。
     * Given perm_mgmt base_url=null 且 admin 持有 perm:view
     * When GET /api/v1/gateway/perm_mgmt/data（admin token）
     * Then 400 + code=1305，下游 RestTemplate 零调用
     */
    @Test
    @DisplayName("准入拒绝：模块服务地址未配置返回 400 code=1305 且不转发")
    void rejectsWhenBaseUrlMissing() throws Exception {
        // Given: 种子模块 perm_mgmt 清空 base_url（测试事务回滚）
        SysModule module = moduleByCode("perm_mgmt");
        module.setBaseUrl(null);
        moduleMapper.updateById(module);

        String token = getAdminToken();

        // When/Then
        mockMvc.perform(get(GATEWAY + "perm_mgmt/data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1305));
        verify(restTemplate, never()).exchange(any(RequestEntity.class), eq(byte[].class));
    }
}
