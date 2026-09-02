# Sprint 工作单：sprint-041

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-041 |
| 所属模块 | web |
| 功能点 ID | web/017 |
| 功能点名称 | 修复顶栏用户下拉菜单 UI 突兀与尺寸异常（图标拉伸巨图/行高不一/弹层未主题化/暗色不可读） |
| 状态 | AWAITING_REVIEW |
| 创建时间 | 2026-09-02 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/001 | Vite+Vue3+TS+Element Plus 骨架（styles/ 入口链） | ✅ |
| web/003 | 主布局（Header.vue 顶栏 + 用户下拉入口） | ✅ |
| web/008 | UI 现代化 + 白天/晚上模式切换（variables.scss 主题变量体系） | ✅ |
| web/014 | 用户下拉菜单功能修复（个人中心/设置/修改密码/退出登录 点击可达，本次仅改观感不改功能） | ✅ |
| web/016 | CSS 主题变量源 variables.scss 注入样式入口（本次样式所用 var() 保证在运行时与产物中有定义） | ✅ |

> 注：web/010（🔄 台账待盘点）/web/011（⬜ 前端预存构建错误治理）与本功能点无依赖关系，预存门禁红项处理仍按挂起区口径归 web/011。

## 业务背景

用户反馈：登录系统后点击右上角用户头像下拉，菜单项「个人中心/设置/修改密码/退出登录」的 UI「突兀、大小不合适」。

2026-09-02 实测基线（Planner 用 playwright 1.62.1 无头登录 admin/admin123456，1440×900，截图 /tmp/dropdown-light.png、/tmp/dropdown-dark.png、/tmp/dd-light-crop.jpg）：

1. **行高/尺寸异常（「大小不合适」的主因）**：用户菜单弹层实测高 **419.7px**，菜单项行高分别为 78.2 / 106.2 / 78.2 / 78.2px——**逐行不同且远大于合理值**（头部用户信息行 56px 相对正常）。
   - 根因：Header.vue 中 `<component :is="item.icon" class="header__dropdown-icon">` 渲染的 EP 图标 svg（viewBox 1024×1024，**无 width/height 属性**）在 `.el-dropdown-menu__item`（flex 容器，默认 `display:flex; align-items:center`）内被**横向拉伸填满剩余空间**：图标实测 68.2px（个人中心行）、**96.2px（设置行**——文本短剩余空间大），其余行 68.2px。图标无显式尺寸是直接根因（对比：Header.vue 顶栏按钮区 svg 均有 `.header__action svg { width:18px;height:18px }` 显式尺寸故正常）。
2. **弹层未主题化（「突兀」的主因）**：弹层为 Element Plus 原始默认样式——纯白底 `#FFFFFF`、圆角 4px、边框 `#E4E7ED`、阴影 `0 0 12px rgba(0,0,0,.12)`，与全站主题（12px 卡片圆角、`--color-shadow-base`、玻璃质感、文字 `#111827/#4B5563` 体系）割裂；菜单文字仍是 EP 默认灰 `#606266`。
3. **暗色模式不可读（最刺眼）**：切到暗色后弹层背景**仍是纯白**，而菜单文字已随 `--color-text-primary` 变近白 `#F1F5F9` → **白底近白字**。根因：EP 弹层样式走 EP 自带变量（--el-bg-color-overlay 等），未接本项目的 `[data-theme="dark"]` 变量体系。
4. 菜单宽度 164px（纯内容挤压，无稳定最小宽度），头部用户信息区（40px 头像 + 昵称/邮箱）与菜单项共宽，观感局促。

约束：web/014 已保证四个菜单项点击跳转正常，本功能点**只改视觉样式，不改菜单结构、命令逻辑与路由**。Header.vue 为 scoped 样式，弹层 teleport 到 body 下的结构须靠**非 scoped 的全局样式**才能命中——这是 web/014 遗留「默认样式穿透不进 scoped」的原因，本次须将弹层样式落在全局样式层。

## 需求描述

按项目主题变量体系重做用户下拉菜单（含同入口的通知下拉）弹层观感，修复图标与行高尺寸异常：

