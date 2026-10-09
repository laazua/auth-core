# Sprint 工作单：sprint-085

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-085 |
| 所属模块 | web |
| 功能点 ID | **web/036c1**（web/036 行第三子功能点 036c 的第一切片；036a ✅ sprint-067、036b ✅ sprint-084；036c 余量拆 036c2+ 预留） |
| 功能点名称 | smoke 过期口径同步与弱断言强化——web-013/020/026 过期 grep 复活 + profile spec 单字符类弱正则强化 + 计数断言语义化 |
| 状态 | PLANNED |
| 创建时间 | 2026-10-09 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/036a | EP 暗色变量接入（036 行底座） | ✅ sprint-067 |
| web/036b | 暗色局部白底缺陷修复（上一切片） | ✅ sprint-084 |
| web/011b/011c | 计数断言 passed==total 先例 + lint 门禁恢复（011b 同步口径本单复用） | ✅ sprint-082/083 |
| sprint-084 | 门禁基线（lint 0E\|165W、test 315、冒烟 65=56✅+7❌+2⏭️、mvn 154 4F+2E） | ✅ PASS 9.5/10 |

## 业务背景

036c 为 registry 036 行预留收口子功能点（「打磨与口径治理：冗余覆盖与死代码清理、smoke 过期 grep 同步」）。本日只读调研将 036c **首切片边界钉死为「口径治理」**（清理面因规模待普查归 036c2+）：

1. **冒烟 7❌ 全量分类实录（2026-10-09 Planner 只读复跑，逐条定性）**：
   - `web-013`：断言 LoginView.spec `19 passed`——**实测 24 passed**，过期计数（用例名言「密码输入框默认隐藏」而体只跑整文件计数，名实脱节）→ 本单
   - `web-026`：断言 LoginView.spec `23 passed`——**实测 24 passed** 过期计数；`.env` 断言实测在场 → 本单
   - `web-020`：负断言（`sidebar__footer`/`sidebar__toggle` 已除）实测通过；正断言 `header__toggle` 锁死 `Header-*.css`——**该 chunk 不存在**，产物实际承载于 `DefaultLayout--0UvTHqz.css`（grep 全 dist 实测命中）→ 本单
   - `roles-001`/`roles-002`：定向含基线失败类 `RoleControllerTest`（1F=assignPermissionsInvalidPermissionReturns400，veto-6 基线豁免五类）→ **基线下游，本单不背**，归后端五类基线治理（后续/人工裁决）
   - `model-008`：子集复跑定位 `RoleServiceInterfaceSpec#roleServiceInterfaceHasAllMethods` 失败（全量态通过、`-Dtest` 子集态失败，11:1F）→ **真实测试缺陷（确定性/环境敏感）**，后端测试质量，归 **036c2/后续**，本单不背
   - `model-010`：与 model-008 同族（子集态定向，未逐断言定位）→ 同归 036c2/后续
2. **083 评审改进建议③（归 036c）**：`profile-view-icon-size.spec.ts:38` 单字符类弱正则 `['"@/components/BaseButton.vue['"]`——字符类含路径全部字符，等价于「匹配任意含单引号/双引号/任一路径字符的串」，**错例误通过面**（083 仅做转义清理未强化）→ 本单
3. **084 评审改进建议②（归 036c 顺手项）**：smoke `web-011b` 注释 `# web/084 语义同步` 疑为 `sprint-084` 笔误 → 同波勘误（scripts/smoke.sh 同文件零成本）
4. **083 评审改进建议⑤**：冒烟 web-013/020/026 过期 grep 归 036c——与本调研一致，即本单主体
5. **计数断言反模式延续治理**：011b 先例（`passed==total` 模式 + vitest `|| exit 1`）已验证等强（084 RED 期正确拒 `7 failed`）；web-013/026 硬编码计数属同族反模式，同口径语义同步，**防止下次用例新增再次自伤**

**036c 拆分登记（本单只做第一切片）**：
- **036c1（本单，sprint-085）**：smoke 过期口径同步 + profile 弱断言强化 + 注释勘误——口径治理，纯测试层
- **036c2（预留）**：冗余覆盖与死代码清理（需全仓普查，规模待定，>6 文件须问案B）+ model-008/010 子集态测试缺陷
- **036c3（预留）**：串联 `:deep()` 42 处/17 组件（068 登记）——规模大，>6 文件须问案B
- **后续/人工**：roles-001/002 = 后端基线五类下游（veto-6 豁免面，非 036c 可修）；any 165 → **011d**；`--max-warnings`/版本升级（§1 固化）

## 需求描述

把 3 条过期冒烟用例复活并对准当前产物/用例现状（计数断言语义化、chunk 文件名对准实际承载）、强化 profile spec 的单字符类弱正则为精确断言、同波勘误 smoke 注释笔误；追加至少 1 条冒烟用例并让全量冒烟 ❌ 从 7 收敛到 ≤4（剩余 4 条=已登记不背面），零新增失败。纯测试/口径层变更，产品代码零改动。

