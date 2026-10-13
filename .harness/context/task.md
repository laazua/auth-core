# Sprint 工作单：sprint-091

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-091 |
| 所属模块 | web |
| 功能点 ID | **web/048**（新注册，拆分总盘后续②提前——用户报障直提） |
| 功能点名称 | 角色-权限绑定·数据源暗修（GET /permissions/tree 新增 + RoleVO.permissions 回显字段） |
| 状态 | **BLOCKED**（Generator 超限熔断 2026-10-12：实际 8 文件 >6，Planner 预估漏算 Service 层两文件与 PermissionVO children 载体；重切分建议 048a=tree 数据源 6 文件顶格 / 048b=RoleVO 回显 4 文件，见 session-state 挂起区；R1 平铺方案定夺在案） |
| 创建时间 | 2026-10-12 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| roles/001 | 角色 CRUD API 与 RoleVO | ✅ sprint-019 |
| perms/001 | 权限 CRUD API（GET /permissions 分页、/grouped） | ✅ sprint-021 |
| web/005 | 角色管理页（PermissionAssignDrawer/门禁按钮已在场） | ✅ sprint-027 |
| web/046 | 用户-角色暗修先例（VO 加字段+前端消费侧已备好只等后端模式） | ✅ sprint-090 PASS 9.6/10 |
| sprint-090 | 门禁基线（mvn 162=4F+2E 五类、lint 0E\|165W、test 321/321、build ✓、冒烟 71=65✅+4❌+2⏭️） | ✅ PASS 9.6/10 |

## 业务背景

**用户需求（2026-10-12）**：「角色 权限分配接口报错」+ 9998 日志实证——`GET /permissions/tree` 请求落到 `GET /permissions/{id}`，`MethodArgumentTypeMismatchException: For input string: "tree"`，GlobalExceptionHandler 兜底 500。

### 根因实锤（Planner 只读取证，2026-10-12，9998 在线探针）

| 断点 | 实证 |
|------|------|
| **tree 接口缺失** | 9998 实测 `GET /permissions/tree` → HTTP 500 `{"code":500}`（MethodArgumentTypeMismatchException，同用户日志）；`PermissionController` 仅有 `GET /permissions`(:44)、`/grouped`(:63)、`/{id}`(:76)，**无 /tree** |
| **/grouped 不可复用** | 9998 实测 `/grouped` 返回 `Map<ModuleVO, List<PermissionVO>>`——Jackson 序列化对象键为 `ModuleVO[id=1, name=用户管理, code=user_mgmt]` **toString 串**；前端 `PermissionTreeNode extends PermissionVO { children: PermissionTreeNode[] }`（types/permission.ts:38-40）期望嵌套数组，无法消费 |
| **回显数据源缺失** | `RoleVO.java` 无 `permissions` 字段；`PermissionAssignDrawer.vue:102-115` 读 `role.permissions` 恒 undefined |
| **消费侧已就位（与 046 同构）** | `PermissionAssignDrawer.vue:73` 调 `permissionApi.getTree()`、`api/permission.ts:15-16` 已声明 `GET /permissions/tree` 返回 `PermissionTreeNode[]`——**前端契约已备好只等后端** |

**范围外登记（归 web/049/050，本单不动）**：提交契约 405（`api/role.ts:31` `http.post` vs `RoleController:119` `@PutMapping`）；`role:assign-permission` 后端无码+`roles/IndexView.vue:85-94` 无门禁渲染（`canAssignPermission` :120 定义未接线）；分配接口方法级 `@PreAuthorize` 缺失（SecurityConfig:92-95 仅 authenticated 越权面）——均归 049/050。

### 本单定位（拆分总盘第 2 单·暗修不放行）

三绑定总盘（session-state 挂起区 2026-10-12）后续②。**用户 2026-10-12 报障直提角色-权限链路 → 048 提前于 047**（047 用户-角色放行闭环顺延，两单不同链路无依赖）。本单=**角色-权限·数据源暗修**：

