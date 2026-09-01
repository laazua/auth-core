package com.authcore.test.integration;

import com.authcore.BaseIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseIntegrationTest 基座验证：
 * - MySQL 容器自动启动
 * - Flyway 自动迁移到最新版本
 * - 6 张核心表均已创建
 */
class BaseIntegrationTestSpec extends BaseIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    Flyway flyway;

    @Test
    @DisplayName("Testcontainers MySQL 容器启动且 Flyway 迁移到最新版本")
    void containerStartsAndFlywayMigrates() {
        // 验证容器已启动：能执行简单查询
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(one).isEqualTo(1);

        // 验证 Flyway 迁移已应用到最新版本（当前 6 个迁移脚本）
        String currentVersion = flyway.info().current().getVersion().toString();
        assertThat(currentVersion).isEqualTo("6");

        // 验证 6 张核心表存在
        assertTableExists("sys_user");
        assertTableExists("sys_role");
        assertTableExists("sys_module");
        assertTableExists("sys_permission");
        assertTableExists("sys_user_role");
        assertTableExists("sys_role_permission");
    }

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables " +
            "WHERE table_schema = DATABASE() AND table_name = ?",
            Integer.class, tableName);
        assertThat(count).as("表 %s 应存在", tableName).isEqualTo(1);
    }
}