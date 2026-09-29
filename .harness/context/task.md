# Sprint 工作单：sprint-068

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-068 |
| 所属模块 | web |
| 功能点 ID | web/037 |
| 功能点名称 | 面包屑导航间距优化（与顶栏/内容卡片留白）+ 面包屑图标改横排（图标置于文字左侧） |
| 状态 | AWAITING_REVIEW |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/003 | 主布局(侧边菜单/顶栏)+动态菜单渲染（Breadcrumb 挂载于 DefaultLayout） | ✅ |
| web/025 | 优化主内容区图标与文字间距（breadcrumb__icon 13×13 + margin-right 12px 现状来源） | ✅ |

## 业务背景

用户需求（原文）：「导航栏的与head和main区域太紧凑了，优化到合适的距离，另外将导航栏的图标不要放在文字的上方，放在文字左边」；经 Planner 澄清问答，用户确认「导航栏」指 **header 下方的面包屑导航**（非左侧侧边栏）。

Planner 只读调研（构建产物 CSS 层叠实证，不入仓库）已定位两处根因：

1. **图标竖排根因**：`.el-breadcrumb__item` 为 `float:left;display:inline-flex`，其子元素 `.el-breadcrumb__inner`（`a`/`span`）被块化为 block 但**内部不是 flex 容器**（EP 产物规则 `.el-breadcrumb__inner{color:...}` 无 display）；叠加全局 `reset.css:63` `svg{display:block}` → svg 独占一行、文字换行其下，即用户所见「图标在文字上方」。`Breadcrumb.vue:92-97` 的 `.breadcrumb__icon` 虽写了 `flex-shrink:0`/`margin-right:12px`，但父容器非 flex，规则形同虚设。
2. **间距紧凑根因**：`DefaultLayout.vue:102` `.layout__content{padding: 0 24px 24px}` 顶距为 **0** → 面包屑紧贴 header 底边；`Breadcrumb.vue:59` `.breadcrumb{margin-bottom:16px}` → 与内容卡片仅 16px，用户感知与 main 区域同样紧凑。

对照 `docs/01-architecture.md`：纯前端布局样式优化，不涉及 §3-§6 RBAC 硬语义与 §1 技术栈表（不引入新技术栈），**无架构冲突**，无需仲裁登记。

**规模预估**：验收标准 4 条（未超 4）；预估文件变更 4 个（未超 6）→ 不拆分。

**关联登记**：与 web/025（主内容区图标文字间距 ✅）为同一区域的后续迭代，不重开 web/025；Evaluator 积压（sprint-047/048/060/061/062/063）不变，见 session-state 挂起区。

## 需求描述

1. **与 head 留白**：面包屑与顶栏（header）之间由 0 间距改为 16px 顶部留白（`.layout__content` padding-top）。
2. **与 main 留白**：面包屑与内容卡片（`.layout__page`）之间由 16px 增至 24px（`.breadcrumb` margin-bottom）。
3. **图标横排**：面包屑内图标（首页 Monitor 图标与各路由 meta.icon）渲染在文字**同一行的左侧**，垂直居中；根治方式为让 `.el-breadcrumb__inner` 成为 flex 容器（或等效的行内布局规则），且 `.breadcrumb__icon` 13×13 尺寸与右侧 12px 间距不回归。
4. **不回归**：全量前端测试零新增失败（基线：2026-09-29 实测 20 failed / 276 passed，属 web/011 预存红项）；后端零改动；冒烟追加 web-037 用例。

> 范围边界（防蔓延）：本功能点只做**面包屑**的间距与图标方向，**不做**侧边栏菜单调整、不做 Header/卡片内边距改动、不做暗色模式调整（归 web/036b/036c）；实现中若预估变更文件超过 6 个，须停止并回报 Planner 重新拆分。

## 验收标准（TDD 驱动）

- [x] AC1 — 面包屑与顶栏间距由 0 增至 16px
- [x] AC2 — 面包屑与内容卡片间距由 16px 增至 24px
- [x] AC3 — 面包屑图标渲染在文字左侧同一行且图标尺寸不回归
- [x] AC4 — 全量测试零新增失败且冒烟 web-037 通过

### AC1 — 面包屑与顶栏间距由 0 增至 16px
> `frontend/src/layouts/DefaultLayout.vue` 样式中 `.layout__content` 的 padding 由 `0 24px 24px` 改为 `16px 24px 24px`（顶距 0→16px，左右 24px 与底 24px 不变）。源码文本断言：`.layout__content` 规则块内含 `padding: 16px 24px 24px` 且不存在 `padding: 0 24px 24px`。

**用例**：`AC1 ← 用例 breadcrumb-nav.spec.ts#web-037 面包屑导航优化 > layout__content 顶距 16px 与顶栏留白`

### AC2 — 面包屑与内容卡片间距由 16px 增至 24px
> `frontend/src/components/Breadcrumb.vue` 样式中 `.breadcrumb` 的 `margin-bottom` 由 `16px` 改为 `24px`。源码文本断言：`.breadcrumb` 规则块内含 `margin-bottom: 24px` 且不存在 `margin-bottom: 16px`。

