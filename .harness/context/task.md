# Sprint 工作单：sprint-093

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-093 |
| 所属模块 | web |
| 功能点 ID | **web/048b**（web/048 拆分第二段——角色-权限回显；048a ✅ sprint-092 已收口） |
| 功能点名称 | 角色-权限绑定·回显数据源暗修（RoleVO.permissions 批量填充，修抽屉「已分配权限不回显」） |
| 状态 | **AWAITING_REVIEW**（Generator 九步完成 2026-10-13，移交 Evaluator） |
| 创建时间 | 2026-10-13 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/048a | GET /permissions/tree 平铺数据源（抽屉树数据源） | ✅ sprint-092 PASS 8.4/10 |
| web/005 | 角色管理页与 PermissionAssignDrawer（消费侧在场 :102-115 读 `role.permissions`） | ✅ sprint-027 |
| perms/001 | 权限 CRUD 与 PermissionVO | ✅ sprint-021 |
| roles/001 | 角色 CRUD 与 RoleVO | ✅ sprint-017 |
| web/046 | 批量填充防 N+1 先例（loadRolesByUserIds 模式）与 UserVO.roles 同 sprint 先例 | ✅ sprint-090 PASS 9.6/10 |

## 业务背景

**用户需求（2026-10-12）**：「角色 权限分配接口报错」排查实录（sprint-090，session-state 挂起区②）之回显断点——`PermissionAssignDrawer.vue:102-115` 打开时读 `role.permissions` 渲染已分配勾选，但 `RoleVO` 无 `permissions` 字段 → 恒 `undefined` → **已分配权限不回显**；配合 049 的提交语义（全量替换），存在「看不见既有授权、一提交就清掉」的数据风险。

### 现状与断点实证（sprint-091/092 取证在案）

| 断点 | 实证 |
|------|------|
| **RoleVO 无 permissions** | `RoleVO.java` record 8 参（id/name/code/status/createTime/updateTime…）无 permissions；抽屉 :102-115 `role.permissions` 恒 undefined |
| **消费侧已就位** | `PermissionAssignDrawer.vue:73` 已调 `getTree()`（048a 已修通）、:102-115 读 `role.permissions` 初始化勾选——**前端契约只等后端补字段** |
| **树数据源已通** | 048a 收口：`GET /permissions/tree` 200 平铺 `children=[]`（sprint-092 PASS） |

### 本单定位（暗修不放行）

- **修什么**：`RoleVO` 增 `List<PermissionVO> permissions`；`RoleServiceImpl.toVO` 经 `sys_role_permission` 批量填充（列表路径防 N+1，镜像 046 `loadRolesByUserIds` 模式）。
- **不修什么**：不动 `api/role.ts`（POST→PUT 归 049）；不动 `PermissionAssignDrawer.vue`（消费侧已备好，零前端改动）；不动 `roles/IndexView.vue` 按钮门禁（归 050）；不动 `GET /permissions/tree` 与既有 `/roles` 五映射。**用户侧变化**：抽屉打开能看到已分配勾选（配 048a 的树）；**提交仍 405（049）、按钮无门禁（050）属预期**。

## 需求与架构对照

- §6.2 用户/角色授权链路：角色挂权限为标准语义，本单仅补**读侧回显**数据源，不引入绕过角色的直接授权 → **不冲突**。
- §3 实体表零变更（RoleVO 加字段 = 046 UserVO.roles、048a PermissionVO.children 双先例）。
- §4 禁止 N+1：`sys_role_permission` 关联查询列表路径必须批量（`in roleId` 一批 + `permissionMapper.selectBatchIds` 一批），单条详情路径允许一次按 id 查询；代码须有可 grep 的批量形态（冒烟 G2 护栏）。
- 新增读字段与既有 `/roles` 五映射并存互不替代 → **无架构冲突，无需仲裁**。

## 验收标准（TDD 驱动）

