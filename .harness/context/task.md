# Sprint 工作单：sprint-100

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-100 |
| 所属模块 | **roles** |
| 功能点 ID | **roles/003** |
| 功能点名称 | 角色分配权限「权限不存在」错误码撞车修复（冒烟仅剩 2 ❌ 的根因清偿） |
| 状态 | **DONE** |
| 创建时间 | 2026-10-15 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| roles/002 | 角色-权限分配 API（本单修复其 AC2 错误码口径缺陷） | ✅ |

## 业务背景

**根因（Planner 2026-10-15 只读取证）**：`RoleServiceImpl:182` 用 `BusinessException(1201, "权限不存在")`，但 1201 在 Permission 域=「权限名已存在」（PermissionControllerTest 锁定 409+1201 三条断言）；`GlobalExceptionHandler` 按 code 映射 → 1201→**409 CONFLICT**。而 roles/002 当期 AC2 契约=「权限不存在返回 **400**」（`RoleControllerTest:466` `status().isBadRequest()+code 1201`）→ 400 vs 409 恒红。

**冒烟影响**：roles-001（RoleControllerTest,RoleServiceTest）与 roles-002（assignPermissions 四方法）**❌ 同根**——均为 466 这一条断言。修复后冒烟 **❌ 由 2 降 0（首次全绿 SMOKE PASSED）**，mvn 基线 **4F+2E→3F+2E**（RoleController 项移出豁免清单）。

**Planner 定夺（handler 注释明示「通用错误码口径 Planner 定夺登记于工作单」）**：**保 HTTP 400、错误码 1201→400**——理由：
1. AC2 原契约（400）是 roles/002 当期登记的需求，本单修的是「错误码撞车」不是「状态码契约」；
2. 先例=PermissionServiceImpl 两处 `BusinessException(400, "module_id 不存在")`（关联引用非法=参数校验失败→400，与 handler 口径注释「参数校验失败 400、不侵占资源段」一致）；权限分配传入不存在的 permissionId 同属「请求体引用非法」；
3. 否决案 A（对齐 1001→404）：改 HTTP 契约（400→404）+须改方法名 Returns400→404 + 连带改 smoke roles-002 引用——动静大且无消费方依赖支撑；否决案 C（新造 1204→默认 400）：无 AC 依据凭空造码。

## 验收标准（TDD 驱动）

- [ ] AC1 — 测试先行 RED：`RoleControllerTest` AC2 段（:441-466）`$.code` 断言 1201→**400**（status 400 保留、方法名 `assignPermissionsInvalidPermissionReturns400` 保留→smoke roles-002 引用零改）→ 定向跑 **必红**：旧实现 1201→409，`Status expected:<400> but was:<409>`。← 用例 `AC1 ← 用例 mvn -Dtest='RoleControllerTest#assignPermissionsInvalidPermissionReturns400' 红`
- [ ] AC2 — 实现 GREEN：`RoleServiceImpl:182` `BusinessException(1201, "权限不存在")`→`BusinessException(400, "权限不存在")` + `RoleService.java:71` javadoc `code=1201 权限不存在`→`code=400 权限不存在`（引用非法）→ 定向绿：`RoleControllerTest,RoleServiceTest` 15 用例 0 失败、`assignPermissions*` 四方法全绿。← 用例 `AC2 ← 用例 mvn -Dtest='RoleControllerTest,RoleServiceTest' 绿`
- [ ] AC3 — 门禁基线更新：`mvn -q verify` 新基线 **168=3F+2E**（四类豁免=DataSourceConfigBinding×2、SeedData `sys_role 22≠2`、TestLayersSpec/TestUtilsSpec Docker 2E；**RoleController:466 项移出豁免清单**）；前端三线=基线（lint 0E|166W、test 324/324、build ✓）——前端零动零回归（frontend 无 1201/409 角色分配依赖，已 grep 实证）。← 用例 `AC3 ← 用例 五线基线对照`
- [ ] AC4 — 冒烟全绿：`bash scripts/smoke.sh` → **0❌**（roles-001/002 ❌→✅），`SMOKE PASSED（77 用例）`、⏭=infra-004/model-006 不变、✅ 计数=75（77-2⏭，用 `grep -c "✅ 通过"` 精确取证）。← 用例 `AC4 ← 用例 bash scripts/smoke.sh 首次 SMOKE PASSED`

