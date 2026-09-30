# Sprint 工作单：sprint-070

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-070 |
| 所属模块 | web |
| 功能点 ID | web/038a |
| 功能点名称 | 模块服务统一入口（a 段）——JWT Cookie 双承载与网关内嵌响应头剥离（后端支撑） |
| 状态 | DONE |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/002 | 登录签发 JWT（Cookie 的签发来源，登录响应挂 Set-Cookie 的落点） | ✅ |
| auth/003 | JWT 校验过滤器 JwtAuthenticationFilter（本单在其内新增 Cookie 承载分支） | ✅ |
| auth/006 | POST /api/v1/auth/logout 登出接口（无状态语义，本单在其响应挂清 Cookie 指令） | ✅ |
| modules/002 | 网关准入与转发 GatewayService（Cookie 认证验证端点与下游响应头剥离落点，sprint-069 评审通过） | ✅ |
| web/002 | 前端登录/登出既有调用（响应体契约不变、前端零改动的前提） | ✅ |

## 业务背景

用户需求（原文，2026-09-29 澄清收敛）：

1. 「我想要的效果是在进行模块配置后，能在当前系统中点击url然后就跳转到该模块的系统服务」
2. 「我的要求是当前系统做统一的jwt鉴权，管理员给指定的用户授权后它能够看到自己可以访问的模块并进行访问」
3. 「我的意思跳转的模块服务，还是在当前系统中进行」→ 形态选定「系统内嵌页面（iframe）」；入口选定「新菜单页 + 管理页可进入」。

**设计文档**（用户逐节确认后评审通过，commit 83b19fb）：`docs/superpowers/specs/2026-09-29-module-iframe-access-design.md`。本工作单为该设计拆分后的**第一个子功能点 web/038a**，后续 038b~038e 依序规划。

**本段解决的问题**：

- 浏览器同源 iframe 导航无法携带 `Authorization: Bearer` 头 → 需为同一 JWT 增加 `HttpOnly Cookie` 承载（签名/过期/校验零改动，仅多一个传输位置）；
- 外部系统响应常带 `X-Frame-Options` / `Content-Security-Policy: frame-ancestors` → 浏览器拒绝 iframe 渲染，网关须按代理语义剥离这两个内嵌限制头。

**架构对照（无冲突，两处口径登记）**：

- 架构 §安全「JWT(HS256)」与登录契约（payload{uid,username,exp}、2h 有效期）未限定 HTTP 承载头；本单仅新增 Set-Cookie 承载，签发/payload/过期/校验零改动，响应体契约不变，不涉 §3 表字段 → 无冲突。
- 网关响应头剥离 = sprint-069 已登记的代理语义（「转发成功的下游响应原样透传」）的**头维度明确化**：所剥两头是浏览器内嵌限制控制头、非业务契约；登记 session-state 挂起区。
- 安全权衡（设计文档第 5 节已登记）：① 过滤器接受 Cookie 后所有受保护端点技术上可被 Cookie 调用 → 依赖 `SameSite=Lax` 阻断跨站写，写操作仍走 Bearer；② 剥离 XFO = 授权用户可将外部系统内嵌，取得响应仍需 JWT+模块权限，等同内容转发权，登记接受。

**规模预估**：验收标准 4 条（未超 4）；预估文件变更 6 个（顶格未超 6）→ 本子功能点不拆分。

**总体需求拆分（一次只注册第一个子功能点 web/038a；整组登记于 registry web/038 行备注）**：

- **web/038a（本次 sprint-070，🔄）**：后端内嵌支撑——Cookie 双承载（登录下发/登出清除/过滤器读取）+ 网关剥离下游 XFO/CSP；
- web/038b（预留 ⬜）：`GET /modules/accessibles` 可访问模块列表（可见性=准入同源，前置 modules/002/auth/004）；
- web/038c（预留 ⬜）：`baseUrl/createdAt` 字段错位修复（硬前置：页面配置存不进 base_url，后端 DTO @JsonAlias + 前端读 createdAt）；
- web/038d（预留 ⬜）：「我的模块」菜单页（前置 038b，卡片网格+空态+跳转）；
- web/038e（预留 ⬜）：iframe 内嵌视图 + 管理页「进入」（前置 038a/038d，点击访问闭环）。

**关联登记**：Evaluator 积压六项不变（sprint-047/048/060/061/062/063），见 session-state 挂起区。

## 需求描述

