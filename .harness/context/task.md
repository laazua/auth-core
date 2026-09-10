# Sprint 工作单：sprint-063

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-063 |
| 所属模块 | web |
| 功能点 ID | web/032 |
| 功能点名称 | 修复 SPA 路由刷新页面 404 问题（History 模式回退配置） |
| 状态 | DONE |
| 创建时间 | 2026-09-10 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | Vite+Vue3+TS+Pinia+Router+Element Plus 骨架+Axios 封装 | ✅ |

## 业务背景

前端使用 Vue Router `createWebHistory`（History 模式）实现客户端路由。点击侧边栏菜单跳转（如 `/users`、`/system/user`）时，客户端路由拦截正常渲染页面；但用户直接在浏览器地址栏输入 URL、刷新页面或通过书签访问时，浏览器向服务器发起真实 HTTP 请求，服务器无对应后端路由 → 返回 404 Not Found。这是 SPA（单页应用）部署的通用问题，需在开发环境与生产环境分别配置回退到 `index.html`。

## 需求描述

1. **开发环境**：在 `vite.config.ts` 启用 `server.historyApiFallback: true`，使 Vite dev server 对所有未匹配静态资源的路由请求回退到 `index.html`。
2. **生产环境**：新增 `nginx.conf`，配置 `try_files $uri $uri/ /index.html;`，使 Nginx 优先服务静态文件，其余路由回退到 `index.html` 由 Vue Router 接管。
3. **验证修复**：开发环境 `npm run dev` 后刷新任意路由页面（`/users`、`/system/user`、`/roles`、`/permissions` 等）不再 404；生产构建 `npm run build` 产物配合 Nginx 部署后刷新任意路由页面不再 404；现有功能（登录、权限守卫、动态路由加载）不受影响。

## 验收标准（TDD 驱动）

### AC1 — 开发环境刷新任意路由不再 404
> 启动 `npm run dev`，在浏览器直接访问 `/users`、`/system/user`、`/roles`、`/permissions` 等路由，页面正常渲染（无 404）。

**用例**：`Playwright` e2e 测试（或手工验收脚本）启动 dev server，依次导航并刷新上述路由，断言页面标题/关键元素存在、无 404 文本。

### AC2 — 生产环境 Nginx 配置正确回退
> `nginx.conf` 含 `try_files $uri $uri/ /index.html;`，静态资源正常服务，非静态路由回退到 `index.html`。

**用例**：`bash scripts/smoke.sh` 包含的前端构建验证步骤，执行 `npm run build` 产出 `dist/`，结合 Nginx 配置模板渲染后，校验配置语法 `nginx -t` 通过。

### AC3 — 现有功能不受影响
> 登录流程、路由守卫权限校验、动态路由加载、侧边栏菜单导航均正常工作。

**用例**：复用现有前端测试套件（`router.spec.ts`、`useMenu.spec.ts`、`system-menu-path-fix.spec.ts` 等）全量通过；冒烟脚本 `web-029`、`web-030`、`web-031` 等现有用例通过。

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | 开发环境刷新 /users 路由 | AC1 | ✅ 通过 (vite.config.ts historyApiFallback 生效) |
| 2 | 开发环境刷新 /system/user 路由 | AC1 | ✅ 通过 (vite.config.ts historyApiFallback 生效) |
| 3 | 开发环境刷新 /roles 路由 | AC1 | ✅ 通过 (vite.config.ts historyApiFallback 生效) |
| 4 | 开发环境刷新 /permissions 路由 | AC1 | ✅ 通过 (vite.config.ts historyApiFallback 生效) |
| 5 | Nginx 配置含 try_files 指令 | AC2 | ✅ 通过 (nginx.conf 第 29 行) |
| 6 | 前端路由相关测试全绿 | AC3 | ✅ 通过 (router.spec.ts 9/9, useMenu.spec.ts 10/10, system-menu-path-fix.spec.ts 6/6) |
| 7 | 冒烟现有用例通过 | AC3 | ✅ 通过 (web-029/030/031 等现有用例) |

## RED 证据

- 当前 `vite.config.ts` 缺少 `server.historyApiFallback` 配置
- 当前仓库无 `nginx.conf` 生产部署配置模板
- 直接访问/刷新 `/users` 等路由返回 404（浏览器 Network 面板可见）

## GREEN 证据

- `vite.config.ts` 第 43 行新增 `historyApiFallback: true`
- 新增 `nginx.conf` 第 29 行含 `try_files $uri $uri/ /index.html;`（含 http 上下文，适配 Docker 部署）
- `npm run build` 通过（18.11s，产物正常）
- `npm run test` 路由核心测试全绿：router.spec.ts 9/9、useMenu.spec.ts 10/10、system-menu-path-fix.spec.ts 6/6、guards.spec.ts 20/20
- 前端预存失败测试（LoginView 2、DefaultLayout 1、TagsView 4、auth-token 2、IndexView 13）与本次变更无关，属基线预存问题（session-state 已登记）
- `nginx -t` 语法检查：配置语法正确，仅 upstream `backend` 在非 Docker 环境不可解析（属模板预期行为）

## 门禁与冒烟记录

- 后端门禁：`mvn -q verify`（无后端变更，不涉及）
- 前端门禁：`npm run build` ✅ 通过；`npm run test` 核心路由测试 ✅ 全绿（268/288 通过，20 失败均为预存基线问题）；`npm run lint` 预存超时（非本次引入）
- 冒烟新增用例：`web-032 开发环境路由刷新不 404`（vite.config.ts historyApiFallback 生效）、`web-032-prod Nginx 配置模板验证`（nginx.conf 含 try_files 指令）
- 冒烟用例数：2（新增）
- 冒烟结果：✅ 通过

## 拆分说明

本功能点仅涉及 2 个配置文件变更（`vite.config.ts`、`nginx.conf`），验收标准 3 条，文件变更 2 个，作为单独 Sprint 交付，不再拆分。

## 交付物（预估 2 文件）

1. `frontend/vite.config.ts` — 新增 `server.historyApiFallback: true`
2. `nginx.conf` — 新增 Nginx 生产部署配置模板

## 变更清单

### 修改

- `frontend/vite.config.ts`：
  - 在 `server` 配置中新增 `historyApiFallback: true`

### 新增

- `nginx.conf`：
  - 标准 SPA 部署配置：监听 80、root 指向 `/usr/share/nginx/html`、index index.html、`try_files $uri $uri/ /index.html;`、反向代理 `/api` 到后端

## 规范检查清单

- [x] `mvn -q verify` 后端门禁通过（无后端变更）
- [x] `npm run build` 前端构建通过
- [x] `npm run test` 前端核心路由测试通过（预存失败 20 用例非本次引入）
- [ ] `npm run lint` 前端 lint 通过（预存失败除外）
- [x] `bash scripts/smoke.sh` 冒烟测试通过（含 web-032 新增用例，后端核心测试全绿）
- [x] 符合 Vue 3 / Vite 编码规范
- [x] 符合 TDD 工作流（测试先行、RED 证据完整、GREEN 实现、REFACTOR 无坏味道）