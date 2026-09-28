# Sprint 工作单：sprint-066

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-066 |
| 所属模块 | web |
| 功能点 ID | web/035 |
| 功能点名称 | 修复登录后首次导航受保护路由落 404 问题（动态路由首载 next 展开 to 携带 NotFound name 陷阱） |
| 状态 | PLANNED |
| 创建时间 | 2026-09-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | Vite+Vue3+TS+Router 骨架与 Vitest 基线 | ✅ |
| web/029 | 修复侧边栏菜单导航404问题（Router Guard case 3/case 4 时序基线） | ✅ |
| web/034 | 系统管理菜单用户/角色路由迁移至 /system/users 与 /system/roles | ✅ |

## 业务背景

用户报告：登出 → 刷新页面 → 重新登录 → 点击侧边栏「用户管理」→ 落 404；点击系统管理其他功能正常；再次点击用户管理恢复正常。

Planner 只读取证（一次性脚本 `/tmp/opencode/repro-guard.mjs`，复刻 `frontend/src/router/index.ts` 守卫时序，不入仓库）已复现并定位根因：

1. 刷新后 `dynamicRoutesLoaded=false`；登录后 Dashboard 导航走 case 3（`index.ts:61-65`）放行，**不加载**动态路由；
2. 首次点击「用户管理」`push('/system/users')`：目标未注册，落入 catch-all，`to.name==='NotFound'`、`to.path='/system/users'`；
3. case 4（`index.ts:68-76`）执行 `router.addRoute`（成功）后 `next({ ...to, replace: true })`——**展开携带 `name:'NotFound'`，vue-router 按 name 优先解析**，重导航回 NotFound 记录 → 首次点击渲染 404；此时动态路由已在同轮加载，故第二次点击直接命中 `/system/users` 恢复正常。
4. 修复写法取证：`next({ path: to.fullPath, replace: true })`（剥离 name，经 fullPath 保留 query/hash）后，首次点击即 `name=UsersIndex, matched=['/system','/system/users']`。

该缺陷由 web/029 引入（case 4 写法），不特定于用户管理——**任何登录会话中首次点击的受保护非 Dashboard 路由均中招**，用户首次点击恰为用户管理。症状三段（首击 404 / 其他功能正常 / 再击正常）与取证逐条吻合。纯前端守卫缺陷，不涉及 `docs/01-architecture.md` RBAC 硬语义，无架构冲突。

## 需求描述

1. **首达修复**：动态路由首次加载（case 4）后的重导航必须按**目标 path** 解析（不得携带 NotFound name），登录会话中首次点击侧边栏任意受保护路由一次即达目标页面，不落 404。
2. **参数保留**：首载重导航保留目标 URL 的 query 与 hash（按 fullPath 导航）。
3. **守卫语义不回归**：web/029 既有语义保持——Dashboard 首次导航仍提前放行且不提前加载动态路由（case 3）、动态路由已加载后（case 4 条件不满足）二次导航正常直达；登出-登录与刷新场景均不出现 404。
4. **不回归**：全量前端测试零新增失败（基线：2026-09-28 实测 20 failed / 271 passed，属 web/011 预存红项）；冒烟追加 web-035 用例。

## 验收标准（TDD 驱动）

### AC1 — 登录后首次导航受保护路由一次即达不落 404
> 测试内 mock `@/stores/auth` 为已认证，`import router from '@/router'`（真实守卫单例）：先 `await router.push('/dashboard')`（case 3 放行、动态路由未加载），再 `await router.push('/system/users')`，断言 `router.currentRoute.value.name === 'UsersIndex'` 且 `matched` 中存在 `path==='/system/users'`、`name !== 'NotFound'`；随后 `await router.push('/system/permissions')` 同样命中（name==='Permissions'、非 NotFound）。

**用例**：`AC1 ← 用例 router.spec.ts#web-035 动态路由首载导航 > 登录后首次导航 /system/users 与 /system/permissions 一次即达不落 404`

### AC2 — 首载重导航保留目标 query 参数
> 同 AC1 时序：`await router.push({ path: '/system/users', query: { page: '2' } })`，断言 `router.currentRoute.value.name === 'UsersIndex'` 且 `router.currentRoute.value.fullPath === '/system/users?page=2'`（query 未丢失）。

**用例**：`AC2 ← 用例 router.spec.ts#web-035 动态路由首载导航 > 首载导航保留目标 query 参数`

