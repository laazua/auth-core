# Sprint 工作单：sprint-020

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-020 |
| 所属模块 | roles |
| 功能点 ID | roles/002 |
| 功能点名称 | 角色-权限分配 API（批量设置） |
| 状态 | PLANNED |
| 创建时间 | 2026-08-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| roles/001 | 角色 CRUD API | ✅ |
| model/005 | sys_user_role+sys_role_permission 关联表 | ✅ |

## 需求描述

实现角色-权限批量分配接口：
1. `PUT /api/v1/roles/{id}/permissions` — 批量设置角色权限（全量替换）
2. 请求体：`RolePermissionAssignDTO {permissionIds: List<Long>}` — 权限 ID 列表
3. 逻辑：先删除该角色现有所有 sys_role_permission 记录，再批量插入新的 permissionIds
4. 校验：每个 permissionId 必须在 sys_permission 存在，否则 400 code=1201
4. 响应：`Result<Void>` code=0

## 业务背景

角色获得权限的唯一路径是角色-权限关联（RBAC0）。管理员通过此接口为角色分配权限，进而控制用户权限。架构 §4：错误码 12xx 权限。删除保护 §6.4：删除权限前校验 sys_role_permission 引用。

## 交付物

1. `RolePermissionAssignDTO.java` — DTO（`com.authcore.dto.role`）
2. 更新 `RoleService.java` — 新增 `assignPermissions(Long roleId, List<Long> permissionIds)`
3. 更新 `RoleController.java` — 新增 `PUT /{id}/permissions`
4. 测试用例

## 验收标准（TDD 驱动）

### AC1 — 批量分配权限成功
> 给定存在的角色与有效权限 IDs，PUT /api/v1/roles/{id}/permissions {permissionIds} 返回 200，sys_role_permission 表中该角色仅关联指定权限。

**用例**：`RoleControllerTest#assignPermissionsBatchReplace`
- 准备：角色 roleA、权限 perm1/perm2 存在
- 操作：PUT /api/v1/roles/1/permissions {permissionIds:[3,4]}
- 断言：status=200、sys_role_permission 含 (1,3) 和 (1,4) 仅两行

### AC2 — 权限不存在返回 400
> 给定不存在的 permissionId，返回 400，code=1201（权限不存在）。

**用例**：`RoleControllerTest#assignPermissionsInvalidPermissionReturns400`
- 操作：PUT /api/v1/roles/1/permissions {permissionIds:[999]}
- 断言：status=400、code=1201

### AC3 — 角色不存在返回 404
> 给定不存在的 roleId，返回 404，code=1101。

**用例**：`RoleControllerTest#assignPermissionsRoleNotFoundReturns404`
- 操作：PUT /api/v1/roles/99999/permissions {permissionIds:[1]}
- 断言：status=404、code=1101

### AC4 — 空列表清空权限
> permissionIds 为空列表，清空该角色所有权限关联。

**用例**：`RoleControllerTest#assignPermissionsEmptyListClearsPermissions`
- 操作：PUT /api/v1/roles/1/permissions {permissionIds:[]}
- 断言：status=200、sys_role_permission 该角色行数为 0

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托 service→包装 Result）
- [ ] Service @Transactional，全量替换在同一事务
- [ ] 权限存在性校验，无效返回 1201
- [ ] 事务内先删后增，保证原子性
- [ ] 错误码分段：11xx 角色（1101 不存在）、12xx 权限（1201 不存在）
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