# Sprint 工作单：sprint-104

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-104 |
| 所属模块 | **web** |
| 功能点 ID | **web/036c3** |
| 功能点名称 | 嵌套/串联 `:deep` 编译失效治理（暗色模式 036c 收尾片） |
| 状态 | **PLANNED** |
| 创建时间 | 2026-10-16 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/036a | EP 暗色变量体系接入 | ✅ |
| web/036b | 局部白底缺陷修复 | ✅ |
| web/036c1 | 口径治理 | ✅ |
| web/036c2a / 036c2b | 计数同步 / 死代码清理 | ✅ |
| web/037 | 面包屑单 `:deep` 规避先例（068 登记本缺陷） | ✅ |

## 业务背景

**068 登记的预存缺陷（sprint-068 Generator 登记，36 个 Sprint 后执行）**：scoped 样式中**嵌套/串联 `:deep()`**（如 `.a { :deep(.b) { :deep(.c) {...} } }`）时，@vue/compiler-sfc **仅转换首个 `:deep()`**，后续以字面选择器残留 → 规则静默失效，EP 默认样式兜底（无崩溃、无报错）。

**Planner 2026-10-16 只读实证（缺陷持久，非 068 口误）**：
- **dist 产物字面 `:deep(` 残留 = 42**（恰与 068 登记数吻合），分布 4 个 CSS：DefaultLayout 24、LoginView 7、Breadcrumb 6、el-drawer 5
- **源侧嵌套规则体 = 35 处 / 4 文件**：Sidebar 20、LoginView 6、BaseTable 5、Breadcrumb 4（python 大括号配平探测；差额来自多选择器规则产物展开）
- 受害规则（全部死）：Sidebar 菜单 40px/圆角/hover/展开箭头、Breadcrumb 颜色族、BaseTable 表格边框与加载圈、LoginView 复选框内芯

**规模裁决**：068 原估「>6 问案B」——**只读实测证伪**：修复面 = 4 vue 扁平化 + smoke 护栏 + sidebar spec 锚点同步 = **6 文件顶格 ≤6** → 无需问案B 直接建单（036c2a 证伪 5>6、036c2b 证伪同款先例）。sidebar-icon-size.spec 的 `extractRuleBlock(':deep(.el-menu-item)')` 锚点随扁平化必破（已实证嵌套形态在 Sidebar:164），**同步属计划内**；breadcrumb-nav.spec 锚点指向 037 单 `:deep` 规则（不动）与 profile-view-spacing.spec（非受害文件）均不受牵连。

## 验收标准（TDD 驱动）

- [ ] AC1 — RED（三路首跑红）：① `sidebar-icon-size.spec.ts` 新增全仓结构用例「所有 .vue 无嵌套 :deep 规则体」首跑红（实录列 4 文件 35 处）；② 同文件既有两用例锚点按扁平化形态同步后首跑红（当前源为嵌套形态，marker 抽块 null）；③ `bash scripts/smoke.sh web-036c3` 首跑 ❌（G1 定向 spec 非 `3 passed`、G2 dist 字面 42≠0）。← 用例 `AC1 ← 用例 sidebar-icon-size.spec 三用例红+web-036c3 护栏红`
- [ ] AC2 — GREEN：4 vue 扁平化（**每叶子规则保留完整后代路径于单 `:deep()` 参数**，如 `:deep(.el-menu .el-menu-item)`；仅改嵌套形态，037 类单 `:deep` 规则零动）→ spec 3/3 绿 + 源侧嵌套探测=0 + `npx vite build` 后 `grep -rho ':deep(' dist/assets/ | wc -l` = **0**（42→0 产物实证）。← 用例 `AC2 ← 用例 sidebar-icon-size.spec 3/3+结构探测 0+dist 残留 0`
- [ ] AC3 — 门禁：`npm run lint` **0E|166W**（±0）；`npm run test` **42 文件 329/329**（+1 结构用例，Generator 实测回填）；`npm run build` ✓（内含 vue-tsc EXIT=0）；后端零动不跑 mvn。← 用例 `AC3 ← 用例 四门禁=基线/恰+1 用例`
- [ ] AC4 — 冒烟：`bash scripts/smoke.sh web-036c3` = 1 用例 ✅（G1=定向 spec `3 passed`；G2=build 后 dist 字面=0）；全量 **80=78✅+2⏭ 0❌ `SMOKE PASSED（80 用例）`**。← 用例 `AC4 ← 用例 web-036c3 护栏+全量冒烟 80`

### AC1/AC2 — 结构护栏 TDD

**步骤与期望**：test 段=结构用例+锚点同步+smoke 护栏（三红）→ feat 段=4 vue 扁平化 → 三路转绿。既有 037 横排/036a/036b 规则与单 `:deep` 规则零动（037 产物生效实证先例）。

**用例**：`AC1/AC2 ← 用例 结构 spec 红绿+dist 残留 42→0`

### AC3/AC4 — 门禁与冒烟

