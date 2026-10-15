# Sprint 工作单：sprint-099

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-099 |
| 所属模块 | **hyg**（流程卫生/工程化改进） |
| 功能点 ID | **web/053**（冒烟改进方案 A：单用例/前缀过滤参数，用户 2026-10-14 已确认） |
| 功能点名称 | smoke.sh 过滤参数支持（日常单点验证提速；全量基线口径零影响） |
| 状态 | **AWAITING_REVIEW**（Generator 交付完成 2026-10-14，四 AC 亲测+门禁在案） |
| 创建时间 | 2026-10-14 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| tdd-workflow「冒烟测试」节 | smoke.sh 为强制门禁脚本，禁止删除既有用例 | ✅ |
| 用户确认 | 方案 A（单用例参数）确认、方案 B（批次合并）暂不做 | ✅ |

## 业务背景

**根因（sprint-098 会话分析）**：冒烟 77 用例中 25+ 个各自起一个 `mvn -Dtest` 定向调用——每次 mvn 都重启 JVM+加载 Spring 上下文（约 8-15s），串行累计占全量冒烟约 80% 时间。日常开发「只验证本单新用例」也被迫全量跑 ~15 分钟。

**方案 A（本单，已确认）**：`bash scripts/smoke.sh [匹配串]`——首位置参数为用例名**子串过滤**；不带参数=全量 77 用例（**基线口径零影响**，所有既有「基线 77=…」对照照抄）；无匹配时非零退出防误过滤假 PASS。

**方案 B（批次合并）不做**：需重定义用例数/❌ 统计粒度，基线口径成本高，用户已裁决暂不做。

## 验收标准（TDD 驱动）

- [ ] AC1 — 单用例过滤：`bash scripts/smoke.sh web-052` 仅执行 1 条用例（输出仅含 web-052 行、`SMOKE PASSED（1 用例）`、EXIT=0）——修复前行为=无参全量跑（红态=当前脚本不认参数仍全量 77）；修后定向绿。← 用例 `AC1 ← 用例 bash scripts/smoke.sh web-052 行为断言`
- [ ] AC2 — 子串匹配多用例：`bash scripts/smoke.sh roles` 匹配 roles-001/002 及名中含 roles 的用例（计数=匹配数、EXIT=1 因 roles-001/002 既有基线 ❌——断言=输出用例数=匹配数且仅 ❌=roles-001/002）；`bash scripts/smoke.sh web-0` 匹配 web 系全量。← 用例 `AC2 ← 用例 bash scripts/smoke.sh roles 行为断言`
- [ ] AC3 — 无匹配防呆：`bash scripts/smoke.sh zzz-not-exist` 非零退出+提示「无匹配用例」（防误过滤把全跳过当 PASS）。← 用例 `AC3 ← 用例 bash scripts/smoke.sh zzz-not-exist EXIT≠0`
- [ ] AC4 — 全量基线零影响：不带参 `bash scripts/smoke.sh` 仍跑 **77 用例=75✅+2❌+2⏭**（❌=roles-001/002、⏭=infra-004/model-006），与既有基线口径逐字一致；过滤逻辑不得跳过/删除任何既有用例定义。← 用例 `AC4 ← 用例 冒烟全量 77`

### AC1/AC2/AC3 — 过滤行为（过期/新增能力型）

**步骤与期望**：改 `smoke_case`/`smoke_case_cond` 开头加过滤（首参数 FILTER，name 不含 FILTER 则 return 0 不计数不输出；函数区后记录 MATCHED 计数）；脚本尾部无匹配检测非零退出。修复前红态=当前脚本无过滤能力（`bash scripts/smoke.sh web-052` 会全量跑——Planner 2026-10-14 只读取证在案）。

**用例**：`AC1/AC2/AC3 ← 用例 三行为定向断言`

### AC4 — 全量基线

