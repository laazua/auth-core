# Sprint 工作单：sprint-089

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-089 |
| 所属模块 | web |
| 功能点 ID | **web/044**（新注册） |
| 功能点名称 | 修复用户/角色/权限/模块四管理页搜索关键词不命中——后端列表查询多字段 AND 语义缺陷（应为 OR） |
| 状态 | **DONE**（sprint-089 评审通过 9.8/10，2026-10-12） |
| 创建时间 | 2026-10-12 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| users/001 | 用户分页条件查询 API（GET /users，四关键词参数） | ✅ sprint-016 |
| roles/001 | 角色分页条件查询 API（GET /roles，name/code 参数） | ✅ sprint-019 |
| perms/001 | 权限分页条件查询 API（GET /permissions，name/code/moduleId） | ✅ sprint-021 |
| modules/001 | 模块分页条件查询 API（GET /modules，name/code/status） | ✅ sprint-022 |
| web/004/005/006 | 四管理页前端（搜索框多字段同值复制发送） | ✅ sprint-026/027/028 |
| web/041/042/043 | 筛选下拉竞态/权限页模块筛选/模块页切换按钮——三单已收口，本单为其后报障排查的新缺陷 | ✅ sprint-086/087/088 |
| sprint-088 | 门禁基线（mvn 156=4F+2E 五类、lint 0E\|165W、test 320/320、build ✓、冒烟 69=63✅+4❌+2⏭️） | ✅ PASS 9.9/10 |

## 业务背景

**用户需求（2026-10-12）**：「前端 用户管理，角色管理，权限管理，模块管理 这几个路由页面的筛选框不准确，不能进行准确搜索，筛选」。

### 报障面排查实录（Planner 只读取证，2026-10-12）

sprint-086/087/088 已收口：四页下拉打不开（web/041）、权限页模块筛选锁死（web/042）、模块页切换按钮不可见（web/043）；用户/角色/模块三页**状态筛选**经浏览器实测正常（sprint-087 A 面，「选停用按钮变启用」=RBAC 正确语义）。本次报障排查聚焦**文本搜索框**，四页共性根因实锤：

| 页 | 前端行为 | 后端语义 | 实测（9998 在线，只读 GET） |
|----|---------|---------|------------------------------|
| 用户 /system/users | 搜索框绑 `username`，`handleSearch` 同值复制到 nickname/email/phone（users/IndexView.vue:166-172） | `UserServiceImpl.queryUsers` 四字段各自 `like` **AND 叠加**（:53-64） | `?username=admin&nickname=admin&email=admin&phone=admin` → **total=0**；仅 `username=admin` → total=1(admin)；四字段同值搜「管理」→ 0，仅 `nickname=管理` → 1(管理员) |
| 角色 /system/roles | 搜索框绑 `code`，`handleSearch` 同值复制到 name（roles/IndexView.vue:141-144） | `RoleServiceImpl.queryRoles` name like ∧ code like **AND**（:55-60） | `?name=ROLE_ADMIN&code=ROLE_ADMIN` → **total=0**；仅 `code=ROLE_ADMIN` → 1(管理员/ROLE_ADMIN)；`name=管理&code=管理` → 0，仅 `name=管理` → 1 |
| 权限 /system/permissions | 搜索框绑 `code`，同值复制到 name（system/IndexView.vue:307-313） | `PermissionServiceImpl.queryPermissions` name ∧ code **AND**（:52-56） | `?name=user:view&code=user:view` → **0**；仅 `code=user:view` → 1(用户查看)；`name=用户&code=用户` → 0，仅 `name=用户` → 4 |
| 模块 /system/modules | 搜索框绑 `code`，同值复制到 name（system/IndexView.vue:328-331） | `ModuleServiceImpl.queryModules` name ∧ code **AND**（:52-57） | `?name=user_mgmt&code=user_mgmt` → **0**；仅 `code=user_mgmt` → 1(用户管理)；仅 `name=用户管理` → 1 |