1. **登录下发 Cookie**：POST `/api/v1/auth/login` 成功时响应追加 `Set-Cookie: <JWT>; Max-Age={jwt.expire-hours×3600}; HttpOnly; SameSite=Lax; Path=/`；响应体 JSON 契约不变（token 照常返回，前端 axios 零改动）。
2. **登出清除 Cookie**：POST `/api/v1/auth/logout` 响应追加同名 Cookie 清除指令（`Max-Age=0`）；服务端无状态语义不变（不引入 token 黑名单）。
3. **过滤器双承载**：JwtAuthenticationFilter 在请求缺失 `Authorization` 头时，读取同名 Cookie 的值按**完全相同**的校验逻辑（签名/过期）填充 SecurityContext；Bearer 存在时行为与现状逐字节一致（承载优先级：Bearer > Cookie）。
4. **网关内嵌支撑**：GatewayServiceImpl 转发下游响应时剥离 `X-Frame-Options` 与 `Content-Security-Policy` 两个响应头；其余行为（状态码/响应体原样透传、剥离请求侧 Authorization、模块级准入 1304/1305/403+1403/401+1401）与 sprint-069 逐条一致不回归。

> 范围边界（防蔓延）：**不做** accessibles 接口（归 038b）、字段错位修复（归 038c）、任何前端改动（归 038d/038e）；**不改** SecurityConfig（sprint-069 实证 `anyRequest().authenticated()` 天然覆盖，仅动过滤器）与 GlobalExceptionHandler；**不改** JWT 签发/payload/过期语义与登录响应体契约；**不引入**第二套会话体系（Cookie 仅是同一 JWT 的另一承载）。实现中预估变更文件超过 6 个，Generator 必须停止实现并回报 Planner 重新拆分。

## 验收标准（TDD 驱动）

- [x] AC1 — 登录响应下发 HttpOnly/SameSite=Lax/Max-Age 匹配配置的 Cookie，登出响应清除之
- [x] AC2 — 无 Authorization 但携带有效 Cookie 的请求通过认证并执行网关转发
- [x] AC3 — 无效/过期 Cookie 且无 Bearer 时返回 401 code=1401 且不发起下游调用
- [x] AC4 — 下游 XFO/CSP 不透传且响应 XFO 不为 DENY（口径修订见下），后端门禁零新增失败、冒烟 web-038a 通过

### AC1 — 登录响应下发 HttpOnly/SameSite=Lax/Max-Age 匹配配置的 Cookie，登出响应清除之
> Given 合法 username/password。When `POST /api/v1/auth/login`。Then 响应 `Set-Cookie` 含有效 JWT 值、`HttpOnly`、`SameSite=Lax`、`Path=/`、`Max-Age={测试配置 jwt.expire-hours×3600}`，响应体 token 与现状契约一致。When 携带该 Cookie `POST /api/v1/auth/logout`。Then 响应含同名 Cookie 的 `Max-Age=0` 清除指令，响应体仍为 `Result{code=0}`。

**用例**：`AC1 ← 用例 AuthControllerTest#loginSetsHttpOnlyCookie / AuthControllerTest#logoutClearsCookie`（Planner 登记双方法，同一 AC 的下发/清除两端）

### AC2 — 无 Authorization 但携带有效 Cookie 的请求通过认证并执行网关转发
> Given 模块 `ORDER`（启用、`base_url` 已配置）且当前用户有其下有效权限，持有有效 JWT 但**不带** Authorization 头、仅带 `Cookie: <name>=<JWT>`。When `GET /api/v1/gateway/ORDER/api/items`。Then 网关按准入放行，下游被调用恰好一次（`verify(restTemplate, times(1))`），转发请求不含 Authorization 头，响应状态码与体按代理语义透传。

**用例**：`AC2 ← 用例 GatewayControllerTest#validCookieAuthenticatesGatewayRequest`

### AC3 — 无效/过期 Cookie 且无 Bearer 时返回 401 code=1401 且不发起下游调用
> 携带伪造签名或已过期的 Cookie（且无 Authorization）请求网关 → HTTP 401、响应体 `Result{code=1401}`，且 `verify(restTemplate, never())`。**如实登记**：若实现前该请求即因无凭证返回 401，须核验是 Cookie 分支解析后拒绝而非仅凭无 Authorization 命中（以有效 Cookie 放行的 AC2 为对照；RED 由 AC2/AC3 的相对关系与守护断言共同承担，不得伪造）。

**用例**：`AC3 ← 用例 GatewayControllerTest#invalidCookieRejectedWith401AndNoDownstreamCall`

