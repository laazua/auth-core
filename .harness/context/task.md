# Sprint 工作单：sprint-075

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-075 |
| 所属模块 | web |
| 功能点 ID | web/039 |
| 功能点名称 | 创建时间字段全站对齐（user/role/permission 域）——JSON 输出 `createTime` 与前端读法收敛 |
| 状态 | PLANNED |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/004 | 用户管理列表页（创建时间列读 `row.createTime`） | ✅ |
| web/005 | 角色管理列表页（创建时间列读 `row.createTime`） | ✅ |
| web/006 | 权限管理列表页（创建时间列读 `row.createTime`） | ✅ |
| web/038c | 模块域字段错位修复·同方案先例（ModuleVO `@JsonProperty("createTime")` 方案 B 用户裁定在案，sprint-072 通过） | ✅ |

## 业务背景

2026-09-29 web/038c 规划调研发现的同源遗留，registry 127 行已注册占位（防遗漏）。现状实证（2026-09-29 本单规划只读核对）：

- **后端**：`UserVO`/`RoleVO`/`PermissionVO` 三 record 的 `LocalDateTime createdAt` **均无 Jackson 注解** → JSON 输出键 `createdAt`；模块域 `ModuleVO` 已由 038c 加 `@JsonProperty("createTime")` → 输出 `createTime` ✓。
- **前端**：`types/user.ts`、`types/role.ts`、`types/permission.ts` 及五处页面列（`users/IndexView`、`roles/IndexView`、`system/IndexView` 权限列、`profile/IndexView` 等）全部读 `row.createTime`。
- **缺陷**：user/role/permission 三域列表与详情响应给 `createdAt`、前端读 `createTime` → `undefined` → 三张列表页创建时间列与个人中心创建时间**恒显 `—`/空**。

**方案（Planner 定案，038c 方案 B 同构延伸，登记如下）**：三 VO 各加 `@JsonProperty("createTime")`，**后端对齐前端既有读法，前端零改动**。与 038c 模块域修复完全同构（sprint-072 方案 B 用户裁定在案），全局 JSON 时间字段名收敛为 `createTime` 单一形态。备选的「前端改读 createdAt」被否：需改 3 types + 5 处页面引用 + 前端多 spec，且与模块域已定的 `createTime` 输出分叉为两种形态。

**架构对照（无冲突）**：§3「字段名与约束为契约」指数据库字段（`created_at`），JSON 输出键名不在其列；§4 只规定时间值为 ISO-8601（UTC+8），本单不改值格式仅改键名；§4 Result/分页结构零改动。**方案 B 先例已由用户裁定过（038c），本单同构延伸不再新增裁量**；如用户对方案有异议可在评审前推翻。

**范围边界（YAGNI）**：不改数据库/实体/MetaObjectHandler（Java 层 `createdAt()` 访问器零改动）；不改前端任何文件；不动 `ModuleAccessibleVO`（本就不含时间管理字段）；不做创建时间筛选参数（`types/user.ts` 的 `createTimeStart/End` 是查询入参，属另域）。

## 需求描述

后端 `UserVO`、`RoleVO`、`PermissionVO` 三个 record 的 `createdAt` 组件加 `@JsonProperty("createTime")`，使 user/role/permission 三域**列表与详情**接口的 JSON 输出键由 `createdAt` 变为 `createTime`，与前端既有读法对齐，消除三张列表页创建时间恒显 `—` 缺陷。新增契约测试类锁定「`createTime` 存在且 `createdAt` 不存在」。既有测试中断言模块域 `createdAt` 键的遗留点（研究项 R2）就地同步（038c 先例：既有测试就地改零增行）。

## 验收标准（TDD 驱动）

