# Sprint 工作单：sprint-103

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-103 |
| 所属模块 | **web** |
| 功能点 ID | **web/045** |
| 功能点名称 | BaseInput clearable 点 × 触发重查（clear 只发 clear 不发 change 暗修） |
| 状态 | **DONE** |
| 创建时间 | 2026-10-16 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/044 | 四管理页搜索 OR 修复（本单是其范围外登记项的清偿） | ✅ |
| web/053 | smoke FILTER 参数（本单护栏可单跑 `smoke.sh web/045`） | ✅ |
| web/054 | 冒烟输出收敛（本单门禁取证=157 行级干净输出） | ✅ |

## 业务背景

**根因（Planner 2026-10-16 只读实测）**：`BaseInput.handleClear`（BaseInput.vue:79-83）点击清除按钮时只 `emit('update:modelValue','')`+`emit('clear')`，**不 emit `change`**。四管理页搜索框（users/IndexView:289、roles/IndexView:263、system/IndexView:535、system/IndexView:619）全部 `clearable`+`@change="handleSearch…"` 接线——点 × 后 v-model 清空但不触发重查，**列表保持旧筛选结果（stale）**，用户须再敲一键才刷新。sprint-089（web/044）排查时登记为范围外项保留候选。

**爆炸半径实锤（全仓 grep）**：BaseInput 的 `@change` 消费方**恰为上述 4 处搜索框**——settings:71 的 `@change="toggleTheme"`=ElSwitch、其余 `@change`=BaseSelect，均与本组件无关；29 处 `<BaseInput` 模板消费中仅这 4 处带 `@change`。**方案 A（组件级 1 行）即可修 4 页，无任何副作用面**。

**方案对比**：A=组件 `handleClear` 追加 `emit('change','')`（1 行，`change` 语义=「值已变更」，清空即是变更，原生 input 的 change 亦在值变化后触发，语义自洽）；B=四页各接 `@clear="handleSearch"`（8 处重复、未来新消费方易漏）。**取 A**。

**TDD 载体**：当前**无 BaseInput spec**（`components/__tests__/` 仅 base-select/breadcrumb/Header）→ 新建 `BaseInput.spec.ts` 4 用例；smoke 追加 `web/045` 护栏（vitest 定向跑+实现锚点），78→79。

## 验收标准（TDD 驱动）

- [x] AC1 — RED：新增 `frontend/src/components/__tests__/BaseInput.spec.ts`（4 用例）首跑红——「点 × 后 emit change 恰一次且值 `''`」失败（当前 `emitted('change')`=undefined）；smoke `web/045` 护栏同步首跑 ❌（vitest 非 `4 passed`）。← 用例 `AC1 ← 用例 BaseInput.spec 首跑红+护栏红`
- [x] AC2 — GREEN：`handleClear` 追加 `emit('change','')`（**恰 1 行**）→ spec 4/4 绿：① change 恰一次=`['']`；② `update:modelValue ['']`+`clear` 双 emit 保留；③ input 保持 focus；④ `clearable=false` 或 `modelValue` 空时无 `.base-input__clear` 按钮（v-if 契约回归）。← 用例 `AC2 ← 用例 BaseInput.spec 4/4 绿`
- [x] AC3 — 门禁：`npm run test` 全量 100% 通过（基线 41 文件 324/324 → **+1 文件 +4 用例=42 文件 328**，Generator 亲测回填实测数）；`npm run lint` **0E|166W**（±0）；`vue-tsc` **EXIT=0**；`npm run build` **✓**。← 用例 `AC3 ← 用例 四门禁=基线/增量恰+4`
- [x] AC4 — 冒烟：`bash scripts/smoke.sh web/045` = 1 用例 ✅（G1=定向 vitest `4 passed`；G3=`handleClear` 含 `emit('change','')` 实现锚点）；全量 **79=77✅+2⏭ 0❌ `SMOKE PASSED（79 用例）`**。← 用例 `AC4 ← 用例 web/045 护栏+全量冒烟 79`

### AC1/AC2 — 组件行为 TDD

**步骤与期望**：先落 spec（RED）→ 1 行 feat（GREEN）→ smoke 护栏随之转 ✅。四页 IndexView **零改动**（方案 A 卖点，消费者 `@change` 接线已就位）。

