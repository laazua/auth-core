# Sprint 工作单：sprint-015

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-015 |
| 所属模块 | auth |
| 功能点 ID | auth/006 |
| 功能点名称 | 登出策略（无状态 JWT v1：接口+失效语义说明） |
| 状态 | PLANNED |
| 创建时间 | 2026-08-27 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/003 | JWT 校验过滤器 + SecurityContext 注入 | ✅ |

## 需求描述

实现登出接口与无状态 JWT 失效语义说明：
1. `POST /api/v1/auth/logout` — 登出端点（无状态 JWT v1 语义：客户端丢弃 token，服务端不维护黑名单）
2. 响应：`Result<Void>` code=0，message="登出成功，请客户端清除 token"
3. 无需服务端撤销 token（无状态 JWT v1 不维护 token 黑名单/白名单），文档说明由前端清除本地存储
3. 可选：若后续版本引入 token 刷新机制，预留刷新 token 撤销扩展点

## 业务背景

架构 §5：登出策略（无状态 JWT v1：接口+失效语义说明）。无状态 JWT 意味着服务端不存储 token 状态，登出本质是前端清除 token。RBAC0 §6.5：停用用户 token 即时失效于下次校验（由 JwtAuthenticationFilter 校验用户 status 实现，不需登出接口配合）。

## 交付物

1. `LogoutRequest.java` / `LogoutResponse.java` — DTO（可选，简化为无参/返回 message）
2. 更新 `AuthController.java` — 新增 `POST /logout` 端点
3. `AuthControllerTest.java` — 测试用例

## 验收标准（TDD 驱动）

### AC1 — 登出接口返回成功
> 给定有效 JWT，POST /api/v1/auth/logout 返回 200，Result.code=0，message="登出成功，请客户端清除 token"。

**用例**：`AuthControllerTest#logoutReturnsSuccess`
- 准备：有效 admin token
- 操作：POST /api/v1/auth/logout header Authorization: Bearer <token>
- 断言：status=200、code=0、message 包含"登出成功"

### AC2 — 无 token 也可调用（幂等）
> 无 token 调用 /logout 返回 200（幂等，前端清除本地 token 不依赖服务端状态）。

**用例**：`AuthControllerTest#logoutWithoutTokenReturnsSuccess`
- 操作：POST /api/v1/auth/logout 无 header
- 断言：status=200、code=0

### AC3 — 接口文档化语义
> 控制器方法上有 @Operation(summary="登出（无状态 JWT v1：服务端不撤销 token，客户端自行清除）") 说明语义。

**用例**：`AuthControllerTest#logoutEndpointHasDocumentation`
- 断言：方法上存在 @Operation 注解且 summary 包含"无状态 JWT v1"

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托→包装 Result）
- [ ] 无需 Service 层逻辑（纯接口语义）
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