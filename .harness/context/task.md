# Sprint 工作单：sprint-090

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-090 |
| 所属模块 | web |
| 功能点 ID | **web/046**（新注册） |
| 功能点名称 | 用户-角色绑定·数据回显与提交契约暗修（三类绑定报障第一单：UserVO 补 roles 回显字段 + 前端 assignRoles POST→PUT 对齐） |
| 状态 | **PLANNED** |
| 创建时间 | 2026-10-12 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| users/001 | 用户 CRUD API（GET /users、GET /users/{id}） | ✅ sprint-016 |
| users/002 | 用户-角色分配 API（PUT /users/{id}/roles，全量替换） | ✅ sprint-017 |
| web/004 | 用户管理页（角色分配按钮/抽屉/列表角色列 formatter 已在场） | ✅ sprint-026 |
| web/041~044 | 四管理页筛选/搜索链路四单已收口（本单为其后新报障） | ✅ sprint-086~089 |
| sprint-089 | 门禁基线（mvn 160=4F+2E 五类、lint 0E\|165W、test 320/320、build ✓、冒烟 70=64✅+4❌+2⏭️） | ✅ PASS 9.8/10 |

## 业务背景

**用户需求（2026-10-12）**：「当前系统 用户与角色绑定 角色与权限绑定 权限与模块绑定 的操作没有可以操作的地方」。

### 三绑定只读探查实录（Planner 只读取证，2026-10-12）

用户报障覆盖三条绑定链路。全仓只读探查（前端视图/API/类型、后端 Controller/DTO/迁移、git 考古）实锤根因分层如下，**本单聚焦第一层「用户-角色绑定」的暗修部分**：

| 绑定链路 | 操作入口现状 | 根因分层（实证） |
|---------|-------------|-----------------|
| 用户 ↔ 角色 | 按钮在场但**永不可见**（`users/IndexView.vue:123` 门禁 `hasPermission('user:assign-role')`，后端 V6/V7 种子 17 权限码**无此码**，ROLE_ADMIN 亦未授权 → `/me` 恒不含 → 恒 false，与 sprint-088 `module:toggle-status` 缺码完全同型）；即便可见，提交必 405（前端 `api/user.ts:42` `http.post('/users/{id}/roles')` vs 后端 `UserController.java:167` `@PutMapping`）；抽屉无回显（`RoleAssignDrawer.vue:141` 打开 `assignedRoleIds=[]`，无按 userId 拉取逻辑 → 提交全量替换会**清空已有绑定**，种子 `sys_user_role` admin→ROLE_ADMIN 将被抹掉）；列表角色列恒 `—`（`users/IndexView.vue:64-72` formatter 读 `row.roles`，后端 `UserVO.java` 8 参**无 roles 字段**；前端类型 `types/user.ts:14` 已声明 `roles?: RoleVO[]` = 前端契约已备好只等后端） |
| 角色 ↔ 权限 | 按钮在场**无门禁渲染**（`roles/IndexView.vue:85-94`，门禁 `canAssignPermission` :120 定义仅 `defineExpose` 未接线）；点开必空树（前端 `permissionApi.getTree()` → `GET /permissions/tree` **后端不存在**，落 `GET /{id}` 转 Long 400）；回显空（`RoleVO.java` 无 permissions 字段，抽屉 :102-115 读 `role.permissions` 恒 undefined）；提交 405（前端 POST vs `RoleController.java:119` PUT）；`role:assign-permission` 后端亦无码 |
| 权限 ↔ 模块 | 表单与「所属模块」选择器**在场**（`PermissionFormDrawer.vue:165-172`，选项来自 `GET /modules/permissions/all`）；疑似断点=保存 400（前端 payload `moduleId`（`types/permission.ts:26/33`）vs 后端 `PermissionCreateDTO.java:23`/`PermissionUpdateDTO.java:18` `@JsonProperty("module_id")`——全仓唯一下划线请求 DTO，无全局 naming-strategy → moduleId 被丢弃 → @NotNull 400，**待 Generator 实测钉死**）；附带：前端编码正则 `/^perm:[a-z0-9:_]+$/`（`PermissionFormDrawer.vue:47-50`）禁 `user:`/`role:`/`module:` 前缀创建 |

