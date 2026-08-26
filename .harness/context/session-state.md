# 会话状态
> 本文件是跨会话交接的唯一真相源。任何会话开始必须先读本文件，按「下一步动作」路由（规则见 `AGENTS.md`）。

## 当前阶段
- 状态: 🟡 sprint-001 待评审（AWAITING_REVIEW）
- 当前模块: infra
- 当前功能点: infra/001

## 下一步动作
Evaluator：按 `.harness/rules/review-criteria.md` 五维评审 sprint-001（工作单：`.harness/context/task.md`）。实测口径——后端门禁 `mvn -q verify` 必须亲自复跑并贴输出；冒烟 `bash scripts/smoke.sh` 必须亲自复跑，并以 git diff 取证 smoke.sh 用例区相对基线新增 1 条；前端门禁本 Sprint 暂缓适用（理由见挂起区登记，自 web/001 起恢复双门禁）。

## 挂起
- 2026-08-26: Generator(sprint-001) — 前端门禁暂缓适用：`frontend/` 目录属 web/001 范围尚未创建，infra 阶段仅后端可验证；非架构冲突，自 web/001 交付起恢复「后端+前端」双门禁口径。

## 最近更新
- 2026-08-26: 系统 — Harness 增补强制冒烟机制（smoke.sh 接线六处流程，registry 状态不受影响仍全 ⬜）
- 2026-08-26: 系统 — Harness 交付，registry 32 个功能点全部 ⬜
