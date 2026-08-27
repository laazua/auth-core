# Sprint 工作单：sprint-014

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-014 |
| 所属模块 | auth |
| 功能点 ID | auth/005 |
| 功能点名称 | 权限校验 API POST /api/v1/auth/check（供外部模块集成调用） |
| 状态 | PLANNED |
| 创建时间 | 2026-08-27 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/003 | JWT 校验过滤器 + SecurityContext 注入 | ✅ |

## 需求描述

实现权限校验接口，供外部模块集成调用：
1. `POST /api/v1/auth/check` — 接收 `CheckRequest{userId, permissionCode}`，返回 `Result<CheckResponse>`
2. `CheckRequest`：`userId: Long`（被校验用户 ID）、`permissionCode: String`（权限编码，如 "user:create"）
3. `CheckResponse`：`hasPermission: boolean`、 `userId: Long`、 `permissionCode: String`
4. 逻辑：基于 userId 查询用户角色 → 角色关联权限 → 判断是否包含指定 permissionCode
5. 复用 `AuthService` 的权限查询逻辑
6. 无需当前登录用户上下文，直接按 userId 校验（内部服务间调用）

## 业务背景

架构 §5：权限校验 API `POST /api/v1/auth/check` 供外部模块集成调用。RBAC0 §6.2：用户权限仅通过角色获得。外部模块在执行敏感操作前调用此接口校验权限。

## 交付物

1. `CheckRequest.java` / `CheckResponse.java` — DTO（在 `com.authcore.dto.auth`）
2. 更新 `AuthService.java` — 新增 `boolean checkPermission(Long userId, String permissionCode)`
3. 更新 `AuthController.java` — 新增 `POST /check` 端点
4. 测试用例

## 验收标准（TDD 驱动）

### AC1 — 权限校验通过返回 true
> 给定用户拥有指定权限，POST /api/v1/auth/check {userId, permissionCode} 返回 200，Result.code=0，data.hasPermission=true。

**用例**：`AuthControllerTest#checkPermissionReturnsTrue`
- 准备：种子数据 admin 用户拥有 "user:create" 权限
- 操作：POST /api/v1/auth/check {userId: 1, permissionCode: "user:create"}（需认证 header）
- 断言：status=200、code=0、hasPermission=true

### AC2 — 无权限返回 false
> 给定用户不拥有指定权限，返回 200，data.hasPermission=false。

**用例**：`AuthControllerTest#checkPermissionReturnsFalse`
- 操作：POST /api/v1/auth/check {userId: 2, permissionCode: "user:delete"}（普通用户无删除权限）
- 断言：status=200、code=0、hasPermission=false

### AC3 — 用户不存在返回 404
> 给定不存在的 userId，返回 404，code=1001（用户不存在）。

**用例**：`AuthControllerTest#checkPermissionUserNotFoundReturns404`
- 操作：POST /api/v1/auth/check {userId: 99999, permissionCode: "user:view"}
- 断言：status=404、code=1001

### AC4 — 未认证返回 401
> 无 token 调用 /check，返回 401，code=1401。

**用例**：`AuthControllerTest#checkWithoutAuthReturns401`
- 操作：POST /api/v1/auth/check {userId: 1, permissionCode: "user:view"} 无 header
- 断言：status=401、code=1401

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托 service→包装 Result）
- [ ] Service 层事务 @Transactional(readOnly=true)
- [ ] 复用既有权限查询逻辑，无 N+1
- [ ] 错误码分段：10xx 用户、14xx 认证（coding-standards §6、架构 §4）
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