# Sprint 工作单：sprint-071

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-071 |
| 所属模块 | web |
| 功能点 ID | web/038b |
| 功能点名称 | 模块服务统一入口（b 段）——可访问模块列表 `GET /modules/accessibles`（后端） |
| 状态 | PLANNED |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/003 | JWT 认证过滤器（登录态判定与当前用户解析来源） | ✅ |
| auth/004 | GET /api/v1/auth/me 当前用户信息（`AuthService.getCurrentUserInfo(userId)` 提供用户有效权限集合，网关同款数据源） | ✅ |
| modules/001 | 模块 CRUD 基建（ModuleController/ModuleServiceImpl/ModuleVO 落点，本单在其内新增端点） | ✅ |
| modules/002 | 网关准入语义（`checkAllowed`：启用 ∧ 权限交集≥1，本单列表化复用同款语义，sprint-069 评审通过） | ✅ |
| model/004 | sys_module 表模型（模块列表与 status 过滤数据源） | ✅ |
| model/010 | sys_permission 表模型（权限归属模块、交集判定数据源） | ✅ |
| web/038a | Cookie 双承载与网关内嵌头处理（本组前序段，sprint-070 评审通过，均分 9.0/10） | ✅ |

## 业务背景

用户原始需求（sprint-069/070 一组）：模块管理中的「模块」是外部运行服务，管理员授权后，**登录用户应在当前系统中看到自己可以访问的模块列表并进行访问**。设计文档 `docs/superpowers/specs/2026-09-29-module-iframe-access-design.md`（用户逐节确认，commit 83b19fb）第 3 节表格第 3 行规定：

- `GET /modules/accessibles`：ModuleController/Service 新增；任何登录用户可调（仅要求 authenticated，**不绑 `module:view`**——该语义由 `SecurityConfig` 现有 `anyRequest().authenticated()` 天然覆盖，本单零 SecurityConfig 改动）；返回精简记录列表（`id/name/code/baseUrl/description/status`）；条件「启用 ∧ 用户权限交集≥1」，复用网关同款准入语义。

本端点是 038d「我的模块」菜单页的唯一数据源；038e iframe 视图按 code 跳转 `/workspace/module/:code`。

## 需求描述

新增 `GET /api/v1/modules/accessibles`：任何已登录用户调用，返回当前用户可访问的模块精简列表。过滤语义 = 网关 `checkAllowed` 的列表化：模块 `status=1`（启用，架构 §6.5 停用模块权限无效）∧ 模块在 `sys_permission` 下的权限码与当前用户有效权限集合交集 ≥1（架构 §6.1 权限必须归属模块）。与网关差异仅在：网关对单模块拒绝抛 403/1304/1305，本端点对列表做静默过滤（不出现即不可达，设计第 5 节「403/1304/1305 场景被前置消灭」）。

## 验收标准（TDD 驱动）

- [ ] AC1 — 登录用户调用 `GET /api/v1/modules/accessibles` 返回 200、`code=0`，列表含「启用 ∧ 权限交集≥1」的模块（至少种子模块 `user_mgmt` 出现），每条记录字段为 `id/name/code/baseUrl/description/status` 六项（不含 `createdAt/updatedAt` 等管理字段）
- [ ] AC2 — 当前用户有权限交集但 `status=0` 的模块不出现在列表（架构 §6.5）
- [ ] AC3 — `status=1` 但与当前用户权限交集为空的模块不出现在列表（架构 §6.1，与网关 checkAllowed 同款）
- [ ] AC4 — 未登录请求返回 401、`code=1401`；后端门禁零新增失败（基线对照）；冒烟 `web-038b` 单跑通过

### AC1 — 登录用户可访问列表含交集启用模块且字段精简
> Given 已登录（admin，持 `user:view` 等种子权限）。When `GET /api/v1/modules/accessibles`。Then 200、`$.code=0`、`$.data` 为数组且含 `code=user_mgmt` 的记录（该记录 `status=1`、`baseUrl/description` 字段存在），记录字段集为 `id/name/code/baseUrl/description/status`（`createdAt` 不出现）。

