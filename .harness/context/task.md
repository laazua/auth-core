# 工作单（当前 Sprint）：sprint-006

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
- Sprint ID: sprint-006
- 所属模块: model
- 需求描述: sys_module 表迁移+实体+Mapper（registry ID：model/003）
- 业务背景: 模块实体落地，承载 RBAC 硬语义「权限必须归属模块」（架构 §6.1）的归属主体，也是 modules/001 模块管理与 integration 外部接入的数据基础。契约（§3）：id、name、code UNIQUE、base_url(可空,模块服务地址)、description、status、created_at、updated_at。**规格留白的 Planner 定夺（供评审仲裁）**：name NOT NULL（同 model/002 先例）；status NOT NULL DEFAULT 1；base_url 与 description 可空。另据 sprint-004/005 两轮评审建议，本功能点为集成测试样板第三次出现，达规范 §0.4 抽象时机——新增集成测试公共基座，仅本功能点起的新测试使用；已完成条目（SysUser/SysRole 测试类）按「不改已完成条目」原则保持原样。
- 前置依赖: infra/002（✅）
- 状态: AWAITING_REVIEW

## 验收标准
<!-- 每条映射测试用例；Generator 按 tdd-workflow 先写测试确认 RED 再最小实现 -->

- [x] AC1 迁移可重复应用：真实库上 spring.flyway.enabled=true 启动后 Flyway 成功应用 V3（flyway_schema_history 存在 version='3' 且 success=1 恰一条），重放 migrate 不报错不新增记录 ← 用例 `SysModuleModelIntegrationTest#test_迁移_V3首次应用成功且重放幂等`
- [x] AC2 表结构符合 §3 契约：information_schema 断言 sys_module 含 id/name/code/base_url/description/status/created_at/updated_at 八列；name/code/status 均 NOT NULL；base_url 与 description 可空；code 存在唯一索引 uk_sys_module_code ← 用例 `SysModuleModelIntegrationTest#test_表结构_契约列与约束齐备`
- [x] AC3 Mapper CRUD 主线：insert 后 selectById 字段逐项一致（含 base_url/description），updateById 生效，deleteById 物理删除后不可再查；createdAt/updatedAt 自动填充非空 ← 用例 `SysModuleModelIntegrationTest#test_增查改删主线与时间自动填充`（@Transactional 回滚隔离）
- [x] AC4 code 全局唯一由数据库保证（架构 §6.3）：同事务插入重复 code 抛出 DuplicateKeyException ← 用例 `SysModuleModelIntegrationTest#test_重复code_触发唯一约束异常`

## 实现要点（Planner 提示，Generator 裁量落地）
- DDL 口径：id BIGINT AUTO_INCREMENT PK、name VARCHAR(64) NOT NULL、code VARCHAR(64) NOT NULL + uk_sys_module_code、base_url VARCHAR(255) NULL、description VARCHAR(255) NULL、status TINYINT NOT NULL DEFAULT 1、InnoDB/utf8mb4
- 新增测试基座 `AbstractModelIntegrationTest`（com.authcore.mapper 包下，abstract 类）：承载 @SpringBootTest(flyway 启用)、JdbcTemplate/Flyway 注入、IT_PREFIX 常量与 flyway 历史/information_schema 断言辅助方法；`SysModuleModelIntegrationTest` 继承使用
- 测试隔离口径沿用：写路径 @Transactional 回滚、`__it_` 前缀；门禁/冒烟需环境含 MYSQL_PASSWORD（~/.bashrc 已注入）
- 冒烟增量：追加 1 条定向用例运行 `SysModuleModelIntegrationTest`（用例数递增至 6）

## 测试清单
1. `AbstractModelIntegrationTest` —— 新基座本体（非判定用例）：装配与辅助断言方法，由子类验证间接覆盖
2. `SysModuleModelIntegrationTest#test_迁移_V3首次应用成功且重放幂等` —— version='3' 恰一条 + 重放不新增（覆盖 AC1）
3. `SysModuleModelIntegrationTest#test_表结构_契约列与约束齐备` —— NOT NULL 六列 + 可空二列分组断言 + 唯一索引（覆盖 AC2）
4. `SysModuleModelIntegrationTest#test_增查改删主线与时间自动填充` —— @Transactional 回滚隔离 CRUD 含 base_url/description 字段比对（覆盖 AC3）
5. `SysModuleModelIntegrationTest#test_重复code_触发唯一约束异常` —— 同事务重复插入断言 DuplicateKeyException（覆盖 AC4）

## RED 证据
<!-- Generator 先写测试运行确认失败后实时摘录 -->
```text
[RED] COMPILATION ERROR — SysModuleModelIntegrationTest 先行时实体与 Mapper 均不存在（基座 AbstractModelIntegrationTest 编译通过）：
        backend/src/test/java/com/authcore/mapper/SysModuleModelIntegrationTest.java:
        [3,27] 找不到符号 类 SysModule / [23,13] 找不到符号 类 SysModuleMapper
```

## 实现说明
- 新增 `db/migration/V3__create_sys_module.sql`：八列对齐 §3（base_url/description 可空），uk_sys_module_code 唯一索引，DDL 注释登记定夺来源。
- 新增 `entity/SysModule.java`、`mapper/SysModuleMapper.java`：沿用既有模式。
- 新增测试基座 `AbstractModelIntegrationTest.java`：@SpringBootTest(flyway) 上移至抽象基类、JdbcTemplate/Flyway 注入、IT_PREFIX、countFlywaySuccess/countColumnsWithNullability/countUniqueIndexes 三个辅助断言；本功能点起消除 model 层测试样板重复（承接 sprint-004/005 评审建议）。已完成条目未做任何改动。
- REFACTOR：零操作，重跑保持全绿。

## 冒烟记录
- Generator：本次追加 1 条用例（`model-003 sys_module 数据层与迁移定向测试`）；`bash scripts/smoke.sh` 全组通过（exit=0，`SMOKE PASSED（6 用例）`）；共享库 `__it_%` 残留 0 行。
- Evaluator 复核结论：（待 Evaluator 复跑填写）

## 评审意见
- （由 Evaluator 评审时填写）