## 验收标准（TDD 驱动）

- [ ] AC1 — web-013 与 web-026 计数断言语义同步：两用例对 LoginView.spec 的断言改为 passed==total 模式（`^      Tests  [0-9]+ passed \([0-9]+\)$`，vitest `\|\| exit 1` 门禁保留，不删除用例、不断言体），断言行零硬编码具体计数（19/23 字样从断言中消失）；`bash scripts/smoke.sh` 中两用例复跑 ✅。← 用例 `AC1 ← 用例 smoke web-013、web-026（当前 ❌ 即 RED，同步后复跑 ✅）`
- [ ] AC2 — web-020 正断言对准现存产物：`header__toggle` 断言目标由不存在的 `Header-*.css` 改为实际承载文件 `DefaultLayout-*.css`（或全 `dist/assets/css/*.css` 兜底，二选一在实现说明登记）；`sidebar__footer`/`sidebar__toggle` 负断言原语义保留；复跑 ✅。← 用例 `AC2 ← 用例 smoke web-020（当前 ❌ 即 RED，修正后复跑 ✅）`
- [ ] AC3 — profile spec 弱正则强化：`profile-view-icon-size.spec.ts:38` 单字符类 `['"@/components/BaseButton.vue['"]` 改为精确断言（精确匹配 import 语句完整形态），并给出**断言有效性证据**：旧正则对错例（如 `import x from 'zzz'`）误通过、新正则拒绝同一错例；该 spec 独立运行 ✅ 且 `npm run test` 全量 0 failed。顺波落位 084 建议②：smoke web-011b 注释 `web/084`→`sprint-084` 勘误。← 用例 `AC3 ← 用例 profile-view-icon-size spec 强化断言 + 全量回归`
- [ ] AC4 — 追加冒烟用例与全量收敛：为本单能力追加 **≥1 条**冒烟用例（建议形态：web-013 名实对齐——登录页密码框默认隐藏行为的直接断言，取代/叠加「整文件计数」体；Generator 可选等效形态但须覆盖本单口径能力并留 RED）；全量 `bash scripts/smoke.sh`：总数 **≥66**（65+新增）、**❌ 恰为 4 条**（model-008、model-010、roles-001、roles-002，=登记不背面）、零新增失败。← 用例 `AC4 ← 用例 门禁五线实测 + 冒烟全量复跑`

### AC1 — web-013/026 计数断言语义同步
> Given 两用例硬编码 19/23 而实测 24、用例新增即自伤。When 按 011b 先例改 passed==total 语义。Then 断言零硬编码且两用例复跑 ✅。
**用例**：`AC1 ← 用例 smoke web-013、web-026`

### AC2 — web-020 正断言对准现存产物
> Given header__toggle 实存于 DefaultLayout chunk 而用例锁死不存在的 Header-*.css。When 目标改为实际承载。Then 负断言保留、用例复跑 ✅。
**用例**：`AC2 ← 用例 smoke web-020`

### AC3 — profile spec 弱正则强化
> Given 单字符类正则错例误通过。When 改为精确断言并留有效性证据。Then 错例拒真例过、全量 0 failed、注释勘误同波。
**用例**：`AC3 ← 用例 profile-view-icon-size spec + 全量回归`

### AC4 — 追加冒烟用例与全量收敛
> Given 增量约定强制 + 7❌ 中 3 条属本单口径。When 追加用例并同步三用例。Then 总数 ≥66、❌ 恰 4（登记不背面）、零新增失败。
**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

> TDD 载体：**本单为口径/断言治理型——三用例当前 ❌ 即天然 RED（先录失败实证再动手）**；AC3 以「旧正则对错例误通过、新正则拒绝」的变异式证据充当 RED；新增用例先写（对现状必红或必缺）后绿。产品代码零改动 → 无产品 spec 层；门禁按基线口径复跑。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | smoke web-013/web-026 同步前失败实证（计数 19/23 vs 实测 24） | AC1 | ✓ RED-1=两用例 EXIT=1 + 实测 `Tests 24 passed (24)`（evidence in RED 槽） |
| 2 | smoke web-020 同步前失败实证（Header-*.css 不存在，实载 DefaultLayout-*.css） | AC2 | ✓ RED-2=EXIT=1（目标文件不存在）+ DefaultLayout-*.css 命中=1 |
| 3 | profile spec 强化断言 + 旧正则错例误通过变异证据 | AC3 | ✓ RED-3=旧正则错例 true 误过 / 新正则 false 拒真例 true（node 实录） |
| 4 | 新增冒烟用例首跑（对现状缺/红）+ 门禁五线 + 全量冒烟 ❌=4 | AC4 | ✓ 新用例 web-036c1 首跑 EXIT=1（三反模式在场=RED）；门禁/冒烟待 GREEN |

## RED 证据

