# Sprint 工作单：sprint-088

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-088 |
| 所属模块 | web |
| 功能点 ID | **web/043**（新注册） |
| 功能点名称 | 修复模块管理页状态切换按钮永不可见——后端种子缺 `module:toggle-status` 权限码 |
| 状态 | **DONE**（sprint-088 评审通过 9.9/10，2026-10-10） |
| 创建时间 | 2026-10-10 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/006 | 权限管理页+模块管理页（按钮宿主） | ✅ sprint-028 |
| model/006 | 种子数据迁移 V6（权限/授权基线 16 权限） | ✅ sprint-009 |
| auth/004 | GET /auth/me 权限聚合（前端门禁数据源） | ✅ sprint-013 |
| modules/001 | 模块 CRUD API（切换=PUT /modules/{id}） | ✅ sprint-022 |
| web/011 | 前端门禁基线（build ✓/test 0 failed/lint 0E） | ✅ sprint-082/083 |
| sprint-087 | 门禁基线（mvn 154=4F+2E、lint 0E\|165W、test 320/320、build ✓、冒烟 68=62✅+4❌+2⏭️） | ✅ PASS 9.7/10 |

## 业务背景

**用户需求（2026-10-10）**：报障链「状态下拉筛选不准确，筛选的同时操作列第二个按钮状态被改变」→ Planner 三页核验实测：用户/角色页筛选=用户确认期望行为（状态列对应状态、按钮=可执行动作，停用行显示启用），**原报障用户裁定收口**；模块页核验发现**真实缺陷**——启用行无状态切换按钮（操作列=「编辑进入查看权限删除」）→ 用户裁定「**开单修复（补权限种子）**」。

### 缺陷根因实录（Planner 只读取证，2026-10-10）

| 项 | 实证 |
|----|------|
| 前端门禁 | `system/IndexView.vue:245` `canToggleModuleStatus = hasPermission('module:toggle-status')`，:193 按钮 `v-if` 该门禁 |
| 后端种子 | `V6__seed_data.sql` 16 权限（user/role/perm/module 各 4：view/create/update/delete），**无 `module:toggle-status`**；`grep -r "module:toggle-status" backend/` 零命中 |
| 授权链 | V6 `sys_role_permission`：ROLE_ADMIN 全量授权 16 权限（`SeedDataIntegrationTest` 断言 16）→ admin 也无此码 → `hasPermission` 恒 false → **按钮永不可见** |
| me 数据源 | `AuthServiceImpl:93-110` role→permission 聚合零改动即自动返回（`AuthControllerTest:219` 断言 admin 权限数==16） |
| 测试盲区 | 前端 spec 280/356/1494 **mock 注入**该权限码 → 单测绿、真实环境不可见 |
| 实测对照 | headless Chromium /system/modules：启用 4 行操作列无 `.module-table__action-status`；筛选「停用」0 行（库中无停用模块） |
| 后端鉴权现状 | `ModuleController` 无 `@PreAuthorize`（模块接口仅认证）→ 按钮可见后点击 PUT 闭环可用，**后端 Java 零改动** |

**期望行为（用户 2026-10-10 确认口径）**：筛选时状态列显示对应状态、按钮=可执行动作（停用行显示「启用」）——模块页修复后须按此验收（点「停用」→ 行翻停用 → 筛「停用」该行在场且按钮显「启用」）。

## 需求与架构对照

- **§6.1 权限必须归属模块**：新权限 `module_id` 指向 `module_mgmt` 模块 ✓；**§6.3 code 全局唯一**：`module:toggle-status` 现库不存在，无冲突 ✓。
- 架构未定义具体权限码清单（§3 六实体/§6 五条硬语义均不涉权限码枚举）→ 种子补权限属**实现层数据补齐**，无架构冲突，无需仲裁登记。
- 语义自洽：模块自身管理权限挂 `module_mgmt` 模块下（与 `user:view` 挂 `user_mgmt` 同构）；若 `module_mgmt` 停用则其下权限无效（§6.5）→ 按钮消失，语义闭环。

