# Sprint 工作单：sprint-067

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-067 |
| 所属模块 | web |
| 功能点 ID | web/036a（父功能点 web/036 首个子功能点） |
| 功能点名称 | 暗色模式全站 UI 协调治理·接入 Element Plus 暗色变量体系（根因：两套变量体系只切一套） |
| 状态 | AWAITING_REVIEW |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | Vite+Vue3+TS+Router 骨架与 Vitest 基线 | ✅ |
| web/003 | 主布局(侧边菜单/顶栏)+动态菜单渲染 | ✅ |
| web/008 | UI现代化优化+白天/晚上模式切换（data-theme 方案来源） | ✅ |
| web/016 | CSS 主题变量源 variables.scss 注入样式入口 | ✅ |

## 业务背景

用户需求（原文）：「登录系统后，暗色模式时，整个系统页面有的区域暗色，有的区域白色，太不协调了，重新优化整系统的 UI，让其更加美观」。

Planner 只读调研（explore 代理全仓检索，不入仓库）已定位根因——**项目维护了两套并行变量体系，切换时只切了一套**：

1. **A 体系 `--color-*`（自研）**：`frontend/src/styles/variables.scss:184-265` 定义 `[data-theme="dark"]` 暗段，随 `src/stores/app.ts:55-57 applyTheme()` 写入 `html[data-theme]` 正常切换 → 侧边栏/顶栏/卡片/登录页/Base 自研组件全部正常变暗；
2. **B 体系 `--el-*`（Element Plus）**：`src/styles/element-plus.scss:1` 只引入亮色主题；**全仓未引入 `element-plus/theme-chalk/dark/css-vars.css`，`applyTheme` 也从未添加 `html.dark` class**（全仓 grep `html.dark`/`classList.add('dark')`/`dark/css-vars` 零命中）→ 所有 EP 原生组件（表格、抽屉×6、分页×4、消息框/消息提示、树、选择器、开关、Tabs 等）**恒为白底/浅底**，即用户所见「有的区域暗色、有的区域白色」；
3. 产物实证：`frontend/dist/assets/css/` 中 `--el-bg-color: #ffffff`、`.dark` 选择器仅 1 处（color-picker 自带），EP 暗色变量文件未进包。

该问题为纯前端主题缺陷，不涉及 `docs/01-architecture.md` RBAC 硬语义与技术栈表（Element Plus 为表内选型，dark css-vars 为同一库内置主题文件，非新技术栈），**无架构冲突**。

**拆分口径**（预估总变更远超 6 文件，按 Planner 行为流程第 3 步拆分为三个子功能点，本次仅注册首个 web/036a）：

- **web/036a（本次 sprint-067）**：体系接入根因治理——`applyTheme`/`initTheme` 双轨同步 `html.dark` class + 引入 EP dark css-vars，让 EP 组件全站统一走 `--el-*` 暗色变量；
- web/036b（后续 Sprint）：局部白底缺陷修复——`BaseTable.vue:196-202` 暗色选择器编译失效（loading 遮罩恒白）、`settings/IndexView.vue:146,159` 非法 prop `variant` 致白底按钮、分页/树/弹层组件级 deep 覆盖补漏、EP 暗值与自研 slate 暗色系的视觉协调映射；
- web/036c（后续 Sprint）：打磨与口径治理——冗余暗色覆盖与死代码清理（`layouts/Layout.vue`、`components/Layout/TagsView.vue`、`styles/index.ts`）、`smoke.sh` web-015/016/017 过期 grep 同步、暗色端到端用例补强。

**关联登记**：与 registry `web/017`（sprint-041 待评审，EP 弹层白底未主题化）高度同源——web/036a 接入 EP 暗色变量后弹层问题将大面积收敛，Evaluator 评审 sprint-067 时可一并对照 web/017 积压工作单裁决，避免重复工作。Evaluator 积压（sprint-047/048/060/061/062/063）不变，见 session-state 挂起区。

## 需求描述

