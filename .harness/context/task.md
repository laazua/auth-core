# Sprint 工作单：sprint-012

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-012 |
| 所属模块 | auth |
| 功能点 ID | auth/003 |
| 功能点名称 | JWT 校验过滤器 + SecurityContext 注入 |
| 状态 | PLANNED |
| 创建时间 | 2026-08-27 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/001 | Spring Security 无状态基线 + BCrypt 编码器 | ✅ |
| auth/002 | 登录接口 POST /api/v1/auth/login 签发 JWT | ✅ |

## 需求描述

实现 JWT 校验过滤器，拦截所有受保护请求，验证 Authorization: Bearer <token>，解析用户身份并注入 SecurityContext：

1. `JwtAuthenticationFilter` — 实现 `OncePerRequestFilter`
   - 从 `Authorization` 头提取 Bearer token
   - 调用 `JwtTokenProvider.validateToken(token)` 校验
   - 校验通过：`JwtTokenProvider.getUsernameFromToken(token)` 获取 username
   - 用 `CustomUserDetailsService.loadUserByUsername(username)` 加载 UserDetails
   - 构建 `UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())`
   - `SecurityContextHolder.getContext().setAuthentication(authentication)`
   - 校验失败/无 token：放行（由 SecurityConfig 的 authorizeHttpRequests 决定 401/403）

2. 在 `SecurityConfig.filterChain` 中注册过滤器：`.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)`

## 业务背景

这是鉴权链路的核心：auth/002 签发 token，auth/003 校验 token 并建立上下文。后续 auth/004 (/me) 与 auth/005 (/check) 直接依赖 SecurityContext 中的认证信息。架构 §5：无状态 JWT、HS256、payload{uid, username, exp}。RBAC0 §6.5：停用用户 token 即时失效（CustomUserDetailsService 已校验 status=1，过期用户自然被拦截）。

## 交付物

1. `JwtAuthenticationFilter.java` — JWT 校验过滤器（`com.authcore.config.security`）
2. 更新 `SecurityConfig.java` — 注册过滤器
3. `JwtAuthenticationFilterTest.java` — 测试用例

## 验收标准（TDD 驱动）

### AC1 — 有效 token 请求通过，SecurityContext 注入用户
> 给定有效 JWT，GET /api/v1/users（受保护端点）携带 Authorization: Bearer <token>，返回 200（或 404 因无 controller，但不应 401/403），且 SecurityContext 中 Authentication 为 UsernamePasswordAuthenticationToken，principal 为 CustomUserDetails。

**用例**：`JwtAuthenticationFilterTest#validTokenSetsSecurityContext`
- 准备：JwtTokenProvider 生成 admin token
- 操作：MockMvc GET /api/v1/users header Authorization: Bearer <token>
- 断言：status != 401/403；SecurityContextHolder.getContext().getAuthentication() 不为 null；principal 为 CustomUserDetails；username=admin

### AC2 — 无效/过期 token 返回 401
> 给定篡改/过期 JWT，GET /api/v1/users 携带 Authorization: Bearer <bad_token>，返回 401，Result.code=1401。

**用例**：`JwtAuthenticationFilterTest#invalidTokenReturns401`
- 操作：GET /api/v1/users header Authorization: Bearer invalid.token.here
- 断言：status=401、code=1401

### AC3 — 无 Authorization 头返回 401
> 访问受保护端点无 Authorization 头，返回 401，Result.code=1401。

**用例**：`JwtAuthenticationFilterTest#missingAuthHeaderReturns401`
- 操作：GET /api/v1/users 无 header
- 断言：status=401、code=1401

### AC4 — 非 Bearer 格式返回 401
> Authorization 头非 Bearer 开头，返回 401。

**用例**：`JwtAuthenticationFilterTest#nonBearerAuthHeaderReturns401`
- 操作：GET /api/v1/users header Authorization: Basic xxx
- 断言：status=401、code=1401

## 规范检查清单（Evaluator 逐项核对）

- [ ] OncePerRequestFilter 正确实现，不重复校验同一请求
- [ ] 仅对有 Authorization: Bearer 头的请求尝试校验，否则放行
- [ ] 校验失败不抛异常，而是清空 SecurityContext 并继续（由 SecurityConfig 统一处理 401/403）
- [ ] 线程安全：SecurityContextHolder 使用 ThreadLocal，过滤器链无状态
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