**git 考古（只读）**：`git log --diff-filter=D -- frontend/src/views` 零输出；`git log -S "user:assign-role"` 仅增无删——**不存在「入口被删除」历史；入口自 web/004/web/005 落地起就带着后端从未配套的门禁码与断链契约**。

### 本单定位（拆分第一单·暗修不放行）

三绑定总修复面（数据源+契约+种子+门禁+安全授权）预估 **≥25 文件 ≫ 6 文件熔断** → 按行为流程第 3 步**强制拆分总盘 6 单**（登记见「拆分说明」与 session-state 挂起区，一次只注册本单）。本单=**用户-角色绑定·数据回显与提交契约暗修**：

- **修什么**：① `UserVO` 增 `roles` 回显字段并由 ServiceImpl 填充（详情+列表，列表防 N+1）；② 前端 `api/user.ts` `assignRoles` POST→PUT 对齐后端既有契约；③ 双端契约测试先行 RED。
- **不修什么（暗修口径=不放行零恶化）**：不动权限种子（V8 不在本单，按钮门禁 `user:assign-role` 继续拦截 → 用户侧无任何可见变化，无「按钮可见但 405/清空绑定」的中间态恶化）；不动 `RoleAssignDrawer`（回显改造归 web/047）；不动计数断言（17→18 归 web/047 与 V8 同步）。
- **为什么 PUT 对齐归本单而非 047**：后端 PUT 为既有契约（`users/002` 评审通过 10.0、测试全用 PUT），前端改 1 行即对齐；与回显数据源同属「契约与数据层」，同单双端联测；047 放行单只做「种子+计数+抽屉回显消费」，与 sprint-088 修法模板同构。

## 需求与架构对照

- `docs/01-architecture.md` §6.2「用户获得权限的唯一路径是角色（RBAC0）」→ 用户-角色绑定操作是核心 RBAC 语义，其数据可见性（回显）为可操作性前提，本单为其铺数据层，**强化而非违反硬语义**。
- §3「字段名与约束为契约，不得增删改名」指**实体表**字段（sys_user/sys_user_role 等），本单零表变更；`UserVO` 属 API 视图对象，加 `roles` 字段有 038c/039/040 系列先例（VO 层增字段对齐前端），不违 §3。
- §4 未规定写接口 HTTP 方法语义；后端 `PUT /users/{id}/roles`（全量替换、幂等）为已评审契约（users/002 sprint-017，10.0），前端对齐后端=改动面最小（后端零改动），契约测试面以后端为准 → **无架构冲突，无需仲裁**。
- §6.5 停用角色不参与鉴权聚合：回显返回全部绑定关系（含停用角色）属绑定事实陈述，与鉴权语义分层，不冲突（AC 不涉停用角色场景，登记 R1）。

## 验收标准（TDD 驱动）