**用例**：`AC1 ← 用例 ModuleControllerTest#accessiblesReturnsEnabledModulesWithPermissionOverlap`

### AC2 — 停用模块不出现
> Given 用 mapper 将 `user_mgmt` 置 `status=0`（@Transactional 回滚）。When 登录调用 accessibles。Then 响应 `$.data` 不含 `code=user_mgmt`。

**用例**：`AC2 ← 用例 ModuleControllerTest#accessiblesExcludesDisabledModule`

### AC3 — 无权限交集的启用模块不出现
> Given 新建 `status=1` 模块并挂一个当前用户不具备的权限码（测试前缀命名）。When 登录调用 accessibles。Then 响应 `$.data` 不含该 `code`（启用但交集为空 → 静默过滤）。

**用例**：`AC3 ← 用例 ModuleControllerTest#accessiblesExcludesModuleWithoutPermissionOverlap`

### AC4 — 未登录 401，门禁与冒烟通过
> ① 未携带任何凭证 `GET /api/v1/modules/accessibles` → 401、`$.code=1401`（`anyRequest().authenticated()` + JWT entryPoint 天然覆盖，无 SecurityConfig 改动）。② `mvn -q verify` 失败集与实现前基线逐条一致零新增（2026-09-29 口径：141 用例 4F+2E，sprint-070 实测）。③ 本功能点零前端改动，前端门禁不适用（基线对照实测留痕：lint 124 挂起 / test 20 failed / build ✓）。④ `bash scripts/smoke.sh` 新增 `web-038b` 用例单跑通过、整体失败与基线逐条一致零新增（基线：52 用例 9 失败）。

**用例**：`AC4 ← 用例 ModuleControllerTest#accessiblesUnauthenticatedReturns401 + mvn -q verify 基线对照 + bash scripts/smoke.sh#web-038b`

## 测试清单

> Generator 按 `.harness/rules/tdd-workflow.md` 先于实现写出并运行留 RED 证据；每条验收标准至少一例。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | ModuleControllerTest#accessiblesReturnsEnabledModulesWithPermissionOverlap | AC1 | ✅ RED（200 期望实得 500，端点缺失） |
| 2 | ModuleControllerTest#accessiblesExcludesDisabledModule | AC2 | ✅ RED（200 期望实得 500） |
| 3 | ModuleControllerTest#accessiblesExcludesModuleWithoutPermissionOverlap | AC3 | ✅ RED（200 期望实得 500） |
| 4 | ModuleControllerTest#accessiblesUnauthenticatedReturns401 | AC4-① | ✅ 实现前即绿（无凭证在 Security 链即 401，与端点存在无关，守护断言如实登记，非 RED） |
| 5 | 冒烟 web-038b 定向测试（追加于 scripts/smoke.sh，定向跑本测试类） | AC4-④ | （待追加） |
| 6 | 后端全量门禁基线对照（实现前基线：141 用例 4F+2E，2026-09-29 sprint-070 实测） | AC4-②③ | （待实测） |

**行数红线**：ModuleControllerTest 现 421 行，`coding-standards` §1 文件 ≤500 行——4 用例须紧凑（预估 +75 行内）；若逼近红线立即回报 Planner，禁止为腾行数删改既有用例。

## RED 证据

> Generator 于实现前执行测试清单 #1~#4 并粘贴关键失败输出（实时留痕，2026-09-29）。

```text
[RED] mvn -f backend/pom.xml test -Dtest='ModuleControllerTest'（实现前执行）
Tests run: 11, Failures: 3, Errors: 0, Skipped: 0
[RED] accessiblesReturnsEnabledModulesWithPermissionOverlap:453->callAccessibles:431 Status expected:<200> but was:<500>（GET /modules/accessibles 端点不存在，/{id} 模板吞路径致转换失败）
[RED] accessiblesExcludesDisabledModule:475->callAccessibles:431 Status expected:<200> but was:<500>
[RED] accessiblesExcludesModuleWithoutPermissionOverlap:488->callAccessibles:431 Status expected:<200> but was:<500>
AC4 accessiblesUnauthenticatedReturns401 实现前即绿：无凭证请求在 Security 过滤链即由 entryPoint 判 401+1401，与端点存在性无关（守护断言如实登记，非 RED；与 AC1-3 构成对照）
```

