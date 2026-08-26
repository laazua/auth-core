# 工作单（当前 Sprint）：sprint-002

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
- Sprint ID: sprint-002
- 所属模块: infra
- 需求描述: MySQL 接入 + MyBatis-Plus 配置 + Flyway 迁移机制——**配置化接入口径**（registry ID：infra/002）
- 业务背景: 为 model 层六实体（sys_user 等，`docs/01-architecture.md` §3）落地预置持久化基座，ORM/DB/Flyway 选型固化于 §1。用户裁决（2026-08-26，登记于 session-state 挂起区）：本机不使用 Docker，数据库连接完全走配置（application.yml + 环境变量占位），真实连通与迁移应用验证延后至 MySQL 实例就绪；因此本 Sprint 全部验收用例必须离线可跑。
- 前置依赖: infra/001（✅ sprint-001 交付）
- 状态: AWAITING_REVIEW

## 验收标准
<!-- 每条映射测试用例；Generator 按 tdd-workflow 先写测试确认 RED 再最小实现 -->

- [x] AC1 `cd backend && mvn -q verify` 构建成功；pom.xml 相对上一版新增依赖仅限 mybatis-plus-spring-boot3-starter(3.5.x)、mysql-connector-j(runtime)、flyway-core、flyway-mysql 四项，无表外技术栈 ← 判定 `mvn -q verify` 门禁 + 既有用例 `AuthCoreApplicationTests#contextLoads` 保持全绿（新依赖不得破坏既有装配）
- [x] AC2 MyBatis-Plus 分页插件就位：配置类提供的 `MybatisPlusInterceptor` 内含 `PaginationInnerInterceptor` 且方言为 `DbType.MYSQL` ← 用例 `MybatisPlusConfigTest#test_分页拦截器_已注册且方言为MySQL`
- [x] AC3 数据源三要素完全外置：application.yml 中 url/username/password 均为 `${ENV:默认值}` 占位（环境变量名 MYSQL_URL / MYSQL_USERNAME / MYSQL_PASSWORD），Binder 绑定测试证明「环境变量存在时覆盖默认值」← 用例 `DataSourceConfigBindingTest#test_环境变量覆盖数据源默认配置`
- [x] AC4 Flyway 开关与位置配置化：`spring.flyway.enabled` 绑定 `FLYWAY_ENABLED`（**默认 false，保证无实例时应用可启动**）、`spring.flyway.locations` 固定 `classpath:db/migration` ← 用例 `DataSourceConfigBindingTest#test_flyway开关与迁移目录绑定`

## 实现要点（Planner 提示，Generator 裁量落地）
- `src/main/resources/db/migration/` 目录用 `.gitkeep` 占位；首个业务表迁移自 model/001 起（命名 `V{n}__{描述}.sql`，§3 规则）
- 所有测试离线可跑：禁止引入任何需要真实 MySQL 的集成测试（已延后登记）
- 密码/凭据不得出现真实值（默认占位符），符合规范第 6 节
- 冒烟增量：追加 1 条「后端构建与全量测试」用例（幂等），DB 类冒烟（Flyway 迁移应用）延后补入

## 测试清单
1. `DataSourceConfigBindingTest#test_环境变量覆盖数据源默认配置` —— 隔离模拟环境变量 + 主 yml 解析：覆盖方向与缺省回落方向双向断言（覆盖 AC3）
2. `DataSourceConfigBindingTest#test_flyway开关与迁移目录绑定` —— FLYWAY_ENABLED=true/false 两分支 + locations 固定值断言（覆盖 AC4）
3. `MybatisPlusConfigTest#test_分页拦截器_已注册且方言为MySQL` —— 配置类单测（覆盖 AC2）
4. 存量回归：`AuthCoreApplicationTests#contextLoads`、`HealthEndpointApiTest` 保持全绿（覆盖 AC1）

## RED 证据
<!-- 两个 TDD 小循环，均实测于实现存在之前（命令 cd backend && mvn test） -->
```text
[RED-A] Tests run: 2, Failures: 0, Errors: 2 — DataSourceConfigBindingTest 全错：
        java.io.FileNotFoundException: class path resource [application.yml] cannot be opened
        （test_环境变量覆盖数据源默认配置 / test_flyway开关与迁移目录绑定）
[RED-B] COMPILATION ERROR — MybatisPlusConfigTest 先行时依赖与实现均不存在：
        程序包 com.baomidou.mybatisplus.annotation/extension.plugins(.inner) 不存在；
        找不到符号 类 MybatisPlusConfig（实现类尚未创建）
```

