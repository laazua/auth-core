package com.authcore.controller;

import com.authcore.service.RoleService;
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
 * RoleController 依赖接口断言（AC3）。
 */
@DisplayName("RoleController 依赖接口断言")
@SpringBootTest
@AutoConfigureMockMvc
class RoleControllerSpec {

    @Autowired
    private RoleController roleController;

    @Test
    @DisplayName("Controller 构造器参数类型为 RoleService 接口")
    void controllerInjectsServiceInterface() throws NoSuchMethodException {
        var constructor = RoleController.class.getDeclaredConstructors()[0];
        var paramTypes = constructor.getParameterTypes();

        boolean hasRoleServiceParam = Stream.of(paramTypes)
                .anyMatch(RoleService.class::equals);
        assertTrue(hasRoleServiceParam, "RoleController 构造器应依赖 RoleService 接口");
    }

    @Test
    @DisplayName("Controller 不直接依赖 RoleServiceImpl")
    void controllerNotDirectlyDependsOnImpl() throws NoSuchMethodException {
        var constructor = RoleController.class.getDeclaredConstructors()[0];
        var paramTypes = constructor.getParameterTypes();

        boolean hasImplParam = Stream.of(paramTypes)
                .anyMatch(c -> c.getSimpleName().equals("RoleServiceImpl"));
        assertFalse(hasImplParam, "RoleController 不应直接依赖 RoleServiceImpl");
    }
}