**用例**：`AC2 ← 用例 breadcrumb-nav.spec.ts#web-037 面包屑导航优化 > breadcrumb 与内容卡片间距 24px`

### AC3 — 面包屑图标渲染在文字左侧同一行且图标尺寸不回归
> 两层判定：① 单测（源码层）——`Breadcrumb.vue` 样式中存在针对 `.el-breadcrumb__inner` 的 `display: inline-flex`（或 `display: flex`）+ `align-items: center` 规则，使图标与文字同行且垂直居中；② 冒烟（产物层）——`npm run build` 后 `dist/assets/css/*.css` 中存在选择器含 `el-breadcrumb__inner` 且声明含 `display:inline-flex`（或 `display:flex`）的规则进包。同时 `.breadcrumb__icon` 的 `width: 13px`/`height: 13px`/`margin-right: 12px` 保持不变（既有 breadcrumb-spacing.spec.ts 用例持续通过）。

**用例**：`AC3 ← 用例 breadcrumb-nav.spec.ts#web-037 面包屑导航优化 > el-breadcrumb__inner 横排规则与图标尺寸不回归 + bash scripts/smoke.sh#web-037 产物横排规则进包`

### AC4 — 全量测试零新增失败且冒烟 web-037 通过
> `npm run test` 失败数不超过实现前基线 20 条（零新增失败，清单与 2026-09-29 基线逐条一致）；`npm run lint` 全量卡死为预存、分片 lint 对本次改动文件零新增告警；`npm run build` 通过；`mvn -q verify` 零后端改动与基线一致（4 Failures + 2 Errors）；`bash scripts/smoke.sh` 新增 `web-037` 用例且单跑通过、整体失败与基线逐条一致零新增。

**用例**：`AC4 ← 用例 npm run test 全量基线对照 + bash scripts/smoke.sh#web-037 间距与横排产物验证整体跑`

## 测试清单

> 先于实现写出，规则遵循 `.harness/rules/tdd-workflow.md`；Generator 运行确认 RED 后填入实际输出。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | breadcrumb-nav.spec.ts#web-037 面包屑导航优化 > layout__content 顶距 16px 与顶栏留白 | AC1 | ✅ RED（1 failed 实时留痕）→ GREEN |
| 2 | breadcrumb-nav.spec.ts#web-037 面包屑导航优化 > breadcrumb 与内容卡片间距 24px | AC2 | ✅ RED（1 failed 实时留痕）→ GREEN |
| 3 | breadcrumb-nav.spec.ts#web-037 面包屑导航优化 > el-breadcrumb__inner 横排规则与图标尺寸不回归 | AC3 | ✅ RED（1 failed 实时留痕）→ GREEN |
| 4 | 冒烟 web-037 面包屑间距与图标横排产物验证（`.el-breadcrumb__inner` display 进包 + `.layout__content` 16px + `.breadcrumb` 24px 三断言） | AC3/AC4 | ✅ RED（既有产物无横排规则实证）→ GREEN（单跑通过，产物三断言全中） |
| 5 | 全量前端测试基线对照（实现前 20 failed / 276 passed，零新增失败） | AC4 | ✅ 实测 20 failed / 279 passed（299 = 基线 296 + 新增 3，failed 分布逐条一致零新增） |

## RED 证据

> Generator 于实现前执行测试清单 #1~#4 并粘贴关键失败输出。

```text
[RED] src/components/__tests__/breadcrumb-nav.spec.ts — web-037 面包屑导航优化 > layout__content 顶距 16px 与顶栏留白
  AssertionError: expected '.layout__content {\n  flex: 1;\n  ove…' to match /padding:\s*16px 24px 24px/
[RED] src/components/__tests__/breadcrumb-nav.spec.ts — web-037 面包屑导航优化 > breadcrumb 与内容卡片间距 24px
  AssertionError: expected '.breadcrumb {\n    margin-bottom: 16p…' to match /margin-bottom:\s*24px/
[RED] src/components/__tests__/breadcrumb-nav.spec.ts — web-037 面包屑导航优化 > el-breadcrumb__inner 横排规则与图标尺寸不回归
  AssertionError: expected null not to be null（源码无 .breadcrumb :deep(.el-breadcrumb__inner) 单 deep 横排规则）
  Test Files  1 failed (1) | Tests  3 failed (3)
（实测时间 2026-09-29，实现前执行）
```

## 门禁与冒烟记录

