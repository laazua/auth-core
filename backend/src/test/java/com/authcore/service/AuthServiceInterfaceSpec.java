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
 * AuthService 接口方法签名完整性验证（AC1）。
 */
@DisplayName("AuthService 接口方法签名完整性")
class AuthServiceInterfaceSpec {

    @Test
    @DisplayName("接口声明了全部 2 个公共方法")
    void authServiceInterfaceHasAllMethods() {
        Method[] methods = AuthService.class.getDeclaredMethods();
        assertEquals(2, methods.length, "接口应声明 2 个公共方法");

        Set<String> methodNames = Stream.of(methods)
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(methodNames.contains("getCurrentUserInfo"), "应包含 getCurrentUserInfo 方法");
        assertTrue(methodNames.contains("checkPermission"), "应包含 checkPermission 方法");
    }

    @Test
    @DisplayName("AuthService 是接口类型")
    void authServiceIsInterface() {
        assertTrue(AuthService.class.isInterface(), "AuthService 应为接口类型");
    }
}