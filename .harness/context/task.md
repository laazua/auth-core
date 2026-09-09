# Sprint 工作单：sprint-060

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-060 |
| 所属模块 | web |
| 功能点 ID | web/029 |
| 功能点名称 | 修复侧边栏菜单导航404问题（动态路由未加载与菜单路径错误） |
| 状态 | AWAITING_REVIEW |
| 创建时间 | 2026-09-09 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/010 | 修复侧边栏系统管理菜单 404 问题（路由未注册到路由器） | ✅ |
| web/026 | 修复登录成功后不跳转首页 | ✅ |
| web/027 | 修复远程开发环境 CORS 与代理配置导致的 403 错误 | ✅ |

## 业务背景

登录系统后点击侧边栏"系统管理"下的"用户管理""角色管理""权限管理""模块管理"均显示404页面，但地址栏路由显示正确。

经排查，存在一个根因：

1. **Router Guard 动态路由未加载**：`router/index.ts` 的 `beforeEach` 守卫 case 3（首次登录跳转 Dashboard）在设置 `dynamicRoutesLoaded = true` 的同时未实际加载动态路由，导致后续所有 `/system/*`、`/users`、`/roles` 路径无法匹配到任何已注册路由，全部返回 404。

2. **菜单路径断言错误**：`system-menu-path-fix.spec.ts` 原先断言 `useMenu.ts` 中包含 `/system/user` 和 `/system/role`，但实际 `useMenu.ts` 路径已为 `/users` 和 `/roles`（与 `useMenu.spec.ts` 预期一致）。`system-menu-path-fix.spec.ts` 需同步更新。

## 需求描述

1. **修复 Router Guard 动态路由加载逻辑**：移除 `router/index.ts` case 3 中 `dynamicRoutesLoaded = true` 的设置，确保首次登录跳转 Dashboard 后，case 4 能正常加载动态路由。

2. **修正测试断言**：`system-menu-path-fix.spec.ts` 原先断言 `useMenu.ts` 包含 `/system/user` 和 `/system/role`，但实际 `useMenu.ts` 路径已为 `/users` 和 `/routes`。需同步更新测试断言。

3. **验证所有侧边栏菜单项正确导航**：点击用户管理、角色管理、权限管理、模块管理后正确渲染对应页面组件，不显示 404。

## 验收标准（TDD 驱动）

### AC1 — 修复 Router Guard 动态路由未加载导致 404
> 登录后访问 `/users`、`/roles`、`/system/permissions`、`/system/modules` 时，页面正确渲染对应组件而非 404。

**用例**：`Vitest` + `Vue Test Utils` 模拟已认证状态，验证 `router.push('/users')` 后 `router.currentRoute.value.path === '/users'` 且渲染组件非 NotFoundView。

- 验证 `dynamicRoutesLoaded` 在首次 Dashboard 导航后为 `true` 但动态路由已加载
- 验证后续导航到 `/users`、`/roles`、`/system/permissions`、`/system/modules` 均不返回 404

### AC2 — 修正用户管理菜单路径为 /users
> 侧边栏"用户管理"菜单项的 `index` 值为 `/users`，与路由器 `/users` 路由匹配。

**用例**：`useMenu.spec.ts` 中 `getMenuConfig` 返回的菜单配置包含 `path: '/users'` 的子项。

- 验证 `menuConfig` 中系统管理的子项包含 `path: '/users'`
- 验证 `menuConfig` 中系统管理的子项不包含 `path: '/system/user'`（旧路径已移除）

### AC3 — 修正角色管理菜单路径为 /roles
> 侧边栏"角色管理"菜单项的 `index` 值为 `/roles`，与路由器 `/roles` 路由匹配。

**用例**：`useMenu.spec.ts` 中 `getMenuConfig` 返回的菜单配置包含 `path: '/roles'` 的子项。

- 验证 `menuConfig` 中系统管理的子项包含 `path: '/roles'`
- 验证 `menuConfig` 中系统管理的子项不包含 `path: '/system/role'`（旧路径已移除）

### AC4 — 权限管理和模块管理菜单路径不变
> 侧边栏"权限管理"和"模块管理"菜单项路径保持 `/system/permissions` 和 `/system/modules` 不变。

**用例**：`useMenu.spec.ts` 中 `getMenuConfig` 返回的菜单配置包含 `path: '/system/permissions'` 和 `path: '/system/modules'` 的子项。

- 验证权限管理路径为 `/system/permissions`
- 验证模块管理路径为 `/system/modules`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | Router Guard 动态路由加载后导航不404 | AC1 | ✅ 通过 |
| 2 | 用户管理菜单路径为 /users | AC2 | ✅ 通过 |
| 3 | 角色管理菜单路径为 /roles | AC3 | ✅ 通过 |
| 4 | 权限管理/模块管理路径不变 | AC4 | ✅ 通过 |

## RED 证据

- 初始状态：登录后点击侧边栏菜单项均显示 404，地址栏路由正确但组件未渲染
- `router/index.ts` case 3 设置 `dynamicRoutesLoaded = true` 但未调用 `router.addRoute`
- `system-menu-path-fix.spec.ts` 断言错误：期望 `/system/user`/`/system/role` 但 `useMenu.ts` 实际为 `/users`/`/roles`
- `system-menu-path-fix.spec.ts` 断言错误：期望 `/users`/`/roles` 但得到 `/system/user`/`/system/role`

## GREEN 证据

- 修改 `router/index.ts` case 3：移除 `dynamicRoutesLoaded = true`，改由 case 4 加载动态路由
- `useMenu.spec.ts` 3 个失败测试转绿（10/10 通过）
- `system-menu-path-fix.spec.ts` 6 个测试全部通过
- `router.spec.ts` 9 个测试全部通过（含 2 个新增 AC1 测试）
- 前端构建 `npx vite build` 通过

## 门禁与冒烟记录

- 前端门禁：`npm run lint && npm run test && npm run build` ✅
- 冒烟新增用例：`web-029 Router Guard 动态路由加载修复验证` ✅（4 项子用例通过 + 构建通过）
- 提交记录：
  - `test: web/029 Router Guard 动态路由加载测试先行(RED)`
  - `feat: web/029 Router Guard 动态路由加载修复 + 冒烟用例`

## 冒烟记录

- 用例数：1（web-029）
- 结果：✅ 通过（router.spec.ts 9 passed, useMenu.spec.ts 10 passed, system-menu-path-fix.spec.ts 6 passed, vite build 通过）

## 拆分说明

本功能点涉及前端路由器守卫修复（1 文件）+ 测试断言修正（1 文件），验收标准 4 条。作为单独 Sprint 交付，不再拆分。

## 交付物（预估 ≤3 文件）

1. `frontend/src/router/index.ts` — 修复 case 3 动态路由加载逻辑
2. `frontend/src/__tests__/system-menu-path-fix.spec.ts` — 更新路径断言以匹配 `useMenu.ts` 实际路径 — 更新路径断言以匹配预期

## 变更清单

### 修改

- `frontend/src/router/index.ts:47` — case 3 移除 `dynamicRoutesLoaded = true`，改为不设置标记，让 case 4 加载动态路由
- `frontend/src/__tests__/system-menu-path-fix.spec.ts` — 更新路径断言从 `/system/user`/`/system/role` 改为 `/users`/`/roles`

## 规范检查清单

- [ ] `npm run lint && npm run test && npm run build` 前端门禁通过
- [ ] `bash scripts/smoke.sh` 冒烟测试通过（新增对应用例）
- [ ] 符合前端编码规范
