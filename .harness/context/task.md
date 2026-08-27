# Sprint 工作单：sprint-013

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-013 |
| 所属模块 | auth |
| 功能点 ID | auth/004 |
| 功能点名称 | GET /api/v1/auth/me（用户+角色+权限集合） |
| 状态 | PLANNED |
| 创建时间 | 2026-08-27 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/003 | JWT 校验过滤器 + SecurityContext 注入 | ✅ |
| model/005 | sys_user_role+sys_role_permission 关联表 | ✅ |

## 需求描述

实现当前登录用户信息查询接口：
1. `GET /api/v1/auth/me` — 返回当前认证用户的完整信息
2. 响应结构：`Result<MeResponse>`，其中 `MeResponse` 包含：
   - `UserVO`：id、username、nickname、email、phone、status
   - `List<RoleVO>`：角色列表（id、name、code、status）
   - `List<PermissionVO>`：权限码集合（code 列表，用于前端权限判断）
3. 从 `SecurityContextHolder.getContext().getAuthentication()` 获取 `CustomUserDetails`
4. 基于用户 ID 查询关联角色（sys_user_role → sys_role）与权限（sys_user_role → sys_role → sys_role_permission → sys_permission）
5. 权限码去重，按模块分组或扁平列表均可

## 业务背景

前端登录后调用 `/me` 获取用户完整信息与权限，用于动态菜单渲染、按钮级权限控制。架构 §5：登录后前端需获取用户信息。RBAC0 §6.2：用户权限仅通过角色获得（sys_user_role → sys_role_permission → sys_permission）。model/005 已建好关联表。

## 交付物

1. `MeResponse.java` / `UserVO.java` / `RoleVO.java` / `PermissionVO.java` — DTO（在 `com.authcore.dto.auth`）
2. `AuthService.java` — 业务逻辑：查询用户+角色+权限（在 `com.authcore.service`）
3. `AuthController.java` — 新增 `GET /me` 端点
4. `AuthControllerTest.java` — 新增测试用例

## 验收标准（TDD 驱动）

### AC1 — 认证用户调用 /me 返回完整信息
> 给定有效 JWT，GET /api/v1/auth/me 返回 200，Result.code=0，data 包含 user（id/username/nickname/email/phone/status）、roles（含 admin 角色）、permissions（含 14 个权限码）。

**用例**：`AuthControllerTest#meReturnsUserRolesPermissions`
- 准备：种子数据 admin 用户、admin 角色、全部 14 权限
- 操作：GET /api/v1/auth/me header Authorization: Bearer <admin_token>
- 断言：status=200、code=0、user.username=admin、roles 含 admin、permissions.size()=14

### AC2 — 权限码去重正确
> admin 角色关联全部 14 权限，返回的 permissions 列表无重复。

**用例**：`AuthControllerTest#mePermissionsDeduplicated`
- 断言：permissions.stream().distinct().count() == permissions.size()

### AC3 — 未认证访问返回 401
> 无 token 访问 /me，返回 401，code=1401。

**用例**：`AuthControllerTest#meWithoutAuthReturns401`
- 操作：GET /api/v1/auth/me 无 header
- 断言：status=401、code=1401

### AC4 — 服务层查询正确性
> `AuthService.getCurrentUserInfo(userId)` 返回完整 MeResponse，角色与权限通过关联表正确聚合。

**用例**：`AuthServiceTest#getCurrentUserInfoAggregatesCorrectly`
- 准备：测试库插入用户、角色、权限、关联数据
- 操作：service.getCurrentUserInfo(adminId)
- 断言：user 不为 null、roles 不为空、permissions 含 "user:view" 等

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托 service→包装 Result）
- [ ] Service 层事务 @Transactional(readOnly=true)
- [ ] 关联查询无 N+1（使用 join 或批量查询）
- [ ] 权限码去重（Set/Stream distinct）
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