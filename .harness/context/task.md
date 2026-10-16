# Sprint 工作单：sprint-102

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-102 |
| 所属模块 | **web** |
| 功能点 ID | **web/054** |
| 功能点名称 | 冒烟用例输出收敛（stderr/stdout 噪声治理，取证可读性） |
| 状态 | **PLANNED** |
| 创建时间 | 2026-10-16 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/053 | smoke.sh FILTER 过滤参数（本单沿用其计数/过滤语义，不回归） | ✅ |
| sprint-100 评审意见 | 扣分项② 冒烟 stderr 噪声（本单清偿） | — |
| sprint-101 评审意见 | 同上再度建议（本单清偿） | — |

## 业务背景

**根因（Planner 2026-10-16 只读实测）**：`smoke_case`/`smoke_case_cond` 直接 `"$@"` 执行，用例输出（stdout+stderr）全部泄入终端——实测 `model-001` 一条即产生：stdout 侧 Spring banner+INFO 数十行、stderr 侧 Mockito self-attaching/Java agent/`--enable-native-access` 警告。全量 78 用例累计噪声淹没 `✅/❌` 结论行，评审取证与人工阅读体验差（连续两轮评审判罚扣分）。

**修法**：每条用例输出合并重定向到 per-case 日志文件，成功=静默（仅 `✅ 通过`）；失败=仅回放日志**末 40 行**到 stderr（保留排障信息）+打印日志路径。计数/过滤/退出码语义零变。

## 验收标准（TDD 驱动）

- [ ] AC1 — RED（合成用例护栏）：`bash -c 'source <(sed -n "1,/^# ===================== 用例区/p" scripts/smoke.sh); smoke_case "syn-x" bash -c "echo SYNTHETIC_NOISE; exit 1"' 2>/dev/null | grep -q SYNTHETIC_NOISE` —— 断言「stdout 零噪声」；**修复前 = 噪声泄漏 stdout（断言 false）**。← 用例 `AC1 ← 用例 合成失败用例 stdout 泄漏红`
- [ ] AC2 — GREEN（三断言）：① 同 AC1 断言转 true（stdout 零噪声）；② 失败日志回放=stderr 含 `SYNTHETIC_NOISE`（`... 2>&1 >/dev/null | grep -q`）；③ 成功路径 `syn-ok`（echo 噪声+exit 0）→ stdout 与 stderr 均零噪声回放；④ 计数器不回归：失败合成用例后 `$CASES=1`、`$FAIL=1`。← 用例 `AC2 ← 用例 合成用例三断言+计数器`
- [ ] AC3 — 既有能力回归：`bash scripts/smoke.sh web-052` = 1 用例 ✅（FILTER 不回归）；`bash scripts/smoke.sh zzz-not-exist` = exit 2（无匹配防呆不回归）。← 用例 `AC3 ← 用例 FILTER/防呆回归`
- [ ] AC4 — 全量冒烟零漂移+噪声收敛：`bash scripts/smoke.sh` → **78=76✅+2⏭ 0❌ `SMOKE PASSED（78 用例）`** 且 stdout 实测 `grep -c "Mockito is currently self-attaching"`=0、`grep -c "SpringBootTestContextBootstrapper"`=0（噪声归零实证）。← 用例 `AC4 ← 用例 全量冒烟 78+噪声计数为 0`

### AC1/AC2 — 合成用例护栏（test-first）

**步骤与期望**：先写合成断言（AC1）→ 改两函数（AC2）。合成用例通过 source 用例区前的函数定义块实现，不新增正式冒烟用例（**用例区零动**，78 口径不变——同 053 先例）。

**用例**：`AC1/AC2 ← 用例 合成失败/成功用例定向断言`

### AC3/AC4 — 回归与冒烟

**用例**：`AC3/AC4 ← 用例 FILTER 回归+全量冒烟噪声计数`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | 合成失败用例 stdout 零噪声（修复前红） | AC1 | 待红 |
| 2 | 合成失败用例日志回放+成功路径静默+计数器 `1 1` | AC2 | 待绿 |
| 3 | `smoke.sh web-052`=1 用例；`smoke.sh zzz-not-exist`=exit 2 | AC3 | 待验 |
| 4 | 全量 78=76✅+2⏭ 0❌ + stdout 噪声计数 0 | AC4 | 待亲测 |

## RED 证据

```text
[修复前红态] 证据槽 — 待 Generator 填写（合成失败用例 SYNTHETIC_NOISE 泄漏 stdout）
```

## 门禁与冒烟记录

> Generator 亲测填写。基线（sprint-100 起）：冒烟 78=76✅+2⏭ 0❌；前端/后端零动。

| 门禁项 | 本单后预期 | 实测 | 结论 |
|--------|---------|------|------|
| `bash scripts/smoke.sh` | 78=76✅+2⏭ 0❌ `SMOKE PASSED` | 待填 | 待验 |
| stdout 噪声计数 | `Mockito...`=0、`SpringBootTestContextBootstrapper`=0 | 待填 | 待验 |
| 合成用例三断言+计数器 | 全绿 | 待填 | 待验 |
| FILTER/防呆回归 | 1 用例 / exit 2 | 待填 | 待验 |
| 前端/后端 | 零动（不跑） | — | 零动 |

## 实现说明

**实现要点**（`smoke_case` 与 `smoke_case_cond` 对称改造）：
1. `local log="${TMPDIR:-/tmp}/smoke-case-$$-${CASES}.log"`；`"$@" >"$log" 2>&1`（cond 的 `eval "$cond_cmd"` 同样重定向 `/dev/null`——cond 输出无诊断价值）。
2. 成功：`echo "    ✅ 通过"` + `rm -f "$log"`。
3. 失败：`echo "    ❌ 失败: $name"` + `echo "    ---- 日志末 40 行（$log）----"` + `tail -n 40 "$log"`（**输出到 stderr**）+ `FAIL=1`。
4. 计数器/过滤/尾部汇总语义零变；既有 14 条用例内部自写 `/tmp/smoke-*.log` 不受影响（其定向 grep 不变）。
5. 头部注释补一行「诊断输出策略：成功静默、失败回放末 40 行」。

**超限熔断**：改动 >1 文件、动用例区（78 用例增减/改名/改命令）、动尾部汇总格式、改 FILTER 语义、失败日志不留档（丢排障能力）→ 停止回报。

**不动清单**：用例区全部 78 用例；`SMOKE_APP_LOG` 与 14 处内部自写日志；FILTER/RAN/exit 2 语义；SMOKE PASSED/FAILED 汇总行格式；后端/前端全部；application.yml.template D 态。

## 交付物（1 业务文件）

1. `scripts/smoke.sh` — 两函数输出收敛+头部注释

### 新增
（无）

### 修改
- `scripts/smoke.sh`

### 删除
（无）

### 变更计数

业务文件 **1**；用例区零动（78 口径不变）；计数/过滤/退出码语义零变。

## 评审意见

（待 Evaluator 评审）
