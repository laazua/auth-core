# Sprint 工作单：sprint-009

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-009 |
| 所属模块 | model |
| 功能点 ID | model/006 |
| 功能点名称 | 种子数据迁移（内置 admin + 示例角色/权限/模块） |
| 状态 | PLANNED |
| 创建时间 | 2026-08-27 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| model/005 | sys_user_role+sys_role_permission 关联表 | ✅ |

## 需求描述

实现种子数据 Flyway 迁移脚本：在空库执行 V1-V6 迁移后，数据库预置可用的管理员账号、示例模块/角色/权限、关联数据，供后续 auth/API 功能开发与手工验收使用。

## 业务背景

六张表结构已就绪（V1-V5）。需要一条数据迁移脚本（V6）插入：
1. 管理员用户（username: admin，BCrypt 密码，status=1）
2. 4 个示例模块（用户管理、角色管理、权限管理、模块管理，code 唯一，status=1）
3. 每模块 3-4 个权限（共 14 个，code 唯一，module_id 关联，description 可空）
5. 2 个角色（admin 管理员、user 普通用户，code 唯一，status=1）
6. 关联：admin 用户→admin 角色；admin 角色→所有权限；user 角色→只读权限（查看类）

## 交付物

1. Flyway 数据迁移脚本：`V6__seed_data.sql`

## 验收标准（TDD 驱动）

### AC1 — 迁移脚本可应用且幂等
> Flyway 执行 V6 后，flyway_schema_history 增加 version=6 成功记录；重放 migrate 不产生重复记录。

**用例**：`SeedDataIntegrationTest#migrationV6AppliesAndIdempotent`
- 准备：V1-V5 已应用，Flyway 配置正常
- 操作：flyway.migrate()
- 断言：countFlywaySuccess("6") == 1；再次 migrate 仍为 1

### AC2 — 核心实体数据存在且字段正确
> 迁移后，`sys_user` 含 admin 用户（username=admin，password 非空 BCrypt，status=1）；`sys_module` 含 4 行（code 唯一）；`sys_role` 含 admin/user 两行；`sys_permission` 含 14 行（每行 module_id 指向有效模块）。

**用例**：`SeedDataIntegrationTest#coreEntitiesExistWithCorrectFields`
- 断言：jdbcTemplate 查询各表行数与关键字段值

### AC3 — 关联数据正确
> `sys_user_role` 含 (admin_id, admin_role_id) 一行；`sys_role_permission` 含 admin_role_id 关联全部 14 个 permission_id，user_role_id 关联只读权限（如 code 以 view: 开头）。

**用例**：`SeedDataIntegrationTest#associationsCorrect`
- 断言：user-role 关联 1 行、role-permission 关联行数与预期一致

### AC4 — 密码可被 BCrypt 验证
> admin 用户的 password 字段为有效 BCrypt 哈希，可通过 BCryptPasswordEncoder.matches("admin123456", storedHash) 验证（预置明文口令 admin123456）。

**用例**：`SeedDataIntegrationTest#adminPasswordValidBcrypt`
- 准备：注入 BCryptPasswordEncoder
- 断言：matches("admin123456", admin.password) == true

## 规范检查清单（Evaluator 逐项核对）

- [ ] 迁移脚本命名 `V6__seed_data.sql`，仅含 INSERT 语句，无 DDL
- [ ] BCrypt 密码生成逻辑在脚本中明确（或注释说明预置口令）
- [ ] 无硬编码密钥/明文密码（脚本含 BCrypt 哈希值，非明文）
- [ ] 关联数据满足联合唯一约束（不违反 model/005 唯一索引）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