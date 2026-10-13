# Sprint 工作单：sprint-094

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-094 |
| 所属模块 | web |
| 功能点 ID | **web/047**（用户-角色放行闭环；三绑定拆分总盘后续①，sprint-090 登记，本次注册立项） |
| 功能点名称 | 用户-角色绑定·放行与回显闭环（V8 补 `user:assign-role` 种子+授权，RoleAssignDrawer 按 userId 回显已分配角色） |
| 状态 | **AWAITING_REVIEW**（Generator 交付完成 2026-10-13，RED 证据+门禁亲测在案） |
| 创建时间 | 2026-10-13 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/046 | UserVO.roles 数据源+PUT 契约（抽屉回显消费源） | ✅ sprint-090 PASS 9.6/10 |
| users/001 | 用户 CRUD 与 UserController（PUT /users/{id}/roles） | ✅ |
| users/002 | 用户管理页与 RoleAssignDrawer（按钮门禁 :123、抽屉组件） | ✅ |
| web/044 | 用户列表搜索（IndexView 既有形态参考） | ✅ sprint-089 |
| perms/001 | 权限种子与 sys_permission 计数基线（17→18 联动） | ✅ |

## 业务背景

**用户需求（2026-10-12 报障排查实录①，session-state 挂起区在案）**：「用户与角色绑定的操作没有可以操作的地方」——三断点中 046 已修数据源与 PUT 契约，**剩余两断点本单收口**：

### 断点实锤（sprint-090 只读取证）

| 断点 | 实证 |
|------|------|
| **按钮永不可见** | `users/IndexView.vue:123` 门禁 `user:assign-role`；V6/V7 种子 17 码零含此码、ROLE_ADMIN 未授权 → `/me` 恒不含 → 按钮恒隐藏（与 module:toggle-status 缺码同型，043 已修先例） |
| **抽屉无回显** | `RoleAssignDrawer.vue:141` watch 打开时 `assignedRoleIds.value = []` 硬编码，无按 userId 拉已分配；配合 PUT 全量替换语义，提交会**清空**已有角色（如 admin→ROLE_ADMIN 被清掉） |
| **消费源已就位** | 046 交付 `UserVO.roles`（`api/user.ts:17-18` getById 已返回）——抽屉只需调 `userApi.getById(userId)` 读 `data.roles` |

### 本单定位（放行闭环，非暗修）

- **修什么**：① `V8` 迁移补 `user:assign-role`（挂 user_mgmt 模块，镜像 V7 形态）+ROLE_ADMIN 幂等授权+两处计数断言 17→18；② `RoleAssignDrawer` 打开时按 userId 回显已分配角色（消费 046 数据源）+Vitest spec。
- **不修什么**：不动 `IndexView.vue`（门禁逻辑已有，码到位即放行）；不动 `api/user.ts`/`UserController`（046 已通）；不动 V1-V7 与既有 17 码；不动 `api/role.ts`（归 049）。**用户侧变化**：角色分配按钮对 admin 可见、抽屉打开能看到已分配、提交链路 046 已通——**用户-角色绑定操作端到端可完成**。

## 需求与架构对照

- §6.2 用户获得权限唯一路径=角色 → 绑定可操作性是该语义的核心支撑（报障主诉）→ **强化非冲突**。
- §3 V8 新增迁移文件零实体表变更；权限码=字典数据。
- §4 计数断言随种子演进更新（V7 16→17 先例；本次 17→18）。
- §6 所有管理接口需要认证+授权判定以数据库权限码为准 → 本单补齐缺失权限码，**恰是规范要求的收口**。
- 无架构冲突，无需仲裁。

## 验收标准（TDD 驱动）

- [ ] AC1 — 种子迁移与计数：`V8__seed_user_assign_role_permission.sql` 存在且含 `user:assign-role`、归属 `user_mgmt`、`ROLE_ADMIN` 幂等授权（`INSERT IGNORE`）形态；`SeedDataIntegrationTest` 计数断言更新为 `sys_permission` **18** 行且 `ROLE_ADMIN` 关联 **18** 权限——修复前断言 17 → RED（`expected: <18> but was: <17>`）。迁移应用后该方法内计数断言全过（方法整体仍因预存 22≠2 失败属 veto-6 基线豁免，GREEN 判定=fail 信息收敛至仅 `sys_role 2 行 ==> expected: <2> but was: <22>`）。← 用例 `SeedDataIntegrationTest#核心实体数据存在且字段正确`（断言更新）
- [ ] AC2 — me 放行面：`AuthControllerTest#meReturnsUserRolesPermissions` 断言更新为 admin 权限集合 **18** 个且 `contains("user:assign-role")`——修复前 17 → RED；GREEN 后该测试全绿（me 端点真实返回新码=按钮门禁 `hasPermission('user:assign-role')` 的放行数据源）。← 用例 `AuthControllerTest#meReturnsUserRolesPermissions`
- [ ] AC3 — 抽屉回显：`RoleAssignDrawer.spec.ts` 断言打开（`visible=true`+`userId=1`）后调用 `userApi.getById(1)` 且 `assignedRoleIds` 初始化为返回角色的 id 列表（如 mock `roles=[{id:2,...}]` → `[2]`）——修复前 watch 硬编码 `[]` 不调 getById → RED。← 用例 `RoleAssignDrawer.spec.ts#打开抽屉时按userId回显已分配角色`
- [ ] AC4 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-047` 用例（G1 grep V8 迁移 `user:assign-role`+`user_mgmt` 形态、G2 grep `RoleAssignDrawer.vue` 存在 `userApi.getById` 回显形态、G3 定向跑 `AuthControllerTest#meReturnsUserRolesPermissions`、G4 定向跑 `RoleAssignDrawer.spec.ts` vitest），实现前 G1-G3 RED；冒烟总数 73→74、❌ 恰 4（model-008/010、roles-001/002）零新增；门禁五线基线对照零新增（mvn 168 用例数不变=计数断言修改不加方法、前端 test 文件 39→40 且新增 spec 通过）。← 用例 `AC4 ← 用例 门禁五线 + 冒烟全量`