> Generator 于任何修改动作前执行测试清单并粘贴实证。

```text
[RED-1] web-013/web-026 用例体首跑（2026-10-09 实时，/tmp/r085-013.log、/tmp/r085-026.log）：
web-013 EXIT=1、web-026 EXIT=1（均为 ❌ 状态）
计数实证（直跑 LoginView.spec 绕过 grep -q 吞噬）：`Tests  24 passed (24)`——用例硬编码 '19 passed'/'23 passed' 全部过期
（smoke.sh:182 `grep -q '19 passed'`、:334 `grep -q '23 passed'`）

[RED-2] web-020 用例体首跑（/tmp/r085-020.log）：
EXIT=1；失败尾 `grep: dist/assets/css/Header-*.css: 没有那个文件或目录`（目标文件不存在）
对照：grep 全 dist 命中 `header__toggle` 恰在 `DefaultLayout-*.css`（命中计数=1，Planner 预研复核）

[RED-3] profile 弱正则变异实录（node 单行，/tmp 实时输出）：
旧正则 `/['"@\/components\/BaseButton.vue['"]/` 对错例 `totally.bogus junk` 误通过 = **true**（缺陷面）
新正则 `/^import BaseButton from ['"]@\/components\/BaseButton\.vue['"];?$/m` 错例 = false、真例 = true
另：新自检用例 web-036c1 首跑 EXIT=**1**（'19 passed'/'23 passed'/'Header-*.css' 三反模式在场=红，/tmp/r085-036c1.sh）
```

> 治理后复跑。

```text
[GREEN] （待 Generator——三用例转绿、spec 强化过、冒烟 ❌7→4、门禁五线=基线）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-09 sprint-084 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（五类，veto-6 豁免） | （待填） | （待填） |
| `npm run lint` / `lint:check` | **EXIT=0，0 errors / 165 warnings** | （待填） | （待填） |
| `npm run test` | **0 failed \| 315 passed (315)** | （待填） | （待填） |
| `npm run build` | ✓（~18s） | （待填） | （待填） |
| `bash scripts/smoke.sh` | **65=56✅+7❌+2⏭️** | （待填：目标 总数≥66、❌恰4=008/010/roles×2） | （待填） |

## 拆分说明

预估验收标准 **4 条（=上限）**；预估文件变更 **≈2-4**（`scripts/smoke.sh` 1 + `profile-view-icon-size.spec.ts` 1 + 新增用例若独立文件 1-2，文档回写 4 不计入）——**≤6 文件不拆分**。036c 整体超限故按本单首切片登记（036c2/036c3 预留见业务背景拆分登记）。

**研究项（Generator 先于动手核实）**：
- **R1 同步形态**：web-013/026 断言采用 011b 同款 passed==total 正则 + 保留 `npx vitest … || exit 1`；核对两用例体其余断言（web-026 的 `.env` 断言）原语义保留。禁止删除用例或降级为无断言。
- **R2 chunk 承载核对**：`header__toggle` 实载文件以 Generator 亲跑 build 后 grep 为准（Planner 预研=DefaultLayout--*.css）；选定目标须幂等（chunk 哈希变化不破）——推荐 `DefaultLayout-*.css` 通配或全 css 兜底，并在实现说明登记选择理由。
- **R3 新增用例形态**：优先建议「web-013 名实对齐」——直接断言登录页密码框默认隐藏行为（源码 `type="password"` 默认态 / LoginView spec `-t` 定向均可，Generator 按 tdd-workflow 冒烟节形态定），须与既有 web-033（密码可见性切换）互补不重复。
- **R4 变异式 RED**：AC3 的 RED=旧正则对构造错例的误通过实录（Node 单行复现即可）+ 新正则拒绝同错例；避免「强化后直接绿」无 RED 的形式主义。

**超限熔断**：实际文件 >6、或需动产品代码（.vue/.java/yml）、或新断言引发既有用例连锁失败需改他案用例语义、或普查中发现第 4 类新缺陷面 → **停止回报，回 Planner 重切分**。

**不动清单**：产品代码全部（.vue/.ts 产品文件/java/yml，`profile-view-icon-size.spec.ts` 除外）；web-011b/011c/036a/036b 既有断言语义（084 仅勘误注释文字）；roles-001/002 与 model-008/010 四条 ❌ 用例**不修改不删除**（登记不背面）；后端五类基线；any 165（011d）；036c2/036c3 全部；`--max-warnings`/版本升级（§1）。

## 交付物（≈2-4 文件）

1. `scripts/smoke.sh`：web-013/020/026 三用例同步 + web-011b 注释勘误 + 新增 ≥1 用例
2. `frontend/src/views/__tests__/profile-view-icon-size.spec.ts`：弱正则强化 + 变异证据
3. （若新用例需独立 spec 文件）新增测试文件 1 个
4. 文档回写：task/session/iteration/registry 四件

## 实现说明

（待 Generator）

## 评审意见

- （待 Evaluator）