- [x] AC1 — 角色权限回显：`GET /api/v1/roles/{id}`（登录 admin，种子 `ROLE_ADMIN` id=1）返回 HTTP 200、`code=0`，`$.data.permissions` 为 JSON **数组**，数组元素含 `code="user:view"` 且元素含 `id`/`code`/`children` 键（PermissionVO 完整形态）；修复前该路径 `$.data.permissions` 键缺失 → RED。← 用例 `RolePermissionsEchoContractTest#角色详情返回已分配权限回显`
- [x] AC2 — 零回归护栏：`GET /api/v1/roles?page=1&size=10` 返回 `code=0`、`data.records` 数组、命中行既有键（id/name/code/status/createTime/updateTime）不变（`permissions` 新增键对分页消费者非破坏）；`POST /api/v1/roles` 创建再清理链路仍 `code=0`（toVO 改动不破坏既有路径）。← 用例 `RolePermissionsEchoContractTest#既有分页与新建角色接口保持可用`
- [x] AC3 — 空授权角色回显为**空数组**：自建临时角色（`ROLE_REGRESSION_TMP`，必无 `sys_role_permission` 授权；测试库存在其它测试给非 ADMIN/USER 角色授权的污染，种子依赖不稳，故用自建确定性角色）`GET /api/v1/roles/{id}` 返回 `$.data.permissions` 为数组且 `size=0`（空数组而非 null/缺失，前端 `?.length` 判断安全），测试尾部 DELETE 清理。← 用例 `RolePermissionsEchoContractTest#无授权角色回显空权限数组`
- [x] AC4 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-048b` 用例（G1 grep `RoleVO.java` 存在 `List<PermissionVO> permissions` 形态、G2 grep `RoleServiceImpl.java` 存在批量填充形态（`selectBatchIds` 或 `in(...)` 一批）、G3 定向跑 `RolePermissionsEchoContractTest`、G4 grep `PermissionAssignDrawer.vue` 消费 `role.permissions` 形态在场=零前端改动护栏），实现前 G1-G3 RED；冒烟总数 72→73、❌ 恰 4（model-008/010、roles-001/002）零新增；门禁五线基线对照零新增（mvn 165→≈167=+2 契约方法、前端三线=基线零前端改动）。← 用例 `AC4 ← 用例 门禁五线 + 冒烟全量`

### AC1 — 角色详情权限回显

**前置**：种子 ROLE_ADMIN id=1 已被 V6/V7 幂等授权 17 权限码。

**步骤与期望**：契约测试 MockMvc 登录 admin，`GET /api/v1/roles/1` → HTTP 200、`$.code=0`、`$.data.permissions` isArray、`$.data.permissions[?(@.code=='user:view')]` 非空、命中元素含 id/code/children 键。修复前 jsonPath 取 `$.data.permissions` 抛 `No value at JSON path` → **RED**。

**用例**：`AC1 ← 用例 RolePermissionsEchoContractTest#角色详情返回已分配权限回显`

### AC2 — 既有角色接口零回归

**步骤与期望**：独立方法：分页 `GET /api/v1/roles?page=1&size=10` → `$.code=0`、records isArray、首行既有六键在场；幂等清理（`GET /api/v1/roles?code=ROLE_REGRESSION_TMP` 有残留则先 DELETE）后 `POST /api/v1/roles` `{name:"回归临时角色", code:"ROLE_REGRESSION_TMP", status:1}` → `code=0` 且返回 data 含新 id（本方法不断言 `permissions` 键，保持实现前后皆绿的纯护栏），测试尾部经 `DELETE /api/v1/roles/{id}` 清理。基线护栏（实现前后皆绿）。

**用例**：`AC2 ← 用例 RolePermissionsEchoContractTest#既有分页与新建角色接口保持可用`

### AC3 — 无授权角色空数组

