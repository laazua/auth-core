# 工作单（当前 Sprint）：sprint-001

> 本文件是 Generator 实现与 Evaluator 验收的唯一工作契约。字段语义见下方「模板字段定义」；空槽模板与历史工作单见 git 历史。

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

## 基本信息
- Sprint ID: sprint-001
- 所属模块: infra
- 需求描述: Maven 项目骨架 + Spring Boot 启动 + actuator 健康检查（registry ID：infra/001）
- 业务背景: 后端承载骨架是全链关键路径起点——infra/002（MySQL+MyBatis-Plus+Flyway）、infra/003（统一响应体）及全部业务 API 均依赖一个可启动、可通过健康检查探活的 Spring Boot 单模块工程。技术口径依据 `docs/01-architecture.md` §1（Java 21 + Spring Boot 3.x Web/Validation/Actuator 固化选型）与 §2（monorepo 仅 `backend/`+`frontend/`，后端根包 `com.authcore`，启动类 `AuthCoreApplication`，七个固定业务包）。
- 前置依赖: —（registry 中无前置；本功能点即关键路径起点）
- 状态: AWAITING_REVIEW

## 验收标准
<!-- 每条映射测试用例；Generator 按 tdd-workflow 先写测试确认 RED 再最小实现 -->

- [x] AC1 `cd backend && mvn -q verify` 构建成功（退出码 0）；pom.xml 以 spring-boot-starter-parent 3.x 为父级、java.version=21，引入 web/validation/actuator 三个 starter，且为单 Maven 模块 ← 用例 `AuthCoreApplicationTests#contextLoads`（随构建运行的上下文装配判定）
- [x] AC2 应用上下文启动后请求 GET /actuator/health 返回 HTTP 200，响应体 JSON 中 status="UP" ← 用例 `HealthEndpointApiTest#test_get_actuator_health_返回200且status为UP`
- [x] AC3 仓库根目录执行 `bash scripts/smoke.sh` 全部用例通过（任何失败非零退出），其中包含本次新增「应用可启动且 /actuator/health 为 UP」冒烟用例，且幂等可重复 ← 用例 `scripts/smoke.sh#case_app_starts_and_health_up`

## 测试清单
1. `AuthCoreApplicationTests#contextLoads` —— @SpringBootTest 容器装配判定（覆盖 AC1）
2. `HealthEndpointApiTest#test_get_actuator_health_返回200且status为UP` —— @SpringBootTest + MockMvc，断言 GET /actuator/health 返回 200 且 $.status=UP（覆盖 AC2）
3. `scripts/smoke.sh#case_app_starts_and_health_up` —— 冒烟用例：后台启动应用→轮询 curl /actuator/health 断言 status=UP→清理进程（覆盖 AC3，随 GREEN 交付追加）

## RED 证据
<!-- 实测时间 2026-08-26，命令 cd backend && mvn test（主代码为零时运行） -->
```text
[RED] Tests run: 2, Failures: 0, Errors: 2 — BUILD FAILURE
[RED] com.authcore.AuthCoreApplicationTests.contextLoads: IllegalStateException: Unable to find a @SpringBootConfiguration by searching packages upwards from the test.
[RED] com.authcore.controller.HealthEndpointApiTest.test_get_actuator_health_返回200且status为UP: IllegalStateException: Unable to find a @SpringBootConfiguration by searching packages upwards from the test.
```

## 实现说明
- 新增 `backend/pom.xml`：继承 spring-boot-starter-parent 3.5.5（对齐架构 §1 Spring Boot 3.x 固化选型），`java.version=21`，依赖仅 web/validation/actuator 三 starter + test；spring-boot-maven-plugin 负责运行与打包。未提前引入 MyBatis-Plus/Flyway/Security（属 infra/002、auth/001，YAGNI）。
- 新增 `com.authcore.AuthCoreApplication`：本功能点唯一主代码；启动类位于架构规定的根包，组件扫描天然覆盖七个业务包；actuator health 默认暴露于 `/actuator/health`，无需任何附加配置类。
- 未新增 application.yml：默认配置已满足全部验收标准，避免无消费的配置项。
- 冒烟用例首跑失败的根因是宿主机 v2ray 服务占用 8080（环境因素而非交付缺陷），修正为专用端口 18080 启动 + setsid 进程组清理 + 启动前端口预检，保证幂等可重复。
- REFACTOR：骨架阶段无重复逻辑与坏味道可消除，重构为零操作，重构后全量重跑保持全绿。

## 冒烟记录
- Generator：本次追加 1 条用例（`infra-001 应用启动且 /actuator/health 为 UP`）；`bash scripts/smoke.sh` 连续 3 次运行全部通过（exit=0，输出 `SMOKE PASSED（1 用例）`），幂等性已实证。
- Evaluator 复核结论：（待 Evaluator 复跑填写）

## 评审意见
- （由 Evaluator 评审时填写）
