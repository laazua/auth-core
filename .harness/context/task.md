# Sprint 工作单：sprint-095

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-095 |
| 所属模块 | web |
| 功能点 ID | **web/049**（角色-权限绑定·提交契约与 readOnly 收口；三绑定拆分总盘后续②，sprint-090 登记，本次注册立项） |
| 功能点名称 | 角色-权限绑定·前端提交契约 PUT 对齐与 Service readOnly 补标（api/role.ts POST→PUT + 两 ServiceImpl 查询方法 @Transactional(readOnly=true)） |
| 状态 | **AWAITING_REVIEW**（Generator 交付完成 2026-10-14，RED 证据+门禁亲测在案） |
| 创建时间 | 2026-10-13 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/048a | GET /permissions/tree 数据源（048a ✅ sprint-092 PASS 8.4/10） | ✅ |
| web/048b | RoleVO.permissions 回显+批量填充（048b ✅ sprint-093 PASS 8.5/10） | ✅ |
| roles/001 | 后端 PUT /roles/{id}/permissions 契约（RoleControllerTest 已有） | ✅ |
| web/044 | 角色列表查询（IndexView 提交链路 :223 已就位） | ✅ sprint-089 |
| web/047 | V8 种子与计数基线（18 权限，本单零动） | ✅ sprint-094 |

## 业务背景

**用户报障「角色 权限分配接口报错」剩余断点**（sprint-090 排查实录+048 拆分登记）：048a/048b 已修数据源与回显，**提交链路仍 405**：

### 断点实锤

| 断点 | 实证 |
|------|------|
| **前端方法 405** | `api/role.ts:32` `assignPermissions` 发 **POST** `/roles/{id}/permissions`；后端 `RoleController` 契约=**PUT**（048 基线绿）→ Method Not Allowed（与 046 users 侧 `api/user.ts` POST→PUT 修复同型——046 先例：一行修复+spec+冒烟） |
| **消费侧已就位** | `roles/IndexView.vue:223` 已调 `roleApi.assignPermissions(...)`、`PermissionAssignDrawer.vue:167` handleSubmit emit 已就位——**零界面改动**，api 一行修正即端到端通 |
| **readOnly 挂起** | §3 硬规则「查询方法 @Transactional(readOnly=true)」——048a/048b 连续两单扣分，047 评审升格为「**049 规划时显式列入交付物**」（session 挂起区+048b 评审改进建议①在案）；违反清单：`RoleServiceImpl` 查询方法（类级 @Transactional 默认读写）、`PermissionServiceImpl` 同款 |

### 本单定位

- **修什么**：① `api/role.ts:32` `http.post`→`http.put`+spec（RED 先行）；② `RoleServiceImpl`/`PermissionServiceImpl` 公有查询方法补 `@Transactional(readOnly=true)`；③ 冒烟 web-049 护栏（前端契约形态+后端 PUT 契约定向+readOnly 形态 grep）。
- **不修什么**：不动 Drawer/IndexView（消费侧已就位）、不动后端 Controller/Service 业务逻辑（PUT 契约已通、readOnly=纯注解零行为变更）、不动 V1-V8、不动 `api/user.ts`（046 已修）、不动其它 ServiceImpl（ModuleServiceImpl:212 已有先例不重标）。**用户侧变化**：角色-权限分配提交不再 405，报障「角色 权限分配接口报错」剩余断点闭合。

## 需求与架构对照

- §3 硬规则查询方法 readOnly → 补标=规范收口（ModuleServiceImpl:212 先例在场）。
- §6 角色-权限绑定=核心业务语义 → 提交链路修复=可用性收口。
- 046 同型修复先例（api/user.ts POST→PUT 一行+spec+冒烟，sprint-090 PASS 9.6/10）。
- 无架构冲突，无需仲裁。

## 验收标准（TDD 驱动）

