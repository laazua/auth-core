# Sprint 工作单：sprint-083

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-083 |
| 所属模块 | web |
| 功能点 ID | **web/011c**（由 web/011 行预留独立转正注册；011 行已 ✅ 约束不改） |
| 功能点名称 | 全量 lint 门禁恢复——dist 忽略解锁卡死 + 全量 Error 4559 清零 |
| 状态 | DONE |
| 创建时间 | 2026-10-08 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/011 | 父行收口（011a build ✓、011b test 0 failed；011c 由其备注预留转正） | ✅ sprint-082 |
| sprint-082 | 门禁基线入册（test 0 failed\|308、冒烟 63、mvn 154 4F+2E、定向 lint 0/0/151/45） | ✅ PASS 9.3/10 |
| 案B 裁决 | **用户 2026-10-08 问答裁定：60 文件超 6 熔断单波豁免**（详见拆分说明与挂起区登记） | ✅ 问答在案 |

## 业务背景

`npm run lint` 自 web/001 起卡死 **EXIT=124 超时 5+ 次复现**（065 登记 300s、080/081/082 评审均实测 124），长期只以定向分片替代门禁，082 案A 将定向预存 196（LoginView 151/IndexView 45）登记归 011c。本单规划只读实测揭示全貌：

1. **卡死根因第一嫌疑**：`.eslintrc.cjs` 无 `ignorePatterns`、无 `.eslintignore` → `eslint .` 扫描 **`dist/` 29 个 1.9M 构建产物**（prettier 插件对 bundle 逐文件重排）；对照实证：`npx eslint src` 240s 内完成 exit=1（≠124），差异面=dist（R1 钉死）。
2. **真实残余远超 196 预估**：src 全量 **4748 问题 / 60 文件**——`prettier/prettier` **4536 Error**（60 文件，单命令可自动修）+ 手工 **23 Error**（约 11 文件：no-unused-vars 8 / no-useless-catch 6 / vue/no-parsing-error 4 / no-useless-escape 2 / no-ref-as-operand 1 / vue/no-dupe-keys 1 / vue/no-deprecated-filter 1）+ any **171 Warning**（散布 23 文件）+ vue/attributes-order 18 Warning（可自动修）。196 只是冰山一角。
3. **门禁语义**：eslint exit 码只由 Error 决定，Warning 不阻断 → 门禁绿 = **Error 清零**（4559±，根级文件增量以首跑实测钉死）；any 171 不在本单（登记 011d 预留）。

**分治登记（本单不背）**：① any **171 Warning** 清零（需 defineExpose 类产品改动，散布 23 文件）→ 预留 **011d**；② 冒烟 7❌ 中 web-013/020/026 过期 grep 归 **036c**、model-008/010+roles-001/002 后端漂移归后续后端单（082 在案）；③ `--max-warnings` 收紧与 eslint/prettier 版本升级不在本单（§1 选型固化）。

## 需求描述

恢复 `npm run lint` 全量门禁至可用并清零全部 Error：①忽略构建产物解除 124 卡死；②4536 prettier 类错误单命令自动修复；③约 23 条手工 Error 逐条修复（真缺陷判定先行）。4559 Error → 0、`lint:check` exit 0；零语义回归以「机械段 `git diff -w` 空 + 手工段逐条登记 + test/build/mvn 三线=基线」三重证明；6 文件熔断经用户案B 裁决豁免。

## 验收标准（TDD 驱动）

