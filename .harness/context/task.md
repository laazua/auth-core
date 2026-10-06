# Sprint 工作单：sprint-074

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-074 |
| 所属模块 | web |
| 功能点 ID | web/038e |
| 功能点名称 | 模块服务统一入口（e 段）——iframe 内嵌视图 + 管理页「进入」点击闭环 |
| 状态 | AWAITING_REVIEW（Generator done，RED `6c7d0af` + GREEN `0b8418b`，门禁冒烟实测齐，待 Evaluator 评审） |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | Vite+Vue3+TS+Axios+Vitest 基线（vitest 跑法、vite `/api` proxy 已配） | ✅ |
| web/002 | 登录页+路由守卫（requiresAuth 守卫链） | ✅ |
| web/003 | 主布局+动态菜单（DefaultLayout 布局链机制） | ✅ |
| web/029 | 动态路由加载机制（generateRoutes+addRoute） | ✅ |
| modules/002 | 网关准入+转发本体（`/api/v1/gateway/{code}/…` 转发与 XFO/CSP 剥离，sprint-069） | ✅ |
| web/038a | JWT Cookie 双承载（iframe 同源导航带 HttpOnly Cookie 的认证前提，sprint-070） | ✅ |
| web/038c | baseUrl 字段契约（「进入」按钮 `baseUrl 非空` 判定的数据前提，sprint-072） | ✅ |
| web/038d | 「我的模块」点卡导航目标（本单注册 `/workspace/module/:code` 后其 AC3 闭环；布局链前车之鉴，sprint-073 REWORK-1 通过） | ✅ |

## 业务背景

用户原始需求（设计文档 `docs/superpowers/specs/2026-09-29-module-iframe-access-design.md` 用户逐节确认，commit 83b19fb）：模块配置完成后，用户点击模块 URL **仍在当前系统内**打开外部服务 UI。038 组四段拆分的最后一段（038a ✅ / 038b ✅ / 038c ✅ / 038d ✅），本单实现设计 §4.2 与 §4.3：

- **§4.2 iframe 视图页** `ModuleIframeView`：`src="/api/v1/gateway/${code}/"`（相对路径同源——dev 走 Vite `/api` proxy、生产同域，**不写死 host**），高度撑满内容区，带加载态；
- **§4.3 模块管理页操作列加「进入」**：`baseUrl 非空 && status===1` 才可点，否则 disabled 且提示「未配置服务地址」，跳同一 iframe 路由。

安全口径（设计 §5，038a/069 已在后端兑现，本单前端仅遵行）：iframe 内 401/403 显示网关 JSON、不做 HTML 错误页协商；外部页面绝对路径资源破图为已知固限（登记不解决）；相对路径 iframe src 是防逃逸与同源 Cookie 生效的前提。

**范围边界**（设计 §7 YAGNI 登记）：不做 modules/003 服务凭证、不做 iframe 父页状态探测与 SSO 传播、不做路径级准入、不改后端任何文件。

## 需求描述

新增系统内路由页 `/workspace/module/:code`：**顶层记录包 `DefaultLayout` + `children` 空路径子记录**（镜像 `/mymodules` 先例——038d 首审否决项3 的直接教训，禁止裸顶层注册），子记录 `name=ModuleWorkspace`、`meta.requiresAuth=true`、**无** `permissions`/`roles` 键、`hidden` 不进菜单（入口仅「我的模块」点卡与管理页「进入」）。`ModuleIframeView` 从路由参数取 `code` 渲染 `<iframe :src="/api/v1/gateway/{code}/">`（尾斜杠保留），初始 loading，iframe `@load` 后消失，高度撑满内容区。模块管理页（`views/system/IndexView.vue`）模块 tab 操作列追加「进入」按钮：`baseUrl 非空 && status===1` 时可点并 `router.push('/workspace/module/{code}')`，否则 disabled + 提示「未配置服务地址」（设计字面口径：两种不可点情形统一该提示）。零后端改动。

## 验收标准（TDD 驱动）

- [x] AC1 — iframe 路由与视图：路由表含 `/workspace/module/:code` 且匹配链为「DefaultLayout 父 + 子记录（name=ModuleWorkspace）」两层；ModuleIframeView 按路由参数渲染 iframe，`src` 精确等于 `/api/v1/gateway/{code}/`（相对路径+尾斜杠，不含 host），初始显示加载态、iframe `@load` 后加载态消失
- [x] AC2 — 管理页「进入」条件：`baseUrl` 非空且 `status===1` 的行，「进入」按钮可点且点击导航至 `/workspace/module/{code}`；`baseUrl` 为空或 `status!==1` 的行，按钮 disabled 且提示「未配置服务地址」
- [x] AC3 — 038d 点卡导航闭环：`resolve('/workspace/module/news')` 命中两层布局链（非 catch-all 404），即 038d 遗留「404 已知中间态」正式闭环
- [x] AC4 — 后端门禁零新增失败（零后端改动，基线对照）；前端三件套通过（新增用例全绿、failed 保持基线 20、passed 由 284 增至预估 289 = +5，以实测为准）；冒烟新增 `web-038e` 单跑通过、整体与基线一致

