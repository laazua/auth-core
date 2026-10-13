package com.authcore.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 权限树数据源契约（web/048a）。
 *
 * <p>前端角色分配抽屉消费 {@code GET /permissions/tree} 为 {@code PermissionTreeNode[]}
 * （PermissionVO + children 嵌套数组）；本类断言 tree 可消费结构、既有接口零回归与
 * 平铺结构护栏（id 唯一、children 恒空、按 moduleId,id 排序）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermissionTreeContractTest {

    @Autowired
    private MockMvc mockMvc;

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

        var mapper = new ObjectMapper();
        var root = mapper.readTree(loginResponse);
        return root.path("data").path("token").asText();
    }

    /**
     * AC1: tree 接口返回可消费结构。
     * Given 种子 17 权限（V6/V7）
     * When GET /api/v1/permissions/tree
     * Then HTTP 200、code=0、data 为数组、含 code=user:view 且 moduleId 非空
     * 修复前：路径落 GET /{id} → "tree" 转 Long 失败 → 500。
     */
    @Test
    @DisplayName("树接口返回可消费结构且含种子权限")
    void treeReturnsConsumableStructureWithSeedPermission() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/api/v1/permissions/tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.code == 'user:view')].moduleId").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.code == 'user:view')].moduleId[0]",
                        Matchers.not(Matchers.anyOf(Matchers.nullValue(), Matchers.is(0L)))))
                .andExpect(jsonPath("$.data[0].children").isArray());
    }

    /**
     * AC2: 既有分页与 grouped 接口零回归。
     * When GET /api/v1/permissions?page=1&size=10 与 GET /api/v1/permissions/grouped
     * Then 分页 code=0、records 数组、命中行既有键不变；grouped code=0 且为对象
     */
    @Test
    @DisplayName("既有分页与grouped接口保持可用")
    void existingPagedAndGroupedEndpointsRemainAvailable() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/api/v1/permissions")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].id").exists())
                .andExpect(jsonPath("$.data.records[0].moduleId").exists())
                .andExpect(jsonPath("$.data.records[0].moduleName").exists())
                .andExpect(jsonPath("$.data.records[0].name").exists())
                .andExpect(jsonPath("$.data.records[0].code").exists())
                .andExpect(jsonPath("$.data.records[0].createTime").exists())
                .andExpect(jsonPath("$.data.records[0].updateTime").exists())
                .andExpect(jsonPath("$.data.records[?(@.code == 'user:view')]").value(Matchers.hasSize(1)));

        mockMvc.perform(get("/api/v1/permissions/grouped")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isMap());
    }

    /**
     * AC3: 平铺结构护栏。
     * When tree 响应
     * Then 数组长度≥17、children 全为空数组、id 全局唯一、按 moduleId,id 升序
     */
    @Test
    @DisplayName("树为权限平铺且children为空与id唯一")
    void treeIsFlatPermissionsWithEmptyChildrenAndUniqueIds() throws Exception {
        String token = getAdminToken();

        MvcResult result = mockMvc.perform(get("/api/v1/permissions/tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode data = new ObjectMapper().readTree(result.getResponse().getContentAsString()).path("data");
        assertTrue(data.isArray(), "data 应为数组");
        assertTrue(data.size() >= 17, "平铺节点数应 ≥ 种子权限 17，实际 " + data.size());

        List<Long> ids = new ArrayList<>();
        long prevModuleId = -1;
        long prevId = -1;
        for (JsonNode node : data) {
            assertTrue(node.path("children").isArray(), "children 应为数组: " + node);
            assertEquals(0, node.path("children").size(), "平铺节点 children 应为空: " + node);
            long moduleId = node.path("moduleId").asLong();
            long id = node.path("id").asLong();
            if (prevModuleId >= 0) {
                assertTrue(moduleId > prevModuleId
                                || (moduleId == prevModuleId && id > prevId),
                        "应按 moduleId,id 升序");
            }
            prevModuleId = moduleId;
            prevId = id;
            ids.add(id);
        }
        assertEquals(ids.size(), new HashSet<>(ids).size(), "id 应全局唯一");
        Set<Long> idSet = new HashSet<>(ids);
        assertEquals(ids.size(), idSet.size(), "id 去重后应与总数一致");
    }
}