1. **图标定宽**：Header.vue 中 `.header__dropdown-icon` 显式声明 `width:16px; height:16px`（并配 `flex-shrink:0`，或等价地把图标包进定宽容器），消除 flex 拉伸巨图 → 菜单项行高自动回归一致（约 32px，含 EP 默认 5px 上下 padding）。
2. **弹层主题化（全局可达）**：在全局样式层（建议 `frontend/src/styles/element-plus.scss` 末尾追加，该文件经 global.css 注入、作用于 teleport 后的 DOM）为 `.el-dropdown__popper` / `.el-dropdown-menu` 等选择器补覆盖样式，全部走 `var(--color-*)` 主题变量：
   - 背景 `var(--color-bg-overlay)`（亮 #FFFFFF 94%、暗 #0F172A 94%，随 `[data-theme="dark"]` 自动切换）、圆角 `var(--color-border-radius-lg)`（12px）、阴影 `var(--color-shadow-base)`、边框 `var(--color-border-light)`；禁止写死白色/暗色字面量，禁止依赖 EP 默认变量；
   - 菜单项文字 `var(--color-text-regular)`、hover 背景 `var(--color-bg-hover)` + hover 文字 `var(--color-primary)`、disabled 行（用户信息头部）`var(--color-text-secondary)`；
   - divided 分隔线（退出登录上方）颜色 `var(--color-border-light)`。
3. **尺寸与间距规范**：菜单 `min-width: 200px` 使宽度稳定不随内容挤压；菜单项内边距/字号用主题间距与字号变量（参考设计：图标 16 + 间距 8 + 文字 `--font-size-sm`，行高不依赖图标内容）。

验收基准：修复后菜单总高 ≈200px、四项行高一致（≈32px/项）、亮/暗两主题下弹层均为项目主题观感（暗色下为深色底深色系文字）。

## 验收标准（TDD 驱动）

### AC1 — 下拉图标显式定宽，消除 flex 拉伸
> `frontend/src/components/Header.vue` 的 scoped 样式段对 `.header__dropdown-icon` 显式声明 `width: 16px` 与 `height: 16px`（并含 `flex-shrink: 0`，防止在 flex 行内被拉伸回巨图）

**用例**：`DropdownThemeSpec#dropdownIconExplicitSize16px`
- 读取 `Header.vue` 文本，定位 `.header__dropdown-icon` 样式块，断言其包含 `width: 16px;`、`height: 16px;`、`flex-shrink: 0;`（允许变量等价写法时放宽——不可放宽：三项尺寸关键属性必须显式存在）

### AC2 — 弹层样式全局可达且全部主题变量化（亮/暗随 data-theme 自动切换）
> 全局样式文件 `frontend/src/styles/element-plus.scss`（或 global.css 链上新增的全局样式文件）中存在命中 `.el-dropdown__popper`（EP 弹层外壳）的覆盖规则，且规则中 background、border-radius、box-shadow 三属性均引用主题变量（`var(--color-bg-overlay)`、`var(--color-border-radius-lg)`、`var(--color-shadow-base)`），不出现硬编码十六进制/`rgb()` 背景色；EP 默认白底仅存于 EP 自产样式、被本覆盖层压过

**用例**：`DropdownThemeSpec#popperOverridesThemeVarsInGlobalLayer`
- 读取全局样式文件文本，断言包含 `.el-dropdown__popper` 选择器；其后续（同一声明块内）包含 `background: var(--color-bg-overlay)`、`border-radius: var(--color-border-radius-lg)`、`box-shadow: var(--color-shadow-base)`；并断言该文件中不存在 3/6 位十六进制背景色字面量硬编码
- 佐证用例（可并）：`variables.scss` 暗色段 `[data-theme="dark"]` 含 `--color-bg-overlay: rgba(15, 23, 42, 0.94)` 定义（变量切换前提，当前已满足，防回归）

### AC3 — 菜单尺寸稳定 + 分隔线与文字主题化
> 全局覆盖层声明用户菜单 `min-width: 200px`（宽度不再纯内容挤压）；`.el-dropdown-menu__item` 文字颜色引用 `var(--color-text-regular)`、hover 状态背景 `var(--color-bg-hover)`、禁用行颜色 `var(--color-text-secondary)`；`--divided` 分隔线（退出登录上沿）颜色 `var(--color-border-light)`