### AC1 — 测试先行（RED）

**步骤与期望**：改 :461 断言 `.andExpect(jsonPath("$.code").value(1201))` → `value(400)`；javadoc「Then status=400、code=1201」→「code=400」。跑 `mvn -q test -Dtest='RoleControllerTest#assignPermissionsInvalidPermissionReturns400'` 期望失败（当前 impl 409/1201）。

**用例**：`AC1 ← 用例 定向红`

### AC2 — 实现（GREEN）

**步骤与期望**：impl :182 code 1201→400、RoleService javadoc 同步；定向跑 15 用例全绿。

**用例**：`AC2 ← 用例 定向绿`

### AC3/AC4 — 门禁与冒烟

**用例**：`AC3/AC4 ← 用例 五线+全量冒烟`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | `RoleControllerTest#assignPermissionsInvalidPermissionReturns400` 定向红（改断言后、改 impl 前） | AC1 | **✅ 红**：`Status expected:<400> but was:<409>`（1 用例 1 失败） |
| 2 | `mvn -q test -Dtest='RoleControllerTest,RoleServiceTest'` 15/15 绿 | AC2 | **✅ 15/15**（9+6，0 失败 0 错误，surefire 报告实证） |
| 3 | `mvn -q verify` 168=3F+2E（新豁免四类） | AC3 | **✅ failures=3+errors=2**：F=DataSourceConfigBinding×2+SeedData、E=TestLayers/TestUtils；**RoleControllerTest 移出失败清单** |
| 4 | 前端三线 0E\|166W / 324 / ✓ | AC3 | **✅ 0E\|166W / 41 文件 324/324 / ✓ 17.10s**（零动=基线） |
| 5 | `bash scripts/smoke.sh` 0❌ SMOKE PASSED（77） | AC4 | **✅ 首次全绿 `SMOKE PASSED（77 用例）`**：精确计数 ✅=75、❌=0、⏭=2（infra-004/model-006） |

## RED 证据

```text
[修复前红态 2026-10-15] AC1 断言 1201→400 后、impl 未改：mvn -Dtest='RoleControllerTest#assignPermissionsInvalidPermissionReturns400'
→ Tests run: 1, Failures: 1 —— java.lang.AssertionError: Status expected:<400> but was:<409>（旧实现 code1201→409 撞车实证）
[GREEN] impl 1201→400 后：RoleControllerTest 9/9 + RoleServiceTest 6/6 = 15 用例 0 失败 0 错误（surefire XML 实证）
```

## 门禁与冒烟记录

> Generator 亲测填写。历史基线：mvn 168=4F+2E（本单目标 3F+2E）、冒烟 77=73✅+2❌+2⏭（本单目标 0❌）。

| 门禁项 | 本单后基线 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 168=**3F**+2E（RoleController 项移出） | 3F=DataSourceConfigBinding×2+SeedData、2E=TestLayers/TestUtils | ✅ 新基线达成 |
| `npm run lint` | 0E\|166W | 0E\|166W | ✅=基线 |
| `npm run test` | 41 文件 324/324 | 41/324 全绿 | ✅=基线 |
| `npm run build` | ✓ | ✓ 17.10s | ✅=基线 |
| `bash scripts/smoke.sh` | **0❌**（77 用例、⏭=2、SMOKE PASSED） | `SMOKE PASSED（77 用例）`：✅=75、❌=0、⏭=2 | ✅ **首次全绿** |

## 实现说明

**实现要点**：
1. test 段：RoleControllerTest :461 断言 1201→400 + javadoc → 定向红（物证）。
2. feat 段：RoleServiceImpl:182 code 1201→400；RoleService.java:71 javadoc 同步 → 定向绿 → 五线 → 全量冒烟 0❌。
3. 基线口径迁移：后续所有单的 mvn 基线=168=3F+2E、冒烟基线=0❌（Evaluator 回写时在 session-state 最近更新登记新口径）。

