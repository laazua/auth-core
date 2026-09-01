package com.authcore;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 集成测试基座：使用 Testcontainers MySQL 启动真实数据库，
 * 自动执行 Flyway 迁移，提供真实库上下文。
 * <p>
 * 所有集成测试类应继承此类，而非直接使用 {@code @SpringBootTest}。
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    /**
     * MySQL 8.0 容器：共享单例，跨测试类复用以加速。
     * 配置：
     * - 数据库名：authcore
     * - 用户名/密码：test/test
     * - 初始化脚本：由 Flyway 接管，不使用容器自带初始化
     */
    @Container
    private static final MySQLContainer<?> MYSQL =
        new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("authcore")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/migration/V1__init_schema.sql")
            .withReuse(true);

    /**
     * 动态注入数据源属性，覆盖 application.yml 中的配置。
     * 确保 Spring 上下文使用 Testcontainers 提供的数据库连接。
     */
    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        // 禁用 Flyway 自动执行，改为手动控制（在 BeforeAll 中）
        registry.add("spring.flyway.enabled", () -> "false");
    }

    /**
     * 容器启动后、Spring 上下文初始化前执行 Flyway 迁移。
     * 使用与应用相同的迁移脚本位置，确保 schema 一致性。
     */
    @BeforeAll
    static void migrateDatabase() {
        MYSQL.start();
        Flyway flyway = Flyway.configure()
            .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
            .locations("classpath:db/migration")
            .load();
        flyway.migrate();
    }
}