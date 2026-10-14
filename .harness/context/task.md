# Sprint 工作单：sprint-098

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-098 |
| 所属模块 | web |
| 功能点 ID | **web/036c2a**（036c2 拆分：model-008/010 冒烟 ❌ 根因——两 ServiceInterfaceSpec 过期计数同步；冗余覆盖清理另案 036c2b） |
| 功能点名称 | RoleService/ModuleService 接口 Spec 计数断言同步（6→7 / 6→8） |
| 状态 | **AWAITING_REVIEW**（Generator 交付完成 2026-10-14，修复前红态+门禁亲测在案） |
| 创建时间 | 2026-10-14 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| roles/001 | RoleService 接口（listAllPermissions 后续演进新增） | ✅ |
| modules/001 | ModuleService 接口（listAllPermissions+listAccessibles web/038b 新增） | ✅ |
| 048a 先例 | PermissionServiceInterfaceSpec 6→7 同款同步已做，本单=同型漏改补做 | ✅ |

## 业务背景

**冒烟 ❌ 四条之二（model-008/010）根因实测（2026-10-14 只读取证）**：

| 冒烟 ❌ | 失败断言 | 实际接口 |
|--------|---------|---------|
| model-008 | `RoleServiceInterfaceSpec:25 接口应声明 6 个公共方法 ==> expected: <6> but was: <7>` | RoleService 7 方法（+`listAllPermissions`） |
| model-010 | `ModuleServiceInterfaceSpec:24 接口应声明 6 个公共方法 ==> expected: <6> but was: <8>` | ModuleService 8 方法（+`listAllPermissions`+`listAccessibles`） |

**根因**：接口演进新增方法时 Spec 计数断言未同步（048a PermissionService 同款已同步，这两处漏改）。**修复=过期断言同步**（纯测试修复，零业务代码）；修后冒烟 model-008/010 自动转 ✅——❌ 四条降为 2 条（roles-001/002 保留基线）。

**036c2 拆分登记**：原 036c2「冗余覆盖与死代码清理+model-008/010（>6 问案B）」——实测拆出计数同步仅 2 文件，无需问案B；冗余覆盖清理另案 036c2b（预估另核）。

## 验收标准（TDD 驱动）

- [ ] AC1 — RoleSpec 计数同步：`RoleServiceInterfaceSpec` 计数断言 6→7+补 `listAllPermissions` 方法名断言——修复前 6→GREEN（接口已 7，过期断言修复无红-绿行为差，「修复前红态」=model-008 冒烟 ❌ 证据在案）；修后该 Spec 全绿。← 用例 `RoleServiceInterfaceSpec#roleServiceInterfaceHasAllMethods`
- [ ] AC2 — ModuleSpec 计数同步：`ModuleServiceInterfaceSpec` 6→8+补 `listAllPermissions`/`listAccessibles` 断言——同上。← 用例 `ModuleServiceInterfaceSpec#moduleServiceInterfaceHasAllMethods`
- [ ] AC3 — 冒烟 ❌ 消除：冒烟 `model-008`/`model-010` 用例由 ❌ 转 **✅**（定向跑两 Spec 绿）；冒烟总数 77 不变、❌ 由 4 降为 **2**（roles-001/002 保留）；web-052 等既有 ✅ 零回归。← 用例 `AC3 ← 用例 冒烟全量（model-008/010 转 ✅ 断言）`
- [ ] AC4 — 门禁：mvn verify 168 用例 Failures 由 4 降为 **2**（RoleServiceInterfaceSpec/ModuleServiceInterfaceSpec 两红消除，剩 DataSourceConfigBinding×2+RoleController 400but409+SeedData 22≠2=**需核对**——实际 verify 4F 中含此两 Spec？否——verify 基线 4F=DataSourceConfigBinding×2+RoleController+SeedData，Spec 是 `*Spec` 不入 surefire 默认扫描（pom 无 include）→ **verify 168 用例数与 4F+2E 不变**（Spec 仅冒烟 `-Dtest` 拉入）；前端三线不变；lint/test/build 零动。← 用例 `AC4 ← 用例 门禁五线`

### AC1/AC2 — 过期断言同步（过期修复型）

