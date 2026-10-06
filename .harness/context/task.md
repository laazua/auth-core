# Sprint 工作单：sprint-073

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-073 |
| 所属模块 | web |
| 功能点 ID | web/038d |
| 功能点名称 | 模块服务统一入口（d 段）——「我的模块」菜单页（前端） |
| 状态 | IN_PROGRESS（RED 已留痕） |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | Vite+Vue3+TS+Axios+Vitest 基线（http 封装、vitest 跑法） | ✅ |
| web/002 | 登录页+路由守卫（requiresAuth 守卫链，新路由免额外守卫代码） | ✅ |
| web/003 | 主布局（侧边菜单/顶栏）+动态菜单渲染（`useMenu` menuConfig 过滤机制本体，注册入口） | ✅ |
| web/029 | 动态路由加载机制（`router/index.ts:72-73` 登录后 `generateRoutes`+`addRoute`） | ✅ |
| web/034 | `/system` URL 体系（本单新顶级路径 `/mymodules` 与其无冲突先例） | ✅ |
| web/038b | `GET /modules/accessibles` 可访问模块列表（本页唯一数据源，sprint-071 评审通过 9.0/10） | ✅ |
| web/038c | baseUrl/createTime 字段契约收敛（accessibles 响应六字段契约稳定，sprint-072 评审通过 9.0/10，硬前置已解除） | ✅ |

## 业务背景

用户原始需求（sprint-069/072 一组，设计文档 `docs/superpowers/specs/2026-09-29-module-iframe-access-design.md` 用户逐节确认，commit 83b19fb）：模块配置完成后，**登录用户应在当前系统中看到自己可以访问的模块列表**（入口 = 新菜单页「我的模块」+ 管理页「进入」，见设计 §1 澄清 4）。本单实现其中「我的模块」菜单页，即设计 §4.1：

- 「我的模块」菜单：**全员登录可见**（按 web/003 动态菜单渲染机制注册，**不绑具体权限码**，内容自过滤——`useMenu.ts:75-87` 对无 `permissions`/`roles` 键的条目恒放行，先例：`/dashboard`）；
- 页面 `MyModulesView`：调 `GET /modules/accessibles`，卡片网格（**名称/编码/描述**，设计仅此三字段），空态「暂无可访问模块」，点卡片跳 `/workspace/module/:code`。

设计 §4.4 中「types/module.ts 读 createdAt」为设计期草案，**已被 sprint-072 用户裁定的方案 B 覆盖**（后端 ModuleVO 输出 `createTime`、前端零改动、038c 实际交付口径见 registry/web/038 行），本单不改 `types/module.ts`。

**范围边界**（设计 §4 拆分 + registry 038d/038e 分工）：iframe 视图页 `ModuleIframeView` 与管理页操作列「进入」按钮属 **web/038e**（前置 038a/038d）；点卡目标路由 `/workspace/module/:code` 由 038e 注册——**本单断言导航目标（意图级），038e 未就绪期点卡落 404 为组内已知中间态**，非本单缺陷。

## 需求描述

新增「我的模块」功能页：顶级动态路由 `/mymodules`（`meta.requiresAuth=true`、**无** `permissions`/`roles` 键）+ `menuConfig` 同路径菜单条目（**无** `permissions` 键 → 零权限用户亦可见）+ `MyModulesView` 页面（`moduleApi.getAccessibles()` 拉取 → 卡片网格渲染 name/code/description；空数组渲染「暂无可访问模块」空态；点卡 `router.push('/workspace/module/' + code)`）。`api/module.ts` 新增 `getAccessibles()`（`GET /modules/accessibles`），`ModuleAccessible` 类型**就近内联声明于 api 文件**（先例：同文件 `getAllPermissions` 内联返回类型，省 `types/module.ts` 第 7 文件，守 6 文件预算）。纯前端单，零后端改动。

## 验收标准（TDD 驱动）

- [ ] AC1 — 路由与菜单全员可见：路由表含 `/mymodules`（`requiresAuth=true`、无 `permissions`/`roles`），**零权限**用户（`hasAnyPermission`/`hasAnyRole` 恒 false）`getSortedMenuTree()` 仍含路径 `/mymodules`、标题「我的模块」的菜单条目
- [ ] AC2 — 列表渲染与空态：`moduleApi.getAccessibles` 存在（函数类型断言）；mock 返回 2 项 → 渲染 2 张卡片且各含 name/code/description 文本；mock 返回 `[]` → 显示「暂无可访问模块」且无卡片
- [ ] AC3 — 点卡导航：点击 `code=news` 的卡片 → `router.push('/workspace/module/news')`（目标路由由 038e 注册，本单断言导航目标）
- [ ] AC4 — 后端门禁零新增失败（零后端改动，基线对照）；前端三件套通过（新增 4 用例全绿、失败数保持基线）；冒烟 `web-038d` 单跑通过、整体与基线一致

