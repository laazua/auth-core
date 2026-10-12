package com.authcore.controller;

import com.authcore.entity.SysUser;
import com.authcore.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户-角色绑定数据回显契约（web/046）。
 *
 * <p>前端列表角色列与角色分配抽屉消费 {@code UserVO.roles}；本类断言详情/列表响应
 * 携带已分配角色（含种子 admin→ROLE_ADMIN），为 web/047 放行与抽屉回显提供数据源契约。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRolesEchoContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper userMapper;

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

    private Long adminId() {
        SysUser admin = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "admin"));
        assertNotNull(admin, "种子库应存在 admin 用户");
        return admin.getId();
    }

    /**
     * AC1: 用户详情返回已分配角色。
     * Given 种子库 admin→ROLE_ADMIN（V6 sys_user_role）
     * When GET /api/v1/users/{adminId}
     * Then code=0、data.roles 为数组且元素含 id/name 与 code=ROLE_ADMIN
     */
    @Test
    @DisplayName("用户详情返回已分配角色且含ROLE_ADMIN")
    void userDetailReturnsRolesWithAdminRole() throws Exception {
        String token = getAdminToken();
        Long id = adminId();

        mockMvc.perform(get("/api/v1/users/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles[*].code", Matchers.hasItem("ROLE_ADMIN")))
                .andExpect(jsonPath("$.data.roles[*].id", Matchers.notNullValue()))
                .andExpect(jsonPath("$.data.roles[*].name", Matchers.notNullValue()));
    }

    /**
     * AC2: 用户列表命中行返回已分配角色。
     * When GET /api/v1/users?username=admin&page=1&size=10
     * Then records 中 username=admin 行含 roles 数组且元素 code 含 ROLE_ADMIN
     */
    @Test
    @DisplayName("用户列表命中行返回已分配角色")
    void userListRowReturnsRoles() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[?(@.username == 'admin')].roles[*].code",
                        Matchers.hasItem("ROLE_ADMIN")));
    }
}
