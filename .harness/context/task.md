# Sprint 工作单：sprint-072

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-072 |
| 所属模块 | web |
| 功能点 ID | web/038c |
| 功能点名称 | 模块服务统一入口（c 段）——`baseUrl`/`createTime` 字段错位修复（模块域，038d/038e 硬前置） |
| 状态 | PLANNED |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| modules/001 | 模块 CRUD 基建（ModuleCreateDTO/ModuleUpdateDTO/ModuleVO 与既有测试载荷/断言所在，本单就地修复其字段契约） | ✅ |
| model/004 | sys_module 表模型（base_url/created_at 为**表列**契约，本单不改列只改 JSON 映射） | ✅ |
| web/002 | 前端模块管理页 ModuleFormDrawer（提交形状 `baseUrl` 来源，`ModuleFormDrawer.vue:33,183`） | ✅ |
| web/003 | 模块列表页字段列（读取形状 `createTime` 来源，`IndexView.vue:133`、`types/module.ts:10,22`） | ✅ |
| web/038b | GET /modules/accessibles（精简 VO 已按设计第 3 节用 camel `baseUrl` 输出，sprint-071 评审通过 9.0/10——本单向其对齐） | ✅ |

## 业务背景

用户原始需求（sprint-069/071 一组）：模块管理中的「模块」是外部运行服务，管理员授权后登录用户应看到并访问自己的模块。038d「我的模块」页与 038e iframe 视图依赖模块管理页维护 `baseUrl`（内嵌跳转地址来源）与列表页展示创建时间。只读调研（2026-09-29，Planner 亲核两侧源码）确认两处静默错位：

1. **输入侧**：前端提交 `baseUrl`（`ModuleFormDrawer.vue:33` 表单模型、`:183` 表单项），后端 `ModuleCreateDTO.java:22` / `ModuleUpdateDTO.java:14` 标注 `@JsonProperty("base_url")` —— Jackson 默认忽略未知字段，UI 新建/编辑提交的 `baseUrl` 被静默丢弃，永 never 入库；后端既有测试载荷恰用 `base_url`（`ModuleControllerTest:242,277`）故测试恒绿、缺陷只在真实 UI 路径暴露。
2. **输出侧**：后端 `ModuleVO.java:15` 输出 `createdAt`，前端全站类型层与列表列读 `createTime`（`types/module.ts:10,22`、`IndexView.vue:133` formatter 读 `row.createTime`）—— 模块表「创建时间」列恒显 `—`。

**方向裁决**（架构 §3 冲突判定：§3 的 `base_url`/`created_at` 是**数据库表列**契约，本单不改任何表列；JSON 契约架构未规定，全站前端 6 个类型文件与 2 处列表列均用 camel `baseUrl`/`createTime`，新 `ModuleAccessibleVO`（038b）与全部后端响应（`ModuleVO.baseUrl`、Login/Me 等）已是 camel —— JSON 侧收敛 camel 是最小改动且与设计第 3 节 `baseUrl` 输出先例一致）。

**范围裁定**：同源系统性错位影响 user/role/permission 域（`UserVO/RoleVO/PermissionVO` 均输出 `createdAt` 而前端对应类型均读 `createTime`，三张列表页创建时间同显 `—`）—— 修复这三个 VO + 三份测试类将超 6 文件预算，且非 038d/038e 硬前置，**登记独立占位 web/039（⬜ 待规划）**，本单只修模块域。

## 需求描述

模块域 JSON 契约收敛 camel：`POST /api/v1/modules` 与 `PUT /api/v1/modules/{id}` 请求体接受 `baseUrl`（与前端提交形状一致，修复静默丢弃）；`GET /api/v1/modules` 与 `GET /api/v1/modules/{id}` 响应输出 `createTime`（不再输出 `createdAt`，与前端列表列/类型直读一致）。后端 Java 侧 `ModuleVO` 记录组件随输出改名（构造位置参数不变，唯一构造点 `ModuleServiceImpl:182` 零改动；`.createdAt()` 访问器无调用面，调研 grep 实证）。旧 `base_url` 输入载荷不再保留兼容（Jackson 默认忽略未知字段 → 与今日前端遭遇同构，登记为接受的契约收敛；其唯二消费者是本就要改的既有测试载荷）。前端零改动。

## 验收标准（TDD 驱动）

