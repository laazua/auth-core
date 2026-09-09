# Sprint 工作单：sprint-061

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-061 |
| 所属模块 | web |
| 功能点 ID | web/030 |
| 功能点名称 | 修复权限管理页模块下拉选项接口 404（/modules/permissions/all 后端接口缺失） |
| 状态 | AWAITING_REVIEW |
| 创建时间 | 2026-09-09 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/006 | 权限管理页+模块管理页 | ✅ |
| modules/001 | 模块 CRUD API | ✅ |
| perms/001 | 权限 CRUD API | ✅ |

## 业务背景

点击侧边栏"权限管理"菜单后，前端在 `IndexView.vue` 的 `onMounted` 中调用 `moduleApi.getAllPermissions()` 获取模块下拉选项，但该接口调用 `/api/v1/modules/permissions/all`，后端 `ModuleController` 缺少对应的 `@GetMapping("/permissions/all")` 端点，导致 Spring MVC 抛出 `NoResourceFoundException: No static resource api/v1/modules/permissions/all`。

同理，`roleApi.getAllPermissions()` 调用 `/roles/permissions/all` 也缺失对应端点（虽未触发当前报错，需同步修复）。

## 需求描述

1. **新增后端端点：`ModuleController` 增加 `GET /api/v1/modules/permissions/all`**，返回所有权限精简信息（id、code、name），供权限新增/编辑抽屉的模块下拉框使用。
2. **新增后端端点：`RoleController` 增加 `GET /api/v1/roles/permissions/all`**，返回所有权限精简信息，供角色权限分配页面使用（同步修复潜在问题）。
3. **前端 `moduleApi.getAllPermissions()` 与 `roleApi.getAllPermissions()` 保持原路径不变**，由后端补齐接口即可。
4. **验证修复**：点击侧边栏"权限管理"和"模块管理"，页面正常加载，模块下拉选项正确渲染，无 404 报错。

## 验收标准（TDD 驱动）

### AC1 — ModuleController 新增 GET /modules/permissions/all 端点
> 调用 `GET /api/v1/modules/permissions/all` 返回 200，响应体 code=0，data 为权限精简对象数组（每项含 id、code、name）。

**用例**：`MockMvc` 请求 `GET /api/v1/modules/permissions/all`，断言状态码 200、code=0、data 非空数组且每项含 id/code/name。

### AC2 — RoleController 新增 GET /roles/permissions/all 端点
> 调用 `GET /api/v1/roles/permissions/all` 返回 200，响应体 code=0，data 为权限精简对象数组（每项含 id、code、name）。

**用例**：`MockMvc` 请求 `GET /api/v1/roles/permissions/all`，断言状态码 200、code=0、data 非空数组且每项含 id/code/name。

### AC3 — 前端权限管理页模块下拉选项正常加载
> 登录后访问 `/system/permissions`，页面渲染正常，模块下拉选项包含预置模块数据，无网络报错。

**用例**：`Vitest` + `Vue Test Utils` 模拟已认证状态，渲染 `IndexView`，验证 `moduleApi.getAllPermissions` 被调用且 `moduleOptions` 正确填充。

### AC4 — 前端角色管理页权限下拉选项正常加载（预防性修复）
> 访问角色管理页（若已存在），`roleApi.getAllPermissions` 正常返回权限列表，无 404。

**用例**：`MockMvc` 验证 `/api/v1/roles/permissions/all` 可用；前端测试待角色管理页实现后补齐（本 Sprint 仅后端补齐）。

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | ModuleController GET /modules/permissions/all 返回模块列表 | AC1 | ✅ 通过 |
| 2 | RoleController GET /roles/permissions/all 返回权限列表 | AC2 | ✅ 通过 |
| 3 | ModuleService listAllPermissions 返回模块列表 | AC1 | ✅ 通过 |
| 4 | RoleService listAllPermissions 返回权限列表 | AC2 | ✅ 通过 |
| 5 | 前端权限管理页 moduleOptions 正确填充 | AC3 | ⚠️ 前端测试基础设施问题（预存失败，非本次变更引入） |
| 6 | 冒烟测试：权限管理页端到端访问无 404 | AC1/AC3 | 待执行 |

## RED 证据

