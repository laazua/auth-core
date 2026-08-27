package com.authcore.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：拦截请求，校验 Authorization: Bearer <token>，
 * 解析用户身份并注入 SecurityContext（auth/003）。
 * <p>
 * - 仅处理以 "Bearer " 开头的 Authorization 头
 * - 校验通过：设置 UsernamePasswordAuthenticationToken 到 SecurityContext
 * - 校验失败/异常：清空 SecurityContext，继续过滤器链（由 SecurityConfig 统一返回 401/403）
 * - 非 Bearer/无头：直接放行（由 SecurityConfig 决定 401/403）
 * </p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    /**
     * 构造器注入。
     *
     * @param jwtTokenProvider       JWT 生成/校验组件
     * @param customUserDetailsService 用户详情服务
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                    CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    String username = jwtTokenProvider.getUsernameFromToken(token);
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}