**用例**：`AC1/AC2 ← 用例 BaseInput.spec 定向红绿`

### AC3/AC4 — 门禁与冒烟

**用例**：`AC3/AC4 ← 用例 四门禁+web/045 护栏`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | BaseInput.spec 首跑红（change 恒 0）+ smoke web/045 首跑 ❌ | AC1 | **✅ 红**：spec 1 failed\|3 passed（`emitted('change')` expected `[['']]` got undefined）+护栏 ❌ EXIT=1 |
| 2 | spec 4/4 绿（change=['']+双 emit 保留+focus+按钮 v-if 契约） | AC2 | **✅ 绿**：定向 4 passed；护栏 ✅（G1+G3）；实现=handleClear 恰 +1 行 |
| 3 | test 42 文件 328 全绿 / lint 0E\|166W / vue-tsc 0 / build ✓ | AC3 | **✅**：test 42 文件 **328/328**（恰 +1 文件 +4 用例）；lint **0E\|166W**（±0）；build ✓ 16.69s（内含 vue-tsc EXIT=0） |
| 4 | `smoke.sh web/045`=1 ✅；全量 79=77✅+2⏭ 0❌ | AC4 | **✅**：web/045=1 用例 ✅ EXIT=0；全量 `SMOKE PASSED（79 用例）` 77/0/2 |

## RED 证据

```text
[RED 2026-10-16] spec 定向首跑：Tests 1 failed | 3 passed (4)
  ❯ BaseInput.spec.ts:18 expect(wrapper.emitted('change')).toEqual([['']])
    - Expected [['']]; + Received undefined  —— handleClear 不 emit change（行为红，可复现）
[RED-护栏] bash scripts/smoke.sh web/045 → ❌ 失败: web/045（G1 非 4 passed）EXIT=1
```

## 门禁与冒烟记录

> Generator 亲测填写。基线（sprint-102 后）：冒烟 **78=76✅+2⏭ 0❌**（本单后预期 79=77✅+2⏭）；前端 test 41 文件 324/324、lint 0E|166W、build ✓~17s、vue-tsc EXIT=0；后端零动。

| 门禁项 | 本单后预期 | 实测 | 结论 |
|--------|---------|------|------|
| `npm run test` | 42 文件 328/328（+1 文件 +4） | 42 文件 328/328 | ✅ 恰+4 |
| `npm run lint` | 0E\|166W（±0） | 0E\|166W | ✅ 持平 |
| `vue-tsc` / `build` | EXIT=0 / ✓ | build ✓ 16.69s（vue-tsc 内含 EXIT=0） | ✅ |
| `bash scripts/smoke.sh` | 79=77✅+2⏭ 0❌ `SMOKE PASSED（79 用例）` | `SMOKE PASSED（79 用例）` 77/0/2 | ✅ |
| 后端 mvn | 零动（不跑） | — | 零动 |

## 实现说明

**实现要点**：
1. spec（test 段）：`mount(BaseInput, {props:{modelValue:'abc', clearable:true}})` → `find('.base-input__clear').trigger('click')` → 断言 `wrapper.emitted('change')`=`[['']]`（当前=undefined 红）；回归三断言（update:modelValue+clear 双 emit、`document.activeElement`=input、非 clearable/空值时按钮不存在）。
2. smoke（test 段，护栏先行）：用例区唯一闭合分隔线前追加 `web/045`——G1 `npm run test -- --run src/components/__tests__/BaseInput.spec.ts 2>&1 | grep -q '4 passed'`；G3 `grep -q "emit('change', '')" .../BaseInput.vue`。**双引号内 `''` 无需转义，但 G3 的 `emit('change', '')` 外层双引号内含单引号直接可用；`$` 前缀变量无**。
3. feat 段：`handleClear` 在 `emit('clear')` 后追加 `emit('change', '')` 恰 1 行（focus 行保持不动）。
4. 四页 IndexView、BaseSelect、其余 BaseInput 逻辑（handleChange/togglePassword/模板 v-if）**零改**。

**超限熔断**：改动 >3 文件、动四页 IndexView、动 BaseInput 非 handleClear 逻辑、改既有 spec/用例、spec 用例数 ≠4 → 停止回报。