- [x] AC1 — 卡死根因消除：治理前 `npm run lint:check`（`eslint .` 口径）全量 **EXIT=124** 实录入 RED 槽；R1 对照实证钉死根因（dist 扫描 vs 其他，含 `eslint .` vs 忽略 dist 后的对照命令输出）；治理后全量在 300s 内完成运行、exit ∈ {0,1}（**≠124**）且输出零 `dist/` 路径。← 用例 `smoke web-011c lint 全量门禁恢复` + 门禁实测
- [x] AC2 — 全量 Error 清零：忽略 dist 后首跑清单入 RED 槽（Error 总数与按文件/规则分布，预期 ≈4559 = 4536 prettier + 23 手工 + 根级增量，以实测钉死）；修复后 `npm run lint:check` **exit 0、Error 0**；Warning 处置口径入册——attrs-order 18 随自动修复清零、any 171 允许残留但逐条登记归 011d。← 用例 `AC2 ← 门禁实测 lint:check exit 0（RED=4559±Error 清单）`
- [x] AC3 — 零语义回归：①机械段（自动修复涉及的约 59 文件）**逐文件证明 `工作树版 == prettier(HEAD 版)`（diff 空）——变更恰为 prettier 规范化、零额外改动**【案A 口径修订 2026-10-08 用户问答在案：prettier 含长行拆分等风格改写，原稿「`git diff -w` 空」字面不可达，升级为 prettier 等价证明（更强，可抦截任何夹带）；风格 token（拆行/逗号/引号）允许】；②手工段文件清单与逐处 diff 理由登记入实现说明（预期 ≈11 文件/23 Error）；③`npm run test` **0 failed | 308 passed** 与 `npm run build` ✓ 均不回归（手工段若涉产品文件，每处须论证行为等价并由对应 spec 守护）。← 用例 `AC3 ← prettier 等价逐文件实测 + 门禁两线`
- [x] AC4 — 门禁五线与冒烟：`mvn -q verify` 154 用例 4F+2E=基线五类逐条；`bash scripts/smoke.sh` 存量 **63 用例不减不改** + 本单追加 `web-011c` → 64（7❌ 属基线不背锅）；行数债登记——`IndexView.vue`(752)/`IndexView.spec`(1496)/`LoginView.spec`(667) 等 >500 行文件经格式化后的行数变化逐条入册。← 用例 `AC4 ← 门禁五线实测 + 冒烟 64`

### AC1 — 卡死解除
> Given 全量 lint:check 124 实录。When R1 判定 dist 根因并施加忽略。Then 完成运行 ≠124 且零 dist 路径。
**用例**：`AC1 ← 用例 smoke web-011c + 门禁实测`

### AC2 — Error 清零
> Given 首跑 4559± Error 清单 RED。When 机械+手工修复完成。Then lint:check exit 0、Warning 口径入册。
**用例**：`AC2 ← 用例 门禁实测 lint:check exit 0`

### AC3 — 零语义回归
> Given 60 文件改动。When 机械段 diff -w 空、手工段逐条登记。Then test 308 绿、build ✓。
**用例**：`AC3 ← 用例 git diff -w + 门禁两线`

### AC4 — 门禁五线与冒烟
> Given 基线 mvn 154 4F+2E、冒烟 63。When 本单交付。Then 五线=基线/达标且冒烟 64（63 存量+web-011c）。
**用例**：`AC4 ← 用例 门禁五线实测`

## 测试清单

> TDD 载体：**门禁型 bugfix**——RED 三实证先行（先于任何治理动作）：① 全量 124 卡死实录；② 忽略 dist 后首跑 Error 清单（4559±按文件/规则分布）；③ smoke `web-011c` 用例先写（RED 态断言完成性预期必失败）。修复手段不得触碰断言语义。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | 全量 `lint:check` 卡死实录（124）与 R1 对照实证 | AC1 | ✓ RED-1=124 + RED-2 对照 <60s 完成（dist=根因钉死） |
| 2 | 忽略 dist 后首跑 Error 清单（4559± 分布入册） | AC2 | ✓ RED-2=4748 问题/Error 4559/60 文件，root 0 增量 |
| 3 | smoke `web-011c` lint 全量门禁恢复（先写后绿） | AC1/AC4 | （待 Generator） |
| 4 | 门禁五线：lint:check exit 0 / test 0 failed\|308 / build ✓ / mvn=基线 / 冒烟 63 存量+新增 | AC2/AC3/AC4 | ✓ 五线全过（lint 链 EXIT=0、test 308/308、build 17.75s、mvn 154 4F+2E=基线、冒烟 64=55✅+7❌+2⏭️） |

