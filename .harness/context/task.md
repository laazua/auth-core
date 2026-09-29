# Sprint 工作单：sprint-069

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-069 |
| 所属模块 | modules |
| 功能点 ID | modules/002 |
| 功能点名称 | 模块（外部服务）统一访问入口——经认证与模块级权限准入后按 base_url 转发 |
| 状态 | DONE |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/003 | JWT 校验过滤器 + SecurityContext 注入（网关 401/1401 门槛与当前登录用户身份来源） | ✅ |
| auth/004 | GET /api/v1/auth/me 权限聚合（AuthService.getCurrentUserInfo 提供用户权限码集合，准入判定来源） | ✅ |
| modules/001 | 模块 CRUD API（模块存在性/状态/base_url 的数据来源与 ModuleService 接口） | ✅ |
| model/004 | sys_permission 表 module_id 非空（架构 §6.1 权限必须归属模块，模块级准入的判定依据） | ✅ |
| model/010 | ModuleService 接口化（model/012「Controller 依赖接口」约定） | ✅ |

## 业务背景

用户需求（原文）：「关于模块管理，我想要的是这里的模块不是指当前系统的用户，角色，权限等，而是指别的运行服务，该服务需要通过在当前系统进行登录认证后，才能通过当前系统进行访问」。

Planner 只读调研（explore 2026-09-29 实证）关键事实：

1. **base_url 是死字段**：`V3__create_sys_module.sql:6` 已有 `base_url VARCHAR(255) NULL`（注释语义即模块服务地址），但全仓无任何逻辑消费它——仅 CRUD 存取与前端列展示，外部服务在本系统中只是死数据；
2. **只有一种凭证**：系统仅有用户 JWT（auth/002 login / auth/003 过滤器），无服务级凭证、client_id/secret 概念；
3. **check API 不是访问通道**：`POST /api/v1/auth/check`（auth/005）为「userId + permissionCode」校验，要求用户 JWT，不涉及模块维度准入，更不承载对服务的访问；
4. **SecurityConfig 天然覆盖新路径**：`SecurityConfig.java:90-91` 仅 permitAll actuator/login/logout，其余 `anyRequest().authenticated()`——新增网关路径自动受保护，无需修改 SecurityConfig。

**语义澄清裁量**（澄清问答被用户中断并指示「继续」，Planner 按架构一致性裁量，已登记 session-state 挂起区，Evaluator/用户可复核推翻）：

- **「登录认证」= 有效 JWT 准入**：访问方（终端用户或以服务账号身份的调用方）须先持有 auth-core 签发的有效 JWT（复用 auth/002/003），不引入服务级新凭证类型——避免与架构 §3「字段名与约束为契约，不得增删改名」冲突；「服务级凭证与服务登录」另立 modules/003 预留，启动前须先走 §3 仲裁（或采用 sys_user 服务账号零改表方案）。
- **「通过当前系统进行访问」= 网关代理**：auth-core 作为统一入口，对目标模块（外部服务）做认证 + 模块级准入裁决后，按模块 `base_url` 反向转发请求；base_url 由死字段变为真实路由依据。

**架构对照（无冲突，一处口径登记）**：

- 不改表结构（§3 ✓）；不引入表外技术栈——RestTemplate 属 Spring Boot Web 框架内置（§1 ✓）；接口在 `/api/v1/{resource}` 前缀内（§4 ✓）；准入语义直接由 §6.1（权限必须归属模块）与 §6.5（停用模块下权限视为无效）推导（✓）。
- **口径登记（§4 兼容性）**：§4「响应体统一 Result 结构」未覆盖代理场景。裁量：网关**自身产生的准入错误响应**（401/403/400）仍为 `Result{code,message,data}`；转发**成功的下游响应**按代理语义原样透传状态码与响应体（下游为外部服务自有契约）。已登记 session-state 挂起区，如需强制包裹 Result 由用户裁决。
- 错误码映射复用现状、零改动：403 抛 `AccessDeniedException` → `GlobalExceptionHandler.java:53-57` → 403+1403；新码 1304/1305 不在 409 清单 → `handleBusiness` 默认分支 400（`GlobalExceptionHandler.java:41-43`）。**注意**：抛 `BusinessException(1403)` 会被 `GlobalExceptionHandler.java:33-34` 翻成 401，必须用 `AccessDeniedException`。