- **修什么**：① 后端新增 `GET /permissions/tree` 返回前端可消费的 `PermissionTreeNode[]`；② `RoleVO` 增 `permissions` 字段并由 `RoleServiceImpl.toVO` 填充（列表防 N+1）；③ 双端契约测试先 RED。
- **不修什么（暗修口径=不放行零恶化）**：不动 `api/role.ts`（POST→PUT 归 web/049）；不动 `roles/IndexView.vue`/`RoleAssignDrawer.vue`/门禁接线（归 049/050）；不动 `PermissionController` 既有 `/`、`/grouped`、`/{id}`、POST、PUT 映射（零回归护栏）；不动 V1-V7 迁移与计数断言。**用户侧变化**：抽屉点开不再 500、树可渲染、回显有数据；**提交仍 405 属预期**（归 049），无中间态数据损坏（提交不可达前清空类恶化不存在——提交 405 即拒绝）。

## 需求与架构对照

- `docs/01-architecture.md` §6.1「权限必须归属模块」→ tree 以模块为分组层返回权限，**强化而非违反**硬语义。
- §6.4 删除保护「删除角色前校验 sys_role_permission 引用」→ RoleVO.permissions 为绑定事实陈述，不动删除语义。
- §3「字段名与约束为契约」指实体表——本单零表变更；`RoleVO` 属 API 视图对象，加 `permissions` 有 046 UserVO.roles 先例（同 sprint 当日），不违 §3。
- §4 未定义 GET 方法语义细节；`GET /permissions/tree` 为新增只读端点，与既有分页/`/grouped` 并存互不替代 → **无架构冲突，无需仲裁**。

## 验收标准（TDD 驱动）

- [ ] AC1 — tree 接口数据源：`GET /api/v1/permissions/tree`（登录 admin）返回 `code=0` 且 `data` 为 JSON 数组，数组元素含 `id`/`code`/`children` 键（`children` 可为数组可为空），全部 17 条种子权限中存在 `code="user:view"` 的节点；修复前该路径 500（落 `/{id}` Long 转换失败）→ RED。← 用例 `PermissionTreeContractTest#树接口返回可消费结构且含种子权限`
- [ ] AC2 — 角色回显数据源：`GET /api/v1/roles/{id}`（id=种子 ROLE_ADMIN 的 id）返回 `code=0` 且 `data.permissions` 为非空 JSON 数组，数组元素含 `id`/`code` 两键，存在元素 `code="user:view"`；修复前响应无 `data.permissions` 键（RoleVO 无该字段）→ RED。← 用例 `RolePermissionsEchoContractTest#角色详情返回已分配权限且含user_view`
- [ ] AC3 — 既有权限接口零回归护栏：`GET /api/v1/permissions?page=1&size=10` 返回 `code=0`、`data.records` 数组、命中行含 `code="user:view"` 且**无新增破坏性键变更**；`GET /api/v1/permissions/grouped` 返回 `code=0`。← 用例 `PermissionTreeContractTest#既有分页与grouped接口保持可用`
- [ ] AC4 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-048` 用例（G1 grep `PermissionController.java` 存在 `/tree` 映射形态、G2 grep `RoleVO.java` 存在 `permissions` 字段形态、G3 定向跑两个契约测试类、G4 前端 `api/permission.ts` getTree 形态在场），实现前 G1-G3 RED；冒烟总数 71→72、❌ 恰 4（model-008/010、roles-001/002）零新增；门禁五线基线对照零新增（mvn 162→164=+2 契约方法量级、前端 test 不增=本单零前端代码）。← 用例 `AC4 ← 用例 门禁五线 + 冒烟全量`

### AC1 — GET /permissions/tree（新增只读端点）

**前置**：种子 17 权限全挂四模块（V6/V7）。

**步骤与期望**：契约测试 MockMvc 登录 admin 后 `GET /api/v1/permissions/tree` → `$.code=0`、`$.data` isArray、`$.data..code` 集合 hasItem("user:view")、结构键（`children` 或顶层模块分组键）在场；护栏：响应 HTTP 200（非 500）。修复前 500 → **RED**。

**用例**：`AC1 ← 用例 PermissionTreeContractTest#树接口返回可消费结构且含种子权限`

### AC2 — RoleVO.permissions 回显

**步骤与期望**：独立类独立方法：种子库查 `sys_role.code=ROLE_ADMIN` 得 id → `GET /api/v1/roles/{id}` → `$.code=0`、`$.data.permissions` isArray、`$.data.permissions[?(@.code=='user:view')]` 非空；护栏：RoleVO 既有键（id/name/code/status/createTime/updateTime）不变。修复前无 `$.data.permissions` → **RED**。

