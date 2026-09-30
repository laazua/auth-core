package com.authcore.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：拦截请求，优先解析 Authorization: Bearer <token>，
 * 缺失时回退解析同名会话 Cookie，校验后注入 SecurityContext（auth/003 + web/038a 双承载）。
 *
 * <p>
 * - 承载优先级：Bearer &gt; Cookie（Bearer 存在时行为与原实现一致）
 * - 会话 Cookie 名为 {@link #AUTH_COOKIE_NAME}，与登录接口下发、登出接口清除的 Set-Cookie 对应
 * - 校验通过：设置 UsernamePasswordAuthenticationToken 到 SecurityContext
 * - 校验失败/异常：清空 SecurityContext，继续过滤器链（由 SecurityConfig 统一返回 401/403）
 * - 两者皆无：直接放行（由 SecurityConfig 决定 401/403）
 * </p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 会话 Cookie 名：登录下发、登出清除、本过滤器读取（web/038a）。 */
    public static final String AUTH_COOKIE_NAME = "AUTH_TOKEN";

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
        String token = extractToken(request);

        if (token != null) {
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

    /**
     * 提取凭证：优先 Authorization Bearer，缺失时回退同名会话 Cookie（web/038a 双承载）。
     *
     * @param request 当前请求
     * @return JWT 值；两种承载皆无有效值时为 null
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return extractCookieToken(request);
    }

    /**
     * 读取会话 Cookie 的值。
     *
     * @param request 当前请求
     * @return Cookie 值；不存在或空白时为 null
     */
    private String extractCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }
}
