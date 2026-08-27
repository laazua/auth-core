package com.authcore.controller;

import com.authcore.common.Result;
import com.authcore.config.security.JwtTokenProvider;
import com.authcore.dto.auth.LoginRequest;
import com.authcore.dto.auth.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器：登录接口（auth/002）。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 构造器注入。
     *
     * @param authenticationManager 认证管理器
     * @param jwtTokenProvider      JWT 生成器
     */
    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 登录接口。
     *
     * @param request 登录请求（username、password）
     * @return Result<LoginResponse>，包含 token、tokenType、expiresIn
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtTokenProvider.generateToken(userDetails);
            long expiresIn = 3600L * 2; // 默认 2 小时，与 JwtProperties.expireHours 一致
            return Result.ok(new LoginResponse(token, "Bearer", expiresIn));
        } catch (BadCredentialsException | DisabledException e) {
            throw new com.authcore.common.BusinessException(1401, "用户名或密码错误");
        }
    }
}