## 验收标准（TDD 驱动）

- [x] AC1 — 种子存在：迁移应用后 `sys_permission` 存在 `code='module:toggle-status'` 行且 `module_id` 指向 `module_mgmt` 模块；V7 迁移重放幂等（`flyway_schema_history` V7 success=1 恰 1 条、权限行恰 1 条）。← 用例 `AC1 ← 用例 SeedDataIntegrationTest#test_模块状态切换权限种子存在且归属module_mgmt`（GREEN 全过 + 真实库留档 V7 success=1、行挂 module_mgmt）
- [x] AC2 — admin 授权与 me 闭环：`ROLE_ADMIN` 经 `sys_role_permission` 关联 `module:toggle-status`，且 admin `GET /api/v1/auth/me` 响应 `permissions` 含 `module:toggle-status`（size 由 16→17）。← 用例 `AC2 ← 用例 AuthControllerTest#admin权限集合含module_toggle_status`（GREEN 全过 + R3 探针 me n=17 含码）
- [x] AC3 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-043` 用例（V7 文件在场 + `module:toggle-status` 插入 + `module_mgmt` 归属 + `ROLE_ADMIN` 授权 + `INSERT IGNORE` 幂等五形态断言，首跑 RED → GREEN）；门禁五线基线对照零新增。← 用例 `AC3 ← 用例 门禁五线 + 冒烟全量`（RED-3 EXIT=1 → 69=63✅ 含 web-043+4❌ 恰基线+2⏭️）
- [x] AC4 — 浏览器端到端：R3 headless Chromium admin 登录 `/system/modules` → 模块行 `.module-table__action-status` 按钮在场（修复前实测 0 在场=RED 实证）；点击「停用」→ `PUT /api/v1/modules/{id}` 成功、状态列翻「停用」；筛「停用」→ 该行在场且按钮显示「启用」（=可执行动作，用户确认口径）。← 用例 `AC4 ← 用例 R3 浏览器探针`（探针 13/13 PASS，见下方 R3 实测）

### AC1 — 种子存在且归属 module_mgmt（幂等）

**前置**：测试上下文 `@SpringBootTest(properties="spring.flyway.enabled=true")` 启动即对真实库 migrate → V7 自动应用。

**步骤与期望**：新增**独立** `@Test` 方法断言：① `SELECT COUNT(*) FROM sys_permission WHERE code='module:toggle-status'` == 1；② 该行 `module_id` 与 `module_mgmt` 模块 id 相等；③ `countFlywaySuccess("7")` == 1。当前无 V7 → count=0 → **RED**；V7 应用后 → GREEN；重放迁移（测试上下文二次启动）行仍 1 → 幂等。

**用例**：`AC1 ← 用例 SeedDataIntegrationTest#test_模块状态切换权限种子存在且归属module_mgmt`

### AC2 — ROLE_ADMIN 授权 + me permissions 含新码

**前置**：AC1 通过（权限行存在），V7 授权段已插入 `sys_role_permission`。

**步骤与期望**：`AuthControllerTest` 既有 admin me 权限断言处：① 新增 `permissions` 包含 `"module:toggle-status"` 断言（当前 16 权限集合无此码 → **RED**）；② 既有 `assertEquals(16, permissions.size(), ...)` 同步为 17（过期口径，见 R2）；③ 种子层佐证断言（`SeedDataIntegrationTest` 新增独立方法）`ROLE_ADMIN ↔ module:toggle-status` 关联 count==1。

**用例**：`AC2 ← 用例 AuthControllerTest#admin权限集合含module_toggle_status`（+ 种子关联独立断言）

### AC3 — 冒烟 web-043 与门禁总账

**步骤与期望**：smoke 新增 web-043：G1 V7 文件在场（`db/migration/V7__*.sql`）；G2 grep `module:toggle-status` 命中；G3 grep 归属形态（`module_mgmt`）；G4 grep `ROLE_ADMIN` 授权段；G5 grep `INSERT IGNORE` 幂等形态——文件缺失时首跑全 RED；V7 落地 → GREEN。五线亲跑基线对照（口径见前置依赖表）。