**用例**：`AC2 ← 用例 RolePermissionsEchoContractTest#角色详情返回已分配权限且含user_view`

### AC3 — 零回归护栏

**步骤与期望**：独立方法：`GET /api/v1/permissions?page=1&size=10` → `$.code=0`、`$.data.records` isArray、records 含 code=user:view 行；`GET /api/v1/permissions/grouped` → `$.code=0`。断言为既有契约的存续性护栏，实现后必须全绿（实现前亦绿——基线护栏，非 RED 驱动）。

**用例**：`AC3 ← 用例 PermissionTreeContractTest#既有分页与grouped接口保持可用`

### AC4 — 冒烟 web-048 与门禁总账

**步骤与期望**：smoke 新增 web-048：G1 `grep -q '/tree' PermissionController.java`（映射形态以 Generator 实现为准钉死）、G2 `grep -q 'List<PermissionVO> permissions' RoleVO.java`（字段形态钉死）、G3 `mvn -q -Dtest='PermissionTreeContractTest+RolePermissionsEchoContractTest'` 定向通过、G4 grep `api/permission.ts` getTree 形态在场；实现前 G1/G2/G3 RED。五线亲跑基线对照（口径见前置依赖表）。

**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

> TDD 载体：两个契约测试类**先于**产品改动写出（tree 500 / 无 permissions 键 = 天然 RED）；冒烟用例体首跑 RED。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | PermissionTreeContractTest#树接口返回可消费结构且含种子权限 | AC1 | 待 RED→GREEN |
| 2 | RolePermissionsEchoContractTest#角色详情返回已分配权限且含user_view | AC2 | 待 RED→GREEN |
| 3 | PermissionTreeContractTest#既有分页与grouped接口保持可用 | AC3 | 基线护栏（实现前后皆绿） |
| 4 | smoke web-048 用例体首跑 + 门禁五线 + 冒烟全量 | AC4 | 待 RED EXIT=1→GREEN EXIT=0；冒烟 72=66✅+4❌+2⏭️ |

## RED 证据

> Generator 于任何修改动作前执行测试清单并粘贴实证（两契约类首跑、冒烟 web-048 用例体首跑）。

