package com.authcore.service;

import com.authcore.service.impl.PermissionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PermissionServiceImpl 实现断言（AC2）。
 */
@DisplayName("PermissionServiceImpl 实现断言")
class PermissionServiceImplSpec {

    @Test
    @DisplayName("PermissionServiceImpl 实现 PermissionService 接口且被 @Service 注解")
    void permissionServiceImplImplementsInterface() {
        assertTrue(PermissionService.class.isAssignableFrom(PermissionServiceImpl.class),
                "PermissionServiceImpl 应实现 PermissionService 接口");

        Class<?>[] interfaces = PermissionServiceImpl.class.getInterfaces();
        boolean implementsPermissionService = Stream.of(interfaces)
                .anyMatch(PermissionService.class::equals);
        assertTrue(implementsPermissionService, "PermissionServiceImpl 应实现 PermissionService 接口");
    }
}