- [ ] AC1 — 三域列表 JSON 契约：`GET /api/v1/users`、`GET /api/v1/roles`、`GET /api/v1/permissions` 分页响应 data 内列表项含 `createTime`（非空 ISO-8601 字符串）且**不含** `createdAt` 键
- [ ] AC2 — 三域详情 JSON 契约：`GET /api/v1/users/{id}`、`GET /api/v1/roles/{id}`、`GET /api/v1/permissions/{id}` 详情响应 data 含 `createTime` 且不含 `createdAt` 键
- [ ] AC3 — 模块域零回退：038c 已修的模块域契约测试 `ModuleFieldContractTest`（列表+详情 `createTime` 存在/`createdAt` 不存在 3 断言组）保持全绿；`mvn -q verify` 失败集与基线逐条一致零新增（研究项 R2 命中点就地同步后仍为基线口径）
- [ ] AC4 — 门禁与冒烟：后端 `mvn -q verify` 基线对照零新增；前端零改动、三件套与基线一致（20 failed 基线不动）；`bash scripts/smoke.sh` 新增 `web-039` 单跑通过、整体失败/跳过与基线逐条一致（当前基线 56 用例 45✅+9❌+2⏭️，新增后 57）

### AC1 — 三域列表契约
> Given 三域均有已建数据（种子/测试数据）。When 请求三域分页列表接口。Then 列表项 `createTime` 存在且非空、`createdAt` 不存在。

**用例**：`AC1 ← 用例 TimestampFieldContractTest#listsOutputCreateTimeForUserRolePermissionDomain`

### AC2 — 三域详情契约
> Given 三域各一条已知记录。When 请求详情接口。Then data `createTime` 存在且 `createdAt` 不存在。

**用例**：`AC2 ← 用例 TimestampFieldContractTest#detailOutputsCreateTimeForUserRolePermissionDomain`

### AC3 — 模块域与既有测试零回退
> Given 038c 已修模块域契约。When 全量跑后端测试。Then `ModuleFieldContractTest` 保持全绿、`mvn -q verify` 失败集=基线六条（4F+2E）零新增。

**用例**：`AC3 ← 既有 ModuleFieldContractTest 保持通过 + mvn -q verify 基线对照`（R2 同步点若需改则就地改断言键，属同步非新功能）

### AC4 — 门禁与冒烟
> ① `mvn -q verify` 失败集=基线 148 用例 4F+2E 零新增。② 前端零文件改动，`npm run test` failed 保持基线 20、build 通过、lint EXIT=124 基线。③ `bash scripts/smoke.sh` 新增 `web-039` 用例单跑通过，整体失败/跳过=基线逐条。

**用例**：`AC4 ← mvn -q verify 基线对照 + npm 三件套 + bash scripts/smoke.sh#web-039`

## 测试清单

> Generator 按 `.harness/rules/tdd-workflow.md` 先于实现写出并运行留 RED 证据；每条验收标准至少一例。新 spec 集中于新建契约测试类 `TimestampFieldContractTest`（对齐 038c 的 `ModuleFieldContractTest` 先例）。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | TimestampFieldContractTest#listsOutputCreateTimeForUserRolePermissionDomain | AC1 | 待填（预期 RED：现输出 `createdAt` 键） |
| 2 | TimestampFieldContractTest#detailOutputsCreateTimeForUserRolePermissionDomain | AC2 | 待填（预期 RED：同上） |
| 3 | 门禁基线对照（mvn 148 4F+2E；ModuleFieldContractTest 保持全绿；前端零改动基线不动） | AC3/AC4 | 待填 |
| 4 | 冒烟 web-039 定向测试（追加 scripts/smoke.sh） | AC4-③ | 待填 |

**行数红线**：新测试类 ≤500 行（038c 的 ModuleFieldContractTest 181 行先例，预估 ~150）；三 VO 各 +2 行（import + 注解），`UserVO`/`RoleVO`/`PermissionVO` 增量后均 ≪500。

## RED 证据

> Generator 于实现前执行测试清单 #1~#2 并粘贴关键失败输出（实时留痕，不得事后补记）。

