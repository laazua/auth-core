# Sprint 工作单：sprint-065

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-065 |
| 所属模块 | web |
| 功能点 ID | web/034 |
| 功能点名称 | 系统管理菜单用户/角色路由迁移至 /system/users 与 /system/roles |
| 状态 | DONE |
| 创建时间 | 2026-09-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/003 | 主布局(侧边菜单/顶栏)+动态菜单渲染 | ✅ |
| web/004 | 用户管理页(含角色分配/启停用/重置密码) | ✅ |
| web/005 | 角色管理页(含权限分配树) | ✅ |
| web/010 | 修复侧边栏系统管理菜单 404 问题（路由未注册到路由器） | ✅ |
| web/029 | 修复侧边栏菜单导航404问题（动态路由未加载与菜单路径错误） | ✅ |

## 业务背景

侧边栏「系统管理」菜单下「用户管理」「角色管理」两项当前指向顶级路径 `/users`、`/roles`（`frontend/src/composables/useMenu.ts:37,44`），而同组「权限管理」「模块管理」为 `/system/permissions`、`/system/modules`，URL 体系分裂；用户报告访问 `/users`、`/roles` 出现 404。路由侧现状（`frontend/src/router/routes.ts`）同时存在两套：① `/system` 父路由下的占位 children `user`、`role`（指向 `views/system/UserView.vue`、`views/system/RoleView.vue`，URL 为 `/system/user`、`/system/role`，菜单不引用）；② 顶级独立路由 `/users`、`/roles`（指向实际功能页 `views/users/IndexView.vue`、`views/roles/IndexView.vue`）。本次将用户/角色管理的菜单路径与功能路由统一迁移为 `/system/users`、`/system/roles`（作为 `/system` 的 children），并移除重复的 `user`、`role` 占位 children，消除同名菜单双路由的歧义。本需求为纯前端路由重构，不涉及 `docs/01-architecture.md` 的 RBAC 硬语义与 API 契约，无架构冲突。

## 需求描述

1. **菜单路径统一**：`useMenu.ts` 中「用户管理」菜单 path 由 `/users` 改为 `/system/users`、「角色管理」由 `/roles` 改为 `/system/roles`，二者仍为 `/system`（系统管理）父菜单的 children，权限标记（`user:view` / `role:view`）与图标、排序不变。
2. **路由结构统一**：`routes.ts` 中删除顶级 `/users`、`/roles` 路由，将实际功能页 `views/users/IndexView.vue`、`views/roles/IndexView.vue` 注册为 `/system` 路由的 children `users`、`roles`（完整路径 `/system/users`、`/system/roles`），meta（requiresAuth/title/icon/permissions）保持等价迁移；同时删除 `/system` 下指向 `system/UserView.vue`、`system/RoleView.vue` 的 `user`、`role` 占位 children（避免与新路径并存造成同名菜单歧义；`/system` 的 redirect `/system/permissions` 不变）。
3. **导航可达（修复 404）**：登录态下侧边栏菜单点击与直接 `router.push('/system/users')`、`/system/roles` 均命中新路由记录、渲染对应功能页，不落入 404；Generator 须先复现用户报告的旧路径 404 现象并在实现后确认新路径在单测与浏览器（dev 或构建产物）双层可达。
4. **不回归**：`/system/permissions`、`/system/modules` 的菜单与路由不变；受本变更影响的既有断言（`system-menu-path-fix.spec.ts`、`useMenu.spec.ts`、`router.spec.ts` 中旧路径断言）同步更新；全量前端测试零新增失败（基线：2026-09-28 实测 20 failed / 268 passed，属 web/011 预存红项）。

## 验收标准（TDD 驱动）

### AC1 — 菜单配置路径迁移至 /system/users 与 /system/roles ✅ 达成（2026-09-28，用例 1 通过）
> `frontend/src/composables/useMenu.ts` 的 menuConfig 中，「用户管理」菜单项 `path` 字段值为 `'/system/users'`、「角色管理」菜单项 `path` 字段值为 `'/system/roles'`，且两项均位于 `path: '/system'` 菜单项的 `children` 数组内；menuConfig 中不存在 `path: '/users'` 或 `path: '/roles'` 的菜单项。

**用例**：`AC1 ← 用例 system-menu-path-fix.spec.ts#web-034 系统管理路由迁移 > useMenu 用户/角色菜单路径为 /system/users 与 /system/roles`