**规模预估**：验收标准 4 条（未超 4）；预估文件变更 6 个（顶格未超 6）→ 本功能点不拆分。

**总体需求拆分（一次只注册第一个子功能点 modules/002）**：

- **modules/002（本次 sprint-069，🔄）**：后端统一访问入口——认证 + 模块级准入 + 按 base_url 转发；
- modules/003（预留，⬜ 未注册）：服务级接入凭证与「服务登录」——启动前须先裁决架构 §3 字段契约（或 sys_user 服务账号零改表方案）；
- web/038（预留，⬜ 未注册）：模块管理页「外部服务」语义适配（模块即服务展示、base_url/接入状态、经网关访问说明）。

**关联登记**：Evaluator 积压六项不变（sprint-047/048/060/061/062/063），见 session-state 挂起区。

## 需求描述

1. **网关端点**：`ALL /api/v1/gateway/{moduleCode}/**`，承接对模块（外部服务）的访问请求；moduleCode 后的剩余路径为转发目标子路径。
2. **认证门槛**：未携带或携带无效/过期 JWT → HTTP 401、`Result.code=1401`（复用 JwtAuthenticationEntryPoint 现有行为），且不发起任何下游调用。
3. **准入裁决**（当前用户身份从 SecurityContext 取，不信任请求体）：
   - 模块码不存在 → HTTP 400、`code=1304`；
   - 模块存在但 `base_url` 为空 → HTTP 400、`code=1305`；
   - 当前用户对模块无任何**有效**权限（模块启用：其下权限中至少一个属于当前用户权限码集合；模块停用时其下权限按 §6.5 视为无效）→ HTTP 403、`code=1403`（抛 AccessDeniedException 翻译）；
   - 任一拒绝情形均不发起下游调用。
4. **转发**：通过准入后，以 RestTemplate 将**请求方法、查询串、请求体**转发至 `base_url + 剩余路径`（base_url 尾部 `/` 归一去重）；**剥离 Authorization 头**（JWT 不外泄给外部服务）；下游响应的**状态码与响应体原样透传**。
5. **不回归**：后端既有测试零新增失败；前端零改动；冒烟追加 `modules-002` 用例。

> 范围边界（防蔓延）：准入粒度=模块级（模块下任一有效权限即放行该模块全部路径），**不做**路径级/接口级细粒度鉴权；**不做**服务级凭证与服务登录（归 modules/003）；**不做**前端改造（归 web/038）；**不做**下游超时/重试/熔断/流式响应；**不修改**既有模块 CRUD、auth/005 check API 与 SecurityConfig（默认已保护）；**不修改** GlobalExceptionHandler（1304/1305 走默认 400 分支）。实现中预估变更文件超过 6 个，Generator 必须停止实现并回报 Planner 重新拆分。

## 验收标准（TDD 驱动）

- [x] AC1 — 未认证访问网关返回 401 code=1401 且不发起下游调用
- [x] AC2 — 通过准入的请求按模块 base_url 转发且响应原样透传（Authorization 不外泄）
- [x] AC3 — 准入失败的任一情形均拒绝且不发起下游调用
- [x] AC4 — 后端门禁零新增失败且冒烟 modules-002 通过

### AC1 — 未认证访问网关返回 401 code=1401 且不发起下游调用
> 不携带 Authorization（以及携带伪造 Bearer token）请求 `GET /api/v1/gateway/ORDER/detail` → HTTP 401、响应体 `Result{code=1401}`；且下游 RestTemplate 调用零次发生（mock `verify(restTemplate, never())`）。**如实登记**：实现前 Security 默认行为对该路径即返回 401，本条为安全门槛守护断言（写入时可能即绿），RED 证据由 AC2/AC3/冒烟承担；若实现中被 permitAll 放行则本条失败。

**用例**：`AC1 ← 用例 GatewayControllerTest#accessWithoutTokenReturns401`

