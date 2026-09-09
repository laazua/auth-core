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
 * UserService 接口方法签名完整性验证（AC1）。
 */
@DisplayName("UserService 接口方法签名完整性")
class UserServiceInterfaceSpec {

    @Test
    @DisplayName("接口声明了全部 9 个公共方法")
    void userServiceInterfaceHasAllMethods() {
        Method[] methods = UserService.class.getDeclaredMethods();
        assertEquals(9, methods.length, "接口应声明 9 个公共方法");

        Set<String> methodNames = Stream.of(methods)
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(methodNames.contains("queryUsers"), "应包含 queryUsers 方法");
        assertTrue(methodNames.contains("getUserById"), "应包含 getUserById 方法");
        assertTrue(methodNames.contains("createUser"), "应包含 createUser 方法");
        assertTrue(methodNames.contains("updateUser"), "应包含 updateUser 方法");
        assertTrue(methodNames.contains("toggleStatus"), "应包含 toggleStatus 方法");
        assertTrue(methodNames.contains("deleteUser"), "应包含 deleteUser 方法");
        assertTrue(methodNames.contains("assignRoles"), "应包含 assignRoles 方法");
        assertTrue(methodNames.contains("changePassword"), "应包含 changePassword 方法");
        assertTrue(methodNames.contains("resetPassword"), "应包含 resetPassword 方法");
    }

    @Test
    @DisplayName("UserService 是接口类型")
    void userServiceIsInterface() {
        assertTrue(UserService.class.isInterface(), "UserService 应为接口类型");
    }
}