# Sprint 工作单：sprint-064

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-064 |
| 所属模块 | web |
| 功能点 ID | web/033 |
| 功能点名称 | 登录页新增显示密码功能（密码框可见性切换按钮） |
| 状态 | AWAITING_REVIEW |
| 创建时间 | 2026-09-10 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/002 | 登录页+路由守卫 | ✅ |
| web/013 | 修复登录页密码输入框默认明文显示问题（默认掩码基线） | ✅ |

## 业务背景

登录页密码框当前固定以掩码方式显示密码（`frontend/src/views/LoginView.vue` 中 BaseInput 绑定 `:show-password="false"`），用户输入时无法核对密码字符，易导致输错、登录失败重试。BaseInput 组件本身已具备可见性切换能力（`showPassword` prop + `.base-input__suffix` 眼睛图标 + `togglePassword` 切换 `type=password/text`），但登录页显式关闭了该能力，且 BaseInput 样式中 `.base-input__suffix` 继承 `pointer-events: none`，真实浏览器下点击不会命中切换按钮。本次在登录页启用该功能并使按钮在浏览器中真实可点击，同时必须保持 web/013 既有语义：默认掩码，仅在用户主动点击切换按钮时明文显示。

## 需求描述

1. **启用切换按钮**：登录页密码框 `BaseInput` 的 `show-password` 置为 `true`，密码框默认 `type=password`（掩码）并在输入框尾部渲染可见性切换按钮（眼睛图标）。
2. **交互切换**：点击切换按钮，密码由掩码切换为明文（`type=text`）；再次点击恢复掩码（`type=password`）；切换过程中已输入的密码值不变。
3. **浏览器可点击**：修复 BaseInput 切换按钮的 CSS `pointer-events: none` 问题，使生产构建产物中切换按钮声明 `pointer-events: auto`，真实浏览器可点击。
4. **不回归**：登录提交 payload（username/password）、表单校验、既有 LoginView 全部测试用例不受影响。

## 验收标准（TDD 驱动）

### AC1 — 密码框默认掩码且渲染可见性切换按钮 ✅ 达成（2026-09-10，用例 1 通过）
> 挂载 LoginView 后，`input[placeholder="请输入密码"]` 的 `type="password"`；该密码 BaseInput 的 `showPassword` prop 为 `true`；其内部存在 `.base-input__suffix` 切换按钮元素。

**用例**：`AC1 ← 用例 LoginView.spec.ts#PasswordVisibility > renders password masked with toggle rendered by default`

### AC2 — 点击切换按钮在明文/掩码间双向切换 ✅ 达成（2026-09-10，用例 2 通过）
> 初始 `type=password`；点击 `.base-input__suffix` 一次后该 input `type="text"`；再次点击后恢复 `type="password"`。

**用例**：`AC2 ← 用例 LoginView.spec.ts#PasswordVisibility > toggle switches password type to text and back`

### AC3 — 构建产物 CSS 保证切换按钮真实可点击 ✅ 达成（2026-09-10，冒烟 web-033 grep 通过）
> `npm run build` 产物 `dist/assets/css/*.css`（实际为组件 chunk `BaseInput-*.css`）中 `.base-input__suffix` 选择器规则包含 `pointer-events: auto` 声明。（路径口径修正：Planner 原稿写 `index-*.css`，实测样式按组件分 chunk，修正为 glob 匹配，不改变判定语义）

**用例**：`AC3 ← 用例 bash scripts/smoke.sh#web-033 登录页密码可见性切换验证`

### AC4 — 切换显隐不改变密码值且登录提交 payload 不变 ✅ 达成（2026-09-10，用例 3 通过，全量零新增失败）
> 输入密码后点击切换按钮，`loginForm.password` 仍等于输入原值；随后触发 `handleLogin()`，`authApi.login` 收到的参数 `{username, password, rememberMe}` 与切换前一致；Password Visibility 3 条用例全部通过。回归口径：`LoginView.spec.ts` 失败数不超过既有基线 2 条（`handles successful login response`、web/028 redirect，均属 web/011 预存红项，本次零新增失败）。

