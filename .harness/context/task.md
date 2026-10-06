# Sprint 工作单：sprint-076

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-076 |
| 所属模块 | web |
| 功能点 ID | web/040a（web/040 拆分首段） |
| 功能点名称 | 更新时间字段对齐·三域（user/role/permission）——JSON 输出 `updateTime` 与前端读法收敛 |
| 状态 | PLANNED |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/038c | createTime 方案 B 用户裁定先例（模块域） | ✅ |
| web/039 | createTime 三域对齐（sprint-075，`TimestampFieldContractTest` 契约基座交付） | ✅ |

## 业务背景

sprint-075（web/039）评审时 Evaluator 改进建议②新发现的同族缺陷（registry 已登记）。现状实证（2026-09-29 Planner 只读盘点）：

- **后端**：四 VO 的 `updatedAt` 组件均**无 Jackson 注解** → JSON 输出键 `updatedAt`；后端代码 `updateTime` 键**零出现**。
- **前端**：仅 3 处读 `updateTime`——`types/auth.ts:10 updateTime?: string`、`types/user.ts:12 updateTime?: string`、`profile/IndexView.vue:92 userInfo.updateTime`（数据源 `/auth/me` → `MeResponse.user` 内嵌 `UserVO`）→ 个人中心「更新时间」恒空。
- 当前模块/三域 JSON 已混杂形态 `{createTime, updatedAt}`（038c/075 只修了 create 侧）。

**用户裁决（2026-09-29 问答在案）**：① 修复范围=**四 VO 全站对称**（输出统一 `{createTime, updateTime}`）；② 方案=沿 038c/075 方案 B 同构——后端 VO 加 `@JsonProperty("updateTime")`、**前端零改动**；③ R1 架构分页键名不一致选**案A（文档对齐实现）**。

**拆分说明（planner.md 第 3 步：4 VO+测试+同步+smoke=7 文件超 6 熔断）**：web/040 拆两段——**040a 三域 updateTime（本单，6 文件顶格）** / 040b 模块域 ModuleVO updateTime（JSON 断言同步点 ModuleControllerTest:146/177 归属）+ R1 案A 架构分页键名文档勘误（`docs/01-architecture.md:46` + 4 个 controller Javadoc `data={list,...}` 失实同步）。一次只注册首段。

**架构对照（无冲突）**：§4 只规定时间值 ISO-8601 格式、未规定 JSON 键名；方案 B 已两经用户裁定（038c/075）；§3 数据库 `updated_at` 字段不动。**JSON 键名不在 §3 契约列**。

**范围边界（YAGNI）**：不动数据库/entity/MetaObjectHandler；不改前端任何文件；ModuleVO 与 R1 文档留 040b（本单禁改）；不做 updateTime 查询入参。

## 需求描述

后端 `UserVO`、`RoleVO`、`PermissionVO` 三 record 的 `updatedAt` 组件加 `@JsonProperty("updateTime")`，使三域列表/详情及 `/auth/me`（内嵌 UserVO）响应输出键 `updatedAt`→`updateTime`，修复个人中心「更新时间」恒空并将三域 JSON 形态收敛为 `{createTime, updateTime}`。扩展既有契约测试 `TimestampFieldContractTest`（075 交付）新增 updateTime 两用例；既有 JSON 断言同步点 `ModuleControllerTest:219`（权限列表 `path("updatedAt")`）就地同步为 `updateTime`（075 R2 同步先例）。`ModuleControllerTest:146/177` 为模块域断言（ModuleVO）**禁改**，归 040b。

## 验收标准（TDD 驱动）

- [ ] AC1 — 三域列表 JSON 契约：`GET /api/v1/users`、`/roles`、`/permissions` 分页响应列表项含 `updateTime`（非空 ISO-8601）且**不含** `updatedAt` 键；`createTime` 既有契约保持（同项并存断言）
- [ ] AC2 — 三域详情与 me JSON 契约：三域 `GET /{id}` 详情 data 含 `updateTime` 不含 `updatedAt`；`GET /auth/me` 响应 `data.user` 含 `updateTime` 不含 `updatedAt`（个人中心修复面锁定，落地 075 改进建议④）
- [ ] AC3 — 既有零回退：075 的 `TimestampFieldContractTest` createTime 断言保持全绿；`ModuleFieldContractTest` 3 例保持全绿；`ModuleControllerTest` 仅 :219 一处同步（:146/:177 模块域禁改保持）；`mvn -q verify` 失败集=基线逐条一致
- [ ] AC4 — 门禁与冒烟：后端 `mvn -q verify` 基线对照（150 用例 4F+2E → 本单 +2 = 152 预期）；前端零改动三件套=基线（20 failed|288 passed）；`bash scripts/smoke.sh` 新增 `web/040` 用例单跑通过、整体=基线逐条（57 → 58 预期）

### AC1 — 三域列表契约
> Given 三域种子数据非空。When 请求三域分页列表。Then 首项同时含 `createTime` 与 `updateTime` 键、不含 `createdAt` 与 `updatedAt` 键。

**用例**：`AC1 ← 用例 TimestampFieldContractTest#listsOutputCreateTimeAndUpdateTimeForThreeDomains`