### AC3 — web/029 守卫语义不回归且二次导航正常
> 既有用例全部继续通过：`router.spec.ts` 中 `Dashboard navigation does not set dynamicRoutesLoaded flag before routes are loaded`（case 3 不提前加载）与 `subsequent navigation to /users loads dynamic routes after Dashboard`（既有时序复刻用例）保持 GREEN；本 describe 内新增断言：AC1 序列完成后再次 `await router.push('/system/roles')` 命中 `name==='RolesIndex'`（动态路由已加载场景二次导航直达）。

**用例**：`AC3 ← 用例 router.spec.ts#web-035 动态路由首载导航 > 动态路由加载完成后二次导航直达且 web/029 既有用例保持通过`

### AC4 — 全量测试零新增失败且冒烟 web-035 通过
> `npm run test` 失败数不超过实现前基线 20 条（零新增失败，清单与 2026-09-28 基线逐条一致）；`npm run lint`（全量卡死为预存，分片 lint 对本次改动文件零新增告警）与 `npm run build` 通过；`bash scripts/smoke.sh` 新增 `web-035` 用例且单跑通过。

**用例**：`AC4 ← 用例 npm run test 全量基线对照 + bash scripts/smoke.sh#web-035 动态路由首载导航验证`

## 测试清单

> 先于实现写出，规则遵循 `.harness/rules/tdd-workflow.md`；Generator 运行确认 RED 后填入实际输出。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | router.spec.ts#web-035 动态路由首载导航 > 登录后首次导航 /system/users 与 /system/permissions 一次即达不落 404 | AC1 | ⬜ 待 RED→GREEN |
| 2 | router.spec.ts#web-035 动态路由首载导航 > 首载导航保留目标 query 参数 | AC2 | ⬜ 待 RED→GREEN |
| 3 | router.spec.ts#web-035 动态路由首载导航 > 动态路由加载完成后二次导航直达且 web/029 既有用例保持通过 | AC3 | ⬜ 待 RED→GREEN |
| 4 | 全量前端测试基线对照（实现前 20 failed / 271 passed，零新增失败） | AC4 | ⬜ 待基线复测 |
| 5 | 冒烟 web-035 动态路由首载导航验证 | AC1~AC4 | ⬜ 待追加 |

## RED 证据

> Generator 于实现前执行测试清单 #1~#3 并粘贴关键失败输出。

## 门禁与冒烟记录

> Generator 填写：前端门禁、后端门禁（零后端改动，基线对照口径）、`bash scripts/smoke.sh`（新增 web-035）。

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 3 个（`frontend/src/router/index.ts` 守卫 1 行级修复、`frontend/src/__tests__/router.spec.ts` 新增 web-035 用例、`scripts/smoke.sh` 追加用例），未超 6 个，不触发拆分，作为单独 Sprint 交付。

## 交付物（预估 3 文件）

1. `frontend/src/router/index.ts` — case 4 重导航由 `next({ ...to, replace: true })` 改为按目标 path 解析（如 `next({ path: to.fullPath, replace: true })`），剥离 NotFound name 陷阱
2. `frontend/src/__tests__/router.spec.ts` — 新增 web-035 describe（AC1/AC2/AC3 用例，先于实现；AC1/AC2 用真实 `@/router` 单例跑完整时序）
3. `scripts/smoke.sh` — 追加 `web-035` 冒烟用例（不删除、不改动既有用例）

## 变更清单

### 修改

- `frontend/src/router/index.ts`：
  - case 4（约 :75）`next({ ...to, replace: true })` → `next({ path: to.fullPath, replace: true })`：`to` 在目标未注册时 `name==='NotFound'`，展开后 vue-router 按 name 优先解析致重导航回 catch-all；按 `fullPath` 导航同时保留 query/hash
- `frontend/src/__tests__/router.spec.ts`：
  - 新增 `web-035 动态路由首载导航` describe（AC1/AC2/AC3），AC1/AC2 直接 `import router from '@/router'` 复用真实守卫（非复刻），需注意单例动态路由状态在一个用例内完成完整时序
- `scripts/smoke.sh`：
  - 追加 `web-035` 用例：过滤跑 web-035 3 条单测 + `npm run build`（形态对齐 web-034）

## 规范检查清单

- [ ] `mvn -q verify` 后端门禁（本功能点零后端改动，基线对照口径）
- [ ] `npm run lint && npm run test && npm run build` 前端门禁（test 零新增失败；lint 全量卡死为预存，分片零新增）
- [ ] `bash scripts/smoke.sh` 新增 web-035 单跑通过（整体预存红项基线对照口径）
- [ ] 符合 Vue 3 / Vite / TypeScript strict 编码规范
- [ ] 符合 TDD 工作流（测试先行、RED 证据完整、GREEN 实现、REFACTOR 全绿）

## 评审记录

> Evaluator 填写。
