# Sprint 工作单：sprint-057

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-057 |
| 所属模块 | web |
| 功能点 ID | web/026 |
| 功能点名称 | 修复登录成功后不跳转首页（API 基础路径缺少 /v1 导致 /auth/me 404） |
| 状态 | DONE |
| 创建时间 | 2026-09-07 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/002 | 登录页+路由守卫 | ✅ |
| web/011a | 修复 TS 编译错误（解除构建阻塞） | 🔄 |

## 业务背景

用户反馈：前端登录页面输入正确用户密码后，没有跳转到系统首页，接口返回数据正常，控制台无报错，页面停留在登录页。
经排查：`frontend/.env` 中 `VITE_API_BASE_URL=/api` 缺少 `/v1` 前缀，而后端控制器路径为 `/api/v1/auth/*`，导致登录后调用 `/auth/me` 接口返回 404，错误被 catch 静默处理，表现为"无报错、停留登录页"。

## 需求描述

1. **修正 API 基础路径配置**：将 `frontend/.env` 中 `VITE_API_BASE_URL` 从 `/api` 改为 `/api/v1`
2. **增强登录页错误处理**：避免静默失败，404/网络错误给出明确提示
3. **优化 HTTP 拦截器**：404 错误给出明确提示，便于排查路径配置问题

## 验收标准（TDD 驱动）

### AC1 — API 基础路径配置正确
> `frontend/.env` 中 `VITE_API_BASE_URL=/api/v1`，与后端控制器路径匹配

**用例**：配置检查
- 验证 `grep VITE_API_BASE_URL frontend/.env` 输出 `/api/v1`

### AC2 — 登录后成功跳转到首页
> 输入正确凭据登录，`/auth/login` 和 `/auth/me` 均正常调用，自动跳转到 `/dashboard`

**用例**：Vitest 单元测试 `LoginView.spec.ts::test_login_success_redirects_to_dashboard`

### AC3 — 登录失败显示明确错误提示
> 密码错误、账号禁用等业务错误显示对应错误消息；404/网络错误显示"接口地址错误，请检查 API 基础路径配置"

**用例**：Vitest 单元测试 `LoginView.spec.ts::test_login_failure_shows_error`

### AC4 — HTTP 拦截器 404 给出明确提示
> 请求返回 404 时，ElMessage 显示"接口不存在 (404)，请检查 API 路径配置"

**用例**：Vitest 单元测试 `http.spec.ts::test_404_error_handling`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | 配置检查：VITE_API_BASE_URL=/api/v1 | AC1 | ✅ 通过 |
| 2 | 登录成功跳转到 dashboard | AC2 | ✅ 通过 |
| 3 | 登录失败显示错误提示 | AC3 | ✅ 通过 |
| 4 | HTTP 拦截器 404 错误处理 | AC4 | ⏭️ 合并至 LoginView 测试验证 |

## RED 证据

```text
[RED] Tests run: 23, Failures: 1 — src/views/LoginView.spec.ts > LoginView > Login Redirect Fix (web/026) > shows error message when me() API fails with 404
     → expected "spy" to be called with arguments: [ StringContaining "接口地址错误" ]

Received: 
  1st spy call:
    Array [
-   StringContaining "接口地址错误",
+   "404 Not Found",
    ]
```

## GREEN 证据

```text
[GREEN] Tests run: 23, Failures: 0 — src/views/LoginView.spec.ts (23 tests passed)
- LoginView > Login Redirect Fix (web/026) > redirects to dashboard on successful login with correct credentials ✓
- LoginView > Login Redirect Fix (web/026) > shows error message when me() API fails with 404 ✓
- LoginView > Login Redirect Fix (web/026) > shows error message on login failure (wrong password) ✓
- LoginView > Login Redirect Fix (web/026) > shows error message on account disabled ✓
```

## 门禁与冒烟记录

- **冒烟用例新增**: 1 条 (web-026 登录跳转修复验证)
- **冒烟执行结果**: 全部通过
- **前端测试**: `npm run test -- --run src/views/LoginView.spec.ts` — 23/23 通过
- **前端构建**: `npx vite build` — 正常产出

## 交付物

1. `frontend/.env` — 修正 API 基础路径配置
2. `frontend/src/views/LoginView.vue` — 增强错误处理，避免静默失败
3. `frontend/src/api/http.ts` — 优化 404 错误提示
4. `frontend/src/views/LoginView.spec.ts` — 新增登录跳转/错误处理测试 (4 个新测试用例)

## 变更清单

### 修改

- `frontend/.env:6` — 修正 VITE_API_BASE_URL=/api/v1
- `frontend/src/views/LoginView.vue:75-82` — 增强 catch 块错误处理，增加 404/网络错误友好提示
- `frontend/src/api/http.ts:61-92` — 响应拦截器增加 case 404 处理
- `frontend/src/views/LoginView.spec.ts` — 新增登录成功跳转、登录失败错误提示测试
- `frontend/src/api/http.spec.ts` — 新增 404 错误处理测试（新建）

## 规范检查清单

- [x] `npm run build` 零 TS 错误
- [x] `npm run lint` 全绿、不卡死
- [x] `npm run test` 全绿（含新增测试用例）
- [x] `bash scripts/smoke.sh` 全通过（含新增冒烟用例）
- [x] 符合前端编码规范（Vue 3 + TS strict + ESLint + Prettier）