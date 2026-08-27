package com.authcore.config.security;

import com.authcore.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 认证入口点：未认证访问受保护资源时返回 401 + Result(code=1401)。
 * 替代默认的 HttpStatusEntryPoint，提供统一业务响应格式（auth/003）。
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final int UNAUTHORIZED_CODE = 1401;
    private static final String UNAUTHORIZED_MESSAGE = "未授权访问，请先登录";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result<Void> result = Result.error(UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE);
        objectMapper.writeValue(response.getOutputStream(), result);
    }
}