### AC1 — 路由与菜单全员可见
> Given 模拟零权限已登录用户（`hasAnyPermission`/`hasAnyRole` 恒 false）。When 读取 `staticRoutes+generateRoutes` 合并后路由与 `getSortedMenuTree()`。Then 路由含 `path=/mymodules`、`meta.requiresAuth=true` 且 `meta` 无 `permissions`/`roles` 键；菜单树含 `/mymodules` 条目且 `title=我的模块`（系统管理子项因零权限被过滤亦可并存断言）。

**用例**：`AC1 ← 用例 MyModulesView.spec#myModulesMenuAndRouteVisibleWithoutPermission`

### AC2 — 列表渲染与空态
> Given `moduleApi.getAccessibles` 存在且被 mock。When mock 返回 2 项（各含 name/code/description）挂载 `MyModulesView`。Then 渲染 2 张卡片、三字段文本均出现。When mock 返回 `[]` 重新挂载。Then 显示「暂无可访问模块」且卡片数为 0。

**用例**：`AC2 ← 用例 MyModulesView.spec#rendersAccessibleModuleCards + MyModulesView.spec#rendersEmptyStateWhenNoModules`（首例含 `expect(typeof moduleApi.getAccessibles).toBe('function')` 前置断言，保证 api 缺失时 RED）

### AC3 — 点卡导航至工作区路由
> Given mock 返回 1 项 `{code: 'news', …}`。When 挂载页面并点击该卡片。Then `router.push` 被调用且参数为 `/workspace/module/news`。

**用例**：`AC3 ← 用例 MyModulesView.spec#navigatesToModuleWorkspaceOnCardClick`

### AC4 — 门禁与冒烟通过
> ① `mvn -q verify` 失败集与基线逐条一致零新增（2026-09-29 口径：148 用例 4F+2E，sprint-072 实测；本单零后端文件改动）。② `npm run lint` EXIT=124 基线、`npm run test` 失败数保持基线 20（passed 由 279 增至 283 = +4 新用例）、`npm run build` 通过。③ `bash scripts/smoke.sh` 新增 `web-038d` 单跑通过、整体失败/跳过与基线逐条一致（基线：54 用例 43✅+9❌+2⏭️）。

**用例**：`AC4 ← mvn -q verify 基线对照 + npm 三件套 + bash scripts/smoke.sh#web-038d`

## 测试清单

> Generator 按 `.harness/rules/tdd-workflow.md` 先于实现写出并运行留 RED 证据；每条验收标准至少一例。新 spec 全部用例集中于 `MyModulesView.spec.ts`（AC1 菜单/路由用例同文件独立 describe，**不改既有 `useMenu.spec.ts`** 守文件预算）。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | MyModulesView.spec#myModulesMenuAndRouteVisibleWithoutPermission | AC1 | RED 已留痕（2026-09-29：组件 import 缺失致整套失败）；待 GREEN 复测 |
| 2 | MyModulesView.spec#rendersAccessibleModuleCards | AC2 | RED 已留痕（同上）；待 GREEN 复测 |
| 3 | MyModulesView.spec#rendersEmptyStateWhenNoModules | AC2 | RED 已留痕（同上）；待 GREEN 复测 |
| 4 | MyModulesView.spec#navigatesToModuleWorkspaceOnCardClick | AC3 | RED 已留痕（同上）；待 GREEN 复测 |
| 5 | 冒烟 web-038d 定向测试（追加于 scripts/smoke.sh，定向跑 MyModulesView.spec 全 4 例，`grep -q 'N passed'` 口径沿先例 web-013/014） | AC4-③ | 待填 |
| 6 | 门禁基线对照（后端 148 4F+2E / 前端 20 failed|279 passed→283） | AC4-①② | 待填 |

**行数红线**：新 spec ≤500 行（预估 ~200）；`MyModulesView.vue` ≤500 行（预估 ~130）；**既有文件零增行压力点**——`useMenu.spec.ts`（206 行）与 `types/module.ts` 一律不动。

## RED 证据

> Generator 于实现前执行测试清单 #1~#4 并粘贴关键失败输出（实时留痕，不得事后补记）。

```text
$ npm run test -- --run src/views/MyModulesView.spec.ts   （2026-09-29 实时留痕，实现前执行）

 RUN  v1.6.1 /opt/codes/auth-core/frontend
 ❯ src/views/MyModulesView.spec.ts  (0 test)
 FAIL  src/views/MyModulesView.spec.ts [ src/views/MyModulesView.spec.ts ]
 Error: Failed to resolve import "@/views/MyModulesView.vue" from "src/views/MyModulesView.spec.ts". Does the file exist?
 Test Files  1 failed (1)
      Tests  no tests

说明：组件尚未存在 → 整套 spec 解析失败（4 用例全红），同时覆盖 #1~#4 预期失败口径
（AC1 路由/菜单条目、AC2 `getAccessibles` 函数断言、AC3 组件+导航均因实现缺失而未达）。
```

