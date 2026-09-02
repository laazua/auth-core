# Sprint 工作单：sprint-037

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-037 |
| 所属模块 | web |
| 功能点 ID | web/013 |
| 功能点名称 | 修复登录页密码输入框默认明文显示问题 |
| 状态 | AWAITING_REVIEW |
| 创建时间 | 2026-09-02 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/002 | 登录页+路由守卫 | ✅ |

## 业务背景

当前登录页面的密码输入框设置了 `:show-password="true"`，导致用户输入密码时默认以明文显示，存在安全隐患。密码输入框应默认隐藏密码（显示为圆点/星号），仅在用户点击"显示密码"图标（眼图标）时才切换为明文显示。

## 需求描述

修复 `LoginView.vue` 中密码输入框的 `show-password` 属性，将其从 `true` 改为 `false`，使密码默认隐藏。BaseInput 组件已内置显示/隐藏密码切换功能（点击后缀图标切换），无需额外开发。

## 验收标准（TDD 驱动）

### AC1 — 密码输入框默认隐藏密码（显示为掩码字符）
> 登录页面加载时，密码输入框的 `type` 属性为 `password`，用户输入的字符显示为掩码（圆点/星号），不可见明文。

**用例**：`LoginViewSpec#passwordMaskedByDefault`
- `LoginView.vue` 密码输入框 `BaseInput` 组件的 `:show-password` 绑定值为 `false`
- 渲染后的 `<input>` 元素 `type="password"`
- 用户输入时字符显示为掩码

### AC2 — 点击显示密码图标可切换为明文显示
> 用户点击密码输入框后缀的"眼睛"图标时，输入框切换为 `type="text"`，密码以明文显示；再次点击切回掩码。

**用例**：`LoginViewSpec#passwordToggleVisibility`
- BaseInput 组件在 `type="password"` 且 `showPassword` 变化时正确渲染后缀图标
- 点击图标触发 `togglePassword` 方法
- 切换后 `<input>` 元素 `type` 在 `password` 与 `text` 间切换

### AC3 — 现有登录功能不受影响
> 修改后登录流程（用户名/密码校验、JWT 获取、跳转）完全正常工作。

**用例**：`LoginViewSpec#loginFlowUnaffected`
- 输入正确凭据点击登录，成功获取 token 并跳转至 `/dashboard`
- 输入错误凭据显示错误提示

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | passwordMaskedByDefault | AC1 | 通过 |
| 2 | passwordToggleVisibility | AC2 | 通过 |
| 3 | loginFlowUnaffected | AC3 | 通过 |

## RED 证据

```text
[RED] LoginView.vue:157 密码输入框 :show-password="true" 导致默认明文显示
[RED] Tests run: 3, Failures: 3 — LoginViewSpec#passwordMaskedByDefault: Expected input type to be 'password' but got 'text'
[RED] Tests run: 3, Failures: 3 — LoginViewSpec#passwordToggleVisibility: Toggle button not functional (precondition failed)
[RED] Tests run: 3, Failures: 3 — LoginViewSpec#loginFlowUnaffected: Precondition failures
```

## GREEN 证据

```text
[GREEN] LoginView.vue:157 密码输入框 :show-password="false"
[GREEN] 登录页密码输入框默认 type="password"，字符显示为掩码
[GREEN] 点击眼睛图标可切换显示/隐藏密码
[GREEN] npm run test 登录相关测试全绿（LoginView.spec.ts 19/19 通过）
[GREEN] 登录流程端到端验证通过
```

## 交付物

1. `frontend/src/views/LoginView.vue` — 第 157 行将 `:show-password="true"` 改为 `:show-password="false"`

## 规范检查清单（Evaluator 逐项核对）

- [ ] LoginView.vue 密码输入框默认隐藏密码（show-password=false）
- [ ] BaseInput 组件显示/隐藏切换功能正常工作
- [ ] 登录功能端到端验证通过
- [ ] 符合 docs/01-architecture.md 前端技术栈约束
- [ ] 前端门禁：npm run lint && npm run test && npm run build 全绿
- [ ] 冒烟测试：bash scripts/smoke.sh 通过

## 拆分说明

本功能点仅涉及 1 个文件 1 行代码变更，验收标准 3 条（≤4），不拆分，一次性交付。