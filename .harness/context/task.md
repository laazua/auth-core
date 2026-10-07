# Sprint 工作单：sprint-078

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-078 |
| 所属模块 | web |
| 功能点 ID | web/040b1（web/040b 拆分首段：模块域 updateTime 收尾） |
| 功能点名称 | 模块域更新时间对齐——`ModuleVO` 输出 `updateTime`（四 VO 对称收口） |
| 状态 | PLANNED |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/038c | 模块域 createTime 方案 B（ModuleVO 注解 + ModuleFieldContractTest 交付） | ✅ |
| web/040a | 三域 updateTime 对齐（契约测试基座与同步模式先例） | ✅ |
| web/040c | me 面时间修复（方案 B 四连至此收口仅余模块域） | ✅ |

## 业务背景

用户裁决「四 VO 全站对称」（2026-09-29 问答在案）的收口段：`dto.user|role|permission` 三 VO 已于 web/040a 输出 `{createTime, updateTime}`，me 面已由 web/040c 修复——**仅余 `dto.module.ModuleVO`**：其 `updatedAt` 组件无注解（`ModuleVO:19`，createTime 已由 038c 加注解），模块 JSON 仍输出 `createdAt` 已修但 `updatedAt` 未修的半对称态 `{createTime, updatedAt}`。前端模块列表（`IndexView` 模块表）不读 updateTime（YAGNI 前端零改动），本单价值=① 用户裁决对称收口、② 消除跨域 JSON 键不一致（`{createTime, updateTime}` 全站统一）、③ 消除未来前端读模块 updateTime 的再踩坑面。

**拆分说明**：web/040b 原范围「ModuleVO 收尾 + R1 案A 架构分页键名勘误」共约 9 文件（4 段实现 + 5 处文档）超 6 熔断，按 planner.md 第 3 步拆两段——**040b1 模块域 updateTime（本单，4 文件）** / 040b2 R1 案A 文档勘误（`docs/01-architecture.md:46` + 4 个 controller Javadoc `data={list,...}` → `{records,...}`，5 文件，用户裁决案A 在案）。一次只注册首段。

**同步点**：JSON 层 `updatedAt` 断言全盘点仅 `ModuleControllerTest:146`（模块列表）/`:177`（模块详情）两处（076 R2 盘点已核、`:219` 权限域已随 040a 改），本单就地同步两处。`ModuleFieldContractTest`（038c 交付）只断 `createTime` 不断 `updatedAt`（156-180 实测）→ 加注解零回退。

**架构对照（无冲突）**：§3 数据库字段不动；§4 时间 ISO-8601 既有；模块接口响应外壳不变（`ModuleVO` 对象内键名演进，与 038c/039/040a/040c 先例一致）。

## 需求描述

`ModuleVO.updatedAt` 组件加 `@JsonProperty("updateTime")`，使模块列表与详情响应输出键 `updatedAt`→`updateTime`，四 VO（user/role/permission/module）JSON 形态统一为 `{createTime, updateTime}`。扩展 `TimestampFieldContractTest` 新增模块域用例（**不改动既有三域四处循环数组**，新增独立模块域列表+详情断言）；同步 `ModuleControllerTest:146/:177` 两处断言键名；smoke 追加 `web/040b1` 用例。

## 验收标准（TDD 驱动）

- [ ] AC1 — 模块列表 JSON 契约：`GET /api/v1/modules` 分页响应列表项含 `updateTime`（非空 ISO-8601）且不含 `updatedAt`/`createdAt` 键
- [ ] AC2 — 模块详情 JSON 契约：`GET /api/v1/modules/{id}` 详情 data 含 `updateTime`（非空 ISO-8601）且不含 `updatedAt` 键
- [ ] AC3 — 既有零回退：`ModuleFieldContractTest` 3 例（038c createTime 契约）保持全绿；`TimestampFieldContractTest` 5 例（039/040a/040c）保持全绿；`ModuleControllerTest` 仅 `:146/:177` 两处同步后 11 例全绿；`mvn -q verify` 失败集=基线逐条一致
- [ ] AC4 — 门禁与冒烟：后端 `mvn -q verify` 基线对照（153 用例 4F+2E → 本单 +1 = 154 预期）；前端零改动三件套=基线（lint 124 / 20 failed|288 passed / build ✓）；`bash scripts/smoke.sh` 新增 `web/040b1` 用例单跑通过、整体=基线逐条（59 → 60 预期）

### AC1 — 模块列表契约
> Given 模块种子数据非空。When `GET /api/v1/modules`。Then 首项含 `updateTime` 键（非空 ISO-8601）且不含 `updatedAt` 与 `createdAt` 键。

**用例**：`AC1 ← 用例 TimestampFieldContractTest#moduleListsAndDetailOutputUpdateTime`

### AC2 — 模块详情契约
> Given 列表可取首项 id。When `GET /api/v1/modules/{id}`。Then data 含 `updateTime` 键（非空 ISO-8601）且不含 `updatedAt` 键。

**用例**：`AC2 ← 用例 TimestampFieldContractTest#moduleListsAndDetailOutputUpdateTime`（同用例详情段；列表/详情为同键名契约的两形态）

### AC3 — 既有零回退
> Given 本单注解与两处同步。When 全量后端测试。Then 038c/039/040a/040c 契约类全绿、mvn 失败集=基线六条。

