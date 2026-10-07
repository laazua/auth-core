# Sprint 工作单：sprint-077

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-077 |
| 所属模块 | web |
| 功能点 ID | web/040c（web/040 拆分第三段，me 面） |
| 功能点名称 | 个人中心创建/更新时间修复——`/auth/me` 的 user 输出 createTime/updateTime |
| 状态 | DONE（Evaluator 通过 2026-09-29，平均分 9.5/10，零否决项） |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/004 | `GET /api/v1/auth/me`（MeResponse 契约所属） | ✅ |
| web/039 | createTime 契约基座（TimestampFieldContractTest 交付） | ✅ |
| web/040a | updateTime 三域对齐（076 评审通过，me 面拆出本单） | ✅ |

## 业务背景

个人中心「创建时间/更新时间」恒空的根因（sprint-076 实现期实证、挂起区 2026-09-29 Generator(sprint-076) 登记在案）：

- `/auth/me` 响应 `data.user` 的实际类型是 **`com.authcore.dto.auth.UserVO`（6 参：id/username/nickname/email/phone/status，无任何时间字段）**——与三域列表/详情用的 `com.authcore.dto.user.UserVO`（8 参带时间）**同名异类**；`AuthServiceImpl:5` import 的是前者，`toUserVO`（:124-133）6 参构造不填时间。
- 前端 `profile/IndexView.vue:88` 读 `userInfo.createTime`、`:92` 读 `userInfo.updateTime`（数据源 `/auth/me`）→ 两字段恒 undefined，个人中心创建/更新时间显示恒空。
- 该修复面在 sprint-075（web/039）评审时被误判为「MeResponse 内嵌 UserVO 自动连带修复」（当时未察觉同名异类）；sprint-076 实现 AC2 me 断言时发现，**用户裁决拆出 web/040c**（问题工具答卷在案）、Evaluator sprint-076 改进建议③建议优先排期。

**范围**：`dto.auth.UserVO` 增加 `createdAt`/`updatedAt` 两参并加 `@JsonProperty("createTime")`/`@JsonProperty("updateTime")` 注解（与 038c/039/040a 方案 B 四连同构）；`AuthServiceImpl.toUserVO` 补两实参（`user.getCreatedAt()`/`user.getUpdatedAt()`，entity getter 已存在 :148/:162）。两键一次修复（只补 updateTime 留 createTime 恒空=半修复）。

**YAGNI 边界**：`dto.auth.RoleVO`（4 参）/`dto.auth.PermissionVO` 同样无时间字段，但 me 响应的 roles/permissions 时间键**前端零消费**（profile 只读 user 面），本单不修——已登记挂起区供后续对称性裁量。不动 `dto.user`/`dto.role`/`dto.permission`/`dto.module` 四域 VO（040a/040b 范围）。

**架构对照（无冲突）**：§3 数据库字段不动（本单仅 JSON DTO 层加字段，entity/迁移零改动）；§4 时间 ISO-8601 既有格式；auth/004 响应 data 结构 `{user,roles,permissions}` 不变（user 对象内新增时间键，与 038c→039→040a 键演进先例一致，响应外壳不变）。如与 `docs/01-architecture.md` 冲突以它为准并登记。

## 需求描述

`dto.auth.UserVO` 补时间两字段与方案 B 注解、`AuthServiceImpl.toUserVO` 填值，使 `GET /api/v1/auth/me` 响应 `data.user` 输出 `createTime`/`updateTime` 键（ISO-8601 非空），修复个人中心两个时间显示恒空；扩展 `TimestampFieldContractTest` 新增 me 契约用例锁定修复面（该用例 RED 实证 `No value at JSON path "$.data.user.updateTime"` 已在 sprint-076 `061817b` 留痕，本单复现并补齐 createTime 面）；smoke 追加 `web/040c` 用例。

## 验收标准（TDD 驱动）

- [x] AC1 — me 时间字段契约：管理员登录后 `GET /api/v1/auth/me` 响应 `data.user` 同时含 `createTime` 与 `updateTime` 键（值非空 ISO-8601 字符串），且不含 `createdAt`/`updatedAt` 键
- [x] AC2 — 既有零回退：040a 的 `TimestampFieldContractTest` 四用例（createTime 列表/详情、updateTime 列表/详情）保持全绿；`ModuleFieldContractTest` 3 例、`ModuleControllerTest` 11 例保持全绿；`mvn -q verify` 失败集=基线逐条一致
- [x] AC3 — 门禁与冒烟：后端 `mvn -q verify` 基线对照（152 用例 4F+2E → 本单 +1 = 153 预期）；前端零改动三件套=基线（lint 124 / 20 failed|288 passed / build ✓）；`bash scripts/smoke.sh` 新增 `web/040c` 用例单跑通过、整体=基线逐条（58 → 59 预期）

### AC1 — me 时间字段契约
> Given 管理员登录持有有效 token。When `GET /api/v1/auth/me`。Then `data.user` 含 `createTime` 与 `updateTime`（非空 ISO-8601）且不含 `createdAt` 与 `updatedAt`。

