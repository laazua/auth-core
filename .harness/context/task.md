# Sprint 工作单：sprint-082

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-082 |
| 所属模块 | web |
| 功能点 ID | web/011（子功能点 **web/011b 前端失败测试收敛**） |
| 功能点名称 | 修复前端失败测试——全量 test 15 failed 收敛至 0 |
| 状态 | AWAITING_REVIEW |
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

**分治登记（本单不背）**：① 全量 `npm run lint` 卡死（EXIT=**124 超时**，非错误数，已 5+ 次复现）→ 预留 **011c**；② 冒烟 7❌（web-013/020/026 过期 grep 与 **web/036c**「smoke 过期 grep 同步」重叠、model-008/010+roles-001/002 后端漂移）→ 归 036c/后续；③ DefaultLayout.spec:72/:183 两处预存 lint 按挂起区登记**并入本单清理**（R5）。④ LoginView/IndexView 定向 lint 预存 196（130 prettier 错误+66 any 警告）→ **并入 011c**（案A 裁定 2026-10-08，本单引入 0）。

## 需求描述

修复三组共 15 条失败测试至全绿：判定每组根因（测试侧口径同步 vs 产品侧缺陷），优先测试侧共享注册点一处修复（R4）；产品侧改动计入 6 文件限额；断言只允许语义同步、禁止删除式转绿（tdd-workflow 测试质量红线）。

## 验收标准（TDD 驱动）

- [x] AC1 — auth-token 持久化 2 条用例转绿：`calls storage.set when setToken called with token`（期望 `['token', '"test-jwt-token"']`）与 `calls storage.remove when setToken(null) called` 按 R3 判定的口径修复——若 038a 后持久化路径合法改道则同步断言至新口径（必须保留「写入/清除被实际调用」验证），若属产品缺陷则修产品；转绿后断言强度不低于原稿。← 用例 `auth-token-persistence.spec#calls storage.set…` / `#calls storage.remove…`
- [x] AC2 — LoginView 2 条用例转绿：`handles successful login response`（无 `undefined value` 抛错，错误消息位为空或成功态）与 `redirects to dashboard after successful login`（断言 `route.name === 'Dashboard'`）根因消除。← 用例 `LoginView.spec#handles successful login response` / `#redirects to dashboard after successful login`
- [x] AC3 — system/IndexView 11 条用例转绿：渲染含「权限管理/模块管理」tab（`_` 块标记消失=EP 解析恢复）、AC1/AC2/AC4/AC5/AC6/AC7 全部断言成立（list/getList spy 实调）。← 用例 `system/IndexView.spec` 全部 11 条 FAIL（Rendering 1+AC1 2+AC2 2+AC4 1+AC5 2+AC6 2+AC7 1）
- [x] AC4 — 门禁：`npm run test` 全量 **0 failed**（15→0；308 基数，修复中增/改断言致基数变化须逐条登记）、定向 lint **本单引入 0 问题**【案A 口径裁定 2026-10-08：`DefaultLayout.spec:72/:183` 与 auth-token 预存 2 处清理完成=点名文件全 0；LoginView 151/IndexView 45 为基线预存（130 prettier+66 any，script-setup vm 访问无干净类型解），登记归 011c——原稿「触碰文件 0 问题」系 Planner 仅实测 DefaultLayout 2 处之误】、`npm run build` ✓、`mvn -q verify` 154 4F+2E=基线、冒烟 62 不减不改+本单追加 web-011b（7❌ 属基线对照不背锅）。← 用例 门禁五线实测

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
| 1 | auth-token-persistence.spec 2 条（根因判定 R3 后修复/口径同步） | AC1 | ✓ 2/2 绿（test: 提交态实测） |
| 2 | LoginView.spec 2 条（element-plus 桩/解析根因） | AC2 | ✓ 2/2 绿（feat: 覆盖桩后实测） |
| 3 | system/IndexView.spec 11 条（EP 注册共享点修复） | AC3 | ✓ 40/40 绿（11 条失败全清） |
| 4 | 门禁五线：test 0 failed / 定向 lint 0 / build ✓ / mvn=基线 / 冒烟 62 不减 | AC4 | ✓ 五线实测（见门禁表，lint 按案A 口径） |

## RED 证据