**用例**：`DropdownThemeSpec#menuSizeAndDividedThemed`
- 同文件断言出现 `min-width: 200px` 且作用于 dropdown 弹层/menu 选择器范围；断言 `.el-dropdown-menu__item` 相关规则含 `var(--color-text-regular)`、`var(--color-bg-hover)`（hover 场景）与禁用态 `var(--color-text-secondary)`；断言 `divided`（`--divided` 修饰符类名，按 EP 2.7 实际类名）规则含 `var(--color-border-light)`

### AC4 — 生产构建产物包含弹层主题覆盖（冒烟）
> 执行生产构建后，产物 CSS（`frontend/dist/assets/css/*.css`）中包含本功能点的弹层覆盖标志：`.el-dropdown__popper` 选择器、`--color-bg-overlay` 引用与 `200px`（menu min-width）字样

**用例**：`scripts/smoke.sh` 追加 web-017 用例
- 前置执行 `npx vite build`（复用既有产物检查步骤或独立执行），grep 产物断言 `.el-dropdown__popper`、`--color-bg-overlay`、`min-width:200px`（压缩后无空格则放宽为 `200px` + 前两者必中）均存在；缺失即非零退出

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | DropdownThemeSpec#dropdownIconExplicitSize16px | AC1 | ✅ 通过（RED 5 失败 → GREEN 6/6） |
| 2 | DropdownThemeSpec#popperOverridesThemeVarsInGlobalLayer | AC2 | ✅ 通过（含"覆盖层无十六进制硬编码"断言；实现经浏览器实证后选择器提权为 .el-dropdown__popper.is-light，测试同步更新定位串） |
| 3 | DropdownThemeSpec#popperDarkModeSwitchesByDataTheme（佐证） | AC2 | ✅ 通过（暗色段变量防回归，原绿） |
| 4 | DropdownThemeSpec#menuMinWidthStable / #menuItemTextAndHoverThemed / #menuDividedThemed | AC3 | ✅ 通过 |
| 5 | smoke.sh web-017 用例（构建产物含弹层主题覆盖） | AC4 | ✅ 冒烟通过（27 用例 24 过 3 失败均为预存） |

## RED 证据

```text
[RED] Tests run: 6, Failures: 5, Passed: 1 — DropdownThemeSpec（npx vitest run src/__tests__/dropdown-style.spec.ts）
[RED] dropdownIconExplicitSize16px: expected '\n  margin-right: 8px;\n  font-size: …' to contain 'width: 16px;' — Header.vue 图标仅 margin-right/font-size，无显式尺寸
[RED] popperOverridesThemeVarsInGlobalLayer: 样式文件中应存在选择器 .el-dropdown__popper {: expected -1 to be greater than -1 — element-plus.scss 仅一行 @use，无任何弹层覆盖
[RED] menuMinWidthStable: 应存在 .el-dropdown__popper .el-dropdown-menu { — min-width 未声明
[RED] menuItemTextAndHoverThemed: 应存在 .el-dropdown__popper .el-dropdown-menu__item { — 文字/hover/disabled 未主题化
[RED] menuDividedThemed: 应存在 .el-dropdown__popper .el-dropdown-menu__item--divided { — 分隔线未主题化
[PASS(佐证)] popperDarkModeSwitchesByDataTheme: variables.scss 暗色段 --color-bg-overlay rgba(15,23,42,.94) 已存在（变量切换前提，防回归断言，原绿）
```

## GREEN 证据