## RED 证据

> Generator 于任何治理动作前执行测试清单并粘贴实证；机械段 `--fix` 前必须先存首跑清单快照。

```text
[RED-1] 全量卡死实录（2026-10-08 实时，/tmp/opencode/e083-red1.log）：
$ cd frontend && timeout 300 npm run lint:check
> eslint . --ext .vue,.ts,.js
（300s 内零输出——eslint 静默扫描 dist 29 bundle 直至超时）
RED1_EXIT=124   ← 与 5+ 次历史复现（065 登记 300s、082 评审 124）一致

[RED-2] 忽略 dist 后首跑清单（2026-10-08 实时，--ignore-pattern 'dist/**'，/tmp/opencode/e083-red2.log）：
RED2_EXIT=1（完成运行，耗时 <60s——对照 RED-1 即钉死根因=dist 扫描）
4748 problems（60 文件；root 配置 0 问题——vite/vitest/stylelint.config 零增量）
  Error 4559 = prettier/prettier 4536 + no-unused-vars 8 + no-useless-catch 6
             + vue/no-parsing-error 4 + no-useless-escape 2 + vue/no-ref-as-operand 1
             + vue/no-dupe-keys 1 + vue/no-deprecated-filter 1
  Warning 189 = no-explicit-any 171 + vue/attributes-order 18   ← 不阻断 exit，any 归 011d
按文件 top：system/IndexView.vue 576、LoginView.vue 450、users/IndexView.vue 321、roles/IndexView.vue 308、…、LoginView.spec 151（082 案A 的 196=其中两文件子集）

[RED-3] smoke web-011c 用例体首跑（2026-10-08 实时，用例体原文执行，/tmp/smoke-web-011c-red.log）：
RED3_CASE_EXIT=124 → smoke harness 判 ❌ 失败（lint:check 300s 内零输出即超时，同 RED-1 签名）
用例本体已先行写入 scripts/smoke.sh（web-011c 组，断言 timeout 300 完成 exit 0 且输出零 dist/）
```

> 治理后复跑。