## 实现说明
- `backend/pom.xml`：新增四项依赖——mybatis-plus-spring-boot3-starter 3.5.7（选 3.5.7 因其自含 jsqlparser 能力，避免 3.5.9+ 需额外引入 mybatis-plus-jsqlparser 模块而突破 AC1 的四项依赖口径）、mysql-connector-j(runtime)/flyway-core/flyway-mysql 三项版本交由 Boot BOM 管理。
- `application.yml`（新增）：datasource 三要素全部 `${ENV:默认}` 占位；flyway.enabled 绑定 FLYWAY_ENABLED 默认 false；management.health.db.enabled 绑定 DB_HEALTH_ENABLED 默认 false。
- `MybatisPlusConfig`（新增）：仅注册 PaginationInnerInterceptor(MYSQL)，最小实现。
- GREEN-B 过程中发现真实回归：数据源装配后 actuator 健康检查主动探测 DB → 无实例整体 503/DOWN，破坏 sprint-001 已验收的 health=UP。以同主题的配置化手段修复（DB_HEALTH_ENABLED），修复后 5/5 全绿。该开关语义已写入 yml 注释与挂起区口径：实例接入后应置 true。
- `db/migration/.gitkeep`：占位目录，注释声明 V{n}__{描述}.sql 规则与首个业务迁移归属 model/001。
- REFACTOR：零操作（各类均 ≤60 行单一职责），重构后全量重跑保持全绿。

## 冒烟记录
- Generator：本次追加 1 条用例（`infra-002 后端构建与全量测试`，mvn -q verify 幂等）；`bash scripts/smoke.sh` 连续 2 次全组通过（exit=0，输出 `SMOKE PASSED（2 用例）`）。DB 类冒烟按挂起区登记延后补入。
- Evaluator 复核结论：已亲自复跑——`SMOKE FAILED（2 用例）`，`infra-002 后端构建与全量测试` 失败（其内嵌 mvn verify 因 DataSourceConfigBindingTest 断言失败而失败）；infra-001 用例仍通过。根因见评审意见问题 2。

## 评审意见
- ❌ 不通过（2026-08-26，REWORK 第 1 次）。五维均分不满足通过线之外的独立事实：命中一票否决项 2 与 6。
- 问题清单：
  1. [严重·一票否决项2 明文密码] `backend/src/main/resources/application.yml:9` 默认口令落真实值 `abc123456`，且已随 aa4b59b 进入 git 历史——违反规范 §6「application.yml 不落真实密钥」与本工作单实现要点「凭据不得出现真实值」。→ 修复：默认值改空占位 `${MYSQL_PASSWORD:}`；凭据一律经环境变量注入；已入史的口令建议轮换（历史清理与否由用户裁决，登记挂起区）。
  2. [严重·一票否决项6 回归失败] Evaluator 亲跑 `mvn -q verify` exit=1：`DataSourceConfigBindingTest#test_环境变量覆盖数据源默认配置`(DataSourceConfigBindingTest.java:65) expected:<true> but was:<false>——交付后 yml 默认 url/username 被改为环境特定值 `192.168.165.88/root`，与 AC3 契约及测试断言不一致。→ 修复二选一并保持门禁全绿：(a) 恢复中性默认值（localhost/authcore），真实环境经 env 注入；(b) 若团队约定该库为共享开发默认，则同步修订绑定测试断言并在实现说明中登记理由。无论哪种，密码不得作为默认值入库。
  3. [非否决·登记] 变更范围核对发现 aa4b59b 夹带了非 Generator 编写的 yml 改动（提交前未复查工作区），流程教训记入返工说明：feat 提交前必须 `git status/diff` 核对暂存内容。
- 改进建议（不计分）：用户 MySQL 实例已就绪（192.168.165.88），REWORK 通过后建议 Planner 安排挂起区延后的 DB 连通 + Flyway 迁移应用验证。
- 返工记录（2026-08-26）：① application.yml 口令默认值已改 `${MYSQL_PASSWORD:}` 并加「凭据一律经环境变量注入」注释；② 绑定测试回落分支断言改为协议头+库名 authcore+用户名 root（不钉死环境特定 host，处置口径 b）；③ 本次返工提交前已用 git status/diff 复查暂存内容。复跑：mvn -q verify exit=0（5/5）+ smoke exit=0（2 用例）。
