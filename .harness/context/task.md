# Sprint 工作单：sprint-083

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-083 |
| 所属模块 | web |
| 功能点 ID | **web/011c**（由 web/011 行预留独立转正注册；011 行已 ✅ 约束不改） |
| 功能点名称 | 全量 lint 门禁恢复——dist 忽略解锁卡死 + 全量 Error 4559 清零 |
| 状态 | PLANNED |
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

- [ ] AC1 — 卡死根因消除：治理前 `npm run lint:check`（`eslint .` 口径）全量 **EXIT=124** 实录入 RED 槽；R1 对照实证钉死根因（dist 扫描 vs 其他，含 `eslint .` vs 忽略 dist 后的对照命令输出）；治理后全量在 300s 内完成运行、exit ∈ {0,1}（**≠124**）且输出零 `dist/` 路径。← 用例 `smoke web-011c lint 全量门禁恢复` + 门禁实测
- [ ] AC2 — 全量 Error 清零：忽略 dist 后首跑清单入 RED 槽（Error 总数与按文件/规则分布，预期 ≈4559 = 4536 prettier + 23 手工 + 根级增量，以实测钉死）；修复后 `npm run lint:check` **exit 0、Error 0**；Warning 处置口径入册——attrs-order 18 随自动修复清零、any 171 允许残留但逐条登记归 011d。← 用例 `AC2 ← 门禁实测 lint:check exit 0（RED=4559±Error 清单）`
- [ ] AC3 — 零语义回归：①机械段（自动修复涉及的约 59 文件）`git diff -w` **空**（零非空白变更）；②手工段文件清单与逐处 diff 理由登记入实现说明（预期 ≈11 文件/23 Error）；③`npm run test` **0 failed | 308 passed** 与 `npm run build` ✓ 均不回归（手工段若涉产品文件，每处须论证行为等价并由对应 spec 守护）。← 用例 `AC3 ← git diff -w 实测 + 门禁两线`
- [ ] AC4 — 门禁五线与冒烟：`mvn -q verify` 154 用例 4F+2E=基线五类逐条；`bash scripts/smoke.sh` 存量 **63 用例不减不改** + 本单追加 `web-011c` → 64（7❌ 属基线不背锅）；行数债登记——`IndexView.vue`(752)/`IndexView.spec`(1496)/`LoginView.spec`(667) 等 >500 行文件经格式化后的行数变化逐条入册。← 用例 `AC4 ← 门禁五线实测 + 冒烟 64`

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
| 1 | 全量 `lint:check` 卡死实录（124）与 R1 对照实证 | AC1 | （待 Generator RED 槽） |
| 2 | 忽略 dist 后首跑 Error 清单（4559± 分布入册） | AC2 | （待 Generator RED 槽） |
| 3 | smoke `web-011c` lint 全量门禁恢复（先写后绿） | AC1/AC4 | （待 Generator） |
| 4 | 门禁五线：lint:check exit 0 / test 0 failed\|308 / build ✓ / mvn=基线 / 冒烟 63 存量+新增 | AC2/AC3/AC4 | （待 Generator） |

## RED 证据

> Generator 于任何治理动作前执行测试清单并粘贴实证；机械段 `--fix` 前必须先存首跑清单快照。

```text
[RED-1] 全量卡死实录：（待填——npm run lint:check 全量 EXIT=124 命令行与耗时）
[RED-2] 忽略 dist 后首跑清单：（待填——Error 总数 + 按文件 top + 按规则分解 + Warnings 口径）
[RED-3] smoke web-011c 首跑失败实录：（待填）
```

> 治理后复跑。

```text
[GREEN] （待填——lint:check exit 0、test 0 failed|308、build ✓、smoke 64=55✅+7❌+2⏭️ 预期）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-08 sprint-082 评审实测 + 本单规划只读实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（五类，veto-6 豁免） | （待填） | （待填） |
| `npm run lint:check` 全量 | **EXIT=124 卡死**（5+ 次复现）；src-only 实测 4748 问题/60 文件 | （待填：治后 exit 0） | （待填） |
| `npm run test` | **0 failed \| 308 passed**（EXIT=0） | （待填） | （待填） |
| `npm run build` | ✓（~17s） | （待填） | （待填） |
| `bash scripts/smoke.sh` | **63=54✅+7❌+2⏭️**（7❌ 基线不背锅） | （待填：预期 64=55✅+7❌+2⏭️） | （待填） |

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

（待 Generator：R1-R5 判定结论 + 变更清单 + 提交链 + 断言/行为等价登记）

## 评审意见

- （待 Evaluator）
