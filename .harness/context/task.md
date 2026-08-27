# Sprint 工作单：sprint-016

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-016 |
| 所属模块 | users |
| 功能点 ID | users/001 |
| 功能点名称 | 用户 CRUD API（分页/条件查询/创建加密/更新/启停用/删除） |
| 状态 | PLANNED |
| 创建时间 | 2026-08-27 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/003 | JWT 校验过滤器 + SecurityContext 注入 | ✅ |
| model/001 | sys_user 表迁移+实体+Mapper | ✅ |

## 需求描述

实现用户管理完整 CRUD API：
1. `GET /api/v1/users` — 分页查询，支持条件（username、nickname、email、phone、status）
2. `GET /api/v1/users/{id}` — 单用户详情
3. `POST /api/v1/users` — 创建用户，密码 BCrypt 加密，username 唯一校验
4. `PUT /api/v1/users/{id}` — 更新用户（不含密码，username 不可改）
5. `PATCH /api/v1/users/{id}/status` — 启停用（status=1/0）
6. `DELETE /api/v1/users/{id}` — 删除用户，引用保护（sys_user_role 存在则拒绝，code=1101）

## 业务背景

用户管理是 RBAC 系统的基础。架构 §4：分页参数 page/size，响应 data={list,total,page,size}。错误码分段：10xx 用户。RBAC0 §6.4：删除用户前校验 sys_user_role 引用。§6.5：停用用户不可登录。

## 交付物

1. `UserQueryDTO.java` / `UserCreateDTO.java` / `UserUpdateDTO.java` / `UserStatusDTO.java` / `UserVO.java` — DTO（`com.authcore.dto.user`）
2. `UserService.java` — 业务逻辑（在 `com.authcore.service`）
3. `UserController.java` — REST 端点（在 `com.authcore.controller`）
4. `UserControllerTest.java` / `UserServiceTest.java` — 测试用例

## 验收标准（TDD 驱动）

### AC1 — 分页查询支持多条件
> GET /api/v1/users?page=1&size=10&username=admin&status=1 返回 200，data 含 list/total/page/size，list 中用户匹配条件。

**用例**：`UserControllerTest#queryUsersWithPaginationAndFilters`
- 准备：种子数据 admin + 2 个测试用户
- 操作：GET /api/v1/users?username=adm&status=1
- 断言：status=200、total≥1、list 非空、每项含 id/username/nickname/email/phone/status

### AC2 — 创建用户密码加密且唯一
> POST /api/v1/users {username, password, nickname, email, phone} 返回 200，data.id 非空，密码已 BCrypt 加密存储，重复 username 返回 409 code=1002。

**用例**：`UserControllerTest#createUserEncryptsPasswordAndUniqueUsername`
- 操作：POST /api/v1/users {username:"newuser", password:"Pass1234", nickname:"新用户"}
- 断言：status=200、id 非空、数据库 password 为 BCrypt、再次创建同 username 返回 409 code=1002

### AC3 — 更新用户不含密码、username 不可改
> PUT /api/v1/users/{id} {nickname, email, phone} 返回 200，nickname/email/phone 更新，username/password 不变。

**用例**：`UserControllerTest#updateUserExcludesPasswordAndUsername`
- 操作：PUT /api/v1/users/2 {nickname:"新昵称", email:"new@test.com"}
- 断言：status=200、nickname/email 更新、username/password 与原值一致

### AC4 — 启停用与删除引用保护
> PATCH /api/v1/users/{id}/status {status:0} 返回 200；DELETE /api/v1/users/{id} 若 sys_user_role 存在则 409 code=1101，否则 200 物理删除。

**用例**：`UserControllerTest#statusToggleAndDeleteWithReferenceProtection`
- 操作：PATCH /api/v1/users/2 {status:0} → GET /me 验证不可登录（由 auth/003 拦截）
- 操作：POST /api/v1/auth/login 创建 user2，分配角色 → DELETE /api/v1/users/2 → 409 code=1101
- 断言：status 切换生效、有角色引用时删除被拒

## 规范检查清单（Evaluator 逐项核对）

- [ ] Controller 仅做三件事（接参→委托 service→包装 Result）
- [ ] Service @Transactional，创建/更新/删除事务边界正确
- [ ] 密码仅 BCrypt，创建时加密，更新不修改密码
- [ ] 分页响应固定结构 {list,total,page,size}
- [ ] 错误码分段：10xx 用户（1001 不存在、1002 用户名已存在、1003 删除引用保护）
- [ ] 删除引用保护：sys_user_role 存在拒绝（code=1101）
- [ ] 无硬编码密钥/明文密码（coding-standards §6）
- [ ] 测试 AAA 结构有效（coding-standards §9）
- [ ] `mvn -q verify` 全绿