**用例**：`AC1 ← 用例 TimestampFieldContractTest#meOutputsCreateTimeAndUpdateTime`

### AC2 — 既有零回退
> Given 本单两文件改动。When 全量后端测试。Then 040a 四用例与 038c 契约类全绿、mvn 失败集=基线六条。

**用例**：`AC2 ← 既有 TimestampFieldContractTest 四用例 + ModuleFieldContractTest 3 例 + mvn -q verify 基线对照`

### AC3 — 门禁与冒烟
> ① mvn 基线对照零新增。② 前端零改动三件套=基线。③ smoke 新增 `web/040c` 单跑通过、整体=基线逐条。

**用例**：`AC3 ← mvn -q verify + npm 三件套 + bash scripts/smoke.sh#web/040c`

## 测试清单

> 扩展既有 `TimestampFieldContractTest`（现 167 行，+1 用例预估 ~185 ≤500）；先于实现写出留 RED 证据。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | TimestampFieldContractTest#meOutputsCreateTimeAndUpdateTime | AC1 | GREEN ✓ 5/5 |
| 2 | 门禁基线对照（mvn 152+1=153 预期 4F+2E；040a/038c 契约类全绿；前端零改动） | AC2/AC3 | 通过：mvn 153 4F+2E 失败六条=基线逐条；35 用例定向全绿（Timestamp 5+Auth 16+ModuleField 3+ModuleController 11）；lint 124/test 20 failed\|288/build ✓ |
| 3 | 冒烟 web/040c 定向测试（追加 scripts/smoke.sh） | AC3-③ | 通过：web/040c 单跑 ✅（行7322），整体 59=48✅+9❌+2⏭️ 失败=基线逐条 |

**行数红线**：`TimestampFieldContractTest` 扩展后 ≤500；`dto.auth.UserVO` 增量后 ≪500；`AuthServiceImpl` 现 <500 行、增量 +2 行。

## RED 证据

> Generator 于实现前执行测试清单 #1 并粘贴关键失败输出（实时留痕，不得事后补记）。

```text
[RED] Tests run: 5, Failures: 1, Errors: 0 — com.authcore.controller.TimestampFieldContractTest
  meOutputsCreateTimeAndUpdateTime:185 No value at JSON path "$.data.user.createTime"
  （040a 四用例 createTime/updateTime 列表与详情保持绿；me.user 现无时间键与 076 061817b 留痕一致）
[RED] 执行时点：2026-09-29 实现前（TDD 第 1 拍），mvn -q -f backend/pom.xml test -Dtest='TimestampFieldContractTest' EXIT=1
```

> 实现后复跑与 REFACTOR 复查（实时留痕）。

