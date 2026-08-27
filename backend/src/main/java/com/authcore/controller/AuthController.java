package com.authcore.controller;

import com.authcore.common.Result;
import com.authcore.config.security.CustomUserDetails;
import com.authcore.config.security.JwtTokenProvider;
import com.authcore.dto.auth.CheckRequest;
import com.authcore.dto.auth.CheckResponse;
import com.authcore.dto.auth.LoginRequest;
import com.authcore.dto.auth.LoginResponse;
import com.authcore.dto.auth.MeResponse;
import com.authcore.mapper.SysUserMapper;
import com.authcore.service.AuthService;
import jakarta.validation.Valid;
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

/**
 * 认证控制器：登录接口（auth/002）、当前用户信息（auth/004）。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final SysUserMapper userMapper;

    /**
     * 构造器注入。
     *
     * @param authenticationManager 认证管理器
     * @param jwtTokenProvider      JWT 生成器
     * @param authService           认证业务服务
     * @param userMapper            用户数据访问
     */
    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          AuthService authService,
                          SysUserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authService = authService;
        this.userMapper = userMapper;
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
}