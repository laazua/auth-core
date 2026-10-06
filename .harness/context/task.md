# Sprint 工作单：sprint-073

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-073 |
| 所属模块 | web |
| 功能点 ID | web/038d |
| 功能点名称 | 模块服务统一入口（d 段）——「我的模块」菜单页（前端） |
| 状态 | REWORK（第 1 次） |
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

- [x] AC1 — 路由与菜单全员可见：路由表含 `/mymodules`（`requiresAuth=true`、无 `permissions`/`roles`），**零权限**用户（`hasAnyPermission`/`hasAnyRole` 恒 false）`getSortedMenuTree()` 仍含路径 `/mymodules`、标题「我的模块」的菜单条目
- [x] AC2 — 列表渲染与空态：`moduleApi.getAccessibles` 存在（函数类型断言）；mock 返回 2 项 → 渲染 2 张卡片且各含 name/code/description 文本；mock 返回 `[]` → 显示「暂无可访问模块」且无卡片
- [x] AC3 — 点卡导航：点击 `code=news` 的卡片 → `router.push('/workspace/module/news')`（目标路由由 038e 注册，本单断言导航目标）
- [x] AC4 — 后端门禁零新增失败（零后端改动，基线对照）；前端三件套通过（新增 4 用例全绿、失败数保持基线）；冒烟 `web-038d` 单跑通过、整体与基线一致

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
| 1 | MyModulesView.spec#myModulesMenuAndRouteVisibleWithoutPermission | AC1 | RED 留痕 → **GREEN ✅** |
| 2 | MyModulesView.spec#rendersAccessibleModuleCards | AC2 | RED 留痕 → **GREEN ✅** |
| 3 | MyModulesView.spec#rendersEmptyStateWhenNoModules | AC2 | RED 留痕 → **GREEN ✅** |
| 4 | MyModulesView.spec#navigatesToModuleWorkspaceOnCardClick | AC3 | RED 留痕 → **GREEN ✅**（AC3 断言基建按 Header.spec 先例修正，见 GREEN 证据） |
| 5 | 冒烟 web-038d 定向测试（追加于 scripts/smoke.sh，定向跑 MyModulesView.spec 全 4 例，`grep -q '4 passed'`） | AC4-③ | **✅ 通过**（smoke 全量实测） |
| 6 | 门禁基线对照（后端 148 4F+2E / 前端 20 failed|279 passed→283） | AC4-①② | **✅ 逐条一致**（实测后端 148 4F+2E、前端 20 failed\|283 passed） |

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
$ npm run test -- --run src/views/MyModulesView.spec.ts   （2026-09-29 实时留痕）
 ✓ src/views/MyModulesView.spec.ts  (4 tests) 66ms
 Test Files  1 passed (1)
      Tests  4 passed (4)
EXIT=0

REFACTOR 复查（实时）：
- 行数：spec 144 行 / MyModulesView.vue 128 行（均 ≤500）；useMenu.spec.ts=206、types/module.ts=51 零改动（git diff 确认）
- 行宽：新改 3 文件 awk length>120 零命中
- 分层：视图 views/、API 集中 api/module.ts（getAccessibles+ModuleAccessible 内联）、路由/菜单各归其位
- 既有文件仅触 3 处最小增行：api/module.ts +13、routes.ts +6、useMenu.ts +6；零改禁改清单

