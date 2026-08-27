# Sprint 工作单：sprint-008

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-008 |
| 所属模块 | model |
| 功能点 ID | model/005 |
| 功能点名称 | sys_user_role + sys_role_permission 关联表 |
| 状态 | PLANNED |
| 创建时间 | 2026-08-26 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| model/001 | sys_user 迁移+实体+Mapper | ✅ |
| model/002 | sys_role 迁移+实体+Mapper | ✅ |
| model/004 | sys_permission 迁移+实体+Mapper(FK module_id) | ✅ |

## 需求描述

实现 RBAC0 用户-角色/角色-权限关联表：两张关联表的 Flyway 迁移脚本 + Java 实体 + Mapper，为后续 auth/users/roles 模块提供关联数据访问基础。

## 业务背景

关联表是 RBAC 模型的骨架：用户通过 sys_user_role 获得角色，角色通过 sys_role_permission 获得权限（架构 §6 RBAC0 硬语义）。删除保护语义要求：删除角色前校验 sys_user_role 引用，删除权限前校验 sys_role_permission 引用（架构 §6 第 4 条）。

## 交付物

1. Flyway 迁移脚本：`sys_user_role` + `sys_role_permission`（联合唯一索引）
2. Java 实体：`UserRoleEntity` + `RolePermissionEntity`
3. Mapper 接口：`UserRoleMapper` + `RolePermissionMapper`

## 验收标准（TDD 驱动）

### AC1 — 迁移脚本可应用
> Flyway 执行迁移脚本后，`sys_user_role` 和 `sys_role_permission` 两张表存在于目标数据库，每张表含 `id`（PK BIGINT AUTO_INCREMENT）、业务字段（user_id/role_id 或 role_id/permission_id）、`created_at` 默认当前时间。

**用例**：`UserRoleMapperTest#migrationCreatesTablesAndColumns`
- 准备：Flyway 已配置，测试库可用
- 断言：`SELECT COUNT(*) FROM sys_user_role` 与 `SELECT COUNT(*) FROM sys_role_permission` 无异常；表含正确列

### AC2 — 联合唯一约束生效
> 向 `sys_user_role` 插入重复的 (user_id, role_id) 对时，数据库拒绝并抛 `DuplicateKeyException`；`sys_role_permission` 同理。

**用例**：`UserRoleMapperTest#duplicateUserRoleIdThrowsDuplicateKeyException`
- 准备：已插入 (user_id=1, role_id=1)
- 操作：再次插入 (1, 1)
- 断言：`DuplicateKeyException`

### AC3 — 实体映射正确
> `UserRoleEntity` 含 id/userId/roleId/createdAt/updatedAt 字段，`RolePermissionEntity` 含 id/roleId/permissionId/createdAt/updatedAt 字段；字段名与表列自动映射（snake_case ↔ camelCase）。

**用例**：`UserRoleMapperTest#entityFieldsMatchTableColumns`
- 准备：构造实体对象（id=1, userId=1, roleId=1）
- 操作：插入并查回
- 断言：各字段值与插入时一致

### AC4 — Mapper 接口可用
> `UserRoleMapper` 和 `RolePermissionMapper` 继承 `BaseMapper`，注入无异常。

**用例**：`UserRoleMapperTest#mapperInjectionSucceeds`
- 断言：`@Autowired` 注入不抛异常；mapper 不为 null

## 规范检查清单（Evaluator 逐项核对）

- [ ] 迁移脚本命名 `V{n}__{描述}.sql`，内容含联合唯一约束
- [ ] 实体字段命名 snake_case ↔ camelCase 映射正确；表名 `sys_` 前缀
- [ ] 行数/方法长度达标（coding-standards §1）
- [ ] 无 N+1、无 select *、批量分批 ≤1000（coding-standards §4）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿
