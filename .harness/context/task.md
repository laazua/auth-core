# Sprint 工作单：sprint-022

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-022 |
| 所属模块 | modules |
| 功能点 ID | modules/001 |
| 功能点名称 | 模块 CRUD API+模块下权限级联查询+删除引用保护 |
| 状态 | PLANNED |
| 创建时间 | 2026-08-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/003 | JWT 校验过滤器 + SecurityContext 注入 | ✅ |
| model/003 | sys_module 表迁移+实体+Mapper | ✅ |

## 需求描述

实现模块管理完整 CRUD API：
1. `GET /api/v1/modules` — 分页查询，支持条件（name、code、status）
2. `GET /api/v1/modules/{id}` — 单模块详情
3. `GET /api/v1/modules/{id}/permissions` — 模块下权限级联查询（返回 List<PermissionVO>）
4. `POST /api/v1/modules` — 创建模块，name/code 唯一校验，code 规范
5. `PUT /api/v1/modules/{id}` — 更新模块（name/base_url/description/status，code 不可改）
6. `DELETE /api/v1/modules/{id}` — 删除模块，引用保护（sys_permission 存在则拒绝，code=1302）

## 业务背景

模块是权限的归属容器（架构 §6.1）。架构 §4：分页参数 page/size，响应 data={list,total,page,size}。错误码分段：13xx 模块。RBAC0 §6.1：权限必须归属模块。§6.4：删除模块前校验其下权限引用。

## 交付物

1. `ModuleQueryDTO.java` / `ModuleCreateDTO.java` / `ModuleUpdateDTO.java` / `ModuleVO.java` — DTO（`com.authcore.dto.module`）
2. `ModuleService.java` — 业务逻辑（在 `com.authcore.service`）
3. `ModuleController.java` — REST 端点（在 `com.authcore.controller`）
4. `ModuleControllerTest.java` / `ModuleServiceTest.java` — 测试用例

## 验收标准（TDD 驱动）

### AC1 — 分页查询支持多条件
> GET /api/v1/modules?page=1&size=10&name=用户&status=1 返回 200，data 含 list/total/page/size，list 中模块匹配条件。

**用例**：`ModuleControllerTest#queryModulesWithPaginationAndFilters`
- 准备：种子数据 4 个模块
- 操作：GET /api/v1/modules?name=用户&status=1
- 断言：status=200、total≥1、list 非空、每项含 id/name/code/base_url/description/status

### AC2 — 模块下权限级联查询
> GET /api/v1/modules/{id}/permissions 返回 200，data 为 List<PermissionVO>，含该模块下所有权限。

**用例**：`ModuleControllerTest#queryModulePermissionsCascade`
- 操作：GET /api/v1/modules/1/permissions
- 断言：status=200、list 非空、每项含 id/module_id/name/code/description

### AC3 — 创建模块唯一校验
> POST /api/v1/modules {name, code, base_url, description, status} 返回 200，data.id 非空，重复 name/code 返回 409 code=1301/1302。

**用例**：`ModuleControllerTest#createModuleUniqueNameAndCode`
- 操作：POST {name:"新模块", code:"NEW_MOD", base_url:"http://new", status:1}
- 断言：status=200、id 非空、再次创建同 name 返回 409 code=1301、同 code 返回 409 code=1302

### AC4 — 更新模块 code 不可改
> PUT /api/v1/modules/{id} {name, base_url, description, status} 返回 200，name/base_url/description/status 更新，code 不变。

**用例**：`ModuleControllerTest#updateModuleCodeImmutable`
- 操作：PUT /api/v1/modules/2 {name:"新模块名", status:0}
- 断言：status=200、name/status 更新、code 与原值一致

### AC5 — 删除模块引用保护
> DELETE /api/v1/modules/{id} 若 sys_permission 存在则 409 code=1302，否则 200 物理删除。

**用例**：`ModuleControllerTest#deleteModuleWithReferenceProtection`
- 操作：创建模块并创建权限 → DELETE /api/v1/modules/3 → 409 code=1302
- 断言：无引用时删除成功、有引用时被拒

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托 service→包装 Result）
- [ ] Service @Transactional，创建/更新/删除事务边界正确
- [ ] name/code 唯一校验，code 不可改
- [ ] 分页响应固定结构 {list,total,page,size}
- [ ] 错误码分段：13xx 模块（1301 name 重复、1302 code 重复、1303 引用保护）
- [ ] 删除引用保护：sys_permission 存在拒绝
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