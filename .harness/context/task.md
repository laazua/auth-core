# Sprint 工作单：sprint-097

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-097 |
| 所属模块 | web |
| 功能点 ID | **web/052**（系统管理死文件卫生清理；051 实测登记卫生项升格执行） |
| 功能点名称 | 删除四个无路由引用的占位死文件（PermissionView/ModuleView/RoleView/UserView） |
| 状态 | **PLANNED** |
| 创建时间 | 2026-10-14 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/051 | 只读实测登记卫生项（PermissionView/ModuleView 两死文件） | ✅ 实测关单 |
| 本次 Planner 补充实测 | **RoleView/UserView 同款死文件**（各 21 行占位、零引用） | ✅ 2026-10-14 取证 |

## 业务背景

**卫生项实测（零功能影响，纯代码健康）**：

| 文件 | 行数 | 实测证据 |
|------|------|---------|
| `views/system/PermissionView.vue` | 21 | 占位「待实现」；`/system/permissions` 路由指向 `system/IndexView.vue`（824 行已实现）非它；全仓 import/路由零引用 |
| `views/system/ModuleView.vue` | 21 | 同款；`/system/modules` 也指向 `system/IndexView.vue` |
| `views/system/RoleView.vue` | 21 | 同款占位；`/system/roles` 指向 `roles/IndexView.vue` |
| `views/system/UserView.vue` | 21 | 同款占位；`/system/users` 指向 `users/IndexView.vue` |

早期占位遗留（四页实现早已迁移到各自 IndexView），保留易误导（占位文案与真实实现并存）。**删除即收口**。

## 验收标准（TDD 驱动）

- [ ] AC1 — 死文件删除且零引用：四文件从工作区删除；`grep -rn "system/PermissionView\|system/ModuleView\|system/RoleView\|system/UserView" frontend/src` 零命中（import/路由）；lint/test/build 三线绿=编译期零依赖证明。← 用例 `AC1 ← 用例 前端三线 + 引用 grep`
- [ ] AC2 — 冒烟与门禁：`scripts/smoke.sh` 新增 `web-052` 用例（G1 grep 四文件均不存在于文件系统、G2 grep 路由 routes.ts 四路由仍指向各自真实 IndexView——防误删路由护栏）；冒烟 76→77、❌ 恰 4 零新增；mvn verify 基线不变（后端零动）。← 用例 `AC2 ← 用例 冒烟全量 + mvn 基线`

### AC1 — 删除与三线

**步骤与期望**：先写 smoke web-052 用例体首跑（G1 因文件存在而红）→ 删四文件 → G1 绿+三线绿。

**用例**：`AC1 ← 用例 前端三线 + 引用 grep`

### AC2 — 冒烟

**用例**：`AC2 ← 用例 冒烟全量`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | smoke web-052 用例体首跑（G1 红）+ 删除后绿 | AC1 | 待 RED→GREEN |
| 2 | 前端三线（lint/test/build） | AC1 | 待亲测 |
| 3 | 冒烟全量 77 + mvn 基线 | AC2 | 待亲测 |

## RED 证据

```text
[RED] AC1 smoke web-052 G1: test ! -f 四文件组合 EXIT=1（PermissionView/ModuleView/RoleView/UserView 均存在于文件系统）
```

## 门禁与冒烟记录

> Generator 亲测填写。基线口径（2026-10-14 sprint-096 评审实测）。

| 门禁项 | 基线口径 | 实测 | 结论 |
|--------|---------|------|------|
| 后端 `mvn -q verify` | 168 用例 4F+2E（五类 veto-6） | 待填（后端零动） | 待验 |
| `npm run lint` | EXIT=0，0 errors / 166 warnings | 待填（删除文件或降 warning 数——死文件若贡献 warning 则预期 ≤166） | 待验 |
| `npm run test` | 41 文件 324/324 | 待填（死文件无 spec→预期不变） | 待验 |
| `npm run build` | ✓ | 待填 | 待验 |
| `bash scripts/smoke.sh` | 76=70✅+4❌+2⏭️ → 本单 +1=77 | 待填（预期 77=71✅+4❌+2⏭️ 含 web-052） | 待验 |

## 实现说明

**实现要点**：
1. 先写 smoke web-052（G1 `test ! -f` 四文件取反 grep 不存在判定、G2 grep routes.ts 四 component 路径在场）→ 用例体首跑 G1 红（文件在）。
2. `git rm` 四死文件 → G1 绿+lint/test/build 绿。
3. 后端零动（mvn 基线照抄预期不变）。

**超限熔断**：需改 routes.ts/IndexView/其它业务文件、三线任一红、mvn 基线漂移 → 停止回报。

**不动清单**：routes.ts（零改——路由本就指向真实实现）；system/IndexView.vue；四 FormDrawer；users/roles IndexView；冒烟既有用例与 ❌ 四条；web/036c/045/051 挂起区；工作区预存 `application.yml.template` D 态。

## 交付物（5 操作，净文件 -4）

1. `frontend/src/views/system/PermissionView.vue` — **删除**
2. `frontend/src/views/system/ModuleView.vue` — **删除**
3. `frontend/src/views/system/RoleView.vue` — **删除**
4. `frontend/src/views/system/UserView.vue` — **删除**
5. `scripts/smoke.sh` — +web-052 用例
6. 文档回写：task/session/iteration/registry 四件

### 新增
（无）

### 修改
- `scripts/smoke.sh`

### 删除
- `frontend/src/views/system/PermissionView.vue`
- `frontend/src/views/system/ModuleView.vue`
- `frontend/src/views/system/RoleView.vue`
- `frontend/src/views/system/UserView.vue`

### 变更计数

净 **-4 文件**；routes.ts/IndexView/FormDrawer/后端零改动。

## 评审意见

（待 Evaluator 评审）
