# Sprint 工作单：sprint-080

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-080 |
| 所属模块 | Evaluator 评审轮（跨 web 模块） |
| 功能点 ID | 积压评审轮·第一轮（web/017、web/024、web/030、web/031、web/032 五项） |
| 功能点名称 | Evaluator 积压台账评审——五项「实现完成待评审」功能点补评审与台账归位 |
| 状态 | DONE |
| 创建时间 | 2026-09-29 |
| 消费者 | **Evaluator**（本单无实现段，Generator 跳过；Planner→Evaluator 直达） |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| 积压红线 | 「禁止跳过 Evaluator」红线连续多轮违例积压（自 2026-09-02 起） | 本单清理 |
| 评审基线 | sprint-079 eval 后 HEAD=f110179，基线 mvn 154/冒烟 61/前端三件套 | ✅ |

## 业务背景

session-state 挂起区「Evaluator 积压六项」清单**经 Planner sprint-080 台账考古勘误为五项待评审**，两项剔除：

| 原清单项 | 勘误结论 | 依据 |
|---------|---------|------|
| sprint-060（web/029） | **已评审 PASS 8.6/10，剔除** | eval commit `f6d8acd` 实证 |
| sprint-047（web/023） | **未实现，剔除评审轮归待实现** | registry 🔄「规划中」+ `components/Layout/TagsView.vue` 仍存在（移除目标未动） |
| sprint-041（web/017） | **原清单漏列，补入** | RED `fe0e402`+GREEN `a232011` 实证实现完成、无 eval |

**真实待评审五项**（历史流程欠账：实现完成但 Evaluator 从未评审，「禁止跳过 Evaluator」红线清理）：

| # | 功能点 | sprint | 实现证据索引 | task 快照 | 备考 |
|---|--------|--------|-------------|----------|------|
| 1 | web/017 顶栏用户下拉 UI 修复 | 041 | `fe0e402`(RED)+`a232011`(GREEN) 三段式缺 docs | 被后续覆盖 | 067 期曾登记「与 web/036 同源可一并对照」 |
| 2 | web/024 主内容区图标尺寸 | 048 | **⚠️ 09-03 窗口无独立 feat commit**（窗口 task=model/010） | 被覆盖 | 仅 registry 备注 AC 摘要（ProfileView BaseButton 16×16/20×20、Breadcrumb 13×13）；**考古风险见 AC2** |
| 3 | web/030 权限页模块下拉 404 | 061 | `1d934de`（`-S permissions/all` 命中，大杂烩提交） | 被覆盖 | 接口现存活于 `ModuleController:151` |
| 4 | web/031 用户抽屉确认按钮 | 062 | `c49bed0`（含 drawer test；loading 三态现存活） | 被 063 覆盖 | 单文件修复 |
| 5 | web/032 SPA 刷新 404 | 063 | `c49bed0`（vite.config historyApiFallback+nginx try_files，均现存活） | **`c49bed0:.harness/context/task.md` 完整**（状态自标 DONE） | 063 期自行标 DONE 属当时惯例，未评审是核心事实 |

## 需求描述

Evaluator 对五项逐一执行完整评审流程（考古→AC 核对→门禁对照→五维打分→结论→回写），一份轮报告含五个小节；共享门禁只亲跑一次服务全部。**非三段式/混杂提交系历史流程债**：TDD 维度按「当时可得的测试证据」评（混入提交的 test 文件计为有测试证据），不按现代三段式硬性要求扣死，但考古缺失（无实现证据）须如实判退。

## 验收标准

- [x] AC1 — web/017（sprint-041）评审完成：核对 4 条原始 AC（registry 备注+挂起区浏览器实测基线）、RED/GREEN commit 考古、当前代码对照（下拉弹层主题化/图标 16px 是否仍生效）、五维打分+结论
- [x] AC2 — web/024（sprint-048）评审完成：**考古优先**——09-02~09-04 全窗口找实现证据（含混入 `2ac5581` 大杂烩的可能）；**若考古确认无实现证据→本项判退回（FAIL 退回实现），不得因 registry 标注「实现完成」放行**；有证据则按 3 条 AC（registry 备注）核对+打分
- [x] AC3 — web/030/031/032（sprint-061/062/063）三项评审完成：各自实现考古（索引见背景表）、原始 AC 核对（030 有完整 task 快照、031/032 从 registry 备注重建）、当前功能探针（接口/按钮/回退配置现存活=实现落库实证）、逐项五维打分+结论
- [x] AC4 — 共享门禁与台账归位：当前 HEAD 亲跑一次 `mvn -q verify`（期望 154 4F+2E）、前端三件套（期望基线）、冒烟（期望 61=50✅+9❌+2⏭️）——**注意评审对象为历史交付，门禁按当前基线对照（veto 6 豁免先例）**；五项 registry 状态回写（评审通过→✅ 备注含均分；判退→❌ 或退回实现登记）、挂起区积压清单清零、iteration-log/session-state 归位

