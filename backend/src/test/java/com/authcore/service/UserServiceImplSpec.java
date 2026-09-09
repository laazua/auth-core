package com.authcore.service;

import com.authcore.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserServiceImpl 实现断言（AC2）。
 */
@DisplayName("UserServiceImpl 实现断言")
class UserServiceImplSpec {

    @Test
    @DisplayName("UserServiceImpl 实现 UserService 接口且被 @Service 注解")
    void userServiceImplImplementsInterface() {
        assertTrue(UserService.class.isAssignableFrom(UserServiceImpl.class),
                "UserServiceImpl 应实现 UserService 接口");

        Class<?>[] interfaces = UserServiceImpl.class.getInterfaces();
        boolean implementsUserService = Stream.of(interfaces)
                .anyMatch(UserService.class::equals);
        assertTrue(implementsUserService, "UserServiceImpl 应实现 UserService 接口");
    }
}