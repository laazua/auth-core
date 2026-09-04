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
 * ModuleService 接口方法签名完整性验证（AC1）。
 */
@DisplayName("ModuleService 接口方法签名完整性")
class ModuleServiceInterfaceSpec {

    @Test
    @DisplayName("接口声明了全部 6 个公共方法")
    void moduleServiceInterfaceHasAllMethods() {
        Method[] methods = ModuleService.class.getDeclaredMethods();
        assertEquals(6, methods.length, "接口应声明 6 个公共方法");

        Set<String> methodNames = Stream.of(methods)
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(methodNames.contains("queryModules"), "应包含 queryModules 方法");
        assertTrue(methodNames.contains("getModuleById"), "应包含 getModuleById 方法");
        assertTrue(methodNames.contains("createModule"), "应包含 createModule 方法");
        assertTrue(methodNames.contains("updateModule"), "应包含 updateModule 方法");
        assertTrue(methodNames.contains("queryModulePermissions"), "应包含 queryModulePermissions 方法");
        assertTrue(methodNames.contains("deleteModule"), "应包含 deleteModule 方法");
    }

    @Test
    @DisplayName("ModuleService 是接口类型")
    void moduleServiceIsInterface() {
        assertTrue(ModuleService.class.isInterface(), "ModuleService 应为接口类型");
    }
}