### AC4 — 下游内嵌限制头不透传且响应可被同源 iframe 内嵌，且门禁与冒烟通过
> ① Given mock 下游返回 200 + `X-Frame-Options: DENY` + `Content-Security-Policy: frame-ancestors 'none'` + 业务体。When 经网关透传。Then 下游两头不进网关响应、响应 `X-Frame-Options` **不为 DENY**（框架默认 DENY 须由网关侧预置 SAMEORIGIN 覆盖，同源 iframe 可内嵌、外源仍拒；字节码实证见「AC4 口径修订」）、状态码与响应体原样，且 sprint-069 的 Authorization 请求头剥离行为不回归。② `mvn -q verify` 失败集与实现前基线逐条一致零新增（2026-09-29 口径：136 用例 4F+2E）；③ 本功能点零前端改动，前端门禁不适用（基线对照实测留痕）；④ `bash scripts/smoke.sh` 新增 `web-038a` 用例单跑通过、整体失败与基线逐条一致零新增。

**用例**：`AC4 ← 用例 GatewayControllerTest#stripsFrameBlockingHeadersFromDownstream + mvn -q verify 基线对照 + bash scripts/smoke.sh#web-038a`

**AC4 口径修订（Generator RED 实测登记，Planner 原文「网关透传响应不含 X-Frame-Options 与 Content-Security-Policy」，Evaluator 可复核推翻）**：
- RED 实测响应含 `X-Frame-Options`，字节码溯源：Spring Security 6.5 `XFrameOptionsHeaderWriter` 无参构造默认 **DENY**（`HeadersConfigurer$FrameOptionsConfig` 构造调 `enable()` → 无参 writer），SecurityConfig 未定制 headers → **全站响应写 DENY**（已存在同名头则跳过）。DENY 会阻断**一切** iframe 内嵌（含同源），与设计目标直接冲突——严格「不含」需改 SecurityConfig（本单范围明令不改）。
- 修订口径：**下游的 XFO/CSP 不透传 + 响应 XFO 不为 DENY**——实现=GatewayServiceImpl 预置 `X-Frame-Options: SAMEORIGIN`（利用框架「已存在头则跳过」行为覆盖默认 DENY；同源内嵌放行、外源仍拒，安全语义优于剥离）。CSP 框架默认不写，断言不存在。设计目标（iframe 可渲染）完整达成，SecurityConfig 零改动、文件预算不变。
- **GREEN 二次修订（2026-09-29，用户裁定，字节码+实测实证）**：预置 SAMEORIGIN 方案被证伪——`XFrameOptionsHeaderWriter` 非 ALLOW_FROM 分支 `setHeader` **无条件覆盖**（无 containsHeader 守卫），响应提交时 DENY 覆盖控制器预置，实测 24/25 仅该项仍红。裁定：**修订「不改 SecurityConfig」约束，允许 +1 行 `.headers(h -> h.frameOptions(f -> f.sameOrigin()))`（标准做法，同源放行外源拒绝，生产正确）；文件预算 6→7**。GatewayServiceImpl 预置保留（网关路径显式声明，不依赖安全配置）。CSP 默认不写已由 `ContentSecurityPolicyConfig` 构造字节码证实（writer 仅显式配置时初始化）。

## 测试清单

> 先于实现写出，规则遵循 `.harness/rules/tdd-workflow.md`；Generator 运行确认 RED 后填入实际输出（若有实现前即绿的守护断言须如实注明，不得伪造 RED）。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | AuthControllerTest#loginSetsHttpOnlyCookie | AC1-下发 | ✅ RED（Set-Cookie 缺失）→ GREEN 通过 |
| 2 | AuthControllerTest#logoutClearsCookie | AC1-清除 | ✅ RED（Set-Cookie 缺失）→ GREEN 通过 |
| 3 | GatewayControllerTest#validCookieAuthenticatesGatewayRequest | AC2 | ✅ RED（401 vs 200）→ GREEN 通过 |
| 4 | GatewayControllerTest#invalidCookieRejectedWith401AndNoDownstreamCall | AC3 | ✅ 实现前即绿（守护断言如实登记，非 RED；与 AC2 构成对照），GREEN 复跑通过 |
| 5 | GatewayControllerTest#stripsFrameBlockingHeadersFromDownstream | AC4-① | ✅ RED（XFO=DENY）→ 预置方案证伪 → SecurityConfig sameOrigin 后 GREEN 通过 |
| 6 | 冒烟 web-038a 定向测试（追加于 scripts/smoke.sh，定向跑本单新用例） | AC4-④ | ✅ 已追加，52 用例中单跑 ✅ 通过 |
| 7 | 后端全量门禁基线对照（实现前基线留痕：136 用例 4F+2E 清单，2026-09-29） | AC4-②③ | ✅ 141 用例 4F+2E 逐条一致零新增（136+5=141） |

