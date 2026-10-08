# Sprint 工作单：sprint-081

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-081 |
| 所属模块 | web |
| 功能点 ID | web/023（sprint-047 遗留收口，源码层移除） |
| 功能点名称 | 移除整个标签页栏 TagsView——源码层删除与测试同步收口 |
| 状态 | PLANNED |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/003 | DefaultLayout 骨架 | ✅ |
| web/020 | 标签页操作区移除（组件内 actions 层） | ✅ |
| sprint-080 | 台账勘误：023 系未实现非待评审 | ✅ |

## 业务背景

**半完成态考古**（本单核心事实）：web/023「移除整个标签页栏，仅保留面包屑导航」经 1d934de 大杂烩**部分实现**——布局层已移除（`DefaultLayout.vue` 零引用、`default-layout-tagsview-removed.spec` 3/3 绿、smoke `web-023` 产物断言已 ✅：孤儿组件不进产物故 JS/CSS 均无 `tags-view`），但**源码层遗留四件**：

1. `components/Layout/TagsView.vue`（10307 字节）成孤儿文件（全仓唯一非测试引用=`components.d.ts` 自动声明）
2. **5 条基线红 spec**（20 failed 中的 TagsView 5 条）：`DefaultLayout.spec AC4 renders TagsView`（布局已移除未同步，1 条）+ `tags-view-actions-removed`/`tags-view-dropdown-removed`（020/022 交付断言组件内 actions/dropdown 已移除，但组件本体经 1d934de 后仍含 `:159 actions`/`:160 el-dropdown`/`:45 refreshTag`/`:18 showMore` → 4 条红）
3. `components.d.ts`（TRACKED 入库）残留 TagsView 组件声明
4. smoke `web-021/022` 断言语义过时：仍要求「**保留**右键菜单 `tags-view__context-menu`」——组件整体移除后产物无任何 tags-view 样式 → 该 `grep` 不中系此二用例 ❌ 根因（**删源组件不改变产物**，本单必须同步用例断言）

原 047 期 4 条 AC 快照已被覆盖（task.md 单文件制式），本单 AC 依 registry 备注「移除整个标签页栏，仅保留面包屑导航」+考古语境**重建并登记**。

## 需求描述

源码层完成 web/023：删除孤儿 `TagsView.vue`、清 `components.d.ts` 声明、5 条红 spec 同步收口（改写为删除态断言或移除过时段）、smoke `web-021/022` 断言语义同步（「保留右键菜单」→ 整体移除语义），使源码/测试/冒烟三层与「仅保留面包屑」目标一致。`DefaultLayout.vue` 布局层已达成（本单零改动）。

## 验收标准（TDD 驱动）

- [ ] AC1 — 源码层移除：`frontend/src/components/Layout/TagsView.vue` 文件不存在；全仓源码（`frontend/src/`，排除 `__tests__`/spec 内删除态断言自身）零 `TagsView` 引用；`components.d.ts` 无 TagsView 声明
- [ ] AC2 — 测试同步收口：`tags-view-actions-removed.spec` 与 `tags-view-dropdown-removed.spec` 改写为「组件文件不存在」删除态断言（RED=改写时组件尚在）；`DefaultLayout.spec` AC4 段同步（renders→不存在，布局已移除）；处置后前端 `npm run test` **20 failed→15 failed（预期精确值，失败清单=原 20 减 TagsView 5 条，逐条核对无新增）**
- [ ] AC3 — 冒烟同步：`web-021/022` 断言语义更新（移除「保留 `tags-view__context-menu`」过时断言，改为产物无相关样式/与 web-023 整体移除一致）；`web-023` 既有用例保持 ✅；`bash scripts/smoke.sh` 变化=原 9❌ 中 web-021/022 **预期转绿**（9→7❌），其余逐条=基线，整体用例数=61 不变
- [ ] AC4 — 门禁：`mvn -q verify` 154 4F+2E=基线（零后端改动）；前端 lint=124 基线、build ✓；test 与冒烟变化**仅允许上述预期改善**（改善=基线变好，登记实测值，无新增失败）

### AC1 — 源码移除
> When 删除组件与声明。Then 文件不存在、源码零引用。

**用例**：`AC1 ← 用例 smoke#web/023（产物层已有）+ 定向 grep 实测`

### AC2 — 测试收口
> Given 3 spec 先行改写（RED=组件尚在/删除态断言不成立）。When 删组件。Then 15 failed 精确、零新增。

**用例**：`AC2 ← npm run test 失败清单逐条对照`

