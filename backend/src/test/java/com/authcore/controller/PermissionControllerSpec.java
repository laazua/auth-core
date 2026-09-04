package com.authcore.controller;

import com.authcore.service.PermissionService;
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
 * PermissionController 依赖接口断言（AC3）。
 */
@DisplayName("PermissionController 依赖接口断言")
@SpringBootTest
@AutoConfigureMockMvc
class PermissionControllerSpec {

    @Autowired
    private PermissionController permissionController;

    @Test
    @DisplayName("Controller 构造器参数类型为 PermissionService 接口")
    void controllerInjectsServiceInterface() throws NoSuchMethodException {
        var constructor = PermissionController.class.getDeclaredConstructors()[0];
        var paramTypes = constructor.getParameterTypes();

        boolean hasPermissionServiceParam = Stream.of(paramTypes)
                .anyMatch(PermissionService.class::equals);
        assertTrue(hasPermissionServiceParam, "PermissionController 构造器应依赖 PermissionService 接口");
    }

    @Test
    @DisplayName("Controller 不直接依赖 PermissionServiceImpl")
    void controllerNotDirectlyDependsOnImpl() throws NoSuchMethodException {
        var constructor = PermissionController.class.getDeclaredConstructors()[0];
        var paramTypes = constructor.getParameterTypes();

        boolean hasImplParam = Stream.of(paramTypes)
                .anyMatch(c -> c.getSimpleName().equals("PermissionServiceImpl"));
        assertFalse(hasImplParam, "PermissionController 不应直接依赖 PermissionServiceImpl");
    }
}