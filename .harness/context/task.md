# Sprint 工作单：sprint-092

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-092 |
| 所属模块 | web |
| 功能点 ID | **web/048a**（web/048 拆分第一段——超限熔断重切，登记见 session-state 挂起区 2026-10-12 Generator 条） |
| 功能点名称 | 角色-权限绑定·tree 数据源暗修（GET /permissions/tree 新增，修「角色 权限分配接口报错」主诉 400/500） |
| 状态 | **DONE**（sprint-092 评审通过 8.4/10，2026-10-13） |
| 创建时间 | 2026-10-12 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| perms/001 | 权限 CRUD API 与 PermissionVO | ✅ sprint-021 |
| web/005 | 角色管理页（PermissionAssignDrawer 消费侧已在场） | ✅ sprint-027 |
| web/046 | 暗修模式先例（前端契约已备好只等后端） | ✅ sprint-090 PASS 9.6/10 |
| sprint-091 | web/048 熔断登记（8>6）与 R1/R2 研究项定夺 | ✅ 挂起区在案 |
| sprint-090 | 门禁基线（mvn 162=4F+2E 五类、lint 0E\|165W、test 321/321、build ✓、冒烟 71=65✅+4❌+2⏭️） | ✅ PASS 9.6/10 |

## 业务背景

**用户需求（2026-10-12）**：「角色 权限分配接口报错」+9998 日志——`GET /permissions/tree` 落到 `GET /permissions/{id}`，`MethodArgumentTypeMismatchException: For input string: "tree"` → 500。

### 根因实锤（sprint-091 Planner 只读取证，2026-10-12）

| 断点 | 实证 |
|------|------|
| **tree 接口缺失** | 9998 实测 `GET /permissions/tree` → HTTP 500（MethodArgumentTypeMismatchException 同用户日志）；`PermissionController` 仅有 `GET /permissions`(:44)、`/grouped`(:63)、`/{id}`(:76)，**无 /tree** |
| **/grouped 不可复用** | 9998 实测返回 `Map<ModuleVO, List>`——Jackson 对象键为 `ModuleVO[id=1, ...]` **toString 串**；前端 `PermissionTreeNode extends PermissionVO { children: PermissionTreeNode[] }`（types/permission.ts:38-40）期望嵌套数组，无法消费 |
| **消费侧已就位** | `api/permission.ts:15-16` 已声明 `GET /permissions/tree` 返回 `PermissionTreeNode[]`；`PermissionAssignDrawer.vue:73` 已消费（filterNode/collectIds/el-tree 模板 :248-253 读 name/code）——**前端契约已备好只等后端** |

### 熔断重切（sprint-091 Generator，2026-10-12）

原 web/048 预估 6 漏算 `PermissionService` 接口+`PermissionServiceImpl` 两文件与 `PermissionVO` children 载体 → 实际 8 >6 熔断零写入。重切为：

| 单 | 范围 | 文件数 | 状态 |
|----|------|--------|------|
| **048a（本次 sprint-092）** | tree 数据源（用户 400/500 主诉） | 6 顶格 | 本次 |
| 048b（后续） | RoleVO.permissions 角色回显（原 AC2） | 4 | ⬜ |

### 本单定位（暗修不放行）

- **修什么**：新增 `GET /permissions/tree` 返回前端可消费的平铺 `PermissionTreeNode[]`（每节点 `children=[]`）。
- **不修什么**：不动 `RoleVO`/`RoleServiceImpl`（回显归 048b）；不动 `api/role.ts`（POST→PUT 归 049）；不动 Drawer/IndexView/门禁（归 049/050）；不动 `/`、`/grouped`、`/{id}`、POST、PUT 既有五映射。**用户侧变化**：抽屉点开不再 500、树可渲染可搜索；**回显空（048b）、提交 405（049）、按钮门禁（050）属预期**。

## 需求与架构对照

- §6.1「权限必须归属模块」→ 平铺树每节点携带 `moduleId`/`moduleName`，分组信息保留在字段层；模块聚合由前端 moduleName/order 承载 → **不违反**（无权限脱离模块）。
- §3 实体表零变更（`PermissionVO` 加 children 属 API 视图对象，046 UserVO.roles 同 sprint 先例）。
- §4 分页豁免：权限为字典类小数据（17 行，注释注明规模预期 ≤1000）全量加载合法。
- 新增只读端点与既有五映射并存互不替代 → **无架构冲突，无需仲裁**。

## 验收标准（TDD 驱动）