### AC3 — 冒烟同步
> When web-021/022 断言语义同步。Then 转绿、web-023 保持、其余=基线。

**用例**：`AC3 ← bash scripts/smoke.sh 61 用例逐条`

### AC4 — 门禁
> mvn/前端三线与冒烟=基线或预期改善。

**用例**：`AC4 ← 门禁实测记录`

## 测试清单

> TDD 载体=3 个 spec 删除态断言先行（RED）→删组件（GREEN）。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | tags-view-actions-removed / tags-view-dropdown-removed 改写为删除态断言（先行 RED：existsSync=true 断言失败） | AC1/AC2 | 待填 |
| 2 | DefaultLayout.spec AC4 段同步（renders→not exists） | AC2 | 待填 |
| 3 | 门禁：mvn 154 + 前端 lint124/**15 failed 预期**/build ✓ | AC4 | 待填 |
| 4 | 冒烟 61 用例（web-021/022 预期转绿、web-023 保持 ✅、9→7❌ 预期） | AC3/AC4 | 待填 |

## RED 证据

> Generator 于删除组件前执行测试清单 #1 并粘贴关键输出（组件尚在，删除态断言失败=RED 实证）。

```text
（待 Generator 填写）
```

> 实现后复跑与 REFACTOR 复查。

```text
（待 Generator 填写）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E | 待填 | 待填 |
| `npm run lint` | EXIT=124 | 待填 | 待填 |
| `npm run test` | 20 failed\|288 passed → **预期 15 failed\|293 passed（308 不变）** | 待填 | 待填 |
| `npm run build` | ✓ | 待填 | 待填 |
| `bash scripts/smoke.sh` | 61=50✅+9❌+2⏭️ → **预期 61=52✅+7❌+2⏭️**（web-021/022 转绿） | 待填 | 待填 |

## 拆分说明

预估验收标准 4 条（=上限）；预估文件变更 **6 个（=上限，超限即熔断回报）**：

1. `frontend/src/components/Layout/TagsView.vue` — **删除**（AC1）
2. `frontend/src/components.d.ts` — 清 TagsView 声明（AC1，TRACKED 入库须手动/重生成）
3. `frontend/src/__tests__/tags-view-actions-removed.spec.ts` — 改写为删除态断言（AC2，测试先行）
4. `frontend/src/__tests__/tags-view-dropdown-removed.spec.ts` — 同上（AC2）
5. `frontend/src/layouts/DefaultLayout.spec.ts` — AC4 段同步（AC2）
6. `scripts/smoke.sh` — web-021/022 断言语义同步（AC3；web-023 用例已存在不动）

**超限熔断**：>6 文件或 >4 AC 即停止回报。**不动清单**：`DefaultLayout.vue`（布局层已达成零改动）、`default-layout-tagsview-removed.spec`（3/3 绿保持）、`main-area-icon-size-audit.spec`（提及系注释级无断言）、`web-023` smoke 用例（已 ✅）、后端全部、`web-020` 相关（侧边栏折叠与本功能点无关，其 ❌ 属基线）。

**研究项（Generator 先于动手核实）**：
- **R1 15 failed 精确性**：改写前跑 `npm run test` 记录 20 failed 完整清单基线，AC2 完成后逐条对照=**15 条且恰为原清单减 TagsView 5**（若出现非 TagsView 条目变化即熔断回报）。
- **R2 components.d.ts 重生成**：unplugin-vue-components 于 dev/build 自动重生成——删组件后跑 `npm run build` 观察是否自动清除声明；不自动则手动删行并复跑 build 验证 TS 通过。
- **R3 smoke web-021/022 改写形态**：现断言 `! grep tags-view__actions`（产物本已无 ✓）+ `grep tags-view__context-menu`（本 ❌ 根因）——改写=**删除「保留 context-menu」行**并把用例语义对齐 web-023（或在注释登记「右键菜单随组件整体移除」）；改后实跑转绿确认。
- **R4 删除态断言实现**：`fs.existsSync(tagsViewPath)` 断言 false（顶层 readFileSync 须先移除，防组件删除后 spec 收集期抛错整批崩）。

**历史快照登记**：047 期 4 AC 原文已不可考（task 覆盖制式），本单 4 AC 系 Planner 依 registry 备注与考古语境重建——Evaluator 评审按重建口径。

## 交付物（6 文件）

1-2. 删 TagsView.vue + 清 components.d.ts（AC1）
3-5. 三 spec 同步（AC2）
6. smoke web-021/022 同步（AC3）

## 评审记录

- （待 Evaluator）
