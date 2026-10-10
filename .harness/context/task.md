# Sprint 工作单：sprint-086

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-086 |
| 所属模块 | web |
| 功能点 ID | **web/041**（新注册） |
| 功能点名称 | 修复用户/角色/权限/模块四管理页工具栏筛选下拉框点击无法展开——BaseSelect click-outside 同事件竞态 |
| 状态 | AWAITING_REVIEW |
| 创建时间 | 2026-10-10 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | 前端骨架（BaseSelect 所属组件层） | ✅ sprint-023 |
| web/004 | 用户管理页（报障页 1/4） | ✅ sprint-026 |
| web/005 | 角色管理页（报障页 2/4） | ✅ sprint-027 |
| web/006 | 权限管理页+模块管理页（报障页 3、4/4） | ✅ sprint-028 |
| web/011a/b/c | 前端门禁基线（build ✓、test 0 failed、lint 0E/165W） | ✅ sprint-082/083 |
| sprint-085 | 门禁基线（lint 0E\|165W、test 315、冒烟 66=60✅+4❌+2⏭️、mvn 154 4F+2E） | ✅ PASS 9.5/10 |

## 业务背景

**用户需求（2026-10-10）**：「前端 用户管理，角色管理，权限管理，模块管理 这几个路由页面的全部状态下拉框，不能下拉；按照 harness 系统要求，以 planner 开始依次执行」。

### 报障面（四路由，只读实证）

| 路由 | 页面文件 | 工具栏下拉 |
|------|---------|-----------|
| /system/users | `views/users/IndexView.vue:293` | 状态筛选（statusOptions，placeholder=全部状态） |
| /system/roles | `views/roles/IndexView.vue:267` | 状态筛选（statusOptions，placeholder=全部状态） |
| /system/permissions | `views/system/IndexView.vue:533` | 模块筛选（moduleOptions，placeholder=全部模块） |
| /system/modules | `views/system/IndexView.vue:617` | 状态筛选（statusOptions，placeholder=全部状态） |

> 口径登记（R4）：用户原话「全部状态下拉框」按**报障面整体**理解——四页工具栏筛选下拉（权限页为模块筛选）一并覆盖，均为同一 `BaseSelect` 组件实例。

### 根因（Phase 1-3 完成，Chromium 真实鼠标事件四采样点实录，2026-10-10 Planner 只读实测）

四页 toolbar 下拉**点击后 `base-select--open=false`、dropdown 不渲染**（探针 repro/probe3）；抽屉表单内同组件对照**可正常展开**。

事件传播采样（probe4，LIST 用户管理页）：

```text
A-root-cap   open=false dd=false   （传播起点）
B-root-bub   open=true  dd=true    ← Vue @click(handleClick) 置 isOpen=true，listener 边界
                                   checkpoint 已 flush 渲染，dropdownRef 就位
C-body-bub   open=true  dd=true    （尚未被关）
D-doc-bub    open=false dd=false   ← document 的 handleClickOutside 执行：
                                   dropdownRef 存在且 event.target(=trigger) 不在 dropdown 内
                                   → 判定「点外面」→ isOpen=false，开→关自相矛盾
```

**根因机理**：HTML 规范要求每个事件监听器回调结束后执行 microtask checkpoint——根 div 的 `handleClick`（BaseSelect.vue:79）把 `isOpen=true` 后，Vue flushJobs 在同一 click 事件传播到 document **之前**完成渲染；随后 `handleClickOutside`（BaseSelect.vue:134-138）用 `dropdownRef.contains(target)` 判定 outside，而 target 是 trigger（组件内部、dropdown 之外）→ 把刚打开的下拉立即关闭。**四页在普通 DOM 链上（事件直达 document）→ 全部打不开**。

**抽屉对照为何可用**（同探针 DRAWER 段）：抽屉场景 click 冒泡**未达 document**（seq 只有 A、B，C/D 未触发——EP drawer 容器层传播中断）→ outside handler 未执行 → 侥幸正常。同一缺陷、被传播中断掩盖。

**测试盲区**：四页面 spec 均 mock BaseSelect（`base-select-mock`，users:45/roles:45/system:45/UserFormDrawer:45），组件本体无专测 → 全量 315 绿但浏览器坏。

**jsdom 复现边界（Planner 临时探针实测，跑完即删）**：jsdom 下 `dispatchEvent(click bubbles:true)` 完整传播**不复现**竞态（`open=true dd=true` 通过）——jsdom 的 listener 间 checkpoint 与 Chromium 行为差异。**结论：AC1 的 RED 不能靠「jsdom 完整传播」直接复现**，须采用与真实缺陷同构的时序测试（渲染后重放 document outside 调用），见研究项 R1。

