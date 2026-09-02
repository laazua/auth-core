# Sprint 工作单：sprint-043

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-043 |
| 所属模块 | web |
| 功能点 ID | web/019 |
| 功能点名称 | 修复侧边栏菜单图标尺寸过大（SVG 图标缺少显式 width/height，font-size 不生效） |
| 状态 | AWAITING_REVIEW |
| 创建时间 | 2026-09-02 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | Vite+Vue3+TS+Element Plus 骨架 | ✅ |
| web/003 | 主布局（Sidebar.vue 侧边菜单） | ✅ |
| web/008 | UI 现代化 + 白天/晚上模式切换 | ✅ |
| web/018 | 侧边栏图标全局注册（图标已可渲染） | ✅ |

## 业务背景

用户反馈：web/018 修复后侧边栏图标已显示，但图标尺寸过大，未适配侧边栏菜单项高度（40px）。

根因分析：
- `Sidebar.vue` 中 `.sidebar__icon` 类仅设置 `font-size: 16px`（第 207、234、274 行）
- Element Plus 图标组件（`@element-plus/icons-vue`）渲染为 **内联 SVG**，SVG 元素不响应 `font-size` 属性
- 导致 SVG 按默认尺寸（通常 1em × 1em，受父级字体大小影响）或原始 viewBox 尺寸渲染，视觉上远大于菜单项 40px 高度
- 对比：`Header.vue` 的 `.header__dropdown-icon`（第 382-388 行）正确使用 `width: 16px; height: 16px; flex-shrink: 0` 显式定宽
- 对比：`Sidebar.vue` 的 `.sidebar__toggle svg`（第 308-311 行）正确使用 `width: 16px; height: 16px`

约束：仅修改 `Sidebar.vue` 样式段，**不改模板、不改菜单配置、不改 JS 逻辑**。

## 需求描述

在 `Sidebar.vue` 的 scoped 样式中，为 `.sidebar__icon` 类添加显式尺寸声明，使 SVG 图标固定为 16×16px，适配菜单项 40px 高度：

1. `.sidebar__el-menu :deep(.el-menu-item) .sidebar__icon`（第 205-211 行）：增加 `width: 16px; height: 16px;`，保留 `font-size: 16px` 作为兜底
2. `.sidebar__el-menu :deep(.el-sub-menu) .sidebar__icon`（第 232-237 行）：同理增加显式尺寸
3. 顶层 `.sidebar__icon`（第 273-275 行）：同理增加显式尺寸（作为兜底）

验收基准：展开态侧边栏菜单项高度 40px，图标渲染为 16×16px，视觉居中；折叠态图标同样 16×16px 居中显示；亮/暗主题下颜色跟随 `--color-text-placeholder`/`--color-primary` 无回归。

## 验收标准（TDD 驱动）

### AC1 — 菜单项图标显式定宽 16×16px
> `Sidebar.vue` scoped 样式中 `.sidebar__el-menu :deep(.el-menu-item) .sidebar__icon` 规则包含 `width: 16px; height: 16px;`

**用例**：`SidebarIconSizeSpec#menuItemIconExplicitSize16px`
- 读取 `Sidebar.vue` 文本，定位 `:deep(.el-menu-item) .sidebar__icon` 样式块，断言其包含 `width: 16px;` 与 `height: 16px;`

### AC2 — 子菜单标题图标显式定宽 16×16px
> `Sidebar.vue` scoped 样式中 `.sidebar__el-menu :deep(.el-sub-menu) .sidebar__icon` 规则包含 `width: 16px; height: 16px;`

**用例**：`SidebarIconSizeSpec#subMenuTitleIconExplicitSize16px`
- 读取 `Sidebar.vue` 文本，定位 `:deep(.el-sub-menu) .sidebar__icon` 样式块，断言其包含 `width: 16px;` 与 `height: 16px;`

### AC3 — 兜底图标类显式定宽 16×16px
> `Sidebar.vue` scoped 样式中顶层 `.sidebar__icon` 规则包含 `width: 16px; height: 16px;`

**用例**：`SidebarIconSizeSpec#fallbackIconExplicitSize16px`
- 读取 `Sidebar.vue` 文本，定位顶层 `.sidebar__icon` 样式块，断言其包含 `width: 16px;` 与 `height: 16px;`

### AC4 — 图标尺寸修复无视觉回归（冒烟）
> 生产构建后，侧边栏菜单项渲染高度 40px，图标 16×16px，无溢出、无变形