1. **双轨同步切换**：主题切到 dark 时 `document.documentElement` 同时具备 `data-theme="dark"` 属性与 `dark` class；切回 light 时同步恢复 `data-theme="light"` 且移除 `dark` class；自研 `--color-*` 暗段（`[data-theme="dark"]` 选择器）行为不回归。
2. **启动初始化一致**：应用启动/刷新后按 localStorage 恢复的主题初始化时，双轨状态与切换路径一致（恢复 dark 必须带 `dark` class），亮色默认态不带 `dark` class。
3. **EP 暗色变量进包**：构建产物包含 Element Plus 暗色变量（`.dark` 作用域的 `--el-*` 暗值规则），暗色下 EP 原生组件（表格/抽屉/分页/消息框等）背景由暗值驱动、不再是白底；亮色模式 EP 组件外观不回归。
4. **不回归**：全量前端测试零新增失败（基线：2026-09-28 实测 20 failed / 274 passed，属 web/011 预存红项）；后端零改动；冒烟追加 web-036a 用例。

> 范围边界（防蔓延）：本子功能点只做「体系接入 + 双轨同步」，**不做**组件级白底修补、视觉色系微调、死代码清理（归 web/036b/036c）；实现中若预估变更文件超过 6 个，须停止并回报 Planner 重新拆分。

## 验收标准（TDD 驱动）

- [x] AC1 — 暗色/亮色切换时 html 双轨状态同步且自研变量行为不回归
- [x] AC2 — 启动初始化恢复主题时双轨状态与切换路径一致
- [x] AC3 — 构建产物含 EP 暗色变量且亮色模式不回归
- [x] AC4 — 全量测试零新增失败且冒烟 web-036a 通过

### AC1 — 暗色/亮色切换时 html 双轨状态同步且自研变量行为不回归
> 调用 `toggleTheme()` 后断言 `document.documentElement`：`getAttribute('data-theme') === 'dark'` 且 `classList.contains('dark') === true`；再次调用切回后 `getAttribute('data-theme') === 'light'` 且 `classList.contains('dark') === false`。`setTheme('dark')` 直设路径同样满足双轨。既有断言（`data-theme` 属性写入、`theme-transitioning` 类）保持通过。

**用例**：`AC1 ← 用例 stores.spec.ts#web-036a 主题双轨同步 > toggleTheme 与 setTheme 同步维护 data-theme 属性与 dark class`

### AC2 — 启动初始化恢复主题时双轨状态与切换路径一致
> 预置 localStorage `theme='dark'` 后调用 `initTheme()`（store 与 composable 两入口按实际实现落点取一，须覆盖刷新恢复路径）：断言 `document.documentElement` 同时满足 `data-theme='dark'` 且含 `dark` class；预置 `theme='light'`（或空）时断言 `data-theme='light'` 且无 `dark` class。既有 `initTheme`/system preference 用例保持通过。

**用例**：`AC2 ← 用例 useTheme.spec.ts#web-036a 主题双轨同步 > initTheme 恢复 dark 与 light 时双轨状态一致`

### AC3 — 构建产物含 EP 暗色变量且亮色模式不回归
> `npm run build` 后对 `frontend/dist/assets/css/*.css`（全 chunk，非单文件路径）grep 断言：① 存在 `.dark` 作用域的 EP 暗色变量规则（如 `--el-bg-color` 暗值，dark css-vars 内容进包）；② 亮色默认 `--el-bg-color` 白值规则仍在（亮色不回归）；③ 自研 `[data-theme=dark]` 暗段（如 `--color-bg-page`）仍进包（A 体系不回归）。辅助证据（非门禁）：Generator 浏览器实测登录后切暗色，用户管理页 `el-table` 表体与打开的抽屉背景计算色非白底。

**用例**：`AC3 ← 用例 bash scripts/smoke.sh#web-036a Element Plus 暗色变量接入产物验证`

### AC4 — 全量测试零新增失败且冒烟 web-036a 通过
> `npm run test` 失败数不超过实现前基线 20 条（零新增失败，清单与 2026-09-28 基线逐条一致）；`npm run lint` 全量卡死为预存、分片 lint 对本次改动文件零新增告警；`npm run build` 通过；`mvn -q verify` 零后端改动与基线一致（4 Failures + 2 Errors）；`bash scripts/smoke.sh` 新增 `web-036a` 用例且单跑通过、整体失败与基线 10 条逐条一致。

