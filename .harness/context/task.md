# Sprint 工作单：sprint-084

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-084 |
| 所属模块 | web |
| 功能点 ID | **web/036b**（web/036 行子功能点；036a ✅ sprint-067、036c 预留不动） |
| 功能点名称 | 暗色模式局部白底缺陷修复——BaseTable 遮罩死选择器 + settings 非法 variant prop + 反模式全仓清零 |
| 状态 | AWAITING_REVIEW（Generator 完成，门禁+冒烟通过，待 Evaluator） |
| 创建时间 | 2026-10-09 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/036a | EP 暗色变量体系接入 + 双轨同步（data-theme + html.dark），本单修复面以其为底座 | ✅ sprint-067 |
| web/001/003/008/016 | 036 行登记前置（布局/主题基础） | ✅ |
| sprint-083 | 门禁基线（lint 0E\|165W、test 308、冒烟 64=55✅+7❌+2⏭️、mvn 154 4F+2E） | ✅ PASS 9.3/10 |

## 业务背景

036b 为 registry 036 行预留子功能点（067 登记：BaseTable 遮罩失效选择器、settings 非法 variant prop、组件级覆盖补漏、暗色系协调映射）。本日只读调研将边界**钉死为可证实的两点缺陷 + 反模式全仓清查**：

1. **BaseTable 遮罩暗色规则死选择器（症状实锤）**：`frontend/src/components/BaseTable.vue:205-210` 亮色规则 `[data-v-…] .el-loading-mask{background:#ffffffe6!important}` 编译有效；暗色规则源为 `[data-theme='dark'] & { }`（& 父选择器惯用），SCSS+vue scoped 编译产物为 **`[data-theme=dark][data-v-c7766763] .el-loading-mask`——要求同一元素同时携带 data-theme（在 `<html>`）与 data-v（在组件元素）→ 永不命中**。时序证据：reset.css 全局暗规则（`[data-theme="dark"] .el-loading-mask{background-color:#0f172acc!important}`）在 index chunk，scoped 白规则在 `el-drawer-*.css` 晚加载 chunk，二者选择器特异度同为 (0,2,0) → **源序在后者胜** → 暗色下表格 v-loading 遮罩=白 0.9 底。产物 grep 实证（byte 偏移 34192/34275，index chunk 无 ffffffe6）。
2. **settings 非法 variant prop**：`frontend/src/views/settings/IndexView.vue:148,163` 对 `ElButton` 传 `variant="primary"/"danger"`——**Element Plus 按钮无 variant prop**（`variant` 是项目自有 BaseButton 的 prop，见 `BaseButton.vue:6`），预期主色/危险色样式从未生效（亮暗两态均渲染默认灰按钮）。全站惯例：**14 个 views 用 `<BaseButton>`，settings 是唯一 `<ElButton>` 用点、全仓 `type=` 用点为 0**——作者意图即 BaseButton。
3. **反模式全仓清查（036b 边界钉死）**：`[data-theme='dark'] &` 同元素复合死模式**恰 1 处**（BaseTable:207）；`<ElButton … variant=` **恰 2 处**；非 spec 的 .vue 硬编码 `rgba(255, 255, 255` 浅底**恰 1 处**（即遮罩亮色源 :205）。067 登记的「组件级覆盖补漏、暗色系协调映射」其余面在 036a 波内已完成（上述清查=0 反证），本单不做扩大解释。
4. **连带口径同步（防自伤 veto-6）**：smoke `web-011b` 用例硬编码 `Tests 308 passed (308)` 精确匹配，本单新增 spec 必破线——须同波**语义同步**（计数模式断言、用例不删除），082 评审改进建议①在本单提前落地。

**分治登记（本单不背）**：① 串联 `:deep()` 42 处/17 组件（068 登记）→ **036c**；② 冒烟 7❌（036c/后续）；③ any 165 → **011d**；④ `--max-warnings` 与版本升级（§1 固化）；⑤ 死代码与冗余覆盖清理 → 036c。

## 需求描述

