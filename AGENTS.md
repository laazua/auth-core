# auth-core — AI Agent 项目地图

## 会话入口规则（最高优先级）
1. 任何会话开始必须先读 `.harness/context/session-state.md`
2. 按 session-state 的「下一步动作」加载对应角色指令：
   - 规划 → 读 `.harness/planner.md` 并严格执行
   - 实现 → 读 `.harness/generator.md` 并严格执行
   - 评审 → 读 `.harness/evaluator.md` 并严格执行
3. 用户输入「继续」时，直接按上述路由执行，无需追问

## 项目
通用权限管理系统（RBAC）：用户/角色/权限/模块管理 + 关联授权 + 外部模块接入鉴权。
技术栈与业务硬语义见 `docs/01-architecture.md`；进度全景见 `.harness/modules/registry.md`。

## 硬性规则
- TDD 强制：任何业务代码必须测试先行，流程见 `.harness/rules/tdd-workflow.md`
- 禁止跳过 Evaluator；禁止自行宣布完成
- 门禁：后端 mvn -q verify；前端 npm run lint && npm run test && npm run build
- 与 `docs/01-architecture.md` 冲突时以它为准并在 session-state 登记
- 文档与交流一律中文

## 命令速查
后端：mvn -q verify / mvn spring-boot:run（backend/）
前端：npm run lint && npm run test && npm run build / npm run dev（frontend/）
环境前置：Java 21、Maven 3.9+、Node 20+、Docker（Testcontainers）、MySQL 8
Harness 自检：bash scripts/verify-harness.sh