### AC1 — V8 迁移与计数断言

**步骤与期望**：Generator 先改 `SeedDataIntegrationTest` 两处断言 17→18（:111 permCount、:145 adminRolePermCount）+注释同步 → 定向跑 RED（`expected: <18> but was: <17>`）；实现=V8 迁移（`INSERT IGNORE` 权限+授权，幂等可重复）；迁移在测试库应用后断言过（方法整体仍 22≠2 基线红）。

**用例**：`AC1 ← 用例 SeedDataIntegrationTest#核心实体数据存在且字段正确`

### AC2 — me 端点含新码

**步骤与期望**：先改 `AuthControllerTest#meReturnsUserRolesPermissions`（size 17→18+`contains("user:assign-role")`+注释）→ RED；V8 应用后 me 返回 18 码含新码 → 绿。

**用例**：`AC2 ← 用例 AuthControllerTest#meReturnsUserRolesPermissions`

### AC3 — 抽屉按 userId 回显

**步骤与期望**：新建 `RoleAssignDrawer.spec.ts`（mock `@/api/user` 与 `@/api/role`），mount 设 `userId=1`+`visible=true`，flush 后断言 `userApi.getById` 被调用且已分配区含 mock 角色 id；修复前（watch 仅 `assignedRoleIds=[]`+fetchRoles）getById 未被调 → RED。

**用例**：`AC3 ← 用例 RoleAssignDrawer.spec.ts#打开抽屉时按userId回显已分配角色`

### AC4 — 冒烟 web-047 与门禁总账

**步骤与期望**：smoke 新增 web-047（G1-G4）；实现前 G1/G2/G3 RED。五线亲跑基线对照。

**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

> TDD 载体：计数断言与 me 断言先改先 RED；spec 先于组件改动写出 RED；冒烟用例体首跑 RED。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | SeedDataIntegrationTest#核心实体数据存在且字段正确（断言 17→18） | AC1 | 待 RED→GREEN（方法整体 22≠2 基线红豁免，计数断言须过） |
| 2 | AuthControllerTest#meReturnsUserRolesPermissions（18+contains 新码） | AC2 | 待 RED→GREEN |
| 3 | RoleAssignDrawer.spec.ts#打开抽屉时按userId回显已分配角色 | AC3 | 待 RED→GREEN |
| 4 | smoke web-047 用例体首跑 + 门禁五线 + 冒烟全量 | AC4 | 待 RED EXIT=1→GREEN；冒烟 74=68✅+4❌+2⏭️ |

## RED 证据

> Generator 于任何修改动作前执行测试清单并粘贴实证。

```text
[RED] AC1 SeedDataIntegrationTest#test_关联数据正确:
  ROLE_ADMIN 应关联全部 18 个权限 ==> expected: <18> but was: <17>  （:145，V8 前 17 行）
  （#test_核心实体数据存在且字段正确 预存红于 :99 sys_role 22≠2——veto-6 基线豁免，permCount 18 断言在其后待 GREEN 判定收敛）
[RED] AC2 AuthControllerTest#meReturnsUserRolesPermissions:
  admin 权限集合应含 user:assign-role ==> expected: <true> but was: <false>  （:221 contains，V8 前 me 无此码）
[RED] AC3 RoleAssignDrawer.spec.ts#打开抽屉时按userId回显已分配角色:
  expected "spy" to be called with arguments: [ 1 ] / Number of calls: 0  （watch 硬编码 [] 不调 getById）
[RED] AC4 smoke web-047 G1/G2: V8 文件不存在（grep exit=2）
```

## 门禁与冒烟记录

