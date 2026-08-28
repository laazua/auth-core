# Sprint 工作单：sprint-021

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-021 |
| 所属模块 | perms |
| 功能点 ID | perms/001 |
| 功能点名称 | 权限 CRUD API（支持按模块分组查询） |
| 状态 | PLANNED |
| 创建时间 | 2026-08-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/003 | JWT 校验过滤器 + SecurityContext 注入 | ✅ |
| model/004 | sys_permission 表迁移+实体+Mapper(FK module_id) | ✅ |

## 需求描述

实现权限管理完整 CRUD API：
1. `GET /api/v1/permissions` — 分页查询，支持条件（name、code、module_id、status）
2. `GET /api/v1/permissions/grouped` — 按模块分组查询（返回 Map<ModuleVO, List<PermissionVO>>）
3. `GET /api/v1/permissions/{id}` — 单权限详情
4. `POST /api/v1/permissions` — 创建权限，name/code 唯一校验，module_id 必填（FK→sys_module）
5. `PUT /api/v1/permissions/{id}` — 更新权限（name/description/module_id，code 不可改）
6. `DELETE /api/v1/permissions/{id}` — 删除权限，引用保护（sys_role_permission 存在则拒绝，code=1202）

## 业务背景

权限管理是 RBAC 核心。架构 §4：分页参数 page/size，响应 data={list,total,page,size}。错误码分段：12xx 权限。RBAC0 §6.1：权限必须归属模块（module_id 非空）。§6.4：删除权限前校验 sys_role_permission 引用。

## 交付物

1. `PermissionQueryDTO.java` / `PermissionCreateDTO.java` / `PermissionUpdateDTO.java` / `PermissionVO.java` / `ModuleVO.java` — DTO（`com.authcore.dto.permission`）
2. `PermissionService.java` — 业务逻辑（在 `com.authcore.service`）
3. `PermissionController.java` — REST 端点（在 `com.authcore.controller`）
4. `PermissionControllerTest.java` / `PermissionServiceTest.java` — 测试用例

## 验收标准（TDD 驱动）

### AC1 — 分页查询支持多条件
> GET /api/v1/permissions?page=1&size=10&name=用户&module_id=1 返回 200，data 含 list/total/page/size，list 中权限匹配条件。

**用例**：`PermissionControllerTest#queryPermissionsWithPaginationAndFilters`
- 准备：种子数据 14 个权限
- 操作：GET /api/v1/permissions?name=用户&module_id=1
- 断言：status=200、total≥1、list 非空、每项含 id/name/code/module_id/description

### AC2 — 按模块分组查询
> GET /api/v1/permissions/grouped 返回 200，data 为 Map<ModuleVO, List<PermissionVO>>，每模块下含其权限列表。

**用例**：`PermissionControllerTest#queryPermissionsGroupedByModule`
- 操作：GET /api/v1/permissions/grouped
- 断言：status=200、data 为 Map、key 含模块、value 含该模块下权限

### AC3 — 创建权限唯一校验 + module_id 必填
> POST /api/v1/permissions {name, code, module_id, description} 返回 200，data.id 非空，重复 name/code 返回 409 code=1201/1202，缺 module_id 返回 400。

**用例**：`PermissionControllerTest#createPermissionUniqueAndModuleRequired`
- 操作：POST {name:"新权限", code:"perm:new", module_id:1}
- 断言：status=200、id 非空、再次创建同 name 返回 409 code=1201、同 code 返回 409 code=1202、无 module_id 返回 400

### AC4 — 更新权限 code 不可改
> PUT /api/v1/permissions/{id} {name, description, module_id} 返回 200，name/description/module_id 更新，code 不变。

**用例**：`PermissionControllerTest#updatePermissionCodeImmutable`
- 操作：PUT /api/v1/permissions/2 {name:"新权限名", module_id:2}
- 断言：status=200、name/module_id 更新、code 与原值一致

### AC5 — 删除权限引用保护
> DELETE /api/v1/permissions/{id} 若 sys_role_permission 存在则 409 code=1202，否则 200 物理删除。

**用例**：`PermissionControllerTest#deletePermissionWithReferenceProtection`
- 操作：创建权限并分配给角色 → DELETE /api/v1/permissions/3 → 409 code=1202
- 断言：无引用时删除成功、有引用时被拒

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托 service→包装 Result）
- [ ] Service @Transactional，创建/更新/删除事务边界正确
- [ ] name/code 唯一校验，module_id 必填，code 不可改
- [ ] 分页响应固定结构 {list,total,page,size}；分组查询返回 Map
- [ ] 错误码分段：12xx 权限（1201 name 重复、1202 code 重复、1203 引用保护）
- [ ] module_id 逻辑外键校验（存在性）
- [ ] 删除引用保护：sys_role_permission 存在拒绝
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