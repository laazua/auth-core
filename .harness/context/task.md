# Sprint 工作单：sprint-079

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-079 |
| 所属模块 | web |
| 功能点 ID | web/040b2（web/040b 拆分末段：R1 案A 架构分页键名勘误） |
| 功能点名称 | R1 案A 文档勘误——架构 §4 与 4 个 controller Javadoc 分页 data 口径对齐实现 |
| 状态 | PLANNED |
| 创建时间 | 2026-09-29 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/040b1 | 模块域 updateTime 收尾（040b 拆分首段，评审通过 9.7/10） | ✅ |
| R1 案A 裁决 | 用户裁决：文档对齐实现（案A），非实现迁就文档（案B） | ✅ 在案 |

## 业务背景

R1 口径错位（sprint-066 发现、070/072 复述、075/076/077/078 规划连续登记）：架构文档与 controller Javadoc 声明分页 `data={list,total,page,size}`，实现实际为 MyBatis-Plus `IPage` 序列化 `{records,total,size,current,...}`（测试断言 `data.records`/`data.total` 多处、契约用例均以 records 为准）。**用户裁决案A**（2026-09-29）：文档对齐实现，零行为变更。040b 系列（040a 三域 / 040c me / 040b1 模块域）行为侧已全部收口，本单为**纯文档收尾段**：修 5 处声明、终清残留面、关闭 R1 问题单。

**拆分说明**：040b→040b1（已 ✅）+ 040b2（本单）两段系 9 文件超 6 熔断产物；本单 6 文件恰在上限（不得再加）。

## 需求描述

将 5 处 `data={list,total,page,size}` 勘误为实现口径 `data={records,total,size,current,...}`：

1. `docs/01-architecture.md:46` — 分页接口 data 声明行（架构 §4 权威口径）
2. `backend/src/main/java/com/authcore/controller/ModuleController.java:46` — `@return 分页结果：data={list,total,page,size}`
3. `backend/src/main/java/com/authcore/controller/PermissionController.java:41` — 同款
4. `backend/src/main/java/com/authcore/controller/RoleController.java:42` — 同款
5. `backend/src/main/java/com/authcore/controller/UserController.java:47` — 同款

**不动清单**：`docs/superpowers/plans/2026-08-26-harness-system.md:198`（历史计划文档，不维护——已登记）；分页参数 page/size 请求语义描述保持（勘误仅 data 载荷字段）；实现代码零改动（Javadoc 注释行）；前端零改动。

## 验收标准（TDD 驱动）

- [ ] AC1 — 架构 §4 口径勘误：`docs/01-architecture.md:46` 分页 data 声明改为 `{records,total,size,current,...}`（保留 page/size 请求参数说明）
- [ ] AC2 — 四 Javadoc 同步：Module/Permission/Role/User 四 controller `@return 分页结果：data={list,total,page,size}` → `data={records,total,size,current,...}`（仅注释行，方法体零改动）
- [ ] AC3 — 残留面终清（TDD 载体）：`grep -rn 'data={list,total,page,size}' docs/01-architecture.md backend/src/main/java/com/authcore/controller/` 零命中（历史计划文档 198 除外且不计入）；实现口径实测核对一次分页响应 JSON 字段集与文档一致（若实测与裁决措辞有出入，以实测为准并回写 task 登记）
- [ ] AC4 — 门禁与冒烟：后端 `mvn -q verify` **153 用例 4F+2E=基线**（纯注释改动 +0 用例，失败六条逐条=基线）；前端零改动三件套=基线（lint124 / 20 failed|288 passed / build ✓）；`bash scripts/smoke.sh` 新增 `web/040b2` 用例（残留面 grep 零命中断言）单跑通过、整体=**61=50✅+9❌+2⏭️**（基线 60 + 新用例 1，9❌ 逐条=基线）

### AC1 — 架构 §4
> Given 案A 裁决在案。When 编辑 `docs/01-architecture.md` §4 分页行。Then data 载荷声明为 `{records,total,size,current,...}`，page/size 请求参数说明保留。

**用例**：`AC1 ← 用例 smoke#web/040b2（grep 零残留覆盖架构行）`

### AC2 — 四 Javadoc
> When 替换四处 `@return` 注释行。Then 与架构口径一致、方法体与 @param 零改动。

**用例**：`AC2 ← 用例 smoke#web/040b2 + git diff 仅注释行核验`

### AC3 — 残留面终清
> When 全库 grep 指定串。Then docs+controller 零命中（历史计划文档 198 例外）；实测分页 JSON 字段集与文档一致。

**用例**：`AC3 ← 用例 smoke#web/040b2`