```text
[GREEN] npm run lint:check（2026-10-08 实时，/tmp/opencode/e083-lintcheck2.log、e083-frontend-gates.log）：
EXIT=0 — ✖ 165 problems (0 errors, 165 warnings)   ← Error 4559→0
Warning 口径：165 = any 171 − 6（6 处 catch (error:any) 拆壳连带消除）+ attrs-order 18 波内清零；余者全为 no-explicit-any，登记 011d。
差值链：[RED-1] EXIT=124 → 治理后 <60s 完成；[RED-2] E4559 → [GREEN] E0；[RED-3] 用例体 124 → web-011c ✅。
案A 等价证明（逐文件 `工作树 == eslintFix(HEAD)`）：**50 机械等价 + 8 手工 + 1 配置 = 59 文件，零夹带**；
手工段 delta 仅 117 行（/tmp/opencode/e083-hand-hunks.log + sidebar spec 单行同步复核实录）。
门禁链原文 `npm run lint && npm run test && npm run build` = EXIT 0（308 passed、✓ built 17.75s）。
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-08 sprint-082 评审实测 + 本单规划只读实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（五类，veto-6 豁免） | **154 用例 Failures 4 + Errors 2**，五类逐条=基线（/tmp/opencode/e083-mvn.log） | ✓ =基线零新增 |
| `npm run lint:check` 全量 | **EXIT=124 卡死**（5+ 次复现）；RED-2 dist 忽略首跑 4748 问题/60 文件 | **EXIT=0，0 errors / 165 warnings**（Error 4559→0；RED 对照 e083-red1/red2.log、治后 lintcheck2.log） | ✓ 达标（AC1+AC2） |
| `npm run test` | **0 failed \| 308 passed**（EXIT=0） | **0 failed \| 308 passed (36)** EXIT=0（波后 1 failed→sidebar spec 标记语义同步→复测全绿，e083-test2/gates.log） | ✓ 达标 |
| `npm run build` | ✓（~17s） | ✓ EXIT=0（17.75s，e083-frontend-gates.log） | ✓ 达标 |
| `bash scripts/smoke.sh` | **63=54✅+7❌+2⏭️**（7❌ 基线不背锅） | **64=55✅+7❌+2⏭️**（存量 63 不减不改 + 本单 `web-011c` ✅；7❌=基线逐条 model-008/010、roles-001/002、web-013/020/026；e083-smoke.log） | ✓ 零回归（AC4） |

## 拆分说明

预估验收标准 **4 条（=上限）**；预估文件变更 **≈66**（`.eslintrc.cjs` 1 + 问题文件 60 + `scripts/smoke.sh` 1 + 文档回写 4）——**超 6 文件熔断，用户 2026-10-08 案B 裁决单波豁免（问答在案）**：

1. 豁免依据：机械段（约 59 文件）由单命令产生，`git diff -w` 空 = 零语义客观证明；手工段仅 ≈11 文件/23 Error 小面可审；三线门禁（test/build/mvn）+ 冒烟守回归。
2. 严格拆分（案A）需按目录切 9-11 单纯格式化波，机械性逐单价值极低——用户裁定不采纳。

**超限熔断（豁免边界）**：实际变更 >70 文件、或需改 eslint/prettier 版本、或机械段修复后出现任何非空白 diff、或 23 手工 Error 中 `vue/no-dupe-keys`/`vue/no-parsing-error` 判定为需大改产品行为 → **停止回报，回 Planner 重切分**。

**不动清单**：后端全部；`scripts/smoke.sh` 仅追加 web-011c 一行组；**any 171 Warning 不修**（011d 预留）；冒烟 7❌（036c/后续）；`--max-warnings` 与版本升级（§1 固化）；`dist/` 目录本体不删不改（仅加忽略）。

**研究项（Generator 先于动手核实）**：
- **R1 卡死根因对照实证**：`timeout N npm run lint:check`（治前 124 实录）vs 同命令加 dist 忽略后完成——两份输出入 RED 槽钉死根因；若差异面不是 dist（如根级文件/其他路径）回报。
- **R2 23 手工 Error 修法判定**：逐条判测试侧 vs 产品侧；`vue/no-dupe-keys`（1）与 `vue/no-parsing-error`（4）疑似真缺陷，优先定性——是产品缺陷则最小修复并 spec 守护，超预期面回报。
- **R3 自动修复口径**：`npm run lint`（含 `--fix`，覆盖 prettier+attrs-order 于 .vue/.ts/.js）为准；`npm run format` 仅当 lint:check 残余涉及 json/css/md 时补跑（format:check 非门禁，不强制）；修复前后清单快照对比。
- **R4 零语义证明链**：机械段 `git diff -w -- <59文件>` 空 + 手工段逐处 diff 登记 + 308 用例全绿（若手工修复改产品行为，对应 spec 必须先行或同步加强）。
- **R5 熔断线**：触发「超限熔断」任一条件即停；格式化触及 >500 行债文件（IndexView.vue/IndexView.spec/LoginView.spec 等）的行数变化逐条登记（074 裁定先例）。

## 交付物（≈66 文件，案B 豁免）

1. `.eslintrc.cjs` `ignorePatterns`（dist/coverage 等，R1 定稿）
2. 机械波 ≈59 文件自动修复（`npm run lint` --fix；diff -w 空证明）
3. 手工段 ≈11 文件 23 Error 修复（R2 判定，逐条登记）
4. `scripts/smoke.sh` + `web-011c` 用例（先写）
5. 文档回写：task/session/iteration/registry 四件

## 实现说明

**研究项判定（R1-R5 全部闭合）**：
1. **R1 根因钉死**：`eslint .`（dist 未忽略）300s 零输出超时 vs 同命令 `--ignore-pattern 'dist/**'` **<60s 完成**——根因=构建产物 29 bundle/1.9M 被扫且 prettier 插件逐个重排；`.eslintrc.cjs` 加 `ignorePatterns: ['dist','coverage']` 治本（非 dist 内容问题，忽略即可，产物本体未动）。
2. **R2 23 手工 Error 逐条**（RED-2 清单全清 → E0）：
   - `no-unused-vars` 8 处**全是「省略式解构排除不可改/敏感字段」惯用法**（`const { code, ...updateData }` 等）——删变量会把 code/password 带回 update 载荷、破坏「code 不可改」契约 → **改配置 `ignoreRestSiblings: true`**（代码零动，契约语义保留）；
   - `no-useless-catch` 6 处纯重抛壳（roles/system/users IndexView）→ 拆壳保留原语句序，行为等价（原 catch 无副作用）；
   - `vue/no-parsing-error` 4 处 = RoleAssignDrawer 模板字面 `<<` 非法标签起始 → `&lt;&lt;` 实体转义（渲染文本相同）；另 1 处（波后 BaseTable:106 新生）= prettier 把模板内联 `as (…)|undefined` 联合类型断言拆行、vue 模板解析器无法解析（RED-2 原为 :101 deprecated-filter 同一行变异）→ **类型断言移入 script**（`toFormatter` 助手，模板只留调用），根除「prettier 换行×模板解析」脆弱面；
   - `vue/no-dupe-keys` 1 处 = BaseInput props 键 `showPassword` 与 setup 局部 ref 同名（模板绑定歧义风险）→ 局部改名 `passwordVisible`（4 处，props 面未动；现行为 setup 态优先本就取 ref，改名零行为变化）；
   - `no-ref-as-operand` 1 处 = roles `canDelete &&` ref 直接作操作数（恒真缺陷，波内 --fix 自动补 `.value`，权限判定恢复真实）——**真缺陷修复**，308 用例全绿守护；
   - `no-useless-escape` 2 处 = profile spec 正则字符类内 `\/` → `/`（零行为）。
3. **R3 自动修复口径**：`npm run lint`（--fix）单波覆盖 prettier 4536+attrs-order 18；`format:check` 非门禁未纳入；波2 仅对手工改动补格式。
4. **R4 零语义证明链**：案A 逐文件等价循环（HEAD 副本 → 同配 `eslint --fix` → 与工作树 diff）——**50 机械等价零夹带**；手工 8 文件 delta 逐行登记（hand-hunks.log117 行 + sidebar 单行）；test 308/308 + build ✓ 守护。
5. **R5 熔断自查**：实际 59 文件 ≤70 熔断线 ✓；无版本升级 ✓；无非空白夹带（等价循环证）✓；真缺陷（dupe-key/ref-as-operand）均为最小修未触发大改回报 ✓；>500 行债文件行数变化（HEAD~1→HEAD，全为 prettier 折行、机械等价证明覆盖）：`system/IndexView.vue` 766→818（+52）、`system/IndexView.spec.ts` 1496→1501（+5）、`LoginView.vue` 640→669（+29）、`LoginView.spec.ts` 667→727（+60，151 处 prettier）；roles/users IndexView 407→428、432→454。四债文件测试数与断言零删改（308/308 守护）。

**波后回归修复登记**：全量 test 曾 1 failed=`sidebar-icon-size.spec#fallbackIconExplicitSize16px`——其源码级标记 `'
.sidebar__icon'`（零缩进假设）被波的 `vueIndentScriptAndStyle` 顶层缩进打破；**测试侧语义同步**为 `'
  .sidebar__icon'`（仍断言顶层规则块 width/height 16px，非删除非放宽），复测 308/308 ✓。