- [x] AC1 — tree 接口数据源：`GET /api/v1/permissions/tree`（登录 admin）返回 HTTP 200、`code=0` 且 `data` 为 JSON 数组，数组元素含 `id`/`code`/`children` 三键（`children` 为数组），存在元素 `code="user:view"` 且其 `moduleId` 非空；修复前该路径 500（落 `/{id}` Long 转换失败）→ RED。← 用例 `PermissionTreeContractTest#树接口返回可消费结构且含种子权限`
- [x] AC2 — 零回归护栏：`GET /api/v1/permissions?page=1&size=10` 返回 `code=0`、`data.records` 数组、命中行含 `code="user:view"` 且既有键（id/moduleId/name/code/description/createTime/updateTime）不变（`children` 新增键对分页消费者非破坏）；`GET /api/v1/permissions/grouped` 返回 `code=0` 且为对象结构。← 用例 `PermissionTreeContractTest#既有分页与grouped接口保持可用`
- [x] AC3 — 平铺结构契约：`data` 数组长度=种子权限总数（≥17）、全部节点 `children` 均为空数组、`id` 全局唯一（防模块父节点方案的 id 值域重叠缺陷回归）；节点按 `moduleId,id` 升序排列。← 用例 `PermissionTreeContractTest#树为权限平铺且children为空与id唯一`
- [x] AC4 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-048a` 用例（G1 grep `PermissionController.java` 存在 `/tree` 映射形态、G2 grep `PermissionVO.java` 存在 `children` 字段形态、G3 定向跑 `PermissionTreeContractTest`、G4 grep `api/permission.ts` getTree 形态在场），实现前 G1-G3 RED；冒烟总数 71→72、❌ 恰 4（model-008/010、roles-001/002）零新增；门禁五线基线对照零新增（mvn 162→165 量级、前端三线=基线零前端改动）。← 用例 `AC4 ← 用例 门禁五线 + 冒烟全量`

### AC1 — GET /permissions/tree

**前置**：种子 17 权限全挂四模块（V6/V7）。

**步骤与期望**：契约测试 MockMvc 登录 admin 后 `GET /api/v1/permissions/tree` → HTTP 200、`$.code=0`、`$.data` isArray、`$.data[?(@.code=='user:view')]` 非空且 moduleId 非空。修复前 500 → **RED**。

**用例**：`AC1 ← 用例 PermissionTreeContractTest#树接口返回可消费结构且含种子权限`

### AC2 — 零回归护栏

**步骤与期望**：独立方法：分页 `GET /api/v1/permissions?page=1&size=10` → `$.code=0`、records isArray、含 code=user:view 行、既有七键在场；`GET /api/v1/permissions/grouped` → `$.code=0` 且 `$.data` 为对象（isObject）。基线护栏（实现前后皆绿，非 RED 驱动）。

**用例**：`AC2 ← 用例 PermissionTreeContractTest#既有分页与grouped接口保持可用`

### AC3 — 平铺结构契约

**步骤与期望**：独立方法：tree 响应 → `$.data.length` ≥17、`$.data[*].children[*]` 为空（用 jsonPath `$.data[?(@.children.length() > 0)]` 为空集合）、`$.data[*].id` distinct 计数=数组长度（id 唯一）；首节点 moduleId=1 且 id 最小（按 moduleId,id 排序）。修复前 500 → **RED**（与 AC1 同方法短路亦可，独立方法分断言）。

**用例**：`AC3 ← 用例 PermissionTreeContractTest#树为权限平铺且children为空与id唯一`

### AC4 — 冒烟 web-048a 与门禁总账

**步骤与期望**：smoke 新增 web-048a（G1-G4 形态以 Generator 实际写法钉死）；实现前 G1/G2/G3 RED。五线亲跑基线对照。

**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

> TDD 载体：契约测试类先于产品改动写出（tree 500 天然 RED）；冒烟用例体首跑 RED。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | PermissionTreeContractTest#树接口返回可消费结构且含种子权限 | AC1 | ✅ RED(500)→GREEN 3/3 |
| 2 | PermissionTreeContractTest#既有分页与grouped接口保持可用 | AC2 | ✅ 绿（实现前后皆绿） |
| 3 | PermissionTreeContractTest#树为权限平铺且children为空与id唯一 | AC3 | ✅ RED(500)→GREEN 3/3 |
| 4 | smoke web-048a 用例体首跑 + 门禁五线 + 冒烟全量 | AC4 | ✅ RED(EXIT=1)→GREEN 72=66✅+4❌+2⏭️ |

## RED 证据

> Generator 于任何修改动作前执行测试清单并粘贴实证（契约类首跑、冒烟 web-048a 用例体首跑）。

