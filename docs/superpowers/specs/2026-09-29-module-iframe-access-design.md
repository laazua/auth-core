# web/038 模块服务内嵌访问 — 设计文档

- 日期：2026-09-29
- 状态：设计已获用户逐节确认，待用户书面评审后移交 harness Planner 转 task.md
- 关联：modules/002（sprint-069 ✅，网关准入+转发已交付）；modules/003（服务凭证，未注册，本设计不涉及）
- 预留 ID：**web/038**（registry 备注需扩展语义：由「模块管理页外部服务语义适配」扩为「模块服务统一入口与内嵌访问」）

## 1. 需求与目标

用户目标：模块配置完成后，在当前系统中点击该模块的 URL，**仍在当前系统内**打开并使用该模块的外部服务。

澄清结论（4 项，用户逐题确认）：
1. 模块服务形态 = **有 Web 页面的外部系统**（浏览器可直接浏览其 UI）
2. 交互形态 = **系统内嵌页面（iframe）**，始终在系统框架内，地址栏仍是当前系统域名
3. 鉴权口径 = **当前系统统一 JWT**：管理员授权后，用户能看到自己可访问的模块并访问之
4. 入口 = **新菜单页（我的模块）+ 模块管理页操作列「进入」**

## 2. 架构与数据流

```
管理员                          普通用户
  │                               │ 登录 POST /auth/login
  │  角色→权限→模块授权（现状）     │ → 返回 token（axios 仍用 Bearer）
  │                               │   + Set-Cookie: HttpOnly, SameSite=Lax
  ▼                               ▼
模块管理页(看全部) ─加入口「进入」  我的模块页（新菜单，GET /modules/accessibles 按权限过滤）
                                        │ 点卡片
                                        ▼
                              系统内路由页 /workspace/module/:code
                              <iframe src=/api/v1/gateway/{code}/…>
                                        │ 同源导航，浏览器自动带 Cookie
                                        ▼
                              网关：Cookie/JWT 认证 → 模块级准入 → 按 base_url 转发
                                        ▼
                              外部系统 HTML/UI 原样渲染在系统框架内
```

三条核心语义：
1. **统一 JWT 双承载**：API 调用继续 `Authorization: Bearer`（前端现状零改动）；同源 iframe 导航用 `HttpOnly Cookie`。Cookie 仅是 JWT 的另一承载，签名/过期校验完全一致，不引入第二种会话体系。
2. **可见性=准入同源**：「我的模块」列表与网关准入使用同一套判断（模块存在 ∧ 启用 ∧ 用户权限交集≥1），看到的即可进，进不去的不出现。
3. **必要前置**：并入修复 `baseUrl/base_url` 字段错位（页面配置存不进则无跳转目标，硬前置）。

已知固限（登记不解决）：外部页面使用绝对路径资源（`/static/…`）或强制跳转自身域名时，iframe 内容会破图/逃逸。缓解：网关剥离下游内嵌限制响应头；**模块服务相对路径资源部署为使用前提**。

## 3. 后端改动

| # | 改动 | 位置 | 要点 |
|---|------|------|------|
| 1 | 认证过滤器双承载 | `JwtAuthenticationFilter`（web/003 层） | Bearer 缺失时读同名 `HttpOnly Cookie` 的 JWT，走完全相同校验（签名/过期）→ SecurityContext 照常填充；改这一处，网关与新接口零额外认证代码 |
| 2 | 登录下发/登出清除 Cookie | 登录接口（auth/002 范围）+ 登出接口（`POST /auth/logout`，auth/006 已存在） | 登录响应 `Set-Cookie: <jwt>; HttpOnly; SameSite=Lax; Path=/; Max-Age={expire-hours}`，响应体 token 不变；登出响应 `Set-Cookie: <jwt>=; Max-Age=0` 清除（无状态语义不变，仅多带清除头，前端已调用该接口） |
| 3 | `GET /modules/accessibles` | ModuleController/Service 新增 | 任何登录用户可调（SecurityConfig 该路径仅要求 authenticated，不绑 `module:view`）；返回精简记录列表（`id/name/code/baseUrl/description/status`），条件「启用 ∧ 用户权限交集≥1」，复用网关同款准入语义 |
| 4 | 字段错位修复（硬前置） | ModuleCreateDTO / ModuleUpdateDTO | `@JsonProperty("base_url")` → `@JsonProperty("baseUrl") + @JsonAlias("base_url")`：新代码统一 camelCase 对齐前端与 VO，旧名仍可读；前端 `types/module.ts` 的 `createTime` 改读 `createdAt`（VO 实际输出） |
| 5 | 网关响应头剥离 | GatewayServiceImpl | 转发时剥下游 `X-Frame-Options` 与 `Content-Security-Policy`（含 frame-ancestors 则浏览器拒绝内嵌）；请求侧剥 Authorization 照旧 |