**用例**：`AC4 ← 用例 LoginView.spec.ts#PasswordVisibility > toggle preserves password value and login payload`

> AC4 修订记录（Planner 于 Generator 执行前修订，2026-09-10）：原稿「LoginView.spec.ts 全量用例通过（0 failed）」与预存基线冲突（实现前实测基线即 2 failed | 22 passed，属 web/011 范围），修订为「零新增失败」对照口径，修订已登记 session-state 挂起区。

## 测试清单

> 先于实现写出，规则遵循 `.harness/rules/tdd-workflow.md`；Generator 运行确认 RED 后填入实际输出。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | PasswordVisibility > renders password masked with toggle rendered by default | AC1 | ✅ GREEN（先 RED：expected false to be true；后 1 passed） |
| 2 | PasswordVisibility > toggle switches password type to text and back | AC2 | ✅ GREEN（先 RED：切换按钮不存在；后 1 passed） |
| 3 | PasswordVisibility > toggle preserves password value and login payload | AC4 | ✅ GREEN（先 RED：切换按钮不存在；后 1 passed） |
| 4 | 构建产物 `.base-input__suffix` 含 `pointer-events: auto` | AC3 | ✅ GREEN（BaseInput-DEEZ2xz8.css 实测含 `cursor:pointer;pointer-events:auto`） |
| 5 | LoginView.spec.ts 全量回归（既有登录/跳转/错误/回显用例） | AC4 | ✅ 22 passed / 2 failed（与实现前基线完全一致，零新增失败） |
| 6 | 冒烟 web-033 登录页密码可见性切换验证 | AC1~AC4 | ✅ 通过（单跑 exit=0：3 passed + 构建 + grep） |

## RED 证据

- 执行：`npm run test -- --run src/views/LoginView.spec.ts -t "Password Visibility"`（实现前，2026-09-10）
- 关键失败输出（Test Files 1 failed，Tests 3 failed | 21 skipped）：

```text
[RED] FAIL  LoginView > Password Visibility (AC1/AC2/AC4) > renders password masked with toggle rendered by default (AC1)
AssertionError: expected false to be true // LoginView.spec.ts:400（showPassword prop 为 false，期望 true）
[RED] FAIL  LoginView > Password Visibility (AC1/AC2/AC4) > toggle switches password type to text and back (AC2)
AssertionError: expected false to be true // LoginView.spec.ts:414（.base-input__suffix 切换按钮不存在）
[RED] FAIL  LoginView > Password Visibility (AC1/AC2/AC4) > toggle preserves password value and login payload (AC4)
AssertionError: expected false to be true // LoginView.spec.ts:435（.base-input__suffix 切换按钮不存在）
```

- AC3（构建产物 pointer-events）：当前 `BaseInput.vue` 的 `.base-input__suffix` 继承 `pointer-events: none`，无 `pointer-events: auto` 声明 → 冒烟 web-033 grep 预期 RED（在冒烟步骤留痕）。
- 根因：`LoginView.vue:185` 密码框 `:show-password="false"` 关闭了 BaseInput 的切换能力。

## GREEN 证据

- `frontend/src/views/LoginView.vue:185` — `:show-password="false"` → `:show-password="true"`（启用切换按钮）
- `frontend/src/components/BaseInput.vue:39` — `const showPassword = ref(false)`：内部状态与 prop 解耦，prop 仅控制切换按钮显隐、初始始终掩码（修复 prop=true 时初始明文的语义错误，对齐 Element Plus 语义）
- `frontend/src/components/BaseInput.vue:284-286` — `.base-input__suffix` 补充 `pointer-events: auto`（覆盖共享规则的 `pointer-events: none`，真实浏览器可点击）
- `npm run test -- --run src/views/LoginView.spec.ts -t "Password Visibility"` → **3 passed**（Test Files 1 passed）
- 全量前端测试：`Tests 20 failed | 268 passed (288)`，20 失败与实现前基线（2026-09-10 实测）逐条一致，零新增
- 构建产物实测：`dist/assets/css/BaseInput-DEEZ2xz8.css` 含 `.base-input__suffix[data-v-dcc27bdb]{cursor:pointer;pointer-events:auto}`
- `npm run lint` exit 0；`npm run build` ✓ built in 16.91s