- [ ] AC1 — 用户详情回显数据源：`GET /api/v1/users/{id}`（id=种子 admin 用户）返回 `code=0` 且 `data.roles` 为非空 JSON 数组，数组元素含 `id`/`name`/`code` 三键，存在元素 `code="ROLE_ADMIN"`；修复前响应**无 `data.roles` 键**（UserVO 8 参）→ RED。← 用例 `UserRolesEchoContractTest#用户详情返回已分配角色且含ROLE_ADMIN`
- [ ] AC2 — 用户列表回显数据源：`GET /api/v1/users?username=admin&page=1&size=10` 返回 `data.records` 中命中行含 `roles` 非空数组且存在元素 `code="ROLE_ADMIN"`；修复前 records 行无 `roles` 键 → RED。← 用例 `UserRolesEchoContractTest#用户列表命中行返回已分配角色`
- [ ] AC3 — 前端提交契约 PUT：前端 API 层契约测试调用 `userApi.assignRoles(1, { roleIds: [2] })`，断言底层 `http.put` 以路径 `/users/1/roles` 与载荷 `{ roleIds: [2] }` 被调用、且 `http.post` 未被以该路径调用；修复前为 `http.post('/users/1/roles')` → RED。← 用例 `<AC3 spec 文件>#assignRoles 使用 PUT 方法`（文件名见研究项 R3）
- [ ] AC4 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-046` 用例（G1 grep `UserVO.java` 存在 `roles` 字段、G2 grep `api/user.ts` 中 `/users/\${id}/roles` 与 `http.put` 同现、G3 定向跑 `UserRolesEchoContractTest`、G4 前端定向跑 AC3 spec），实现前 G1-G4 全 RED → 实现后 GREEN；冒烟总数 70→71、❌ 恰 4（model-008/010、roles-001/002）零新增；门禁五线基线对照零新增。← 用例 `AC4 ← 用例 门禁五线 + 冒烟全量`

### AC1 — 详情回显（UserVO.roles + 填充）

**前置**：真实库种子 `sys_user_role` 存在 admin→ROLE_ADMIN（V6 第 5 段）。

**步骤与期望**：新契约测试类独立方法 MockMvc：`GET /api/v1/users/{adminId}` → `$.code=0`、`$.data.roles` isArray、`$.data.roles[?(@.code=='ROLE_ADMIN')]` 非空（jsonPath 含 code 断言）；护栏：响应其余既有键（id/username/nickname/createTime/updateTime）不变。当前 UserVO 无 roles → `$.data.roles[0].code` 不存在 → **RED**。

**用例**：`AC1 ← 用例 UserRolesEchoContractTest#用户详情返回已分配角色且含ROLE_ADMIN`

### AC2 — 列表回显

**步骤与期望**：独立方法：`GET /api/v1/users?username=admin&page=1&size=10` → `$.data.records[0].roles` 存在且含 code=ROLE_ADMIN 元素（或 records[0] 用 filter 断言 username=admin 行）；护栏：`$.data.total≥1`、既有分页键（records/total/current/size）不变。当前行无 roles 键 → **RED**。

**用例**：`AC2 ← 用例 UserRolesEchoContractTest#用户列表命中行返回已分配角色`

### AC3 — 前端 assignRoles 走 PUT

**步骤与期望**：vitest mock `./http`（http.put/http.post 均 vi.fn），import 真实 `api/user.ts` 调 `userApi.assignRoles(1, { roleIds: [2] })` → 断言 `http.put` 被 calledWith('/users/1/roles', { roleIds: [2] })、`http.post` 未被 calledWith 路径含 `/users/1/roles`。当前实现 http.post → **RED**。既有 `users/IndexView.spec.ts` handleRoleAssignSubmit 用例 mock 的是 userApi 层（不感知方法）→ 预期不受影响（R3）。

**用例**：`AC3 ← 用例 <AC3 spec 文件>#assignRoles 使用 PUT 方法`

### AC4 — 冒烟 web-046 与门禁总账

**步骤与期望**：smoke 新增 web-046：G1 `grep -q 'roles' UserVO.java`（字段形态以 Generator 实现为准钉死，如 `List<.*> roles`）；G2 `grep` 同时命中 `/users/\${id}/roles` 与 `http.put`（api/user.ts）；G3 `mvn -q -Dtest='UserRolesEchoContractTest'` 定向通过；G4 前端定向 AC3 spec 通过——修复前 G1-G4 全 RED；修复后 GREEN。五线亲跑基线对照（口径见前置依赖表，本单后 mvn 用例数 160→162=+2 新方法、前端 test 320→321=+1 spec）。