### 架构冲突

无。纯前端组件交互修复：不涉 RBAC 业务语义（§6）、不涉 §1 技术栈变更、不动接口契约与数据模型。无需挂起区冲突登记。

### registry 建议与用户输入的取舍（登记）

session-state 原「下一步动作」建议 web/036c2（主推）/web/011d（备选）——用户本次直接给出新报障需求，按 AGENTS.md 优先级（用户输入 > session-state 建议）规划 **web/041**；036c2/036c3/011d 预留不变，不改其状态。

## 需求描述

修复 `BaseSelect` 组件 click-outside 判断依据，使组件根元素内部（trigger/label/清除按钮/下拉选项）的点击传播到达 document 时不再被误判为「点外面」而关闭下拉：四管理页工具栏下拉点击可正常展开、选中、再次点击关闭，抽屉表单内下拉与外部点击关闭语义不回归。组件级测试先行（RED→GREEN），追加至少 1 条冒烟用例，全量门禁零新增失败。

## 验收标准（TDD 驱动）

- [x] AC1 — 内部点击传播不关闭下拉（竞态修复核心）：挂载真实 BaseSelect（options 2 项，不 mock），打开下拉并 await 渲染使 `.base-select__dropdown` 就位后，**重放同一点击事件传播的后半段**——调用组件注册在 document 上的 click 监听器（`target` = trigger 元素，即组件根内、dropdown 外），断言下拉保持展开（根含 `base-select--open` 且 dropdown 在场）。当前实现该调用会把打开状态关闭 → **RED**。← 用例 `AC1 ← 用例 base-select.spec#组件根内点击传播到达 document 不关闭下拉`；RED 复现与最终断言全文见 RED 槽。
- [x] AC2 — 外部点击关闭语义保留（防过度修复）：同 spec，下拉展开后调用同一 document 监听器（`target` = 组件根外的外部元素），断言下拉关闭（根不含 `base-select--open`、dropdown 卸载）。与 AC1 成对钉死「根内不关、根外关」判定边界。← 用例 `AC2 ← 用例 base-select.spec#组件根外点击关闭下拉`（护栏用例，与 AC1 同批）。
- [x] AC3 — 报障面冒烟覆盖：追加 **≥1 条**冒烟用例 `web-041`（形态 Generator 按 tdd-workflow 冒烟节定，建议：定向跑组件 spec 断言通过 + 源码级断言四页面 toolbar 均挂 BaseSelect 筛选下拉、修复形态在场），首跑 **RED**（修复前必红）→ 修复后复跑 ✅。← 用例 `AC3 ← 用例 smoke web-041（首跑 RED EXIT=1 → GREEN EXIT=0）`。
- [x] AC4 — 门禁与冒烟总账：`npm run lint && npm run test && npm run build` 零新增（test **0 failed**，基线 315+本单新增；lint **0 errors**/165 warnings 基线；build ✓）；`mvn -q verify` **154 用例 4F+2E=基线五类**（本单零后端改动）；`bash scripts/smoke.sh` 总数 **≥67**（66+新增）、**❌ 恰为 4 条**（model-008、model-010、roles-001、roles-002=登记不背面）、零新增失败。← 用例 `AC4 ← 用例 门禁五线实测 + 冒烟全量复跑`。

### AC1 — 组件根内点击传播不关闭下拉
> Given 修复前 document outside 监听器在渲染后收到 target=trigger 的 click 会误关下拉（浏览器四采样点实录）。When 判定依据修正（R2）后重放该调用。Then 下拉保持展开。
**用例**：`AC1 ← 用例 base-select.spec#组件根内点击传播到达 document 不关闭下拉`

### AC2 — 组件根外点击仍关闭
> Given outside 关闭语义须保留。When target 为组件根外元素时调用同一监听器。Then 下拉关闭。
**用例**：`AC2 ← 用例 base-select.spec#组件根外点击关闭下拉`

### AC3 — 报障面冒烟追加
> Given 冒烟增量约定强制。When 追加 web-041 覆盖修复形态与四页引用。Then 首跑红、复跑绿。
**用例**：`AC3 ← 用例 smoke web-041`

### AC4 — 门禁与冒烟总账
> Given 基线对照推进口径。When 五线实测。Then 零新增失败、冒烟 ❌ 恰 4。
**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

