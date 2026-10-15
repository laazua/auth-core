# Sprint 工作单：sprint-101

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-101 |
| 所属模块 | **web** |
| 功能点 ID | **web/036c2b**（036c2 拆分第二支，前支 036c2a ✅ sprint-098） |
| 功能点名称 | 死代码清理：Layout.vue + styles/index.ts + components/index.ts 坏导出行 |
| 状态 | **PLANNED** |
| 创建时间 | 2026-10-15 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/036 | 暗色模式治理（036c2 拆分：036c2a ✅、036c2b 本次、036c3 预留） | 🔄 |
| web/052 | 死文件清理先例（四占位 View 删除+smoke G1/G2 护栏，sprint-097 PASS 9.0） | ✅ |

## 业务背景

**Planner 2026-10-15 只读实测**（sprint-067 拆分时预估 036c2b「>6 问案B」，实测证伪——恰 4 文件 ≤6 顶格，**无需问案B**）：

| 死代码 | 规模 | 实证 |
|--------|------|------|
| `src/layouts/Layout.vue` | 103 行 | 全仓 grep 零 import（路由用 DefaultLayout；`layouts/Layout` 命中=0） |
| `src/styles/index.ts` | 4 行 | 零引用（main.ts 直接 `import '@/styles/global.css'`；scss/css 经 TS `export *` 本就不可导出） |
| `src/components/index.ts:6` | 1 行 | `export { default as Layout } from './Layout.vue'` 指向**不存在**的 `components/Layout.vue`（坏导出）；该 barrel 零消费方（`from '@/components'` 全仓命中=0，组件经 unplugin 自动导入） |

TagsView.vue 已先行移除（tags-view-*-removed.spec 护栏在场），不在本单。smoke web-015/016/017 当前全绿且断言指向真实产物，**不动**。components/index.ts 整体 barrel 零消费=疑似全死，但拆分登记范围仅「Layout.vue/TagsView.vue/styles.index.ts」三件——本次**只删坏导出行**（最小集），barrel 整体移除另案预留（避免与 unplugin 生成物 components.d.ts 交互风险未探明时扩大动作）。

## 验收标准（TDD 驱动）

- [ ] AC1 — RED（护栏先行）：新增冒烟用例 `web-036c2b 死代码清理与护栏`——G1 `test -f src/layouts/Layout.vue && test -f src/styles/index.ts`（在场才通过→删除后转绿语义相反：G1 改为**三断言** `test ! -f` Layout.vue、`test ! -f` styles/index.ts、`! grep -q "from './Layout.vue'" components/index.ts`）；G2 护栏=`grep -rn "layouts/Layout\|styles/index" src/ --include=*.vue --include=*.ts` 零命中（排除测试）。**首跑红**=三文件均在场（G1 三条 `test ! -f` 均 false）。← 用例 `AC1 ← 用例 bash scripts/smoke.sh web-036c2b 首跑红`
- [ ] AC2 — GREEN：`git rm src/layouts/Layout.vue src/styles/index.ts` + 编辑 `src/components/index.ts` 删第 6 行坏导出 → 冒烟用例转绿、前端门禁三线+vue-tsc 全绿。← 用例 `AC2 ← 用例 bash scripts/smoke.sh web-036c2b 绿`
- [ ] AC3 — 前端门禁=基线：lint 0E（W 数随死文件删除预期下降，**实测登记新值**）、vitest 41 文件 324/324 不变（零 spec 引用死文件，已 grep 实证）、build ✓、`vue-tsc --noEmit` 零错。← 用例 `AC3 ← 用例 前端四线`
- [ ] AC4 — 冒烟全量：**78=76✅+2⏭**（77+新增 1 用例；❌ 保持 0——sprint-100 起基线）。← 用例 `AC4 ← 用例 bash scripts/smoke.sh 全量 SMOKE PASSED（78 用例）` |

### AC1 — 护栏 RED

