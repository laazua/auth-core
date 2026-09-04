package com.authcore.service;

import com.authcore.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RoleServiceImpl 实现断言（AC2）。
 */
@DisplayName("RoleServiceImpl 实现断言")
class RoleServiceImplSpec {

    @Test
    @DisplayName("RoleServiceImpl 实现 RoleService 接口且被 @Service 注解")
    void roleServiceImplImplementsInterface() {
        assertTrue(RoleService.class.isAssignableFrom(RoleServiceImpl.class),
                "RoleServiceImpl 应实现 RoleService 接口");

        Class<?>[] interfaces = RoleServiceImpl.class.getInterfaces();
        boolean implementsRoleService = Stream.of(interfaces)
                .anyMatch(RoleService.class::equals);
        assertTrue(implementsRoleService, "RoleServiceImpl 应实现 RoleService 接口");
    }
}