## 门禁与冒烟记录

> Generator 亲测填写（后端 mvn -q verify / 前端门禁基线对照 / scripts/smoke.sh），Evaluator 不采信自述须亲跑。

（待 Generator 填写）

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 **6 个（顶格）**：

1. `backend/src/main/java/com/authcore/controller/ModuleController.java` — 新增 `GET /accessibles`（字面路径优先于 `/{id}` 模板，须以测试实证不被 `{id}` 吞掉；当前用户解析复用 GatewayController:46 SecurityContext 模式，不新增文件）（AC1-AC3）
2. `backend/src/main/java/com/authcore/service/ModuleService.java` — 接口新增 `listAccessibles(...)` 方法
3. `backend/src/main/java/com/authcore/service/impl/ModuleServiceImpl.java` — 实现：启用模块全量查询 + `sys_permission` 按 `module_id IN` 批量单查 + 用户有效权限集合单取，内存交集组装（**禁 N+1**；模块/权限为字典数据全量加载，注释规模预期 ≤1000 行）（AC1-AC3）
4. `backend/src/main/java/com/authcore/dto/module/ModuleAccessibleVO.java` — **新增** record（`id/name/code/baseUrl/description/status` 六字段，对齐设计第 3 节，不复用含时间字段的 ModuleVO 以隔离 038c 字段错位问题）
5. `backend/src/test/java/com/authcore/controller/ModuleControllerTest.java` — 追加 AC1-AC4 四方法（测试先行；沿既有 @Transactional/getAdminToken/unique 基建）
6. `scripts/smoke.sh` — 追加 `web-038b` 用例（不删除、不改动既有用例，先例 web/037、modules/002、web/038a）

**超限熔断**：实现中若实际变更文件超过 6 个，Generator 必须停止实现并回报 Planner 重新拆分，不得自行扩范围。字段修复（038c）、一切前端改动（038d/038e）、`{id}` 既有端点行为变更一律不做。

## 交付物（预估 6 文件）

1. `backend/src/main/java/com/authcore/controller/ModuleController.java` — GET /accessibles 端点
2. `backend/src/main/java/com/authcore/service/ModuleService.java` — 接口方法
3. `backend/src/main/java/com/authcore/service/impl/ModuleServiceImpl.java` — 列表准入实现
4. `backend/src/main/java/com/authcore/dto/module/ModuleAccessibleVO.java` — 精简记录 VO（新增）
5. `backend/src/test/java/com/authcore/controller/ModuleControllerTest.java` — 四用例（测试先行）
6. `scripts/smoke.sh` — web-038b 冒烟用例

## 变更清单

### 新增
- （待 Generator 填写）

### 修改
- （待 Generator 填写）

### 删除
- （无）

## 规范检查清单

- [ ] `mvn -q verify` 后端门禁零新增失败（基线对照：141 用例 4F+2E）
- [ ] 前端门禁不适用（本功能点零前端改动；test/build/lint 基线对照实测留痕于「门禁与冒烟记录」）
- [ ] `bash scripts/smoke.sh` 新增 web-038b 单跑通过且整体与基线逐条一致（52 用例 9 失败口径）
- [ ] 符合 Java 21 / Spring Boot 3 / Spring Security 6 编码规范与既有分层约定（controller → service 接口 → impl，构造器注入 final 字段，无字段注入；禁 N+1）
- [ ] 符合 TDD 工作流（测试先行、RED 证据实时留痕、GREEN 复跑、REFACTOR 复查）
- [ ] 变更文件不超过 6 个（顶格熔断，超出即回报 Planner）
- [ ] 测试类文件 ≤500 行红线（ModuleControllerTest 421 起步）

## 评审记录

- （待 Evaluator）