**根因**：前端搜索框 placeholder 承诺多字段搜索（「用户名/昵称/邮箱/手机号」「角色名/编码」等），`handleSearch` 把同一关键词复制进全部字段，意图=任一字段命中（OR）；后端却把各字段 `like` 条件 **AND 叠加** → 关键词必须同时出现在所有字段 → 交集几乎恒空 → **用户输入任何常规关键词（用户名、角色名、编码、昵称）四页搜索均返回空列表**。全仓 grep 证实四个 ServiceImpl 均无 `.or(`/`nested` OR 形态。

**期望行为**：搜索框关键词对占位符承诺的字段集**任一命中即返回**（OR），且与既有结构化筛选（用户/角色/模块 status、权限 moduleId）保持 **AND 组合**；如 `?keyword同值多字段&status=1` 只返回命中且启用的行。

## 需求与架构对照

- `docs/01-architecture.md` 未定义列表查询关键词的 AND/OR 语义（§3 实体表、§6 硬语义均不涉查询组合）→ 本单为**实现层查询语义修正**，对齐前端占位符已承诺的多字段搜索意图，**无架构冲突，无需仲裁**。
- 契约面评估：GET 参数名/类型/分页响应结构零改动（仍是 username/nickname/email/phone、name/code、moduleId、status + `{records,total,...}`）；仅关键词多字段组合从 AND 收敛为 OR。唯一前端消费方（四管理页）本就是按 OR 意图发送同值；无其他集成方使用这些列表接口（integration 域仅走 /auth/check 与 gateway）→ 登记接受。
- §6.5 停用语义不受影响：status 仍为独立 `eq` 条件，与关键词 OR 组以 AND 组合。

## 验收标准（TDD 驱动）

- [x] AC1 — 用户域关键词 OR：`UserQueryDTO(page=1,size=10,username=v,nickname=v,email=v,phone=v,status=null)` 四字段同值时，返回 total≥1 且 records 中至少一行的 username/nickname/email/phone 任一含 v（基线实证：v=admin 现返回 0）；`status` 组合仍 AND：同参 + `status=0` 时所有返回行 status=0，且关键词命中条件仍成立。← 用例 `AC1 ← 用例 ListSearchOrSemanticsTest#用户关键词四字段任一命中即返回且与status与逻辑组合`
- [x] AC2 — 角色域关键词 OR：`RoleQueryDTO(1,10,v,v,null)` name/code 同值时 total≥1 且 records 任一行 name 或 code 含 v（基线实证：v=ROLE_ADMIN 现 0，期望命中「管理员/ROLE_ADMIN」；v=管理 现 0，期望命中「管理员」）；`status=1` 组合仍 AND（返回行全启用）。← 用例 `AC2 ← 用例 ListSearchOrSemanticsTest#角色关键词名称或编码任一命中且与status与逻辑组合`
- [x] AC3 — 权限/模块两域关键词 OR（同构）：权限 `PermissionQueryDTO(1,10,v,v,null)` 时 v=「用户」期望 total≥4 且 records 名称含「用户」；v=`user:view` 期望命中 1 行 code=`user:view`；`moduleId` 组合仍 AND（加 user_mgmt 模块 id 后仍命中该模块下用户权限，加其他模块 id 则 0）。模块 `ModuleQueryDTO(1,10,v,v,null)` 时 v=`user_mgmt` 与 v=「用户管理」均各命中 1 行；`status=1` 组合仍 AND。← 用例 `AC3 ← 用例 ListSearchOrSemanticsTest#权限关键词名称或编码任一命中且与模块筛选与逻辑组合` + `ListSearchOrSemanticsTest#模块关键词名称或编码任一命中且与status与逻辑组合`
- [x] AC4 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-044` 用例（四 ServiceImpl OR 形态 grep + 定向跑新契约测试类，首跑 RED → GREEN），冒烟总数 69→70、❌ 恰 4（model-008/010、roles-001/002）零新增；门禁五线基线对照零新增。← 用例 `AC4 ← 用例 门禁五线 + 冒烟全量`

### AC1 — 用户域四关键词 OR + status AND 组合

**前置**：真实库种子存在 admin（username=admin，nickname 含「管理」）。

**步骤与期望**：新契约测试类独立方法：① `queryUsers(new UserQueryDTO(1,10,"admin","admin","admin","admin",null))` → total≥1 且任一行 username/nickname/email/phone 含 "admin"（当前 AND → total=0 = **RED**）；② 同参 + status=1 → 返回行全 status=1 且仍满足关键词任一命中；③ 单字段参数行为不变（`username=admin` 仍命中，护栏）。

**用例**：`AC1 ← 用例 ListSearchOrSemanticsTest#用户关键词四字段任一命中即返回且与status与逻辑组合`