> TDD 载体：**组件级新 spec 先于修复写出**（AC1 当前实现必红=天然 RED）；AC2 同批护栏；AC3 冒烟用例首跑红；AC4 基线对照。注意 jsdom 完整传播不复现竞态（业务背景实证）——AC1 须按 R1 同构时序形态构造，禁止以「jsdom 传播绿」冒充修复验证。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | base-select.spec 新增：组件根内点击传播到达 document 不关闭下拉（渲染后重放 outside 监听器，target=trigger） | AC1 | ✅ RED-1（修复前 1 failed，见下）→ GREEN 后通过 |
| 2 | base-select.spec 新增：组件根外点击关闭下拉（target=外部元素） | AC2 | ✅ RED-1 时已通过（护栏基线），GREEN 后保持通过 |
| 3 | smoke web-041 首跑（修复形态缺失/报障面断言不中 → RED EXIT=1） | AC3 | ✅ RED-2 EXIT=1（stash 复现）→ 恢复后 EXIT=0 |
| 4 | 门禁五线 + 冒烟全量（≥67、❌恰4、零新增） | AC4 | ✅ 全量实测达基线（见门禁与冒烟记录） |

## RED 证据

> Generator 于任何修改动作前执行测试清单并粘贴实证（AC1/AC2 spec 与 AC3 冒烟首跑日志、修复前失败输出）。

```text
[RED-1] base-select.spec 首跑（2026-10-10 实时，修复前）：
 Test Files  1 failed (1)
      Tests  1 failed | 1 passed (2)
 FAIL  src/components/__tests__/base-select.spec.ts > BaseSelect - click-outside 判定边界 > 组件根内点击传播到达document后下拉保持展开
 AssertionError: expected [ 'base-select' ] to include 'base-select--open'
  ❯ src/components/__tests__/base-select.spec.ts:64:36
   62|     await flushPromises();
   63|
   64|     expect(wrapperLocal.classes()).toContain('base-select--open');
解读：重放 document outside 监听器（target=trigger，根内 dropdown 外）后打开状态被关闭
（classes 只剩 ['base-select']）——与 Chromium 四采样点 D（doc-bub open=false）同一缺陷语义；
AC2（根外点击关闭）当前通过 = 护栏基线成立。
[RED-2] smoke web-041 用例体首跑（2026-10-10 实时，git stash 暂存 BaseSelect.vue 修复=修复前基线）：
EXIT=1（Test Files 1 failed、Tests 1 failed | 1 passed）——用例体首条
`npx vitest run src/components/__tests__/base-select.spec.ts` 对修复前代码必红（即 AC1 失败），
四页 BaseSelect 引用 grep 两条在场为常绿前提；恢复 stash 后复跑转绿（GREEN 槽在案）。
```

