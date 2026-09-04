package com.authcore.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PermissionService 接口方法签名完整性验证（AC1）。
 */
@DisplayName("PermissionService 接口方法签名完整性")
class PermissionServiceInterfaceSpec {

    @Test
    @DisplayName("接口声明了全部 6 个公共方法")
    void permissionServiceInterfaceHasAllMethods() {
        Method[] methods = PermissionService.class.getDeclaredMethods();
        assertEquals(6, methods.length, "接口应声明 6 个公共方法");

        Set<String> methodNames = Stream.of(methods)
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(methodNames.contains("queryPermissions"), "应包含 queryPermissions 方法");
        assertTrue(methodNames.contains("queryPermissionsGroupedByModule"), "应包含 queryPermissionsGroupedByModule 方法");
        assertTrue(methodNames.contains("getPermissionById"), "应包含 getPermissionById 方法");
        assertTrue(methodNames.contains("createPermission"), "应包含 createPermission 方法");
        assertTrue(methodNames.contains("updatePermission"), "应包含 updatePermission 方法");
        assertTrue(methodNames.contains("deletePermission"), "应包含 deletePermission 方法");
    }

    @Test
    @DisplayName("PermissionService 是接口类型")
    void permissionServiceIsInterface() {
        assertTrue(PermissionService.class.isInterface(), "PermissionService 应为接口类型");
    }
}