**不动清单**：四页 IndexView 全部；BaseSelect；BaseInput 除 handleClear 外全部；既有 4 个 spec 文件；用例区既有 78 条（仅追加 1 条）；SMOKE APP_LOG/内部日志/FILTER 语义/尾部汇总格式；后端全部；`application.yml.template` D 态（永不 add）。

## 交付物（3 业务文件）

1. `frontend/src/components/__tests__/BaseInput.spec.ts` — **新增**（4 用例，TDD 载体）
2. `scripts/smoke.sh` — 用例区追加 `web/045` 护栏（78→79）
3. `frontend/src/components/BaseInput.vue` — handleClear 追加 1 行

### 新增
- `frontend/src/components/__tests__/BaseInput.spec.ts`

### 修改
- `scripts/smoke.sh`
- `frontend/src/components/BaseInput.vue`

### 删除
（无）

### 变更计数

业务文件 **3**；smoke 用例 78→**79**；BaseInput 净 +1 行；四页消费者零改。

## 变更清单（Generator 交付）

### 新增
- `frontend/src/components/__tests__/BaseInput.spec.ts` — 4 用例：点 × emit change=['']（主断言）/双 emit 契约保留/input 保持焦点/按钮 v-if 回归

### 修改
- `scripts/smoke.sh`:680-687 — 用例区追加 `web/045` 护栏（G1 定向 vitest 4 passed+G3 实现锚点），78→79
- `frontend/src/components/BaseInput.vue`:83 — handleClear 追加 `emit('change','')` 恰 1 行（清除即值变更）

### 删除
（无）

## 评审意见

**sprint-103 评审通过 — 均分 9.7/10**（2026-10-16 Evaluator）

| 维度 | 分 | 依据 |
|------|----|------|
| 功能正确性 | 10 | 4 AC 亲核全满足：AC1 spec 首跑红实录（1 failed\|3 passed，`emitted('change')`=undefined）+护栏 ❌；AC2 定向复跑 4/4+护栏 ✅+实现恰 +1 行；AC3 亲测 test 42 文件 328/328（恰+4）、lint 0E\|166W、build ✓17.65s；AC4 全量 `SMOKE PASSED（79 用例）` 77/0/2 含 web/045 ✅ |
| 代码质量 | 9.5 | diff 实测 BaseInput 恰 +1 行（80-83 hunk）、方案 A 最小无过度设计、spec AAA 结构断言有效非凑数；扣分=smoke G3 静态锚点与 G1 行为断言语义重复（G1 绿已蕴含实现锚点，-0.5） |
| 规范遵守 | 9.5 | script setup/emits 显式/scoped 未动、spec 命名对齐 Header.spec 先例、工作区洁净（lint --fix 零副作用）；扣分=变更清单 smoke.sh 行号记 680-687 实为 **679-685**（+1 偏差，行号须真实可查，-0.5） |
| TDD 执行度 | 9.5 | RED 链完整：提交序 `ff34ec1 test`→`a88516d feat`、静态取证（test 段时点 BaseInput emit change=0 处、spec 主断言=1 处）、task RED 实录实时；扣分=4 用例仅主断言驱动红、3 例首跑即绿（回归用例未受 RED 检验驱动力打折，-0.5） |
| 安全性 | 10 | 纯组件事件面五查零命中：无越权/无 SQL/无明文凭据/无密钥/无日志改动（diff 仅事件 emit+spec+smoke 用例） |

**六一票否决**：零命中（AC 全满足非功能缺失、无安全面、无 Bug、RED 证据链在案、无 RBAC 偏差、门禁冒烟全过+恰增 1 用例）。

**扣分项（-1.5）**：① 变更清单行号 +1 偏差（-0.5）；② G3 冗余护栏（-0.5）；③ 仅 1/4 用例驱动 RED（-0.5）。

**结论**：PASS。web/045 收口——四管理页搜索框点 × 即重查，sprint-089 登记范围外项清偿；组件级方案 A 以 1 行修 4 页、四页零改。后续候选：web/036c3（>6 须先只读实测再问案B）、挂起区小项（barrel 整体移除探明/mvn 计数口径/护栏 G2 反模式/失败日志清理/用例名口径 grep 核对）。
