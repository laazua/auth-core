# Sprint 工作单：sprint-084

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-084 |
| 所属模块 | web |
| 功能点 ID | **web/036b**（web/036 行子功能点；036a ✅ sprint-067、036c 预留不动） |
| 功能点名称 | 暗色模式局部白底缺陷修复——BaseTable 遮罩死选择器 + settings 非法 variant prop + 反模式全仓清零 |
| 状态 | PLANNED |
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

- [ ] AC1 — BaseTable 遮罩暗色覆盖可命中：修复后 `BaseTable.vue` 不再含 `[data-theme='dark'] &` 反模式；构建产物存在**可命中**的暗色遮罩规则（选择器含 `[data-theme=dark]` 且**不存在** `[data-theme=dark][data-v-` 同元素复合形态）且亮色遮罩规则（`ffffffe6` 或等效 rgba 0.9）仍在。← 用例 `AC1 ← 用例 web-036b mask spec（源断言）+ 产物断言（build 后 grep）`
- [ ] AC2 — settings 非法 variant 清零：两处按钮改 `<BaseButton variant="primary">`（前往修改）/`<BaseButton variant="danger">`（退出登录），点击行为 `router.push('/profile/password')`/`handleLogout` 零变；全仓 `<ElButton[^>]*variant=` = 0；settings 的 ElButton import 移除。← 用例 `AC2 ← 用例 web-036b settings spec（源断言）`
- [ ] AC3 — 反模式清零与暗色面不回归：①`[data-theme='dark'] &` 全仓 = 0；②非 spec 的 .vue 中硬编码 `rgba(255, 255, 255` 浅底 = 0；③产物 `[data-theme=dark]` 规则数 ≥ 11（2026-10-09 基线实测）且 036a 三断言保持——`.dark{…--el-` EP 暗段、`--el-bg-color:#fff` 亮值、`--color-bg-page:#020617` 暗段。← 用例 `AC3 ← 用例 spec 全仓清查 + build 产物三断言`
- [ ] AC4 — 门禁五线与冒烟 65：`mvn -q verify` 154 用例 4F+2E = 基线五类；`npm run test` **0 failed**（基线 308 + 本单新增，新增数入册）；`npm run build` ✓；`bash scripts/smoke.sh` 存量 64 不减 + 新增 `web-036b` → **65**（7❌ 基线不背锅），且 `web-011b` 用例同步为计数模式断言后 ✅。← 用例 `AC4 ← 用例 门禁五线实测 + 冒烟 65`

### AC1 — 遮罩暗色覆盖可命中
> Given BaseTable 暗色遮罩规则编译为同元素双属性死形态。When 修复选择器形态并构建。Then 产物存在可命中暗色 mask 规则、死形态归零、亮色规则不回归。
**用例**：`AC1 ← 用例 web-036b mask spec + 产物断言`

### AC2 — settings 非法 variant 清零
> Given ElButton 无 variant prop、意图样式从未生效。When 按 14:1 惯例改用 BaseButton 并清理 import。Then variant 语义合法生效、行为零变、全仓非法 prop 归零。
**用例**：`AC2 ← 用例 web-036b settings spec`

### AC3 — 反模式清零与暗色面不回归
> Given 反模式恰 3 处。When 全部清零且构建。Then 源层清查=0、产物暗色规则 ≥11、036a 三断言保持。
**用例**：`AC3 ← 用例 spec 全仓清查 + build 三断言`

### AC4 — 门禁五线与冒烟 65
> Given 基线 mvn 154 4F+2E、test 308、冒烟 64。When 本单交付。Then 五线=基线/达标、冒烟 65 且 web-011b 计数断言同步。
**用例**：`AC4 ← 用例 门禁五线实测`

## 测试清单

> TDD 载体：**源级断言 + 产物断言型缺陷修复**——RED 三实证先行（先于任何修复动作）：① 反模式 spec 先写（源断言修复前必红）；② 产物形态断言先写（build 后死形态在、可命中形态缺 → 红）；③ smoke `web-036b` 用例先写（修复前必失败）。修复手段不得触碰断言语义；web-011b 计数断言同步须与新增测试同波落位并登记。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | `web-036b` 反模式 spec（死模式/非法 prop/硬编码浅底 全仓清查断言） | AC1/AC2/AC3 | （待 Generator） |
| 2 | 产物形态断言（死形态 `[data-theme=dark][data-v-` 归零 + 可命中暗 mask 规则存在 + 036a 三断言） | AC1/AC3 | （待 Generator） |
| 3 | smoke `web-036b` 暗色遮罩与按钮语义修复（先写后绿）+ `web-011b` 计数断言语义同步 | AC1/AC2/AC4 | （待 Generator） |
| 4 | 门禁五线：mvn=基线 / test 0 failed（308+新增）/ build ✓ / 冒烟 65 | AC4 | （待 Generator） |