**步骤与期望**：smoke.sh 用例区末尾追加 `web-036c2b` 用例（先于删除提交）；首跑=红（G1 三 `test ! -f` 全 false）。

**用例**：`AC1 ← 用例 smoke web-036c2b 首跑 EXIT=1`

### AC2 — 删除 GREEN

**用例**：`AC2 ← 用例 smoke web-036c2b EXIT=0`

### AC3/AC4 — 门禁与冒烟

**用例**：`AC3/AC4 ← 用例 前端四线+全量冒烟`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | `bash scripts/smoke.sh web-036c2b` 首跑红（三文件在场） | AC1 | 待红 |
| 2 | 同上转绿（三文件已删+坏导出行已删） | AC2 | 待绿 |
| 3 | lint / vitest / build / vue-tsc 四线 | AC3 | 待亲测 |
| 4 | `bash scripts/smoke.sh` 全量 78=76✅+2⏭ 0❌ | AC4 | 待亲测 |

## RED 证据

```text
[修复前红态] 证据槽 — 待 Generator 填写（smoke web-036c2b 首跑：G1 三条 test ! -f 均 false → EXIT=1）
```

## 门禁与冒烟记录

> Generator 亲测填写。当前基线（sprint-100 起生效）：lint 0E|166W、vitest 41/324、build ✓、冒烟 77=75✅+2⏭ 0❌。

| 门禁项 | 本单后预期 | 实测 | 结论 |
|--------|---------|------|------|
| `npm run lint` | 0E（W≤166，死文件删除后实测登记） | 待填 | 待验 |
| `npm run test` | 41 文件 324/324（不变） | 待填 | 待验 |
| `npm run build` | ✓ | 待填 | 待验 |
| `vue-tsc --noEmit` | 零错 | 待填 | 待验 |
| `bash scripts/smoke.sh` | **78=76✅+2⏭ 0❌**（+1 用例） | 待填 | 待验 |
| 后端 | 零动（mvn 不跑，回归面无后端文件） | — | 零动 |

## 实现说明

**实现要点**：
1. test 段：smoke.sh 用例区末尾追加 web-036c2b 用例（G1 三 `test ! -f`+坏导出行断言、G2 零引用 grep 护栏）→ 首跑红。
2. feat 段：`git rm src/layouts/Layout.vue src/styles/index.ts`；`components/index.ts` 删 `export { default as Layout } from './Layout.vue';` 行 → smoke web-036c2b 绿 → 前端四线 → 全量冒烟 78。
3. G2 护栏 grep 排除测试目录（tags-view-*-removed.spec 等护栏测试字符串引用 `components/Layout/TagsView.vue` 路径属正常）。

**超限熔断**：改动 >4 文件（含 smoke.sh）、动路由/DefaultLayout.vue/components.d.ts、删 components/index.ts 整体（另案预留）、动 smoke 既有用例（web-015/016/017）、mvn 后端文件 → 停止回报。

**不动清单**：smoke 既有 77 用例（仅追加 1）；DefaultLayout.vue 及其 spec；components.d.ts（unplugin 生成物）；tags-view-*-removed.spec（护栏）；后端全部；application.yml.template D 态。

## 交付物（4 业务文件）

1. `scripts/smoke.sh` — 追加 web-036c2b 用例（77→78）
2. `frontend/src/layouts/Layout.vue` — 删除（103 行）
3. `frontend/src/styles/index.ts` — 删除（4 行）
4. `frontend/src/components/index.ts` — 删坏导出行（1 行）

### 新增
（无）

### 修改
- `scripts/smoke.sh`
- `frontend/src/components/index.ts`

### 删除
- `frontend/src/layouts/Layout.vue`
- `frontend/src/styles/index.ts`

### 变更计数

业务文件 **4**；净 -107 行（103+4+1）+1 冒烟用例；后端零动；组件 barrel 整体移除另案预留。

## 评审意见

（待 Evaluator 评审）