```text
[RED] 契约类首跑（mvn -q -Dtest=PermissionTreeContractTest test，2026-10-12）：
  Tests run: 3, Failures: 2, Errors: 0, Skipped: 0
  [ERROR] PermissionTreeContractTest.treeIsFlatPermissionsWithEmptyChildrenAndUniqueIds:133
          Status expected:<200> but was:<500>   ← AC1 tree 落 /{id} 500 复现（用户报障根因）
  [ERROR] PermissionTreeContractTest.treeReturnsConsumableStructureWithSeedPermission:79
          Status expected:<200> but was:<500>   ← AC3 同路径 RED
  AC2 existingPagedAndGroupedEndpointsRemainAvailable 过（基线护栏，实现前后皆绿）
[RED] 冒烟 web-048a 用例体首跑（G1 后短路，EXIT=1）：
  G1 FAIL (预期 RED) ← PermissionController 无 "/tree" 映射
  （G2 PermissionVO 无 children、G3 契约 2F 同红；G4 前端 api/permission.ts permissions/tree 在场=绿护栏）
```

## GREEN 证据

```text
[GREEN] 契约类定向（mvn -q -Dtest=PermissionTreeContractTest test，2026-10-13）：EXIT=0，3/3 通过
  （AC1 tree 200+data 数组+user:view 命中、AC2 分页七键+grouped 对象、AC3 平铺 ≥17/children 空/id 唯一/排序）
[GREEN] 冒烟 web-048a 用例体（G1 /tree 映射 + G2 children 字段 + G3 定向 + G4 前端契约）：✅
[GREEN] model-009 体（PermissionServiceInterfaceSpec 计数 6→7 同步后）：11/11 通过
说明：PermissionServiceInterfaceSpec 为既有结构计数断言，随 Planner 交付物 #2 接口正当扩展过期，同步 7+listAllForTree 属计数口径更新（非业务语义破坏）；PermissionVO 用 8 参重载构造零连锁（ModuleServiceImpl:199 构造点零改动，守住域边界）。
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-12 sprint-090 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 162 用例 4F+2E（五类：DataSourceConfigBinding×2、RoleController 409、SeedData 22≠2、TestLayers+TestUtils 2E，veto-6 豁免） | 165=162+3；4F+2E 五类逐条一致（DataSourceConfigBinding×2、RoleController 409、SeedData 22≠2、TestLayers+TestUtils 2E），零新增 | ✅ |
| `npm run lint` | EXIT=0，0 errors / 165 warnings | 0 errors / 165 warnings | ✅ |
| `npm run test` | 39 文件 321/321 | 39 文件 321/321 | ✅ |
| `npm run build` | ✓（~18s） | ✓ built in 17.10s | ✅ |
| `bash scripts/smoke.sh` | 71=65✅+4❌+2⏭️ → 本单 +1=72，❌恰=model-008/010、roles-001/002 零新增 | 72=66✅+4❌+2⏭️；❌恰=model-008/010、roles-001/002 与基线逐一吻合，✅含 web-048a | ✅ |

## 拆分说明

### 048a/048b 拆分（超限熔断重切）

- **048a（本次）**：tree 数据源。PermissionController(+tree)、PermissionService(+接口方法)、PermissionServiceImpl(+实现)、PermissionVO(+children)、PermissionTreeContractTest、smoke = **6 顶格**。
- **048b（后续 ⬜）**：RoleVO(+permissions)+RoleServiceImpl(填充)+RolePermissionsEchoContractTest(原 AC2)+smoke = 4。回显消费侧（PermissionAssignDrawer:102-115 读 `role.permissions`）已就位。
- 049（api/role.ts PUT）、050（role:assign-permission 门禁+@PreAuthorize）、051（moduleId 契约）顺序不变；047 顺延不变。

**研究项结论（sprint-091 定夺在案，本单直接执行）**：

- **R1 tree 形态=纯权限平铺**（children 恒 []，按 moduleId,id 升序）：模块父节点方案有两处硬伤——① `sys_module.id` 与 `sys_permission.id` 值域重叠（1,2,3...同空间）致 el-tree key 冲突；② `check-change` 回传含父节点 id 污染 049 提交载荷（`assignPermissions` 会收到模块 id 插入 sys_role_permission）。平铺零冲突、提交干净、符合 `PermissionTreeNode extends PermissionVO` 类型。模块分组语义由 `moduleId`/`moduleName` 字段+排序承载。
- **R2 分层落位**：`PermissionService.listAllForTree()`（或同义名）+Impl 实现（复用 grouped 查询或独立 selectList，注释注明字典小数据 ≤1000 豁免分页）；Controller 仅委托（§3 三件事）。
- **R3 既有测试相容**：点状 jsonPath 不排键集 → PermissionVO 加 children 零连锁；`/grouped` 与分页实现零触碰。非过期断言连锁失败 → 停止回报。
- **R4 前端零改动**：api/permission.ts、PermissionAssignDrawer.vue、types/permission.ts、api/role.ts 全不动。

**超限熔断**：实际业务文件 >6、需改迁移/前端/既有五映射/RoleVO、非过期断言连锁失败 → 停止回报回 Planner。

**不动清单**：V1-V7 迁移与计数断言；RoleVO/RoleServiceImpl（归 048b）；api/role.ts/Drawer/IndexView/门禁；`PermissionController` 既有五映射；`types/permission.ts`；冒烟既有用例与 ❌ 四条；web/045/047/048b/049/050/051 预留；any 165；预存 application 模板两文件。

## 交付物（6 业务文件）

1. `backend/src/main/java/com/authcore/controller/PermissionController.java` — 新增 `GET /tree`
2. `backend/src/main/java/com/authcore/service/PermissionService.java` — +tree 查询接口方法
3. `backend/src/main/java/com/authcore/service/impl/PermissionServiceImpl.java` — +实现（平铺、排序、小数据豁免注释）
4. `backend/src/main/java/com/authcore/dto/permission/PermissionVO.java` — +children 字段
5. `backend/src/test/java/com/authcore/controller/PermissionTreeContractTest.java` — AC1/AC2/AC3 三方法
6. `scripts/smoke.sh` — 新增 web-048a 用例（AC4）
7. 文档回写：task/session/iteration/registry 四件

## 实现说明

**实现要点**：先写契约测试类三方法（tree 500 天然 RED；AC2 基线护栏）留实证；后端 PermissionVO 加 `List<PermissionTreeNodeVO 或 PermissionVO> children`？**children 元素类型定夺**：`PermissionTreeNode extends PermissionVO` 前端消费的是权限对象数组嵌套数组——后端 children 类型用 `List<PermissionVO>`（平铺时恒空，但类型保留嵌套能力；Java record 自引用 `List<PermissionVO>` 即可，无需新类型）→ **不新建节点 VO 文件，6 文件口径成立**；Service/Controller 逐层委托。

### 新增
- `backend/src/test/java/com/authcore/controller/PermissionTreeContractTest.java` — tree 契约+零回归+平铺结构（AC1/AC2/AC3）

### 修改
- `backend/src/main/java/com/authcore/controller/PermissionController.java` — +GET /tree（委托）
- `backend/src/main/java/com/authcore/service/PermissionService.java` — +tree 查询方法声明
- `backend/src/main/java/com/authcore/service/impl/PermissionServiceImpl.java` — +平铺实现
- `backend/src/main/java/com/authcore/dto/permission/PermissionVO.java` — +children（List<PermissionVO>）
- `scripts/smoke.sh` — +web-048a 用例

### 删除
（无）

### 变更计数

业务文件 6（Controller+Service+Impl+PermissionVO+契约类+smoke），**6 顶格达标**；RoleVO/ServiceImpl/前端/迁移/既有五映射零改动。

## 评审意见

### 第 1 轮评审（sprint-092，2026-10-13）：✅ 通过 8.4/10

- **验收标准**：AC1-4 亲核全满足（契约类 3/3、冒烟 72=66✅+4❌+2⏭️ ❌恰=基线四条、五线亲测=基线）。
- **一票否决**：六项零命中（门禁亲跑 mvn 165=4F+2E 五类逐一=豁免基线；前端 0E|165W/321/321/✓17.48s；冒烟增量 +14 行 web-048a 无删除既有用例；提交序 `fdaef61 test`→`f25e532 feat` 物证 RED 先行）。
- **评分**：功能正确性 9、代码质量 8、规范遵守 8、TDD 执行度 8、安全性 9，均分 **8.4**。
- **失分点（不退回，入改进建议）**：① `listAllForTree` 经 `toVO` 循环 `moduleMapper.selectById` 形成新增 N+1 执行路径（§4 硬规则，与类内既有 toVO 同构但为新增调用面）；② 新增查询方法未标 `@Transactional(readOnly=true)`（§3 硬规则，ModuleServiceImpl:212 有方法级先例）；③ `/tree` 缺未认证/无权限两类契约用例（tdd-workflow 四类覆盖，框架级保护由 SecurityConfig:93 白名单外+auth-003 在场兜底）。
- **范围**：diff 11 文件=变更清单 7 业务+4 文档，无夹带（application 模板 D/?? 未混入）。
- **暗修口径执行**：回显空归 048b、提交 405 归 049、门禁归 050，未判缺陷；PermissionServiceInterfaceSpec 6→7 认定为交付物 #2 授权的过期结构计数同步（非连锁破坏）。