## 门禁与冒烟记录

- 后端门禁：`mvn -q verify` ❌ Tests run: 130, Failures: 4, Errors: 2 —— 全部为预存/环境类（Docker 缺失 TestLayersSpec/TestUtilsSpec、真实库种子 sys_role 22≠2、DataSourceConfigBinding 环境变量绑定 ×2、RoleControllerTest#assignPermissionsInvalidPermissionReturns400 expected 400 was 409 已登记）；本次零后端改动，用户裁决基线对照推进（2026-09-10）
- 前端门禁：`npm run lint` ✅ exit 0；`npm run test` 288 用例 20 failed | 268 passed（失败清单与实现前基线完全一致，零新增）✅；`npm run build` ✅ 16.91s
- 冒烟新增用例：`web-033 登录页密码可见性切换验证`（单跑 ✅ exit=0，覆盖 AC1/AC2/AC4 单测 + AC3 构建产物 grep）
- 冒烟用例数：1（新增）
- 冒烟结果：整体 ❌（45 用例 10 失败，全部为实现前基线预存：infra-001 健康探测 90s 超时、model-008/010 接口 spec 方法数 6≠7、roles-001/002 409 已登记、web-013/026 grep 计数过期、web-020/021/022 构建产物 grep 过期且 web/022 已废弃⚠️）——用户裁决「基线对照推进」，预存红项已登记 session-state 挂起区交 Evaluator 对照裁决

## 拆分说明

预估验收标准 4 条（未超 4 条）、业务文件变更 3 个（`LoginView.vue`、`LoginView.spec.ts`、`scripts/smoke.sh`，BaseInput.vue 至多 1 行样式修正，未超 6 个），不触发拆分，作为单独 Sprint 交付。

## 交付物（预估 3~4 文件）

1. `frontend/src/views/LoginView.vue` — 密码框 `:show-password` 置为 `true`
2. `frontend/src/views/LoginView.spec.ts` — Password Visibility 用例改写/新增（AC1/AC2/AC4，先于实现）
3. `frontend/src/components/BaseInput.vue` — 切换按钮 `pointer-events: auto` 样式修正（1 行级）
4. `scripts/smoke.sh` — 追加 `web-033` 冒烟用例

## 变更清单

### 修改

- `frontend/src/views/LoginView.vue`：
  - 密码框 BaseInput 的 `:show-password="false"` 改为 `:show-password="true"`（默认掩码语义不变，仍为 `type="password"`）
- `frontend/src/components/BaseInput.vue`：
  - 切换按钮 `.base-input__suffix`（`togglePassword` 所在 span）补充 `pointer-events: auto`，覆盖 `&__prefix,&__suffix` 规则中的 `pointer-events: none`；仅作用于可点击切换按钮，不影响 prefix/纯展示 suffix 的既有行为
- `frontend/src/views/LoginView.spec.ts`：
  - 改写 `Password Visibility (AC1, AC2)` 既有用例（当前断言 `showPassword` prop 为 `false`、切换按钮存在时才断言，属条件断言）为 AC1/AC2/AC4 三条确定性用例
- `scripts/smoke.sh`：
  - 追加 `web-033` 用例：Password Visibility 3 条单测通过（`-t` 过滤，规避预存失败干扰）+ `npx vite build` 后 grep `dist/assets/css/*.css` 中 `.base-input__suffix` 含 `pointer-events: auto`（未删除、未改动既有用例）

## 规范检查清单

- [~] `mvn -q verify` 后端门禁：基线预存 6 失败（零后端改动，用户裁决基线对照，登记挂起区）
- [x] `npm run lint && npm run test && npm run build` 前端门禁通过（lint exit 0、build 通过、test 零新增失败=基线 20 条）
- [~] `bash scripts/smoke.sh`：新增 web-033 单跑 ✅；整体 45 用例 10 失败均为实现前预存（用户裁决基线对照，登记挂起区）
- [x] 符合 Vue 3 / Vite / Element Plus 编码规范
- [x] 符合 TDD 工作流（测试先行、RED 证据完整、GREEN 实现、REFACTOR 无坏味道）