```text
[GREEN] mvn -q -f backend/pom.xml test -Dtest='TimestampFieldContractTest,AuthControllerTest,ModuleFieldContractTest,ModuleControllerTest' EXIT=0
  TimestampFieldContractTest tests=5 failures=0（me 用例转绿、040a 四用例保持绿）
  AuthControllerTest tests=16 failures=0（R3 预判验证：字段级断言加时间键零回退）
  ModuleFieldContractTest tests=3 failures=0 / ModuleControllerTest tests=11 failures=0（零回退）
[GREEN] 实现要点：① dto.auth.UserVO +2 参（createdAt/updatedAt）+ @JsonProperty("createTime")/("updateTime")；
  ② AuthServiceImpl.toUserVO 补 user.getCreatedAt()/getUpdatedAt() 两实参；③ smoke +2 行 web/040c。
  执行时点 2026-09-29 实现后实时留痕。
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 152 用例 4F+2E → 本单 +1 | **153 用例 4F+2E**（失败六条=基线逐条：DataSourceConfig×2、RoleController:466、SeedData、TestLayers/TestUtils 无 Docker） | 零新增 ✓ |
| `npm run lint` | EXIT=124 挂起基线 | EXIT=124 | =基线 ✓ |
| `npm run test` | 20 failed\|288 passed（前端零改动） | Test Files 6 failed\|30 passed、Tests **20 failed\|288 passed (308)** | =基线 ✓ |
| `npm run build` | ✓ | ✓ built in 19.61s | 通过 ✓ |
| `bash scripts/smoke.sh` | 58=47✅+9❌+2⏭️ → 预期 59 | **59=48✅+9❌+2⏭️**，web/040c 单跑 ✅（行7322），9❌+2⏭️=基线逐条 | 追加 1 用例 ✓ |

## 拆分说明

预估验收标准 3 条；预估文件变更 **4 个（≤6 不拆分）**：

1. `backend/.../dto/auth/UserVO.java` — `createdAt`/`updatedAt` 两参 + `@JsonProperty("createTime")`/`@JsonProperty("updateTime")`（AC1）
2. `backend/.../service/impl/AuthServiceImpl.java` — `toUserVO` 补 `user.getCreatedAt()`/`user.getUpdatedAt()` 两实参（AC1）
3. `backend/.../controller/TimestampFieldContractTest.java` — 新增 me 契约用例（测试先行）（AC1/AC2）
4. `scripts/smoke.sh` — 追加 `web/040c` 用例（AC3-③）

**超限熔断**：>6 文件或 >4 AC 即停止回报。**禁改清单**：`dto/user|role|permission` 三域 VO 与 `dto/module/ModuleVO`（040a/040b 范围）、`dto/auth/RoleVO`/`dto/auth/PermissionVO`（YAGNI：me 面时间键前端零消费，已挂起登记）、`docs/01-architecture.md` 与 4 个 controller Javadoc（R1 案A 归 040b）、迁移/entity/MetaObjectHandler、**前端全部文件**。

**研究项（Generator 先于写用例核实）**：
- **R1 me RED 复现**：现文件无 me 用例（076 收窄时已摘），按 AC1 重写；预期 RED=`No value at JSON path "$.data.user.createTime"`（createTime 先于 updateTime 断言）或 updateTime 缺失，与 `061817b` 留痕一致。
- **R2 构造面终核**：`grep -rn "new UserVO" backend/src/main/java/com/authcore/service/impl/AuthServiceImpl.java` 应仅 `toUserVO` 一处 6 参调用；测试侧若 mock `AuthService`/构造 `MeResponse` 有 6 参依赖（`grep -rn "dto.auth.UserVO\|MeResponse" backend/src/test/`）需同步评估——若测试构造点超本单预算即停止回报。
- **R3 AuthControllerTest 影响面**：`AuthControllerTest` 若断言 me 响应结构（键集合），加键可能致其失败——跑后如需就地同步视为本单文件预算内（第 1/3 文件同源），超预算停止回报。
- **R4 smoke 形态**：沿 `web/039`/`web/040` 先例 `mvn -q -f "$APP_DIR/pom.xml" test -Dtest='TimestampFieldContractTest'`。
- **R5（040b 预研登记）**：`dto.auth.RoleVO`（4 参）/`dto.auth.PermissionVO` 无时间字段、前端零消费——如需对称性补齐由 040b/后续裁量。

**用户裁决在案**：me 面拆 web/040c（2026-09-29 问题工具答卷）、方案 B 同构四连（038c/039/040a/040c）、Evaluator sprint-076 建议优先排期——Evaluator 评审按此口径。

## 交付物（4 文件）

1. `dto/auth/UserVO.java` — 时间两参 + 两注解
2. `service/impl/AuthServiceImpl.java` — `toUserVO` 填值
3. `TimestampFieldContractTest.java` — 新增 me 用例（测试先行）
4. `scripts/smoke.sh` — web/040c 冒烟用例

## 变更清单（Generator 产出）

### 新增
- （无）

### 修改
- `backend/src/main/java/com/authcore/dto/auth/UserVO.java`:12-20 — record 加 `createdAt`/`updatedAt` 两参 + `@JsonProperty("createTime")`/`@JsonProperty("updateTime")`（方案 B 四连），import `JsonProperty`/`LocalDateTime`（AC1）
- `backend/src/main/java/com/authcore/service/impl/AuthServiceImpl.java`:133-134 — `toUserVO` 补 `user.getCreatedAt()`/`user.getUpdatedAt()` 两实参填值（AC1）
- `backend/src/test/java/com/authcore/controller/TimestampFieldContractTest.java`:158-189 + 类 Javadoc — 新增 `meOutputsCreateTimeAndUpdateTime` 用例（测试先行，RED `cb54af0`）、类 Javadoc 扩展至 039/040a/040c 三段覆盖面（AC1/AC2）
- `scripts/smoke.sh`:427-428 — 追加 `web/040c me 面时间字段修复定向测试` 用例（+2 行，既有用例零改动）（AC3-③）

### 删除
- （无）

## 评审记录

- **2026-09-29：✅ 通过（REWORK 0 次，平均分 9.5/10）** — Evaluator 亲测：定向 5+16+3+11 全绿、mvn `153 4F+2E`=基线六条逐条、前端 `20 failed|288 passed`+lint124+build19.84s（前端 diff=0）、冒烟 `59=48✅9❌2⏭️` web/040c ✅（行7322）+smoke 增量 +2 行取证、禁改面 diff=0、4 文件=清单一致零夹带、me 值非空日志实证（createdAt/updatedAt=2026-08-27T11:00:26）、提交链 `cb54af0 test:`→`f37f988 feat:` 物证、AuthServiceImpl:82 超宽行为预存行（diff 零超宽）历史债不计。评分=功能 9.5 / 质量 9.5 / 规范 9.5 / TDD 9.5 / 安全 9.5；六否决项零命中（否决6 沿基线对照裁决）。改进建议 4 条不计分：①me 用例补值断言 ②断言风格统一（连续三轮）③:82 预存超宽行治理 ④**web/040b 建议下轮排期**。

- （待 Evaluator）