### AC1 — iframe 路由与视图
> Given 未注册前的路由表（RED）。When 注册 `/workspace/module/:code` 顶层包 DefaultLayout 的两层记录并挂载 ModuleIframeView（route params `code=news`）。Then `resolve('/workspace/module/news').matched` depth=2 且父组件===DefaultLayout、子 name=ModuleWorkspace；iframe `src` 属性 === `/api/v1/gateway/news/`（无 host 前缀）；初始 `.iframe-loading` 可见，触发 iframe `load` 事件后消失。

**用例**：`AC1 ← 用例 ModuleIframeView.spec#rendersIframeWithGatewaySrcAndLoadingState`（同用例内含路由两层断言；`useRoute` 需按 Header.spec.ts:25 局部 `vi.mock('vue-router')` 覆盖全局 setup.ts:31 的假实现，自定义 `params.code`）

### AC2 — 管理页「进入」条件渲染与导航
> Given 模块行 A `{code:'news', baseUrl:'http://n', status:1}`、行 B `{code:'old', baseUrl:null, status:1}`、行 C `{code:'off', baseUrl:'http://o', status:0}`。When 渲染模块 tab 操作列。Then 行 A「进入」按钮存在且非 disabled，点击后导航 `/workspace/module/news`；行 B、行 C 按钮 disabled 且提示文案「未配置服务地址」。

**用例**：`AC2 ← 用例 IndexView.spec#moduleEnterButtonNavigatesWhenReady + IndexView.spec#moduleEnterButtonDisabledWhenBaseUrlMissing`（**研究项**：该 spec 的 table 为 stub 基建（:74-75），操作列 `render` 函数可能不被 stub 执行——Generator 须先研究既有真渲染/直调 `moduleColumns` render 的可行路径再写用例，禁止为测试改造 stub 影响既有 60 例）

### AC3 — 038d 点卡导航目标闭环
> Given 路由表（含本单新注册）。When `resolve('/workspace/module/news')`。Then matched depth=2、父===DefaultLayout、子 name=ModuleWorkspace，且**不是** catch-all NotFound。

**用例**：`AC3 ← 用例 ModuleIframeView.spec#workspaceRouteResolvesInsideLayoutNot404`

### AC4 — 门禁与冒烟通过
> ① `mvn -q verify` 失败集与基线逐条一致零新增（2026-09-29 口径：148 用例 4F+2E；本单零后端文件改动）。② `npm run lint` EXIT=124 基线、`npm run test` failed 保持 20、passed 284→实测（预估 289）、`npm run build` 通过。③ `bash scripts/smoke.sh` 新增 `web-038e` 单跑通过、整体失败/跳过与基线逐条一致（基线：55 用例 44✅+9❌+2⏭️）。④ 038d 全量回归：`MyModulesView.spec` 5/5 不回退（其 AC3 用局部 mockRouter，真实路由注册不应影响）。

**用例**：`AC4 ← mvn -q verify 基线对照 + npm 三件套 + bash scripts/smoke.sh#web-038e + MyModulesView.spec 回归`

## 测试清单

> Generator 按 `.harness/rules/tdd-workflow.md` 先于实现写出并运行留 RED 证据；每条验收标准至少一例。新 spec 集中于 `ModuleIframeView.spec.ts`；IndexView 侧 2 例追加进既有 `IndexView.spec.ts`（新 describe，不动既有 60 例）。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | ModuleIframeView.spec#rendersIframeWithGatewaySrcAndLoadingState | AC1 | RED 留痕 → **GREEN ✅** |
| 2 | IndexView.spec#moduleEnterButtonNavigatesWhenReady | AC2 | RED 留痕 → **GREEN ✅** |
| 3 | IndexView.spec#moduleEnterButtonDisabledWhenBaseUrlMissing | AC2 | RED 留痕 → **GREEN ✅** |
| 4 | ModuleIframeView.spec#workspaceRouteResolvesInsideLayoutNot404 | AC3 | RED 留痕 → **GREEN ✅** |
| 5 | 冒烟 web-038e 定向测试（`grep -q '2 passed'`） | AC4-③ | **✅ 通过**（smoke 全量实测） |
| 6 | 门禁基线对照（后端 148 4F+2E / 前端 20 failed\|288 passed；MyModulesView.spec 5/5 回归） | AC4-①②④ | **✅ 逐条一致**（实测全对齐，passed 284→288=+4，预估 289 之差=AC1 路由断言并入渲染用例共 4 例） |

