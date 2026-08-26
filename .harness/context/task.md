# 工作单（当前 Sprint）：sprint-007

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
- Sprint ID: sprint-007
- 所属模块: model
- 需求描述: sys_permission 表迁移+实体+Mapper(FK module_id)（registry ID：model/004）
- 业务背景: 权限实体落地，直接承载 RBAC 第一硬语义「权限必须归属模块，不存在脱离模块的权限」（架构 §6.1）——module_id NOT NULL 即该条款的数据层表达；code 全局唯一对应 §6.3。契约（§3）：id、module_id NOT NULL(逻辑外键→sys_module)、name、code UNIQUE、description、created_at、updated_at——**七字段，无 status 列**。关联口径：架构 §3 明示「逻辑外键」、规范 §4 要求 service 层维护引用完整性（删除保护在 modules/001 落地），故 DDL **不建物理 FOREIGN KEY**。**规格留白的 Planner 定夺（供评审仲裁）**：name NOT NULL、code NOT NULL、description 可空。对 sprint-006 评审建议（key_column_usage 辅助）的处置：不采纳——物理 FK 不存在，该视图无从断言；基座本轮无需增强。
- 前置依赖: model/003（✅ sprint-006）
- 状态: AWAITING_REVIEW

## 验收标准
<!-- 每条映射测试用例；Generator 按 tdd-workflow 先写测试确认 RED 再最小实现 -->

- [x] AC1 迁移可重复应用：真实库上 spring.flyway.enabled=true 启动后 Flyway 成功应用 V4（flyway_schema_history 存在 version='4' 且 success=1 恰一条），重放 migrate 不报错不新增记录 ← 用例 `SysPermissionModelIntegrationTest#test_迁移_V4首次应用成功且重放幂等`
- [x] AC2 表结构符合 §3 契约：information_schema 断言 sys_permission 含 id/module_id/name/code/description/created_at/updated_at 七列；NOT NULL 组恰为 id/module_id/name/code/created_at/updated_at 六列（含 §6.1 落地的 module_id）；description 可空；code 存在唯一索引 uk_sys_permission_code ← 用例 `SysPermissionModelIntegrationTest#test_表结构_契约列与约束齐备`
- [x] AC3 Mapper CRUD 主线：insert 后 selectById 字段逐项一致，updateById 生效，deleteById 物理删除后不可再查；createdAt/updatedAt 自动填充非空 ← 用例 `SysPermissionModelIntegrationTest#test_增查改删主线与时间自动填充`（@Transactional 回滚隔离）
- [x] AC4 code 全局唯一由数据库保证（架构 §6.3）：同事务插入重复 code 抛出 DuplicateKeyException ← 用例 `SysPermissionModelIntegrationTest#test_重复code_触发唯一约束异常`

## 实现要点（Planner 提示，Generator 裁量落地）
- DDL 口径：id BIGINT AUTO_INCREMENT PK、module_id BIGINT NOT NULL COMMENT 标注逻辑引用 sys_module.id、name VARCHAR(64) NOT NULL、code VARCHAR(64) NOT NULL + uk_sys_permission_code、description VARCHAR(255) NULL、时间列同前；InnoDB/utf8mb4；**无 status 列、无物理 FOREIGN KEY**
- 实体/Mapper/测试完全复用基座模式（AbstractModelIntegrationTest 已就位）
- 测试隔离口径沿用：@Transactional 回滚、`__it_` 前缀；门禁/冒烟需 MYSQL_PASSWORD（~/.bashrc 已注入）
- 冒烟增量：追加 1 条定向用例运行 `SysPermissionModelIntegrationTest`（用例数递增至 7）

## 测试清单
1. `SysPermissionModelIntegrationTest#test_迁移_V4首次应用成功且重放幂等` —— version='4' 恰一条 + 重放不新增（覆盖 AC1）
2. `SysPermissionModelIntegrationTest#test_表结构_契约列与约束齐备` —— NOT NULL 六列含 module_id + description 可空 + **status 列不得存在**（反增断言）+ 唯一索引（覆盖 AC2）
3. `SysPermissionModelIntegrationTest#test_增查改删主线与时间自动填充` —— @Transactional 回滚隔离 CRUD（覆盖 AC3）
4. `SysPermissionModelIntegrationTest#test_重复code_触发唯一约束异常` —— 同事务重复插入断言 DuplicateKeyException（覆盖 AC4）

## RED 证据
<!-- Generator 先写测试运行确认失败后实时摘录 -->
```text
[RED] COMPILATION ERROR — SysPermissionModelIntegrationTest 先行时实体与 Mapper 均不存在：
        backend/src/test/java/com/authcore/mapper/SysPermissionModelIntegrationTest.java:
        [3,27] 找不到符号 类 SysPermission / [24,13] 找不到符号 类 SysPermissionMapper
```

## 实现说明
- 新增 `db/migration/V4__create_sys_permission.sql`：七列对齐 §3（无 status 列），module_id NOT NULL 承载 §6.1，DDL 头注释登记逻辑外键口径与定夺来源；uk_sys_permission_code。
- 新增 `entity/SysPermission.java`（无 status 字段，Javadoc 注明）、`mapper/SysPermissionMapper.java`：基座模式第四次复用，零既有代码改动。
- REFACTOR：零操作，重跑保持全绿。

## 冒烟记录
- Generator：本次追加 1 条用例（`model-004 sys_permission 数据层与迁移定向测试`）；`bash scripts/smoke.sh` 全组通过（exit=0，`SMOKE PASSED（7 用例）`）；共享库 `__it_%` 残留 0 行。
- Evaluator 复核结论：（待 Evaluator 复跑填写）

## 评审意见
- （由 Evaluator 评审时填写）