**用例**：`AC4 ← 用例 npm run test 全量基线对照 + bash scripts/smoke.sh#web-036a 双轨与产物验证整体跑`

## 测试清单

> 先于实现写出，规则遵循 `.harness/rules/tdd-workflow.md`；Generator 运行确认 RED 后填入实际输出。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | stores.spec.ts#web-036a 主题双轨同步 > toggleTheme 与 setTheme 同步维护 data-theme 属性与 dark class | AC1 | ✅ RED（1 failed 实时留痕）→ GREEN |
| 2 | useTheme.spec.ts#web-036a 主题双轨同步 > initTheme 恢复 dark 与 light 时双轨状态一致 | AC2 | ✅ RED（1 failed 实时留痕）→ GREEN |
| 3 | 冒烟 web-036a Element Plus 暗色变量接入产物验证（`.dark` 暗值规则 + 亮色白值规则 + 自研暗段三断言） | AC3 | ✅ RED（既有产物无 `.dark` EP 规则实证）→ GREEN（单跑通过，产物三断言全中） |
| 4 | 全量前端测试基线对照（实现前 20 failed / 274 passed，零新增失败） | AC4 | ✅ 实测 20 failed / 276 passed（296 = 基线 294 + 新增 2，failed 分布逐条一致零新增） |

## RED 证据

> Generator 于实现前执行测试清单 #1~#3 并粘贴关键失败输出。

```text
[RED] src/__tests__/stores.spec.ts — Pinia Stores > app store > web-036a 主题双轨同步 > toggleTheme 与 setTheme 同步维护 data-theme 属性与 dark class
  AssertionError: expected false to be true (stores.spec.ts:173, toggleTheme 切 dark 后 classList 不含 'dark')
  Test Files 1 failed (1) | Tests 1 failed | 12 skipped (13)
[RED] src/composables/useTheme.spec.ts — useTheme > web-036a 主题双轨同步 > initTheme 恢复 dark 与 light 时双轨状态一致
  AssertionError: expected false to be true (useTheme.spec.ts:236, initTheme 恢复 dark 后 classList 不含 'dark')
  Test Files 1 failed (1) | Tests 1 failed | 13 skipped (14)
[RED] smoke web-036a 产物断言（对既有 dist 实测）: grep -rE '\.dark\{[^}]*--el-bg-color' dist/assets/css/*.css → 无匹配，RED confirmed: 产物无 .dark EP 暗色变量规则
（实测时间 2026-09-29，实现前执行）
```

## 门禁与冒烟记录

- 后端 `mvn -q verify`：130 用例 4 Failures + 2 Errors，与基线逐条一致（DataSourceConfigBinding ×2、RoleControllerTest 409、SeedData 22≠2、TestLayers/TestUtils Docker 缺失），零后端改动、零新增 ✅（基线对照裁决口径）
- 前端全量测试：`Test Files 6 failed | 27 passed (33)`，`Tests 20 failed | 276 passed (296)`——failed 20 与 2026-09-28 基线逐条一致（LoginView 2、DefaultLayout 1、tags-view-actions/dropdown 4、auth-token 2、system/IndexView 11），新增 2 用例全绿零新增失败 ✅
- 前端 `npm run build`：✓ built in 17.16s ✅
- `npm run lint`：全量卡死为预存（沿基线对照口径）；分片 lint `npx eslint src/stores/app.ts src/__tests__/stores.spec.ts src/composables/useTheme.spec.ts` 初跑 3 处 prettier 格式（均在本次新增代码内），`--fix` 后复跑零告警（exit 0）✅
- `bash scripts/smoke.sh`：49 用例（48+新增 web-036a），失败 9 条全部为基线 10 条子集（infra-001 本轮转绿属环境波动，model-008/010、roles-001/002、web-013、web-020/021/022、web-026 逐条一致）零新增；**web-036a 单跑 ✅ 通过**；计数同步一处（web-015 断言 `27 passed`→`28 passed`，因本次新增 1 条 useTheme 用例过期，用例未删除，首跑失败后同步转绿）
- AC3 辅助浏览器实测（非门禁）：本机 Playwright 不可用未执行，产物三断言 + 双轨单测已构成 AC3 判定证据；实际视觉效果（表格/抽屉/消息框暗色协调）留待 Evaluator 复核

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 **6 个（顶格）**：