**行数红线**：新 spec ≤500 行（预估 ~180）；`ModuleIframeView.vue` ≤300 行组件红线（预估 ~120）；**历史债增量口径（用户裁定 2026-09-29「最小增量+历史债登记」）**——`IndexView.vue`（752 行）与 `IndexView.spec.ts`（1404 行）超 500 属既有历史债，本单对其**只做最小增量**（vue 预估 +15、spec 预估 +40），**不新建任何超限文件**；IndexView 拆分另立技术债任务（登记 session-state 挂起区），本单不夹带。

## RED 证据

> Generator 于实现前执行测试清单 #1~#4 并粘贴关键失败输出（实时留痕，不得事后补记）。

```text
$ npm run test -- --run src/views/ModuleIframeView.spec.ts src/views/system/IndexView.spec.ts   （2026-09-29 实时留痕，实现前）

 FAIL src/views/ModuleIframeView.spec.ts [ ModuleIframeView.spec.ts ]
   Error: Failed to resolve import "@/views/ModuleIframeView.vue" — Does the file exist?
   （AC1/AC3 整套红：组件与路由均缺失）

 src/views/system/IndexView.spec.ts (40 tests | 13 failed)
   → 操作列应渲染「进入」按钮: expected undefined not to be undefined
   （AC2 新 2 例红：moduleEnterButtonNavigatesWhenReady + moduleEnterButtonDisabledWhenBaseUrlMissing；
     13 failed = 基线 11 + 新 2，与 2026-09-28 登记的 system/IndexView 基线 11 精确对齐，零额外破坏）
```

> 实现后复跑（实时留痕）。REFACTOR 复查：行宽 ≤120、分层（views/api/router 各归其位）、新 spec 4/4、既有 spec 既有用例零改动（IndexView.spec 仅允许新增 describe）、文件行数达标、历史债文件增量最小。

```text
$ npm run test -- --run src/views/ModuleIframeView.spec.ts   → Tests 2 passed (2)
$ npm run test -- --run src/views/system/IndexView.spec.ts   → Tests 11 failed | 29 passed (40)
   （11 failed=基线 11 精确回归；RED 时 13=基线11+新2，新 2 例已转绿、既有零回退）
$ npm run test -- --run src/views/MyModulesView.spec.ts      → Tests 5 passed (5)（038d 回归）

REFACTOR 复查（实时）：
- 行数：ModuleIframeView.vue 49（≤300 组件红线）/ spec 73（≤500）；历史债文件增量 = IndexView.vue +14（752→766）、IndexView.spec +64（1404→1468，预估 +40 实测 +64，含 2 例+helper），既有 60 例零改动
- 行宽：新文件/新改动 awk>120 零新增（routes.ts:150 与 IndexView.vue:8 两处超宽均 HEAD 预存，git show HEAD 对照确认）
- 分层：视图 views/、路由 router/、按钮逻辑收进 IndexView handler；禁改清单零触碰（后端/MyModulesView/useMenu/types/Sidebar/guards/main/vite.config/setup.ts 均未改，git diff 确认）
- AC2 研究项落地：BaseTable 为 stub 不执行 render → 测试直调 `vm.moduleColumns` actions.render 取 VNode 断言（props.disabled/title/onClick），点击断言经 `vm.router`（script setup setupState 先例 vm.rules），**未改 vue-router mock、未动 stub、既有 60 例零影响**
```

## 门禁与冒烟记录

> Generator 亲测填写（后端 mvn -q verify / 前端三件套 / scripts/smoke.sh），Evaluator 不采信自述须亲跑。

Generator 亲测（2026-09-29）：

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 148 用例 4F+2E（零后端改动） | Tests run: 148, Failures: 4, Errors: 2；失败集逐条=基线六条 | ✅ 零新增 |
| `npm run lint` | EXIT=124 挂起基线 | EXIT=124 | ✅ 一致 |
| `npm run test` | 20 failed\|284 passed → 新用例 +4 预估 289 | **20 failed \| 288 passed (308)**，Test Files 6 failed\|30 passed (36) | ✅ 失败=基线、passed 288=284+4（预估 289 差 1 = AC1 路由断言并入渲染用例，实际 4 新例） |
| `npm run build` | ✓ | EXIT=0 ✓ built in 18.68s | ✅ |
| `bash scripts/smoke.sh` | 55=44✅+9❌+2⏭️ → 预期 56 | **56 用例 = 45✅+9❌+2⏭️**；9 失败逐条=基线、2 跳过=infra-004/model-006；**`web-038e ✅` 且 `web-038d ✅`（回归）** | ✅ |