### AC2 — 路由表注册 /system/users 与 /system/roles 且移除旧路由 ✅ 达成（2026-09-28，用例 2 通过）
> `generateRoutes(roles)` 返回的路由表中：`path: '/system'` 路由的 children 同时包含 `path: 'users'`（完整路径 `/system/users`，component 为 `views/users/IndexView.vue`）与 `path: 'roles'`（完整路径 `/system/roles`，component 为 `views/roles/IndexView.vue`）；返回表中不存在顶级 `path: '/users'`、`path: '/roles'` 路由，`/system` 的 children 中不存在 `path: 'user'`、`path: 'role'` 占位记录；`/system` 的 children 仍包含 `permissions`、`modules`，`/system` 的 redirect 仍为 `/system/permissions`。

**用例**：`AC2 ← 用例 router.spec.ts#web-034 系统管理路由迁移 > generateRoutes 注册 /system/users 与 /system/roles 并移除顶级 /users /roles 与占位 user /role`

### AC3 — 新路径导航可达不 404，旧顶级路径不再作为功能入口 ✅ 达成（2026-09-28，用例 3 通过）
> 测试路由器按 `router/index.ts` 同等方式注册 `staticRoutes`、`layoutRoutes` 与 `generateRoutes` 结果后：`await router.push('/system/users')`，`currentRoute.value.matched` 中存在 `path === '/system/users'` 的记录且 `currentRoute.value.name !== 'NotFound'`；`await router.push('/system/roles')` 同理命中 `/system/roles`；而 `await router.push('/users')`、`await router.push('/roles')` 命中 404（`currentRoute.value.name === 'NotFound'` 或 matched 命中 catch-all），即旧顶级路径不再是功能页入口。

**用例**：`AC3 ← 用例 router.spec.ts#web-034 系统管理路由迁移 > push /system/users /system/roles 可达且旧 /users /roles 落 404`

### AC4 — 既有菜单/路由断言与全量测试零新增失败 ✅ 达成（2026-09-28，用例 4/5 通过，零新增失败）
> 受影响既有断言同步更新后全部通过：`useMenu.spec.ts`（含 `/system/permissions`、`/system/modules` 路径不变、菜单不含 `/system/user`、`/system/role` 占位路径、子路径集合含新路径）与 `system-menu-path-fix.spec.ts`（web/029 旧断言改为新路径口径）；全量 `npm run test` 失败数不超过实现前基线 20 条（零新增失败）；`npm run lint` 与 `npm run build` 通过。

**用例**：`AC4 ← 用例 useMenu.spec.ts#既有菜单断言（更新后）+ npm run test 全量基线对照`

## 测试清单

> 先于实现写出，规则遵循 `.harness/rules/tdd-workflow.md`；Generator 运行确认 RED 后填入实际输出。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | system-menu-path-fix.spec.ts#web034SystemRouteMigration > AC1 useMenu 用户/角色菜单路径为 /system/users 与 /system/roles 且旧顶级路径不存在 | AC1 | ✅ GREEN（先 RED：expected to contain "path: '/system/users'"；后通过，该文件 7 用例全绿） |
| 2 | router.spec.ts#web-034 系统管理路由迁移 > AC2 generateRoutes 注册 /system/users 与 /system/roles 并移除顶级 /users /roles 与占位 user /role | AC2 | ✅ GREEN（先 RED：expected [ 'user', 'role', … ] to include 'users'；后通过） |
| 3 | router.spec.ts#web-034 系统管理路由迁移 > AC3 push /system/users 与 /system/roles 可达且旧 /users /roles 落 404 | AC3 | ✅ GREEN（先 RED：expected false to be true，新路径落 catch-all；后通过，router.spec 11 用例全绿） |
| 4 | useMenu.spec.ts 既有菜单断言更新后全量通过（含 `/system/user`、`/system/role` 不含断言、permissions/modules 不变断言） | AC4 | ✅ GREEN（先 RED：3 条断言失败；后 10 用例全绿） |
| 5 | 全量前端测试基线对照（实现前 20 failed / 268 passed，零新增失败） | AC4 | ✅ 20 failed / 271 passed (291)：失败清单与基线逐条一致（LoginView 2、DefaultLayout 1、TagsView 4、auth-token 2、system/IndexView 11），271=268+3 新增用例 |
| 6 | 冒烟 web-034 系统管理路由迁移验证（1~4 单测过滤跑 + 构建产物断言） | AC1~AC4 | ✅ 单跑 exit=0；全量冒烟中 ✅ 通过 |