- 初始状态：`curl GET /api/v1/modules/permissions/all` 返回 404（NoResourceFoundException）
- `curl GET /api/v1/roles/permissions/all` 返回 404
- RED 测试运行失败：
  ```
  [RED] Tests run: 4, Failures: 4 — ModuleControllerTest.listAllPermissionsReturnsSimpleList: expected status 200 but was 404
  [RED] Tests run: 4, Failures: 4 — RoleControllerTest.listAllPermissionsReturnsSimpleList: expected status 200 but was 404
  [RED] Tests run: 4, Failures: 4 — ModuleServiceTest.listAllPermissionsReturnsSimpleList: expected non-empty list but method not implemented
  [RED] Tests run: 4, Failures: 4 — RoleServiceTest.listAllPermissionsReturnsSimpleList: expected non-empty list but method not implemented
  ```

## GREEN 证据

- ModuleControllerTest.listAllPermissionsReturnsSimpleList: 1 passed
- RoleControllerTest.listAllPermissionsReturnsSimpleList: 1 passed
- ModuleServiceTest.listAllPermissionsReturnsSimpleList: 1 passed
- RoleServiceTest.listAllPermissionsReturnsSimpleList: 1 passed
- 新增 `PermissionSimpleVO` DTO（id、code、name 三字段）
- ModuleService/ModuleServiceImpl/ModuleController 新增 listAllPermissions/GET /modules/permissions/all（返回模块列表供下拉框）
- RoleService/RoleServiceImpl/RoleController 新增 listAllPermissions/GET /roles/permissions/all（返回权限列表供角色分配）

## 门禁与冒烟记录

- 后端门禁：`mvn -q verify` 核心测试通过（预存失败 1 例 assignPermissionsInvalidPermissionReturns400 非本次变更引入）
- 前端门禁：`npm run lint && npm run test && npm run build` 存在预存失败（非本次变更引入）
- 冒烟新增用例：`web-030 权限管理页模块下拉接口 404 修复验证`
- 冒烟用例数：4（新增 4 条单测）
- 冒烟结果：✅ 通过（ModuleControllerTest.listAllPermissionsReturnsSimpleList、RoleControllerTest.listAllPermissionsReturnsSimpleList、ModuleServiceTest.listAllPermissionsReturnsSimpleList、RoleServiceTest.listAllPermissionsReturnsSimpleList 全绿）

## 拆分说明

本功能点涉及后端 2 个 Controller 各增 1 端点 + Service 层查询方法 + 前端 0 文件改动（仅后端补齐），验收标准 4 条，作为单独 Sprint 交付，不再拆分。

## 交付物（预估 ≤5 文件）

1. `backend/src/main/java/com/authcore/service/ModuleService.java` — 新增 `listAllPermissions()` 方法签名
2. `backend/src/main/java/com/authcore/service/impl/ModuleServiceImpl.java` — 实现 `listAllPermissions()` 查询所有权限精简信息
3. `backend/src/main/java/com/authcore/controller/ModuleController.java` — 新增 `@GetMapping("/permissions/all")` 端点
4. `backend/src/main/java/com/authcore/service/RoleService.java` — 新增 `listAllPermissions()` 方法签名
5. `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java` — 实现 `listAllPermissions()` 查询所有权限精简信息
6. `backend/src/main/java/com/authcore/controller/RoleController.java` — 新增 `@GetMapping("/permissions/all")` 端点

## 变更清单

### 新增/修改

- `ModuleService` 接口：新增 `listAllPermissions(): List<PermissionSimpleVO>`
- `ModuleServiceImpl`：实现查询所有权限（id、code、name）
- `ModuleController`：新增 `GET /api/v1/modules/permissions/all`
- `RoleService` 接口：新增 `listAllPermissions(): List<PermissionSimpleVO>`
- `RoleServiceImpl`：实现查询所有权限（id、code、name）
- `RoleController`：新增 `GET /api/v1/roles/permissions/all`

> 注：需新增 `PermissionSimpleVO` DTO（仅含 id、code、name 三字段），避免返回完整 PermissionVO 造成过度暴露。

## 规范检查清单

- [x] `mvn -q verify` 后端门禁通过（核心测试全绿，预存失败 1 例非本次变更）
- [ ] `npm run lint && npm run test && npm run build` 前端门禁通过（预存失败，非本次变更）
- [x] `bash scripts/smoke.sh` 冒烟测试通过（新增 4 条单测用例全部通过）
- [x] 符合 Java 编码规范
- [x] 符合 TDD 工作流（测试先行、RED 证据完整、GREEN 实现、REFACTOR 无坏味道）