# Sprint 工作单：sprint-042

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-042 |
| 所属模块 | web |
| 功能点 ID | web/018 |
| 功能点名称 | 修复侧边栏菜单图标不显示（图标字符串未解析为组件） |
| 状态 | AWAITING_REVIEW |
| 创建时间 | 2026-09-02 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | Vite+Vue3+TS+Element Plus 骨架 | ✅ |
| web/003 | 主布局（Sidebar.vue 侧边菜单 + 动态菜单渲染） | ✅ |
| web/008 | UI 现代化 + 白天/晚上模式切换 | ✅ |

## 业务背景

用户反馈：登录系统后，侧边栏各个路由选项的 icon 均不显示（空白）。

根因分析：
- `frontend/src/composables/useMenu.ts` 中菜单配置的 `icon` 字段为字符串（如 `'Monitor'`、`'Setting'`、`'User'`、`'UserFilled'`、`'Lock'`、`'Grid'`），对应 `@element-plus/icons-vue` 包中的图标组件名
- `frontend/src/components/Sidebar.vue` 模板中使用 `<component :is="item.icon" class="sidebar__icon" />`，其中 `item.icon` 为字符串
- Vue 的 `<component :is="string">` 仅能解析**全局注册**的组件名；Element Plus 图标组件未在 `main.ts` 全局注册，也未被 `unplugin-vue-components` 自动导入（动态组件无法静态分析）
- 对比：`Header.vue` 显式 `import { User, Setting, ... } from '@element-plus/icons-vue'` 并直接用组件对象，故图标正常

约束：菜单配置 `useMenu.ts` 仅存字符串语义（便于序列化/权限过滤/后端下发扩展），**不改为直接引入组件对象**；修复应在全局注册层或 Sidebar 组件内部完成字符串→组件映射。

## 需求描述

在前端应用启动时全局注册 `@element-plus/icons-vue` 所有图标组件（或菜单用到的子集），使 `<component :is="iconName">` 能正确解析并渲染 SVG 图标。

实现路径（二选一，推荐方案 1）：
1. **main.ts 全局注册**（推荐）：在 `main.ts` 中批量导入 `@element-plus/icons-vue` 所有导出，遍历 `app.component(name, component)` 注册为全局组件；一次性解决当前及未来所有动态图标使用场景。
2. **Sidebar 内部映射**：在 `Sidebar.vue` 中导入所需图标组件，建立 `const iconMap = { Monitor, Setting, User, UserFilled, Lock, Grid }`，模板改为 `<component :is="iconMap[item.icon]" class="sidebar__icon" />`。

验收基准：登录后侧边栏一级菜单（仪表盘/系统管理）及二级菜单（用户/角色/权限/模块管理）图标均正常显示 16×16px，亮/暗主题下颜色跟随 `--color-text-placeholder`/`--color-text-secondary` 主题变量。

## 验收标准（TDD 驱动）

### AC1 — main.ts 全局注册 Element Plus 图标组件
> `frontend/src/main.ts` 启动阶段批量注册 `@element-plus/icons-vue` 所有导出为全局组件（组件名即导出名，如 `Monitor`、`Setting` 等）

**用例**：`SidebarIconSpec#mainRegistersAllElementPlusIcons`
- 读取 `main.ts` 文本，断言存在 `import * as ElementPlusIconsVue from '@element-plus/icons-vue'` 或等价批量导入；断言存在 `for (const [name, component] of Object.entries(ElementPlusIconsVue)) { app.component(name, component) }` 或等价遍历注册逻辑

### AC2 — 侧边栏菜单图标正确渲染（静态源码校验）
> `Sidebar.vue` 模板中 `<component :is="item.icon" class="sidebar__icon" />` 保持不变；菜单配置 `useMenu.ts` 的 `icon` 字段保持字符串不变；经 AC1 全局注册后，字符串可被 Vue 解析为对应图标组件

**用例**：`SidebarIconSpec#sidebarMenuIconsRenderViaGlobalRegistry`
- 读取 `Sidebar.vue` 与 `useMenu.ts` 文本，断言模板仍为 `<component :is="item.icon" class="sidebar__icon" />`、菜单配置 `icon` 仍为字符串；结合 AC1 实现代码存在即视为通过（运行时渲染由 E2E/冒烟保证）

### AC3 — 图标尺寸与主题色符合设计规范
> `Sidebar.vue` scoped 样式中 `.sidebar__icon` 显式声明 `font-size: 16px`（或等价 `width: 16px; height: 16px`），颜色使用 `var(--color-text-placeholder)`，hover/激活态跟随菜单项变为 `var(--color-primary)`；已有样式基本满足，仅确认无回归

**用例**：`SidebarIconSpec#iconSizeAndThemeColorCompliance`
- 读取 `Sidebar.vue` 样式段，断言 `.sidebar__icon` 含 `font-size: 16px`（或 `width: 16px; height: 16px`）、`color: var(--color-text-placeholder)`、`.is-active .sidebar__icon { color: var(--color-primary) }`

### AC4 — 生产构建产物包含图标注册代码（冒烟）
> 执行生产构建后，产物 JS（`frontend/dist/assets/js/*.js`）中包含图标注册特征：`ElementPlusIconsVue` 或 `app.component(` 字样