AC3 断言基建修正记录（仅测试文件内部，AC 语义不变）：
首版 spec 用真 router + vi.spyOn(router,'push') 断言，3/4 过但 AC3 push 零调用。查因：
`src/__tests__/setup.ts:31` 全局 `vi.mock('vue-router')` 将 `useRouter()` mock 为每次调用
返回新假对象（push: vi.fn()），测试持有的 router 实例与组件注入实例不同。
修正：按既有先例 Header.spec.ts:25 在本 spec 局部 `vi.mock('vue-router')` 返回共享
mockRouter，AC3 断言 `mockRouter.push` 收到 `/workspace/module/news`（机制与 Header.spec
AC4 同源）。组件实现零改，断言目标与 AC3 定义一致。
```

## 门禁与冒烟记录

> Generator 亲测填写（后端 mvn -q verify / 前端三件套 / scripts/smoke.sh），Evaluator 不采信自述须亲跑。

Generator 亲测（2026-09-29）：

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 148 用例 4F+2E（零后端改动） | Tests run: 148, Failures: 4, Errors: 2；失败集逐条一致（DataSourceConfigBindingTest×2、RoleControllerTest#assignPermissionsInvalidPermissionReturns400、SeedDataIntegrationTest、TestLayersSpec、TestUtilsSpec） | ✅ 零新增 |
| `npm run lint` | EXIT=124 挂起基线 | EXIT=124 | ✅ 一致 |
| `npm run test` | 20 failed\|279 passed → 预期 283 | **20 failed \| 283 passed (303)**，Test Files 6 failed\|29 passed (35) | ✅ 失败=基线、passed +4 精确命中 |
| `npm run build` | ✓ | EXIT=0 ✓ built in 18.80s | ✅ |
| `bash scripts/smoke.sh` | 54=43✅+9❌+2⏭️ → 预期 55 | **55 用例 = 44✅+9❌+2⏭️**；9 失败逐条=基线（model-008/010、roles-001/002、web-013/020/021/022/026）；2 跳过=infra-004/model-006；**`web-038d 我的模块菜单页定向测试 ✅`** | ✅ 新增用例通过、失败/跳过逐条=基线 |

（smoke 脚本整体退出 1 源于基线 9 失败存在，与 sprint-070/071/072 同口径：按「新增用例通过+失败集与基线逐条一致」判定通过。）

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

### 新增（实测）
- `frontend/src/views/MyModulesView.vue` — 我的模块页（128 行）
- `frontend/src/views/MyModulesView.spec.ts` — 新 spec 4 用例（144 行）

### 修改（实测 git diff）
- `frontend/src/router/routes.ts` — `asyncRoutes` 追加 `/mymodules` 条目（+6）
- `frontend/src/composables/useMenu.ts` — `menuConfig` 追加条目（+6）
- `frontend/src/api/module.ts` — `getAccessibles` + `ModuleAccessible` 接口（+13）
- `scripts/smoke.sh` — `web-038d` 冒烟用例（+4：注释+smoke_case 三行，未动既有用例）

合计 6 文件 = 预算顶格，未超熔断。

### 删除
- （无）

## 规范检查清单

- [x] `mvn -q verify` 后端门禁零新增失败（148 用例 4F+2E 与基线逐条一致，零后端改动）
- [x] 前端 `npm run lint && npm run test && npm run build` 通过（lint 124 基线挂起豁免口径沿用；test 失败保持 20、passed 279→283；build ✓）
- [x] `bash scripts/smoke.sh` web-038d ✅；55 用例 9 失败+2 跳过与基线逐条一致
- [x] 符合前端分层约定（API 调用集中 `src/api/`、视图在 views/、菜单/路由各归其位；`<script setup lang="ts">`、TS 严格）
- [x] 符合 TDD 工作流（RED 4 例实时留痕 → GREEN → REFACTOR 复查）
- [x] 变更文件 6 个 = 预算顶格，未超熔断；禁改清单零触碰
- [x] 新 spec 与 `MyModulesView.vue` 均 ≤500 行；既有 `useMenu.spec.ts`/`types/module.ts` 零改动

## 评审记录

### 验收标准核对
- [x] AC1 — 路由与菜单全员可见：路由表含 /mymodules、requiresAuth、无 permissions/roles 键；零权限菜单含「我的模块」（用例 1/1 过）——**但路由为裸顶层注册，缺布局父链，见问题 1**
- [x] AC2 — 列表渲染与空态：getAccessibles 函数断言 +2 卡 + 空态，2/2 用例过
- [x] AC3 — 点卡导航：mockRouter.push 收到 `/workspace/module/news`，1/1 过
- [x] AC4 — 门禁与冒烟：亲测四项+冒烟全部对齐基线（见下）
- [ ] **功能点核心语义「菜单页」**：点菜单进入后脱离 DefaultLayout，侧栏/顶栏整体消失——页面渲染与 AC 字面通过，但作为菜单页**功能不可用**（唯一回退=浏览器后退）→ 命中否决项 3，见问题 1

### 门禁与冒烟实测
Evaluator 2026-09-29 亲自运行（不采信 Generator 自述）：
- `mvn -q verify`：Tests run: 148, Failures: 4, Errors: 2 —— 失败集逐条=基线（DataSourceConfigBindingTest×2、RoleControllerTest:466、SeedDataIntegrationTest:99、TestLayersSpec、TestUtilsSpec），零后端改动，零新增 ✅
- `npm run test`：**20 failed | 283 passed (303)** —— failed=基线 20、passed +4 命中 ✅；`npm run build`：✓ built in 17.43s ✅；`npm run lint`：EXIT=124（基线挂起）✅
- `bash scripts/smoke.sh`：**55 用例 = 44✅+9❌+2⏭️**；9 失败逐条=基线（model-008/010、roles-001/002、web-013/020/021/022/026）、2 跳过=infra-004/model-006；`web-038d 我的模块菜单页定向测试 ✅`；追加用例符合冒烟节要求（否决项 6 经基线对照豁免口径不中）✅
- 布局链实证（临时 spec，已删）：`/dashboard => depth 2 | parents: / > /dashboard`；`/system/users => depth 2 | parents: /system > /system/users`；**`/mymodules => depth 1 | parents: /mymodules`**

### 评分
| 维度 | 分数 | 证据 |
|------|------|------|
| 功能正确性 | 6 | AC1-4 字面全过（4/4 用例），但功能点核心语义破损：routes.ts:74-76 裸注册致 `resolve('/mymodules')` depth=1（对照 /system depth=2），点菜单即失主导航；异常路径缺失：MyModulesView.vue:10-17 onMounted 无 catch，getAccessibles 失败→unhandled rejection 且误导性空态 |
| 代码质量 | 6 | 违背既有布局模式（routes.ts:72-79 /system 包 DefaultLayout，/mymodules 未包）；onMounted 无错误处理且异常路径零测试 |
| 规范遵守 | 8 | 行数 spec 144/组件 128 ≤500、行宽零超、分层正确、禁改清单零触碰（git diff 确认 useMenu.spec/types/module/Sidebar/guards/main/后端零改）；扣分：task.md 变更清单记 smoke.sh "+4 行"，git diff 实为 +5 行（记录不实） |
| TDD 执行度 | 7 | RED `0e98cfa`→GREEN `b4f8d6b` 两段式证据链完整、AC3 断言基建修正有记录、断言非凑数；扣分：AC1 仅断言 route.path/meta 字段、无 matched 父链断言，布局缺陷从测试网逃逸 |
| 安全性 | 9 | 五查无命中：无越权（requiresAuth ✓，数据源 accessibles 本身按交集过滤）、无注入面（无 SQL 拼接）、无明文密钥、日志无敏感信息；扣分项：无 |

**均分 = 35/5 = 7.0**，但命中否决项 → 不通过。

### 决策
❌ **不通过（REWORK 第 1 次）**——命中一票否决项 **3（明确 Bug：核心逻辑错误导致功能不可用）**：`/mymodules` 路由以裸顶层注册脱离主布局，「我的模块」菜单页进入后侧栏/顶栏整体消失、系统导航不可用（实证 depth 1 vs 先例 depth 2），功能点核心交付不可用。

### 问题列表
1. **[严重·否决级]** `frontend/src/router/routes.ts:74-76`：/mymodules 裸注册（无 DefaultLayout 父记录）→ 点菜单渲染裸页面、失去全部主导航。修复：镜像 /system 模式改为 `{ path: '/mymodules', component: DefaultLayout, children: [{ path: '', name: 'MyModules', component: MyModulesView, meta: {...} }] }`；**并同步修 AC1 用例**（`MyModulesView.spec.ts:66` 起）追加 matched 父链断言（depth≥2 且父记录 component 解析为 DefaultLayout，或等价断言），防回归。注意：task.md 需求描述「顶级动态路由」措辞与布局一致性冲突，按 AGENTS.md 以架构/先例为准修正实现，并在任务单留一行修订记录。
2. **[中]** `frontend/src/views/MyModulesView.vue:10-17`：onMounted 无 catch——接口失败时 unhandled rejection 且渲染误导性「暂无可访问模块」。修复：捕获失败态（区分「加载失败」与「无数据」），至少日志+状态区分；建议补 1 例失败态用例（用例数变化须同步更新 smoke.sh 的 `grep -q 'N passed'` 口径）。
3. **[轻]** `task.md` 变更清单：smoke.sh 记 "+4 行"，实测 +5（git diff 5 insertions）——更正记录。

### 改进建议（不计分）
- 菜单 `order: 1.5`（useMenu.ts:34）小数序合法但可读性一般，若后续调整建议整数重排（本单禁改既有条目，维持现状可接受）。
- AC1 用例声明 `async` 但体内无 await（MyModulesView.spec.ts:61），可去冗余 async。