**步骤与期望**：改两 Spec 计数+方法名断言 → 定向跑绿；「修复前红态」证据=smoke model-008/010 ❌ 与 `expected:<6> but was:<7>/<8>` 实测（2026-10-14 Planner 取证在案）。

**用例**：`AC1/AC2 ← 用例 两 Spec 定向跑`

### AC3 — 冒烟 ❌ 消除

**用例**：`AC3 ← 用例 冒烟全量（77 用例 ❌=2）`

### AC4 — 门禁基线

**用例**：`AC4 ← 用例 mvn 168=4F+2E + 前端三线（零动照抄）`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | RoleServiceInterfaceSpec（6→7+listAllPermissions） | AC1 | 待绿（过期同步） |
| 2 | ModuleServiceInterfaceSpec（6→8+两方法断言） | AC2 | 待绿（过期同步） |
| 3 | 冒烟全量 77（model-008/010 ✅、❌=2） | AC3 | 待亲测 |
| 4 | 门禁五线（mvn 168 不变+前端三线） | AC4 | 待亲测 |

## RED 证据

```text
[修复前红态] RoleServiceInterfaceSpec:25 接口应声明 6 个公共方法 ==> expected: <6> but was: <7>  （Planner 2026-10-14 只读取证 + model-008 冒烟 ❌ 实证）
[修复前红态] ModuleServiceInterfaceSpec:24 接口应声明 6 个公共方法 ==> expected: <6> but was: <8>  （同上 + model-010 冒烟 ❌ 实证）
[GREEN] 两 Spec 定向跑 EXIT=0（全绿）
```

## 门禁与冒烟记录

> Generator 亲测（2026-10-14）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 168 用例 4F+2E | **168, Failures: 4, Errors: 2**（Spec 不入 surefire 默认扫描基线照抄） | ✅ 基线 |
| `npm run lint` | 0E\|166W | **0E\|166W**（前端零动） | ✅ 基线 |
| `npm run test` | 41 文件 324/324 | **41 文件 324/324** | ✅ 基线 |
| `npm run build` | ✓ | **✓ built in 17.35s** | ✅ |
| `bash scripts/smoke.sh` | 77=71✅+4❌+2⏭️ | **77=75✅+2❌+2⏭️**——❌ 由 4 降为 **2**（model-008/010 转 ✅ 消除，roles-001/002 保留基线），✅ 含 web-047/049/050/052 零回归 | ✅ **达成 ❌ 4→2** |

**提交物证**：`6a6bf16 test`（2 文件过期计数同步）。后端主代码/接口/冒烟脚本/前端零动；冒烟脚本零改（用例本就跑 Spec，断言绿自动转 ✅）。

## 实现说明

**实现要点**：
1. 两 Spec 计数断言 6→7/6→8+补方法名 `assertTrue`（RoleService+`listAllPermissions`；ModuleService+`listAllPermissions`/`listAccessibles`）——镜像 048a PermissionService 同步先例。
2. 定向跑两 Spec 绿；冒烟全量复核 model-008/010 转 ✅、❌=2。
3. 冒烟脚本零改（用例本就跑 Spec，断言绿即 ✅）。

**超限熔断**：需改业务接口/ServiceImpl/冒烟脚本、mvn verify 基线漂移、前端三线动 → 停止回报。

**不动清单**：RoleService/ModuleService 接口与 Impl（零业务动——过期的是 Spec 非接口）；冒烟脚本；其余 Spec/测试；web/036c2b 冗余覆盖清理另案；web/045/036c3 预留；工作区预存 `application.yml.template` D 态。

## 交付物（2 业务文件）

1. `backend/src/test/java/com/authcore/service/RoleServiceInterfaceSpec.java` — 计数 6→7+断言
2. `backend/src/test/java/com/authcore/service/ModuleServiceInterfaceSpec.java` — 计数 6→8+断言
3. 文档回写：task/session/iteration/registry 四件

### 新增
（无）

### 修改
- `backend/src/test/java/com/authcore/service/RoleServiceInterfaceSpec.java`
- `backend/src/test/java/com/authcore/service/ModuleServiceInterfaceSpec.java`

### 删除
（无）

### 变更计数

业务文件 **2**；后端主代码/接口/冒烟脚本/前端零改动；冒烟 ❌ 由 4 降为 2（model-008/010 消除）。

## 评审意见

（待 Evaluator 评审）
