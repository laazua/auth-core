# Sprint 工作单：sprint-087

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-087 |
| 所属模块 | web |
| 功能点 ID | **web/042**（新注册） |
| 功能点名称 | 修复权限管理页模块筛选——初始锁死 moduleId=0 空列表、缺「全部模块」选项无法清除筛选 |
| 状态 | DONE |
| 创建时间 | 2026-10-10 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/006 | 权限管理页+模块管理页（报障页） | ✅ sprint-028 |
| web/030 | 权限页模块下拉选项接口（/modules/permissions/all） | ✅ sprint-080 |
| web/041 | BaseSelect click-outside 竞态修复（下拉可交互前提） | ✅ sprint-086 |
| web/011a/b/c | 前端门禁基线（build ✓、test 0 failed、lint 0E/165W） | ✅ sprint-082/083 |
| sprint-086 | 门禁基线（lint 0E\|165W、test 317、冒烟 67=61✅+4❌+2⏭️、mvn 154 4F+2E） | ✅ PASS 9.7/10 |

## 业务背景

**用户需求（2026-10-10）**：「你上面的优化，虽然下拉框可以进行选择了，但是没有达到筛选的功能；当选择停用，操作选项第二个按钮变成了启用，选择启用，又变成了停用；请重新优化」；随后用户对调查汇报回复「继续」授权按流程开工。

### 报障面调查实录（Planner 只读取证，2026-10-10 浏览器实测）

| 页面 | 下拉 | 实测结论 |
|------|------|---------|
| **权限管理** /system/permissions | 模块筛选 | **❌ 双缺陷实锤**：① 初始请求 `moduleId=0`（permissionQuery 初值 `moduleId: 0`，IndexView.vue:44）→ 后端 `eq(module_id, 0)` 无匹配 → **打开即空表**（rowCount=0、empty=true）；② moduleOptions 仅 `[{name,id}×4]` **无「全部模块」选项** → 选中模块后**无法清除筛选**，且 `Number(permissionQueryForSelect.moduleId) || 0`（IndexView.vue:304）把空串转 0 恒回锁死态 |
| 用户 /system/users | 状态筛选 | ✅ 正常：status=0→28 条全停用、status=1→1077 条全启用、`status=`（全部）→1105 混合；回显/选中高亮/分页 total 均正确 |
| 角色 /system/roles | 状态筛选 | ✅ 正常：status=0→10、status=1→12，参数/结果/高亮一致 |
| 模块 /system/modules | 状态筛选 | ✅ 正常：status=0→0 条（库中无停用模块，非缺陷）、status=1→4 |

**「选停用→操作列按钮变启用」语义判定**：按钮=对行数据可执行的动作（`user.status === 1 ? '停用' : '启用'`，users/IndexView.vue:109），筛选出生用/停用行后按钮必然呈现对侧动作——**RBAC 正确语义**（docs/01-architecture.md 第 6 节第 5 条停用语义），行内切换+筛选保持亦实测正常。故 **A 面（用户/角色/模块三页状态筛选）不纳入本单代码改动**，证据在案；若用户后续提供精确复现步骤另立工作单。

**本单范围 = B 面（权限页模块筛选双缺陷）**：初始不过滤 + 可清除（「全部模块」选项）+ 选中模块过滤护栏。

## 需求与架构对照

- module_id 过滤属列表查询能力，不触碰 docs/01-architecture.md 第 6 节 RBAC 硬语义（权限归属/授权路径/删除保护/停用语义均不涉及）；`eq(module_id, 0)` 之 0 非合法模块 ID，属实现层缺陷非架构冲突。
- 后端 `PermissionServiceImpl` 的 `if (dto.moduleId() != null)` 语义正确（null=不过滤），**本单不动后端**：修前端初值与空值传参即可达成「初始不过滤、空选不过滤」。

## 验收标准（TDD 驱动）

