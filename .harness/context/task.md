# Sprint 工作单：sprint-103

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-103 |
| 所属模块 | **web** |
| 功能点 ID | **web/045** |
| 功能点名称 | BaseInput clearable 点 × 触发重查（clear 只发 clear 不发 change 暗修） |
| 状态 | **PLANNED** |
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

- [ ] AC1 — RED：新增 `frontend/src/components/__tests__/BaseInput.spec.ts`（4 用例）首跑红——「点 × 后 emit change 恰一次且值 `''`」失败（当前 `emitted('change')`=undefined）；smoke `web/045` 护栏同步首跑 ❌（vitest 非 `4 passed`）。← 用例 `AC1 ← 用例 BaseInput.spec 首跑红+护栏红`
- [ ] AC2 — GREEN：`handleClear` 追加 `emit('change','')`（**恰 1 行**）→ spec 4/4 绿：① change 恰一次=`['']`；② `update:modelValue ['']`+`clear` 双 emit 保留；③ input 保持 focus；④ `clearable=false` 或 `modelValue` 空时无 `.base-input__clear` 按钮（v-if 契约回归）。← 用例 `AC2 ← 用例 BaseInput.spec 4/4 绿`
- [ ] AC3 — 门禁：`npm run test` 全量 100% 通过（基线 41 文件 324/324 → **+1 文件 +4 用例=42 文件 328**，Generator 亲测回填实测数）；`npm run lint` **0E|166W**（±0）；`vue-tsc` **EXIT=0**；`npm run build` **✓**。← 用例 `AC3 ← 用例 四门禁=基线/增量恰+4`
- [ ] AC4 — 冒烟：`bash scripts/smoke.sh web/045` = 1 用例 ✅（G1=定向 vitest `4 passed`；G3=`handleClear` 含 `emit('change','')` 实现锚点）；全量 **79=77✅+2⏭ 0❌ `SMOKE PASSED（79 用例）`**。← 用例 `AC4 ← 用例 web/045 护栏+全量冒烟 79`

### AC1/AC2 — 组件行为 TDD

**步骤与期望**：先落 spec（RED）→ 1 行 feat（GREEN）→ smoke 护栏随之转 ✅。四页 IndexView **零改动**（方案 A 卖点，消费者 `@change` 接线已就位）。

**用例**：`AC1/AC2 ← 用例 BaseInput.spec 定向红绿`

### AC3/AC4 — 门禁与冒烟

**用例**：`AC3/AC4 ← 用例 四门禁+web/045 护栏`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | BaseInput.spec 首跑红（change 恒 0）+ smoke web/045 首跑 ❌ | AC1 | 待红 |
| 2 | spec 4/4 绿（change=['']+双 emit 保留+focus+按钮 v-if 契约） | AC2 | 待绿 |
| 3 | test 42 文件 328 全绿 / lint 0E\|166W / vue-tsc 0 / build ✓ | AC3 | 待亲测 |
| 4 | `smoke.sh web/045`=1 ✅；全量 79=77✅+2⏭ 0❌ | AC4 | 待亲测 |

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
| `npm run test` | 42 文件 328/328（+1 文件 +4） | 待填 | 待验 |
| `npm run lint` | 0E\|166W（±0） | 待填 | 待验 |
| `vue-tsc` / `build` | EXIT=0 / ✓ | 待填 | 待验 |
| `bash scripts/smoke.sh` | 79=77✅+2⏭ 0❌ `SMOKE PASSED（79 用例）` | 待填 | 待验 |
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

## 评审意见

（待 Evaluator 评审）