- 后端 `mvn -q verify`：130 用例 4 Failures + 2 Errors，与基线逐条一致（DataSourceConfigBinding ×2、RoleControllerTest 409、SeedData 22≠2、TestLayers/TestUtils Docker 缺失），零后端改动、零新增 ✅（基线对照裁决口径）
- 前端全量测试：`Test Files 6 failed | 28 passed (34)`，`Tests 20 failed | 279 passed (299)`——failed 20 与基线逐条一致（DefaultLayout 1、auth-token 2、tags-view-actions/dropdown 4、LoginView 2、system/IndexView 11），新增 3 用例全绿零新增失败 ✅
- 前端 `npm run build`：✓ built in 17.55s；产物三断言预检全中（`.layout__content` padding:16px 24px 24px、`.breadcrumb` margin-bottom:24px、`.breadcrumb[data-v] .el-breadcrumb__inner{display:inline-flex` 单 :deep 正确编译无字面残留）✅
- `npm run lint`：全量卡死为预存（沿基线对照口径）；分片 lint `npx eslint src/components/Breadcrumb.vue src/layouts/DefaultLayout.vue src/components/__tests__/breadcrumb-nav.spec.ts` → 本次改动两文件 exit 0 零告警，DefaultLayout.vue 78 处 prettier 经 stash 对照实证与基线逐条一致（预存，不做无关格式化）✅
- `bash scripts/smoke.sh`：**50 用例**（49 基线 + 新增 web-037），失败 9 条与基线 10 条子集逐条一致（model-008/010、roles-001/002、web-013、web-020/021/022、web-026；infra-001 连续第二轮转绿属环境波动）零新增；**web-037 单跑 ✅ 通过**；web-015 计数断言 `28 passed` 未受影响（本次 3 用例置于独立 spec，未并入其覆盖的四个文件）✅
- AC3 浏览器视觉复核（非门禁）：本机无 Playwright/Chromium 未执行，产物横排规则进包 + 源码单测已构成 AC3 判定证据；实际视觉效果（图标同行、留白观感）留待 Evaluator 复核

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 **4 个**：

1. `frontend/src/layouts/DefaultLayout.vue` — `.layout__content` padding-top 0→16px（AC1）
2. `frontend/src/components/Breadcrumb.vue` — `.el-breadcrumb__inner` 横排规则 + `.breadcrumb` margin-bottom 16→24px（AC2/AC3）
3. `frontend/src/components/__tests__/breadcrumb-nav.spec.ts` — 新建，AC1/AC2/AC3 用例（先于实现，源码文本断言形态沿用 web/025 breadcrumb-spacing.spec.ts 先例）
4. `scripts/smoke.sh` — 追加 `web-037` 用例（不删除、不改动既有用例；新用例置于独立 spec，web-015 覆盖的四个文件无新增用例，计数 `28 passed` 不过期）

**超限熔断**：实现中若实际变更文件超过 6 个，Generator 必须停止实现并回报 Planner 重新拆分，不得自行扩范围。侧边栏、Header、卡片内边距、暗色系调整一律不做。

## 交付物（预估 4 文件）

1. `frontend/src/layouts/DefaultLayout.vue` — 内容区顶部留白 16px
2. `frontend/src/components/Breadcrumb.vue` — 图标横排规则 + 与卡片间距 24px
3. `frontend/src/components/__tests__/breadcrumb-nav.spec.ts` — AC1/AC2/AC3 用例（测试先行）
4. `scripts/smoke.sh` — web-037 产物级冒烟用例

## 变更清单

### 新增
- `frontend/src/components/__tests__/breadcrumb-nav.spec.ts` — AC1/AC2/AC3 源码层三断言用例（测试先行，52 行）

### 修改
- `frontend/src/layouts/DefaultLayout.vue`:102 — `.layout__content` padding `0 24px 24px`→`16px 24px 24px`，面包屑与顶栏留白 0→16px（AC1）
- `frontend/src/components/Breadcrumb.vue`:59 — `.breadcrumb` margin-bottom `16px`→`24px`，面包屑与内容卡片留白 16→24px（AC2）
- `frontend/src/components/Breadcrumb.vue`:92-97 — 新增单 `:deep(.el-breadcrumb__inner)` 规则 `display:inline-flex; align-items:center`（附 why 注释：串联 :deep() 编译残留字面量失效），图标与文字同行且垂直居中（AC3）
- `scripts/smoke.sh`:392-404 — 追加 `web-037 面包屑间距与图标横排验证` 冒烟用例（单测 3 passed + 构建产物三断言；不删除、不改动既有用例）

### 删除
- （无）

## 规范检查清单

- [x] `mvn -q verify` 后端门禁（本功能点零后端改动，基线对照口径：130 用例 4F+2E 逐条一致）
- [x] `npm run lint && npm run test && npm run build` 前端门禁（test 零新增失败；lint 全量卡死为预存，分片本次改动文件零告警、DefaultLayout 78 处预存与基线一致；build 通过）
- [x] `bash scripts/smoke.sh` 新增 web-037 单跑通过（50 用例，9 失败为基线 10 条子集，预存红项基线对照口径）
- [x] 符合 Vue 3 / Vite / TypeScript strict 编码规范
- [x] 符合 TDD 工作流（测试先行、RED 证据完整、GREEN 实现、REFACTOR 全绿）
- [x] 变更文件不超过 6 个（实际 4 个：DefaultLayout.vue、Breadcrumb.vue、breadcrumb-nav.spec.ts、smoke.sh，未超熔断）

## 评审记录

（待 Evaluator 填写）