**变更清单（三节）**：
### 新增
- （无）——smoke `web-011c` 用例追加于既有 `scripts/smoke.sh`（test: 提交内）

### 修改
- `frontend/.eslintrc.cjs`:3-6,27-29 — `ignorePatterns ['dist','coverage']`（AC1 卡死根因）+ `ignoreRestSiblings: true`（AC2 契约口径，8 处 unused-vars）
- 机械段 **50 文件** — eslint --fix（prettier 4536 + attrs-order 18），逐文件 `eslintFix(HEAD)≡工作树` 证明零夹带
- `frontend/src/components/BaseInput.vue`:39-141 — dupe-key 改名 passwordVisible×4
- `frontend/src/components/BaseTable.vue`:49-52,104 — toFormatter 助手 + 模板调用（模板联合类型断言解析错误根治）
- `frontend/src/views/roles/IndexView.vue`:214-241 — 2 处无用 catch 拆壳（+波内 canDelete .value 自动真缺陷修）
- `frontend/src/views/system/IndexView.vue`:389,468 — 2 处无用 catch 拆壳
- `frontend/src/views/users/IndexView.vue`:246,259 — 2 处无用 catch 拆壳
- `frontend/src/views/users/RoleAssignDrawer.vue`:232,240 — `<<`→`&lt;&lt;`×2（4 条解析错误）
- `frontend/src/views/__tests__/profile-view-icon-size.spec.ts`:39 — 正则 `\/`→`/`×2
- `frontend/src/__tests__/sidebar-icon-size.spec.ts`:45-46 — 源码标记随格式口径语义同步
- `scripts/smoke.sh`:170-175 — +`web-011c` 用例（test: 提交）
- 本单文档四件（task/session/iteration/registry）