**用例**：`AC3 ← 用例 门禁五线 + 冒烟全量`

### AC4 — 浏览器端到端（R3）

**步骤与期望**：重启后端使 V7 应用真实库 → headless Chromium：admin 重新登录（刷新 authStore 权限）→ `/system/modules`：① `.module-table__action-status` 在场（修复前 0 在场）；② 点「停用」→ 状态列翻「停用」（PUT 200）；③ 工具栏筛「停用」→ 该行在场、按钮文本「启用」、状态列「停用」。全程记录 API 请求（仅该次 PUT + 筛选 GET）。

**用例**：`AC4 ← 用例 R3 浏览器探针`

## 测试清单

> TDD 载体：AC1/AC2 后端断言**先于 V7 迁移写出**（当前无权限行/无授权 = 天然 RED）；AC3 冒烟用例体首跑 RED；AC4 修复前 0 按钮实证已在案。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | SeedDataIntegrationTest 新增独立方法：toggle 权限种子存在+归属+V7 幂等 | AC1 | 待 RED（count=0）→ GREEN |
| 2 | AuthControllerTest：me permissions 含 module:toggle-status + size 16→17 同步 | AC2 | 待 RED（集合无此码）→ GREEN |
| 3 | SeedDataIntegrationTest 新增独立方法：ROLE_ADMIN 关联 count==1 | AC2 | 待 RED（关联不存在）→ GREEN |
| 4 | smoke web-043 五形态断言首跑 | AC3 | 待 RED（V7 文件缺失全拒绝）→ GREEN |
| 5 | 门禁五线 + 冒烟全量（69、❌恰4、零新增）+ R3 探针 | AC3/AC4 | 五线=基线、R3 三动作 PASS |

## RED 证据

> Generator 于任何修改动作前执行测试清单并粘贴实证（AC1/AC2 spec 首跑、AC3 冒烟首跑、AC4 修复前 0 按钮探针输出）。

```text
[RED-1] AC1/AC2 种子断言首跑（V7 缺失，2026-10-10 mvn -q test -Dtest='SeedDataIntegrationTest,AuthControllerTest'）
 Tests run: 22, Failures: 5, Errors: 0
 [RED-AC1] SeedDataIntegrationTest.test_模块状态切换权限种子存在且归属module_mgmt:173
   module:toggle-status 权限应恰存在 1 行 ==> expected: <1> but was: <0>
   （该方法内 countFlywaySuccess("7") 与归属断言因首断言先抛未执行，V7 落地后同批验证）
 [RED-AC2-种子] SeedDataIntegrationTest.test_ROLE_ADMIN已关联模块状态切换权限:205
   ROLE_ADMIN 应关联 module:toggle-status 恰 1 条 ==> expected: <1> but was: <0>
 [RED-口径] SeedDataIntegrationTest.test_关联数据正确:145
   ROLE_ADMIN 应关联全部 17 个权限 ==> expected: <17> but was: <16>（过期口径同步后、V7 未落地）
 既有 test_核心实体数据存在且字段正确:99 sys_role 22≠2 = 基线预存（真实库污染，veto-6 豁免）
 （permCount 16→17 断言位于同方法 :99 之后，被 22≠2 先抛掩盖——R2 预判的独立方法隔离生效）
```

```text
[RED-2] AC2 me 断言首跑（V7 缺失，同批运行）
 [RED-AC2-me] AuthControllerTest.meReturnsUserRolesPermissions:219
   admin 权限集合应含 module:toggle-status（web/043 模块页状态切换按钮门禁数据源） ==> expected: <true> but was: <false>
 （AuthControllerTest 其余用例全绿）
```