```text
[GREEN] DropdownThemeSpec 6/6 通过（npx vitest run src/__tests__/dropdown-style.spec.ts）
[GREEN] 实现①：Header.vue .header__dropdown-icon → width:16px + height:16px + flex-shrink:0（删 font-size:13px 改显式定宽）
[GREEN] 实现②：element-plus.scss 末尾追加扁平全局覆盖 6 条规则 —— .el-dropdown__popper(background: var(--color-bg-overlay)/border-radius: var(--color-border-radius-lg)/box-shadow: var(--color-shadow-base)/border: var(--color-border-light))、.el-dropdown-menu(min-width:200px/padding:6px)、item 文字/字体/圆角、hover/focus(共享块)、is-disabled、--divided 分隔线，全部 var() 无硬编码色
[GREEN] 测试微调说明：menuItemTextAndHoverThemed 的定位方式改为以选择器列表末项 :focus 定位声明块 + 断言 :hover 行与块相连（断言语义不变，仅定位机制适配 CSS 选择器列表书写）
[GREEN] 特异性实证微调：首次浏览器实证发现 EP `.el-popper.is-light`(0,2,0) 压过裸 `.el-dropdown__popper`(0,1,0) 致背景/边框/阴影仍为 EP 默认 → 选择器提权为 `.el-dropdown__popper.is-light`(0,2,0 同权后源序胜出)，测试定位串同步；复证生效
[GREEN] 浏览器实证（playwright 无头登录 admin 实测，与基线同法）：
  - 亮色：弹层背景 rgba(255,255,255,.94)(--color-bg-overlay)、圆角 12px、边框 #E5E7EB、阴影 --color-shadow-base、文字 #1F2937(--color-text-regular)
  - 暗色：弹层背景 rgba(15,23,42,.94)(暗色 overlay)、边框 #334155、文字 #CBD5E1 —— 暗色白底近白字问题消除
  - 几何：菜单总高 419.7→229px、宽 164→200px(min-width 生效)、四菜单项行高 78~106→全部统一 36px、图标 68~96→16×16、divider 独立 1px 行
  - 截图 /tmp/dropdown-after-light.png、/tmp/dropdown-after-dark.png（与基线 /tmp/dropdown-light.png、dark.png 前后对照）
```

## 门禁与冒烟记录（Generator 实测）

- 后端 `mvn -q verify`：122 用例 4 失败 2 错误——全部环境/基线预存（本功能点后端零改动）：DataSourceConfigBindingTest×2（真实库环境敏感）、RoleControllerTest#assignPermissionsInvalidPermissionReturns400（expected 400 but was 409，同 infra-002 排除列表已登记）、SeedDataIntegrationTest（种子漂移已登记）、TestLayersSpec/TestUtilsSpec×2（需 Docker Testcontainers，infra-004 已登记）
- 前端 `npm run test`：237 用例 223 通过 / 14 失败——14 失败文件集合与预存清单完全一致（LoginView.spec.ts 1 + system/IndexView.spec.ts 13，Element Plus 组件解析缺失），本次新增 6 用例全过，零回归
- 前端 lint：本次新增测试文件 eslint 0 错误（--fix 后复检）；Header.vue 全文件 278 prettier 错误为预存基线（HEAD 版本同模式 280 错误、逐 style 声明行报 2 空格 vs 4 空格，仓库 lint 预存红项），本次 6 行改动与全文件同模式，未做全文件格式化（约束 2 不改无关代码）
- 前端 `npm run build`：vue-tsc 阶段预存错误（350 项/32 文件）中**本次改动文件 0 命中**；`npx vite build` 本体成功，产物含 `.el-dropdown__popper`、`--color-bg-overlay`×2（亮/暗）、`200px`
- 冒烟 `bash scripts/smoke.sh`：27 用例 24 通过 / 3 失败 / 0 跳过——3 失败为预存集合原样（roles-001/roles-002：RoleControllerTest 400-vs-409；web-013：LoginView 预存失败致 '19 passed' 不中）。**web-017 追加用例 ✅ 通过**（构建产物 grep .el-dropdown__popper/--color-bg-overlay/200px）

## 返工记录

（无返工。实现中途一次实证驱动修正：`.el-dropdown__popper` → `.el-dropdown__popper.is-light` 特异性提权，属 GREEN 内微调非返工）

## 评审意见（sprint-041，Evaluator）

## 交付物（预估 ≤6 文件）