## GREEN 证据

- `frontend/src/router/routes.ts:81-96` — `/system` children 占位 `user`、`role`（system/UserView.vue、system/RoleView.vue）替换为 `users`（views/users/IndexView.vue，name `UsersIndex`）、`roles`（views/roles/IndexView.vue，name `RolesIndex`），meta（requiresAuth/title/icon/permissions）等价迁移；`permissions`、`modules` children 与 redirect `/system/permissions` 不变
- `frontend/src/router/routes.ts:117-143（原）` — 顶级 `path: '/users'`、`path: '/roles'` 两条路由删除（异步路由表由 3 条减为 1 条 `/system`）
- `frontend/src/composables/useMenu.ts:37,44` — 菜单 path `/users`→`/system/users`、`/roles`→`/system/roles`（title/icon/permissions/order 不变）
- 三 spec 定向运行：`Test Files 3 passed (3)`，`Tests 28 passed (28)`（system-menu-path-fix 7 + router 11 + useMenu 10）

## RED 证据

- 执行：`npm run test -- --run src/__tests__/system-menu-path-fix.spec.ts src/__tests__/router.spec.ts src/composables/useMenu.spec.ts`（实现前，2026-09-28）
- 关键失败输出（Test Files 3 failed，Tests 9 failed | 19 passed (28)）：

```text
[RED] FAIL  src/__tests__/router.spec.ts > Route Access Control - AC1 > generates routes without admin role when user has permissions
AssertionError: expected [ 'user', 'role', 'permissions', …(1) ] to include 'users'
[RED] FAIL  src/__tests__/router.spec.ts > web-034 系统管理路由迁移 > AC2 generateRoutes 注册 /system/users 与 /system/roles 并移除顶级 /users /roles 与占位 user /role
AssertionError: expected [ 'user', 'role', 'permissions', …(1) ] to include 'users'
[RED] FAIL  src/__tests__/router.spec.ts > web-034 系统管理路由迁移 > AC3 push /system/users 与 /system/roles 可达且旧 /users /roles 落 404
AssertionError: expected false to be true // Object.is equality（push('/system/users') matched 不含 /system/users，落入 catch-all）
[RED] FAIL  src/__tests__/system-menu-path-fix.spec.ts > userMenuPathFixed > useMenu.ts 中"用户管理"菜单路径为 /system/users
AssertionError: expected ... to contain "path: '/system/users'"
[RED] FAIL  src/__tests__/system-menu-path-fix.spec.ts > roleMenuPathFixed > useMenu.ts 中"角色管理"菜单路径为 /system/roles
AssertionError: expected ... to contain "path: '/system/roles'"
[RED] FAIL  src/__tests__/system-menu-path-fix.spec.ts > web034SystemRouteMigration > AC1 useMenu 用户/角色菜单路径为 /system/users 与 /system/roles 且旧顶级路径不存在
AssertionError: expected ... to contain "path: '/system/users'"
[RED] FAIL  src/composables/useMenu.spec.ts > filters menu items based on user permissions for regular user
AssertionError: expected [ '/users', '/roles', …(1) ] to include '/system/users'
[RED] FAIL  src/composables/useMenu.spec.ts > hides entire module when user has no permissions for any child
AssertionError: expected '/users' to be '/system/users'
[RED] FAIL  src/composables/useMenu.spec.ts > menu items point to correct functional pages
AssertionError: expected [ '/users', '/roles', …(2) ] to include '/system/users'
```

- **404 复现留痕**（AC3，实现前实测）：按 `router/index.ts` 同等方式注册路由后 `router.push('/system/users')` 命中 catch-all → 用户期望的目标路径当前确为客户端 404；同一测试中 `push('/users')` 当前**可达**（断言其落 404 失败，即旧路径在路由表单测层存在），说明用户报告的旧路径 404 不由静态路由表缺失直接导致，候选根因为动态路由加载时序（`router/index.ts` guard case 4 失败走 `catch → next()` 兜底放行时旧路径未注册）或部署层回退（web/032 已修 nginx try_files，用户环境产物可能过期）——本次交付以「菜单与路由统一迁移至 /system/users、/system/roles 并单测+浏览器双层可达」收敛该问题，差异如实登记交 Evaluator 对照。
- 根因：`frontend/src/router/routes.ts:117,131` 顶级 `/users`、`/roles` 与 `/system` children 占位 `user`、`role` 双套并存；`frontend/src/composables/useMenu.ts:37,44` 菜单指向旧顶级路径。

