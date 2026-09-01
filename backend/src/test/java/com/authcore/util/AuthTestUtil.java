package com.authcore.util;

import com.authcore.common.Result;
import com.authcore.dto.auth.LoginRequest;
import com.authcore.dto.auth.LoginResponse;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证测试工具：快速获取 JWT、模拟登录、构建授权头。
 * 用于集成测试和控制器测试中需要认证的场景。
 */
public final class AuthTestUtil {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private AuthTestUtil() {}

    /**
     * 执行登录并返回完整的登录响应（含 token）。
     */
    public static Result<LoginResponse> login(MockMvc mockMvc, String username, String password) throws Exception {
        LoginRequest request = new LoginRequest(username, password);

        MvcResult result = mockMvc.perform(post(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonTestUtil.toJson(request)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonTestUtil.fromJson(
            result.getResponse().getContentAsString(),
            new com.fasterxml.jackson.core.type.TypeReference<Result<LoginResponse>>() {}
        );
    }

    /**
     * 执行登录并直接返回 JWT token 字符串。
     */
    public static String obtainJwt(MockMvc mockMvc, String username, String password) throws Exception {
        Result<LoginResponse> response = login(mockMvc, username, password);
        if (response.code() != 0 || response.data() == null) {
            throw new IllegalStateException("登录失败: " + response.message());
        }
        return response.data().token();
    }

    /**
     * 执行登录并返回 Bearer Token 字符串（含 "Bearer " 前缀）。
     */
    public static String obtainBearerToken(MockMvc mockMvc, String username, String password) throws Exception {
        return "Bearer " + obtainJwt(mockMvc, username, password);
    }

    /**
     * 构建带 Authorization 头的请求构建器。
     */
    public static ResultActions authorizedRequest(MockMvc mockMvc, String bearerToken, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) throws Exception {
        return mockMvc.perform(builder.header("Authorization", bearerToken));
    }

    /**
     * 快速发起带认证的 GET 请求。
     */
    public static ResultActions doGet(MockMvc mockMvc, String bearerToken, String url) throws Exception {
        return authorizedRequest(mockMvc, bearerToken, get(url));
    }

    /**
     * 快速发起带认证的 POST 请求（JSON body）。
     */
    public static ResultActions doPost(MockMvc mockMvc, String bearerToken, String url, Object body) throws Exception {
        return authorizedRequest(mockMvc, bearerToken,
            post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body != null ? JsonTestUtil.toJson(body) : ""));
    }

    /**
     * 快速发起带认证的 PUT 请求（JSON body）。
     */
    public static ResultActions doPut(MockMvc mockMvc, String bearerToken, String url, Object body) throws Exception {
        return authorizedRequest(mockMvc, bearerToken,
            put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body != null ? JsonTestUtil.toJson(body) : ""));
    }

    /**
     * 快速发起带认证的 DELETE 请求。
     */
    public static ResultActions doDelete(MockMvc mockMvc, String bearerToken, String url) throws Exception {
        return authorizedRequest(mockMvc, bearerToken, delete(url));
    }

    /**
     * 使用默认管理员账号登录并获取 Bearer Token。
     * 前提：种子数据中存在 admin/admin123456。
     */
    public static String adminBearerToken(MockMvc mockMvc) throws Exception {
        return obtainBearerToken(mockMvc, "admin", "admin123456");
    }
}