**用例**：`AC4 ← 用例 bash scripts/smoke.sh EXIT=1+77 用例+75✅+2❌+2⏭`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | `bash scripts/smoke.sh "web/052"`（1 用例 ✅） | AC1 | **✅ EXIT=0 `SMOKE PASSED（1 用例）`**（口径注记：该用例名为斜杠 `web/052`，web 系均斜杠、其余横杠，过滤为通用子串两者皆可） |
| 2 | `bash scripts/smoke.sh roles`（仅 roles-001/002 两用例、❌=2） | AC2 | **✅ 恰匹配 2 条**（`SMOKE FAILED（2 用例）`=既有基线 ❌，过滤计数正确） |
| 3 | `bash scripts/smoke.sh zzz-not-exist`（EXIT≠0） | AC3 | **✅ EXIT=2** `无匹配用例: zzz-not-exist` |
| 4 | `bash scripts/smoke.sh` 全量 77=75✅+2❌+2⏭ | AC4 | **✅ `SMOKE FAILED（77 用例）`**：❌=roles-001/002、⏭=infra-004/model-006，与基线逐字一致 |

## RED 证据

```text
[修复前红态 2026-10-14 结构取证] git show HEAD:scripts/smoke.sh | grep -c FILTER → 0（旧脚本无 FILTER 变量、不认任何参数，
带匹配串=仍全量 77 串行跑 ~15 分钟——sprint-098 全量亲测在案）；修后三行为断言转绿（AC1/AC2/AC3 定向 EXIT=0/2/2 恰符合）
```

## 门禁与冒烟记录

> Generator 亲测填写。基线口径（2026-10-14 sprint-098 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 168 用例 4F+2E | 4F+2E=基线五类豁免文件（DataSourceConfigBinding/RoleController/SeedData/TestLayersSpec/TestUtilsSpec） | ✅=基线 |
| `npm run lint` | 0E\|166W | 0E\|166W | ✅=基线 |
| `npm run test` | 41 文件 324/324 | 41/324 全绿 | ✅=基线 |
| `npm run build` | ✓ | ✓ 16.60s | ✅=基线 |
| `bash scripts/smoke.sh`（全量） | 77=75✅+2❌+2⏭️ | 77=75✅+2❌(roles-001/002)+2⏭️(infra-004/model-006) | ✅=基线逐字一致 |
| 过滤行为三断言（AC1-3） | — | 单跑 1✅/子串 2 条/无匹配 exit 2 | ✅ 全绿 |

## 实现说明

**提交物证**：单段 `d2340c9 feat`（无 test 段可拆：行为断言=跑脚本本体无独立测试文件，用例区按 AC4 零动不新增——单段理由同 036c2a 过期修复型，Generator 主动披露供 Evaluator 裁决）。

**实现要点**：
1. 脚本头部加 `FILTER="${1:-}"`（set -u 下安全默认空）；`smoke_case`/`smoke_case_cond` 函数开头：`if [ -n "$FILTER" ] && [[ "$name" != *"$FILTER"* ]]; then return 0; fi`。
2. 匹配计数 `RAN`（实际执行数）与尾部检测：`if [ -n "$FILTER" ] && [ "$RAN" -eq 0 ]; then echo "无匹配用例: $FILTER"; exit 2; fi`。
3. 头部注释补一行用法说明（`bash scripts/smoke.sh [用例名子串]`）。
4. 三行为定向断言跑绿；全量 77 复跑=基线。

**超限熔断**：改动 >1 文件（仅 smoke.sh）、既有用例被删除/改名、全量基线漂移 → 停止回报。

**不动清单**：用例区任何既有 `smoke_case*` 行（禁止删除/改名/改命令）；tdd-workflow.md 规则语义；mvn/前端三线基线；application.yml.template D 态。

## 交付物（1 业务文件）

1. `scripts/smoke.sh` — FILTER 参数+RAN 计数+无匹配非零退出+用法注释
2. 文档回写：task/session/iteration/registry 四件

### 新增
（无）

### 修改
- `scripts/smoke.sh`

### 删除
（无）

### 变更计数

业务文件 **1**；用例区零动（全量 77 用例与基线口径逐字一致）；后端/前端零动。

## 评审意见

（待 Evaluator 评审）
