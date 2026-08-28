# Sprint 工作单：sprint-018

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-018 |
| 所属模块 | users |
| 功能点 ID | users/003 |
| 功能点名称 | 密码修改+管理员重置 |
| 状态 | PLANNED |
| 创建时间 | 2026-08-27 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| users/001 | 用户 CRUD API | ✅ |

## 需求描述

实现密码修改与管理员重置密码接口：
1. `PUT /api/v1/users/{id}/password` — 用户自助修改密码
   - 请求体：`PasswordChangeDTO {oldPassword: String, newPassword: String}`
   - 校验：oldPassword 与数据库 BCrypt 匹配，newPassword 不同于旧密码
   - 仅当前登录用户可修改自己的密码（userId 需与 SecurityContext 中一致）
2. `POST /api/v1/users/{id}/password/reset` — 管理员重置密码
   - 请求体：`PasswordResetDTO {newPassword: String}`
   - 仅管理员角色（ROLE_ADMIN）可调用
   - 无需旧密码，直接 BCrypt 加密新密码覆盖
3. 响应：`Result<Void>` code=0

## 业务背景

架构 §5：BCrypt 密码存储。RBAC0：管理员拥有全部权限（auth/004 /me 返回角色），前端据此判断是否显示重置按钮。

## 交付物

1. `PasswordChangeDTO.java` / `PasswordResetDTO.java` — DTO（`com.authcore.dto.user`）
2. 更新 `UserService.java` — 新增 `changePassword(Long userId, String oldPassword, String newPassword)`、`resetPassword(Long userId, String newPassword, Long operatorId)`
3. 更新 `UserController.java` — 新增两个端点
4. 测试用例

## 验收标准（TDD 驱动）

### AC1 — 用户自助修改密码成功
> 给定正确旧密码，PUT /api/v1/users/{id}/password {oldPassword, newPassword} 返回 200，数据库密码更新为 newPassword 的 BCrypt，再次登录需用新密码。

**用例**：`UserControllerTest#changePasswordSuccess`
- 准备：用户 user2 密码 Pass1234
- 操作：PUT /api/v1/users/2/password {oldPassword:"Pass1234", newPassword:"NewPass5678"}
- 断言：status=200、登录接口用 NewPass5678 成功、旧密码失败

### AC2 — 旧密码错误返回 400
> 给定错误旧密码，返回 400，code=1005（旧密码错误）。

**用例**：`UserControllerTest#changePasswordWrongOldReturns400`
- 操作：PUT {oldPassword:"WrongPass", newPassword:"NewPass"}
- 断言：status=400、code=1005

### AC3 — 新旧密码相同返回 400
> newPassword 与旧密码一致，返回 400，code=1006。

**用例**：`UserControllerTest#changePasswordSameAsOldReturns400`
- 操作：PUT {oldPassword:"Pass1234", newPassword:"Pass1234"}
- 断言：status=400、code=1006

### AC4 — 管理员重置密码成功
> 管理员调用 POST /api/v1/users/{id}/password/reset {newPassword} 返回 200，目标用户密码更新，无需旧密码。

**用例**：`UserControllerTest#adminResetPasswordSuccess`
- 准备：admin token、目标用户 user2
- 操作：POST /api/v1/users/2/password/reset {newPassword:"AdminReset123"}
- 断言：status=200、user2 用新密码登录成功

### AC5 — 非管理员重置返回 403
> 普通用户调用 reset 端点，返回 403，code=1403（权限不足）。

**用例**：`UserControllerTest#resetPasswordNonAdminReturns403`
- 操作：user2 token 调用 POST /password/reset
- 断言：status=403、code=1403

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托 service→包装 Result）
- [ ] Service @Transactional，BCrypt 加密
- [ ] 自助修改需校验 oldPassword 匹配、新旧不同
- [ ] 管理员重置需 ROLE_ADMIN 权限（@PreAuthorize 或手动校验）
- [ ] 错误码分段：10xx 用户（1005 旧密码错误、1006 新旧相同）、14xx 认证授权
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