1. `frontend/src/stores/app.ts` — `applyTheme()` 同步 `dark` class（双轨核心）
2. `frontend/src/styles/global.css` 或 `frontend/src/main.ts` — 引入 `element-plus/theme-chalk/dark/css-vars.css`（落点二选一由 Generator 按加载顺序定）
3. `frontend/src/composables/useTheme.ts` — 若初始化/composable 路径需同步双轨（视实现落点，可不改）
4. `frontend/src/__tests__/stores.spec.ts` — AC1 用例（先于实现）
5. `frontend/src/composables/useTheme.spec.ts` — AC2 用例（先于实现）
6. `scripts/smoke.sh` — 追加 `web-036a` 用例（不删除、不改动既有用例）

**超限熔断**：实现中若实际变更文件超过 6 个，Generator 必须停止实现并回报 Planner 重新拆分，不得自行扩范围。组件级白底修补、视觉微调、清理类变更一律留给 web/036b/036c。

## 交付物（预估 6 文件）

1. `frontend/src/stores/app.ts` — `applyTheme`/初始化写入 `dark` class 与 `data-theme` 双轨
2. `frontend/src/styles/global.css`（或 `main.ts`）— 接入 EP dark css-vars（顺序：在亮色 EP 变量之后）
3. `frontend/src/composables/useTheme.ts` — 视需要同步双轨（若 store 已收敛则不动）
4. `frontend/src/__tests__/stores.spec.ts` — AC1 用例
5. `frontend/src/composables/useTheme.spec.ts` — AC2 用例
6. `scripts/smoke.sh` — web-036a 产物级冒烟用例

## 变更清单

### 新增
- （无）

### 修改
- `frontend/src/stores/app.ts`:55-59 — `applyTheme()` 增加 `documentElement.classList.toggle('dark', newTheme === 'dark')`，双轨同步 dark class 驱动 EP `--el-*` 暗色变量（附 why 注释）；`toggleTheme/setTheme/initTheme` 均经此函数，切换与初始化天然一致
- `frontend/src/styles/global.css`:4-6 — 顶部 `@import 'element-plus/theme-chalk/dark/css-vars.css'`，置于亮色 `element-plus.scss` 之后保证 `.dark` 作用域变量源顺序胜出（附 why 注释）
- `frontend/src/__tests__/stores.spec.ts`:164-194 — 新增 `web-036a 主题双轨同步` describe（AC1 用例：toggleTheme/setTheme 双向断言 data-theme 与 dark class，先于实现）
- `frontend/src/composables/useTheme.spec.ts`:223-250 — 新增 `web-036a 主题双轨同步` describe（AC2 用例：`vi.importActual` 绕过文件级 store mock 取真实 store，复刻刷新恢复路径断言 dark/light 双轨一致，先于实现）
- `scripts/smoke.sh`:176-180 — web-015 用例断言计数 `27 passed`→`28 passed`（本次新增 1 条 useTheme 用例致过期，用例未删除）
- `scripts/smoke.sh`:378-392 — 追加 `web-036a EP 暗色变量接入与双轨同步验证` 冒烟用例（双轨 Vitest ×2 + 构建产物三断言）

### 删除
- （无）

## 规范检查清单

- [x] `mvn -q verify` 后端门禁（本功能点零后端改动，基线对照口径：130 用例 4F+2E 逐条一致）
- [x] `npm run lint && npm run test && npm run build` 前端门禁（test 零新增失败；lint 全量卡死为预存，分片 --fix 后零告警；build 通过）
- [x] `bash scripts/smoke.sh` 新增 web-036a 单跑通过（整体 9 失败为基线 10 条子集，预存红项基线对照口径）
- [x] 符合 Vue 3 / Vite / TypeScript strict 编码规范
- [x] 符合 TDD 工作流（测试先行、RED 证据完整、GREEN 实现、REFACTOR 全绿）
- [x] 变更文件不超过 6 个（实际 6 业务文件：app.ts、global.css、stores.spec、useTheme.spec、smoke.sh；未超熔断）

## 评审记录

- （待 Evaluator 填写）