```text
（待 Generator 填写）
```

> 实现后复跑（实时留痕）。REFACTOR 复查：行宽 ≤120、分层、新测试类行数达标、既有测试零改动（R2 同步点除外，就地改断言键）。

```text
（待 Generator 填写）
```

## 门禁与冒烟记录

> Generator 亲测填写（后端 mvn -q verify / 前端三件套 / scripts/smoke.sh），Evaluator 不采信自述须亲跑。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 148 用例 4F+2E（零新增） | 待填 | 待填 |
| `npm run lint` | EXIT=124 挂起基线 | 待填 | 待填 |
| `npm run test` | 20 failed\|288 passed（074 后基线） | 待填 | 待填 |
| `npm run build` | ✓ | 待填 | 待填 |
| `bash scripts/smoke.sh` | 56=45✅+9❌+2⏭️ → 预期 57 | 待填 | 待填 |

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 **5~6 个（≤6 不拆分）**：

1. `backend/src/main/java/com/authcore/dto/user/UserVO.java` — `createdAt` 加 `@JsonProperty("createTime")`（AC1/AC2）
2. `backend/src/main/java/com/authcore/dto/role/RoleVO.java` — 同上（AC1/AC2）
3. `backend/src/main/java/com/authcore/dto/permission/PermissionVO.java` — 同上（AC1/AC2）
4. `backend/src/test/java/com/authcore/controller/TimestampFieldContractTest.java` — **新增**：列表+详情两用例（测试先行）（AC1/AC2）
5. `scripts/smoke.sh` — 追加 `web-039` 用例（+5 行左右，不删不改既有用例）（AC4-③）
6. （条件）`ModuleControllerTest.java` — 研究项 R2 命中时就地同步断言键（038c 既有测试同步先例，占第 6 顶格）

**超限熔断**：实际变更超过 6 文件即停止回报 Planner/用户。**禁改清单**：数据库迁移、entity、`MybatisPlusMetaObjectHandler`、`ModuleVO`/`ModuleFieldContractTest`（038c 交付物，仅 AC3 回归对象）、**前端全部文件**（本单前端零改动是方案二的核心收益）、`ModuleAccessibleVO`。

**研究项（Generator 先于写 AC 用例核实）**：
- **R1 分页响应键名**：架构 §4 写 `data{list,total,page,size}`，而 038c 的 `ModuleFieldContractTest:173` 断言 `$.data.records[0]`——两者不一致属预存。Generator 实测三域分页真实键名后写断言；若确认架构与实现不一致，登记 session-state 挂起区（不属本单修复范围）。
- **R2 `ModuleControllerTest:218`**：`item.path("createdAt")` 断言的归属端点核实——若为模块域序列化输出断言则 038c 遗漏点、就地同步为 `createTime`（占第 6 文件）；若为 Java 属性/其他语义则不动并登记说明。
- **R3 详情端点存在性**：核实 `GET /users/{id}`、`/roles/{id}`、`/permissions/{id}` 三详情端点路径与鉴权前置（AC2 可测性）。
- **R4 冒烟形态**：照抄 `scripts/smoke.sh` 后端定向用例既有形态（如 model-008 的 mvn 定向跑 + grep 计数口径）。

**历史债口径（沿 074 用户裁定）**：本单不触碰 IndexView.vue/spec（前端零改动），历史债拆分仍挂起区待排期。

## 交付物（预估 5~6 文件）

1. `UserVO.java` — 输出键 `createTime`
2. `RoleVO.java` — 输出键 `createTime`
3. `PermissionVO.java` — 输出键 `createTime`
4. `TimestampFieldContractTest.java` — 两用例（新增，测试先行）
5. `scripts/smoke.sh` — web-039 冒烟用例
6. （条件）`ModuleControllerTest.java` — R2 断言同步

## 评审记录

- （待 Evaluator）
