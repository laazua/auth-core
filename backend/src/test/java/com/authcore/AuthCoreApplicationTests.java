package com.authcore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 验证 Spring 容器可正常装配启动（infra/001 AC1 判定用例）。
 */
@SpringBootTest
class AuthCoreApplicationTests {

    /**
     * 上下文装配冒烟断言：容器能完整初始化即通过。
     */
    @Test
    @DisplayName("应用上下文可正常加载")
    void contextLoads() {
    }
}