**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

> TDD 载体：契约测试类两方法 + 前端 AC3 spec **先于** 产品改动写出（当前无 roles 键 / POST 形态 = 天然 RED）；冒烟用例体首跑 RED。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | UserRolesEchoContractTest#用户详情返回已分配角色且含ROLE_ADMIN | AC1 | 待 RED→GREEN |
| 2 | UserRolesEchoContractTest#用户列表命中行返回已分配角色 | AC2 | 待 RED→GREEN |
| 3 | <AC3 spec 文件>#assignRoles 使用 PUT 方法（文件名 R3 定） | AC3 | 待 RED→GREEN |
| 4 | smoke web-046 用例体首跑 + 门禁五线 + 冒烟全量 | AC4 | 待 RED EXIT=1→GREEN EXIT=0；冒烟 71=65✅+4❌+2⏭️ |

## RED 证据

> Generator 于任何修改动作前执行测试清单并粘贴实证（契约类两方法首跑、AC3 spec 首跑、冒烟 web-046 用例体首跑）。

```text
[RED-1] 后端契约测试类两方法首跑（改任何产品代码之前，2026-10-12 mvn -q test -Dtest='UserRolesEchoContractTest'）
 Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
 [RED-AC1] UserRolesEchoContractTest.userDetailReturnsRolesWithAdminRole:84
   No value at JSON path "$.data.roles"（PathNotFoundException: $['data']['roles']）
 [RED-AC2] UserRolesEchoContractTest.userListRowReturnsRoles:108
   JSON path "$.data.records[?(@.username == 'admin')].roles[*].code"
   Expected: a collection containing "ROLE_ADMIN" but: was empty
 （两红均为 UserVO 8 参无 roles 键，与拆分前现状一致）
```

```text
[RED-2] 前端 AC3 spec 首跑（改 api/user.ts 之前，2026-10-12 npx vitest run src/api/user.spec.ts）
 FAIL src/api/user.spec.ts > userApi.assignRoles > assignRoles 使用 PUT 方法
 AssertionError: expected "spy" to be called with arguments: [ Array(2) ]
 Number of calls: 0 （当前实现走 http.post，http.put 未被调用）
 Test Files 1 failed (1) | Tests 1 failed (1)
```

```text
[RED-3] 冒烟 web-046 用例体首跑（OR/roles/PUT 形态未落地，2026-10-12 bash -c G1 起拒绝）
 G1 grep 'List<RoleVO> roles' UserVO.java 拒绝（无 roles 字段）→ 用例体 EXIT=1
 （G2-G4 随 G1 短路未执行，实现落地后全量冒烟逐条验证）
```