### AC2 — 角色域 name/code OR + status AND 组合

**前置**：真实库种子存在「管理员/ROLE_ADMIN」。

**步骤与期望**：① `queryRoles(new RoleQueryDTO(1,10,"ROLE_ADMIN","ROLE_ADMIN",null))` → total≥1 且任一行 name 或 code 含 "ROLE_ADMIN"（当前 0 = **RED**）；② v="管理" 同构命中「管理员」；③ status=1 组合返回行全启用。

**用例**：`AC2 ← 用例 ListSearchOrSemanticsTest#角色关键词名称或编码任一命中且与status与逻辑组合`

### AC3 — 权限/模块两域同构 OR（moduleId/status 组合护栏）

**步骤与期望**：权限：① v="用户" name/code 同值 → total≥4（当前 0 = **RED**）；② v="user:view" → 命中 code=`user:view` 行；③ + moduleId=user_mgmt 模块 id → 仍命中该模块下用户权限；+ 非匹配模块 id → 0（AND 保持）。模块：④ v=`user_mgmt` 与 v="用户管理" 均命中「用户管理」模块行（当前同值组合均 0 = **RED**）；⑤ status=1 组合返回行全启用。

**用例**：`AC3 ← 用例 ListSearchOrSemanticsTest#权限关键词名称或编码任一命中且与模块筛选与逻辑组合` + `ListSearchOrSemanticsTest#模块关键词名称或编码任一命中且与status与逻辑组合`

### AC4 — 冒烟 web-044 与门禁总账

**步骤与期望**：smoke 新增 web-044：G1-G4 grep 四个 ServiceImpl 存在 OR 形态（`.or(` 或等价 nested OR，以 Generator 实现形态为准在用例中钉死）；G5 `mvn -q -Dtest='ListSearchOrSemanticsTest'` 定向通过——修复前 G1-G5 全 RED；修复后 GREEN。五线亲跑基线对照（口径见前置依赖表）。

**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

> TDD 载体：新契约测试类四方法**先于**四个 ServiceImpl 改动写出（当前 AND 语义 = 天然 RED）；冒烟用例体首跑 RED；基线实测（9998 只读探针）已在案。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | ListSearchOrSemanticsTest#用户关键词四字段任一命中即返回且与status与逻辑组合 | AC1 | RED（total=0）→ GREEN ✅ |
| 2 | ListSearchOrSemanticsTest#角色关键词名称或编码任一命中且与status与逻辑组合 | AC2 | RED（total=0）→ GREEN ✅ |
| 3 | ListSearchOrSemanticsTest#权限关键词名称或编码任一命中且与模块筛选与逻辑组合 | AC3 | RED（total=0）→ GREEN ✅ |
| 4 | ListSearchOrSemanticsTest#模块关键词名称或编码任一命中且与status与逻辑组合 | AC3 | RED（total=0）→ GREEN ✅ |
| 5 | smoke web-044 用例体首跑 + 门禁五线 + 冒烟全量 | AC4 | RED EXIT=1 → GREEN EXIT=0；冒烟 70=64✅+4❌+2⏭️ |

