# Sprint 工作单：sprint-031

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-031 |
| 所属模块 | integration |
| 功能点 ID | integration/002 |
| 功能点名称 | e2e 冒烟脚本（shell+curl：登录→授权→check 全链路验证） |
| 状态 | PLANNED |
| 创建时间 | 2026-08-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| integration/001 | 第三方模块接入指南 | ✅ |

## 业务背景

提供端到端的冒烟测试脚本，验证 auth-core 核心鉴权链路完整可用：登录获取 Token → 携带 Token 访问受保护资源 → 调用 check API 校验权限。脚本用于 CI/CD 流水线、部署验证、回归测试。

## 需求描述

编写 e2e 冒烟测试 Shell 脚本（`scripts/e2e-smoke.sh`），覆盖核心鉴权全链路：

1. **登录获取 Token**：
   - 调用 POST /api/v1/auth/login
   - 解析响应提取 token、tokenType、expiresIn
   - 验证 code=0、token 非空、三段式 JWT 格式

2. **携带 Token 访问受保护资源**：
   - 携带 Authorization: Bearer <token> 访问 GET /api/v1/auth/me
   - 验证返回用户信息、角色、权限集合

3. **权限校验 Check API**：
   - 携带 Token 调用 POST /api/v1/auth/check
   - 测试有权限场景（hasPermission=true）
   - 测试无权限场景（hasPermission=false）

4. **Token 过期/无效场景**：
   - 无 Token 访问 → 401 code=1401
   - 无效 Token 访问 → 401 code=1401
   - 过期 Token 访问 → 401 code=1401（如支持过期 Token 生成）

5. **权限校验边界**：
   - 管理员用户拥有全部权限 → check 全 true
   - 普通用户仅部分权限 → check 部分 true/false
   - 不存在权限码 → 400 code=1201

## 交付物

1. `scripts/e2e-smoke.sh` — 端到端冒烟测试脚本
2. `scripts/e2e-smoke.sh.spec.md` — 测试用例文档
3. 更新 `scripts/smoke.sh` — 集成 e2e 冒烟入口（可选）

## 验收标准（TDD 驱动）

### AC1 — 登录获取 Token 成功
> 执行登录请求，返回 code=0、token 非空、三段式 JWT。

**用例**：`e2eSmokeTest#loginSuccess`
- 调用登录接口 admin/admin123456
- 断言：code=0、token 非空、token 分段数=3、expiresIn=7200

### AC2 — 携带 Token 访问 /me 成功
> 携带 Token 访问 /api/v1/auth/me，返回用户信息、角色、权限。

**用例**：`e2eSmokeTest#meWithValidToken`
- 携带 Token 访问 /api/v1/auth/me
- 断言：code=0、data.user.username=admin、roles 包含 admin、permissions 包含 14 项

### AC3 — Check API 权限校验通过
> 携带 Token 调用 check API，有权限返回 true，无权限返回 false。

**用例**：`e2eSmokeTest#checkPermissionTrueFalse`
- admin 用户 check user:create → hasPermission=true
- 普通用户 check user:delete → hasPermission=false

### AC4 — 无效/过期 Token 返回 401
> 无 Token、无效 Token、过期 Token 访问受保护接口均返回 401 code=1401。

**用例**：`e2eSmokeTest#invalidTokenReturns401`
- 无 Authorization 头 → 401 code=1401
- Authorization: Bearer invalid.token.here → 401 code=1401
- 过期 Token（如可生成） → 401 code=1401

### AC5 — 权限码不存在返回 400
> check API 传入不存在权限码 → 400 code=1201。

**用例**：`e2eSmokeTest#checkPermissionCodeNotFound`
- POST /api/v1/auth/check {userId:1, permissionCode:"nonexistent:perm"}
- 断言：status=400、code=1201

### AC6 — 脚本幂等可重复执行
> 连续多次执行脚本，每次均通过，无副作用。

**用例**：`e2eSmokeTest#idempotentExecution`
- 连续执行 3 次脚本 → 均通过

## 规范检查清单（Evaluator 逐项核对）

- [ ] 脚本可执行（`chmod +x`）、shebang 正确
- [ ] 使用 `set -euo pipefail` 严格模式
- [ ] 统一错误处理、非零退出码即失败
- [ ] 彩色输出（成功绿、失败红、信息蓝）
- [ ] 进度提示、耗时统计
- [ ] 环境变量配置（BASE_URL、ADMIN_USER、ADMIN_PASS）
- [ ] jq 解析 JSON、正则校验 JWT 格式
- [ ] 清理临时文件、无残留
- [ ] 可作为 CI/CD 步骤直接集成
- [ ] `bash scripts/e2e-smoke.sh` 直接运行通过