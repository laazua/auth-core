package com.authcore.controller;

import com.authcore.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserController 依赖接口断言（AC3）。
 */
@DisplayName("UserController 依赖接口断言")
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerSpec {

    @Autowired
    private UserController userController;

    @Test
    @DisplayName("Controller 构造器参数类型为 UserService 接口")
    void controllerInjectsServiceInterface() throws NoSuchMethodException {
        var constructor = UserController.class.getDeclaredConstructors()[0];
        var paramTypes = constructor.getParameterTypes();

        boolean hasUserServiceParam = Stream.of(paramTypes)
                .anyMatch(UserService.class::equals);
        assertTrue(hasUserServiceParam, "UserController 构造器应依赖 UserService 接口");
    }

    @Test
    @DisplayName("Controller 不直接依赖 UserServiceImpl")
    void controllerNotDirectlyDependsOnImpl() throws NoSuchMethodException {
        var constructor = UserController.class.getDeclaredConstructors()[0];
        var paramTypes = constructor.getParameterTypes();

        boolean hasImplParam = Stream.of(paramTypes)
                .anyMatch(c -> c.getSimpleName().equals("UserServiceImpl"));
        assertFalse(hasImplParam, "UserController 不应直接依赖 UserServiceImpl");
    }
}