- [x] AC1 — 权限页初始加载不带 module_id 过滤：挂载权限管理页触发的 `permissionApi.list` 调用参数中 `moduleId` 为 undefined（不产生 `moduleId=0` 过滤值），即实现「打开即全量列表」。← 用例 `AC1 ← 用例 system/IndexView.spec#权限页初始查询不带moduleId过滤值`
- [x] AC2 — 「全部模块」选项可清除筛选：moduleOptions 首项为 `{ label: '全部模块', value: '' }`（选项列表可在场判定）；选中该项后 `handlePermissionSearch` 发出的 `permissionApi.list` 参数中 `moduleId` 为 undefined（不过滤），列表恢复全量。← 用例 `AC2 ← 用例 system/IndexView.spec#全部模块选项清除moduleId筛选`
- [x] AC3 — 选具体模块仍按其 id 过滤（护栏，防过度修复）：选「角色管理」（id=2 等）后 `permissionApi.list` 参数 `moduleId` 为该数字 id；抽屉「查看权限」联动（IndexView.vue:503 赋值 `permissionQueryForSelect.moduleId`）后同样按 id 过滤。既有用例 `filters permissions by module`（moduleId:1 断言）须保持绿。← 用例 `AC3 ← 用例 system/IndexView.spec#选择模块后按moduleId过滤（含抽屉联动）`
- [x] AC4 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-042` 用例（组件 spec 定向 + 初始无 `moduleId: 0` 锁死形态 + 「全部模块」选项在场源码断言）；门禁五线基线对照（lint 0E|165W、test 317+新增、build ✓、mvn 154=4F+2E 基线、冒烟 ≥68 ❌恰4 零新增）。← 用例 `AC4 ← 用例 门禁五线 + 冒烟全量`

### AC1 — 权限页初始查询不带 module_id 过滤

**前置**：无（默认打开 /system/permissions 或权限 Tab 默认挂载即触发）。

**步骤与期望**：挂载 IndexView → 等待 onMounted 装载完成 → 断言 `mockPermissionApi.list` 首次调用参数 `moduleId === undefined`（键可存在值为 undefined，axios 序列化丢弃 → 网络请求不含 moduleId → 后端 null 不过滤）。当前实现传 `moduleId: 0` → **RED**。

**用例**：`AC1 ← 用例 system/IndexView.spec#权限页初始查询不带 moduleId 过滤值`

### AC2 — 全部模块选项清除筛选

**前置**：权限 Tab 已挂载、moduleOptions 装载完成（既有实证需跨一个宏任务，spec 注释在案）。

**步骤与期望**：① 断言 moduleOptions 首项映射 `{ label: '全部模块', value: '' }`（mock select 首 option 文本「全部模块」）；② select 选值 ''（全部模块）触发 change → `handlePermissionSearch` → 断言 `permissionApi.list` 参数 `moduleId === undefined`。当前无该选项且 `Number('')||0=0` → **RED**。

**用例**：`AC2 ← 用例 system/IndexView.spec#全部模块选项清除 moduleId 筛选`

### AC3 — 选择模块后按 moduleId 过滤（护栏）

**前置**：权限 Tab 挂载、moduleOptions 就绪。

**步骤与期望**：select 选 '2'（角色管理）→ 断言 `permissionApi.list` 最近调用参数 `moduleId: 2`（number）；抽屉联动路径复用既有赋值语义（`permissionQueryForSelect.moduleId = String(id)`）选值后同样过滤。既有 `filters permissions by module`（moduleId:1）须同批保持绿——护栏用例对当前实现即绿，修复后不得回归。

**用例**：`AC3 ← 用例 system/IndexView.spec#选择模块后按 moduleId 过滤（含抽屉联动）`

### AC4 — 冒烟与门禁总账

**步骤与期望**：smoke 新增 web-042 用例并首跑 RED（修复形态缺失）→ GREEN；五线亲跑基线对照（口径见业务背景前置依赖表）。