## GREEN 证据

> 实现后复跑（实时留痕）。REFACTOR 复查：行宽 ≤120、分层（views/api/composables/router 各归其位、API 调用集中 src/api/）、新 spec 4/4、既有 spec 零改动、文件行数达标。

```text
（待 Generator 填写）
```

## 门禁与冒烟记录

> Generator 亲测填写（后端 mvn -q verify / 前端三件套 / scripts/smoke.sh），Evaluator 不采信自述须亲跑。

（待 Generator 填写）

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 **6 个（顶格）**：

1. `frontend/src/views/MyModulesView.vue` — **新增**：卡片网格（name/code/description）+ 空态 + 点卡导航（AC2-AC3）
2. `frontend/src/views/MyModulesView.spec.ts` — **新增**：4 用例（AC1 菜单/路由、AC2 ×2、AC3）+ `vi.mock('@/stores/auth')`、`vi.mock('@/api/module')`、router mock 基建沿 `useMenu.spec.ts`/`LoginView.spec.ts` 先例（测试先行）
3. `frontend/src/router/routes.ts` — `asyncRoutes` 追加 `/mymodules` 条目（无 permissions/roles 键；`generateRoutes` 仅按 roles 过滤故全员进路由表）（AC1）
4. `frontend/src/composables/useMenu.ts` — `menuConfig` 追加同路径条目（无 permissions 键、`order` 置仪表盘与系统管理之间、**不改既有条目**）（AC1）
5. `frontend/src/api/module.ts` — `getAccessibles()` 方法 + `ModuleAccessible` 接口就近内联（先例：同文件 `getAllPermissions` 内联类型；省 types/module.ts）（AC2）
6. `scripts/smoke.sh` — 追加 `web-038d` 用例（+2 行，不删除、不改动既有用例，先例 web-038c）

**超限熔断**：实现中若实际变更文件超过 6 个，Generator 必须停止实现并回报 Planner/用户重新裁定，不得自行扩范围。**禁改清单**：任何后端文件、`useMenu.spec.ts`、`types/module.ts`、`Sidebar.vue`（读 menuConfig 即渲染，零改动）、`guards.ts`（requiresAuth/无 permissions 恒放行）、`main.ts`（Element 图标已全量注册，复用既有图标名即可）。

## 交付物（预估 6 文件）

1. `frontend/src/views/MyModulesView.vue` — 我的模块页（新增）
2. `frontend/src/views/MyModulesView.spec.ts` — 四用例（新增，测试先行）
3. `frontend/src/router/routes.ts` — /mymodules 路由
4. `frontend/src/composables/useMenu.ts` — 菜单条目
5. `frontend/src/api/module.ts` — getAccessibles + 内联类型
6. `scripts/smoke.sh` — web-038d 冒烟用例

## 变更清单

### 新增
- `frontend/src/views/MyModulesView.vue` — 我的模块页（预估 ~130 行）
- `frontend/src/views/MyModulesView.spec.ts` — 新 spec 4 用例（预估 ~200 行）

### 修改
- `frontend/src/router/routes.ts` — `asyncRoutes` 追加 `/mymodules` 条目（预估 +8 行）
- `frontend/src/composables/useMenu.ts` — `menuConfig` 追加条目（预估 +8 行）
- `frontend/src/api/module.ts` — `getAccessibles` + `ModuleAccessible` 接口（预估 +12 行）
- `scripts/smoke.sh` — `web-038d` 冒烟用例（+2 行，未动既有用例）

合计 6 文件 = 预算顶格，未超熔断。

### 删除
- （无）

## 规范检查清单

- [ ] `mvn -q verify` 后端门禁零新增失败（148 用例 4F+2E 与基线逐条一致，零后端改动）
- [ ] 前端 `npm run lint && npm run test && npm run build` 通过（lint 124 基线挂起豁免口径沿用；test 失败保持 20、passed 279→283；build ✓）
- [ ] `bash scripts/smoke.sh` web-038d ✅；55 用例 9 失败+2 跳过与基线逐条一致
- [ ] 符合前端分层约定（API 调用集中 `src/api/`、视图在 views/、菜单/路由各归其位；`<script setup lang="ts">`、TS 严格）
- [ ] 符合 TDD 工作流（RED 4 例实时留痕 → GREEN → REFACTOR 复查）
- [ ] 变更文件 6 个 = 预算顶格，未超熔断；禁改清单零触碰
- [ ] 新 spec 与 `MyModulesView.vue` 均 ≤500 行；既有 `useMenu.spec.ts`/`types/module.ts` 零改动

## 评审记录

- （待 Evaluator）
