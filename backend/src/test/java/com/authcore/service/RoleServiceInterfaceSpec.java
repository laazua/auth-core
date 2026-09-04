package com.authcore.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RoleService 接口方法签名完整性验证（AC1）。
 */
@DisplayName("RoleService 接口方法签名完整性")
class RoleServiceInterfaceSpec {

    @Test
    @DisplayName("接口声明了全部 6 个公共方法")
    void roleServiceInterfaceHasAllMethods() {
        Method[] methods = RoleService.class.getDeclaredMethods();
        assertEquals(6, methods.length, "接口应声明 6 个公共方法");

        Set<String> methodNames = Stream.of(methods)
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(methodNames.contains("queryRoles"), "应包含 queryRoles 方法");
        assertTrue(methodNames.contains("getRoleById"), "应包含 getRoleById 方法");
        assertTrue(methodNames.contains("createRole"), "应包含 createRole 方法");
        assertTrue(methodNames.contains("updateRole"), "应包含 updateRole 方法");
        assertTrue(methodNames.contains("deleteRole"), "应包含 deleteRole 方法");
        assertTrue(methodNames.contains("assignPermissions"), "应包含 assignPermissions 方法");
    }

    @Test
    @DisplayName("RoleService 是接口类型")
    void roleServiceIsInterface() {
        assertTrue(RoleService.class.isInterface(), "RoleService 应为接口类型");
    }
}