**用例**：`AC4 ← 用例 门禁五线 + 冒烟全量`

## 测试清单

> TDD 载体：**组件级 spec 新增用例先于修复写出**（AC1/AC2 当前实现必红=天然 RED）；AC3 护栏同批（当前即绿）；AC4 冒烟首跑红。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | system/IndexView.spec 新增：权限页初始查询不带 moduleId 过滤值 | AC1 | RED（expected +0 to be undefined）→ GREEN（43/43 绿） |
| 2 | system/IndexView.spec 新增：全部模块选项在场 + 选中清除筛选 | AC2 | RED（expected '用户管理' to be '全部模块'）→ GREEN |
| 3 | system/IndexView.spec 新增/复用：选择模块按 moduleId 过滤（含抽屉联动赋值路径） | AC3 | 护栏当场绿 + 修复后保持绿 |
| 4 | smoke web-042 首跑（修复形态缺失 → RED EXIT=1） | AC4 | RED 四断言全拒绝（G1-G4 FAIL）→ GREEN ✅ |
| 5 | 门禁五线 + 冒烟全量（≥68、❌恰4、零新增） | AC4 | 五线=基线、冒烟 68=62✅+4❌+2⏭️ web-042 ✅ |

## RED 证据

> Generator 于任何修改动作前执行测试清单并粘贴实证（AC1/AC2 spec 与 AC3 冒烟首跑日志、修复前失败输出）。

```text
[RED-1] spec 首跑（修复前，npx vitest run src/views/system/IndexView.spec.ts）
 Test Files  1 failed (1)
      Tests  2 failed | 41 passed (43)
 FAIL > sprint-087 web/042 > 初始挂载权限列表查询不带 moduleId 过滤值
 AssertionError: expected +0 to be undefined   ← permissionQuery 初值 moduleId: 0 锁死
 FAIL > sprint-087 web/042 > 筛选下拉首项为全部模块且选中后清除 moduleId 筛选
 AssertionError: expected '用户管理' to be '全部模块'   ← moduleOptions 无「全部模块」首项
 （AC3 护栏与全部既有用例 41 passed 当场保持绿）
```

```text
[RED-2] 冒烟用例体 web-042 首跑（修复前）
 SPEC-RED-EXIT=1（vitest 定向 2 failed 拒绝）
 G1-FAIL（Tests passed 正则不中）
 G2-FAIL（grep "label: '全部模块'" 不中）
 G3-FAIL（! grep 'moduleId: 0' 取反失败——初值锁死形态在场）
 G4-FAIL（! grep '|| 0' 取反失败——空值转 0 形态在场）
→ 四断言全拒绝，用例体 RED
```

