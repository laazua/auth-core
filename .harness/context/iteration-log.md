# 迭代日志

> 本日志为 append-only：新记录追加在最上方（倒序），格式 `YYYY-MM-DD: 角色 ID — 一句话结果（关键数据）`；禁止修改或删除既有记录。

## 记录

- 2026-08-26: Generator — done sprint-002（infra/002，后端门禁 mvn -q verify 全绿 5 用例 + 冒烟 2 用例连续 2 次通过）
- 2026-08-26: Planner — kickoff sprint-002（infra/002，4 条验收标准；用户裁决配置化接入口径并登记挂起区）
- 2026-08-26: Evaluator — pass sprint-001（infra/001，平均分 9.8/10）
- 2026-08-26: Generator — done sprint-001（infra/001，后端门禁 mvn -q verify 全绿 + 冒烟 1 用例连续 3 次通过）
- 2026-08-26: Planner — kickoff sprint-001（infra/001，3 条验收标准）
- 2026-08-26: 系统 — Harness 增补强制冒烟机制：scripts/smoke.sh 骨架 + Generator 自跑/Evaluator 复跑 + 一票否决接线，verify-harness 全绿
- 2026-08-26: 终审仲裁 — 澄清评分为五维平均分制（≥7 通过），详见 spec §5.2 勘误
- 2026-08-26: 系统 — Harness 交付完成，`scripts/verify-harness.sh` 全绿