```text
[RED-3] 冒烟 web-043 用例体首跑（V7 文件缺失，2026-10-10）
 G1 ls V7__*.sql 拒绝（文件不在场）→ 用例体 EXIT=1
 （G2-G5 随 G1 短路未执行，V7 落地后全量冒烟逐条验证）
```

```text
[RED-4] AC4 修复前浏览器实证（已在案，Planner 2026-10-10）
status-filter-3pages2.mjs 模块管理页 [启用] act="编辑进入查看权限删除"
→ .module-table__action-status 在场数 = 0（FAIL 启用: 缺停用动作按钮）
```

```text
[GREEN] 定向复跑（2026-10-10，V7 落地后 mvn -q test -Dtest='SeedDataIntegrationTest,AuthControllerTest'）
 Tests run: 22, Failures: 1 — 仅基线 test_核心实体:99 sys_role 22≠2（真实库污染，veto-6 豁免）
 RED 四条全部转绿：AC1 种子方法（行=1、归属 module_mgmt、V7 success=1、重放幂等）、
 AC2 ROLE_ADMIN 关联=1、test_关联数据正确 17=17、AuthControllerTest me 集合含 module:toggle-status
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-10 sprint-087 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（五类，veto-6 豁免） | 156 跑 4F+2E：DataSourceConfigBinding×2、RoleController 409、SeedData 22≠2、TestLayers+TestUtils Docker 2E——五类逐条一致（154+本单新增 2 方法） | ✅ 零新增失败 |
| `npm run lint` | EXIT=0，0 errors / 165 warnings | EXIT=0，0 errors / 165 warnings | ✅ =基线（前端零改动） |
| `npm run test` | 0 failed \| 320 passed（前端零改动应不变） | 38 文件 320/320 passed | ✅ =基线 |
| `npm run build` | ✓（~18s） | ✓ built in 17.42s | ✅ =基线 |
| `bash scripts/smoke.sh` | 68=62✅+4❌+2⏭️ → 本单 +1=69，❌恰4 零新增 | 69=63✅+4❌+2⏭️：✅ 含 web-043；❌恰=model-008/010、roles-001/002（基线四条零新增）；⏭️=infra-004、model-006 | ✅ 增量 +1=63✅ |

### AC4/R3 浏览器实测（2026-10-10，headless Chromium + vite:3003 → 后端 9998 已重启含 V7，探针 `/tmp/opencode/ac4-web043.mjs`）

- 重新登录 admin → me permissions **n=17 含 `module:toggle-status`**（authStore 链路）
- `/system/modules` 启用列表 `.module-table__action-status` 在场 **4**（修复前 RED-4=0）
- 点首行「用户管理」停用 → 状态列翻**停用**、按钮显**启用**、不误显停用
- 筛「停用」→ 列表非空且全停用、每行按钮=启用、无行误显停用
- 点回启用还原 → 全部筛选下「用户管理」恢复启用、按钮恢复停用（零残留）
- 真实库留档：`flyway_schema_history` V7 success=1（2026-10-10 17:59:52 首次应用）、`sys_permission` 行挂 `module_mgmt`、`sys_role_permission` ROLE_ADMIN 关联在案
- 探针结果：**13 通过 0 失败**（PASS 全录）

## 拆分说明

预估验收标准 **4 条（=上限）**；预估文件变更 **4 业务**（V7 SQL 新增 1 + SeedDataIntegrationTest 1 + AuthControllerTest 1 + smoke.sh 1；文档回写 4 不计入）——**≤6 文件不拆分**。

**研究项（Generator 先于动手核实）**：

- **R1 V7 迁移形态与 checksum 红线**：**禁止修改 V1-V6 任何迁移**（真实库 `flyway_schema_history` 已有 V6 记录，`SeedDataIntegrationTest` `countFlywaySuccess("6")==1` 在案，改 V6 = validate/checksum 失败）。V7 两条语句复制 V6 同构形态：① `INSERT IGNORE INTO sys_permission (module_id, name, code, description) SELECT id, '模块状态切换', 'module:toggle-status', <描述> FROM sys_module WHERE code='module_mgmt';`② `INSERT IGNORE INTO sys_role_permission (role_id, permission_id) SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.code='ROLE_ADMIN' AND p.code='module:toggle-status';`；文件名 `V7__seed_module_toggle_permission.sql`；幂等由 INSERT IGNORE + 唯一键兜底。V6 注释「14 个权限」与实数 16 的过时注释**不改**（checksum 红线），登记即可。
- **R2 过期口径同步三处（先登记后改，web-013/026 先例）**：① `SeedDataIntegrationTest` `assertEquals(16, permCount)` → 17；② 同文件 `assertEquals(16, adminRolePermCount)` → 17；③ `AuthControllerTest:219` `assertEquals(16, permissions.size())` → 17。**新断言必须用独立 @Test 方法**——`test_种子数据正确` 内 `sys_role==2` 在真实库因污染 22≠2 先抛（基线 4F 之一），同行后续断言不执行，混写会掩盖 RED/GREEN。附带 grep 全测试树其余权限计数断言（`16 个权限`/`permCount`/`== 16`）一并同步；真实库 22≠2 属基线豁免（veto-6），GREEN 判定以独立新方法与定向断言消息为准。
- **R3 后端 Java 零改动红线与权限粒度口径**：`ModuleController` 无 `@PreAuthorize`（仅认证），切换走既有 `moduleApi.update` 不受新权限影响 → 后端 Java 零改动；「前端细门禁（module:toggle-status）+ 后端模块接口仅认证」粒度口径本单登记不动。**若调研发现后端存在对模块接口的权限拦截且新权限未覆盖 → 停止回报**。
- **R4 me 聚合链路与 AC2 载体**：`AuthServiceImpl:93-110` role→permission 聚合零改动，V7 授权后 admin me 自动含新码；AC2 载体已定 `AuthControllerTest:213-219`（既有 16 断言处扩展）；种子关联佐证放 SeedData 独立方法。若 AuthControllerTest 需 mock/真实库依赖差异导致无法测 me → 降级方案：AC2 种子关联断言为主、me 由 AC4 R3 登录后按钮在场端到端覆盖（降级须先在 task 登记再改 AC）。
- **R5 服务重启与真实库应用时机**：R3 前重启后端（当前 9998 进程无 V7）；测试上下文 `flyway.enabled=true` 启动即迁移；生产启动确认 FLYWAY_ENABLED 配置化开关（sprint-002 口径）；前端权限来自登录/me → R3 必须**重新登录**（或 refreshAuth）使 authStore 拿到新码；验证后真实库 `flyway_schema_history` V7 记录 + 权限行留档。
- **R6 前端零改动与既有 spec 相容**：`canToggleModuleStatus`（IndexView.vue:245）已就位；spec 280/356/1494 mock 注入该码既有绿 → **前端业务代码零改动，test 320/320 不变**；「按钮在场」前端单测已有覆盖（mock 权限注入即绿），真实链路由 AC4 承担；若发现既有 spec 依赖「16 权限」类假数据断言 → 登记同步。
- **R7 冒烟计数基线**：当前 smoke 68 用例（❌恰=model-008/010、roles-001/002）；`smoke_case` grep 计数含定义行，以脚本尾部 `CASES` 输出为准；本单 +1 → 69。

**超限熔断**：实际业务文件 >6、改动 V1-V6 任一迁移、需动后端 Java（.java）、需动前端业务代码（.vue/.ts）、修复引发既有**非过期**断言连锁失败 → **停止回报，回 Planner 重切分**。

**不动清单**：V1-V6 迁移文件（含 V6 过时注释）；前端业务代码（`system/IndexView.vue` 及全站 .vue/.ts）；BaseSelect.vue；`ModuleController` 等后端 Java；冒烟既有用例断言语义与 ❌ 四条（model-008/010、roles-001/002）不修改不删除；web/036c2/036c3、web/011d 预留；any 165；**工作树预存改动 `backend/src/main/resources/application.yml.template` 删除 + `application.template.yml` 新增（非本单产生，不混入本单提交，提交时按范围精确 stage）**。

## 交付物（≈4 业务文件）

1. `backend/src/main/resources/db/migration/V7__seed_module_toggle_permission.sql`：新增（权限行 + ROLE_ADMIN 授权，INSERT IGNORE 幂等）
2. `backend/src/test/java/com/authcore/mapper/SeedDataIntegrationTest.java`：新增 AC1/AC2 种子独立断言 2 方法 + 16→17 两处同步
3. `backend/src/test/java/com/authcore/controller/AuthControllerTest.java`：新增 AC2 me 含新码断言 + 16→17 同步
4. `scripts/smoke.sh`：新增冒烟用例 web-043（AC3）
5. 文档回写：task/session/iteration/registry 四件

## 实现说明

**实现要点**：后端仅一条新迁移文件补齐权限种子——`V7__seed_module_toggle_permission.sql` 两段 INSERT IGNORE（权限挂 `module_mgmt` + `ROLE_ADMIN` 授权），测试侧三处 16→17 过期口径同步 + 三组新断言（种子行/归属/幂等、授权关联、me 集合含码），冒烟五形态断言锁修复形态。前端与后端 Java 零改动；按钮显隐由既有 `canToggleModuleStatus` 门禁自动恢复。

### 新增
- `backend/src/main/resources/db/migration/V7__seed_module_toggle_permission.sql` — 权限 `module:toggle-status`（name=模块状态切换，module_id=module_mgmt）+ ROLE_ADMIN 授权

### 修改
- `backend/src/test/java/com/authcore/mapper/SeedDataIntegrationTest.java` — 新增独立方法：AC1 种子存在+归属+V7 幂等、AC2 ROLE_ADMIN 关联；`permCount` 与 `adminRolePermCount` 16→17 同步
- `backend/src/test/java/com/authcore/controller/AuthControllerTest.java` — 新增 me permissions 含 `module:toggle-status` 断言；size 16→17 同步
- `scripts/smoke.sh` — 追加冒烟用例 web-043（五形态断言）

### 删除
（无）
### 变更计数

业务文件 4（V7 SQL + 两测试类 + smoke.sh）+ 文档回写 4（task/session/iteration/registry），**≤6 达标**；后端 Java 主代码零改动、前端全部零改动、V1-V6 零改动。

## 评审意见

**sprint-088 Evaluator 评审通过（2026-10-10，平均分 9.9/10；五维 10/9.5/10/10/10；六否决零命中）**

- 4 AC 亲核全满足：AC1 种子存在+module_mgmt 归属+V7 幂等（真实库 flyway success=1、uk 唯一键 V4:13/V5:20 亲核）；AC2 ROLE_ADMIN 关联+me 含码（探针 n=17）；AC3 冒烟 +1 用例 web-043 五形态 ✅ 与五线=基线；AC4 探针 13/13（按钮在场 4、停用翻转、筛停用显启用、还原零残留）
- RED 独立复现（Evaluator 自建 worktree@62ca041 + 独立空库 authcore_eval043）：22 跑 **5 failed**（me contains=false、关联 0、17vs16×2、count=0）四业务红+口径红全复现；RED-3 用例体 V7 缺失 EXIT=1；复现后空库/worktree 已清理
- 门禁亲跑：mvn 156=4F+2E 基线五类逐条；lint 0E|165W；test 320/320；build ✓17.78s；冒烟亲跑 69=63✅+4❌恰基线+2⏭️（web-043 ✅、增量 +1 smoke_case 取证）
- 变更范围：`2bbafa7..HEAD` 8 文件=清单零夹带；V1-V6、后端 Java、前端全零改动；预存 application 模板两文件未混入
- 改进建议（不计分）：① Planner 后续工作单 AC 用例引用应写真实方法名（本单 AC2 占位名与 `meReturnsUserRolesPermissions` 不符，实现说明已如实记载）；② 模块接口后端鉴权粒度如需收紧另立工作单
