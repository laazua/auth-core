# Sprint 工作单：sprint-010

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-010 |
| 所属模块 | auth |
| 功能点 ID | auth/001 |
| 功能点名称 | Spring Security 无状态基线 + BCrypt 编码器 |
| 状态 | PLANNED |
| 创建时间 | 2026-08-27 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| infra/003 | 统一响应体 Result<T> + 全局异常处理 + Bean Validation | ✅ |
| model/001 | sys_user 表迁移+实体+Mapper | ✅ |

## 需求描述

实现 Spring Security 无状态基线配置：
1. `SecurityFilterChain` 配置：禁用 CSRF、禁用 Session（STATELESS）、配置公开端点（/actuator/**、/api/v1/auth/login）、其余需认证
2. `BCryptPasswordEncoder` Bean（strength=10）
3. `UserDetailsService` 实现：基于 `SysUserMapper` 查询用户，支持 username 查找，status=1 校验
4. `AuthenticationProvider` 配置：DaoAuthenticationProvider + BCryptPasswordEncoder + UserDetailsService
5. JWT 相关配置类占位（jwt.secret 从环境变量读取，jwt.expire-hours 默认 2）

## 业务背景

这是 auth 模块的安全基线，所有后续 auth/002-006 功能点的前置依赖。架构 §5 规定：无状态 JWT、HS256、payload{uid, username, exp}、token 有效期默认 2h、secret 仅环境变量。RBAC0 硬语义 §6.5：停用用户(status=0)不可登录、token 即时失效。

## 交付物

1. `SecurityConfig.java` — 核心安全配置类
2. `JwtProperties.java` — JWT 配置绑定类（@ConfigurationProperties(prefix="jwt")）
3. `CustomUserDetailsService.java` — 实现 UserDetailsService，查 sys_user
4. `CustomUserDetails.java` — 实现 UserDetails，包装 SysUser + 权限码集合

## 验收标准（TDD 驱动）

### AC1 — 安全配置生效：无状态、CSRF 关闭、公开端点放行
> 启动应用后，`/actuator/health` 与 `/api/v1/auth/login` 无需认证可访问；其余端点返回 401。

**用例**：`SecurityConfigTest#publicEndpointsAccessibleWithoutAuth`
- 准备：Spring Boot 测试上下文
- 操作：MockMvc GET /actuator/health、POST /api/v1/auth/login
- 断言：状态码 200/400（非 401/403）

### AC2 — 受保护端点拦截
> 访问任意受保护端点（如 /api/v1/users）无 token 时返回 401。

**用例**：`SecurityConfigTest#protectedEndpointsRequireAuth`
- 操作：MockMvc GET /api/v1/users 无 Authorization 头
- 断言：状态码 401

### AC3 — BCryptPasswordEncoder 可用
> 注入 `BCryptPasswordEncoder`，`encode("raw")` 生成哈希，`matches("raw", hash)` 返回 true。

**用例**：`SecurityConfigTest#bCryptEncoderWorks`
- 操作：encoder.encode("test123") → hash；encoder.matches("test123", hash)
- 断言：matches 返回 true

### AC4 — UserDetailsService 查询用户并校验 status
> 给定数据库存在 status=1 用户，loadUserByUsername 返回 UserDetails；status=0 抛 UsernameNotFoundException。

**用例**：`CustomUserDetailsServiceTest#loadUserByUsernameStatusCheck`
- 准备：测试库插入 status=1 与 status=0 用户
- 操作：loadUserByUsername("enabled_user") / loadUserByUsername("disabled_user")
- 断言：前者返回 UserDetails、后者抛 UsernameNotFoundException

## 规范检查清单（Evaluator 逐项核对）

- [ ] SecurityFilterChain 仅一处定义，STATELESS 明确
- [ ] BCryptPasswordEncoder strength=10，单例 Bean
- [ ] UserDetailsService 仅查 sys_user，不含业务逻辑
- [ ] JWT 配置类绑定 jwt.secret/jwt.expire-hours，secret 仅环境变量
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