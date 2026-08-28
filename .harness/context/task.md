# Sprint 工作单：sprint-017

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-017 |
| 所属模块 | users |
| 功能点 ID | users/002 |
| 功能点名称 | 用户-角色分配 API（批量设置） |
| 状态 | PLANNED |
| 创建时间 | 2026-08-27 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| users/001 | 用户 CRUD API | ✅ |
| model/005 | sys_user_role+sys_role_permission 关联表 | ✅ |

## 需求描述

实现用户-角色批量分配接口：
1. `PUT /api/v1/users/{id}/roles` — 批量设置用户角色（全量替换）
2. 请求体：`UserRoleAssignDTO {roleIds: List<Long>}` — 角色 ID 列表
3. 逻辑：先删除该用户现有所有 sys_user_role 记录，再批量插入新的 roleIds
4. 校验：每个 roleId 必须在 sys_role 存在且 status=1，否则 400 code=1004
5. 响应：`Result<Void>` code=0

## 业务背景

用户获得权限的唯一路径是角色（RBAC0 §6.2）。管理员通过此接口为用户分配角色，进而获得对应权限。架构 §4：错误码 10xx 用户。删除保护 §6.4：删除角色前校验 sys_user_role 引用。

## 交付物

1. `UserRoleAssignDTO.java` — DTO（`com.authcore.dto.user`）
2. 更新 `UserService.java` — 新增 `assignRoles(Long userId, List<Long> roleIds)`
3. 更新 `UserController.java` — 新增 `PUT /{id}/roles`
4. 测试用例

## 验收标准（TDD 驱动）

### AC1 — 批量分配角色成功
> 给定存在的用户与有效角色 IDs，PUT /api/v1/users/{id}/roles {roleIds} 返回 200，sys_user_role 表中该用户仅关联指定角色。

**用例**：`UserControllerTest#assignRolesBatchReplace`
- 准备：用户 user2、角色 roleA/roleB 存在
- 操作：PUT /api/v1/users/2/roles {roleIds:[3,4]}
- 断言：status=200、sys_user_role 含 (2,3) 和 (2,4) 仅两行

### AC2 — 角色不存在或停用返回 400
> 给定不存在或 status=0 的 roleId，返回 400，code=1004（角色无效）。

**用例**：`UserControllerTest#assignRolesInvalidRoleReturns400`
- 操作：PUT /api/v1/users/2/roles {roleIds:[999]}
- 断言：status=400、code=1004

### AC3 — 用户不存在返回 404
> 给定不存在的 userId，返回 404，code=1001。

**用例**：`UserControllerTest#assignRolesUserNotFoundReturns404`
- 操作：PUT /api/v1/users/99999/roles {roleIds:[1]}
- 断言：status=404、code=1001

### AC4 — 空列表清空角色
> roleIds 为空列表，清空该用户所有角色关联。

**用例**：`UserControllerTest#assignRolesEmptyListClearsRoles`
- 操作：PUT /api/v1/users/2/roles {roleIds:[]}
- 断言：status=200、sys_user_role 该用户行数为 0

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托 service→包装 Result）
- [ ] Service @Transactional，全量替换在同一事务
- [ ] 角色存在性+status 校验，无效返回 1004
- [ ] 事务内先删后增，保证原子性
- [ ] 错误码分段：10xx 用户（1001 不存在、1004 角色无效）
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