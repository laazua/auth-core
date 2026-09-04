# Sprint 工作单：sprint-056

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-056 |
| 所属模块 | web |
| 功能点 ID | web/011 |
| 功能点名称 | 修复前端构建错误与失败测试 |
| 状态 | PLANNED |
| 创建时间 | 2026-09-04 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/010 | 修复侧边栏系统管理菜单 404 问题（路由未注册到路由器） | ✅ |

## 业务背景

前端项目存在 350+ TS 编译错误（32 文件，属 web/011 范围）、npm run lint 全量卡死/分片含预存错误、前端 test 14 预存失败（LoginView.spec.ts 1 + system/IndexView.spec.ts 13，Element Plus 组件解析缺失）。这些问题阻塞后续 web 模块新功能开发与评审，需集中治理。

## 需求描述

1. **修复 TS 编译错误**：逐文件消除 350+ TS 错误，使 `npm run build` 通过
2. **修复 Lint 错误**：修复 `npm run lint` 报错，使其全量通过且不卡死
3. **修复预存测试失败**：修复 LoginView.spec.ts 1 例 + IndexView.spec.ts 13 例失败，使 `npm run test` 全绿
4. **补充 Element Plus 组件类型声明**：解决组件解析缺失导致的类型错误

## 验收标准（TDD 驱动）

### AC1 — TS 编译零错误
> 执行 `npm run build` 无 TS 编译错误（exit code 0，无红色 error 输出）

**用例**：`npm run build`
- 验证控制台无 `error TS` 字样
- dist 目录正常产出

### AC2 — Lint 全量通过
> 执行 `npm run lint` 无报错、无卡死、exit code 0

**用例**：`npm run lint`
- 验证无 ESLint error 输出
- 耗时 < 60s（排除卡死）

### AC3 — 单元测试全绿
> 执行 `npm run test` 所有测试用例通过（预存失败 14 例全部修复）

**用例**：`npm run test`
- LoginView.spec.ts 通过
- system/IndexView.spec.ts 13 例全部通过
- 无 Element Plus 组件解析报错

### AC4 — 无运行时控制台报错（基础页面）
> 启动 `npm run dev`，访问登录页、系统管理页，浏览器控制台无红色 JS 错误

**用例**：人工验收 / Playwright 冒烟
- 登录页正常渲染
- 登录后系统管理菜单可进入

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | `npm run build` 编译零错误 | AC1 | 待写 |
| 2 | `npm run lint` 全绿 | AC2 | 待写 |
| 3 | `npm run test` 全绿 | AC3 | 待写 |
| 4 | 基础页面无运行时错误 | AC4 | 待写 |

## RED 证据

待 Generator 实现阶段填写。核心 RED 场景：`npm run build` 输出 350+ TS error、`npm run lint` 卡死/报错、`npm run test` 14 例失败（含 Element Plus 组件解析缺失）。

## GREEN 证据

待 Generator 实现阶段填写。

## 门禁与冒烟记录

待 Generator 实现阶段填写。

## 拆分说明

本功能点涉及文件数预估 > 6，验收标准 4 条。需拆分为子功能点：
- web/011a：修复 TS 编译错误（优先级最高，解除构建阻塞）
- web/011b：修复 Lint 错误
- web/011c：修复单元测试失败
- web/011d：验证运行时无报错

**本次仅注册 web/011a（修复 TS 编译错误）作为第一子功能点**，其余留待后续 Sprint。

## 交付物（web/011a 预估 ≤6 文件）

1. `frontend/src/views/LoginView.vue` — 修复 TS/类型错误
2. `frontend/src/views/system/IndexView.vue` — 修复 TS/类型错误
3. `frontend/src/components/*` — 修复组件级 TS 错误
4. `frontend/src/stores/*` — 修复 Pinia store 类型错误
5. `frontend/src/api/*` — 修复 API 调用类型错误
6. `frontend/vite.config.ts` / `tsconfig.json` — 必要的类型配置补充

## 变更清单

### 新增
（无，主要为修改现有文件）

### 修改
- `frontend/src/views/LoginView.vue`
- `frontend/src/views/system/IndexView.vue`
- 其余 30 个有 TS 错误的文件

## 规范检查清单

- [ ] `npm run build` 零 TS 错误
- [ ] `npm run lint` 全绿、不卡死
- [ ] `npm run test` 全绿（含预存失败 14 例）
- [ ] 基础页面启动无运行时错误
- [ ] 符合前端编码规范（Vue 3 + TS strict + ESLint + Prettier）