```text
[GREEN] 证据槽 — 待 Generator 填写
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-12 sprint-089 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 160 用例 4F+2E（五类：DataSourceConfigBinding×2、RoleController 409、SeedData 22≠2、TestLayers+TestUtils 2E，veto-6 豁免） | 待填（预期 162=160+本单 2 方法，4F+2E 五类逐条一致） | 待验 |
| `npm run lint` | EXIT=0，0 errors / 165 warnings | 待填 | 待验 |
| `npm run test` | 0 failed \| 320 passed → 本单 +1=321 | 待填 | 待验 |
| `npm run build` | ✓（~18s） | 待填 | 待验 |
| `bash scripts/smoke.sh` | 70=64✅+4❌+2⏭️ → 本单 +1=71，❌恰=model-008/010、roles-001/002 零新增 | 待填（预期 71=65✅+4❌+2⏭️，✅含 web-046） | 待验 |

## 拆分说明

### 总需求拆分（>6 文件强制拆分，一次只注册本单）

三类绑定报障总修复面 ≥25 文件（含两域数据源、两域 PUT 对齐、两域抽屉回显、三枚权限种子+计数联动、门禁接线、后端授权、契约修复），拆分总盘如下（**后五单 ⬜ 未注册**，由后续 Sprint 按序规划，登记见 session-state 挂起区）：

| 单 | 功能点 | 范围（预估） |
|----|--------|-------------|
| 本次 | **web/046** 用户-角色·数据回显与提交契约暗修 | UserVO.roles+ServiceImpl 填充+api/user.ts PUT+双端契约测试+smoke（6 文件顶格） |
| 后续① | web/047 用户-角色·入口放行与抽屉回显闭环 | V8 补 `user:assign-role` 挂 user_mgmt+ROLE_ADMIN 授权（幂等）+ SeedDataIntegrationTest/AuthControllerTest 计数 17→18 同步 + RoleAssignDrawer 按 userId 回显（消费 046 的 roles 数据源）+ spec + smoke（≈6） |
| 后续② | web/048 角色-权限·数据源 | `GET /permissions/tree`（或复用 grouped 改前端消费，R 定）+ RoleVO.permissions+填充 + 契约测试 + smoke（≈5-6） |
| 后续③ | web/049 角色-权限·提交契约与抽屉消费 | api/role.ts POST→PUT + PermissionAssignDrawer 消费树与回显 + spec + smoke（≈5） |
| 后续④ | web/050 角色-权限·门禁与授权收口 | V9 补 `role:assign-permission` + roles/IndexView 门禁接线（:85 接 canAssignPermission）+ 后端分配接口方法级授权核查（SecurityConfig:92-95 仅 authenticated，PUT 分配接口越权面）+ 计数同步 + smoke（≈6-8，超则再拆问案B） |
| 后续⑤ | web/051 权限-模块·契约修复 | `module_id`→`moduleId` 收敛（@JsonProperty 对齐 038c camel 先例）+ 前端编码正则放宽至后端语义 + RED 实测 400 实证 + spec + smoke（≈6，先实测钉死再定） |

`web/045` 保留给 session-state 已登记的 BaseInput clearable 候选（sprint-089 范围外登记），本单占用 **web/046** 起号避免冲突。

### 本单拆分判定

预估验收标准 **4 条（=上限）**；预估文件变更 **6 业务**（UserVO+UserServiceImpl+api/user.ts+契约测试类+AC3 spec+smoke；文档回写 4 不计入）——**≤6 文件顶格不拆分**。

**研究项（Generator 先于动手核实）**：

- **R1 roles 字段形态与填充**：前端双消费点已就绪——列表列 formatter 读 `roles[].name`（IndexView.vue:64-72）、类型 `roles?: RoleVO[]`（types/user.ts:14）；建议 `UserVO` 增 `List<com.authcore.dto.role.RoleVO> roles`（复用既有 VO，含 id/name/code 等全键，零新类型）或轻量嵌套 record（id/name/code 三键）；键名 `roles` 无需 @JsonProperty；填充在 toVO（详情+列表共用路径）；**列表防 N+1**：按 userIds 一次 in 查询 sys_user_role+sys_role（mapper 级组装），禁止循环单查；停用角色照样回显（绑定事实与 §6.5 鉴权分层），本单 AC 不涉。
- **R2 既有测试相容**：UserControllerTest 全点状 jsonPath（`$.data.nickname` 等，grep 无键集/hasSize 断言）→ 加字段预期**零连锁**；ListSearchOrSemanticsTest 等 service 层用例不涉 VO 序列化 → 不受影响；若实测出现非过期断言连锁失败 → **停止回报**。
- **R3 AC3 spec 载体**：优先新建 `frontend/src/api/user.spec.ts`（api 层 mock http 断言方法形态，与 089「测试类单文件」同构）；若 api 目录已有 spec 约定则并入；`users/IndexView.spec.ts` 既有 handleRoleAssignSubmit 用例（mock userApi 层）不动不断言方法（盲区登记随 047 收口）。载体文件名在 RED 证据中钉死。
- **R4 暗修不放行口径**：本单**不改** V1-V7 迁移、SeedDataIntegrationTest（17 计数）、AuthControllerTest（me size 17）、RoleAssignDrawer.vue、users/IndexView.vue、roles 域、权限/模块域、后端 Controller/DTO（UserVO 除外）、types/user.ts（已含 roles 不动）→ 按钮继续不可见、抽屉不被触达，**用户侧零可见变化=预期**（放行归 047，中间态零恶化）。
- **R5 冒烟形态与计数**：G1-G4 形态以 Generator 实际写法为准在用例体钉死；`smoke_case` 计数含定义行，以脚本尾部 CASES 为准：70→71。
- **R6 预存工作树**：`backend/src/main/resources/application.yml.template`(D) + `application.template.yml`(??) 为预存改动（sprint-089 在案），**不混入本单提交**，提交时按范围精确 stage。

**超限熔断**：实际业务文件 >6、需改迁移/后端 Controller（UserVO 除外）/RoleAssignDrawer/users/IndexView/计数断言、既有**非过期**断言连锁失败 → **停止回报，回 Planner 重切分**。

**不动清单**：V1-V7 迁移与 SeedData/计数断言；RoleAssignDrawer.vue；users/IndexView.vue 与 users/IndexView.spec.ts 既有用例；roles/permissions/modules 三域全部代码；后端 Controller/DTO（UserVO 除外）；types/user.ts；冒烟既有用例语义与 ❌ 四条不改不删；web/045（BaseInput 候选）、web/036c2/036c3、web/011d 预留；any 165；预存 application 模板两文件。

## 交付物（≈6 业务文件）

1. `backend/src/main/java/com/authcore/dto/user/UserVO.java`：增 `roles` 字段（形态见 R1）
2. `backend/src/main/java/com/authcore/service/impl/UserServiceImpl.java`：toVO 路径填充 roles（详情+列表防 N+1）
3. `backend/src/test/java/com/authcore/controller/UserRolesEchoContractTest.java`：新增契约测试类（AC1/AC2 两方法）
4. `frontend/src/api/user.ts`：assignRoles `http.post`→`http.put`（:42 一行）
5. 前端 AC3 spec（文件名 R3 定，优先 `frontend/src/api/user.spec.ts`）
6. `scripts/smoke.sh`：新增冒烟用例 web-046（AC4）
7. 文档回写：task/session/iteration/registry 四件

## 实现说明

**实现要点**：先写双端契约测试（后端契约类两方法 + 前端 AC3 spec）留 RED 实证；后端 UserVO 加 `roles`（复用 RoleVO）并在 toVO 统一填充（列表一次 in 查询防 N+1）；前端 api/user.ts 一行 POST→PUT；冒烟钉字段/方法形态+定向跑。**本单不放行**（种子与抽屉归 047）：交付后用户侧无可见变化属预期，Evaluator 按暗修口径评审，勿以「按钮仍不可见」判缺陷。

### 新增
- `backend/src/test/java/com/authcore/controller/UserRolesEchoContractTest.java` — 详情/列表回显契约（AC1/AC2）
- 前端 AC3 spec（文件名 R3 定） — assignRoles PUT 契约

### 修改
- `backend/src/main/java/com/authcore/dto/user/UserVO.java` — +roles
- `backend/src/main/java/com/authcore/service/impl/UserServiceImpl.java` — 填充 roles（防 N+1）
- `frontend/src/api/user.ts` — assignRoles POST→PUT
- `scripts/smoke.sh` — 追加冒烟用例 web-046

### 删除
（无）

### 变更计数

业务文件 6（UserVO+UserServiceImpl+api/user.ts+契约类+AC3 spec+smoke）+ 文档回写 4（task/session/iteration/registry），**≤6 顶格达标**；迁移/Controller（UserVO 除外）/Drawer/门禁/计数断言/其余三域零改动。

## 评审意见

（待 Evaluator 评审）