**步骤与期望**：独立方法：幂等清理后 POST 建临时角色 `ROLE_REGRESSION_TMP`（必无授权；种子角色因测试库存在其它测试给非 ADMIN/USER 角色授权的污染不稳），`GET /api/v1/roles/{newId}` → `$.code=0`、`$.data.permissions` isArray 且 size=0，finally DELETE 清理。

**用例**：`AC3 ← 用例 RolePermissionsEchoContractTest#无授权角色回显空权限数组`

### AC4 — 冒烟 web-048b 与门禁总账

**步骤与期望**：smoke 新增 web-048b（G1-G4 以 Generator 实际写法钉死）；实现前 G1/G2/G3 RED。五线亲跑基线对照。

**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

> TDD 载体：契约测试类先于产品改动写出（AC1/AC3 无 `$.data.permissions` 键天然 RED）；冒烟用例体首跑 RED。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | RolePermissionsEchoContractTest#角色详情返回已分配权限回显 | AC1 | ✅ RED(No value)→GREEN 3/3 |
| 2 | RolePermissionsEchoContractTest#既有分页与新建角色接口保持可用 | AC2 | ✅ 绿（实现前后皆绿） |
| 3 | RolePermissionsEchoContractTest#无授权角色回显空权限数组 | AC3 | ✅ RED(No value)→GREEN 3/3 |
| 4 | smoke web-048b 用例体首跑 + 门禁五线 + 冒烟全量 | AC4 | ✅ RED(EXIT=1)→GREEN 73=67✅+4❌+2⏭️ |

## RED 证据

> Generator 于任何修改动作前执行测试清单并粘贴实证（契约类首跑、冒烟 web-048b 用例体首跑）。