范围牵连（Planner 规划时登记）：会触碰认证过滤器与 SecurityConfig（sprint-069 当期曾明示不动的文件），属本功能点正当需要，非夹带。

## 4. 前端改动

1. **「我的模块」菜单**（全员登录可见，按 web/003 动态菜单渲染机制注册，不绑具体权限码，内容自过滤）→ `MyModulesView`：调 `GET /modules/accessibles`，卡片网格（名称/编码/描述），空态提示「暂无可访问模块」，点卡片跳 `/workspace/module/:code`
2. **iframe 视图页** `ModuleIframeView`：`src="/api/v1/gateway/${code}/"`（相对路径同源：dev 走 Vite proxy、生产同域，不写死 host），高度撑满内容区，带加载态
3. **模块管理页操作列加「进入」**：`baseUrl 非空 && status=1` 才可点，否则 disabled 提示「未配置服务地址」，跳同一 iframe 路由
4. **字段对齐收尾**：`types/module.ts` 读 `createdAt`；`api/module.ts` 加 `getAccessibles()`
5. **前端测试（vitest）**：我的模块页（2 卡渲染/空态/点卡跳转）、iframe src 拼接、管理页进入按钮条件渲染

## 5. 错误处理与安全

- iframe 内 401/403 显示网关 JSON，**不做** HTML 错误页协商（控范围）
- `accessibles` 只出准入通过模块 → 403/1304/1305 场景被前置消灭
- 会话过期由前端 axios 401 拦截跳登录（现状）兜底
- 安全权衡登记：① 过滤器接受 Cookie 后所有 API 技术上可被 Cookie 调用 → 依赖 `SameSite=Lax` 挡跨站写（写操作仍走 Bearer）；② 剥离 X-Frame-Options = 授权用户可把外部系统嵌入任意页面，等同内容转发权（响应仍需 JWT+模块权限才能取得），登记接受

## 6. 测试策略（TDD 强制）

**RED（后端）**：
1. 网关双承载：有效 Cookie 放行 / 无效 Cookie 401（登录后带 Cookie 的集成用法）
2. `accessibles`：有权限模块出现、停用模块不出现、无权限模块不出现、未登录 401
3. DTO 双兼容：body 用 `base_url` 与 `baseUrl` 均创建成功
4. 下游 `X-Frame-Options` 被剥断言

**RED（前端）**：我的模块页三组、iframe src、进入按钮条件

**冒烟追加**（`web-038` 定向）：登录 → `/modules/accessibles` 断言权限过滤 + 前端构建产物含「我的模块」路由

**门禁**：`mvn -q verify` / `npm run lint && npm run test && npm run build` / `scripts/smoke.sh`（沿基线对照口径）

## 7. 范围边界（YAGNI 登记）

- 不做 modules/003 服务凭证/服务账号登录
- 不做 HTML 错误页协商、不做 iframe 父页状态探测
- 不做路径级准入（模块级，同 modules/002）
- 不做 SSO/单点登出传播（外部系统会话归其自身）
- 不改 GlobalExceptionHandler、既有 check API
- 外部服务相对路径资源部署为使用前提（绝对路径破图为已知限制）

## 8. Planner 移交事项

- 建议功能点 ID：**web/038**（启用预留），registry 备注同步扩展语义
- 跨前后端变更物预估 14-16 文件，超出 sprint-069 的 6 文件口径 → 文件预算按 `tdd-workflow/planner` 现行规则由 Planner 裁定
- 拆分预案：modules/003（服务凭证）与本单无关仍不注册；若 Planner 判定单工作单过大，可按「后端双承载+accessibles（先）」/「前端入口与 iframe（后）」两段拆分，但两段需各自可验收