### 删除
- （无）——零文件删除、零测试删除、any 171 警告按分治登记未修

**提交链**：`9e82973` test RED（RED-1/2/3 三实证+用例先行+AC3 案A 修订）→ `ec7bbc4` feat GREEN（59 文件治理）。

## 评审意见

**2026-10-08 Evaluator — sprint-083 评审：✅ 通过（平均分 9.3/10，六否决项零命中）**

1. **AC 逐条全满足（亲核）**：AC1 亲测 `lint:check` EXIT=0 耗时 11s（≠124）且输出零 `dist/` 路径、smoke web-011c ✅；AC2 RED 复现 4748/4559 规则分布逐条一致、治后 0 errors/165 warnings（全 no-explicit-any，逐文件审计仅 3 文件各 −2=−6=catch 拆壳，171−6=165 精确，零新增 any）；AC3 案A 独立抽查——机械段 3 文件（DashboardView/Header/useTheme）`eslintFix(HEAD)` 复跑等价 + 手工段 117 行 delta 全文复核（catch 拆壳=纯重抛等价、BaseInput 改名 4 处一致性+props 面保留、正则转义字符类内语义不变、`&lt;&lt;` 渲染等价）+ sidebar 单行 delta=标记同步；test 0 failed|308、build ✓ 17.81s；AC4 mvn 亲跑 154 4F+2E=基线五类逐条、冒烟亲跑 64=55✅+7❌+2⏭️（7❌=基线逐条、零新增）、smoke.sh diff=+8 行纯增量零删改、行数债实测登记入册。
2. **RED 独立复现**：worktree @9e82973+dist 软链重跑——RED1 EXIT=124、RED2 4748 problems(4559E/189W) 规则分布 prettier4536/unused8/catch6/parsing4/escape2/ref1/dupe1/deprecated1 与 RED 槽逐条一致；git 序 plan→test→feat 两段式成立。
3. **范围三方比对零越界**：feat 59 文件=config 1+src 58，零后端/零 package.json/零 dist/零版本升级；变更清单与 diff 一致。
4. **五维**：功能正确性 9.5 / 代码质量 9 / 规范遵守 9 / TDD 执行度 9.5 / 安全性 9.5 = **9.3**。
5. **真缺陷修复加分面**：roles `canDelete` ref 裸用恒真（原 :82 render 函数内无权限也渲染删除按钮）→ `.value` 修复，§6 语义贴合。
6. 改进建议（不计分）：① 测试清单 #3 结果格仍「（待 Generator）」待补；② AC4 文中 (752) 系 074 旧值（实测 HEAD~1=766，登记本体已用实测值）；③ profile spec 正则为预存单字符类弱断言（083 仅转义清理），建议 036c 强化；④ any 165+`--max-warnings` 收紧归 011d；⑤ 冒烟 web-013/020/026 过期 grep 归 036c。