## 门禁与冒烟记录

- 后端门禁：`mvn -q verify` ❌ Tests run: 130, Failures: 4, Errors: 2 —— 与实现前基线逐条一致（TestLayersSpec/TestUtilsSpec 无 Docker、SeedDataIntegrationTest sys_role 22≠2、DataSourceConfigBindingTest ×2 环境变量绑定、RoleControllerTest 409 已登记），本功能点零后端改动，沿用户「基线对照推进」裁决
- 前端门禁：`npm run lint` 全量 ❌ 超时卡死（300s 无输出，预存问题，sprint-064 Evaluator 已复现 4 次，本次第 5 次复现）→ 改跑分片 `npx eslint <本次 5 个改动/相关文件>`：仅剩 3 处**预存** prettier 错误（`useMenu.spec.ts:26/207`、`routes.ts:124`，均经 `git show HEAD:` 版本复测确认 HEAD 同样报错），本次引入的 2 处已 REFACTOR 修复，**零新增 lint 告警**；`npm run test` ✅ 20 failed | 271 passed (291)，失败清单与基线 20 条逐条一致、零新增（271 = 基线 268 + 本次 3 条新用例）；`npm run build` ✅ 16.84s
- 冒烟新增用例：`web-034 系统管理路由迁移验证` 1 条（单跑 ✅ exit=0：system-menu-path-fix 7 passed + router.spec 11 passed + useMenu 10 passed + 构建产物含 `/system/users`、`/system/roles` 且 UserView/RoleView 不再打包）
- 冒烟用例数：47（46+1 新增）；口径同步 2 处（用例均保留未删除）：① `web-010` 断言随本功能点演进为新口径（system 子路由 user/role→users/roles、顶级 /users /roles 与占位 user/role 精确匹配断言移除、菜单 /system/users、/system/roles 存在性断言，首跑曾失败即为本变更引入、已修复）；② `web-029` grep 计数随用例数同步（router.spec 9→11、system-menu-path-fix 6→7，useMenu 10 不变）
- 冒烟结果：整体 ❌ 47 用例 10 失败 = **与实现前基线 10 项逐条一致、零新增**（infra-001 健康探测超时、model-008/010 接口 spec 方法数、roles-001/002 409 已登记、web-013/web-026 grep 计数过期、web-020/021/022 构建产物 grep 过期且 web/022 已废弃⚠️）——沿用户「基线对照推进」裁决，交 Evaluator 对照

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 6 个（`frontend/src/router/routes.ts`、`frontend/src/composables/useMenu.ts`、`frontend/src/__tests__/system-menu-path-fix.spec.ts`、`frontend/src/composables/useMenu.spec.ts`、`frontend/src/__tests__/router.spec.ts`、`scripts/smoke.sh`），未超 6 个，不触发拆分，作为单独 Sprint 交付。

不计入变更的说明：`views/users/IndexView.spec.ts`、`views/roles/IndexView.spec.ts` 中的 `/users`、`/roles` 为测试自建路由 fixture（与生产路由表解耦，改动不影响其通过），为控制变更面本次不动；`views/system/UserView.vue`、`views/system/RoleView.vue` 仅移除路由引用、不删除文件。

## 交付物（预估 6 文件）

1. `frontend/src/router/routes.ts` — 删除顶级 `/users`、`/roles`；将功能页注册为 `/system` children `users`、`roles`；删除占位 children `user`、`role`
2. `frontend/src/composables/useMenu.ts` — 菜单 path `/users`→`/system/users`、`/roles`→`/system/roles`
3. `frontend/src/__tests__/system-menu-path-fix.spec.ts` — 旧路径断言改为新路径口径 + 新增 web-034 用例（AC1）
4. `frontend/src/composables/useMenu.spec.ts` — 旧路径断言同步更新（AC4）
5. `frontend/src/__tests__/router.spec.ts` — 新增 web-034 路由注册与可达性用例（AC2/AC3）+ 旧断言更新
6. `scripts/smoke.sh` — 追加 `web-034` 冒烟用例（不删除、不改动既有用例）

