# 工作单（当前 Sprint）：sprint-005

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
- Sprint ID: sprint-005
- 所属模块: model
- 需求描述: sys_role 表迁移+实体+Mapper（registry ID：model/002）
- 业务背景: RBAC 角色实体落地，用户获得权限的唯一路径是角色（架构 §6.2），本表是后续 users/002 角色分配与 roles/002 权限分配的挂靠主体。契约（§3）：id、name、code UNIQUE、status、created_at、updated_at。**规格留白的 Planner 定夺（供评审仲裁）**：§3 未标注 name 的空性约束，定夺 `name NOT NULL`——无名角色无法参与鉴权聚合与展示。基础设施全部复用既有交付：V2 为第二个业务迁移（V1 已被 sys_user 占用）、@MapperScan 已扫描 com.authcore.mapper、MetaObjectHandler 时间填充已就位。
- 前置依赖: infra/002（✅）；建议在 model/001 之后实施以保持迁移序可读（非硬前置）
- 状态: AWAITING_REVIEW

## 验收标准
<!-- 每条映射测试用例；Generator 按 tdd-workflow 先写测试确认 RED 再最小实现 -->

- [x] AC1 迁移可重复应用：真实库上 spring.flyway.enabled=true 启动后 Flyway 成功应用 V2（flyway_schema_history 存在 version='2' 且 success=1 恰一条），重放 migrate 不报错不新增记录 ← 用例 `SysRoleModelIntegrationTest#test_迁移_V2首次应用成功且重放幂等`
- [x] AC2 表结构符合 §3 契约：information_schema 断言 sys_role 存在且含 id/name/code/status/created_at/updated_at 六列，name/code 均 NOT NULL，code 存在唯一索引 uk_sys_role_code ← 用例 `SysRoleModelIntegrationTest#test_表结构_契约列与唯一键齐备`
- [x] AC3 Mapper CRUD 主线：insert 后 selectById 字段逐项一致，updateById 生效，deleteById 物理删除后不可再查；createdAt/updatedAt 自动填充非空 ← 用例 `SysRoleModelIntegrationTest#test_增查改删主线与时间自动填充`（@Transactional 回滚隔离）
- [x] AC4 code 全局唯一由数据库保证（架构 §6.3）：同事务插入重复 code 抛出 DuplicateKeyException ← 用例 `SysRoleModelIntegrationTest#test_重复code_触发唯一约束异常`

## 实现要点（Planner 提示，Generator 裁量落地）
- DDL 口径：id BIGINT AUTO_INCREMENT PK、name VARCHAR(64) NOT NULL、code VARCHAR(64) NOT NULL + uk_sys_role_code、status TINYINT NOT NULL DEFAULT 1、InnoDB/utf8mb4；时间列同 V1 口径
- 测试沿用 sprint-004 模式：共享开发库 192.168.165.88/authcore，写路径 @Transactional 回滚、测试数据 `__it_` 前缀；Evaluator 复跑需环境含 MYSQL_PASSWORD（已注入 ~/.bashrc）
- 冒烟增量：追加 1 条定向用例运行 `SysRoleModelIntegrationTest`（用例数递增至 5）

## 测试清单
1. `SysRoleModelIntegrationTest#test_迁移_V2首次应用成功且重放幂等` —— version='2' success=1 恰一条 + 重放不新增（覆盖 AC1）
2. `SysRoleModelIntegrationTest#test_表结构_契约列与唯一键齐备` —— 六列/name+code NOT NULL/uk_sys_role_code（覆盖 AC2）
3. `SysRoleModelIntegrationTest#test_增查改删主线与时间自动填充` —— @Transactional 回滚隔离 CRUD + 时间填充非空（覆盖 AC3）
4. `SysRoleModelIntegrationTest#test_重复code_触发唯一约束异常` —— 同事务重复插入断言 DuplicateKeyException（覆盖 AC4）

## RED 证据
<!-- Generator 先写测试运行确认失败后实时摘录 -->
```text
[RED] COMPILATION ERROR — SysRoleModelIntegrationTest 先行时实体与 Mapper 均不存在：
        backend/src/test/java/com/authcore/mapper/SysRoleModelIntegrationTest.java:
        [3,27] 找不到符号 类 SysRole / [28,13] 找不到符号 类 SysRoleMapper
```

## 实现说明
- 新增 `db/migration/V2__create_sys_role.sql`：六列对齐 §3，uk_sys_role_code 唯一索引；DDL 注释登记 name NOT NULL 定夺来源。
- 新增 `entity/SysRole.java` 与 `mapper/SysRoleMapper.java`：完全复用 model/001 模式（POJO + BaseMapper），零既有代码改动（@MapperScan/MetaObjectHandler 直接生效）。
- REFACTOR：零操作，重跑保持全绿。

## 冒烟记录
- Generator：本次追加 1 条用例（`model-002 sys_role 数据层与迁移定向测试`）；`bash scripts/smoke.sh` 全组通过（exit=0，`SMOKE PASSED（5 用例）`）；共享库 `__it_%` 残留 0 行。
- Evaluator 复核结论：（待 Evaluator 复跑填写）

## 评审意见
- （由 Evaluator 评审时填写）