## RED 证据

> Generator 于任何修改动作前执行测试清单并粘贴实证（新类四方法首跑、冒烟 web-044 用例体首跑）。

```text
[RED-1] 新契约测试类四方法首跑（改任何 ServiceImpl 之前，2026-10-12 mvn -q test -Dtest='ListSearchOrSemanticsTest'）
 Tests run: 4, Failures: 4, Errors: 0, Skipped: 0
 [RED-AC1] ListSearchOrSemanticsTest.用户关键词四字段任一命中即返回且与status与逻辑组合:111
   四字段同值发送时 username 命中即应返回（OR），实际 total=0 ==> expected: <true> but was: <false>
 [RED-AC2] ListSearchOrSemanticsTest.角色关键词名称或编码任一命中且与status与逻辑组合:143
   name/code 同值发送时 code 命中即应返回（OR），实际 total=0 ==> expected: <true> but was: <false>
 [RED-AC3-权限] ListSearchOrSemanticsTest.权限关键词名称或编码任一命中且与模块筛选与逻辑组合:173
   name/code 同值发送时 name 命中即应返回（OR），实际 total=0 ==> expected: <true> but was: <false>
 [RED-AC3-模块] ListSearchOrSemanticsTest.模块关键词名称或编码任一命中且与status与逻辑组合:201
   name/code 同值发送时 code 命中即应返回（OR），实际 total=0 ==> expected: <true> but was: <false>
 （四红均为 AND 语义下交集恒空，与 9998 探针基线一致）
```

```text
[RED-2] 冒烟 web-044 用例体首跑（OR 形态未落地，2026-10-12 bash -c 五形态断言）
 G1 grep '\.or()' UserServiceImpl.java 拒绝（无 OR 形态）→ 用例体 EXIT=1
 （G2-G5 随 G1 短路未执行，实现落地后全量冒烟逐条验证）
```