### AC2 — 通过准入的请求按模块 base_url 转发且响应原样透传（Authorization 不外泄）
> Given：模块 `ORDER`（status=1，`base_url=http://service-a:8081/`），其下权限 `order:view`，当前用户拥有该权限。When 携带有效 JWT 请求 `POST /api/v1/gateway/ORDER/api/items?page=2`、JSON 体 `{"x":1}`。Then：① 下游收到 `POST http://service-a:8081/api/items?page=2`（尾斜杠归一，剩余路径与查询串原样拼接），请求头保留 Content-Type 且**不含 Authorization**；② 请求体原样 `{"x":1}`；③ 下游返回 201 + `{"id":9}` → 网关原样返回 201 + `{"id":9}`（代理语义，见业务背景口径登记）。

**用例**：`AC2 ← 用例 GatewayControllerTest#forwardsToModuleServiceWhenAuthorized`

### AC3 — 准入失败的任一情形均拒绝且不发起下游调用
> 携带有效 JWT 请求网关，分别构造四种情形，逐条断言状态码与业务码，且每种情形均 `verify(restTemplate, never())`：
> ① 模块启用、用户对该模块无任何有效权限 → 403 + `code=1403`；
> ② 模块 status=0 停用（其下权限按 §6.5 视为无效，即使用户拥有该权限码）→ 403 + `code=1403`；
> ③ 模块码不存在 → 400 + `code=1304`；
> ④ 模块 `base_url` 为空 → 400 + `code=1305`。

**用例**：`AC3 ← 用例 GatewayControllerTest#rejectsWhenUserHasNoModulePermission / rejectsWhenModuleDisabled / rejectsWhenModuleNotFound / rejectsWhenBaseUrlMissing`（Planner 原登记单一方法名，Generator 按 coding-standards「一用例一行为」拆为 4 个方法，AC 语义不变）

### AC4 — 后端门禁零新增失败且冒烟 modules-002 通过
> `mvn -q verify` 失败集与实现前基线逐条一致零新增（基线 130 用例 4 Failures + 2 Errors：TestLayersSpec/TestUtilsSpec 无 Docker、SeedDataIntegrationTest、DataSourceConfigBindingTest ×2、RoleControllerTest 409，2026-09-29 口径）；本功能点零前端改动，前端门禁不适用（如实注明）；`bash scripts/smoke.sh` 新增 `modules-002` 用例（定向跑 GatewayControllerTest）且单跑通过、整体失败与基线逐条一致零新增。

**用例**：`AC4 ← 用例 mvn -q verify 基线对照 + bash scripts/smoke.sh#modules-002`

## 测试清单

> 先于实现写出，规则遵循 `.harness/rules/tdd-workflow.md`；Generator 运行确认 RED 后填入实际输出（AC1 若实现前即绿须如实注明，不得伪造 RED）。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | GatewayControllerTest#accessWithoutTokenReturns401 | AC1 | ✅ 实现前即绿（Security 默认保护新路径，守护断言如实登记，非 RED；实现中若被放行则失败） |
| 2 | GatewayControllerTest#forwardsToModuleServiceWhenAuthorized | AC2 | ✅ RED（实时留痕）→ GREEN（断言精修：Content-Type 按兼容性断言，原样保留含 charset） |
| 3 | GatewayControllerTest#rejectsWhenUserHasNoModulePermission | AC3-① | ✅ RED（实时留痕）→ GREEN |
| 4 | GatewayControllerTest#rejectsWhenModuleDisabled | AC3-② | ✅ RED（实时留痕）→ GREEN |
| 5 | GatewayControllerTest#rejectsWhenModuleNotFound | AC3-③ | ✅ RED（实时留痕）→ GREEN |
| 6 | GatewayControllerTest#rejectsWhenBaseUrlMissing | AC3-④ | ✅ RED（实时留痕）→ GREEN（测试数据精修：updateById 忽略 null 字段，改 LambdaUpdateWrapper.set 显式置空 base_url） |
| 7 | 冒烟 modules-002 网关定向测试（mvn -Dtest=GatewayControllerTest） | AC4 | ✅ 已追加并随全量冒烟通过（smoke 51 用例，modules-002 ✅） |
| 8 | 后端全量门禁基线对照（实现前 130 用例 4F+2E 清单留痕） | AC4 | ✅ 实测 136 用例（130+6 新增）4 Failures + 2 Errors 与基线逐条一致零新增，GatewayControllerTest 6/6 全绿 |