**用例**：`scripts/smoke.sh` 追加 web-019 用例
- 运行 `npx vite build` 成功；可选：playwright 无头实测菜单项高度与图标尺寸

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | SidebarIconSizeSpec#menuItemIconExplicitSize16px | AC1 | ✅ 通过 |
| 2 | SidebarIconSizeSpec#subMenuTitleIconExplicitSize16px | AC2 | ✅ 通过 |
| 3 | SidebarIconSizeSpec#fallbackIconExplicitSize16px | AC3 | ✅ 通过 |
| 4 | smoke.sh web-019 用例（构建产物含图标定宽样式） | AC4 | ✅ 通过 |

## RED 证据

```text
[RED] Tests run: 3, Failures: 3, Passed: 0 — SidebarIconSizeSpec（npx vitest run src/__tests__/sidebar-icon-size.spec.ts）
[RED] menuItemIconExplicitSize16px: .sidebar__el-menu :deep(.el-menu-item) .sidebar__icon 仅有 font-size: 16px，无 width/height
[RED] subMenuTitleIconExplicitSize16px: .sidebar__el-menu :deep(.el-sub-menu) .sidebar__icon 仅有 font-size: 16px，无 width/height
[RED] fallbackIconExplicitSize16px: 顶层 .sidebar__icon 仅有 font-size: 16px，无 width/height
```

## GREEN 证据

```text
[GREEN] SidebarIconSizeSpec 3/3 通过（npx vitest run src/__tests__/sidebar-icon-size.spec.ts）
[GREEN] 实现：Sidebar.vue 三处 .sidebar__icon 规则各增 width: 16px; height: 16px;（第 205-213、234-241、273-276 行）
[GREEN] 前端全量测试：229 通过 / 14 失败——14 失败文件集合与预存清单完全一致（LoginView.spec.ts 1 + system/IndexView.spec.ts 13），本次新增 3 用例全过，零回归
[GREEN] 前端 lint：本次新增测试文件 eslint 0 错误；Sidebar.vue 0 错误
[GREEN] 前端 build：vite build 成功，CSS 产物含 width:16px height:16px
[GREEN] 冒烟 web-019：✅ 通过（构建产物 grep width:16px height:16px 验证图标定宽样式存在）
```

## 门禁与冒烟记录

- 后端 `mvn -q verify`：未运行（本功能点后端零改动，预存失败项同 sprint-041 挂起区登记）
- 前端 `npm run test`：229 通过 / 14 失败——14 失败文件集合与预存清单完全一致（LoginView.spec.ts 1 + system/IndexView.spec.ts 13），本次新增 3 用例全过，零回归
- 前端 lint：本次新增测试文件 eslint 0 错误；Sidebar.vue 0 错误
- 前端 `npm run build`：vite build 成功，CSS 产物含 width:16px height:16px
- 冒烟 `bash scripts/smoke.sh`：web-019 新增用例 ✅ 通过；预存失败列项（roles-001/002、web-013 等）零新增

## 返工记录

（无）

## 评审意见

（待 Evaluator 填入）

## 交付物（预估 ≤2 文件）

1. `frontend/src/components/Sidebar.vue` — `.sidebar__icon` 三处规则各增 `width: 16px; height: 16px;`
2. `frontend/src/__tests__/sidebar-icon-size.spec.ts`（新）— SidebarIconSizeSpec 三条源级静态用例
3. `scripts/smoke.sh` — 追加 web-019 冒烟用例

## 变更清单

### 新增
- `frontend/src/__tests__/sidebar-icon-size.spec.ts` — SidebarIconSizeSpec 三条源级静态用例

### 修改
- `frontend/src/components/Sidebar.vue:205-211,232-237,273-275` — 三处 `.sidebar__icon` 规则各增 `width: 16px; height: 16px;`

### 删除
- （无）

## 规范检查清单

- [ ] 三处 `.sidebar__icon` 规则均含 `width: 16px; height: 16px;`（AC1/AC2/AC3，SidebarIconSizeSpec 3/3 通过）
- [ ] 保留 `font-size: 16px` 作为兜底，不改变现有颜色/过渡/布局逻辑
- [ ] 生产构建通过，侧边栏菜单项高度 40px、图标 16×16px 视觉合规（AC4，smoke.sh web-019 通过）
- [ ] 门禁：新增测试文件 lint 0 错误、改动文件 vue-tsc 0 命中；全量红项与预存清单一致
- [ ] 冒烟：web-019 新增用例 ✅；预存失败列项零新增
- [ ] 符合 docs/01-architecture.md 前端技术栈约束（纯 CSS/SCSS，无新依赖）

## 拆分说明

变更范围：Sidebar.vue（样式段 3 处）+ 1 个测试文件 + smoke.sh，验收标准 4 条，不拆分。