修复暗色模式两处已证实局部白底/失效缺陷并清零同类反模式：① BaseTable 加载遮罩暗色覆盖选择器编译可命中（亮色不回归）；② settings 两处按钮按全站惯例改用 BaseButton（variant 语义生效）；③ 同元素死模式与非法 prop 全仓清零、产物暗色面不回归。以源级 spec + 构建产物断言双层守护，追加冒烟用例并连带同步 web-011b 计数断言。

## 验收标准（TDD 驱动）

- [x] AC1 — BaseTable 遮罩暗色覆盖可命中：修复后 `BaseTable.vue` 不再含 `[data-theme='dark'] &` 反模式；构建产物存在**可命中**的暗色遮罩规则（选择器含 `[data-theme=dark]` 且**不存在** `[data-theme=dark][data-v-` 同元素复合形态）且亮色遮罩规则（`ffffffe6` 或等效 rgba 0.9）仍在。← 用例 `AC1 ← 用例 web-036b mask spec（源断言）+ 产物断言（build 后 grep）`）；实测：源断言 2/2、死形态=0、可命中规则 `[data-theme=dark] .el-loading-mask{#0f172ae6!important}`、亮规则 `ffffffe6` 在。
- [x] AC2 — settings 非法 variant 清零：两处按钮改 `<BaseButton variant="primary">`（前往修改）/`<BaseButton variant="danger">`（退出登录），点击行为 `router.push('/profile/password')`/`handleLogout` 零变；全仓 `<ElButton[^>]*variant=` = 0；settings 的 ElButton import 移除。← 用例 `AC2 ← 用例 web-036b settings spec（源断言）`；实测：spec 3/3 绿、全仓 `<ElButton variant=`(.vue)=0、两按钮 BaseButton variant=primary/danger、import 已移除 ElButton。
- [x] AC3 — 反模式清零与暗色面不回归：①`[data-theme='dark'] &` 全仓 = 0；②非 spec 的 .vue 中硬编码 `rgba(255, 255, 255` 浅底 = 0；③产物 `[data-theme=dark]` 规则数 ≥ 10 且死形态 = 0（2026-10-09 用户案A裁决校准：基线11含1条死形态，合法移除后=10；计数降为辅断言）且 036a 三断言保持——`.dark{…--el-` EP 暗段、`--el-bg-color:#fff` 亮值、`--color-bg-page:#020617` 暗段。← 用例 `AC3 ← 用例 spec 全仓清查 + build 产物三断言`；实测：源层两反模式=0、产物暗规则=10(案A≥10)+死形态=0、036a 三断言 A/B/C 全过。
- [x] AC4 — 门禁五线与冒烟 65：`mvn -q verify` 154 用例 4F+2E = 基线五类；`npm run test` **0 failed**（基线 308 + 本单新增，新增数入册）；`npm run build` ✓；`bash scripts/smoke.sh` 存量 64 不减 + 新增 `web-036b` → **65**（7❌ 基线不背锅），且 `web-011b` 用例同步为计数模式断言后 ✅。← 用例 `AC4 ← 用例 门禁五线实测 + 冒烟 65`；实测：mvn 154=4F+2E 五类基线、test 315 passed(315)、build ✓17.54s、冒烟 65=56✅+7❌+2⏭️、web-011b 同步后 ✅。

### AC1 — 遮罩暗色覆盖可命中
> Given BaseTable 暗色遮罩规则编译为同元素双属性死形态。When 修复选择器形态并构建。Then 产物存在可命中暗色 mask 规则、死形态归零、亮色规则不回归。
**用例**：`AC1 ← 用例 web-036b mask spec + 产物断言`

### AC2 — settings 非法 variant 清零
> Given ElButton 无 variant prop、意图样式从未生效。When 按 14:1 惯例改用 BaseButton 并清理 import。Then variant 语义合法生效、行为零变、全仓非法 prop 归零。
**用例**：`AC2 ← 用例 web-036b settings spec`

### AC3 — 反模式清零与暗色面不回归
> Given 反模式恰 3 处。When 全部清零且构建。Then 源层清查=0、死形态=0、产物暗色规则 ≥10（案A校准）、036a 三断言保持。
**用例**：`AC3 ← 用例 spec 全仓清查 + build 三断言`

