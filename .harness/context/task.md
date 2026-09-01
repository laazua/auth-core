# Sprint 工作单：sprint-033

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-033 |
| 所属模块 | infra |
| 功能点 ID | infra/004 |
| 功能点名称 | 测试基础设施（Testcontainers MySQL 基座 + 测试命名/分层约定） |
| 状态 | PLANNED |
| 创建时间 | 2026-09-01 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| infra/002 | MySQL 接入 + MyBatis-Plus 配置 + Flyway 迁移机制 | ✅ |

## 业务背景

当前后端测试缺乏真实数据库环境，单元测试依赖 Mock 或 H2，集成测试无法验证真实 MySQL 行为（Flyway 迁移、事务、锁、索引等）。需要引入 Testcontainers MySQL 作为测试基座，建立统一的测试命名与分层约定，为后续所有业务模块的集成测试提供可复用的真实库环境。

## 需求描述

1. **引入 Testcontainers MySQL 依赖**：
   - 在 `backend/pom.xml` 添加 `org.testcontainers:testcontainers`、`org.testcontainers:junit-jupiter`、`org.testcontainers:mysql`、`org.testcontainers:flyway`
   - 版本由 BOM 统一管理

2. **创建测试基座抽象类 `BaseIntegrationTest`**：
   - 使用 `@Testcontainers` + `@Container` 静态 MySQLContainer
   - 配置 Flyway 自动迁移（`@AutoConfigureFlyway` 或手动 `Flyway.migrate()`）
   - 提供 `DataSource`、`JdbcTemplate`、`MyBatisPlus` 注入
   - 使用 `@DynamicPropertySource` 动态注入数据源属性

3. **建立测试命名与分层约定**：
   - 单元测试：`*Test.java`，仅测单个类，依赖全部 Mock，放在 `src/test/java/.../unit/`
   - 集成测试：`*IntegrationTest.java`，继承 `BaseIntegrationTest`，真实库，放在 `src/test/java/.../integration/`
   - 控制器测试：`*ControllerTest.java`，使用 `MockMvc` + 真实库，放在 `src/test/java/.../controller/`
   - 命名规范：`Given_When_Then` 或 `should_<expected>_when_<condition>`

4. **提供通用测试工具**：
   - `TestDataBuilder`：Builder 模式构建测试实体
   - `JsonTestUtil`：JSON 序列化/断言工具
   - `AuthTestUtil`：快速获取 JWT、模拟登录

## 验收标准（TDD 驱动）

### AC1 — Testcontainers MySQL 容器启动与 Flyway 迁移
> 运行任意继承 `BaseIntegrationTest` 的测试类，MySQL 容器自动启动，Flyway 迁移自动应用，数据源可用。

**用例**：`BaseIntegrationTestSpec#containerStartsAndFlywayMigrates`
- 容器启动成功，端口可连接
- Flyway 迁移版本达到最新（`sys_user` 等 6 表存在）

### AC2 — 单元/集成/控制器测试三层分离可运行
> 三类测试均能独立运行，互不干扰，产物目录隔离。

**用例**：`TestLayersSpec#unitTestRunsWithMocks`
- 单元测试不启动容器，Mock 生效，< 500ms

**用例**：`TestLayersSpec#integrationTestUsesRealDB`
- 集成测试启动容器，真实写入/查询，回滚隔离

**用例**：`TestLayersSpec#controllerTestUsesMockMvc`
- 控制器测试用 `MockMvc` 发请求，真实库校验

### AC3 — 通用测试工具可用
> `TestDataBuilder`、`JsonTestUtil`、`AuthTestUtil` 在集成测试中正常工作。

**用例**：`TestUtilsSpec#buildUserEntity`
- `TestDataBuilder.user().withUsername("x").build()` 生成合法 `SysUser`

**用例**：`TestUtilsSpec#obtainJwt`
- `AuthTestUtil.obtainJwt("admin")` 返回有效 Bearer Token

### AC4 — Maven 测试命令全绿
> `mvn -q test` 执行所有测试（单元+集成+控制器）通过，无冲突。

**用例**：`MavenTestSpec#allTestsPass`
- 单元测试、集成测试、控制器测试全部通过
- 无端口冲突、无数据污染、无 Flyway 校验失败

## 交付物

1. `backend/pom.xml` — 新增 Testcontainers 依赖
2. `backend/src/test/java/com/authcore/BaseIntegrationTest.java` — 测试基座抽象类
3. `backend/src/test/java/com/authcore/test/unit/` — 单元测试包（空目录占位）
4. `backend/src/test/java/com/authcore/test/integration/` — 集成测试包（空目录占位）
5. `backend/src/test/java/com/authcore/test/controller/` — 控制器测试包（空目录占位）
6. `backend/src/test/java/com/authcore/util/TestDataBuilder.java` — 测试数据构建器
7. `backend/src/test/java/com/authcore/util/JsonTestUtil.java` — JSON 测试工具
8. `backend/src/test/java/com/authcore/util/AuthTestUtil.java` — 认证测试工具
9. `scripts/smoke.sh` — 追加后端测试基座冒烟用例

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | containerStartsAndFlywayMigrates | AC1 | ⬜ |
| 2 | unitTestRunsWithMocks | AC2 | ⬜ |
| 3 | integrationTestUsesRealDB | AC2 | ⬜ |
| 4 | controllerTestUsesMockMvc | AC2 | ⬜ |
| 5 | buildUserEntity | AC3 | ⬜ |
| 6 | obtainJwt | AC3 | ⬜ |
| 7 | allTestsPass | AC4 | ⬜ |

## RED 证据

（此处留空，Generator 阶段填写）

## GREEN 证据

（此处留空，Generator 阶段填写）

## 冒烟记录

（此处留空，Generator/Evaluator 阶段填写）

## 规范检查清单（Evaluator 逐项核对）

- [ ] Testcontainers MySQL 依赖引入正确（BOM 版本管理）
- [ ] BaseIntegrationTest 启动容器、Flyway 迁移、动态属性注入
- [ ] 三层测试目录结构建立（unit/integration/controller）
- [ ] 命名约定文档化（README 或包内 package-info.java）
- [ ] TestDataBuilder/JsonTestUtil/AuthTestUtil 实现完整
- [ ] mvn -q test 全绿，无端口冲突/数据污染
- [ ] 冒烟脚本追加 infra/004 用例并通过
- [ ] 符合 `docs/01-architecture.md` §1 测试栈选型

## 实现说明

（此处留空，Generator 阶段填写）