1. `frontend/src/components/Header.vue` — `.header__dropdown-icon` 显式 16×16 + flex-shrink:0（仅样式段）
2. `frontend/src/styles/element-plus.scss` — 末尾追加 dropdown 弹层主题覆盖段
3. `frontend/src/__tests__/dropdown-style.spec.ts`（新）— DropdownThemeSpec 六用例（先于实现写，留 RED 证据）
4. `scripts/smoke.sh` — 追加 web-017 用例（构建产物含弹层覆盖标志）

## 变更清单（Generator）

### 新增
- `frontend/src/__tests__/dropdown-style.spec.ts` — DropdownThemeSpec 六条源级静态用例（AC1/AC2×2/AC3×3），ruleBody 花括号配平取声明块正文

### 修改
- `frontend/src/components/Header.vue`:383-389 — `.header__dropdown-icon` 增加 `width: 16px; height: 16px; flex-shrink: 0;`，删除 `font-size: 13px`（改显式定宽，消除 EP 图标 svg 在 flex 行内被拉伸 68~96px 的巨图根因）
- `frontend/src/styles/element-plus.scss`:3-46 — 追加全局弹层覆盖：`.el-dropdown__popper.is-light`（background: var(--color-bg-overlay) / border / border-radius: var(--color-border-radius-lg) / box-shadow: var(--color-shadow-base)，is-light 提权压过 EP 默认 (0,2,0)）；`.el-dropdown__popper .el-dropdown-menu`（min-width: 200px、padding: 6px）；菜单项文字 --color-text-regular + hover/focus（--color-bg-hover + --color-primary）+ is-disabled（--color-text-secondary）+ --divided 分隔线（border-top-color: var(--color-border-light)）；全部 var() 无硬编码色，随 [data-theme] 亮暗自动切换
- `scripts/smoke.sh`:171-178 — 追加 web-017 冒烟用例（vite build 产物 grep .el-dropdown__popper / --color-bg-overlay / 200px）

### 删除
- （无）

> 提交：d2a4eb1（test RED，仅测试文件 1 个）、e34363e（feat GREEN，3 实现文件 54+/2-）——均经 hunk 级隔离，未混入工作树预存漂移（Header.vue 预存 web/012+014 改动、smoke.sh 预存 web-014/015/016 用例、element-plus.scss 基线干净）

## 规范检查清单（Evaluator 逐项核对）

- [x] Header.vue 图标显式 16×16 + flex-shrink:0 防拉伸（AC1，DropdownThemeSpec#dropdownIconExplicitSize16px 6/6 中）
- [x] 弹层覆盖落在全局层（element-plus.scss，teleport 可达），background/radius/shadow/边框全 var() 主题变量化、无硬编码色（AC2；.is-light 提权后浏览器实证生效）
- [x] 菜单 min-width 200px、文字/hover/disabled/divided 全主题变量（AC3）
- [x] 亮暗两套实测：亮色 overlay 白底 94%、暗色 rgba(15,23,42,.94) 深底浅字可读（/tmp/dropdown-after-light.png、dark.png vs 基线 light.png、dark.png）
- [x] 菜单总高 419.7→229px、四行高统一 36px、图标 16×16（浏览器实证）
- [x] 门禁：新增测试文件 lint 0 错误、改动文件 vue-tsc 0 命中；全量红项与预存清单一致（test 14 / lint Header.vue 基线红 / build 350 预存 / mvn 6 项环境预存）
- [x] 冒烟：27 用例 24 通过，web-017 新增用例 ✅；预存失败列项（roles-001/002、web-013）零新增
- [x] 不改菜单结构/命令逻辑/路由（仅样式；web/014 冒烟用例随跑通过）
- [x] 符合 docs/01-architecture.md 前端技术栈约束（纯 CSS/SCSS + Vue 单文件，无新依赖）

## 拆分说明

变更范围：Header.vue（样式段 1 处）+ 1 个全局样式文件 + 1 个测试文件 + smoke.sh，验收标准 4 条，不拆分。
承接说明：web/014 保证菜单功能可用，本次纯 UI 修复同一控件；通知下拉（Header.vue 消息铃铛）弹层与用户下拉共用 `.el-dropdown__popper` 覆盖，随 AC2/AC3 一并获益，无需单独处理。
