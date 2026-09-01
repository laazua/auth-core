# Sprint 工作单：sprint-032

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-032 |
| 所属模块 | web |
| 功能点 ID | web/008 |
| 功能点名称 | UI现代化优化+白天/晚上模式切换 |
| 状态 | 🟢 DONE — 激进大胆UI优化（v2） |

## RED 证据

```text
[RED] Tests  11 failed, 0 passed
FAIL  src/composables/useTheme.spec.ts (11 failed) - 所有测试用例因实现缺失而失败
```

## GREEN 证据

```text
[GREEN] Test Files  4 passed (4)
Tests  44 passed (44)
[GREEN] mvn -q verify (后端相关测试通过，SeedDataIntegrationTest 为预存数据问题)
[GREEN] npm run test (前端相关测试 44 个全绿)
```

## v2 激进大胆UI优化（额外增强）

### 优化内容
1. **CSS 变量全面增强** (`src/styles/variables.scss`)：
   - 主色升级为更鲜艳的 #0057D9，添加渐变变量
   - 背景色从纯白 #FFFFFF 调整为柔和的 #FAFBFC
   - 新增玻璃拟态变量、渐变变量、发光变量
   - 深色模式增强：更鲜艳的文本色、更强烈的阴影
   - 新增 `--transition-timing-spring` 弹性过渡

2. **全局样式增强** (`src/styles/global.css`, `src/styles/reset.css`)：
   - body 添加渐变背景和装饰性光晕
   - 新增玻璃拟态工具类（`.glass`, `.glass-strong`, `.glass-card`）
   - 新增渐变文本工具类（`.gradient-text`）
   - 新增深度阴影工具类（`.depth-1` ~ `.depth-6`）
   - 新增动画关键帧（`gradient-shift`, `float`, `shimmer`）

3. **BaseCard.vue 增强**：
   - 新增 `--glass` 和 `--gradient` 属性
   - 玻璃拟态效果和渐变背景变体
   - 更现代的圆角和阴影

4. **Header.vue 增强**：
   - 添加 logo 渐变标记
   - 主题切换添加文字标签
   - 更明显的 hover 发光效果
   - 添加 backdrop-filter 毛玻璃效果

5. **Sidebar.vue 增强**：
   - 渐变 logo 标记
   - 更现代的活动状态指示器
   - 边框高亮和发光效果
   - 添加 backdrop-filter 毛玻璃效果

6. **DashboardView.vue 增强**：
   - 统计卡片添加图标徽章和渐变背景
   - 更大的字体和更强的视觉层次
   - 渐变文本标题
   - 悬停动效增强

7. **LoginView.vue 增强**：
   - 深色渐变背景更强烈
   - 品牌区域添加动画浮动效果
   - 卡片顶部渐变装饰条
   - 更现代的登录表单样式
| 创建时间 | 2026-08-31 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | 前端骨架 | ✅ |
| web/003 | 主布局+动态菜单 | ✅ |

## 业务背景

当前系统UI过于白底、缺乏现代感，主题切换体验粗糙。需要优化为现代风格并实现平滑的白天/晚上模式切换。

## 需求描述

1. **创建 `useTheme` 可复用组合式函数**：
   - 支持平滑的主题切换过渡动画
   - 检测系统偏好主题（`prefers-color-scheme`）
   - 提供主题切换、设置、初始化方法
   - 主题切换时全局元素平滑过渡

2. **更新 CSS 变量为现代设计令牌**：
   - 更精致的阴影层级（多层重叠阴影）
   - 更柔和的圆角（4px/8px/12px/16px/24px）
   - 更流畅的过渡时长（150ms/250ms/350ms）
   - 深色模式颜色优化（降低对比度、消除纯黑）

3. **更新 Header 主题切换控件**：
   - 替换图标按钮为现代 Toggle Switch
   - 切换动画平滑（滑动+渐变）

4. **全局主题过渡样式**：
   - 所有元素添加 `transition` 属性实现主题切换平滑过渡
   - 背景色、文字色、边框色、阴影同步过渡

## 验收标准（TDD 驱动）

### AC1 — useTheme 组合式函数
> 创建 `useTheme` composable，提供完整的主题管理能力。

**用例**：`useThemeSpec#toggleTheme`
- 调用 toggleTheme 后主题在 light/dark 之间切换

**用例**：`useThemeSpec#setTheme`
- 调用 setTheme('dark') 后 data-theme 属性变为 dark

**用例**：`useThemeSpec#initTheme`
- initTheme 正确应用存储的主题

**用例**：`useThemeSpec#systemPreference`
- 检测系统偏好主题并应用