## RED 证据

> Generator 于实现前执行测试清单 #1~#5 并粘贴关键失败输出（实时留痕，2026-09-29）。

```text
[RED] mvn -q -f backend/pom.xml test -Dtest='AuthControllerTest,GatewayControllerTest'（实现前执行）
Tests run: 25, Failures: 4, Errors: 0, Skipped: 0
[RED] AuthControllerTest.loginSetsHttpOnlyCookie:534 Response header 'Set-Cookie' — 期望含 AUTH_TOKEN=/HttpOnly/SameSite=Lax/Max-Age=7200/Path=/，实际缺失
[RED] AuthControllerTest.logoutClearsCookie:573 Response header 'Set-Cookie' — 期望 Max-Age=0 清除指令，实际缺失
[RED] GatewayControllerTest.stripsFrameBlockingHeadersFromDownstream:360 Response header 'X-Frame-Options' — Expected: not "DENY" but: was "DENY"（框架默认 DENY 实证，见 AC4 口径修订）
[RED] GatewayControllerTest.validCookieAuthenticatesGatewayRequest:302 Status expected:<200> but was:<401>（Cookie 尚无承载能力）
AC3 invalidCookieRejectedWith401AndNoDownstreamCall 实现前即绿：无凭证本就命中 401+1401（守护断言如实登记，与 AC2 对照构成 RED/GREEN 分野）
```

## GREEN 证据

> 实现后复跑（实时留痕，2026-09-29）。REFACTOR 复查：构造器注入 final 字段、分层 controller→service 接口→impl、Javadoc 完整、无字段注入，与既有风格一致。

```text
[GREEN] mvn -f backend/pom.xml test -Dtest='AuthControllerTest,GatewayControllerTest'
Tests run: 16 -- AuthControllerTest（含 loginSetsHttpOnlyCookie / logoutClearsCookie）
Tests run: 9  -- GatewayControllerTest（含 validCookie / invalidCookie / stripsFrameBlockingHeaders）
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
中间态：SecurityConfig sameOrigin 之前 24/25（预置 SAMEORIGIN 被框架无条件覆盖实证，见 AC4 二次修订）
```

## 门禁与冒烟记录

> Generator 亲测填写（后端 mvn -q verify / 前端门禁基线对照 / scripts/smoke.sh），Evaluator 不采信自述须亲跑。

**后端 `mvn -q verify`（2026-09-29）**：`Tests run: 141, Failures: 4, Errors: 2`（基线 136+5 本单新用例=141；失败集与基线逐条一致零新增）：
- F: DataSourceConfigBindingTest#test_flyway开关与迁移目录绑定、#test_环境变量覆盖数据源默认配置
- F: RoleControllerTest#assignPermissionsInvalidPermissionReturns400、SeedDataIntegrationTest#test_核心实体数据存在且字段正确
- E: TestLayersSpec、TestUtilsSpec

**前端门禁（零前端改动，基线对照实测）**：`npm run lint` EXIT=124（既有挂起）；`npm run test` EXIT=1，Test Files 6 failed | 28 passed，Tests **20 failed | 279 passed**（与基线一致）；`npm run build` EXIT=0 ✓。

**`bash scripts/smoke.sh`（2026-09-29）**：**52 用例**（51+新增 web-038a），9 失败与基线逐条一致零新增：
- model-008、model-010、roles-001、roles-002、web-013、web-020、web-021、web-022、web-026（全部为基线失败）
- 新增 `web-038a Cookie双承载与内嵌头定向测试` → ✅ 通过（定向跑 AuthControllerTest+GatewayControllerTest 25 用例）

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 **6 个（顶格）**，GREEN 期经用户裁定修订为 **7 个**（见 AC4 口径修订）：

1. `backend/src/main/java/com/authcore/config/security/JwtAuthenticationFilter.java` — 缺 Authorization 时读同名 Cookie 走同一校验（AC2/AC3）
2. `backend/src/main/java/com/authcore/controller/AuthController.java` — 登录响应挂 Set-Cookie、登出响应挂清除指令（AC1）
3. `backend/src/main/java/com/authcore/service/impl/GatewayServiceImpl.java` — 转发剥 Cookie 头防凭据外泄 + 预置 XFO SAMEORIGIN（AC4-①）
4. `backend/src/main/java/com/authcore/config/security/SecurityConfig.java` — **（用户裁定追加，2026-09-29）** frameOptions SAMEORIGIN 1 行，覆盖默认 DENY
5. `backend/src/test/java/com/authcore/controller/AuthControllerTest.java` — 追加 AC1 两个方法（测试先行）
6. `backend/src/test/java/com/authcore/controller/GatewayControllerTest.java` — 追加 AC2/AC3/AC4 三个方法（测试先行，沿 sprint-069 既有基建）
7. `scripts/smoke.sh` — 追加 `web-038a` 用例（不删除、不改动既有用例，先例 web/037、modules/002）

