package com.authcore.config.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 无状态基线配置。
 * <p>
 * - 禁用 CSRF
 * - 禁用 Session（STATELESS）
 * - 公开端点：/actuator/**、/api/v1/auth/login
 * - 其余端点需认证
 * - DaoAuthenticationProvider + BCryptPasswordEncoder + CustomUserDetailsService
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 构造器注入。
     *
     * @param userDetailsService          自定义用户详情服务
     * @param jwtProperties               JWT 配置属性
     * @param jwtTokenProvider            JWT 生成/校验组件
     * @param jwtAuthenticationEntryPoint JWT 认证入口点
     */
    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtProperties jwtProperties,
                          JwtTokenProvider jwtTokenProvider, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.userDetailsService = userDetailsService;
        this.jwtProperties = jwtProperties;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    /**
     * JWT 认证过滤器 Bean（auth/003）。
     *
     * @return JwtAuthenticationFilter 实例
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    /**
     * 安全过滤器链配置。
     *
     * @param http HttpSecurity 构建器
     * @return SecurityFilterChain
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（无状态 JWT 无需 CSRF 保护）
                .csrf(csrf -> csrf.disable())
                // 无状态会话管理
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 未认证访问受保护资源返回 401 + Result(code=1401)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // 授权规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                // JWT 认证过滤器（在 UsernamePasswordAuthenticationFilter 之前）
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // 认证提供者
                .authenticationProvider(daoAuthenticationProvider());

        return http.build();
    }

    /**
     * DaoAuthenticationProvider Bean。
     *
     * @return 配置好的 DaoAuthenticationProvider
     */
    @Bean
    public AuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(bCryptPasswordEncoder());
        return provider;
    }

    /**
     * BCryptPasswordEncoder Bean（strength=10）。
     *
     * @return BCryptPasswordEncoder 单例
     */
    @Bean
    public PasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * AuthenticationManager Bean（供登录控制器使用，auth/002）。
     *
     * @param config 认证配置
     * @return AuthenticationManager
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}