```text
[RED] 证据槽 — 待 Generator 填写
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-12 sprint-090 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 162 用例 4F+2E（五类：DataSourceConfigBinding×2、RoleController 409、SeedData 22≠2、TestLayers+TestUtils 2E，veto-6 豁免） | 待填（预期 164=162+本单契约方法，4F+2E 五类逐条一致） | 待验 |
| `npm run lint` | EXIT=0，0 errors / 165 warnings | 待填（本单零前端代码改动预期 = 基线） | 待验 |
| `npm run test` | 39 文件 321/321 | 待填（本单零前端代码改动预期 = 321/321） | 待验 |
| `npm run build` | ✓（~18s） | 待填 | 待验 |
| `bash scripts/smoke.sh` | 71=65✅+4❌+2⏭️ → 本单 +1=72，❌恰=model-008/010、roles-001/002 零新增 | 待填（预期 72=66✅+4❌+2⏭️，✅含 web-048） | 待验 |

## 拆分说明

### 总盘顺序调整（用户报障直提）

| 单 | 功能点 | 状态 |
|----|--------|------|
| 046 | 用户-角色·数据回显与 PUT 契约暗修 | ✅ sprint-090 |
| 047 | 用户-角色·入口放行与抽屉回显闭环 | ⬜ 顺延（用户-角色链路，与本单无依赖） |
| **本次** | **web/048** 角色-权限·数据源暗修 | 本次 sprint-091 |
| 049 | 角色-权限·提交契约（api/role.ts PUT）与抽屉消费收口 | ⬜ |
| 050 | 角色-权限·门禁与授权收口（role:assign-permission+@PreAuthorize） | ⬜ |
| 051 | 权限-模块·moduleId 契约修复 | ⬜ 待实测 |

### 本单拆分判定

预估验收标准 **4 条（=上限）**；预估文件变更 **6 业务**（PermissionController+RoleVO+RoleServiceImpl+两个契约测试类计 2+smoke——若两测试类合一文件则 5；文档回写 4 不计入）——**≤6 文件顶格不拆分**。

**研究项（Generator 先于动手核实）**：

- **R1 tree 形态定夺（关键）**：前端 `PermissionTreeNode extends PermissionVO { children }` + `PermissionAssignDrawer.vue:73-115` 消费逻辑（filterNode 按 code/name 过滤、collectIds 展开）决定树应为「模块父节点+权限 children」还是「纯权限平铺树（children 恒空）」。候选：a) 模块节点伪装 Permission 形态（id=module.id、code=module.code、children=权限）——前端零改动；b) 纯权限数组 children=[]——模块分组信息靠 moduleName 字段（PermissionVO 已含 moduleId/moduleName）。**Generator 读完整个 PermissionAssignDrawer filterNode/模板渲染后钉死一种，写入 GREEN 证据**；禁改前端消费侧。
- **R2 填充防 N+1**：RoleVO.permissions 填充走 `sys_role_permission` 一次 in 批量 + `permissionMapper.selectBatchIds`（镜像 046 loadRolesByUserIds 模式）；列表 `GET /roles` 与详情 `GET /roles/{id}` 共用路径。
- **R3 既有测试相容**：UserControllerTest/RoleControllerTest 等点状 jsonPath 断言不排键集 → 加字段零连锁；`/grouped` 与分页实现零触碰。若实测非过期断言连锁失败 → **停止回报**。
- **R4 前端零改动**：`api/permission.ts`/`PermissionAssignDrawer.vue`/`types/permission.ts`/`api/role.ts` 全部不动（GET 侧消费已就位；PUT 归 049）。
- **R5 冒烟形态**：G1-G4 以 Generator 实际写法钉死；CASES 计数 71→72。

**超限熔断**：实际业务文件 >6、需改迁移/api/role.ts/Drawer/IndexView/门禁/计数断言、既有**非过期**断言连锁失败 → **停止回报，回 Planner 重切分**。

**不动清单**：V1-V7 迁移与计数断言；`api/role.ts`；`roles/IndexView.vue`/`PermissionAssignDrawer.vue`；`PermissionController` 既有五映射；`types/permission.ts`；冒烟既有用例语义与 ❌ 四条；web/045/047/049/050/051 预留；any 165；预存 application 模板两文件。

## 交付物（≈6 业务文件）

1. `backend/src/main/java/com/authcore/controller/PermissionController.java`：新增 `GET /tree` 方法（形态 R1 定）
2. `backend/src/main/java/com/authcore/dto/role/RoleVO.java`：增 `permissions` 字段
3. `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java`：toVO 填充 permissions（防 N+1）
4. `backend/src/test/java/com/authcore/controller/PermissionTreeContractTest.java`：AC1+AC3（两方法）
5. `backend/src/test/java/com/authcore/controller/RolePermissionsEchoContractTest.java`：AC2
6. `scripts/smoke.sh`：新增冒烟用例 web-048（AC4）
7. 文档回写：task/session/iteration/registry 四件

## 实现说明

**实现要点**：先写两个契约测试类（tree 500 天然 RED + RoleVO 无 permissions 天然 RED）留实证；后端加 `/tree` 端点（形态 R1 对齐前端既有消费）+ RoleVO.permissions 批量填充；前端零改动。**本单不放行提交链路**（PUT 归 049）：交付后抽屉可打开可渲染可回显、**提交仍 405 属预期**，Evaluator 按暗修口径评审，勿以「提交 405/按钮无门禁」判缺陷。

### 新增
- `backend/src/test/java/com/authcore/controller/PermissionTreeContractTest.java` — tree+零回归（AC1/AC3）
- `backend/src/test/java/com/authcore/controller/RolePermissionsEchoContractTest.java` — 角色回显（AC2）

### 修改
- `backend/src/main/java/com/authcore/controller/PermissionController.java` — +GET /tree
- `backend/src/main/java/com/authcore/dto/role/RoleVO.java` — +permissions
- `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java` — 填充 permissions（防 N+1）
- `scripts/smoke.sh` — 追加冒烟用例 web-048

### 删除
（无）

### 变更计数

业务文件 6（Controller+RoleVO+RoleServiceImpl+两契约类+smoke）+ 文档回写 4，**≤6 顶格达标**；api/role.ts/Drawer/IndexView/门禁/迁移/计数断言/既有五映射零改动。

## 评审意见

（待 Evaluator 评审）