### AC2 — 三域详情与 me 契约
> Given 三域各一条已知记录且管理员已登录。When 请求三域详情与 `/auth/me`。Then 详情 data 与 `me.data.user` 均含 `updateTime` 不含 `updatedAt`。

**用例**：`AC2 ← 用例 TimestampFieldContractTest#detailAndMeOutputUpdateTimeForThreeDomains`

### AC3 — 既有零回退
> Given 本单注解与同步。When 全量后端测试。Then 075/038c 契约类全绿、ModuleControllerTest 仅 :219 变更、mvn 失败集=基线六条。

**用例**：`AC3 ← 既有 TimestampFieldContractTest createTime 断言 + ModuleFieldContractTest 3 例 + mvn -q verify 基线对照`

### AC4 — 门禁与冒烟
> ① mvn 基线对照零新增。② 前端零改动三件套=基线。③ smoke 新增 `web/040` 单跑通过、整体=基线逐条。

**用例**：`AC4 ← mvn -q verify + npm 三件套 + bash scripts/smoke.sh#web/040`

## 测试清单

> 扩展既有 `TimestampFieldContractTest`（075 交付，114 行，加 2 用例预估 ~160 ≤500）；先于实现写出留 RED 证据。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | TimestampFieldContractTest#listsOutputCreateTimeAndUpdateTimeForThreeDomains | AC1 | 待填（预期 RED：现输出 `updatedAt`） |
| 2 | TimestampFieldContractTest#detailAndMeOutputUpdateTimeForThreeDomains | AC2 | 待填（预期 RED：同上） |
| 3 | 门禁基线对照（mvn 150+2=152 预期 4F+2E；075/038c 契约类全绿；前端零改动） | AC3/AC4 | 待填 |
| 4 | 冒烟 web/040 定向测试（追加 scripts/smoke.sh） | AC4-③ | 待填 |

**行数红线**：`TimestampFieldContractTest` 扩展后 ≤500；三 VO 增量后 ≪500。

## RED 证据

> Generator 于实现前执行测试清单 #1~#2 并粘贴关键失败输出（实时留痕，不得事后补记）。

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
| 后端 `mvn -q verify` | 150 用例 4F+2E → 本单 +2 | 待填 | 待填 |
| `npm run lint` | EXIT=124 挂起基线 | 待填 | 待填 |
| `npm run test` | 20 failed\|288 passed（前端零改动） | 待填 | 待填 |
| `npm run build` | ✓ | 待填 | 待填 |
| `bash scripts/smoke.sh` | 57=46✅+9❌+2⏭️ → 预期 58 | 待填 | 待填 |

## 拆分说明

预估验收标准 4 条；预估文件变更 **6 个（顶格）**：

1. `backend/.../dto/user/UserVO.java` — `updatedAt` 加 `@JsonProperty("updateTime")`（AC1/AC2）
2. `backend/.../dto/role/RoleVO.java` — 同上（AC1/AC2）
3. `backend/.../dto/permission/PermissionVO.java` — 同上（AC1/AC2）
4. `backend/.../controller/TimestampFieldContractTest.java` — 扩展 2 用例（测试先行）（AC1/AC2）
5. `backend/.../controller/ModuleControllerTest.java`:219 — 权限列表断言键 `updatedAt`→`updateTime` 就地同步（**仅此一处；:146/:177 模块域禁改**）（AC3）
6. `scripts/smoke.sh` — 追加 `web/040` 用例（AC4-③）

**超限熔断**：>6 文件即停止回报。**禁改清单**：`ModuleVO`、`ModuleFieldContractTest`（038c 交付）、`ModuleControllerTest:146/177`（模块域断言，归 040b）、`docs/01-architecture.md` 与 4 个 controller Javadoc（R1 案A 勘误归 040b）、迁移/entity/MetaObjectHandler、**前端全部文件**。

**研究项（Generator 先于写用例核实）**：
- **R1 me 响应实测**：登录后 `GET /auth/me` 实际 JSON 形状（`data.user.updateTime` 键现缺失的 RED 实证与 GREEN 复测）。
- **R2 同步点终核**：`grep -rn 'path("updatedAt")' backend/src/test/` 应仅命中 ModuleControllerTest:146/177/219 三处（Planner 已实测），实现后复核 219 之外零波及。
- **R3 smoke 形态**：沿 `web/039` 先例 `mvn -q -f "$APP_DIR/pom.xml" test -Dtest='TimestampFieldContractTest'`。
- **R4（040b 预研登记）**：模块域 `path("updatedAt")` 断言 146/177 与 R1 文档 5 处（architecture:46 + 4 Javadoc）清单已锁定，供 040b 规划直接引用。

**用户裁决在案**：四 VO 全站对称（040a 三域 + 040b ModuleVO）、方案 B 同构、R1 案A——均为 2026-09-29 问答记录，Evaluator 评审按此口径。

## 交付物（6 文件）

1. `UserVO.java` / 2. `RoleVO.java` / 3. `PermissionVO.java` — 输出键 `updateTime`
4. `TimestampFieldContractTest.java` — 扩展 2 用例（测试先行）
5. `ModuleControllerTest.java` — :219 单点同步
6. `scripts/smoke.sh` — web/040 冒烟用例

## 评审记录

- （待 Evaluator）