### AC4 — 门禁五线与冒烟 65
> Given 基线 mvn 154 4F+2E、test 308、冒烟 64。When 本单交付。Then 五线=基线/达标、冒烟 65 且 web-011b 计数断言同步。
**用例**：`AC4 ← 用例 门禁五线实测`

## 测试清单

> TDD 载体：**源级断言 + 产物断言型缺陷修复**——RED 三实证先行（先于任何修复动作）：① 反模式 spec 先写（源断言修复前必红）；② 产物形态断言先写（build 后死形态在、可命中形态缺 → 红）；③ smoke `web-036b` 用例先写（修复前必失败）。修复手段不得触碰断言语义；web-011b 计数断言同步须与新增测试同波落位并登记。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | `web-036b` 反模式 spec（死模式/非法 prop/硬编码浅底 全仓清查断言） | AC1/AC2/AC3 | ✓ RED-1=7 failed（修复前必红，e084-red1.log） |
| 2 | 产物形态断言（死形态 `[data-theme=dark][data-v-` 归零 + 可命中暗 mask 规则存在 + 036a 三断言） | AC1/AC3 | ✓ RED-2 基线=死形态1/暗规则11/036a 三断言在（e084-red2.log），断言已入 smoke web-036b |
| 3 | smoke `web-036b` 暗色遮罩与按钮语义修复（先写后绿）+ `web-011b` 计数断言语义同步 | AC1/AC2/AC4 | ✓ RED-3=用例体 EXIT=1 必失败 + 011b 同步断言 RED 期正确拒绝（7 failed 拒） |
| 4 | 门禁五线：mvn=基线 / test 0 failed（308+新增）/ build ✓ / 冒烟 65 | AC4 | ✓ 五线实测全过（mvn 154=4F+2E、test 315、build 17.54s、冒烟 65=56✅+7❌+2⏭️、011b ✅） |

## RED 证据

> Generator 于任何修复动作前执行测试清单并粘贴实证；产物形态基线快照（修复前 build）必须先存。