**用例**：`AC3 ← 既有 ModuleFieldContractTest 3 例 + TimestampFieldContractTest 5 例 + ModuleControllerTest 11 例 + mvn -q verify 基线对照`

### AC4 — 门禁与冒烟
> ① mvn 基线对照零新增。② 前端零改动三件套=基线。③ smoke 新增 `web/040b1` 单跑通过、整体=基线逐条。

**用例**：`AC4 ← mvn -q verify + npm 三件套 + bash scripts/smoke.sh#web/040b1`

## 测试清单

> 扩展既有 `TimestampFieldContractTest`（现 190 行，+1 用例预估 ~215 ≤500）；先于实现写出留 RED 证据。**既有四处三域循环数组（:79/:104/:128/:157）禁改**。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | TimestampFieldContractTest#moduleListsAndDetailOutputUpdateTime | AC1/AC2 | 待填（预期 RED：模块现输出 `updatedAt`） |
| 2 | 门禁基线对照（mvn 153+1=154 预期 4F+2E；038c/039/040a/040c 契约类全绿；前端零改动） | AC3/AC4 | 待填 |
| 3 | 冒烟 web/040b1 定向测试（追加 scripts/smoke.sh） | AC4-③ | 待填 |

**行数红线**：`TimestampFieldContractTest` 扩展后 ≤500；`ModuleVO` 增量后 ≪500；`ModuleControllerTest`（现 ~500 内）仅 2 行键名替换。

## RED 证据

> Generator 于实现前执行测试清单 #1 并粘贴关键失败输出（实时留痕，不得事后补记）。

```text
（待 Generator 填写）
```

> 实现后复跑与 REFACTOR 复查（实时留痕）。

```text
（待 Generator 填写）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 153 用例 4F+2E → 本单 +1 | 待填 | 待填 |
| `npm run lint` | EXIT=124 挂起基线 | 待填 | 待填 |
| `npm run test` | 20 failed\|288 passed（前端零改动） | 待填 | 待填 |
| `npm run build` | ✓ | 待填 | 待填 |
| `bash scripts/smoke.sh` | 59=48✅+9❌+2⏭️ → 预期 60 | 待填 | 待填 |

## 拆分说明

预估验收标准 4 条；预估文件变更 **4 个（≤6 不拆分）**：

1. `backend/.../dto/module/ModuleVO.java` — `updatedAt` 加 `@JsonProperty("updateTime")`（AC1/AC2）
2. `backend/.../controller/ModuleControllerTest.java`:146/:177 — 两处断言键 `updatedAt`→`updateTime` 同步（模块域，仅此两处）（AC3）
3. `backend/.../controller/TimestampFieldContractTest.java` — 新增模块域列表+详情用例（测试先行，独立用例不改既有四处数组）（AC1/AC2）
4. `scripts/smoke.sh` — 追加 `web/040b1` 用例（AC4-③）

**超限熔断**：>6 文件或 >4 AC 即停止回报。**禁改清单**：既有四处三域循环数组（TimestampFieldContractTest:79/104/128/157）、`ModuleFieldContractTest`（038c 交付零回退）、`dto/user|role|permission` 三 VO 与 `dto/auth/*`（已完成段）、`docs/01-architecture.md` 与 4 个 controller Javadoc（**R1 案A 归 040b2**）、迁移/entity/MetaObjectHandler、**前端全部文件**。

**研究项（Generator 先于写用例核实）**：
- **R1 同步点终核**：`grep -rn 'path("updatedAt")' backend/src/test/` 应仅命中 `ModuleControllerTest:146/:177` 两处（Planner 已实测），实现后复核零遗漏。
- **R2 零回退预判**：`ModuleFieldContractTest` 不断 `updatedAt`（156-180 已核）——加注解后应仍 3/3；`ModuleControllerTest` 两处同步前 `path("updatedAt")` 对缺失键返回 `MissingNode`（`asText()=""` 非 null 故 assertNotNull 不红）——同步是**断言有效性修复**（040a :219 同款），RED 用例先行锁定。
- **R3 模块端点与种子**：`GET /api/v1/modules` 分页（ModuleController:49）+ `/{id}`（:86）；种子模块非空由 038c 用例既有数据保证（同款取列表首项 id 模式）。
- **R4 smoke 形态**：沿 `web/039/040/040c` 先例 `mvn -q -f "$APP_DIR/pom.xml" test -Dtest='TimestampFieldContractTest'`。
- **R5（040b2 预研登记）**：R1 文档五处精确清单已锁定——`docs/01-architecture.md:46`、`ModuleController.java:46`、`PermissionController.java:41`、`RoleController.java:42`、`UserController.java:47`，勘误口径 `{records,total,size,current,...}`（用户裁决案A），供 040b2 规划直接引用；另 `docs/superpowers/plans/2026-08-26-harness-system.md:198` 同款历史文档**不动**（历史计划文档不维护）。

**用户裁决在案**：四 VO 全站对称（040a 三域 + 040c me + 040b1 模块域）、R1 案A（归 040b2）——Evaluator 评审按此口径。

## 交付物（4 文件）

1. `dto/module/ModuleVO.java` — updateTime 注解
2. `ModuleControllerTest.java` — :146/:177 两处同步
3. `TimestampFieldContractTest.java` — 新增模块域用例（测试先行）
4. `scripts/smoke.sh` — web/040b1 冒烟用例

## 评审记录

- （待 Evaluator）