```text
[GREEN] spec 定向复跑：Tests 43 passed (43)（AC1/AC2 转绿、AC3 护栏+41 既有全绿）
[GREEN] 冒烟 web-042 用例体：✅ 通过（vitest 定向 + 三形态断言全中）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。基线口径（2026-10-10 sprint-086 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（五类，veto-6 豁免） | 154 用例 4F+2E（DataSourceConfigBindingTest×2、RoleController 409、SeedData 22、TestLayers+TestUtils Docker 2E） | ✅ 基线逐条一致零新增 |
| `npm run lint` | EXIT=0，0 errors / 165 warnings | 0 errors / 165 warnings | ✅ 基线一致 |
| `npm run test` | 0 failed \| 317 passed (317)+本单新增 | 0 failed \| 320 passed (320)（317+3） | ✅ |
| `npm run build` | ✓（~18s） | ✓ built in 17.62s | ✅ |
| `bash scripts/smoke.sh` | 67=61✅+4❌+2⏭️ | 68=62✅+4❌+2⏭️（新增 web-042 ✅） | ✅ ❌恰=model-008/010、roles-001/002 零新增 |

## 拆分说明

预估验收标准 **4 条（=上限）**；预估文件变更 **3~4**（`views/system/IndexView.vue` 1 + `views/system/IndexView.spec.ts` 1 + `scripts/smoke.sh` 1 + 可能类型定义 `api/permission.ts` 或 types 1；文档回写 4 不计入）——**≤6 文件不拆分**。

**研究项（Generator 先于动手核实）**：

- **R1 空值形态与序列化口径（红线：后端不动）**：候选 a) `permissionQuery.moduleId` 初值 `0` → `undefined`，axios 序列化丢弃 undefined → 请求无 moduleId → 后端 null 不过滤；候选 b) 传 `''` 空串 → Spring `@RequestParam(required=false) Integer` 空串转 null（users 页 `status=` 空串先例实测可行，total=1105 验证）。二选一并给出实测证据；同步 `PermissionQuery` 类型（`moduleId?: number` 或等效，TS strict 无 any）；`Number(v) || 0` 语义改为「空值→不过滤、数值→过滤」，注意 `||` 对合法 0 的吞没本单无碍（module_id 无 0）但建议显式判断防误读。
- **R2 抽屉联动语义保持**：IndexView.vue:503 `permissionQueryForSelect.moduleId = String(newModuleId)`（「查看权限」跳转）修复后仍须按 id 过滤（并入 AC3 断言或独立断言）；503 行所在函数与 handlePermissionSearch 的交互一并自查。
- **R3 A 面范围锁定**：用户/角色/模块三页状态筛选实测正常（业务背景表），**禁止**「顺手优化」三页任何筛选代码；若实现中发现三页确有缺陷实证，停止回报另立工作单。
- **R4 既有 spec 相容性预判**（Planner 已核，Generator 复核）：`loads first page by default on mount` 断言 `objectContaining({page,size})` 不含 moduleId 键 → 与 AC1 相容；`filters permissions by module` 断言 `moduleId: 1` number → 与修复后数值过滤相容；BaseSelect 被 mock 为原生 select（option value 恒字符串）→ AC2 选 '' 与真实组件 emit 空串/空值口径需对齐（`Number('')||0` 改动后两条路径同结论）；moduleOptions 装载有「跨宏任务 20ms」既有实证注释须沿用。**若发现既有断言固化 `moduleId:0` 初始锁死语义** → 属过期口径同步情形，先在 task 登记再改（web-013/026 先例）；**若与本次修复无关的既有用例连带失败 → 熔断停止回报**。

**超限熔断**：实际文件 >6、或需动后端（.java/yml）、或修复引发既有 spec 连锁失败需改**非过期口径**断言、或发现第 2 类独立缺陷面 → **停止回报，回 Planner 重切分**。

**不动清单**：用户/角色/模块三页（IndexView users/roles + system 模块面板筛选逻辑）；后端全部；BaseSelect.vue；冒烟既有用例断言语义与 ❌ 四条（model-008/010、roles-001/002）不修改不删除；web/036c2/036c3、web/011d 预留；any 165；**工作树预存改动 `backend/src/main/resources/application.yml.template` 删除 + `application.template.yml` 新增（非本单产生，不混入本单提交，提交时按范围精确 stage）**。

## 交付物（≈3-4 文件）

1. `frontend/src/views/system/IndexView.vue`：permissionQuery 初值与空值语义、moduleOptions 增「全部模块」、handlePermissionSearch 空值不过滤
2. `frontend/src/views/system/IndexView.spec.ts`：新增 AC1/AC2/AC3 用例（先于修复）
3. `scripts/smoke.sh`：新增冒烟用例 web-042（AC4）
4. 可能：`frontend/src/api/permission.ts` 或类型定义（PermissionQuery moduleId 可选化）
5. 文档回写：task/session/iteration/registry 四件

## 实现说明

**实现要点**：`frontend/src/views/system/IndexView.vue` 三点最小修复——① `permissionQuery` 初值删 `moduleId: 0`（类型 `moduleId?: number` 本就可选，省略=undefined=不产生过滤参数，axios 序列化丢弃该键）；② 新增 `permissionFilterOptions` computed = `[{label:'全部模块', value:''}, ...moduleOptions]` 仅绑筛选下拉（`:options` 换绑，**表单 drawer 的 `moduleOptions` 不动**——AC2 口径登记：task 原文「moduleOptions 首项」指筛选下拉的选项列表，若把「全部」插进 moduleOptions 会污染权限创建表单的必选模块语义，故筛选专用 options 分离，AC 判定实质不变）；③ `handlePermissionSearch` 空串→`undefined`（不过滤）、数值→`Number()`，替代 `|| 0` 的空值转 0。`watch(route.query.moduleId)` 抽屉联动分支（赋数值+String 回显）零改动天然保持。

### 新增
（无）

### 修改
- `frontend/src/views/system/IndexView.spec.ts`:527-579 — 新增 describe「sprint-087 web/042」三用例：AC1 初始查询 `moduleId===undefined`、AC2 筛选下拉首项「全部模块」+ 选 `''` 后清除过滤、AC3 选 `2` 过滤 + 路由 `?moduleId=3` 联动护栏
- `frontend/src/views/system/IndexView.vue`:40-57 — permissionQuery 删 `moduleId: 0` 初值；新增 `permissionFilterOptions` computed（首项「全部模块」）
- `frontend/src/views/system/IndexView.vue`:312-319 — `handlePermissionSearch` 空串→undefined、数值→Number，替代 `Number(v)||0`
- `frontend/src/views/system/IndexView.vue`:545 — 筛选 BaseSelect `:options` 由 `moduleOptions` 换绑 `permissionFilterOptions`
- `scripts/smoke.sh`:500-510 — 追加冒烟用例 web-042（权限页 spec 定向 + 「全部模块」生成、无 `moduleId: 0`、无 `|| 0` 三形态断言）

### 删除
（无）

### 变更计数
业务文件 3（spec/IndexView.vue/smoke.sh）+ 文档回写 4（task/session/iteration/registry），**≤6 达标**；后端零改动、A 面三页零改动、BaseSelect.vue 零改动。

## 评审意见

**sprint-087 评审通过（2026-10-10，平均分 9.7/10，六否决零命中）——REWORK 第 0 次（首轮通过）**

- **AC 逐条**：AC1/AC2/AC3/AC4 全满足（spec 43/43、R3 浏览器四动作实测、smoke web-042 ✅、五线=基线）
- **RED 独立复现**：`git checkout b7833d6 -- frontend/src/views/system/`（test-first 时点：spec 含新用例+实现未动）→ `Tests 2 failed | 41 passed (43)`，恰为 AC1/AC2 两用例 → 恢复 HEAD 干净
- **门禁亲跑**：mvn 154 4F+2E 五类逐条=基线零新增；lint 0E|165W、test 320/320（317+3）、build ✓26.75s；冒烟 68 ❌恰=model-008/010、roles-001/002 零新增、`+smoke_case=1` 增量取证、web-042 ✅
- **R3 浏览器实测**（headless Chromium，登录→/system/permissions）：初始请求 `?page=1&size=10&name=&code=` **无 moduleId**、total=16 非空（缺陷①修复）；选「角色管理」→ `moduleId=2` total=4；选「全部模块」→ 请求回无 moduleId、total=16（缺陷②闭环清除）；路由 `?moduleId=3` → `moduleId=3` total=4（抽屉联动保持）
- **变更范围**：diff 7 文件=清单 3 业务+4 文档零夹带；三段提交 test `b7833d6`→feat `ecd387e`→docs `4a78d63` 顺序物证；A 面三页/BaseSelect/后端零改动
- **AC2 口径**：筛选专用 `permissionFilterOptions`（表单 moduleOptions 不污染必选语义）判定实质与 AC2 一致，登记认可
- **五维**：9.5/9.5/9.5/10/10（评分表见评审报告）
- 六否决项逐项核对零命中。