### AC4 — 门禁与冒烟
> mvn 153=基线、前端=基线、冒烟 61 逐条=基线+新用例 ✅。

**用例**：`AC4 ← mvn -q verify + npm 三件套 + bash scripts/smoke.sh#web/040b2`

## 测试清单

> 纯文档段 TDD 载体=冒烟用例先行（grep 断言 RED→改五处→GREEN）。

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | smoke `web/040b2` 残留面 grep 零命中（追加 scripts/smoke.sh，先行留 RED：5 处命中=失败） | AC1/AC2/AC3 | 待填 |
| 2 | 门禁基线对照（mvn 153 4F+2E=基线；前端零改动三件套=基线） | AC4 | 待填 |
| 3 | 冒烟整体复跑（预期 61=50✅+9❌+2⏭️ 逐条=基线） | AC4 | 待填 |

## RED 证据

> Generator 于改文档前执行测试清单 #1 并粘贴关键输出（grep 命中 5 处=RED 实证）。

```text
[RED] smoke web/040b2 单跑 sh -c '! grep -rn "{list,total,page,size}" docs/01-architecture.md backend/.../controller/' → EXIT=1，命中 5 处（架构:46 + Module:46/Permission:41/Role:42/User:47），历史 198 在扫描面外
[RED] 设计修正留痕：首版 pattern "data={...}" 漏架构行（data 固定为 … 非 data=），改裸串 {list,total,page,size} 后 5 处全命中
[R1 实测] ObjectMapper 序列化 MyBatis-Plus Page{current=1,size=10,total=42,records=[rec]} → {"records":["rec"],"total":42,"size":10,"current":1,"pages":5} —— 实测五字段 {records,total,size,current,pages}，与案A 裁决措辞 {records,total,size,current,...} 一致（省略号=pages），文档按实测写全五字段
[RED] 执行时点：2026-09-29 实现前（TDD 第 1 拍），smoke 用例先行 EXIT=1
```

> 实现后复跑与 REFACTOR 复查。

```text
（待 Generator 填写）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 153 用例 4F+2E（+0） | 待填 | 待填 |
| `npm run lint` | EXIT=124 | 待填 | 待填 |
| `npm run test` | 20 failed\|288 passed | 待填 | 待填 |
| `npm run build` | ✓ | 待填 | 待填 |
| `bash scripts/smoke.sh` | 60=49✅+9❌+2⏭️ → 预期 61 | 待填 | 待填 |

## 拆分说明

预估验收标准 **4 条（=上限，不得再加）**；预估文件变更 **6 个（=上限，不得再加，超限即触发熔断回报）**：

1. `docs/01-architecture.md` — §4 分页行勘误（AC1）
2. `ModuleController.java` — `:46` 注释行（AC2）
3. `PermissionController.java` — `:41` 注释行（AC2）
4. `RoleController.java` — `:42` 注释行（AC2）
5. `UserController.java` — `:47` 注释行（AC2）
6. `scripts/smoke.sh` — 追加 `web/040b2` 残留面 grep 断言用例（AC3 载体）

**超限熔断**：>6 文件或 >4 AC 即停止回报。**不动清单**：`docs/superpowers/plans/2026-08-26-harness-system.md:198`（历史计划）、实现代码体/@param/@Operation、`docs/01-architecture.md` 其余行（§3/§4 时间行/§5 等）、前端全部、四个 VO、harness 规则文件。

**研究项（Generator 先于改写核实）**：
- **R1 实测口径**：跑一次分页请求（或读现有测试日志）实测 `data` JSON 字段集——MyBatis-Plus IPage 预期 `{records,total,size,current,pages,...}`；若实测字段集与裁决措辞 `{records,total,size,current,...}` 不一致，**以实测为准**改文档并回写 task 登记（案A 本质=对齐实现）。
- **R2 smoke 形态**：既有 `smoke_case` 命令形态核对——grep 断言用 `! grep -q ... || exit 1` 或 `test -z "$(grep ...)"` 形式，RED=先加用例跑出失败（命中 5 处），改后 GREEN。
- **R3 残留面全扫**：`grep -rn "list,total,page,size"` 全库确认除五处与历史 198 外零命中（防漏登记新残留）。
- **R4 diff 纯注释核验**：四 Java 文件 `git diff` 仅 `@return` 行变动（+1/-1×4），确保 mvn 编译零风险。

**用户裁决在案**：案A 文档对齐实现（040b2 唯一范围）；历史计划文档不动。Evaluator 评审按此口径。

## 交付物（6 文件）

1. `docs/01-architecture.md` — §4 分页口径
2-5. 四 controller Javadoc — `@return` 行
6. `scripts/smoke.sh` — web/040b2 用例

## 评审记录

- （待 Evaluator）