- [ ] AC1 — 前端提交契约：新建 `api/role.spec.ts`（参照 `user.spec.ts` 同构：vi.mock http）断言 `roleApi.assignPermissions(1, {permissionIds:[2]})` 调 `http.put('/roles/1/permissions', ...)` 且 `http.post` 未被调——修复前 `http.post` → RED（put 0 调用）。← 用例 `role.spec.ts#assignPermissions 使用 PUT 方法`
- [ ] AC2 — readOnly 补标：`RoleServiceImpl` 公有查询方法（`queryRoles`、`getRoleById`、`listAllPermissions`）与 `PermissionServiceImpl` 公有查询方法（`queryPermissions`、`queryPermissionsGroupedByModule`、`listAllForTree`、`getPermissionById`）均标注 `@Transactional(readOnly = true)`；私有 helper `loadPermissionsByRoleIds` 不标（Spring 代理不拦自调用，由调用方事务语义覆盖）；载体=smoke G 静态形态+Evaluator diff 亲核（注解零行为变更，无红-绿行为差，形态护栏先例=043/047 迁移）。← 用例 `AC2 ← 用例 smoke web-049 G + diff 亲核`
- [ ] AC3 — 后端契约在位护栏：冒烟 G3 定向跑 `RoleControllerTest#assignPermissionsBatchReplace`（048 基线绿方法）——证明前端改 PUT 后端契约消费面已就绪；实现前即绿（护栏非 RED 项，但用例体随本单新增首跑须先 G1/G2 RED）。← 用例 `AC3 ← 用例 smoke web-049 G3`
- [ ] AC4 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-049` 用例（G1 grep api/role.ts 存在 `http.put(\`/roles/${id}/permissions\`` 形态、G2 grep spec `assignPermissions 使用 PUT`、G3 定向 mvn PUT 契约、G4 grep 两 ServiceImpl `readOnly = true` 形态），实现前 G1/G2/G4 RED；冒烟总数 74→75、❌ 恰 4 零新增；门禁五线基线对照（mvn 168 用例不变=纯注解+前端、前端 test 40 文件 322+1=323）。← 用例 `AC4 ← 用例 门禁五线 + 冒烟全量`

### AC1 — api/role.spec.ts 契约

**步骤与期望**：先写 spec（mock http）→ RED（`http.put` not called）；改 `http.post`→`http.put` → GREEN。

**用例**：`AC1 ← 用例 role.spec.ts#assignPermissions 使用 PUT 方法`

### AC2 — readOnly 形态

**步骤与期望**：7 处方法级 `@Transactional(readOnly = true)`（3+4）；smoke G4 grep 两文件含 `readOnly = true`；Evaluator 亲核 diff 范围=恰 7 处注解零外溢。

**用例**：`AC2 ← 用例 smoke web-049 G4 + diff 亲核`

### AC3 — 后端契约护栏

**步骤与期望**：G3 `mvn -Dtest='RoleControllerTest#assignPermissionsBatchReplace'` 绿（证明 PUT 契约在位）。

**用例**：`AC3 ← 用例 smoke web-049 G3`

### AC4 — 冒烟 web-049 与门禁总账

**步骤与期望**：冒烟 75=69✅+4❌+2⏭ 零新增；五线亲跑基线对照。

**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | role.spec.ts#assignPermissions 使用 PUT 方法 | AC1 | 待 RED→GREEN |
| 2 | smoke web-049 G1/G2（api put 形态+spec 形态）| AC4 | 待 RED→GREEN |
| 3 | smoke web-049 G3（后端 PUT 契约定向）| AC3 | 待绿（护栏） |
| 4 | smoke web-049 G4（readOnly 形态 grep）| AC2 | 待 RED→GREEN |
| 5 | 门禁五线 + 冒烟全量 | AC4 | 待亲测 |

## RED 证据

```text
[RED] AC1 role.spec.ts#assignPermissions 使用 PUT 方法:
  expected "spy" to be called with arguments: [ '/roles/1/permissions', …(1) ] / Number of calls: 0  （现 http.post，put 未被调）
[RED] AC4 smoke web-049 G1/G2/G4 组合首跑: EXIT=1（api/role.ts 无 put 形态——G1 fail 即整体红）
```

## 门禁与冒烟记录

> Generator 亲测（2026-10-14）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 168 用例 4F+2E（五类 veto-6 豁免） | **Tests run: 168, Failures: 4, Errors: 2**——DataSourceConfigBinding×2、RoleController:466 400but409、SeedData 22≠2、TestLayers/TestUtils Docker 2E，五类逐条一致（readOnly 纯注解零行为变更实证） | ✅ 基线 |
| `npm run lint` | EXIT=0，0 errors / 166 warnings | **EXIT=0，0 errors / 166 warnings** | ✅ 基线 |
| `npm run test` | 40 文件 322/322 | **41 文件 323 passed (323)**（+1 spec ✓） | ✅ 基线+1 |
| `npm run build` | ✓（17.2s） | **✓ built in 17.75s** | ✅ |
| `bash scripts/smoke.sh` | 74=68✅+4❌+2⏭️ → 本单 +1=75 | **75 用例=69✅+4❌+2⏭️**——❌恰=model-008/010、roles-001/002，⏭=infra-004/model-006，✅含 `web/049 角色权限提交契约PUT对齐与readOnly形态测试 ✅` | ✅ 零新增 |

**提交物证**：`98b5ddc test`（spec+smoke RED 先行）→ `b307b94 feat`（api 一行+7 处 readOnly）。业务文件 5=清单零夹带；工作区预存 `application.yml.template` D 态未混入。

## 实现说明

**实现要点**：
1. 先写 `api/role.spec.ts`（同构 user.spec.ts：vi.mock('./http')，断言 put 被调+post 未调）→ RED；`api/role.ts:32` `http.post`→`http.put` → GREEN。
2. readOnly 补标（7 处，仅查询方法，**勿动**写方法 createRole/updateRole/deleteRole/assignPermissions 与类级注解）：
   - `RoleServiceImpl`：`queryRoles`（:55）、`getRoleById`（:83）、`listAllPermissions`（:255）
   - `PermissionServiceImpl`：`queryPermissions`（:47）、`queryPermissionsGroupedByModule`（:72）、`listAllForTree`（:98）、`getPermissionById`（:109）
   - 写法镜像 `ModuleServiceImpl:212` 先例（`@Transactional(readOnly = true)` 方法级，置于 `@Override` 与方法之间）。
3. smoke web-049 四段 G（G1 grep api/role.ts put 形态含 `${id}` 转义、G2 grep spec 名、G3 mvn 定向（exit 码）、G4 grep 两文件 `readOnly = true`）。
4. RED 顺序：spec 先写跑红 → api 一行改 → readOnly 标注（无行为差，G4 形态首红于 grep 未标前）→ smoke 用例体。

**超限熔断**：实际业务文件 >6、需改 Drawer/IndexView/后端业务逻辑/其它 ServiceImpl、mvn 用例数或五类豁免变化 → 停止回报回 Planner。

**不动清单**：`roles/PermissionAssignDrawer.vue`、`roles/IndexView.vue`（消费侧已就位）；后端 Controller/Service 业务逻辑（readOnly=纯注解）；`ModuleServiceImpl`（已合规）；`api/user.ts`（046 已修）；V1-V8；冒烟既有用例与 ❌ 四条；web/050/051/045 预留；any 166；工作区预存 `application.yml.template` D 态（用户手工，不混提交）。

## 交付物（5 业务文件）

1. `frontend/src/api/role.ts` — assignPermissions POST→PUT（1 行）
2. `frontend/src/api/role.spec.ts` — 契约 spec（新）
3. `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java` — readOnly×3
4. `backend/src/main/java/com/authcore/service/impl/PermissionServiceImpl.java` — readOnly×4
5. `scripts/smoke.sh` — +web-049 用例
6. 文档回写：task/session/iteration/registry 四件（registry **新建 web/049 行** ⬜→🔄）

### 新增
- `frontend/src/api/role.spec.ts`

### 修改
- `frontend/src/api/role.ts`
- `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java`
- `backend/src/main/java/com/authcore/service/impl/PermissionServiceImpl.java`
- `scripts/smoke.sh`

### 删除
（无）

### 变更计数

业务文件 **5**；后端主代码改动=纯注解 7 处零逻辑；Drawer/IndexView/Controller/V1-V8/其它 ServiceImpl 零改动。

## 评审意见

（待 Evaluator 评审）
