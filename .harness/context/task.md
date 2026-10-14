# Sprint 工作单：sprint-096

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-096 |
| 所属模块 | web |
| 功能点 ID | **web/050**（角色-权限绑定·门禁收口；三绑定拆分总盘后续③，sprint-090 登记，本次注册立项） |
| 功能点名称 | 角色-权限绑定·按钮门禁放行（V9 补 `role:assign-permission` 种子+计数 19+IndexView 门禁 spec） |
| 状态 | **PLANNED** |
| 创建时间 | 2026-10-14 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/047 | V8 计数基线 18+user:assign-role 同构先例（V9 镜像 V8） | ✅ sprint-094 |
| web/048a/048b/049 | 角色-权限数据源+回显+提交契约三断点已闭合 | ✅ |
| roles/001 | 后端 PUT /roles/{id}/permissions 契约 | ✅ |
| perms/001 | sys_permission 计数断言基线（18→19 联动） | ✅ |

## 业务背景

**用户报障「角色 权限分配接口报错」收尾断点**（048a/048b/049 已修数据源/回显/提交 405；本单=按钮门禁放行闭环收尾）：

### 断点实锤

| 断点 | 实证 |
|------|------|
| **按钮永不可见** | `roles/IndexView.vue:120` `canAssignPermission = authStore.hasPermission('role:assign-permission')`；V6 role_mgmt 仅 4 码（view/create/update/delete）+V7/V8 补码均不在此模块 → `/me` 恒不含 `role:assign-permission` → 分配按钮永隐藏（与 043 module:toggle-status、047 user:assign-role 完全同型） |
| **消费侧已就位** | `:249` canAssignPermission 已进 defineExpose/模板门禁、PermissionAssignDrawer 回显（048b）+提交（049）已通——**零界面改动**，码到位即放行 |
| **spec 载体已备** | `IndexView.spec.ts` 有 `createWrapper(permissions)` mock 模式（:1163 canCreate 断言先例）——扩 canAssignPermission 断言即 RED |

### 本单定位

- **修什么**：① `V9` 迁移补 `role:assign-permission`（挂 role_mgmt，镜像 V8/V7 形态）+ROLE_ADMIN 幂等授权；② 计数断言 18→19×2（SeedData）+me 18→19+contains（AuthControllerTest）；③ `IndexView.spec.ts` 扩 canAssignPermission 有码/无码断言；④ smoke web-050。
- **不修什么**：不动 IndexView.vue/Drawer（门禁与消费逻辑已有）、不动后端业务代码（纯种子迁移）、不动 V1-V8、不动其它 spec。**用户侧变化**：角色-权限分配按钮对 admin 可见——报障四断点（048a/048b/049/050）**全部闭合**，分配链路端到端可完成。

## 需求与架构对照

- §6 绑定操作需授权判定 → 补齐缺失权限码=规范收口（043/047 同型先例两单）。
- §3 V9 新迁移零表结构变更；§4 计数断言随种子演进（V7 16→17、V8 17→18 先例，本次 18→19）。
- 无架构冲突，无需仲裁。

## 验收标准（TDD 驱动）

- [ ] AC1 — V9 迁移与计数：`V9__seed_role_assign_permission_permission.sql` 存在且含 `role:assign-permission`、挂 `role_mgmt`、`ROLE_ADMIN` `INSERT IGNORE` 幂等；`SeedDataIntegrationTest` 计数 18→19×2——修复前 18 → RED（`expected: <19> but was: <18>`）。方法整体仍 22≠2 基线红豁免（GREEN 判定=fail 收敛至仅该条，同 047 先例）。← 用例 `SeedDataIntegrationTest#test_关联数据正确+test_核心实体数据存在且字段正确`（断言更新）
- [ ] AC2 — me 放行面：`AuthControllerTest#meReturnsUserRolesPermissions` 断言 19+`contains("role:assign-permission")`——修复前 → RED；GREEN 后 me 真实返回新码（按钮门禁数据源）。← 用例 `AuthControllerTest#meReturnsUserRolesPermissions`
- [ ] AC3 — 门禁 spec：`IndexView.spec.ts` 扩断言——含 `role:assign-permission` 的权限集合下 `canAssignPermission===true`，不含时 `false`——修复前无该断言 → RED（新用例失败/不存在）。← 用例 `IndexView.spec.ts#canAssignPermission 受 role:assign-permission 门禁控制`
- [ ] AC4 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-050`（G1 grep V9 形态、G2 grep IndexView.vue 门禁码在场护栏、G3 定向 mvn `AuthControllerTest#meReturnsUserRolesPermissions`、G4 定向 vitest `IndexView.spec.ts` canAssignPermission 用例），实现前 G1/G3/G4 RED；冒烟 75→76、❌ 恰 4 零新增；五线基线（mvn 168 不变、前端 test 41 文件 323+1=324）。← 用例 `AC4 ← 用例 门禁五线 + 冒烟全量`

### AC1 — V9 与计数断言

**步骤与期望**：先改 SeedData 两处 18→19+注释 → RED；实现 V9（镜像 V8：INSERT IGNORE 双段）；迁移应用后计数断言过（方法整体仍 22≠2）。

