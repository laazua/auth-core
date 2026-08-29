# Sprint 工作单：sprint-024

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-024 |
| 所属模块 | web |
| 功能点 ID | web/002 |
| 功能点名称 | 登录页+路由守卫 |
| 状态 | PLANNED |
| 创建时间 | 2026-08-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | 前端骨架 | ✅ |
| auth/002 | 登录接口 | ✅ |

## 业务背景

登录页是用户进入系统的第一道门户，路由守卫是前端权限控制的核心防线。登录页需美观易用、表单验证完善、错误提示友好；路由守卫需拦截未认证访问、已登录用户访问登录页自动跳转首页、权限不足时跳转 403 页。

## 需求描述

实现登录页面与完善的路由守卫体系：

1. **登录页** (`/login`)：
   - 响应式布局，左右分栏（左侧品牌展示、右侧登录表单）
   - 表单：用户名/密码输入框、记住我、忘记密码链接、登录按钮
   - 表单验证：用户名必填、密码必填、长度校验
   - 登录中 Loading 状态、错误提示（用户名或密码错误、账号停用等）
   - 登录成功存储 Token、跳转首页或重定向原目标页
   - 响应式适配：移动端单栏、桌面端左右分栏

2. **路由守卫完善**：
   - 认证守卫：无 Token 访问受保护路由 → 重定向 `/login?redirect=...`
   - 已登录访问 `/login` → 重定向 `/dashboard`
   - 权限守卫：基于 `meta.permissions` 校验，无权限 → 跳转 `/403`
   - 动态路由加载预留（后续 web/003 接入）

3. **403 页面**：无权限访问提示页面

## 交付物

1. `src/views/LoginView.vue` — 登录页组件
2. `src/views/ForbiddenView.vue` — 403 页面
3. 更新 `src/router/guards.ts` — 完善认证/权限守卫
3. 更新 `src/router/routes.ts` — 添加登录、403 路由
4. `src/views/LoginView.spec.ts` — 登录页测试
5. `src/router/guards.spec.ts` — 路由守卫测试

## 验收标准（TDD 驱动）

### AC1 — 登录页渲染与表单验证
> 访问 `/login` 正确渲染，空提交提示“用户名不能为空”“密码不能为空”，密码长度 < 6 提示“密码长度不足 6 位”。

**用例**：`LoginView.spec.ts#formValidation`
- 空提交 → 错误提示正确
- 密码过短 → 长度提示正确
- 输入合法 → 验证通过

### AC2 — 登录成功跳转
> 输入正确凭据（admin/admin123456），点击登录 → 显示 Loading → 登录成功 → 存储 Token → 跳转 `/dashboard` 或 `redirect` 参数指定页。

**用例**：`LoginView.spec.ts#loginSuccessRedirect`
- Mock 登录 API 返回 token
- 提交表单 → Loading 显示 → API 调用 → Token 存储 → 跳转目标页

### AC3 — 登录失败错误提示
> 输入错误凭据，登录失败 → 显示错误提示“用户名或密码错误”/“账号已停用”，不跳转。

**用例**：`LoginView.spec.ts#loginFailureError`
- Mock 登录 API 返回 401/403
- 提交表单 → 错误提示显示 → 停留登录页

### AC4 — 路由守卫拦截未认证访问
> 无 Token 访问 `/dashboard` → 重定向 `/login?redirect=/dashboard`；登录后自动跳回原目标页。

**用例**：`guards.spec.ts#authGuardRedirect`
- 清除 Token → 访问受保护路由 → 重定向 `/login?redirect=...`
- 登录成功 → 跳转回原目标页

### AC5 — 已登录用户访问登录页重定向
> 已登录用户访问 `/login` → 自动重定向 `/dashboard`。

**用例**：`guards.spec.ts#loggedInRedirectFromLogin`
- 设置 Token → 访问 `/login` → 重定向 `/dashboard`

### AC6 — 权限守卫拦截无权限访问
> 无权限访问受保护路由 → 跳转 `/403` 页面。

**用例**：`guards.spec.ts#permissionGuardForbidden`
- 设置无权限用户 Token → 访问需权限路由 → 跳转 `/403`

## 规范检查清单（Evaluator 逐项核对）

- [ ] 登录页：响应式布局、表单验证、Loading、错误提示、品牌展示区
- [ ] 路由守卫：认证守卫、权限守卫、重定向逻辑完整
- [ ] 403 页面：友好提示、返回首页按钮
- [ ] 表单验证：VeeValidate + Yup 或原生校验、即时反馈
- [ ] API 对接：复用 `api/auth.ts` 登录接口、错误码映射
- [ ] 状态管理：复用 `authStore` 登录/登出/Token 管理
- [ ] 测试覆盖：登录页表单/登录流程、路由守卫重定向/权限拦截
- [ ] `npm run lint && npm run test && npm run build` 全绿