**超限熔断**：实现中若实际变更文件超过 7 个（2026-09-29 用户裁定 6→7），Generator 必须停止实现并回报重新拆分，不得自行扩范围。accessibles（038b）、字段修复（038c）、一切前端改动（038d/038e）一律不做。

## 交付物（预估 6 文件 → 用户裁定 7 文件）

1. `backend/src/main/java/com/authcore/config/security/JwtAuthenticationFilter.java` — Cookie 承载分支
2. `backend/src/main/java/com/authcore/controller/AuthController.java` — 登录/登出 Cookie 生命周期
3. `backend/src/main/java/com/authcore/service/impl/GatewayServiceImpl.java` — 转发剥 Cookie + XFO 预置
4. `backend/src/main/java/com/authcore/config/security/SecurityConfig.java` — frameOptions SAMEORIGIN（用户裁定追加）
5. `backend/src/test/java/com/authcore/controller/AuthControllerTest.java` — AC1 用例（测试先行）
6. `backend/src/test/java/com/authcore/controller/GatewayControllerTest.java` — AC2/AC3/AC4 用例（测试先行）
7. `scripts/smoke.sh` — web-038a 冒烟用例

## 变更清单

### 新增
- （无）

### 修改
1. `backend/src/main/java/com/authcore/config/security/JwtAuthenticationFilter.java` — Cookie 承载分支 + `AUTH_COOKIE_NAME` 常量
2. `backend/src/main/java/com/authcore/controller/AuthController.java` — 登录 Set-Cookie / 登出清除 + JwtProperties 注入
3. `backend/src/main/java/com/authcore/service/impl/GatewayServiceImpl.java` — STRIPPED_HEADERS 增 cookie + 预置 XFO SAMEORIGIN
4. `backend/src/main/java/com/authcore/config/security/SecurityConfig.java` — frameOptions sameOrigin 1 行（用户裁定追加）
5. `backend/src/test/java/com/authcore/controller/AuthControllerTest.java` — AC1 两用例 + logout 反射签名适配
6. `backend/src/test/java/com/authcore/controller/GatewayControllerTest.java` — AC2/AC3/AC4 三用例
7. `scripts/smoke.sh` — web-038a 冒烟用例

合计 7 文件 = 用户裁定预算顶格，未超熔断。

### 删除
- （无）

## 规范检查清单

- [x] `mvn -q verify` 后端门禁零新增失败（141 用例 4F+2E 与基线逐条一致）
- [x] 前端门禁不适用（零前端改动；lint 124/test 20 failed/build ✓ 基线对照留痕）
- [x] `bash scripts/smoke.sh` web-038a ✅；52 用例 9 失败与基线逐条一致
- [x] 符合 Java 21 / Spring Boot 3 / Spring Security 6 编码规范与既有分层约定（构造器注入 final 字段，无字段注入）
- [x] 符合 TDD 工作流（RED 25 用例 4F 实时留痕 → GREEN 25/25 → REFACTOR 复查一致）
- [x] 变更文件 7 个 = 用户裁定预算顶格，未超熔断

## 评审记录

- 2026-09-29: Evaluator 首轮评审 **✅ 通过（均分 9.0/10）**（sprint-070）。五维：功能正确性 9 / 代码质量 9 / 规范遵守 9 / TDD 执行度 9 / 安全性 9。4 条 AC 全满足（亲跑：mvn 141 用例 4F+2E 与基线逐条一致零新增；前端 lint124/test 20 failed/build ✓ 基线对照；冒烟 52 用例 9 失败为基线子集、web-038a ✅、smoke.sh 增量 +2 行核证）。六项一票否决均未命中（否决项 6 按 2026-09-28 用户「基线对照推进」裁决与 sprint-069 先例豁免）。AC4 两阶段口径修订（预置 SAMEORIGIN 被 `XFrameOptionsHeaderWriter` 无条件覆盖字节码证伪 → 用户裁定 SecurityConfig +1 行 sameOrigin、预算 6→7）经复核认可。改进建议（不计分）：① `AUTH_COOKIE_NAME` 可上移至独立常量类避免 controller→filter 反向引用；② 过期 Cookie 分支建议在 038b/038e 期间补单测；③ `secure` 属性随生产 HTTPS 部署切换登记至设计文档第 7 节部署前提。