**用例**：`AC1 ← 用例 SeedDataIntegrationTest（断言更新）`

### AC2 — me 含新码

**步骤与期望**：先改 AuthControllerTest me（19+contains）→ RED；V9 应用后绿。

**用例**：`AC2 ← 用例 AuthControllerTest#meReturnsUserRolesPermissions`

### AC3 — canAssignPermission 门禁 spec

**步骤与期望**：扩 `IndexView.spec.ts`（createWrapper 先例模式）：`createWrapper(['role:view', ..., 'role:assign-permission'])` → `wrapper.vm.canAssignPermission===true`；不含码 → false。

**用例**：`AC3 ← 用例 IndexView.spec.ts#canAssignPermission 受 role:assign-permission 门禁控制`

### AC4 — 冒烟 web-050 与门禁总账

**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | SeedDataIntegrationTest（18→19×2） | AC1 | 待 RED→GREEN（整体 22≠2 豁免，收敛判定） |
| 2 | AuthControllerTest#meReturnsUserRolesPermissions（19+contains） | AC2 | 待 RED→GREEN |
| 3 | IndexView.spec.ts#canAssignPermission 门禁 | AC3 | 待 RED→GREEN |
| 4 | smoke web-050 + 门禁五线 + 冒烟全量 | AC4 | 待 RED→GREEN；冒烟 76=70✅+4❌+2⏭ |

## RED 证据

```text
[RED] 证据槽 — 待 Generator 填写
```

## 门禁与冒烟记录

> Generator 亲测填写。基线口径（2026-10-14 sprint-095 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 168 用例 4F+2E（五类 veto-6 豁免） | 待填（预期 168 不变） | 待验 |
| `npm run lint` | EXIT=0，0 errors / 166 warnings | 待填 | 待验 |
| `npm run test` | 41 文件 323/323 | 待填（扩用例不加文件→预期仍 41 文件、323+N） | 待验 |
| `npm run build` | ✓（17.7s） | 待填 | 待验 |
| `bash scripts/smoke.sh` | 75=69✅+4❌+2⏭️ → 本单 +1=76 | 待填（预期 76=70✅+4❌+2⏭️ 含 web-050） | 待验 |

## 实现说明

**实现要点**：
1. 先改 SeedData（:111/:145 18→19+注释）+AuthControllerTest me（:221 18→19+contains role:assign-permission+注释）→ 定向 RED；再扩 IndexView.spec.ts（canAssignPermission true/false 两断言或一用例两断言，createWrapper 模式）→ vitest RED。
2. V9 迁移镜像 V8：`INSERT IGNORE INTO sys_permission ... FROM sys_module WHERE code = 'role_mgmt'` + `INSERT IGNORE INTO sys_role_permission`（ROLE_ADMIN）；文件名 `V9__seed_role_assign_permission_permission.sql`。
3. smoke web-050：G1 grep V9（`role:assign-permission`+`role_mgmt`+`INSERT IGNORE`）、G2 grep IndexView.vue `role:assign-permission`（消费点护栏）、G3 mvn me 定向 exit 码、G4 vitest IndexView.spec 定向（grep 通过数）。
4. spec 扩用例注意：createWrapper(permissions) mock 已有 canCreate/canDelete 断言先例（:1163-1176）——同文件追加 canAssignPermission 用例。

**超限熔断**：业务文件 >6、需改 IndexView.vue/Drawer/后端业务逻辑/其它 spec、计数类外测试受影响 → 停止回报。

**不动清单**：`roles/IndexView.vue`、`PermissionAssignDrawer.vue`（消费侧已就位）；后端业务代码；V1-V8；`ModuleServiceImpl`/readOnly（049 已收口）；冒烟既有用例与 ❌ 四条；web/051/045/036c 预留；any 166；工作区预存 `application.yml.template` D 态。

## 交付物（5 业务文件）

1. `backend/src/main/resources/db/migration/V9__seed_role_assign_permission_permission.sql` — 新迁移
2. `backend/src/test/java/com/authcore/mapper/SeedDataIntegrationTest.java` — 计数 18→19×2+注释
3. `backend/src/test/java/com/authcore/controller/AuthControllerTest.java` — me 19+contains+注释
4. `frontend/src/views/roles/IndexView.spec.ts` — canAssignPermission 门禁断言
5. `scripts/smoke.sh` — +web-050 用例
6. 文档回写：task/session/iteration/registry 四件（registry **新建 web/050 行** ⬜→🔄）

### 新增
- `backend/src/main/resources/db/migration/V9__seed_role_assign_permission_permission.sql`

### 修改
- `backend/src/test/java/com/authcore/mapper/SeedDataIntegrationTest.java`
- `backend/src/test/java/com/authcore/controller/AuthControllerTest.java`
- `frontend/src/views/roles/IndexView.spec.ts`
- `scripts/smoke.sh`

### 删除
（无）

### 变更计数

业务文件 **5**；后端主代码零改动（纯迁移+测试）；IndexView.vue/Drawer/V1-V8 零改动。

## 评审意见

（待 Evaluator 评审）
