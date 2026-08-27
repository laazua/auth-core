package com.authcore.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtTokenProvider 生成/校验/解析判定用例（auth/002 AC4 单元层）。
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * AC4: tokenRoundtrip。
     * Given 模拟 UserDetails（username=admin, uid=1）
     * When generateToken -> token -> validateToken -> getUsernameFromToken
     * Then validateToken=true、username="admin"
     */
    @Test
    @DisplayName("JWT 生成-校验-解析全链路正常")
    void tokenRoundtrip() {
        // Arrange: 使用 CustomUserDetails 包装 SysUser
        var sysUser = new com.authcore.entity.SysUser();
        sysUser.setId(1L);
        sysUser.setUsername("admin");
        sysUser.setPassword("$2a$10$thO4/joMz94gBoPayCMfwO7xh/bPiMqefMx7mV39DtEDLy5Zq3fCG");
        sysUser.setStatus(1);
        var customDetails = new CustomUserDetails(sysUser, java.util.List.of());

        // Act
        String token = jwtTokenProvider.generateToken(customDetails);

        // Assert
        assertNotNull(token, "token 不应为空");
        String[] parts = token.split("\\.");
        assertTrue(parts.length == 3, "JWT 应为三段式，实际段数: " + parts.length);

        boolean valid = jwtTokenProvider.validateToken(token);
        assertTrue(valid, "token 应通过校验");

        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("admin", username, "解析用户名应为 admin");
    }
}