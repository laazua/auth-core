# Sprint 工作单：sprint-082

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-082 |
| 所属模块 | web |
| 功能点 ID | web/011（子功能点 **web/011b 前端失败测试收敛**） |
| 功能点名称 | 修复前端失败测试——全量 test 15 failed 收敛至 0 |
| 状态 | PLANNED |
| 创建时间 | 2026-10-08 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/010 | 侧边栏菜单 404 修复（row 依赖） | ✅ sprint-035 |
| web/011a | 修复 TS 编译错误（首子功能点）：build ✓ 实证，web/026/027 以其为前置均 ✅ | ✅ 已达成 |
| sprint-081 | 基线实测入册（test 15 failed、冒烟 7❌、mvn 4F+2E） | ✅ PASS 9.4/10 |

## 业务背景

web/011「修复前端构建错误与失败测试」行 🔄 自 sprint-056：**011a（TS 编译错误）已达成**（build ✓）。本单 **011b** 收敛剩余的「失败测试」面——2026-10-08 sprint-081 评审亲测全量 `npm run test` **15 failed | 293 passed (308)**，恰分三组：

1. **auth-token 2 条**：`calls storage.set/remove` spy **零调用**（`expected "spy" to be called… Received: 空`）——需判定是产品侧持久化路径漂移（038a Cookie 双承载后是否合法改道）还是测试桩过期（R3）
2. **LoginView 2 条**：`Cannot read properties of undefined (reading 'value')` + 登录后未跳转 Dashboard——spec:13 `vi.mock('element-plus',…)` 部分桩疑似破坏组件解析（R1/R4）
3. **system/IndexView 11 条**：渲染出 `{"_":1}` Vue 块标记（**Element Plus 组件解析缺失**实证，sprint-040 已诊断同源）+ list spy 零调用；`vitest.config.ts` 仅 `vue()` 插件、无 EP 注册点（R4）

**分治登记（本单不背）**：① 全量 `npm run lint` 卡死（EXIT=**124 超时**，非错误数，已 5+ 次复现）→ 预留 **011c**；② 冒烟 7❌（web-013/020/026 过期 grep 与 **web/036c**「smoke 过期 grep 同步」重叠、model-008/010+roles-001/002 后端漂移）→ 归 036c/后续；③ DefaultLayout.spec:72/:183 两处预存 lint 按挂起区登记**并入本单清理**（R5）。

## 需求描述

修复三组共 15 条失败测试至全绿：判定每组根因（测试侧口径同步 vs 产品侧缺陷），优先测试侧共享注册点一处修复（R4）；产品侧改动计入 6 文件限额；断言只允许语义同步、禁止删除式转绿（tdd-workflow 测试质量红线）。

## 验收标准（TDD 驱动）

- [ ] AC1 — auth-token 持久化 2 条用例转绿：`calls storage.set when setToken called with token`（期望 `['token', '"test-jwt-token"']`）与 `calls storage.remove when setToken(null) called` 按 R3 判定的口径修复——若 038a 后持久化路径合法改道则同步断言至新口径（必须保留「写入/清除被实际调用」验证），若属产品缺陷则修产品；转绿后断言强度不低于原稿。← 用例 `auth-token-persistence.spec#calls storage.set…` / `#calls storage.remove…`
- [ ] AC2 — LoginView 2 条用例转绿：`handles successful login response`（无 `undefined value` 抛错，错误消息位为空或成功态）与 `redirects to dashboard after successful login`（断言 `route.name === 'Dashboard'`）根因消除。← 用例 `LoginView.spec#handles successful login response` / `#redirects to dashboard after successful login`
- [ ] AC3 — system/IndexView 11 条用例转绿：渲染含「权限管理/模块管理」tab（`_` 块标记消失=EP 解析恢复）、AC1/AC2/AC4/AC5/AC6/AC7 全部断言成立（list/getList spy 实调）。← 用例 `system/IndexView.spec` 全部 11 条 FAIL（Rendering 1+AC1 2+AC2 2+AC4 1+AC5 2+AC6 2+AC7 1）
- [ ] AC4 — 门禁：`npm run test` 全量 **0 failed**（15→0；308 基数，修复中增/改断言致基数变化须逐条登记）、定向 lint（本单触碰文件 + `DefaultLayout.spec.ts:72/:183` 预存两处清理）**0 问题**、`npm run build` ✓、`mvn -q verify` 154 4F+2E=基线、冒烟用例数 62 不减不改（7❌ 属基线对照不背锅）。← 用例 门禁五线实测

### AC1 — auth-token 组
> Given 2 条 spy 零调用 RED。When 按 R3 判定修复。Then 转绿且断言强度不降。
**用例**：`AC1 ← 用例 auth-token-persistence.spec#calls storage.set/remove`

### AC2 — LoginView 组
> Given 2 条 undefined/跳转失败 RED。When 消除组件解析/桩根因。Then 转绿。
**用例**：`AC2 ← 用例 LoginView.spec#handles successful login response / #redirects to dashboard`