> Generator 于任何修复动作前执行测试清单并粘贴三组完整清单（现存失败=bugfix RED 实证）。

```text
[RED] npx vitest run（修复前基线 2026-10-08，实时摘录，完整日志 /tmp/opencode/e082-red.log）
Test Files  3 failed | 33 passed (36)
     Tests  15 failed | 293 passed (308)   ← 恰=工作单基线 15（2+2+11）

[AC1 auth-token 2 条] FAIL auth-token-persistence.spec#calls storage.set when setToken called with token
  → expected "spy" to be called with arguments: [ 'token', '"test-jwt-token"' ] Number of calls: 0（:16）
  根因：spec:5 `vi.mock('@/utils/storage')` 把 storage.set 链路掐成空桩，断言却打在 localStorage spy 上——测试桩自相矛盾（产品 storage.ts:16 本就写 localStorage）
FAIL auth-token-persistence.spec#calls storage.remove when setToken(null) called → Number of calls: 0（:23，同源）

[AC2 LoginView 2 条] FAIL LoginView.spec#handles successful login response
  → expected 'Cannot read properties of undefined (…' to be ''（:193）
  根因：LoginView.vue:75 `router.currentRoute.value.name` 撞 setup.ts:35 全局 useRouter 桩（仅 {push,replace,…} 无 currentRoute）→ :106 catch 写入 errorMessage
FAIL LoginView.spec#redirects to dashboard after successful login → expected 'Login' to be 'Dashboard'（:659）
  根因：useRouter 桩 push 为 no-op，真实 router 停在 /login（测试已备好 isReady+真实 router，只差组件拿到真件）

[AC3 IndexView 11 条] FAIL IndexView.spec#shows tabs for permission and module management
  → expected '{\n "_": 1\n}{\n "_": 1\n}新增权限权限名编码…' to contain '权限管理'（:412）
  根因：element-plus mock 工厂 IndexView.spec:175 模板 `{{ tab.children }}` 打印 vnode 槽对象（toDisplayString→JSON `{_:1}`），label 在 tab.props.label
FAIL IndexView.spec#loads first page by default on mount 等 10 条 → list spy Number of calls: 0（:422）
  根因：setup.ts:43 全局 useRoute 静态桩 path='/' → IndexView.vue:442 watch(route.path,{immediate}) 的 permissions/modules 两分支均不进，fetch 不执行；:435 handleTabClick 的 router.push 亦为 no-op（AC7 pushSpy 断言同源）
分布：11 failed | 29 passed（40），失败面全部=上述两类根因
```

[RED-2] test: 提交态复测（剥离两处 vue-router 覆盖桩、其余 spec 修复保留，2026-10-08 实时，完整日志 /tmp/opencode/e082-red2.log）
Test Files  2 failed | 34 passed (36)
     Tests  10 failed | 298 passed (308)   ← LoginView 2（与 [RED] 同栈）+ IndexView 8（AC1/AC2/AC4/AC5/AC6/AC7/AC8×2，全部 list/push spy 断言失败=静态路由桩根因）
说明：EP label 解析、抽屉 mode 定位、面板作用域/等待等 spec 侧修复不依赖 router 真件，在本提交态已转绿（IndexView 11→8）；
覆盖桩 `vi.mock('vue-router', …)` 归 feat: 提交——test: 提交树保持可复现 RED，供 Evaluator 回放（10 failed 即该态实测）。

> 实现后复跑。

