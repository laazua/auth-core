package com.authcore.test.integration;

import com.authcore.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 三层测试分离验证：
 * - 单元测试：不启动容器，Mock 生效，快速
 * - 集成测试：启动容器，真实库，事务回滚隔离
 * - 控制器测试：MockMvc + 真实库
 * <p>
 * 注意：单元测试和控制器测试的验证需在对应目录下的测试类中执行。
 * 此类仅验证集成测试层真实库可用。
 */
@SpringBootTest
@ActiveProfiles("test")
class TestLayersSpec extends BaseIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("集成测试：使用真实数据库，可写入查询")
    void integrationTestUsesRealDB() {
        // 真实写入
        jdbcTemplate.update("INSERT INTO sys_user (username, password, nickname, email, status) VALUES (?, ?, ?, ?, ?)",
            "it_user_1", "pwd", "集成测试用户", "it1@test.com", 1);

        // 真实查询
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE username = ?", Integer.class, "it_user_1");
        assertThat(count).isEqualTo(1);
    }
}