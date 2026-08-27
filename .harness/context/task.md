# Sprint 工作单：sprint-011

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-011 |
| 所属模块 | auth |
| 功能点 ID | auth/002 |
| 功能点名称 | 登录接口 POST /api/v1/auth/login 签发 JWT |
| 状态 | PLANNED |
| 创建时间 | 2026-08-27 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/001 | Spring Security 无状态基线 + BCrypt 编码器 | ✅ |

## 需求描述

实现登录接口：
1. `POST /api/v1/auth/login` — 接收 `LoginRequest{username, password}`，返回 `Result<LoginResponse>`，其中 `LoginResponse{token, tokenType="Bearer", expiresIn}`
2. 使用 `AuthenticationManager` 进行认证（委托给 auth/001 配置的 `DaoAuthenticationProvider` + `CustomUserDetailsService`）
3. 认证成功后，使用 `JwtTokenProvider` 生成 JWT（HS256，payload {uid, username, exp}，过期时间来自 `JwtProperties.expireHours`）
4. `JwtTokenProvider` 组件：`generateToken(UserDetails)`、`validateToken(String)`、`getUsernameFromToken(String)`
5. 失败返回 401，错误码 1401（用户名或密码错误），`Result.code=1401`

## 业务背景

架构 §5 规定：登录接口 POST `/api/v1/auth/login`，请求体 username+password，认证成功签发 JWT(HS256)，payload{uid, username, exp}，token 有效期默认 2h。secret 仅环境变量。RBAC0 §6.5：停用用户(status=0)不可登录（auth/001 的 UserDetailsService 已校验）。

## 交付物

1. `LoginRequest.java` / `LoginResponse.java` — DTO（在 `com.authcore.dto.auth`）
2. `JwtTokenProvider.java` — JWT 生成/校验/解析组件（在 `com.authcore.config.security`）
3. `AuthController.java` — 登录端点（在 `com.authcore.controller`）
4. `AuthControllerTest.java` — Web 层测试

## 验收标准（TDD 驱动）

### AC1 — 登录成功返回 JWT
> 给定数据库存在 status=1 用户 admin / BCrypt(admin123456)，POST /api/v1/auth/login 携带正确凭据，返回 200，Result.code=0，data.token 为非空 JWT 字符串（三段式，Base64Url 编码），tokenType=Bearer，expiresIn=7200（默认 2h）。

**用例**：`AuthControllerTest#loginSuccessReturnsJwt`
- 准备：种子数据 admin/admin123456 已存在（V6 迁移）
- 操作：POST /api/v1/auth/login {username:"admin", password:"admin123456"}
- 断言：status=200、code=0、token 非空且为 3 段、expiresIn=7200

### AC2 — 凭据错误返回 401
> 用户名不存在或密码错误，返回 401，Result.code=1401（用户名或密码错误），message 非空。

**用例**：`AuthControllerTest#loginWrongCredentialsReturns401`
- 操作：POST /api/v1/auth/login {username:"admin", password:"wrong"}
- 断言：status=401、code=1401

### AC3 — 停用用户不可登录
> 给定 status=0 用户，POST /api/v1/auth/login 返回 401，code=1401。

**用例**：`AuthControllerTest#loginDisabledUserReturns401`
- 准备：插入 status=0 用户
- 操作：POST /api/v1/auth/login {username:"disabled", password:"xxx"}
- 断言：status=401、code=1401

### AC4 — JWT 可被 JwtTokenProvider 解析
> 登录返回的 token，`JwtTokenProvider.validateToken(token)` 返回 true，`getUsernameFromToken(token)` 返回 "admin"。

**用例**：`JwtTokenProviderTest#tokenRoundtrip`
- 操作：generateToken(adminUserDetails) → token；validateToken(token)；getUsernameFromToken(token)
- 断言：validateToken=true、username="admin"

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托→包装 Result），无业务逻辑（coding-standards §3）
- [ ] JWT 生成使用 HS256、payload 含 uid/username/exp、secret 仅环境变量（架构 §5）
- [ ] 错误码分段：14xx 认证类（coding-standards §6、架构 §4）
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