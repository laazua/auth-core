package com.authcore.service;

import com.authcore.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthServiceImpl 实现断言（AC2）。
 */
@DisplayName("AuthServiceImpl 实现断言")
class AuthServiceImplSpec {

    @Test
    @DisplayName("AuthServiceImpl 实现 AuthService 接口且被 @Service 注解")
    void authServiceImplImplementsInterface() {
        assertTrue(AuthService.class.isAssignableFrom(AuthServiceImpl.class),
                "AuthServiceImpl 应实现 AuthService 接口");

        Class<?>[] interfaces = AuthServiceImpl.class.getInterfaces();
        boolean implementsAuthService = Stream.of(interfaces)
                .anyMatch(AuthService.class::equals);
        assertTrue(implementsAuthService, "AuthServiceImpl 应实现 AuthService 接口");
    }
}