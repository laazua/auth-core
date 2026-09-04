package com.authcore.service;

import com.authcore.service.impl.ModuleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ModuleServiceImpl 实现断言（AC2）。
 */
@DisplayName("ModuleServiceImpl 实现断言")
class ModuleServiceImplSpec {

    @Test
    @DisplayName("ModuleServiceImpl 实现 ModuleService 接口且被 @Service 注解")
    void moduleServiceImplImplementsInterface() {
        assertTrue(ModuleService.class.isAssignableFrom(ModuleServiceImpl.class),
                "ModuleServiceImpl 应实现 ModuleService 接口");

        Class<?>[] interfaces = ModuleServiceImpl.class.getInterfaces();
        boolean implementsModuleService = Stream.of(interfaces)
                .anyMatch(ModuleService.class::equals);
        assertTrue(implementsModuleService, "ModuleServiceImpl 应实现 ModuleService 接口");
    }
}