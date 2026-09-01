package com.authcore.test.integration;

import com.authcore.BaseIntegrationTest;
import com.authcore.entity.SysUser;
import com.authcore.util.AuthTestUtil;
import com.authcore.util.JsonTestUtil;
import com.authcore.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试工具类验证：
 * - TestDataBuilder 构建实体
 * - JsonTestUtil 序列化/反序列化/断言
 * - AuthTestUtil 获取 JWT
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TestUtilsSpec extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("TestDataBuilder: 构建合法 SysUser 实体")
    void buildUserEntity() {
        SysUser user = TestDataBuilder.user()
            .withUsername("builder_test")
            .withNickname("构建器测试")
            .withEmail("builder@test.com")
            .build();

        assertThat(user.getUsername()).isEqualTo("builder_test");
        assertThat(user.getNickname()).isEqualTo("构建器测试");
        assertThat(user.getEmail()).isEqualTo("builder@test.com");
        assertThat(user.getStatus()).isEqualTo(1);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("TestDataBuilder: 批量构建")
    void batchBuildUsers() {
        var users = TestDataBuilder.users(3, b -> b.withNickname("批量"));
        assertThat(users).hasSize(3);
        assertThat(users.get(0).getUsername()).startsWith("batch_user_");
    }

    @Test
    @DisplayName("JsonTestUtil: 序列化/反序列化/相等断言")
    void jsonSerialization() {
        SysUser user = TestDataBuilder.user().withUsername("json_test").build();
        String json = JsonTestUtil.toJson(user);
        assertThat(json).contains("json_test");

        SysUser parsed = JsonTestUtil.fromJson(json, SysUser.class);
        assertThat(parsed.getUsername()).isEqualTo("json_test");

        JsonTestUtil.assertJsonEquals(user, parsed);
    }

    @Test
    @DisplayName("JsonTestUtil: 包含片段断言")
    void jsonContains() {
        String json = JsonTestUtil.toJson(TestDataBuilder.user().withUsername("frag_test").build());
        JsonTestUtil.assertJsonContains(json, "username", "frag_test");
    }

    @Test
    @DisplayName("AuthTestUtil: 获取有效 Bearer Token")
    void obtainJwt() throws Exception {
        // 使用种子数据中的 admin 账号
        String token = AuthTestUtil.obtainJwt(mockMvc, "admin", "admin123456");
        assertThat(token).isNotBlank();
        assertThat(token).doesNotContain("Bearer");

        String bearerToken = AuthTestUtil.obtainBearerToken(mockMvc, "admin", "admin123456");
        assertThat(bearerToken).startsWith("Bearer ");
    }
}