```text
[GREEN] npm run test / npx vitest run（feat: 463f6d2 态，2026-10-08 实时，/tmp/opencode/e082-green2.log、e082-test.log）
Test Files  36 passed (36)
     Tests  308 passed (308)
差值链：[RED-2] 10 failed → [GREEN] 0——增量恰为两处 vue-router 覆盖桩；auth-token 2 条在 test: 态已转绿（根因独立于 router 桩）。
断言改写登记（强度不降红线自查）：① AC8 `toHaveBeenCalledTimes 1→2`（旧 1=坏桩下仅初始加载，新 2=初始+reload 两次实调，口径修正增强）；② AC6 造数补 `['module:view','module:create']`（组件 v-if 需要的数据准备，非断言弱化）；③ 038e pushSpy 由「mock 上断言」改「真实 router `vi.spyOn`」（真导航断言，增强）；④ 其余仅修根因（EP mock 模板 label、抽屉 mode 定位、面板作用域+20ms 等待、await router.isReady），断言原文保留——零删断言、零恒真式。
测试基数：308 全程无增减（AC4「基数变化登记」不触发）。
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-08 sprint-081 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（五类，veto-6 豁免） | **154 用例 Failures 4 + Errors 2**，五类逐条=基线（DataSourceConfigBindingTest 2F、RoleControllerTest 1F、SeedDataIntegrationTest 1F、TestLayersSpec 1E、TestUtilsSpec 1E；/tmp/opencode/e082-mvn.log） | ✓ =基线零新增 |
| `npm run lint` | 全量 EXIT=124（卡死超时，011c 预留，案A 下不跑全量）；定向=触碰 4 文件 | 定向实测 DefaultLayout **0** / auth-token **0**（预存清理完成）/ LoginView 151 / IndexView 45（均=基线预存，归 011c）→ **本单引入 0** | ✓（案A 口径） |
| `npm run test` | **15 failed\|293 passed (308)** → **预期 0 failed\|308+passed** | **0 failed \| 308 passed (308)**，EXIT=0（/tmp/opencode/e082-test.log） | ✓ 达标 |
| `npm run build` | ✓ | ✓ EXIT=0（17.44s，/tmp/opencode/e082-build.log） | ✓ 达标 |
| `bash scripts/smoke.sh` | 62=53✅+7❌+2⏭️（7❌ 基线不背锅） | **63=54✅+7❌+2⏭️**（存量 62 零删改 + 本单新增 `web-011b` ✅；7❌=基线集合逐条 model-008/010、roles-001/002、web-013/020/026 零回归；/tmp/opencode/e082-smoke.log） | ✓ 存量不减不改、零回归 |

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

## 实现说明

**根因判定（R1，三组全部=测试侧，产品文件零改动）**：
1. auth-token 2 条：spec:5 `vi.mock('@/utils/storage')` 自相矛盾（桩掐断链路、断言打在 setup.ts 的 localStorage spy 上）——移除该 mock，回归真实 `storage.set→localStorage.setItem` 链路（storage.ts:16 本就写 localStorage，038a Cookie 改道不影响 refresh/token 存储口径）。
2. LoginView 2 条：`LoginView.vue:75` 读 `router.currentRoute.value.name` 撞 setup.ts:35 全局 useRouter 桩（无 currentRoute）→ :106 catch 写入 errorMessage；桩 push no-op 使导航停在 Login——补文件级 `vi.mock('vue-router', importOriginal)` 覆盖桩（本 spec 装真实 router 插件，组件须拿到真件）。
3. system/IndexView 11 条：setup.ts:43 静态 useRoute `path:'/'` 使 IndexView.vue:442 watch(immediate) 两分支永不进入（fetch 零调用、pushSpy no-op）；element-plus mock 模板 `{{ tab.children }}` 打印 vnode 块标记 `{_:1}`——同补 vue-router 文件级覆盖桩 + 模板改 `tab.props?.label` + 抽屉 mode 定位/面板作用域/20ms 等待/`await router.isReady()` 等测试机械修复。

**提交链**：`5ce2de1` test RED（[RED] 15 failed 基线先行 + [RED-2] 10 failed 剥离覆盖桩复现态）→ `463f6d2` feat GREEN（两处覆盖桩 + DefaultLayout 两处预存 lint + smoke web-011b）→ `2c69650` docs（AWAITING_REVIEW）。

**变更清单**（纯测试侧 5 文件 + 本单文档回写）：`frontend/src/__tests__/auth-token-persistence.spec.ts`（去 mock+注释）、`frontend/src/views/LoginView.spec.ts`（+6 覆盖桩）、`frontend/src/views/system/IndexView.spec.ts`（覆盖桩+根因侧修复，130 行 diff）、`frontend/src/layouts/DefaultLayout.spec.ts`（:72 去泛型+:183 EOF）、`scripts/smoke.sh`（+7 行 web-011b）；文档 task/session/iteration/registry。断言强度四条改写登记见 RED 证据区 [GREEN] 块；AC4 案A 口径修订见验收标准与分治登记④。

## 评审意见

- （待 Evaluator）
