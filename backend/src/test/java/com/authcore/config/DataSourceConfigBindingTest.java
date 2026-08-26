package com.authcore.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据源与 Flyway 配置外置口径的判定用例（infra/002 AC3/AC4）。
 *
 * <p>以隔离的模拟环境变量加载主配置文件，验证占位符的覆盖与回落行为，
 * 全程不依赖真实 MySQL 实例。
 */
class DataSourceConfigBindingTest {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    /**
     * 构造隔离 Environment：系统环境变量源被替换为给定变量表，主配置文件置于最低优先级。
     *
     * @param envVars 模拟的系统环境变量
     * @return 已装配的 Spring Environment
     * @throws IOException 主配置文件缺失或解析失败
     */
    private Environment envWith(Map<String, Object> envVars) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        sources.replace("systemEnvironment",
                new SystemEnvironmentPropertySource("systemEnvironment", envVars));
        Resource yaml = new ClassPathResource("application.yml");
        loader.load("application.yml", yaml).forEach(sources::addLast);
        return environment;
    }

    /**
     * Given 环境变量提供三要素 When 解析主配置 Then 占位符取环境变量值；
     * 对照组：Given 环境变量缺省 Then 回落到默认值且默认指向本地库。
     *
     * @throws IOException 配置缺失
     */
    @Test
    @DisplayName("数据源占位：环境变量覆盖，缺省回落默认值")
    void test_环境变量覆盖数据源默认配置() throws IOException {
        Environment overridden = envWith(Map.of(
                "MYSQL_URL", "jdbc:mysql://env-host:3307/envdb",
                "MYSQL_USERNAME", "env-user",
                "MYSQL_PASSWORD", "env-pass"));
        assertEquals("jdbc:mysql://env-host:3307/envdb",
                overridden.getProperty("spring.datasource.url"));
        assertEquals("env-user", overridden.getProperty("spring.datasource.username"));
        assertEquals("env-pass", overridden.getProperty("spring.datasource.password"));

        Environment fallback = envWith(Map.of());
        // 团队共享开发默认值（host/user）允许入库，但断言不钉死环境特定 host；
        // 仅校验协议、库名与用户名占位生效（评审意见问题 2 的处置口径 b）
        String fallbackUrl = fallback.getProperty("spring.datasource.url");
        assertTrue(fallbackUrl.startsWith("jdbc:mysql://"), "默认 url 应为合法 jdbc 前缀");
        assertTrue(fallbackUrl.contains("/authcore?"), "默认库应为 authcore");
        assertEquals("root", fallback.getProperty("spring.datasource.username"));
    }

    /**
     * Given FLYWAY_ENABLED=true When 解析 Then flyway 开启；
     * Given 缺省 Then 关闭（无实例可启动）且迁移目录固定 classpath:db/migration。
     *
     * @throws IOException 配置缺失
     */
    @Test
    @DisplayName("Flyway 开关绑定 FLYWAY_ENABLED 且迁移目录固定")
    void test_flyway开关与迁移目录绑定() throws IOException {
        assertEquals("true", envWith(Map.of("FLYWAY_ENABLED", "true"))
                .getProperty("spring.flyway.enabled"));
        assertEquals("false", envWith(Map.of())
                .getProperty("spring.flyway.enabled"));
        assertEquals("classpath:db/migration", envWith(Map.of())
                .getProperty("spring.flyway.locations"));
    }
}