**用例**：`scripts/smoke.sh` 追加 web-018 用例
- 前置执行 `npx vite build`，grep 产物 JS 断言存在 `ElementPlusIconsVue` 或 `app.component` 调用特征；缺失即非零退出

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | SidebarIconSpec#mainRegistersAllElementPlusIcons | AC1 | ✅ 通过 |
| 2 | SidebarIconSpec#sidebarMenuIconsRenderViaGlobalRegistry | AC2 | ✅ 通过 |
| 3 | SidebarIconSpec#iconSizeAndThemeColorCompliance | AC3 | ✅ 通过 |
| 4 | smoke.sh web-018 用例（构建产物含图标注册代码） | AC4 | ✅ 通过 |

## RED 证据

```text
[RED] Tests run: 3, Failures: 2, Passed: 1 — SidebarIconSpec（npx vitest run src/__tests__/sidebar-icon.spec.ts）
[RED] mainRegistersAllElementPlusIcons: expected main.ts to contain 'import * as ElementPlusIconsVue from @element-plus/icons-vue' — main.ts 仅基础引导，无图标注册逻辑
[RED] iconSizeAndThemeColorCompliance: expected Sidebar.vue to match /.is-active\s+\.sidebar__icon\s*\{[^}]*color:\s*var\(--color-primary\)/ — 样式中 .is-active .sidebar__icon 规则嵌套在 :deep(.el-menu-item) 内，非顶层选择器，测试正则未匹配（实现样式符合预期，测试需调整定位方式）
[PASS] sidebarMenuIconsRenderViaGlobalRegistry: Sidebar.vue 模板含 <component :is="item.icon" class="sidebar__icon" />、useMenu.ts icon 字段均为字符串（Monitor/Setting/User/UserFilled/Lock/Grid）
```

## GREEN 证据

```text
[GREEN] SidebarIconSpec 3/3 通过（npx vitest run src/__tests__/sidebar-icon.spec.ts）
[GREEN] 实现：main.ts 引入 import * as ElementPlusIconsVue from '@element-plus/icons-vue'，遍历 Object.entries 注册为全局组件
[GREEN] 前端全量测试：226 通过 / 14 失败——14 失败文件集合与预存清单完全一致（LoginView.spec.ts 1 + system/IndexView.spec.ts 13），本次新增 3 用例全过，零回归
[GREEN] 前端 lint：本次新增测试文件 eslint 0 错误；main.ts 0 错误
[GREEN] 前端 build：vite build 成功，产物含图标注册代码 Object.entries + .component(
[GREEN] 冒烟 web-018：✅ 通过（构建产物 grep Object.entries 与 .component() 验证图标注册代码存在）
```

## 门禁与冒烟记录

- 后端 `mvn -q verify`：未运行（本功能点后端零改动，预存失败项同 sprint-041 挂起区登记）
- 前端 `npm run test`：226 通过 / 14 失败——14 失败文件集合与预存清单完全一致（LoginView.spec.ts 1 + system/IndexView.spec.ts 13），本次新增 3 用例全过，零回归
- 前端 lint：本次新增测试文件 eslint 0 错误；main.ts 0 错误
- 前端 `npm run build`：vite build 成功，产物含图标注册代码 `Object.entries` + `.component(`
- 冒烟 `bash scripts/smoke.sh`：web-018 新增用例 ✅ 通过；预存失败列项（roles-001/002、web-013 等）零新增

## 返工记录

（无）

## 评审意见

（待 Evaluator 填入）

## 交付物（预估 ≤3 文件）

1. `frontend/src/main.ts` — 新增 Element Plus 图标全局注册逻辑
2. `frontend/src/__tests__/sidebar-icon.spec.ts`（新）— SidebarIconSpec 三条源级静态用例（AC1/AC2/AC3）
3. `scripts/smoke.sh` — 追加 web-018 冒烟用例（构建产物 grep 图标注册特征）

## 变更清单

### 新增
- `frontend/src/__tests__/sidebar-icon.spec.ts` — SidebarIconSpec 三条源级静态用例

### 修改
- `frontend/src/main.ts` — 引入并批量注册 `@element-plus/icons-vue` 所有图标组件

### 删除
- （无）

## 规范检查清单

- [ ] main.ts 批量注册 @element-plus/icons-vue 所有导出（AC1，SidebarIconSpec#mainRegistersAllElementPlusIcons 通过）
- [ ] Sidebar.vue 模板与 useMenu.ts 配置保持不变，仅依赖全局注册生效（AC2，SidebarIconSpec#sidebarMenuIconsRenderViaGlobalRegistry 通过）
- [ ] 图标尺寸 16px、颜色主题变量化、激活态变色无回归（AC3，SidebarIconSpec#iconSizeAndThemeColorCompliance 通过）
- [ ] 生产构建产物含图标注册代码特征（AC4，smoke.sh web-018 通过）
- [ ] 门禁：新增测试文件 lint 0 错误、改动文件 vue-tsc 0 命中；全量红项与预存清单一致
- [ ] 冒烟：web-018 新增用例 ✅；预存失败列项零新增
- [ ] 符合 docs/01-architecture.md 前端技术栈约束（纯 Vue/TS，无新依赖）

## 拆分说明

变更范围：main.ts（注册逻辑 1 处）+ 1 个测试文件 + smoke.sh，验收标准 4 条，不拆分。