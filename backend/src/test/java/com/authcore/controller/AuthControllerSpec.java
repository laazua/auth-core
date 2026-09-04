package com.authcore.controller;

import com.authcore.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthController 依赖接口断言（AC3）。
 */
@DisplayName("AuthController 依赖接口断言")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerSpec {

    @Autowired
    private AuthController authController;

    @Test
    @DisplayName("Controller 构造器参数类型为 AuthService 接口")
    void controllerInjectsServiceInterface() throws NoSuchMethodException {
        var constructor = AuthController.class.getDeclaredConstructors()[0];
        var paramTypes = constructor.getParameterTypes();

        boolean hasAuthServiceParam = Stream.of(paramTypes)
                .anyMatch(AuthService.class::equals);
        assertTrue(hasAuthServiceParam, "AuthController 构造器应依赖 AuthService 接口");
    }

    @Test
    @DisplayName("Controller 不直接依赖 AuthServiceImpl")
    void controllerNotDirectlyDependsOnImpl() throws NoSuchMethodException {
        var constructor = AuthController.class.getDeclaredConstructors()[0];
        var paramTypes = constructor.getParameterTypes();

        boolean hasImplParam = Stream.of(paramTypes)
                .anyMatch(c -> c.getSimpleName().equals("AuthServiceImpl"));
        assertFalse(hasImplParam, "AuthController 不应直接依赖 AuthServiceImpl");
    }
}