**用例**：`AC3/AC4 ← 用例 四门禁+web-036c3 护栏`

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | 结构用例+两锚点同步首跑红（4 文件 35 处实录）+护栏 ❌ | AC1 | 待红 |
| 2 | spec 3/3 绿+源侧嵌套 0+dist 字面 42→0 | AC2 | 待绿 |
| 3 | lint 0E\|166W / test 42 文件 329 / build ✓ | AC3 | 待亲测 |
| 4 | `smoke.sh web-036c3`=1 ✅；全量 80=78✅+2⏭ 0❌ | AC4 | 待亲测 |

## RED 证据

```text
[修复前红态] 证据槽 — 待 Generator 填写（结构用例列 4 文件 35 处+锚点 null+护栏 ❌；静态实证 dist=42）
```

## 门禁与冒烟记录

> Generator 亲测填写。基线（sprint-103 后）：冒烟 **79=77✅+2⏭ 0❌**（本单后预期 80=78✅+2⏭）；前端 test 42 文件 328/328、lint 0E|166W、build ✓~17s、vue-tsc EXIT=0；后端零动。

| 门禁项 | 本单后预期 | 实测 | 结论 |
|--------|---------|------|------|
| `npm run test` | 42 文件 329/329（+1 结构用例） | 待填 | 待验 |
| `npm run lint` | 0E\|166W（±0） | 待填 | 待验 |
| `npm run build` | ✓（vue-tsc EXIT=0） | 待填 | 待验 |
| dist 字面 `:deep(` 残留 | 0（当前 42） | 待填 | 待验 |
| `bash scripts/smoke.sh` | 80=78✅+2⏭ 0❌ `SMOKE PASSED（80 用例）` | 待填 | 待验 |
| 后端 mvn | 零动（不跑） | — | 零动 |

## 实现说明

**实现要点**：
1. 结构探测（spec 与 smoke 共用口径）：大括号配平扫描每个 .vue 的 style 区，选择器含 `:deep(` 的规则体在闭合前又出现 `:deep(` 即计数——当前基线 **35（4 文件）**，目标 **0**。
2. 扁平化规则：嵌套 `:deep(.outer){ :deep(.inner){ P; &:hover{H} } }` → `:deep(.outer .inner){ P }` 与 `:deep(.outer .inner:hover){ H }`（后代路径完整进单 `:deep` 参数）；外层规则自身属性保留为独立 `:deep(.outer){...}`；**禁止**改写为「保持 marker、丢弃祖先路径」的凑 spec 形态（语义变宽=Bug）。
3. sidebar 两锚点同步为扁平化后实际 selector 文本；新增结构用例并入该 spec（**禁止新建 spec 文件**，守 6 文件预算）。
4. smoke 护栏：G1 `npm run test -- --run src/__tests__/sidebar-icon-size.spec.ts 2>&1 | grep -q '3 passed'`；G2 `cd frontend && npx vite build` 后 `grep -rho ':deep(' dist/assets/ | wc -l`=0。用例名 `web-036c3 嵌套 :deep 治理护栏`（**连字符口径，对齐 web-036c2b 先例**）。
5. 尾部追加位置=用例区唯一闭合分隔线前；双引号内 `$` 变量须 `\$` 转义（G2 的 `$(...)` 同）。

**超限熔断**：改动 >6 文件（含计划外 spec 牵连——若 breadcrumb-nav/profile-view-spacing 锚点被迫改动即超 7）、新建 spec 文件、改单 `:deep` 规则（037 横排、036a/b 既有覆盖）、改构建配置/vite 插件、动既有 79 条用例与汇总格式 → 停止回报。

**不动清单**：单 `:deep` 规则全部；非受害文件全部；profile-view-spacing.spec、breadcrumb-nav.spec；用例区既有 79 条（仅追加 1 条）；SMOKE_APP_LOG/内部日志/FILTER/汇总格式；后端全部；`application.yml.template` D 态（永不 add）。

## 交付物（6 业务文件）

1. `frontend/src/__tests__/sidebar-icon-size.spec.ts` — 两锚点同步+新增全仓结构用例
2. `scripts/smoke.sh` — 用例区追加 `web-036c3` 护栏（79→80）
3. `frontend/src/components/Sidebar.vue` — 20 处嵌套扁平化
4. `frontend/src/views/LoginView.vue` — 6 处嵌套扁平化
5. `frontend/src/components/BaseTable.vue` — 5 处嵌套扁平化
6. `frontend/src/components/Breadcrumb.vue` — 4 处嵌套扁平化

### 新增
（无）

### 修改
- `frontend/src/__tests__/sidebar-icon-size.spec.ts`
- `scripts/smoke.sh`
- `frontend/src/components/Sidebar.vue`
- `frontend/src/views/LoginView.vue`
- `frontend/src/components/BaseTable.vue`
- `frontend/src/components/Breadcrumb.vue`

### 删除
（无）

### 变更计数

业务文件 **6**（顶格）；smoke 用例 79→**80**；spec 用例 2→**3**；dist 字面残留 42→**0**。

## 评审意见

（待 Evaluator 评审）