### AC1 — web/017
> Given RED/GREEN commit 实证。When 评审。Then 4 条 AC 核对+当前代码对照+五维打分+结论入轮报告。

**用例**：`AC1 ← 轮报告 §1`

### AC2 — web/024（考古风险项）
> Given 09-03 窗口无独立 feat commit。When 考古全窗口。Then 有证据→核对 3 条 AC 打分；无证据→判退回并登记 registry ❌/退回状态。

**用例**：`AC2 ← 轮报告 §2（含考古过程记录）`

### AC3 — web/030/031/032
> When 三项逐项考古+AC 核对+当前功能探针。Then 各自五维打分+结论。

**用例**：`AC3 ← 轮报告 §3/§4/§5`

### AC4 — 共享门禁与台账归位
> 门禁一次跑三线=当前基线；五项 registry 回写、积压清单清零、上下文归位。

**用例**：`AC4 ← 门禁实测记录 + registry/session-state diff`

## 测试清单

> 评审轮无新测试；「测试」=共享门禁三线+各项目功能探针（接口存活/配置存活/代码对照）。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | 共享门禁：`mvn -q verify` 期望 154 4F+2E=基线 | AC4 | ✓ |
| 2 | 共享门禁：前端三件套期望=基线（lint124/20f\|288p/build ✓） | AC4 | ✓ |
| 3 | 共享门禁：`bash scripts/smoke.sh` 期望 61=50✅+9❌+2⏭️ | AC4 | ✓ |
| 4 | 各项功能探针（017 弹层主题代码/024 图标尺寸/030 接口/031 loading/032 回退配置当前存活） | AC1/AC2/AC3 | ✓ 五项全存活（031 :loading=submitting、030 双端接口、032 vite+nginx、024 audit 1/1、017 spec 绿） |

## RED 证据

评审轮不适用（无实现段）。

## 门禁与冒烟记录

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E | 154 4F+2E 六条逐条=基线 | ✓ PASS |
| `npm run lint` | EXIT=124 | EXIT=124 | ✓=基线 |
| `npm run test` | 20 failed\|288 passed | 20 failed\|288 passed (308) | ✓=基线 |
| `npm run build` | ✓ | ✓ built in 17.46s | ✓=基线 |
| `bash scripts/smoke.sh` | 61=50✅+9❌+2⏭️ | 61=50✅+9❌+2⏭️ 逐条=基线 | ✓ PASS |

## 拆分说明

预估验收标准 4 条（=上限）；无文件变更预算（评审轮只改 harness 回写文件，不计熔断）。**处置规则**：
- 五项中任何一项判退回（FAIL）不阻塞其余四项评审（逐项独立结论）
- web/024 判退回时 registry 记 ❌/退回并登记待实现；**不自行补实现**
- 评分参照 review-criteria；历史提交形态（非三段式）按「测试证据存在性」评 TDD 维度，考古过程写入报告
- 轮报告格式：一份报告五个小节（各节=AC 核对/考古摘要/五维评分/结论），总表汇总五项均分

**研究项（Evaluator 执行中）**：
- R1 web/024 考古：`git log --since=2026-09-01 --until=2026-09-05` 全窗口+`git log -S "16" -- ProfileView.vue`/`-S "header-icon"` 等内容检索；结论定 AC2 走向
- R2 各 sprint 原始 AC：030 有 task 快照（可找 061 期 plan commit，窗口 09-08~09-09 查 task.md）；031/032 从 registry 备注重建；017 从 registry+挂起区
- R3 当前功能探针：五项实现是否仍存活于 HEAD（防后续 sprint 回退）
- R4 共享门禁按当前 HEAD（f110179 后无新改动）三线跑一次

## 交付物（评审轮）

1. 轮报告（五小节+汇总表）写入 task 评审记录
2. registry 五项状态回写 + 挂起区积压清单清零
3. session-state/iteration-log 归位（回写后→Planner 排后续：023 实现或 036b/c 等）

## 评审记录

**2026-09-29 Evaluator（sprint-080 积压评审轮·第一轮）：五项全部 PASS（9.3/8.4/9.2/9.1/9.1），积压清零**

### 汇总表
| # | 功能点 | sprint | 需求 | TDD | 测试 | 文档 | 诚实 | 均分 | 结论 |
|---|--------|--------|------|-----|------|------|------|------|------|
| 1 | web/017 下拉 UI 修复 | 041 | 9.5 | 9 | 9.5 | 9 | 9.5 | **9.3** | PASS→✅ |
| 2 | web/024 图标尺寸 | 048 | 9 | 7.5 | 9 | 7.5 | 9 | **8.4** | PASS→✅（考古风险登记） |
| 3 | web/030 权限下拉 404 | 061 | 9.5 | 8.5 | 9 | 9.5 | 9.5 | **9.2** | PASS→✅ |
| 4 | web/031 抽屉按钮 | 062 | 9.5 | 8.5 | 9.5 | 8.5 | 9.5 | **9.1** | PASS→✅ |
| 5 | web/032 刷新 404 | 063 | 9.5 | 8.5 | 9 | 9.5 | 9 | **9.1** | PASS→✅ |