```text
[GREEN] 定向复跑（四 ServiceImpl OR 落地后，2026-10-12 mvn -q test -Dtest='ListSearchOrSemanticsTest'）
 tests="4" errors="0" skipped="0" failures="0" —— 四方法全绿（RED 四条全部转正）
 冒烟 web-044 用例体复跑 EXIT=0（G1-G5 全过）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-10 sprint-088 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 156 用例 4F+2E（五类：DataSourceConfigBinding×2、RoleController 409、SeedData 22≠2、TestLayers+TestUtils Docker 2E，veto-6 豁免） | 160 跑 4F+2E：DataSourceConfigBinding×2、RoleController 409、SeedData 22≠2、TestLayers+TestUtils 2E——五类逐条一致（156+本单新增 4 方法） | ✅ 零新增失败 |
| `npm run lint` | EXIT=0，0 errors / 165 warnings | EXIT=0，0 errors / 165 warnings | ✅ =基线（前端零改动） |
| `npm run test` | 0 failed \| 320 passed（前端零改动应不变） | 38 文件 320/320 passed | ✅ =基线 |
| `npm run build` | ✓（~18s） | ✓ built in 17.29s | ✅ =基线 |
| `bash scripts/smoke.sh` | 69=63✅+4❌+2⏭️ → 本单 +1=70，❌恰=model-008/010、roles-001/002 零新增 | 70=64✅+4❌+2⏭️：✅ 含 web-044；❌恰=model-008/010、roles-001/002（基线四条零新增）；⏭️=infra-004、model-006（全量复跑留档 /tmp/opencode/smoke-s089.log） | ✅ 增量 +1=64✅ |

## 拆分说明

预估验收标准 **4 条（=上限）**；预估文件变更 **6 业务**（四 ServiceImpl + 新契约测试类 + smoke.sh；文档回写 4 不计入）——**≤6 文件顶格不拆分**。

**研究项（Generator 先于动手核实）**：

- **R1 OR 实现形态**：用 MyBatis-Plus 嵌套 wrapper——`wrapper.and(w -> { w.like(A).or().like(B)... })`（各非空字段进 OR 组；全空则不加条件）；**status/moduleId 等结构化条件保持外层 AND**，不得并入关键词 OR 组。禁止 `apply`/字符串拼接 SQL。四文件同构改法。
- **R2 既有测试相容**：全测试树现有查询用例均为**单关键词字段 + status/moduleId**（UserServiceTest:73 `("query",null,null,null,1)`、RoleServiceTest:75、PermissionServiceTest:82 `("用户",null,1L)`、ModuleServiceTest:79），单字段 OR 退化为原条件 → 预期零连锁失败；若实测出现非过期断言连锁失败 → 停止回报。
- **R3 新测试类基座与断言稳健性**：沿用既有 ServiceTest 的 Spring 真实库基座；断言用 **total≥N + records anyMatch contains**，禁用精确 total 全库计数（真实库有测试造数污染，22≠2 教训）；关键词取真实库稳定种子值（admin/管理/ROLE_ADMIN/user:view/user_mgmt），测试方法相互独立、不依赖执行顺序。
- **R4 前端零改动口径**：前端「同值复制发送」在 OR 语义下与单字段发送等价（同值 OR 同值），本单**不改任何 .vue/.ts**，test 320/320 与 build 不应变化；spec 中既有 `filters by search keyword` mock 用例不受影响（mock 不经真实后端）。
- **R5 冒烟 G 形态钉死**：G1-G4 的 OR 形态 grep 以 Generator 实际写法为准（如 `grep -q '\.or()' 四文件`），用例体内钉死与实现一致；G5 定向跑新类复用 `/tmp/smoke-web-044.log` 形态（参照 web-041/042）。`smoke_case` 计数含定义行，以脚本尾部 CASES 为准：69→70。
- **R6 后端 Controller/DTO 零改动**：`@RequestParam` 参数、DTO record 字段、分页响应序列化均不动；仅 Service 实现层 wrapper 组合变化。
- **R7 范围外登记（不修，候选后续单）**：① BaseInput `clearable` 清除只发 `clear` 不发 `change` → 四页点 × 清空搜索框后列表不重查（stale）；② 前端同值多字段冗余发送可简化。二者均需动前端（vue+spec），超出本单 6 文件熔断，**由 Planner 后续另立单**（如 web/045），本单不动清单保护。

**超限熔断**：实际业务文件 >6、需改 Controller/DTO/前端 .vue/.ts/迁移文件、OR 化引发既有**非过期**断言连锁失败 → **停止回报，回 Planner 重切分**。

**不动清单**：前端全部业务代码与 spec；后端 Controller/DTO/Entity/Mapper；V1-V7 迁移；BaseInput.vue；冒烟既有用例断言语义与 ❌ 四条（model-008/010、roles-001/002）不修改不删除；web/036c2/036c3、web/011d 预留；any 165；**工作树预存改动 `backend/src/main/resources/application.yml.template` 删除 + `application.template.yml` 新增（非本单产生，不混入本单提交，提交时按范围精确 stage）**。

## 交付物（≈6 业务文件）

1. `backend/src/main/java/com/authcore/service/impl/UserServiceImpl.java`：`queryUsers` 四关键词字段 OR 组
2. `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java`：`queryRoles` name/code OR 组
3. `backend/src/main/java/com/authcore/service/impl/PermissionServiceImpl.java`：`queryPermissions` name/code OR 组
4. `backend/src/main/java/com/authcore/service/impl/ModuleServiceImpl.java`：`queryModules` name/code OR 组
5. `backend/src/test/java/com/authcore/service/ListSearchOrSemanticsTest.java`：新增契约测试类（四独立方法，AC1-AC3）
6. `scripts/smoke.sh`：新增冒烟用例 web-044（AC4）
7. 文档回写：task/session/iteration/registry 四件

## 实现说明

**实现要点**：四域列表查询把「多关键词字段各自 like」改为**非空字段进同一 OR 组**（嵌套 and+or），结构化条件（status/moduleId）保持外层 AND；测试侧新增单类四方法契约断言先行 RED；冒烟钉 OR 形态 + 定向跑新类。前端占位符承诺的「用户名/昵称/邮箱/手机号」「角色名/编码」「权限名/编码」「模块名/编码」搜索在 OR 落地后自然成立，前端零改动。

### 新增
- `backend/src/test/java/com/authcore/service/ListSearchOrSemanticsTest.java` — 四域关键词 OR + 结构化筛选 AND 组合契约测试（四独立方法）

### 修改
- `backend/src/main/java/com/authcore/service/impl/UserServiceImpl.java` — queryUsers 关键词四字段 OR
- `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java` — queryRoles name/code OR
- `backend/src/main/java/com/authcore/service/impl/PermissionServiceImpl.java` — queryPermissions name/code OR
- `backend/src/main/java/com/authcore/service/impl/ModuleServiceImpl.java` — queryModules name/code OR
- `scripts/smoke.sh` — 追加冒烟用例 web-044

### 删除
（无）

### 变更计数

业务文件 6（四 ServiceImpl + 契约测试类 + smoke.sh）+ 文档回写 4（task/session/iteration/registry），**≤6 顶格达标**；前端全部零改动、Controller/DTO/迁移零改动。

## 评审意见

**sprint-089 Evaluator 评审通过（2026-10-12，平均分 9.8/10；五维 10/9.5/9.5/10/10；六否决零命中）**

- 4 AC 亲核全满足：AC1 用户四字段 OR+status AND、AC2 角色 name/code OR+status AND、AC3 权限/模块同构 OR+moduleId/status AND 护栏（含 moduleId 不匹配 0 结果）、AC4 冒烟 web/044 ✅+门禁=基线
- RED 独立复现（Evaluator 自建 worktree@fa9aaf2（8fe16f5^）+复制契约测试类 +本地 application.yml）：**4 跑 4 失败**（四红 total=0×4 逐条复现）；首次复现因 worktree 缺 gitignored application.yml 启动失败，补拷后断言红精确复现，复现后 worktree 已清理
- 门禁亲跑：mvn **160=4F+2E** 基线五类逐条（DataSourceConfigBinding×2、RoleController 409、SeedData 22≠2、TestLayers+TestUtils 2E）；lint **0E|165W**；test **320/320**；build ✓17.79s；冒烟亲跑 **70=64✅+4❌+2⏭️** ❌恰=model-008/010、roles-001/002 零新增、**web/044 ✅**（留档 /tmp/opencode/smoke-eval-web044.log），增量取证=两段式提交 diff smoke.sh +11 行恰 web/044 用例、smoke_case 68+2 cond=70
- 变更范围：`8fe16f5^..e142290` 恰 6 文件=清单零夹带；前端/Controller/DTO/迁移零改动；预存 application 模板两文件未混入提交
- 六否决逐项：①功能缺失 ②安全漏洞（wrapper 全 #{} 参数化、无密钥/日志违规）③明确 Bug ④TDD 证据链（两段式 test→feat+独立复现）⑤RBAC 偏差（无权限语义变更）⑥回归/冒烟失败——**零命中**
- 改进建议（不计分）：① 角色/权限/模块三域 8 行三分支 OR 模式重复三处，如后续再增域可抽 SFunction 泛型小助手（本单 6 文件熔断内不值得单开文件）；② BaseInput clearable 清除不触发重查（四页 stale）已由 Planner 登记候选 web/045，建议用户裁定排期；③ 前端同值多字段冗余发送可简化为单字段（OR 落地后无害，收益低）
