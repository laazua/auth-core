# .harness — AI 驱动开发驾驭系统

本目录是一套可直接使用的三智能体开发驾驭系统：**Planner（规划）→ Generator（实现）→ Evaluator（评审）** 循环推进项目迭代。运行时为 CLI（Claude Code / opencode）+ DeepSeek Anthropic 兼容后端；Agent 通过读文件获得全部上下文，不依赖对话历史，每阶段切换开启新会话。

## 工作流

```
用户需求（1-4 句话）
    │
    ▼
[Planner] 查 registry 前置依赖 → 展开工作单 .harness/context/task.md
          registry 状态 ⬜→🔄，session-state 更新
    │
    ▼
[Generator] 读工作单 → TDD 红绿灯实现（RED→GREEN→REFACTOR）
            → 回归门禁 + 冒烟 scripts/smoke.sh → 提交 + 变更清单，session-state 标「待评审」
    │
    ▼
[Evaluator] 亲跑门禁命令与冒烟复跑 → 五维评分裁决
    ├─ ✅ 通过：registry 🔄→✅ → 记 iteration-log → 取下一功能点（新会话）
    └─ ❌ 不通过：问题清单写回 task.md → Generator 返工（REWORK）
              └─ 同一功能点连续 2 次 ❌ → BLOCKED 转人工
```

三个阶段各自独立会话驱动，互不共享对话记忆；跨会话交接由 `.harness/context/session-state.md` 承载（唯一真相源）。

## 文件说明

| 路径 | 用途 |
|------|------|
| `AGENTS.md` | 项目地图 + 硬性规则，CLI 自动加载的入口锚点：任何会话先读 session-state 再路由角色 |
| `.harness/planner.md` | Planner 角色指令：需求展开为单个功能点工作单，校验前置依赖，过大拆分 |
| `.harness/generator.md` | Generator 角色指令：TDD 红绿灯实现工作单，输出代码 + 变更清单 |
| `.harness/evaluator.md` | Evaluator 角色指令：反宽容独立评审，亲跑门禁，五维评分裁决 |
| `.harness/context/task.md` | 当前 Sprint 工作单（文件头含模板定义与空槽实例），Generator 与 Evaluator 的唯一工作契约 |
| `.harness/context/session-state.md` | 会话状态快照：跨会话交接唯一真相源，「下一步动作」决定路由 |
| `.harness/context/iteration-log.md` | 迭代历史，append-only 倒序追加 |
| `.harness/modules/registry.md` | 功能点注册表：预载 RBAC 全量 32 个功能点（ID / 前置依赖 / 状态） |
| `.harness/rules/tdd-workflow.md` | TDD 硬流程：RED→GREEN→REFACTOR、RED 证据格式、提交规范 |
| `.harness/rules/coding-standards.md` | 编码规范硬规则（Java/Vue 双栈），Evaluator 逐条核查 |
| `.harness/rules/review-criteria.md` | Evaluator 唯一裁决依据：五维评分 + 决策阈值 + 一票否决清单 |
| `docs/01-architecture.md` | 固化架构：选型/分层/ER/API 规范/RBAC 业务语义，一切冲突的仲裁依据 |
| `.harness/README.md` | 本文件：总览 + 驱动手册 |
| `scripts/verify-harness.sh` | Harness 自身完整性自检脚本（文件存在性/模板字段/死链等六节校验） |
| `scripts/smoke.sh` | 项目冒烟测试脚本：随功能点增量追加用例，每次迭代 Generator 自跑、Evaluator 复跑，未过即一票否决 |

上下文分三层：指令层（三个 prompt，基本不变）、规则层（rules/*、`docs/01-architecture.md`、`AGENTS.md`，低频）、状态层（context 三件套 + registry，每 Sprint 更新）。

## 首次使用

1. 配置环境变量（DeepSeek Anthropic 兼容后端；`DEEPSEEK_API_KEY` 由使用者自行提供）：

   ```bash
   export ANTHROPIC_BASE_URL=https://api.deepseek.com/anthropic
   export ANTHROPIC_AUTH_TOKEN=$DEEPSEEK_API_KEY
   export ANTHROPIC_MODEL=deepseek-chat
   ```

2. 冒烟自检（两项全过即系统就绪）：

   - 完整性自检：

     ```bash
     bash scripts/verify-harness.sh
     ```

     预期末行输出 `ALL CHECKS PASSED` 且退出码为 0。

   - Planner 干跑验证：

     ```bash
     claude "阅读 .harness/planner.md 并严格执行。需求：实现 infra/001 Maven 项目骨架与 Spring Boot 启动"
     ```

     预期产出符合模板的 `.harness/context/task.md` 工作单，且 `.harness/modules/registry.md` 中 infra/001 对应行的状态由 ⬜ 翻转为 🔄。验证后还原这些演示改动（git checkout 或手工复原），保持 registry 初始态。

3. 前置软件清单：Java 21、Maven 3.9+、Node 20+、Docker（Testcontainers 用）、MySQL 8。

## 日常迭代（每阶段独立会话）

每个阶段开一个全新会话，粘贴对应命令（尖括号处替换为你的内容）：

```text
claude "阅读 .harness/planner.md 并严格执行。需求：<...>"
claude "阅读 .harness/generator.md 并严格执行。开始当前 Sprint。"
claude "阅读 .harness/evaluator.md 并严格执行。"
```

最简驱动：不确定下一步时，直接输入 `claude "继续"` —— AGENTS.md 入口锚点会按 `.harness/context/session-state.md` 的「下一步动作」自动路由到正确角色。

## 状态机与异常处理

- 功能点状态机：⬜ 未开始 → 🔄 进行中 → ✅ 完成；旁路标记：❌ 阻塞、⚠️ 有缺口。
- 冒烟强制：功能点转 ✅ 的前置条件是回归门禁全绿且 `scripts/smoke.sh` 全部用例通过（Generator 追加用例并自跑，Evaluator 亲自复跑）。
- 评审不通过：Evaluator 将问题清单写回 task.md 并置 REWORK，Generator 返工后重新评审。
- 连续 2 次 REWORK 不通过 → registry 标 ❌ BLOCKED，登记 `.harness/context/session-state.md` 挂起区，转人工处理；人工解决后重新进入循环。
- 事故复盘：Agent 错误根因记入 `.harness/context/iteration-log.md`；若属规则缺口，则回写 `.harness/rules/` 对应文件或 `AGENTS.md` 防复发。
- 冲突仲裁：任何 Agent 发现场景与 `docs/01-architecture.md` 冲突时，以 architecture 为准并在 session-state 登记，不得静默偏离。

## 关联文档

- `docs/01-architecture.md` —— 固化技术选型与 RBAC 业务语义（最高仲裁依据）
- `AGENTS.md` —— 项目地图 + 会话入口规则 + 硬性规则速查
- 设计规格：`docs/superpowers/specs/2026-08-26-harness-system-design.md`
