# 工作单模板（task.md）

> 每个 Sprint 一份工作单：由 Planner 从下方「空槽实例」复制并填写，是 Generator 实现与 Evaluator 验收的唯一工作契约。字段语义以本表为准，ID 体系见 `.harness/modules/registry.md`。

## 模板字段定义

| 字段 | 填写者 | 说明 |
|------|--------|------|
| Sprint ID | Planner | 本工作单的全局唯一标识（如 `sprint-001`），用于日志与评审引用 |
| 所属模块 | Planner | 对应 `.harness/modules/registry.md` 中的模块名（infra/model/auth/users/roles/perms/modules/web/integration） |
| 需求描述 | Planner | 本 Sprint 要实现的功能点一句话描述，须与 registry 行语义一致 |
| 业务背景 | Planner | 为什么做：业务价值、RBAC 语义依据（仲裁见 `docs/01-architecture.md`） |
| 前置依赖 | Planner | 依赖的 registry 功能点 ID 列表（如 `model/001`）；无则填「—」 |
| 验收标准 | Planner | 可判定的验收条目清单；每条须映射测试用例（条目 ↔ 测试一一对应） |
| 测试清单 | Generator | 从验收标准导出的用例列表，先于实现写出（规则见 `.harness/rules/tdd-workflow.md`） |
| RED 证据 | Generator | 测试先行的失败输出摘录，格式须符合 `.harness/rules/tdd-workflow.md` 规定 |
| 实现说明 | Generator | 最小实现的落点摘要：改了哪些类/文件、为何这样改、重构内容 |
| 冒烟记录 | Generator 记录；Evaluator 复核 | 本次追加的冒烟用例数与 `scripts/smoke.sh` 执行结果（Evaluator 侧记复跑结论） |
| 状态 | Planner 初填；Generator/Evaluator 流转更新 | 枚举流转：PLANNED→IN_PROGRESS→AWAITING_REVIEW→REWORK→DONE｜BLOCKED |
| 评审意见 | Evaluator | 按 `.harness/rules/review-criteria.md` 五维评分的结论与整改要求 |

> 状态枚举说明：PLANNED 已规划未开工；IN_PROGRESS 实现中；AWAITING_REVIEW 待 Evaluator 评审；REWORK 评审退回返工；DONE 评审通过；BLOCKED 被依赖或评审阻塞（连续 2 次 REWORK 不通过即置 BLOCKED 并登记挂起区）。

---

## 空槽实例（复制以下内容开始一个 Sprint）

````markdown
# Sprint 工作单：<Sprint ID>

## 基本信息
- Sprint ID: 由 Planner 在 kickoff 时填写
- 所属模块: 由 Planner 在 kickoff 时填写
- 需求描述: 由 Planner 在 kickoff 时填写
- 业务背景: 由 Planner 在 kickoff 时填写
- 前置依赖: 由 Planner 在 kickoff 时填写
- 状态: PLANNED

## 验收标准
<!-- 每条须映射测试用例；格式示例：AC1 <可判定条目> ← 用例 <测试类#方法> -->
- 由 Planner 在 kickoff 时填写

## 测试清单
- 由 Generator 在 RED 阶段从验收标准导出后填写

## RED 证据
<!-- 先写测试并运行确认失败后摘录，格式如下 -->
```text
[RED] Tests run: X, Failures: Y — com.authcore.xxx.XxxTest.test行为: expected:<...> but was:<...>
```

## 实现说明
- 由 Generator 实现/重构完成后填写

## 冒烟记录
- 由 Generator 在第 6 步（全量回归通过后）填写（本次追加用例数 + `bash scripts/smoke.sh` 结果）；Evaluator 复核时附复跑结论

## 评审意见
- （由 Evaluator 评审时填写）
````