> Generator 亲测（2026-10-13）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 168 用例 4F+2E（五类 veto-6 豁免） | **Tests run: 168, Failures: 4, Errors: 2**——DataSourceConfigBinding×2、RoleControllerTest 400but409、SeedData 核心实体 22≠2、TestLayersSpec/TestUtilsSpec Docker 2E，五类逐条一致 | ✅ 基线 |
| `npm run lint` | EXIT=0，0 errors / 165 warnings | **EXIT=0，0 errors / 166 warnings**（+1=新 spec 文件引入 1 条 warning，0 error 达标） | ✅ |
| `npm run test` | 39 文件 321/321 | **40 文件 322 passed (322)**（+1 spec ✓） | ✅ 基线+1 |
| `npm run build` | ✓（~17.6s） | **✓ built in 30.69s**（机器负载下成功，无新增错误） | ✅ |
| `bash scripts/smoke.sh` | 73=67✅+4❌+2⏭️ → 本单 +1=74，❌恰零新增 | **74 用例=68✅+4❌+2⏭️**——❌恰=model-008/010、roles-001/002，⏭=infra-004/model-006，✅含 `web/047 用户角色放行闭环与回显契约定向测试 ✅` | ✅ 零新增 |

**提交物证**：`5a615c8 test`（4 文件 RED 先行）→ `e23191b feat`（V8+Drawer GREEN）。业务文件 6=清单零夹带；工作区预存 `application.yml.template` D 态未混入。

## 实现说明

**实现要点**：
1. **先改断言留 RED**：SeedDataIntegrationTest 两处 17→18+注释；AuthControllerTest :221 17→18+`contains("user:assign-role")` 断言+:170/:226 注释同步；定向跑留证。
2. **V8 迁移**（镜像 V7 形态）：`INSERT IGNORE INTO sys_permission (module_id, name, code, description)` 子查询挂 `user_mgmt` 模块 + `INSERT IGNORE INTO sys_role_permission` 子查询挂 `ROLE_ADMIN`；注释注明用户报障 web/047 与幂等性。
3. **抽屉回显**：`import { userApi } from '@/api/user'`；新增 `fetchAssignedRoles()`（getById→`assignedRoleIds = data.roles?.map(r=>r.id) ?? []`）；watch visible 分支在 `fetchRoles()` 前后调用（顺序无硬依赖，建议先回显后列表）；`availableRoles` 过滤逻辑已兼容 assigned 在列表中的展示（filteredAssignedRoles 读 assignedRoleIds ∩ availableRoles）——回显后已分配区即渲染，**无需改 computed**。
4. **spec**：`vi.mock('@/api/user')`+`vi.mock('@/api/role')`，mount + `props:{visible:true, userId:1}`，`await flushPromises`，断言 getById 调用与勾选初始值。
5. G3 注意：`AuthControllerTest#meReturnsUserRolesPermissions` 需登录态（测试类内自洽）；G4 vitest 定向 `npx vitest run src/views/users/RoleAssignDrawer.spec.ts`。

**readOnly 遗留（连续两单扣分警示）**：`RoleServiceImpl`/`PermissionService` 查询方法 `readOnly=true` 补标不在本单（文件数 6 顶格，且与放行闭环主题无关）；挂起区已登记，**049 规划时显式列入交付物**（049 预估重核）。

**超限熔断**：实际业务文件 >6、需改 IndexView/api/user.ts/UserController/既有 V1-V7/其它 Spec 计数类、me 测试 GREEN 后仍红 → 停止回报回 Planner。

**不动清单**：`users/IndexView.vue`（门禁已有，:123 canAssignRole 逻辑零改）；`api/user.ts`/`UserController`（046 已通）；`V1-V7` 迁移；`api/role.ts`/`PermissionAssignDrawer`/`roles/*`（归 049/050）；`RoleServiceImpl` readOnly（挂起区，归 049）；冒烟既有用例与 ❌ 四条；web/045/049/050/051 预留；any 165；工作区预存 application 模板变更（用户手工 `a4bf05b` 及未提交 D 态，不混提交）。

## 交付物（6 业务文件）

1. `backend/src/main/resources/db/migration/V8__seed_user_assign_role_permission.sql` — 新迁移（码+授权）
2. `backend/src/test/java/com/authcore/mapper/SeedDataIntegrationTest.java` — 计数 17→18×2+注释
3. `backend/src/test/java/com/authcore/controller/AuthControllerTest.java` — me 18+contains 新码+注释
4. `frontend/src/views/users/RoleAssignDrawer.vue` — +fetchAssignedRoles 回显
5. `frontend/src/views/users/RoleAssignDrawer.spec.ts` — 回显 spec（新）
6. `scripts/smoke.sh` — +web-047 用例
7. 文档回写：task/session/iteration/registry 四件（registry **新建 web/047 行** ⬜→🔄）

### 新增
- `backend/src/main/resources/db/migration/V8__seed_user_assign_role_permission.sql`
- `frontend/src/views/users/RoleAssignDrawer.spec.ts`

### 修改
- `backend/src/test/java/com/authcore/mapper/SeedDataIntegrationTest.java`
- `backend/src/test/java/com/authcore/controller/AuthControllerTest.java`
- `frontend/src/views/users/RoleAssignDrawer.vue`
- `scripts/smoke.sh`

### 删除
（无）

### 变更计数

业务文件 **6 顶格**；后端主代码零改动（纯迁移+测试断言+前端）；IndexView/api/UserController/V1-V7/RoleServiceImpl 零改动。

## 评审意见

（待 Evaluator 评审）