- [ ] AC1 — `POST /api/v1/modules` 请求体以 `baseUrl`（camel，同 `ModuleFormDrawer` 提交形状）提交 → 200，随后 `GET /api/v1/modules/{id}` 回读 `baseUrl` 等于提交值（修复静默丢弃）
- [ ] AC2 — `PUT /api/v1/modules/{id}` 请求体以 `baseUrl` 提交 → 200，回读 `baseUrl` 等于更新值
- [ ] AC3 — `GET /api/v1/modules` 列表项与 `GET /api/v1/modules/{id}` 详情输出 `createTime` 字段、**不再输出** `createdAt`（前端 `IndexView.vue:133` 直读可用）
- [ ] AC4 — 后端门禁零新增失败（基线对照）；前端门禁不适用（零前端改动，基线对照留痕）；冒烟 `web-038c` 单跑通过、整体失败与基线逐条一致

### AC1 — 创建入参 camel `baseUrl` 正确入库回读
> Given 管理员登录。When `POST /api/v1/modules` 载荷 `{name, code, "baseUrl": "http://camel-input", description, status}`（camel，同前端提交形状）。Then 200、`$.code=0`，且 `GET /api/v1/modules/{新 id}` 的 `$.data.baseUrl` 等于 `http://camel-input`（不再被静默丢弃）。

**用例**：`AC1 ← 用例 ModuleFieldContractTest#createAcceptsCamelBaseUrlAndReadsBack`

### AC2 — 更新入参 camel `baseUrl` 正确入库回读
> Given 管理员登录且存在模块。When `PUT /api/v1/modules/{id}` 载荷含 `"baseUrl": "http://camel-updated"`。Then 200、`$.code=0`，详情 `$.data.baseUrl=http://camel-updated`。

**用例**：`AC2 ← 用例 ModuleFieldContractTest#updateAcceptsCamelBaseUrlAndReadsBack`

### AC3 — 响应输出 `createTime` 且不再输出 `createdAt`
> Given 管理员登录。When `GET /api/v1/modules` 与 `GET /api/v1/modules/{id}`。Then 列表项与详情均含 `createTime`（ISO-8601 字符串）且 **不含** `createdAt` 键。

**用例**：`AC3 ← 用例 ModuleFieldContractTest#listAndDetailSerializeCreateTimeNotCreatedAt`

### AC4 — 门禁与冒烟通过
> ① `mvn -q verify` 失败集与实现前基线逐条一致零新增（2026-09-29 口径：145 用例 4F+2E，sprint-071 实测；本单 +3 用例 → 预期 148 4F+2E）。② 本功能点零前端改动，前端门禁不适用（基线对照实测留痕：lint 124 挂起 / test 20 failed|279 passed / build ✓）。③ `bash scripts/smoke.sh` 新增 `web-038c` 单跑通过、整体失败与基线逐条一致零新增（基线：53 用例 9 失败）。④ 既有 `ModuleControllerTest` 就地同步后（2 处载荷 `base_url`→`baseUrl`、3 处断言 `createdAt`→`createTime`）仍全绿（11/11），净行数不变。

**用例**：`AC4 ← mvn -q verify 基线对照 + bash scripts/smoke.sh#web-038c + ModuleControllerTest 回归`

## 测试清单

> Generator 按 `.harness/rules/tdd-workflow.md` 先于实现写出并运行留 RED 证据；每条验收标准至少一例。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | ModuleFieldContractTest#createAcceptsCamelBaseUrlAndReadsBack | AC1 | 待填（预期 RED：载荷 `baseUrl` 被忽略 → 回读为空） |
| 2 | ModuleFieldContractTest#updateAcceptsCamelBaseUrlAndReadsBack | AC2 | 待填（预期 RED：同上） |
| 3 | ModuleFieldContractTest#listAndDetailSerializeCreateTimeNotCreatedAt | AC3 | 待填（预期 RED：响应有 `createdAt` 无 `createTime`） |
| 4 | ModuleControllerTest 就地同步（载荷/断言改 camel 后回归 11/11） | AC4-④ | 待填（与实现同步进行，须全绿） |
| 5 | 冒烟 web-038c 定向测试（追加于 scripts/smoke.sh，定向跑 ModuleFieldContractTest+ModuleControllerTest） | AC4-③ | 待填 |
| 6 | 后端全量门禁基线对照（实现前基线：145 用例 4F+2E，2026-09-29 sprint-071 实测） | AC4-①② | 待填（预期 148 4F+2E 逐条一致） |

**行数红线**：`ModuleControllerTest` 现 **499 行**——本单**只许就地替换、禁止增行**（新增方法一律入新测试类 `ModuleFieldContractTest`，预估 ≤140 行，红线 ≤500）；若逼近红线立即回报 Planner，禁止删改既有用例。

## RED 证据

> Generator 于实现前执行测试清单 #1~#3 并粘贴关键失败输出（实时留痕）。

```text
（待 Generator 填写）
```

## GREEN 证据

> 实现后复跑（实时留痕）。REFACTOR 复查：行宽 ≤120、record 组件名与 JSON 名一致、DTO 注解删净（含未用 import）、既有测试 11/11、方法/文件行数达标（ModuleFieldContractTest ≤500、ModuleControllerTest 499 不变）。