## 变更清单

### 新增

- （无）

### 修改

- `frontend/src/router/routes.ts`:81-96 — `/system` children 中占位 `user`、`role`（system/UserView.vue、system/RoleView.vue）替换为 `users`、`roles`（实际功能页 users/IndexView.vue、roles/IndexView.vue，name `UsersIndex`/`RolesIndex`，meta 等价迁移），消除同名菜单双路由歧义
- `frontend/src/router/routes.ts`:117-143（原，删除后下一段起于 117）— 删除顶级 `path: '/users'`、`path: '/roles'` 两条独立路由（含各自 DefaultLayout 包装与空 path 子路由）
- `frontend/src/composables/useMenu.ts`:37 — 菜单「用户管理」`path: '/users'` → `'/system/users'`
- `frontend/src/composables/useMenu.ts`:44 — 菜单「角色管理」`path: '/roles'` → `'/system/roles'`
- `frontend/src/__tests__/system-menu-path-fix.spec.ts`:10-34 — web/029 既有断言更新为新路径口径（`/system/users`、`/system/roles`）
- `frontend/src/__tests__/system-menu-path-fix.spec.ts`:55-67 — 新增 `web034SystemRouteMigration` describe（AC1 用例：新路径存在且旧顶级路径不存在）
- `frontend/src/__tests__/router.spec.ts`:134-140 — AC1 既有断言由「顶级 /users、/roles 存在」改为「/system children 含 users、roles 且顶级旧路径不存在」
- `frontend/src/__tests__/router.spec.ts`:284-335 — 新增 `web-034 系统管理路由迁移` describe（AC2 路由结构断言 + AC3 push 可达/旧路径 404 断言）
- `frontend/src/composables/useMenu.spec.ts`:64-65,78,192-207 — 旧路径断言同步为 `/system/users`、`/system/roles`，并补 2 条旧路径不含断言（数组精确匹配，`/system/users` 不命中 `/system/user`）
- `scripts/smoke.sh`:198-221 — web-010 用例断言随本功能点演进为新口径（用例保留，断言同步）
- `scripts/smoke.sh`:315-317 — web-029 grep 计数同步（router.spec 9→11、system-menu-path-fix 6→7，用例保留）
- `scripts/smoke.sh`:348-361 — 追加 `web-034 系统管理路由迁移验证` 冒烟用例

### 删除

- （无文件删除；`views/system/UserView.vue`、`views/system/RoleView.vue` 仅移除路由引用、文件保留）

## 规范检查清单

- [~] `mvn -q verify` 后端门禁：130 用例 4F+2E，与基线逐条一致（零后端改动，沿用户基线对照裁决）
- [~] `npm run lint && npm run test && npm run build` 前端门禁：lint 全量卡死为预存（本次第 5 次复现）→ 分片 lint 零新增（预存 3 处经 HEAD 版复测确认）；test 零新增失败（20 基线一致，271=268+3）；build ✅ 16.84s
- [~] `bash scripts/smoke.sh`：web-034 单跑 ✅ exit=0；整体 47 用例 10 失败与基线逐条一致、零新增（沿用户基线对照裁决）
- [x] 符合 Vue 3 / Vite / Element Plus 编码规范（分片 eslint 本次代码零告警；TS strict 无 any、无类型错误）
- [x] 符合 TDD 工作流（测试先行 RED 9 failed 实时留痕、GREEN 28/28、REFACTOR prettier 修正后重跑全绿，两段式+refactor 三次提交）

## 评审记录

- 2026-09-28: Evaluator — pass sprint-065（平均分 8.8/10，4 条 AC 全满足；门禁/冒烟亲测：`mvn -q verify` 130 用例 4F+2E、前端 test 20 failed | 271 passed、冒烟 47 用例 10 失败，三者均经与基线逐条对照确认零新增；lint 全量卡死第 6 次复现（exit 124），分片 lint 3 处预存经 `git show 54c941a:` 基线版本复测确认、本次零新增；冒烟增量 +1（web-034 单跑 ✅，smoke_case 48→49 亲测）；`vite preview` 亲测 `/system/users`、`/system/roles` HTTP 200；否决项 6 依用户 2026-09-28「基线对照推进」裁决豁免，豁免依据见挂起区）