**超限熔断**：改动 >3 业务文件、动 `GlobalExceptionHandler` 映射表、动 PermissionControllerTest、动 smoke.sh 用例区、改方法名（破坏 smoke roles-002 引用）→ 停止回报。

**不动清单**：`GlobalExceptionHandler` 1201→409 映射（Permission 域锁定）；PermissionServiceImpl 1001/400 语义；smoke.sh 用例区（roles-002 引用方法名不变）；application.yml.template D 态（永不入提交）；前端全部。

## 交付物（3 业务文件）

1. `backend/src/test/java/com/authcore/controller/RoleControllerTest.java` — AC2 段断言 code 1201→400 + javadoc
2. `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java` — :182 code 1201→400
3. `backend/src/main/java/com/authcore/service/RoleService.java` — :71 javadoc 同步

### 新增
（无）

### 修改
- `backend/src/test/java/com/authcore/controller/RoleControllerTest.java`
- `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java`
- `backend/src/main/java/com/authcore/service/RoleService.java`

### 删除
（无）

### 变更计数

业务文件 **3**；smoke/前端/handler 零动；mvn 豁免五类→四类、冒烟 ❌ 2→0。

## 评审意见

**sprint-100 评审通过 — 均分 9.6/10**（2026-10-15 Evaluator）

| 维度 | 分 | 依据 |
|------|----|------|
| 需求符合 | 10 | 4 AC 亲核全满足：AC1 定向红 `expected:<400> but was:<409>`（撞车实证）、AC2 定向绿 EXIT=0（15/15）、AC3 mvn 新基线 **3F+2E**（RoleControllerTest 不在失败清单）+前端三线=基线（0E\|166W/324/✓17.23s）、AC4 全量冒烟复跑 **`SMOKE PASSED（77 用例）`** ✅=75/❌=0/⏭=2 |
| 设计/实现 | 10 | 错误码口径定夺（400=参数校验失败，module_id 先例）裁决**正确**：保 AC2 HTTP 契约、避撞 409 已锁码、方法名保留→smoke 引用零改；3 文件 5+/5- 最小集 |
| 门禁/回归 | 10 | Evaluator 亲跑五线全绿：mvn 3F+2E（F=DataSourceConfigBinding×2+SeedData、E=TestLayers/TestUtils=豁免四类逐条）、前端零动零回归；`git diff 659ec21..b3f0e33 --stat` 实证恰 3 文件 |
| 证据/物证 | 9 | RED→GREEN 可复现（test 段独立提交先行）、surefire XML 计数实证、冒烟精确计数 75+0+2=77；扣分=mvn 计数双口径未本单澄清（历史「168」vs surefire 解析 193，预存计数法差异 -0.6，建议后续登记统一口径） |
| 沟通/流程 | 9 | 两段式+docs `3ad4fbe`+不动清单 0 行实证（scripts/frontend/common diff=0）；扣分=冒烟 stderr 噪声（Mockito agent 警告）混排影响取证可读性（-0.4，挂起区登记治理建议） |

**六一票否决**：零命中（无夹带、无跳门禁、无 force push、无跳 Evaluator、无基线漂移未报、无文档缺失；不动清单零触碰）。

**扣分项（-0.4）**：① mvn 用例计数双口径（168 历史口径 vs 193 解析口径）未在本单登记统一（-0.25）；② 冒烟日志 stderr 噪声（-0.15，建议 smoke.sh 将测试命令 stderr 收敛入日志文件，另案）。

**结论**：PASS。roles/003 收口——冒烟**首次 0❌ 全绿**、mvn 豁免五类→四类；新基线口径（mvn 3F+2E、冒烟 77=75✅+2⏭）自本单生效。后续候选：web/036c2b 冗余覆盖清理（>6 问案B）、web/036c3 串联 :deep 42 处、web/045 BaseInput、冒烟 stderr 噪声治理（挂起区）。