```text
[RED-1] 反模式 spec 首跑（2026-10-09 实时，/tmp/opencode/e084-red1.log）：
RED1_EXIT=1 → Failed Tests 7（spec 首写即 7 断言全红）
  ✗ BaseTable 不含 [data-theme='dark'] &（缺陷在场）
  ✗ BaseTable 不硬编码 rgba(255, 255, 255（:205 在场）
  ✗ settings 零 ElButton / ✗ BaseButton variant=primary+danger / ✗ 全仓 <ElButton variant=
  ✗ 全仓零 & 死模式 / ✗ 全仓零硬编码浅底（BaseTable:205 唯一命中）
  Tests  7 failed (7)

[RED-2] 产物形态基线（2026-10-09 实时，修复前 npx vite build，/tmp/opencode/e084-red2.log）：
BUILD_OK；死形态 `[data-theme=dark][data-v-c7766763] .el-loading-mask` 计数=1（在场）
可命中规则并存态：reset.css 祖先形态 `[data-theme=dark] .el-loading-mask{#0f172acc}` ✓（被 scoped 白规则特异度同级+晚载压制）
亮规则 `[data-v-c7766763] .el-loading-mask{#ffffffe6}` 在；[data-theme=dark] 规则总数=11
036a 三断言基线：.dark{--el ✓ / --el-bg-color:#fff ✓ / --color-bg-page: #020617 ✓（空格形态）

[RED-3] smoke web-036b 用例体首跑（2026-10-09 实时，/tmp/smoke-web-036b-red.log）：
RED3_CASE_EXIT=1（首条源层断言即红——BaseTable 死模式在场）
连带 web-011b 计数断言语义同步（passed==total 模式）RED 期实测：
  vitest 全量 7 failed | 308 passed (315) → 用例 `|| exit 1` 门禁与同步断言双双正确拒绝（同步非放宽证据）
```

> 治理后复跑。

```text
[GREEN]（2026-10-09 实时）
spec：Tests 7 passed (7)（e084-green1.log，SRC_EXIT=0）
产物：死形态计数=0；可命中暗 mask 规则 `[data-theme=dark] .el-loading-mask{background-color:#0f172ae6!important}`（0.9=案A前 R1 判定值）；
  亮规则 `ffffffe6` 在（reset 0.9 + EP `--el-mask-color` 双处）；暗规则总数=10（案A ≥10 达标）；036a 三断言 A_OK/B_OK/C_OK
源层复扫：rgba(255,255,255 .vue=0；`[data-theme='dark'] &` .vue=0；`<ElButton variant=` .vue=0
门禁：lint EXIT=0（0E/165W=基线）｜test 0 failed | 315 passed (315)（308+7）｜build ✓ 17.54s｜mvn 154=4F+2E 五类逐一等于基线
冒烟（复跑标准口径 /tmp/e084-smoke2.log）：65 = 56✅+7❌+2⏭️，7❌=已知分治清单零新增；web-036b/011b/011c/036a 全 ✅
  连带插曲（已修）：首跑 web-036b ❌——源层 ElButton grep 未加 `--include='*.vue'` 误命中本单 spec 用例名字符串（.ts），
  修正为仅扫 .vue 后用例体 CASE_EXIT=0，全量复跑 56✅ 达标；此为用例自身口径缺陷，非实现回退。
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-09 sprint-083 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（五类，veto-6 豁免） | **154=4F+2E**（DataSource 2F/Role 1F/Seed 1F/TestLayers 1E/TestUtils 1E，逐一=基线） | ✅ 基线内 |
| `npm run lint` / `lint:check` | **EXIT=0，0 errors / 165 warnings**（083 治后） | **EXIT=0，0 errors / 165 warnings** | ✅ |
| `npm run test` | **0 failed \| 308 passed**（新增后=308+N） | **0 failed \| 315 passed (315)**（308+本单 spec 7） | ✅ |
| `npm run build` | ✓（~18s） | **✓ built in 17.54s**（1790 modules） | ✅ |
| `bash scripts/smoke.sh` | **64=55✅+7❌+2⏭️**（7❌ 基线） | **65=56✅+7❌+2⏭️**（+web-036b ✅；7❌ 逐一=基线清单，零新增） | ✅ |

## 拆分说明

预估验收标准 **4 条（=上限）**；预估文件变更 **≈4-5**（`BaseTable.vue` 1 + `settings/IndexView.vue` 1 + spec 1 + `scripts/smoke.sh` 1，文档回写 4 不计入）——**≤6 文件不拆分**。前置依赖全 ✅（036a/001/003/008/016）。

**研究项（Generator 先于动手核实）**：
- **R1 遮罩修复形态判定**：三选一——① scoped 内改祖先形态（如 `html[data-theme='dark'] &` 或 `:global` 等价）；② 覆盖下沉至 reset.css 全局层（允许 reset.css **仅遮罩行**最小增改，其余不动）；③ 改用 `var(--el-mask-color)` 暗变量跟随 EP 双轨。判定标准=**产物可命中形态存在 + 死形态归零 + 亮色遮罩不回归**；禁止删除遮罩样式或降级 `!important` 语义导致亮色回归。
- **R2 组件体系判定**：settings 换 BaseButton 的最小 diff（模板 2 处 + import 调整），断言 variant 归属与点击行为（router.push/handleLogout）零变；确认 BaseButton 在暗色下的样式覆盖（element-plus.scss/global 层已有 base-button 规则则只验不改）。
- **R3 产物断言口径**：死形态 grep（`\[data-theme=dark\]\[data-v-`）必须无命中；可命中形态 grep（`\[data-theme=dark\][^{}]*\.el-loading-mask`）必须命中；暗规则计数 ≥10 为辅断言（基线11含死形态1条；案A裁决=形态为主计数为辅，死形态归零后合法计数10）。

**问答在案（2026-10-09 用户裁决）**：
- **案A — AC3③ 计数口径校准**：GREEN 期实测移除死形态后产物 `[data-theme=dark]` 规则=10（基线11含死形态1条——永不命中的坏规则；移除后有效覆盖反而提升，遮罩暗色由「永白」变命中）。裁决：AC3③ 下限 **≥10 且死形态=0**，主断言=死形态归零+可命中规则在+036a 三断言，计数为辅断言。同 083 案A 先例（AC 字面与现实冲突→用户裁定→登记在案）。

**超限熔断**：实际代码文件 >6、或需改 EP/构建版本、或修复中出现删除遮罩/删除既有全局暗规则等行为回退、或反模式清查发现第 4 处未知缺陷面 → **停止回报，回 Planner 重切分**。

**不动清单**：后端全部；串联 `:deep()` 42 处（036c）；冒烟 7❌（036c/后续）；any 165（011d）；`--max-warnings` 与版本升级（§1）；`reset.css`/`variables.scss`/`element-plus.scss` 除 R1 判定的遮罩行外零改动；`web-011b` 用例**仅计数断言语义同步、不删除、不放宽为无效断言**；036c 其余（死代码、过期 grep）。

## 交付物（≈4-5 文件）

1. `BaseTable.vue` 遮罩选择器修复（R1 判定形态）
2. `settings/IndexView.vue` 两处按钮 → BaseButton（R2）
3. `web-036b` 反模式/产物断言 spec（源断言全仓清查 + 产物双断言）
4. `scripts/smoke.sh` + `web-036b` 用例（先写）+ `web-011b` 计数断言语义同步
5. 文档回写：task/session/iteration/registry 四件

## 实现说明

**R1 判定（遮罩形态）**：采用**全局下沉**（选项②）——删除 BaseTable 整个 `:deep(.el-loading-mask)` 块（白底 `rgba(255,255,255,0.9)` 与死形态 `[data-theme='dark'] &` 一并离场，满足 AC3②「.vue 零硬编码浅底」），遮罩主题唯一真相源=reset.css 全局对（亮 `rgba(255,255,255,0.9)` / 暗 `[data-theme="dark"]` 祖先形态 `rgba(15,23,42,0.9)`，仅改 0.8→0.9 两行守住 AC1「亮规则 0.9=ffffffe6」字面）。判否其余两案：① scoped 祖先形态仍留 .vue 暗遮罩规则，AC3② 的「白底离场」字面虽可过但暗规则仍散落组件层、且与 reset 双源易再漂移；③ `var(--el-mask-color)` 依赖 EP 变量注入链路，改动面大于两行且 036a 未盖该链路断言。

**R2 判定（按钮）**：settings 两处 `<ElButton variant=…>` → `<BaseButton variant=…>`（EP 无 variant prop，EP button 不吃该语义）；import 行移除 `ElButton`、增 `import BaseButton from '@/components/BaseButton.vue';`（14 视图显式 import 惯例）；BaseButton 变体样式 scoped `&--primary/&--danger` 编译完备，`@click="router.push('/profile/password')"`/`@click="handleLogout"` 经 BaseButton emit('click') 原样透传，行为零变。

**变更清单**（代码 4 文件 + 测试 2 文件）：
- `frontend/src/components/BaseTable.vue`：删除 `:deep(.el-loading-mask)` 整块（-9 行，死形态+白底同时清零）
- `frontend/src/styles/reset.css`：遮罩对 0.8→0.9 两行（唯一全局改动，落在不动清单允许的遮罩行内）
- `frontend/src/views/settings/IndexView.vue`：import 1 行调整 + 模板 2 处 ElButton→BaseButton
- `scripts/smoke.sh`：新增 web-036b 用例 + web-011b 计数断言同步（test: 段落）+ 用例 ElButton grep 补 `--include='*.vue'`（GREEN 段口径修正）
- `frontend/src/__tests__/web-036b-dark-fix.spec.ts`：7 断言反模式 spec（test: 段落先行）

**连带同步（082 建议①提前落地）**：web-011b 由硬编码 `308 passed (308)` 改为 `^      Tests  [0-9]+ passed \([0-9]+\)$`（passed==total 且无 skipped 行）；vitest `|| exit 1` 门禁保留，语义=零失败不降级、计数随用例演进。RED 期实测该断言对 `7 failed | 308 passed (315)` 正确拒绝，证明非放宽。

**案A 在案**：AC3③ 计数基线 11 含死形态 1 条，合法移除后=10，用户裁决下限 ≥10 且死形态=0（详见「问答在案」）。

**未动**：036c 全部（串联 :deep 42 处、死代码、过期 grep）、011d（any 165）、冒烟 7❌、后端、EP/构建版本。

## 评审意见

- （待 Evaluator）