### 共享门禁（当前 HEAD 亲跑一次服务五项，veto 6 豁免基线对照）
- mvn **154 4F+2E** 六条逐条=基线 ✓；前端 lint124 / **20 failed|288 passed** / build ✓（diff=0）✓；冒烟 **61=50✅+9❌+2⏭️** 逐条=基线 ✓

### §1 web/017（sprint-041）9.3 PASS
- AC 核对：4 条由 `dropdown-style.spec.ts`（RED 交付 97 行）源级断言 AC1-4 全绿亲跑 ✓（图标 16px/弹层主题化/min-width/分隔线）；当前态 Header dropdown 存活
- 考古：`fe0e402`(RED)+`a232011`(GREEN：Header+element-plus.scss+smoke) 完整两段，缺 docs 段（当时惯例）
- 扣分：文档 9（task 快照被后续覆盖、原 registry「规划中」滞后至本单勘误）

### §2 web/024（sprint-048）8.4 PASS（**考古风险项，判退规则未触发**）
- **判退规则核验**：有实现证据（当前态 Breadcrumb 13×13、`profile__header-icon` 20×20 存活；`main-area-icon-size-audit.spec` 1/1 绿；049/web025 评审通过 9.8 的依赖链）→ **不判退**
- 考古实录：**无独立 feat commit**——09-03 窗口 task=model/010，`ProfileView.vue`→`profile/IndexView.vue` 重构与 audit spec 首版均被 `1d934de`（09-09 大杂烩）吸收；`header-icon` 内容检索追溯至 `2ac5581`（09-02）——**提交证据链断裂，实现以当前态考古认定**
- 扣分：TDD 7.5（无 RED/GREEN 独立痕迹、测试后补入库嫌疑）、文档 7.5（task 快照丢失、registry 旧路径 ProfileView 失效）

### §3 web/030（sprint-061）9.2 PASS
- **task 快照完整**：`1d934de:.harness/context/task.md` 状态 AWAITING_REVIEW（从未评审的台账实证）
- AC 核对：4 条；接口双端存活（`ModuleController:151` + `api/module.ts:36`）；task 记「测试先行 4/4」在 154 基线内全绿
- 扣分：TDD 8.5（实现混入 1d934de 大杂烩、无独立 RED commit）

### §4 web/031（sprint-062）9.1 PASS
- AC 核对：3 条；**`:loading="submitting"` 实证**（`UserFormDrawer.vue:288`，提交状态三态 :29/:111/:132/:141，原「绑抽屉可见性」缺陷已修）；`UserFormDrawer.spec.ts` **4/4 亲跑绿**（registry「测试先行 4/4」吻合）
- 扣分：TDD 8.5（混入 `c49bed0` 提交）、文档 8.5（062 task 快照被 063 覆盖、AC 从 registry 重建）

### §5 web/032（sprint-063）9.1 PASS
- **task 快照完整**：`c49bed0:.harness/context/task.md`（自标 DONE——当时惯例，未评审是核心事实，本单补评审关闭）
- AC 核对：3 条；配置双存活（`vite.config.ts:43 historyApiFallback`、`nginx.conf:29 try_files`）；task 记核心路由测试 45/45
- 扣分：TDD 8.5（独立 test commit 缺、混 c49bed0）、诚实 9（自标 DONE 越权为当时惯例）

### 结论
**五项全部 PASS**，Veto 1-6 均不命中（历史提交形态按「测试证据存在性」口径评 TDD 维度——工作单授权）。**积压清单清零**（029 已过+五项本单关闭+023 归待实现）。

### 问题列表
- P1（历史债）web/024 提交证据链断裂（无独立实现 commit，被 2ac5581/1d934de 两次大杂烩吸收）——登记为台账考古警示，后续审计以本报告为证据
- P2（历史债）041/048/061/062 四期 task 快照被后续覆盖（task.md 单文件制式之弊）——评审考古成本高
- P3（历史债）061/062/063 提交非三段式（`1d934de`/`c49bed0` 大杂烩）——当时流程未强制

### 改进建议
- task.md 单文件覆盖制式：历史快照仅能靠 `git show <commit>:.harness/context/task.md` 考古——建议关键节点（plan/eval）提交成为惯例已满足，**AWAITING_REVIEW 状态的 docs 提交**必须落（062 缺）
- 下轮排期：web/023（sprint-047 待实现）、IndexView 拆分、036b/c、011、modules/003
