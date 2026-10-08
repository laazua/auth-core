# Sprint 工作单：sprint-079

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-079 |
| 所属模块 | web |
| 功能点 ID | web/040b2（web/040b 拆分末段：R1 案A 架构分页键名勘误） |
| 功能点名称 | R1 案A 文档勘误——架构 §4 与 4 个 controller Javadoc 分页 data 口径对齐实现 |
| 状态 | IMPLEMENTED（待评审） |
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

- [x] AC1 — 架构 §4 口径勘误：`docs/01-architecture.md:46` 分页 data 声明改为 `{records,total,size,current,...}`（保留 page/size 请求参数说明）
- [x] AC2 — 四 Javadoc 同步：Module/Permission/Role/User 四 controller `@return 分页结果：data={list,total,page,size}` → `data={records,total,size,current,...}`（仅注释行，方法体零改动）
- [x] AC3 — 残留面终清（TDD 载体）：`grep -rn 'data={list,total,page,size}' docs/01-architecture.md backend/src/main/java/com/authcore/controller/` 零命中（历史计划文档 198 除外且不计入）；实现口径实测核对一次分页响应 JSON 字段集与文档一致（若实测与裁决措辞有出入，以实测为准并回写 task 登记）
- [x] AC4 — 门禁与冒烟：后端 `mvn -q verify` **154 用例 4F+2E=基线**（078 后基线 153+1；本单 +0，失败六条逐条=基线；原工作单 153 系旧口径笔误，此处置勘误）；前端零改动三件套=基线（lint124 / 20 failed|288 passed / build ✓）；`bash scripts/smoke.sh` 新增 `web/040b2` 用例（残留面 grep 零命中断言）单跑通过、整体=**61=50✅+9❌+2⏭️**（基线 60 + 新用例 1，9❌ 逐条=基线）

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
| 1 | smoke `web/040b2` 残留面 grep 零命中（追加 scripts/smoke.sh，先行留 RED：5 处命中=失败） | AC1/AC2/AC3 | RED→GREEN ✓ |
| 2 | 门禁基线对照（mvn 154 4F+2E=078后基线；前端零改动三件套=基线） | AC4 | ✓ |
| 3 | 冒烟整体复跑（预期 61=50✅+9❌+2⏭️ 逐条=基线） | AC4 | ✓ 61=50✅+9❌+2⏭️ |

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
[GREEN] 残留断言单跑 sh -c '! grep -rn "{list,total,page,size}" …' → EXIT=0（0 命中）
[GREEN] R4 diff 核验：四 controller 4 insertions/4 deletions 全为 @return 注释行，方法体/@param/@Operation 零变动；架构仅 §4 分页行 1±
[GREEN] mvn -q verify EXIT=1：Tests run 154, Failures 4, Errors 2 —— 六条逐条=078 后基线（工作单 153 旧口径已勘误，本单 +0 用例）
[GREEN] 前端三件套=基线（lint124 / 20 failed|288 passed / build ✓ 16.86s / diff=0）；冒烟 61=50✅+9❌+2⏭️ 逐条=基线，web/040b2 ✅
[GREEN] 执行时点：2026-09-29 实现后（TDD 第 2 拍）
```

## 门禁与冒烟记录

> Generator 亲测填写，Evaluator 不采信自述须亲跑。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 154 用例 4F+2E（078 后基线，+0） | 154 4F+2E 六条逐条=基线 | ✓ PASS |
| `npm run lint` | EXIT=124 | EXIT=124 | ✓=基线 |
| `npm run test` | 20 failed\|288 passed | 20 failed\|288 passed (308)，6 failed\|30 passed (36) | ✓=基线 |
| `npm run build` | ✓ | ✓ built in 16.86s | ✓=基线 |
| `bash scripts/smoke.sh` | 60=49✅+9❌+2⏭️ → 预期 61 | 61=50✅+9❌+2⏭️，9❌逐条=基线，web/040b2 ✅ | ✓ PASS |

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

**2026-09-29 Evaluator（sprint-079）：PASS — 平均 9.8/10（10+10+9.5+9.5+10，无否决项）**

### 验收标准核对
- AC1 ✓ 架构 §4:46 → `{records,total,size,current,pages} 五个字段`，page/size 请求说明保留 — diff ±1 实证
- AC2 ✓ 四 Javadoc `@return` 同步 — git diff 仅 ±4 注释行，方法体/@param/@Operation 零变动
- AC3 ✓ 残留面终清 — 亲跑 `sh -c '! grep -rn "{list,total,page,size}" …'` EXIT=0；RED checkout 1c06c49 复跑 5 命中 EXIT=1；历史 198 零触碰；补扫 README/docs 其余 md 与前端零漏改面；R1 实测五字段与文档一致
- AC4 ✓ 门禁 — mvn **154 4F+2E** 六条逐条=078 后基线（本单 +0）；前端 lint124/20f|288p/build ✓ diff=0；冒烟 **61=50✅+9❌+2⏭️** 逐条=基线、web/040b2 ✅

### 关键实证
- 三段式：028ef15 plan → 1c06c49 test(RED) → 6be3838 feat(GREEN) → 1ad408e docs，违规 0
- RED 真实性：checkout 1c06c49 亲跑 5 命中 EXIT=1，回 1ad408e 干净态
- **TDD 价值实证**：pattern 首版 `data={...}` 漏架构行（非 `data=` 语法），若无先行 RED 即漏改第 5 处——Generator 自查自纠并留痕
- 案A 完整性：裁决措辞 `{records,total,size,current,...}` + ObjectMapper 实测补全 `pages`，以实测为准（工作单 R1 授权）；工作单 153 旧基线笔误 Generator 主动勘误为 154

### 评分表（review-criteria 5 维）
| 维度 | 分 | 依据 |
|------|----|------|
| 需求完成度 | 10 | 五处零漏（grep 终清+全库补扫）、实测五字段对齐、历史 198 精确保留、R1 四轮登记问题单关闭 |
| TDD/提交质量 | 10 | grep 载体先行、RED 复跑实证、pattern 缺陷被 RED 揪出、三段式规范 |
| 测试/冒烟 | 9.5 | smoke 断言设计可用、61 逐条基线；扣 0.5：断言扫描面=两路径，全库防漏依赖人工 R3（可扩为全库+排除表） |
| 文档回写 | 9.5 | task RED/GREEN/门禁/AC/基线勘误齐全、三处回写、唯一动作；扣 0.5：feat 混入 task 更新（P1 连续第二单） |
| 诚实规范 | 10 | pattern 缺陷与基线笔误均主动留痕/勘误，无自宣布、无弱化 |

### 结论
**PASS**。Veto 1-6 均不命中（veto 6：mvn/冒烟/前端三线逐条=基线）。**web/040 组全收口**（040a 9.4 / 040c 9.5 / 040b1 9.7 / 040b2 9.8）。

### 问题列表
- P1（低）feat commit 含 task.md 更新，feat/docs 边界混同（连续第二单，建议后续门禁记录独立 docs 段）
- P2（低）smoke 残留断言仅扫 docs/01-architecture + controller 两路径
- P3（信息）Planner 工作单基线沿用 078 前 153 旧口径（Generator 勘误，跨单基线同步机制缺）

### 改进建议
- Planner 写工作单时基线引用上一单 eval 登记口径（153→154 同步）
- 下轮排期：Evaluator 积压六项评审（sprint-047/048/060/061/062/063），或 IndexView 拆分/036b/c+011/modules-003