## RED 证据

> Generator 于实现前执行测试清单 #1~#6 并粘贴关键失败输出（实时留痕）。

```text
[RED] mvn -q -f backend/pom.xml test -Dtest=GatewayControllerTest（实现前执行，2026-09-29）
Tests run: 6, Failures: 5, Errors: 0, Skipped: 0
[RED] GatewayControllerTest.forwardsToModuleServiceWhenAuthorized:163 Status expected:<201> but was:<500>
[RED] GatewayControllerTest.rejectsWhenUserHasNoModulePermission:199 Status expected:<403> but was:<500>
[RED] GatewayControllerTest.rejectsWhenModuleDisabled:223 Status expected:<403> but was:<500>
[RED] GatewayControllerTest.rejectsWhenModuleNotFound:241 Status expected:<400> but was:<500>
[RED] GatewayControllerTest.rejectsWhenBaseUrlMissing:265 Status expected:<400> but was:<500>
AC1 accessWithoutTokenReturns401 实现前即绿：Security 默认对未注册路径的未认证请求返回 401+1401（守护断言，如实登记不伪造 RED）
```

## 门禁与冒烟记录

- 后端 `mvn -q verify`：**Tests run: 136, Failures: 4, Errors: 2** —— 136 = 基线 130 + 新增 6；失败/错误与基线逐条一致（DataSourceConfigBindingTest ×2、RoleControllerTest 409、SeedDataIntegrationTest、TestLayersSpec/TestUtilsSpec Docker 缺失），**零新增失败**；GatewayControllerTest 6/6 全绿 ✅（2026-09-29 实测）
- 前端门禁：**不适用（本功能点零前端改动）**，按基线对照口径实测留痕：`npm run test` 20 failed / 279 passed 与基线逐条一致零新增；`npm run build` ✓ built in 17.25s；`npm run lint` 全量 timeout 300s exit 124 卡死为预存（沿基线对照口径，第 7 次复现），零前端改动故无分片对象
- `bash scripts/smoke.sh`：**51 用例（50 基线 + 新增 modules-002），失败 9 条与基线 9 条逐条一致零新增**（model-008/010、roles-001/002、web-013、web-020/021/022、web-026，均为既有预存红项）；**modules-002 单跑 ✅ 通过** ✅（2026-09-29 实测）
- AC1 如实说明：实现前即绿（Security 默认对未注册路径的未认证请求返回 401+1401），属安全门槛守护断言，RED 证据由 AC2/AC3 五条失败承担（见上「RED 证据」）

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 **6 个（顶格）**：

1. `backend/src/main/java/com/authcore/controller/GatewayController.java` — 网关端点，解析 moduleCode 与剩余路径，调用 GatewayService（AC1-AC3）
2. `backend/src/main/java/com/authcore/service/GatewayService.java` — 服务接口（model/012 Controller 依赖接口约定）
3. `backend/src/main/java/com/authcore/service/impl/GatewayServiceImpl.java` — 准入裁决（模块存在/启用/base_url/权限交集）+ RestTemplate exchange 转发（AC2/AC3）
4. `backend/src/main/java/com/authcore/config/web/RestTemplateConfig.java` — RestTemplate @Bean（转发实例注入，测试可 @MockBean/MockRestServiceServer 绑定）
5. `backend/src/test/java/com/authcore/controller/GatewayControllerTest.java` — AC1/AC2/AC3 用例（测试先行，`@SpringBootTest + @AutoConfigureMockMvc + @ActiveProfiles("test")` 沿 AuthControllerTest 先例；测试数据沿用 IT_PREFIX 隔离先例）
6. `scripts/smoke.sh` — 追加 `modules-002` 用例（不删除、不改动既有用例，先例 web/037）