```text
（待 Generator 填写）
```

## 门禁与冒烟记录

> Generator 亲测填写（后端 mvn -q verify / 前端门禁基线对照 / scripts/smoke.sh），Evaluator 不采信自述须亲跑。

（待 Generator 填写）

## 拆分说明

预估验收标准 4 条（未超 4 条）；预估文件变更 **6 个（顶格）**：

1. `backend/src/main/java/com/authcore/dto/module/ModuleCreateDTO.java` — 删除 `@JsonProperty("base_url")`（组件名 `baseUrl` 即为默认 JSON 名）+ 同步 `@Size` message 文案 + 删未用 import（AC1）
2. `backend/src/main/java/com/authcore/dto/module/ModuleUpdateDTO.java` — 同上（AC2）
3. `backend/src/main/java/com/authcore/dto/module/ModuleVO.java` — record 组件 `createdAt`→`createTime`（位置参数构造不变，`ModuleServiceImpl:182` 零改动；JSON 输出键随组件名变）（AC3）
4. `backend/src/test/java/com/authcore/controller/ModuleControllerTest.java` — **就地替换 5 处**（载荷 `base_url`→`baseUrl` ×2、断言 `createdAt`→`createTime` ×3）+ 注释文案，净行数不增（499 保持）
5. `backend/src/test/java/com/authcore/controller/ModuleFieldContractTest.java` — **新增**测试类（AC1-AC3 三方法 + 助手，方法级 Javadoc + `@DisplayName` + AAA，文件惯例恢复）
6. `scripts/smoke.sh` — 追加 `web-038c` 用例（+2 行，不删除、不改动既有用例，先例 web/038b）

**超限熔断**：实现中若实际变更文件超过 6 个，Generator 必须停止实现并回报 Planner 重新拆分，不得自行扩范围。`UserVO/RoleVO/PermissionVO` 及任何前端文件（038d/038e 范围）、表列名（架构 §3）、`ModuleAccessibleVO`、SecurityConfig 一律不动。

## 交付物（预估 6 文件）

1. `backend/src/main/java/com/authcore/dto/module/ModuleCreateDTO.java` — 入参 camel 化
2. `backend/src/main/java/com/authcore/dto/module/ModuleUpdateDTO.java` — 入参 camel 化
3. `backend/src/main/java/com/authcore/dto/module/ModuleVO.java` — 出参 `createTime`
4. `backend/src/test/java/com/authcore/controller/ModuleControllerTest.java` — 就地同步（不增行）
5. `backend/src/test/java/com/authcore/controller/ModuleFieldContractTest.java` — AC1-AC3 新用例（新增）
6. `scripts/smoke.sh` — web-038c 冒烟用例

## 变更清单

### 新增
- `backend/src/test/java/com/authcore/controller/ModuleFieldContractTest.java` — 模块字段契约测试类（预估 ~130 行）

### 修改
- `backend/src/main/java/com/authcore/dto/module/ModuleCreateDTO.java`:21-23 — 删 `@JsonProperty("base_url")`、message 文案 `base_url`→`baseUrl`、清 import
- `backend/src/main/java/com/authcore/dto/module/ModuleUpdateDTO.java`:13-15 — 同上
- `backend/src/main/java/com/authcore/dto/module/ModuleVO.java`:15 — 组件 `createdAt`→`createTime`
- `backend/src/test/java/com/authcore/controller/ModuleControllerTest.java`:105,145,176,218,225,242,277,294 — 载荷/断言/注释就地 camel 化（净行数 499 不变）
- `scripts/smoke.sh` — 追加 `web-038c` 冒烟用例（+2 行，未动既有用例）

合计 6 文件 = 预算顶格，未超熔断。

### 删除
- （无）

## 规范检查清单

- [ ] `mvn -q verify` 后端门禁零新增失败（预期 148 用例 4F+2E 与基线逐条一致）
- [ ] 前端门禁不适用（零前端改动；lint 124/test 20 failed/build ✓ 基线对照留痕）
- [ ] `bash scripts/smoke.sh` web-038c ✅；54 用例 9 失败与基线逐条一致
- [ ] 符合 Java 21 / Spring Boot 3 编码规范与既有分层约定（record 组件名 = JSON 名，无冗余注解与未用 import）
- [ ] 符合 TDD 工作流（RED 3 failed 实时留痕 → GREEN → REFACTOR 复查）
- [ ] 变更文件 6 个 = 预算顶格，未超熔断
- [ ] `ModuleFieldContractTest` ≤500 行；`ModuleControllerTest` 499 行**零增行**

## 评审记录

- （待 Evaluator）