```text
[RED] 契约类首跑（mvn -q -Dtest=RolePermissionsEchoContractTest test，2026-10-13）：
  Tests run: 3, Failures: 2, Errors: 0, Skipped: 0
  [ERROR] RolePermissionsEchoContractTest.roleDetailReturnsAssignedPermissionsEcho:93
          java.lang.AssertionError: No value at JSON path "$.data.permissions"   ← AC1 RED（ROLE_ADMIN 详情无 permissions 键）
  [ERROR] RolePermissionsEchoContractTest.roleWithoutGrantEchoesEmptyPermissionsArray:201
          java.lang.AssertionError: No value at JSON path "$.data.permissions"   ← AC3 RED（无授权角色同）
  AC2 existingPagedAndCreateRoleEndpointsRemainAvailable 过（基线护栏，实现前后皆绿）
[RED] 冒烟 web-048b 用例体首跑（G1 后短路，EXIT=1）：
  G1 FAIL (预期 RED) ← RoleVO 无 List<PermissionVO> permissions
  （G2 loadPermissionsByRoleIds 不存在同红；G4 PermissionAssignDrawer res.data.permissions 在场=绿护栏）
[GREEN] 契约类定向（mvn -q -Dtest=RolePermissionsEchoContractTest test，2026-10-13）：Tests run: 3, Failures: 0, Errors: 0
  （AC1 ROLE_ADMIN 详情 permissions 含 user:view+id/children；AC2 分页六键+POST/DELETE 链路；AC3 自建临时角色 permissions=[]）
[GREEN] 冒烟 web-048b 用例体（G1 RoleVO permissions + G2 loadPermissionsByRoleIds+selectBatchIds 批量形态 + G3 定向 + G4 抽屉消费契约）：✅
GREEN 期间两处测试口径修正（实现行为不变）：
  ① AC1 filter 后 .id[0] Jayway 语法不支持 → 改 Java 断言遍历节点断言 id/children；
  ② AC3 种子依赖被测试库污染（其它测试给非 ADMIN/USER 角色授权过 user:view）→ 改自建临时角色 ROLE_REGRESSION_TMP 确定性断言空数组，免迁移种子面漂移。
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-13 sprint-092 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 165 用例 4F+2E（五类：DataSourceConfigBinding×2、RoleController 409、SeedData 22≠2、TestLayers+TestUtils 2E，veto-6 豁免） | 168=165+3；4F+2E 五类逐条一致（DataSourceConfigBinding×2、RoleController 409、SeedData 22≠2、TestLayers+TestUtils 2E），零新增 | ✅ |
| `npm run lint` | EXIT=0，0 errors / 165 warnings | 0 errors / 165 warnings | ✅ |
| `npm run test` | 39 文件 321/321 | 39 文件 321/321 | ✅ |
| `npm run build` | ✓（~17.5s） | ✓ built in 17.51s | ✅ |
| `bash scripts/smoke.sh` | 72=66✅+4❌+2⏭️ → 本单 +1=73，❌恰=model-008/010、roles-001/002 零新增 | 73=67✅+4❌+2⏭️；❌恰=model-008/010、roles-001/002 与基线逐一吻合，✅含 web-048b | ✅ |

## 实现说明

**实现要点**：先写契约测试类三方法（AC1/AC3 天然 RED、AC2 护栏）+ smoke 用例体留 RED 实证；后端 `RoleVO` 加 `List<PermissionVO> permissions`（注意 import com.authcore.dto.permission.PermissionVO——跨包 dto 引用，046 UserVO 引 RoleVO 同款先例）；`RoleServiceImpl` 组装：
- **单条路径**（getRoleById/createRole/updateRole）：按 roleId 查 `sys_role_permission` 一批 + `permissionMapper.selectBatchIds` 一批，或一次 join/两次批量；
- **列表路径**（queryRoles convert）：先收集页内 roleIds → `rolePermissionMapper.selectList(in roleId 一批)` → `permissionMapper.selectBatchIds(permIds 一批)` → 内存组装 map → toVO 带参；
- 可与 046 `loadRolesByUserIds`（UserServiceImpl）对照同构；空授权 → `List.of()`（空数组非 null）。
- **分工提醒**：G2 批量形态可 grep `selectBatchIds`（RoleServiceImpl）；若实现选择其它批量 API（如一次 selectList in 查询后仅两次总查询），G2 以实际形态钉死但必须能区分「批量」与「循环单查」。

**超限熔断**：实际业务文件 >6、需改迁移/Drawer/IndexView/api/role.ts/门禁/既有 `/roles` 五映射、非过期断言连锁失败 → 停止回报回 Planner。

**不动清单**：V1-V7 迁移与计数断言（V8 归 047、V9 归 050）；`api/role.ts`（归 049）；`PermissionAssignDrawer.vue`、`roles/IndexView.vue`（归 049/050）；`GET /permissions/tree` 与 `/roles` 既有五映射；`types/` 前端类型；冒烟既有用例与 ❌ 四条；web/045/047/049/050/051 预留；any 165；预存 application 模板两文件（D/?? 重命名非本单动作，不混提交）。

## 交付物（4 业务文件）

1. `backend/src/main/java/com/authcore/dto/role/RoleVO.java` — +`List<PermissionVO> permissions`
2. `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java` — toVO 批量填充（列表批量/单条按需）
3. `backend/src/test/java/com/authcore/controller/RolePermissionsEchoContractTest.java` — AC1/AC2/AC3 三方法
4. `scripts/smoke.sh` — 新增 web-048b 用例（AC4）
5. 文档回写：task/session/iteration/registry 四件

### 新增
- `backend/src/test/java/com/authcore/controller/RolePermissionsEchoContractTest.java`

### 修改
- `backend/src/main/java/com/authcore/dto/role/RoleVO.java`
- `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java`
- `scripts/smoke.sh`

### 删除
（无）

### 变更计数

业务文件 **4 顶格**；前端（含 Drawer/IndexView/api/role.ts）、迁移、`GET /permissions/tree`、`/roles` 既有五映射零改动。

## 评审意见

（待 Evaluator 评审）