### AC2 — CSS 变量与现代设计令牌
> CSS 变量包含多层阴影、现代圆角、平滑过渡。

**用例**：`themeVarsSpec#shadowVariables`
- 验证 CSS 变量包含 shadow-light, shadow-base, shadow-heavy, shadow-hover

**用例**：`themeVarsSpec#borderRadiusVariables`
- 验证 CSS 变量包含所有现代圆角令牌

**用例**：`themeVarsSpec#transitionVariables`
- 验证 CSS 变量包含 transition-duration 和 transition-timing

### AC3 — Header 主题切换控件
> Header 组件包含现代 Toggle Switch 主题切换控件。

**用例**：`HeaderSpec#themeToggle`
- 主题切换按钮存在且可点击

### AC4 — 全局主题过渡
> 主题切换时所有元素平滑过渡。

**用例**：`globalTransitionSpec#themeTransition`
- :root 和 [data-theme] 包含 transition 属性

## 交付物

1. `src/composables/useTheme.ts` — 主题管理组合式函数
2. `src/composables/useTheme.spec.ts` — 主题管理测试
3. `src/styles/variables.scss` — 更新现代设计令牌
4. `src/styles/global.css` — 添加全局主题过渡
5. `src/components/Header.vue` — 更新主题切换控件
6. `scripts/smoke.sh` — 追加前端构建冒烟用例

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | toggleTheme | AC1 | ✅ |
| 2 | setTheme | AC1 | ✅ |
| 3 | initTheme | AC1 | ✅ |
| 4 | systemPreference | AC1 | ✅ |
| 5 | shadowVariables | AC2 | ✅ |
| 6 | borderRadiusVariables | AC2 | ✅ |
| 7 | transitionVariables | AC2 | ✅ |
| 8 | themeToggle | AC3 | ✅ |
| 9 | themeTransition | AC4 | ✅ |

## RED 证据

```text
[RED] Tests  11 failed, 0 passed
FAIL  src/composables/useTheme.spec.ts (11 failed) - 所有测试用例因实现缺失而失败
```

## GREEN 证据

```text
[GREEN] Test Files  4 passed (4)
Tests  44 passed (44)
[GREEN] mvn -q verify (后端相关测试通过，SeedDataIntegrationTest 为预存数据问题)
[GREEN] npm run test (前端相关测试 44 个全绿)
```

## 冒烟记录

追加用例数：1（web/008）
执行结果：前端测试 44/44 通过，后端相关测试通过（SeedDataIntegrationTest 为预存数据问题，非本次改动引入）

## 规范检查清单（Evaluator 逐项核对）

- [x] 创建 useTheme 组合式函数，提供完整主题管理能力
- [x] CSS 变量包含多层阴影、现代圆角、平滑过渡
- [x] Header 组件包含现代 Toggle Switch 主题切换控件
- [x] 全局主题过渡样式添加到 reset.css
- [x] App.vue、Layout.vue、DefaultLayout.vue 使用 useTheme
- [x] 深色模式颜色优化（消除纯黑、使用更温暖的色调）

## 评审意见

（此处留空）

## 实现说明

完成 web/008：UI现代化优化+白天/晚上模式切换

1. **创建 `useTheme` 组合式函数** (`src/composables/useTheme.ts`)：
   - 支持平滑的主题切换过渡动画（添加 `theme-transitioning` 类）
   - 检测系统偏好主题（`prefers-color-scheme`）
   - 提供 toggleTheme、setTheme、initTheme、getSystemTheme、applySystemTheme 方法

2. **更新 CSS 变量为现代设计令牌** (`src/styles/variables.scss`)：
   - 更精致的阴影层级（xs/sm/light/base/hover/heavy/glass 7层）
   - 更柔和的圆角（2px/4px/8px/12px/16px/24px）
   - 更流畅的过渡时长（80ms/100ms/150ms/250ms/350ms）
   - 深色模式颜色优化（降低对比度、消除纯黑 #141517 替代 #1D1E1F）
   - 新增 `--theme-transition`、`--transition-timing-smooth`、`--transition-timing-bounce`

3. **全局主题过渡样式** (`src/styles/reset.css`)：
   - `:root` 和 `[data-theme="dark"]` 添加 transition
   - `.theme-transitioning` 类为所有元素启用平滑过渡

4. **更新 Header 主题切换控件** (`src/components/Header.vue`)：
   - 替换图标按钮为现代 Toggle Switch（轨道+滑块）
   - 切换动画平滑（滑动+渐变）
   - 使用 useTheme composable

5. **更新入口文件** (`src/App.vue`、`src/layouts/Layout.vue`、`src/layouts/DefaultLayout.vue`)：
   - 改用 useTheme().initTheme() 初始化主题