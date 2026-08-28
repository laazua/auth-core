# Sprint 工作单：sprint-023

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-023 |
| 所属模块 | web |
| 功能点 ID | web/001 |
| 功能点名称 | Vite+Vue3+TS+Pinia+Router+Element Plus 骨架+Axios 封装(token 注入/401 拦截)+Vitest 基线 |
| 状态 | PLANNED |
| 创建时间 | 2026-08-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/002 | 登录接口 POST /api/v1/auth/login 签发 JWT | ✅ |

## 需求描述

搭建现代化前端工程骨架，作为后续所有页面的基础：

1. **工程初始化**：Vite + Vue 3 + TypeScript + Pinia + Vue Router + Element Plus
2. **Axios 封装**：统一请求/响应拦截、Token 自动注入、401 统一跳转登录、错误码统一处理
3. **Pinia Store**：auth store（token、userInfo、roles、permissions）、app store（sidebar、theme、breadcrumbs）
4. **路由配置**：基础路由（登录、404、布局）、动态路由加载预留、路由守卫预留
5. **UI 规范**：现代简洁风格、Element Plus 主题定制、响应式布局、暗色模式预留
6. **测试基线**：Vitest 单元测试配置、组件测试工具、E2E 预留

## 交付物

1. `frontend/` 完整工程目录
2. `package.json` 依赖管理
2. `vite.config.ts` 构建配置
3. `src/main.ts` 入口
4. `src/App.vue` 根组件
5. `src/router/index.ts` 路由配置
6. `src/stores/` Pinia stores
7. `src/api/` Axios 封装 + API 接口定义
8. `src/types/` 类型定义
9. `src/styles/` 全局样式、变量、主题
10. `vitest.config.ts` 测试配置

## 验收标准（TDD 驱动）

### AC1 — 工程启动构建成功
> `npm run dev` 启动开发服务器无报错，`npm run build` 生产构建成功，`npm run test` 单元测试通过。

**用例**：`SmokeTest#buildAndDevServer`
- 操作：执行 `npm install`、`npm run build`、`npm run test`
- 断言：构建产物存在于 `dist/`、测试全部通过

### AC2 — Axios 拦截器生效
> 请求自动携带 Token，响应 401 自动跳转 `/login`，错误码统一提示。

**用例**：`AxiosInterceptorsTest#tokenInjectionAnd401Redirect`
- Mock 登录接口返回 token
- 发起带 token 请求 → Header 包含 Authorization
- Mock 401 响应 → 跳转 `/login`、清除 token

### AC3 — Pinia Store 状态管理
> auth store 正确存取 token/userInfo，app store 控制侧边栏折叠、主题切换。

**用例**：`StoresTest#authAndAppStore`
- setToken/getToken 往返一致
- toggleSidebar 切换状态
- toggleTheme 切换暗色模式

### AC4 — 路由守卫基础
> 未登录访问受保护路由重定向 `/login`，已登录访问 `/login` 重定向首页。

**用例**：`RouterGuardsTest#authRedirect`
- 无 token 访问 `/dashboard` → redirect `/login`
- 有 token 访问 `/login` → redirect `/dashboard`

### AC5 — UI 现代简洁规范
> Element Plus 组件按需引入，主题色配置，响应式断点，暗色模式 CSS 变量就绪。

**用例**：`UIComponentsTest#themeAndResponsive`
- 主色调可通过 CSS 变量修改
- 侧边栏在 < 768px 自动折叠
- 暗色模式类名切换生效

## 规范检查清单（Evaluator 逐项核对）

- [ ] Vite + Vue 3 + TS + Pinia + Router + Element Plus 全家桶就绪
- [ ] Axios 封装：请求/响应拦截、Token 注入、401 跳转、错误统一处理
- [ ] Pinia：auth/app stores 完整、TypeScript 类型完备
- [ ] 路由：基础路由、守卫预留、动态路由加载预留
- [ ] UI：Element Plus 按需引入、主题定制、响应式、暗色模式预留
- [ ] 类型：API 接口、响应体、Store 状态、路由元信息全类型化
- [ ] 测试：Vitest 配置、组件测试工具、基础用例通过
- [ ] 代码规范：ESLint + Prettier + Stylelint 配置、Vue 3 Composition API 规范
- [ ] `npm run lint && npm run test && npm run build` 全绿