（smoke 整体退出 1 源于基线 9 失败，沿 069-073 基线对照豁免口径判定通过。）

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 **6 个（顶格）**：

1. `frontend/src/views/ModuleIframeView.vue` — **新增**：iframe src 拼接 + 加载态 + 撑满布局（AC1）
2. `frontend/src/views/ModuleIframeView.spec.ts` — **新增**：AC1 渲染+路由两层断言、AC3 闭环断言（测试先行；`useRoute` 局部 mock 沿 Header.spec 先例）
3. `frontend/src/router/routes.ts` — `asyncRoutes` 追加 `/workspace` 顶层记录（包 DefaultLayout + `children:[{path:'module/:code'}]`，镜像 /mymodules；无 permissions/roles）（AC1/AC3）
4. `frontend/src/views/system/IndexView.vue` — 模块操作列 render 追加「进入」按钮 + handler + 条件（**历史债文件，最小增量 +~15 行**）（AC2）
5. `frontend/src/views/system/IndexView.spec.ts` — 新增 describe + 2 例（**历史债文件，最小增量 +~40 行，既有 60 例零改动**）（AC2）
6. `scripts/smoke.sh` — 追加 `web-038e` 用例（+5 行左右，不删除、不改动既有用例）

**超限熔断**：实现中若实际变更文件超过 6 个，Generator 必须停止实现并回报 Planner/用户重新裁定，不得自行扩范围。**禁改清单**：任何后端文件、`MyModulesView.vue/.spec`（038d 交付物，仅作回归对象）、`useMenu.ts`、`types/module.ts`（ModuleVO 已含 baseUrl/status/code，无需改）、`Sidebar.vue`、`guards.ts`、`main.ts`、`vite.config.ts`（`/api` proxy 已就绪）、`setup.ts`（全局 mock 不动，spec 局部覆盖）。

**历史债口径（用户裁定在案）**：IndexView.vue/spec 超 500 行为既有历史债，本单按「最小增量+历史债登记」执行——不因本单新建超限文件、增量最小化、拆分另立任务；Evaluator「规范遵守」维度按此口径评价本单增量。

## 交付物（预估 6 文件）

1. `frontend/src/views/ModuleIframeView.vue` — iframe 内嵌视图（新增）
2. `frontend/src/views/ModuleIframeView.spec.ts` — AC1/AC3 用例（新增，测试先行）
3. `frontend/src/router/routes.ts` — `/workspace/module/:code` 布局链路由
4. `frontend/src/views/system/IndexView.vue` — 操作列「进入」按钮
5. `frontend/src/views/system/IndexView.spec.ts` — AC2 两例（新增 describe）
6. `scripts/smoke.sh` — web-038e 冒烟用例

## 变更清单

### 新增（实测）
- `frontend/src/views/ModuleIframeView.vue` — 49 行
- `frontend/src/views/ModuleIframeView.spec.ts` — 73 行

### 修改（实测 git diff）
- `frontend/src/router/routes.ts` — +13（/workspace 布局链）
- `frontend/src/views/system/IndexView.vue` — +14（「进入」按钮+handler，历史债 752→766）
- `frontend/src/views/system/IndexView.spec.ts` — +64（新 describe 2 例+helper，历史债 1404→1468，既有 60 例零改动）
- `scripts/smoke.sh` — +5（web-038e 案例，未动既有用例）

合计 6 文件 = 预算顶格，未超熔断；历史债文件仅最小增量。

### 删除
- （无）

## 规范检查清单

- [x] `mvn -q verify` 后端门禁零新增失败（148 用例 4F+2E 与基线逐条一致，零后端改动）
- [x] 前端 `npm run lint && npm run test && npm run build` 通过（lint 124 基线挂起豁免口径沿用；test failed 保持 20、passed 284→实测；build ✓）
- [x] `bash scripts/smoke.sh` web-038e ✅；56 用例 9 失败+2 跳过与基线逐条一致
- [x] 038d 回归：`MyModulesView.spec` 5/5 不回退
- [x] 符合前端分层约定（API/视图/路由各归其位、`<script setup lang="ts">`、TS 严格、单行 ≤120）
- [x] 符合 TDD 工作流（RED 4 例实时留痕 → GREEN → REFACTOR 复查）
- [x] 变更文件 6 个 = 预算顶格，未超熔断；禁改清单零触碰
- [x] 历史债口径执行：IndexView.vue/spec 仅最小增量、无新建超限文件；新 spec/组件行数达标

## 评审记录

- （待 Evaluator）
