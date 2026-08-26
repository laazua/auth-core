# 工作单（当前 Sprint）：sprint-004

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
- Sprint ID: sprint-004
- 所属模块: model
- 需求描述: sys_user 表迁移+实体+Mapper（registry ID：model/001）
- 业务背景: 数据层六实体之首（`docs/01-architecture.md` §3 契约字段不得增删改名）：id PK、username UNIQUE NOT NULL、password(BCrypt) NOT NULL、nickname/email/phone 可空、status(1启用/0停用)、created_at/updated_at。表结构变更一律走 Flyway `V{n}__{描述}.sql`（本 Sprint 落 **V1__create_sys_user.sql**，为全库首个业务迁移）；唯一性由唯一索引保证而非仅应用层校验（规范 §4）；时间戳经 MetaObjectHandler 自动填充（规范 §4 表设计必备配套）。用户裁决（2026-08-26）：暂沿用旧口令不轮换，凭据已经仓库外 ~/.bashrc 注入 MYSQL_PASSWORD，真实库 192.168.165.88/authcore（MySQL 8.0.45）可用于验收。
- 前置依赖: infra/002（✅ sprint-002）
- 状态: AWAITING_REVIEW

## 验收标准
<!-- 每条映射测试用例；Generator 按 tdd-workflow 先写测试确认 RED 再最小实现 -->

- [x] AC1 迁移可重复应用：真实库上以 spring.flyway.enabled=true 启动上下文后 Flyway 成功应用 V1，flyway_schema_history 存在 version='1' 且 success=1 的记录；再次启动上下文重放不报错且不产生新版本记录 ← 用例 `SysUserModelIntegrationTest#test_迁移_首次应用成功且重放幂等`
- [x] AC2 表结构符合 §3 契约：information_schema 断言 sys_user 存在，含 id/username/password/nickname/email/phone/status/created_at/updated_at 九列，password 与 status 为 NOT NULL，username 存在非唯一属性的唯一索引 uk_sys_user_username ← 用例 `SysUserModelIntegrationTest#test_表结构_契约列与唯一键齐备`
- [x] AC3 Mapper CRUD 主线：insert 后 selectById 字段逐项一致，updateById 生效，deleteById 物理删除后不可再查；createdAt/updatedAt 由 MetaObjectHandler 自动填充非空 ← 用例 `SysUserModelIntegrationTest#test_增查改删主线与时间自动填充`（@Transactional 回滚隔离，不在共享开发库留数据）
- [x] AC4 username 唯一性由数据库保证：同事务内插入重复 username 抛出 DuplicateKeyException ← 用例 `SysUserModelIntegrationTest#test_重复username_触发唯一约束异常`

## 实现要点（Planner 提示，Generator 裁量落地）
- DDL 口径：id BIGINT AUTO_INCREMENT 主键、password VARCHAR(100)（BCrypt 60 字符留裕量）、status TINYINT DEFAULT 1、InnoDB + utf8mb4；禁止 is_ 前缀布尔列（规范 §4）
- 实体为常规 POJO（规范 §2，MyBatis-Plus 兼容），camelCase↔snake_case 依赖 MP 默认映射；时间字段 LocalDateTime + @TableField(fill=...)
- `@MapperScan("com.authcore.mapper")` 加在既有 `MybatisPlusConfig` 上（避免为此改动启动类）
- 集成测试作用于共享开发库：写路径必须 @Transactional 回滚隔离，读路径只读；测试用户名使用可辨识前缀（如 `__it_`）
- 门禁与冒烟运行前置：环境须含 MYSQL_PASSWORD（已注入 ~/.bashrc，交互 shell 自带；非交互执行需自行 export）
- 冒烟增量：追加 1 条定向用例运行 `SysUserModelIntegrationTest`（即 tdd-workflow 冒烟节 model 形态「Flyway 真实库迁移可重复应用」的落位）

## 测试清单
1. `SysUserModelIntegrationTest#test_迁移_首次应用成功且重放幂等` —— flyway_schema_history version='1' success=1 恰一条 + Flyway#migrate 重放不新增（覆盖 AC1，只读+重放）
2. `SysUserModelIntegrationTest#test_表结构_契约列与唯一键齐备` —— information_schema 九列/NOT NULL/uk_sys_user_username non_unique=0（覆盖 AC2，只读）
3. `SysUserModelIntegrationTest#test_增查改删主线与时间自动填充` —— @Transactional 回滚隔离 CRUD 主线 + createdAt/updatedAt 非空（覆盖 AC3）
4. `SysUserModelIntegrationTest#test_重复username_触发唯一约束异常` —— 同事务重复插入断言 DuplicateKeyException（覆盖 AC4）

## RED 证据
<!-- Generator 先写测试运行确认失败后实时摘录 -->
```text
[RED] COMPILATION ERROR — SysUserModelIntegrationTest 先行时实体与 Mapper 均不存在：
        backend/src/test/java/com/authcore/mapper/SysUserModelIntegrationTest.java:
        [28,13] 找不到符号 类 SysUserMapper / [42,13][43,9] 找不到符号 类 SysUser
```

## 实现说明
- 新增 `db/migration/V1__create_sys_user.sql`：全库首个业务迁移，九列严格对齐架构 §3；username 唯一索引 uk_sys_user_username；InnoDB/utf8mb4；时间列带 DB 级默认值兜底（应用层以 MetaObjectHandler 为主）。移除 db/migration/.gitkeep（目录已有真实内容）。
- 新增 `entity/SysUser.java`：常规 POJO（MP 兼容），camelCase↔snake_case 走 MP 默认映射；createdAt/updatedAt 标注 FieldFill.INSERT / INSERT_UPDATE。
- 新增 `mapper/SysUserMapper.java`：仅继承 BaseMapper，无超前自定义 SQL（YAGNI）。
- 新增 `config/MybatisPlusMetaObjectHandler.java`：strictInsertFill/strictUpdateFill 时间字段。
- 修改 `config/MybatisPlusConfig.java`：加 `@MapperScan("com.authcore.mapper")`（按工作单要点，未动启动类）。
- 测试隔离口径落实：写用例 @Transactional 回滚，读用例只读；测试数据统一 `__it_` 前缀。真实库验证通过（MySQL 8.0.45 @192.168.165.88/authcore）。
- REFACTOR：零操作，重跑保持全绿。

## 冒烟记录
- Generator：本次追加 1 条用例（`model-001 sys_user 数据层与迁移定向测试`）；`bash scripts/smoke.sh` 全组通过（exit=0，`SMOKE PASSED（4 用例）`）。
- Evaluator 复核结论：（待 Evaluator 复跑填写）

## 评审意见
- （由 Evaluator 评审时填写）