### AC3 — IndexView 组
> Given 11 条 `_ 块标记/spy 零调用 RED。When EP 注册根因消除。Then 11 条转绿。
**用例**：`AC3 ← 用例 system/IndexView.spec#11 条 FAIL`

### AC4 — 门禁
> Given 基线 15 failed。When 三组修复完成。Then 全量 0 failed 且其余门禁=基线。
**用例**：`AC4 ← 门禁五线实测`

## 测试清单

> TDD 载体：**bugfix 先行**——15 条现存失败即 RED 实证（先于任何修复把三组清单与报错栈实时入 RED 槽）；修复中新增/改写的断言必须先写后修。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | auth-token-persistence.spec 2 条（根因判定 R3 后修复/口径同步） | AC1 | 待 Generator |
| 2 | LoginView.spec 2 条（element-plus 桩/解析根因） | AC2 | 待 Generator |
| 3 | system/IndexView.spec 11 条（EP 注册共享点修复） | AC3 | 待 Generator |
| 4 | 门禁五线：test 0 failed / 定向 lint 0 / build ✓ / mvn=基线 / 冒烟 62 不减 | AC4 | 待 Generator |

## RED 证据

> Generator 于任何修复动作前执行测试清单并粘贴三组完整清单（现存失败=bugfix RED 实证）。

```text
[RED] （待 Generator 于修复前执行 npm run test 并粘贴 15 failed 完整清单与三组报错栈帧）
```

> 实现后复跑。

```text
[GREEN] （待 Generator）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-08 sprint-081 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（五类，veto-6 豁免） | 待填 | 待填 |
| `npm run lint` | 全量 EXIT=124（卡死超时，011c 预留）；定向=DefaultLayout.spec 2 处预存 | 待填（定向须 0） | 待填 |
| `npm run test` | **15 failed\|293 passed (308)** → **预期 0 failed\|308+passed** | 待填 | 待填 |
| `npm run build` | ✓ | 待填 | 待填 |
| `bash scripts/smoke.sh` | 62=53✅+7❌+2⏭️（7❌ 基线不背锅） | 待填（62 不减、非本单用例不回归） | 待填 |

## 拆分说明

预估验收标准 **4 条（=上限）**；预估文件变更 **5 个，产品侧根因则 +1=6（=上限，超限即熔断回报）**：

1. `frontend/src/__tests__/auth-token-persistence.spec.ts`（AC1，若产品侧 bug 则 + 产品文件）
2. `frontend/src/views/LoginView.spec.ts`（AC2）
3. `frontend/src/views/system/IndexView.spec.ts`（AC3）
4. `frontend/src/layouts/DefaultLayout.spec.ts`（AC4 两处预存 lint 清理，挂起区登记归 web/011 在案）
5. EP 测试注册共享点（`frontend/src/__tests__/setup.ts` 或 vitest 配置，R4 判定后取一，一处修复三处受益）

**超限熔断**：>6 文件或 >4 AC 即停止回报。**不动清单**：`scripts/smoke.sh`（7❌ 归 036c/后续）、全量 lint 卡死（011c）、后端全部、`IndexView.vue`(752 行)/`LoginView.vue` 等产品文件**仅当根因判定产品侧才允许最小增量修改**（sprint-074 用户裁定先例，改动须登记历史债行数变化）。

**研究项（Generator 先于动手核实）**：
- **R1 根因三分判定**：每组先判测试侧（mock 桩不完整/注册缺失/断言过期）vs 产品侧（行为真 bug）；判定依据写入 task（复现栈帧+代码位置）。
- **R2 熔断线**：产品侧修复预估 >6 文件或动到 IndexView.vue 大改 → 停止回报，回 Planner 重切分。
- **R3 auth-token 与 038a 关系**：核查 Cookie 双承载后 token 是否仍写 storage——改道=合法演进（断言同步至新口径+登记），未改道而 spy 空=产品 bug（产品侧修）。
- **R4 EP 注册共享点**：`vitest.config.ts` 仅 `vue()`；LoginView.spec:13 `vi.mock('element-plus')` 局部桩——验证「一处注册（setup/globalRegistration）」能否同时救活三组，避免逐 spec 重复灌满 6 文件。
- **R5 断言强度红线**：任何转绿不得以删断言/放宽至恒真实现（tdd-workflow 测试质量节），Evaluator 按此核。

**历史口径登记**：web/011 行自 sprint-056 🔄，011a 已达成、本单 011b、011c（lint 卡死）预留——registry 行备注同步更新于本单。

## 交付物（5-6 文件）

1. auth-token 2 条修复/口径同步（AC1）
2. LoginView 2 条修复（AC2）
3. IndexView 11 条修复（AC3）
4. EP 注册共享点（R4 判定载体）
5. DefaultLayout.spec 两处预存 lint 清理（AC4）
6. （可选 +1）产品侧最小增量（仅当 R1 判定产品 bug 且 ≤6 线内）

## 评审记录

- （待 Evaluator）
