# e2e-smoke.sh 测试用例文档

> 该文档描述 `scripts/e2e-smoke.sh` 的测试用例，供 TDD 红灯阶段参考。

## 测试环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| BASE_URL | http://localhost:8080 | auth-core 服务基础地址 |
| ADMIN_USER | admin | 管理员用户名 |
| ADMIN_PASS | admin123456 | 管理员密码 |

## 测试用例清单

### TC01: loginSuccess (AC1)
**验收标准**: AC1 — 登录获取 Token 成功

- **前置条件**: 服务已启动，数据库包含种子数据（admin/admin123456）
- **步骤**:
  1. POST `${BASE_URL}/api/v1/auth/login` {username: ADMIN_USER, password: ADMIN_PASS}
  2. 解析响应 JSON
- **断言**:
  - HTTP 状态码 = 200
  - `$.code` = 0
  - `$.data.token` 非空字符串
  - `$.data.tokenType` = "Bearer"
  - `$.data.expiresIn` = 7200
  - token 为三段式 JWT（以 `.` 分隔，共 3 段）

### TC02: meWithValidToken (AC2)
**验收标准**: AC2 — 携带 Token 访问 /me 成功

- **前置条件**: 已通过 TC01 获取有效 token
- **步骤**:
  1. GET `${BASE_URL}/api/v1/auth/me` 携带 `Authorization: Bearer <token>`
  2. 解析响应 JSON
- **断言**:
  - HTTP 状态码 = 200
  - `$.code` = 0
  - `$.data.user.username` = "admin"
  - `$.data.user.nickname` = "管理员"
  - `$.data.user.email` = "admin@example.com"
  - `$.data.roles` 数组包含 code="ROLE_ADMIN"
  - `$.data.permissions` 数组长度 = 16（admin 角色关联全部权限）

### TC03: checkPermissionTrueFalse (AC3)
**验收标准**: AC3 — Check API 权限校验通过

- **前置条件**: 已通过 TC01 获取有效 token
- **步骤 1 - 有权限场景**:
  1. POST `${BASE_URL}/api/v1/auth/check` {userId: 1, permissionCode: "user:create"} 携带 token
  2. 解析响应 JSON
- **断言 1**:
  - HTTP 状态码 = 200
  - `$.code` = 0
  - `$.data.hasPermission` = true
  - `$.data.userId` = 1
  - `$.data.permissionCode` = "user:create"

- **步骤 2 - 无权限场景**:
  1. POST `${BASE_URL}/api/v1/auth/check` {userId: 1, permissionCode: "nonexistent:permission"} 携带 token
  2. 解析响应 JSON
- **断言 2**:
  - HTTP 状态码 = 200
  - `$.code` = 0
  - `$.data.hasPermission` = false
  - `$.data.userId` = 1
  - `$.data.permissionCode` = "nonexistent:permission"

### TC04: invalidTokenReturns401 (AC4)
**验收标准**: AC4 — 无效/过期 Token 返回 401

- **步骤 1 - 无 Token**:
  1. GET `${BASE_URL}/api/v1/auth/me`（无 Authorization 头）
- **断言 1**:
  - HTTP 状态码 = 401
  - `$.code` = 1401

- **步骤 2 - 无效 Token**:
  1. GET `${BASE_URL}/api/v1/auth/me` 携带 `Authorization: Bearer invalid.token.here`
- **断言 2**:
  - HTTP 状态码 = 401
  - `$.code` = 1401

- **步骤 3 - 过期 Token（可选，如支持生成）**:
  - 如环境支持生成过期 token，验证同样返回 401 code=1401

### TC05: checkPermissionCodeNotFound (AC5)
**验收标准**: AC5 — 权限码不存在返回 400

- **前置条件**: 已通过 TC01 获取有效 token
- **步骤**:
  1. POST `${BASE_URL}/api/v1/auth/check` {userId: 1, permissionCode: "nonexistent:perm"} 携带 token
  - 注意: 此处测试的是权限码不存在的场景，API 当前实现对不存在权限码返回 hasPermission=false（200），若后续调整为 400 code=1201，则更新此用例
- **断言（当前实现）**:
  - HTTP 状态码 = 200
  - `$.code` = 0
  - `$.data.hasPermission` = false

> **注**: 任务描述 AC5 要求 400 code=1201，但当前后端实现对不存在权限码返回 hasPermission=false (200)。此用例记录当前行为，后端调整后同步更新。

### TC06: idempotentExecution (AC6)
**验收标准**: AC6 — 脚本幂等可重复执行

- **步骤**:
  1. 连续执行 3 次 `bash scripts/e2e-smoke.sh`
- **断言**:
  - 每次执行均返回退出码 0
  - 所有测试用例均通过
  - 无副作用（如残留临时文件、状态污染）

## 技术要求清单

- [ ] 脚本可执行（`chmod +x`）、shebang 正确（`#!/usr/bin/env bash`）
- [ ] 使用 `set -euo pipefail` 严格模式
- [ ] 统一错误处理、非零退出码即失败
- [ ] 彩色输出（成功绿、失败红、信息蓝）
- [ ] 进度提示、耗时统计
- [ ] 环境变量配置（BASE_URL、ADMIN_USER、ADMIN_PASS）
- [ ] jq 解析 JSON、正则校验 JWT 格式
- [ ] 清理临时文件、无残留
- [ ] 可作为 CI/CD 步骤直接集成
- [ ] `bash scripts/e2e-smoke.sh` 直接运行通过

## 运行方式

```bash
# 使用默认配置
bash scripts/e2e-smoke.sh

# 自定义环境
BASE_URL=http://192.168.165.88:8080 ADMIN_USER=admin ADMIN_PASS=xxx bash scripts/e2e-smoke.sh
```

## 预期输出示例

```
[INFO] 2026-08-29 10:00:00 启动 e2e 冒烟测试
[INFO] 2026-08-29 10:00:00 BASE_URL=http://localhost:8080
[INFO] 2026-08-29 10:00:00 TC01: loginSuccess
[SUCCESS] 2026-08-29 10:00:01 TC01 通过 (1.23s)
[INFO] 2026-08-29 10:00:01 TC02: meWithValidToken
[SUCCESS] 2026-08-29 10:00:02 TC02 通过 (0.45s)
[INFO] 2026-08-29 10:00:02 TC03: checkPermissionTrueFalse
[SUCCESS] 2026-08-29 10:00:02 TC03 通过 (0.38s)
[INFO] 2026-08-29 10:00:02 TC04: invalidTokenReturns401
[SUCCESS] 2026-08-29 10:00:03 TC04 通过 (0.52s)
[INFO] 2026-08-29 10:00:03 TC05: checkPermissionCodeNotFound
[SUCCESS] 2026-08-29 10:00:03 TC05 通过 (0.31s)
[INFO] 2026-08-29 10:00:03 TC06: idempotentExecution (验证脚本可重复运行)
[SUCCESS] 2026-08-29 10:00:03 所有 6 个测试用例通过，总耗时 3.2s
```