```text
[GREEN]（2026-10-10 实时）
[GREEN-1] 修复 BaseSelect.vue 后 spec 复跑：
      Tests  2 passed (2)——AC1 重放 target=trigger 保持 base-select--open、AC2 根外点击关闭；
      判定变化：重放 document outside 监听器后 classes 含 'base-select--open'（修复前仅 ['base-select']）。
[GREEN-2] 冒烟 web-041 用例体（恢复 stash=修复在场）：EXIT=0，
      Tests  2 passed (2)、four-pages-grep-OK（users/roles/system 三 IndexView 均引用 BaseSelect）。
[GREEN-3] R3 浏览器实测复验（headless Chromium 真实点击）：修复后
      /system/users、/system/roles、/system/permissions、/system/modules 四页
      工具栏下拉点击均 open=true, hasDropdown=true；抽屉表单下拉开关对照正常。
（修复前 Chromium 四采样点 D「doc-bub open=false」竞态语义 = RED-1 重放形态，同构对照成立）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-09 sprint-085 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（五类，veto-6 豁免） | 154 用例 4F+2E（DataSourceConfigBindingTest 2F、RoleControllerTest 1F、SeedDataIntegrationTest 1F、TestLayersSpec 1E、TestUtilsSpec 1E） | ✅ 与基线五类逐一相符，零新增 |
| `npm run lint` | EXIT=0，0 errors / 165 warnings | EXIT=0，0 errors / 165 warnings | ✅ 等于基线 |
| `npm run test` | 0 failed \| 315 passed (315)+本单新增 | 0 failed \| 317 passed (317)，Test Files 38 passed | ✅ 315+2（AC1/AC2）=317 |
| `npm run build` | ✓（~18s） | ✓ built in 18.02s | ✅ 等于基线 |
| `bash scripts/smoke.sh` | 66=60✅+4❌+2⏭️ | **67=61✅+4❌+2⏭️**，❌恰=model-008/010、roles-001/002（零新增），web-041 ✅ 通过；汇总行 `SMOKE FAILED（67 用例）` 系 4 条登记 ❌ 计数所致（基线同口径亦为 FAILED 字样，通过判据=用例数≥67、❌恰4 零新增、新增用例 ✅） | ✅ 达基线口径 |

## 拆分说明

预估验收标准 **4 条（=上限）**；预估文件变更 **3**（`components/BaseSelect.vue` 1 + `components/__tests__/base-select.spec.ts` 新增 1 + `scripts/smoke.sh` 1；文档回写 4 不计入）——**≤6 文件不拆分**。

**研究项（Generator 先于动手核实）**：

- **R1 RED 复现形态（关键）**：jsdom 完整传播不复现竞态（Planner 实测）→ AC1 采用同构时序：挂载 → 打开（root click）→ `await` 渲染使 dropdown 就位 → 重放 document outside 监听器（`target`=trigger）。监听器捕获方式建议 `vi.spyOn(document, 'addEventListener')` 挂载前截获，或等效手段。**RED 判据**=修复前该重放调用使下拉关闭（断言失败）；若 Generator 找到更强等效形态可替换，但须同时给出：①修复前红/修复后绿证据；②与 Chromium 四采样点缺陷同一语义的对照说明。
- **R2 修复方案（推荐，Generator 可论证替代）**：`handleClickOutside` 判定依据由 `dropdownRef.contains(target)` 改为**组件根元素包含性**（加 rootRef，`!rootRef.contains(target)` 才视为 outside）。收益：根内（trigger/label/clear/dropdown 选项）一律不误关、彻底消除「渲染时机竞态」；多选点选项保持展开、单选由 handleSelect 自关、clear 由 handleClear 自关——语义均不变。备选 `handleClick` 中 `event.stopPropagation()` **不推荐**（污染其他 document 级监听、需改 handler 签名）。实现后自查路径：打开/选中/清空/再次点击关闭/键盘 Escape/filterable 搜索、抽屉表单场景。
- **R3 浏览器实测复验（可选增强）**：修复后可按 Planner 同法复验（本机 headless Chromium 真实点击四页 + 抽屉对照，playwright-core 在 npx 缓存、chromium 在 ms-playwright 缓存）——**严禁将 playwright/依赖写入项目**（package.json 不动）；或移交 Evaluator 按同法亲测。
- **R4 报障面口径**：权限页 toolbar 为模块筛选（全部模块），其余三页为状态筛选——AC3 断言覆盖四页 toolbar 下拉整体，不按字面只测「状态」。

**超限熔断**：实际文件 >6、或需动后端（.java/yml）、或修复引发四页面/抽屉既有 spec 连锁失败需改他案断言语义、或发现第 2 类独立缺陷面 → **停止回报，回 Planner 重切分**。

**不动清单**：四页面与抽屉 spec 的 `base-select-mock`（mock 语义不动）；后端全部；冒烟既有用例断言语义与 ❌ 四条（model-008/010、roles-001/002）不修改不删除；web/036c2/036c3、web/011d 预留；any 165；**工作树预存改动 `backend/src/main/resources/application.yml.template` 删除 + `application.template.yml` 新增（非本单产生，不混入本单提交，提交时按范围精确 stage）**。

## 交付物（≈3 文件）

1. `frontend/src/components/BaseSelect.vue`：click-outside 判定修正（R2）
2. `frontend/src/components/__tests__/base-select.spec.ts`：新增组件 spec（AC1+AC2）
3. `scripts/smoke.sh`：新增冒烟用例 web-041（AC3）
4. 文档回写：task/session/iteration/registry 四件

## 实现说明

**实现要点（R2 落地，3 文件）**：
- `frontend/src/components/BaseSelect.vue`：① 新增 `rootRef`（组件根 div）；② `handleClickOutside` 判定由 `!dropdownRef.contains(target)` 改为 `!rootRef.contains(target)`——根内（trigger/label/clear/dropdown 选项）一律不误关，彻底消除「根 click 置 isOpen → Vue 渲染 checkpoint → 同 click 冒泡到 document 被误判 outside」竞态；③ 删除死引用 `dropdownRef`（script+模板两处，消除 TS6133）。语义不变：多选点选项保持展开、单选 handleSelect 自关、clear handleClear 自关、根外点击关闭、Escape 关闭。
- `frontend/src/components/__tests__/base-select.spec.ts`（新增）：AC1 同构时序（挂载→root click 打开→flush→重放截获的 document outside 监听器 target=trigger→断言 `base-select--open` 在场）；AC2 根外点击关闭护栏。
- `scripts/smoke.sh`：追加 `web-041 四管理页下拉竞态修复定向测试`（spec 全绿 + four-pages-grep 三 IndexView 引用 BaseSelect）。

**变更清单**：同交付物 3 文件（不含文档回写 4 件）；无后端、无既有 spec 断言改动、无 mock 语义改动；工作树预存 `application.yml.template`/`application.template.yml` 改动已排除在提交外。

## 评审意见

（待 Evaluator 填写）