**超限熔断**：实现中若实际变更文件超过 6 个，Generator 必须停止实现并回报 Planner 重新拆分，不得自行扩范围。服务凭证（modules/003）、前端页面（web/038）、路径级鉴权、下游可靠性一律不做。

**注册拆分（见业务背景）**：总体需求拆为 modules/002（本工作单）/ modules/003（服务凭证，预留）/ web/038（前端适配，预留），一次只注册第一个。

## 交付物（预估 6 文件）

1. `backend/src/main/java/com/authcore/controller/GatewayController.java` — 网关端点
2. `backend/src/main/java/com/authcore/service/GatewayService.java` — 服务接口
3. `backend/src/main/java/com/authcore/service/impl/GatewayServiceImpl.java` — 准入裁决 + 转发
4. `backend/src/main/java/com/authcore/config/web/RestTemplateConfig.java` — RestTemplate Bean
5. `backend/src/test/java/com/authcore/controller/GatewayControllerTest.java` — AC1/AC2/AC3 用例（测试先行）
6. `scripts/smoke.sh` — modules-002 产物/定向冒烟用例

## 变更清单

### 新增
- `backend/src/main/java/com/authcore/controller/GatewayController.java` — `ALL /api/v1/gateway/{moduleCode}/**` 网关端点（AC1-AC3）
- `backend/src/main/java/com/authcore/service/GatewayService.java` — 准入+转发服务接口（AC2/AC3）
- `backend/src/main/java/com/authcore/service/impl/GatewayServiceImpl.java` — 模块级准入裁决与 RestTemplate 转发、Authorization 剥离、响应透传（AC2/AC3）
- `backend/src/main/java/com/authcore/config/web/RestTemplateConfig.java` — RestTemplate Bean（AC2 测试可绑定）
- `backend/src/test/java/com/authcore/controller/GatewayControllerTest.java` — AC1/AC2/AC3 用例（测试先行，含 never() 不转发断言）

### 修改
- `scripts/smoke.sh`:403-405 — 追加 `modules-002 模块统一访问入口网关定向测试` 冒烟用例（`mvn -Dtest=GatewayControllerTest`，不删除、不改动既有用例）

### 删除
- （无）

## 规范检查清单

- [x] `mvn -q verify` 后端门禁（136 用例 4F+2E 与基线逐条一致零新增）
- [x] 前端门禁不适用（本功能点零前端改动；test/build/lint 基线对照实测留痕于「门禁与冒烟记录」）
- [x] `bash scripts/smoke.sh` 新增 modules-002 单跑通过且整体 9 失败与基线逐条一致（51 用例）
- [x] 符合 Java 21 / Spring Boot 3 / Spring Security 6 编码规范与既有分层约定（controller → service 接口 → impl，构造器注入 final 字段，无字段注入）
- [x] 符合 TDD 工作流（测试先行、RED 证据实时留痕 5 failed、GREEN 6/6、REFACTOR 复查全绿；AC1 非 RED 如实登记）
- [x] 变更文件不超过 6 个（实际 6：GatewayController/GatewayService/GatewayServiceImpl/RestTemplateConfig/GatewayControllerTest/smoke.sh，顶格未超熔断）

## 评审记录

- 2026-09-29: Evaluator — **通过**（第 1 轮，平均分 9.0/10：功能 9 / 质量 9 / 规范 9 / TDD 9 / 安全 9）。验收标准 4/4 满足（亲测：mvn 136 用例 4F+2E 与基线逐条一致零新增、前端 test 20 failed/build ✓17.64s/lint 卡死 124 预存、冒烟 51 用例 modules-002 ✅ 且 9 失败为基线子集零新增、smoke 增量 +3/-0）；变更范围三方一致（6 业务 + 4 元数据零夹带，RED 提交 acd4a89 中实现文件不存在的 test-first 物证亲验）；六个一票否决逐一核对均未命中（否决项 6 按「基线对照推进」用户裁决口径豁免，先例 sprint-064~068）。改进建议见报告（RestTemplate 超时、base_url SSRF 白名单、路径级细粒度、AC1 守护断言长期价值）。
