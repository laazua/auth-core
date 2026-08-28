# Sprint 工作单：sprint-019

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-019 |
| 所属模块 | roles |
| 功能点 ID | roles/001 |
| 功能点名称 | 角色 CRUD API |
| 状态 | PLANNED |
| 创建时间 | 2026-08-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/003 | JWT 校验过滤器 + SecurityContext 注入 | ✅ |
| model/002 | sys_role 表迁移+实体+Mapper | ✅ |

## 需求描述

实现角色管理完整 CRUD API：
1. `GET /api/v1/roles` — 分页查询，支持条件（name、code、status）
2. `GET /api/v1/roles/{id}` — 单角色详情
3. `POST /api/v1/roles` — 创建角色，name/code 唯一校验，code 规范（如 ROLE_ADMIN）
4. `PUT /api/v1/roles/{id}` — 更新角色（name/code/status，code 不可改）
5. `DELETE /api/v1/roles/{id}` — 删除角色，引用保护（sys_user_role/sys_role_permission 存在则拒绝，code=1102/1103）

## 业务背景

角色管理是 RBAC 核心。架构 §4：分页参数 page/size，响应 data={list,total,page,size}。错误码分段：11xx 角色。RBAC0 §6.4：删除角色前校验 sys_user_role/sys_role_permission 引用。

## 交付物

1. `RoleQueryDTO.java` / `RoleCreateDTO.java` / `RoleUpdateDTO.java` / `RoleVO.java` — DTO（`com.authcore.dto.role`）
2. `RoleService.java` — 业务逻辑（在 `com.authcore.service`）
3. `RoleController.java` — REST 端点（在 `com.authcore.controller`）
4. `RoleControllerTest.java` / `RoleServiceTest.java` — 测试用例

## 验收标准（TDD 驱动）

### AC1 — 分页查询支持多条件
> GET /api/v1/roles?page=1&size=10&name=admin&status=1 返回 200，data 含 list/total/page/size，list 中角色匹配条件。

**用例**：`RoleControllerTest#queryRolesWithPaginationAndFilters`
- 准备：种子数据 admin 角色 + 测试角色
- 操作：GET /api/v1/roles?name=adm&status=1
- 断言：status=200、total≥1、list 非空、每项含 id/name/code/status

### AC2 — 创建角色唯一校验
> POST /api/v1/roles {name, code, status} 返回 200，data.id 非空，重复 name/code 返回 409 code=1101/1102。

**用例**：`RoleControllerTest#createRoleUniqueNameAndCode`
- 操作：POST /api/v1/roles {name:"测试角色", code:"ROLE_TEST", status:1}
- 断言：status=200、id 非空、再次创建同 name 返回 409 code=1101、同 code 返回 409 code=1102

### AC3 — 更新角色 code 不可改
> PUT /api/v1/roles/{id} {name, status} 返回 200，name/status 更新，code 不变。

**用例**：`RoleControllerTest#updateRoleCodeImmutable`
- 操作：PUT /api/v1/roles/2 {name:"新角色名", status:0}
- 断言：status=200、name/status 更新、code 与原值一致

### AC4 — 删除角色引用保护
> DELETE /api/v1/roles/{id} 若 sys_user_role 或 sys_role_permission 存在则 409 code=1102/1103，否则 200 物理删除。

**用例**：`RoleControllerTest#deleteRoleWithReferenceProtection`
- 操作：创建角色并分配给用户 → DELETE /api/v1/roles/3 → 409 code=1102
- 操作：创建角色并分配权限 → DELETE /api/v1/roles/4 → 409 code=1103
- 断言：无引用时删除成功、有引用时被拒

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托 service→包装 Result）
- [ ] Service @Transactional，创建/更新/删除事务边界正确
- [ ] name/code 唯一校验，code 不可改
- [ ] 分页响应固定结构 {list,total,page,size}
- [ ] 错误码分段：11xx 角色（1101 name 重复、1102 code 重复、1103 用户引用、1104 权限引用）
- [ ] 删除引用保护：sys_user_role/sys_role_permission 存在拒绝
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