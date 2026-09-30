package com.authcore.controller;

import com.authcore.common.Result;
import com.authcore.config.security.CustomUserDetails;
import com.authcore.config.security.JwtAuthenticationFilter;
import com.authcore.config.security.JwtProperties;
import com.authcore.config.security.JwtTokenProvider;
import com.authcore.dto.auth.CheckRequest;
import com.authcore.dto.auth.CheckResponse;
import com.authcore.dto.auth.LoginRequest;
import com.authcore.dto.auth.LoginResponse;
import com.authcore.dto.auth.MeResponse;
import com.authcore.mapper.SysUserMapper;
import com.authcore.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 认证控制器：登录接口（auth/002）、当前用户信息（auth/004）。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthService authService;
    private final SysUserMapper userMapper;

    /**
     * 构造器注入。
     *
     * @param authenticationManager 认证管理器
     * @param jwtTokenProvider      JWT 生成器
     * @param jwtProperties         JWT 配置（expire-hours，会话 Cookie 生命周期同源）
     * @param authService           认证业务服务
     * @param userMapper            用户数据访问
     */
    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          JwtProperties jwtProperties,
                          AuthService authService,
                          SysUserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.authService = authService;
        this.userMapper = userMapper;
    }

    /**
     * 登录接口。
     * 成功时同时下发 HttpOnly 会话 Cookie（web/038a 双承载），响应体契约不变。
     *
     * @param request  登录请求（username、password）
     * @param response 响应（追加 Set-Cookie 会话凭据）
     * @return Result<LoginResponse>，包含 token、tokenType、expiresIn
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtTokenProvider.generateToken(userDetails);
            long expiresIn = 3600L * jwtProperties.getExpireHours();
            response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(token, expiresIn).toString());
            return Result.ok(new LoginResponse(token, "Bearer", expiresIn));
        } catch (BadCredentialsException | DisabledException e) {
            throw new com.authcore.common.BusinessException(1401, "用户名或密码错误");
        }
    }

    /**
     * 权限校验接口（auth/005）。
     * 供外部模块集成调用，根据 userId 校验是否拥有指定权限。
     *
     * @param request 权限校验请求（userId、permissionCode）
     * @return Result<CheckResponse> 包含 hasPermission、userId、permissionCode
     */
    @PostMapping("/check")
    public Result<CheckResponse> checkPermission(@Valid @RequestBody CheckRequest request) {
        if (userMapper.selectById(request.userId()) == null) {
            throw new com.authcore.common.BusinessException(1001, "用户不存在");
        }
        boolean has = authService.checkPermission(request.userId(), request.permissionCode());
        return Result.ok(new CheckResponse(has, request.userId(), request.permissionCode()));
    }

    /**
     * 获取当前登录用户完整信息（auth/004）。
     *
     * @return Result<MeResponse> 包含 user、roles、permissions
     */
    @GetMapping("/me")
    public Result<MeResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() instanceof String) {
            throw new com.authcore.common.BusinessException(1401, "未认证");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();
        MeResponse meResponse = authService.getCurrentUserInfo(userId);
        return Result.ok(meResponse);
    }

    /**
     * 登出接口（auth/006）。
     * 无状态 JWT v1 语义：服务端不维护 token 黑名单，登出本质是前端清除本地 token；
     * 同时下发会话 Cookie 清除指令（Max-Age=0，web/038a）。
     *
     * @param response 响应（追加 Set-Cookie 清除指令）
     * @return Result<Void> code=0，message="登出成功，请客户端清除 token"
     */
    @PostMapping("/logout")
    @Operation(summary = "登出（无状态 JWT v1：服务端不撤销 token，客户端自行清除）")
    public Result<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie("", 0).toString());
        return Result.okMessage("登出成功，请客户端清除 token");
    }

    /**
     * 构造会话 Cookie：HttpOnly、SameSite=Lax、Path=/（web/038a 双承载）。
     *
     * @param value        Cookie 值（登出传空串）
     * @param maxAgeSeconds 有效期秒数（与 jwt.expire-hours 同源；登出传 0 表示清除）
     * @return 待写入 Set-Cookie 的 ResponseCookie
     */
    private ResponseCookie sessionCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE_NAME, value)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}