## RED 证据

> Generator 于任何修复动作前执行测试清单并粘贴实证；产物形态基线快照（修复前 build）必须先存。

```text
[RED-1] 反模式 spec 首跑（待 Generator——源断言对 3 处现状必红，摘录失败断言）
[RED-2] 产物形态基线（待 Generator——修复前 build：死形态存在/可命中形态缺失/暗规则计数=11 实录）
[RED-3] smoke web-036b 用例体首跑（待 Generator——修复前必失败实录）
```

> 治理后复跑。

```text
[GREEN] （待填——spec 全绿、产物双断言过、test 0 failed|308+新增、冒烟 65）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-09 sprint-083 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（五类，veto-6 豁免） | （待填） | （待填） |
| `npm run lint` / `lint:check` | **EXIT=0，0 errors / 165 warnings**（083 治后） | （待填） | （待填） |
| `npm run test` | **0 failed \| 308 passed**（新增后=308+N） | （待填） | （待填） |
| `npm run build` | ✓（~18s） | （待填） | （待填） |
| `bash scripts/smoke.sh` | **64=55✅+7❌+2⏭️**（7❌ 基线） | （待填：预期 65=56✅+7❌+2⏭️） | （待填） |

## 拆分说明

预估验收标准 **4 条（=上限）**；预估文件变更 **≈4-5**（`BaseTable.vue` 1 + `settings/IndexView.vue` 1 + spec 1 + `scripts/smoke.sh` 1，文档回写 4 不计入）——**≤6 文件不拆分**。前置依赖全 ✅（036a/001/003/008/016）。

**研究项（Generator 先于动手核实）**：
- **R1 遮罩修复形态判定**：三选一——① scoped 内改祖先形态（如 `html[data-theme='dark'] &` 或 `:global` 等价）；② 覆盖下沉至 reset.css 全局层（允许 reset.css **仅遮罩行**最小增改，其余不动）；③ 改用 `var(--el-mask-color)` 暗变量跟随 EP 双轨。判定标准=**产物可命中形态存在 + 死形态归零 + 亮色遮罩不回归**；禁止删除遮罩样式或降级 `!important` 语义导致亮色回归。
- **R2 组件体系判定**：settings 换 BaseButton 的最小 diff（模板 2 处 + import 调整），断言 variant 归属与点击行为（router.push/handleLogout）零变；确认 BaseButton 在暗色下的样式覆盖（element-plus.scss/global 层已有 base-button 规则则只验不改）。
- **R3 产物断言口径**：死形态 grep（`\[data-theme=dark\]\[data-v-`）必须无命中；可命中形态 grep（`\[data-theme=dark\][^{}]*\.el-loading-mask`）必须命中；暗规则计数 ≥11 为辅断言（修复重写形态时计数可 +1，以形态为主）。

**超限熔断**：实际代码文件 >6、或需改 EP/构建版本、或修复中出现删除遮罩/删除既有全局暗规则等行为回退、或反模式清查发现第 4 处未知缺陷面 → **停止回报，回 Planner 重切分**。

**不动清单**：后端全部；串联 `:deep()` 42 处（036c）；冒烟 7❌（036c/后续）；any 165（011d）；`--max-warnings` 与版本升级（§1）；`reset.css`/`variables.scss`/`element-plus.scss` 除 R1 判定的遮罩行外零改动；`web-011b` 用例**仅计数断言语义同步、不删除、不放宽为无效断言**；036c 其余（死代码、过期 grep）。

## 交付物（≈4-5 文件）

1. `BaseTable.vue` 遮罩选择器修复（R1 判定形态）
2. `settings/IndexView.vue` 两处按钮 → BaseButton（R2）
3. `web-036b` 反模式/产物断言 spec（源断言全仓清查 + 产物双断言）
4. `scripts/smoke.sh` + `web-036b` 用例（先写）+ `web-011b` 计数断言语义同步
5. 文档回写：task/session/iteration/registry 四件

## 实现说明

（待 Generator）

## 评审意见

- （待 Evaluator）
