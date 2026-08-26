# auth-core Harness 系统实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建可直接使用的三智能体（Planner→Generator→Evaluator）AI 开发 harness 系统，用于后续迭代开发 RBAC 权限管理系统。

**Architecture:** 上下文三层模型（指令层 prompt / 规则层 standards+criteria+tdd+architecture / 状态层 context+registry）。Agent 通过读文件获得全部上下文，每次阶段切换新开会话，由 session-state.md 承载结构化交接。spec 见 `docs/superpowers/specs/2026-08-26-harness-system-design.md`。

**Tech Stack:** Markdown 工件 + Bash 校验脚本 + git。无运行时依赖。

## Global Constraints

- 所有文档一律中文；代码/命令/路径保持英文原样
- 功能点状态标记只允许：`⬜ 未开始 / 🔄 进行中 / ✅ 完成 / ❌ 阻塞 / ⚠️ 有缺口`
- registry 必须恰好 32 个功能点（infra 4 / model 6 / auth 6 / users 3 / roles 2 / perms 1 / modules 1 / web 7 / integration 2），初始状态全 ⬜，依赖关系与 spec §4 表一致
- 所有被其他文件引用的路径必须用反引号包裹成行内代码（如 `` `.harness/context/task.md` ``），以便 verify 脚本校验死链
- DeepSeek 接入三个环境变量的精确值：`ANTHROPIC_BASE_URL=https://api.deepseek.com/anthropic`、`ANTHROPIC_AUTH_TOKEN=$DEEPSEEK_API_KEY`、`ANTHROPIC_MODEL=deepseek-chat`
- 评审通过线固定表述：「总分 ≥7 且无一项一票否决」；门禁命令固定表述：后端 `mvn -q verify`，前端 `npm run lint && npm run test && npm run build`
- 每个任务结束必须 git commit；commit message 用中文，前缀 feat:/test:/docs:/chore:
- harness 自身是文档不受 500 行限制；该限制是写给未来业务代码的规则

## File Structure

```
scripts/verify-harness.sh            # 校验脚本 = 本次交付的可执行测试套件（先写，先红）
docs/01-architecture.md              # 固化架构：选型/分层/ER/API/认证/业务硬语义
.harness/rules/coding-standards.md   # 编码规范（Java/Vue 双栈）
.harness/rules/tdd-workflow.md       # RED→GREEN→REFACTOR 流程
.harness/rules/review-criteria.md    # 5 维评分 + 一票否决
.harness/modules/registry.md         # 32 功能点注册表（预载）
.harness/context/task.md             # 工作单（模板定义 + 空槽）
.harness/context/session-state.md    # 会话交接唯一真相源
.harness/context/iteration-log.md    # append-only 历史
.harness/planner.md                  # Planner prompt
.harness/generator.md                # Generator prompt
.harness/evaluator.md                # Evaluator prompt（反宽容调校）
AGENTS.md                            # CLI 自动加载的项目地图 + 锚点路由
.harness/README.md                   # 总览 + 驱动手册 + 冒烟自检
```

构建顺序自底向上：规则层 → 状态层 → 指令层 → 入口锚点 → 手册与终验。

---

### Task 1: 校验脚本（测试基座，先行写红）

**Files:**
- Create: `scripts/verify-harness.sh`

**Interfaces:**
- Consumes: 无
- Produces: `bash scripts/verify-harness.sh`，退出码 0=全部通过 / 1=存在失败；输出每条检查 `✅/❌` 行。后续所有任务靠它转绿。

- [ ] **Step 1: 写校验脚本**

```bash
#!/usr/bin/env bash
# verify-harness.sh — harness 交付完整性校验（spec §8 DoD 的可执行形式）
set -u
cd "$(dirname "$0")/.."
FAIL=0
ok()  { echo "✅ $1"; }
bad() { echo "❌ $1"; FAIL=1; }
has() { grep -qF -- "$2" "$1" 2>/dev/null; }

REQUIRED_FILES=(
  AGENTS.md .harness/README.md
  .harness/planner.md .harness/generator.md .harness/evaluator.md
  .harness/context/task.md .harness/context/session-state.md .harness/context/iteration-log.md
  .harness/modules/registry.md
  .harness/rules/coding-standards.md .harness/rules/review-criteria.md .harness/rules/tdd-workflow.md
  docs/01-architecture.md
)

echo "== 1. 文件存在性 =="
for f in "${REQUIRED_FILES[@]}"; do
  [ -f "$f" ] && ok "存在 $f" || bad "缺失 $f"
done

echo "== 2. Prompt 章节完整性 =="
for p in .harness/planner.md .harness/generator.md .harness/evaluator.md; do
  for sec in "角色定位" "输入源" "行为流程" "约束" "输出要求"; do
    has "$p" "## $sec" && ok "$p 含「$sec」" || bad "$p 缺「$sec」"
  done
done

echo "== 3. Registry 完整性 =="
REG=.harness/modules/registry.md
if [ -f "$REG" ]; then
  ids_defined=$(grep -ohE '^\| *(infra|model|auth|users|roles|perms|modules|web|integration)/[0-9]{3}' "$REG" | tr -d '| ' | sort -u)
  count=$(printf '%s\n' "$ids_defined" | grep -c . )
  [ "$count" -eq 32 ] && ok "功能点计数 = 32" || bad "功能点计数 = $count（期望 32）"
  pending=$(grep -cE '^\| *(infra|model|auth|users|roles|perms|modules|web|integration)/[0-9]{3}.*⬜' "$REG")
  [ "$pending" -eq 32 ] && ok "初始状态全 ⬜" || bad "⬜ 行数 = $pending（期望 32）"
  deps=$(awk -F'|' '/^\| *(infra|model|auth|users|roles|perms|modules|web|integration)\/[0-9]{3}/{print $4}' "$REG" \
        | tr ',、' '\n' | tr -d ' ' | grep -E '^(infra|model|auth|users|roles|perms|modules|web|integration)/[0-9]{3}$' | sort -u)
  orphan=0
  for d in $deps; do
    printf '%s\n' "$ids_defined" | grep -qx "$d" || { bad "依赖未定义：$d"; orphan=1; }
  done
  [ "$orphan" -eq 0 ] && ok "所有前置依赖 ID 均已定义"
else
  bad "缺失 $REG"
fi

echo "== 4. task.md 模板字段 =="
TASK=.harness/context/task.md
for field in "Sprint ID" "所属模块" "需求描述" "业务背景" "前置依赖" "验收标准" "测试清单" "RED 证据" "实现说明" "状态" "评审意见"; do
  has "$TASK" "$field" && ok "task.md 含「$field」" || bad "task.md 缺「$field」"
done

echo "== 5. 入口锚点与驱动手册 =="
has AGENTS.md ".harness/context/session-state.md" && ok "AGENTS.md 锚点指向 session-state" || bad "AGENTS.md 缺锚点"
for kw in "planner.md" "generator.md" "evaluator.md" "ANTHROPIC_BASE_URL=https://api.deepseek.com/anthropic" "ANTHROPIC_AUTH_TOKEN" "ANTHROPIC_MODEL=deepseek-chat"; do
  has .harness/README.md "$kw" && ok "README 含「$kw」" || bad "README 缺「$kw」"
done

echo "== 6. 引用路径死链检测 =="
refs=$(cat .harness/*.md .harness/context/*.md .harness/modules/*.md .harness/rules/*.md AGENTS.md docs/01-architecture.md 2>/dev/null \
  | grep -ohE '\`(\\.harness/[A-Za-z0-9_./-]+|docs/01-architecture\\.md|AGENTS\\.md|scripts/verify-harness\\.sh)\`' \
  | tr -d '\`' | sort -u)
dead=0
while IFS= read -r r; do
  [ -z "$r" ] && continue
  if [ -e "$r" ]; then ok "引用有效 $r"; else bad "死链 $r"; dead=1; fi
done <<< "$refs"

echo ""
if [ "$FAIL" -eq 0 ]; then echo "ALL CHECKS PASSED"; else echo "CHECKS FAILED"; fi
exit $FAIL
```

注意：Step 1 中第 6 节 grep 正则里的反引号需按 bash 语法转义（`\``）；若执行报错，优先修脚本的引号处理，不改检查语义。

- [ ] **Step 2: 赋权并运行，确认为红**

Run: `chmod +x scripts/verify-harness.sh && bash scripts/verify-harness.sh`
Expected: 退出码 1；第 1 节列出 13 条「缺失 …」，其余节大量 ❌；末行 `CHECKS FAILED`

- [ ] **Step 3: Commit**

```bash
git add scripts/verify-harness.sh
git commit -m "test: harness 完整性校验脚本（DoD 可执行化，先红）"
```

---

### Task 2: docs/01-architecture.md（规则层基座）

**Files:**
- Create: `docs/01-architecture.md`

**Interfaces:**
- Consumes: 无
- Produces: 六实体字段定义、API 规范、业务硬语义五条 —— 被 registry.md、三个 prompt、coding-standards.md 引用（路径写作 `` `docs/01-architecture.md` ``）

- [ ] **Step 1: 写架构文档**

包含以下章节（完整内容如下，直接成文）：

```markdown
# auth-core 系统架构

> 本文档是所有 Agent 的仲裁依据：任何实现与本文件冲突时，以本文件为准，并在 `.harness/context/session-state.md` 登记，不得静默偏离。

## 1. 技术栈（已固化，Agent 不得更改）
| 层 | 选型 |
|----|------|
| 语言/运行时 | Java 21 |
| 框架 | Spring Boot 3.x（Web、Validation、Actuator）|
| 安全 | Spring Security 6 + JWT(HS256) + BCrypt |
| ORM | MyBatis-Plus 3.5.x |
| 数据库 | MySQL 8 + Flyway 迁移 |
| 测试 | JUnit5 + Mockito + MockMvc + Testcontainers MySQL |
| 构建 | Maven（单模块）|
| 前端 | Vue3 + TypeScript(strict) + Vite + Pinia + Vue Router + Element Plus + Axios + Vitest |

## 2. Monorepo 结构
backend/   # Spring Boot 单 Maven 模块
frontend/  # Vue3 应用

后端包结构 package-by-layer：
com.authcore.config / controller / service(.impl) / mapper / entity / dto / common / AuthCoreApplication

## 3. 数据模型（六实体）
- sys_user: id PK, username UNIQUE NOT NULL, password(BCrypt) NOT NULL, nickname, email, phone, status(1启用/0停用), created_at, updated_at
- sys_role: id, name, code UNIQUE, status, created_at, updated_at
- sys_module: id, name, code UNIQUE, base_url(可空,模块服务地址), description, status, created_at, updated_at
- sys_permission: id, module_id NOT NULL(逻辑外键→sys_module), name, code UNIQUE, description, created_at, updated_at
- sys_user_role: user_id + role_id 联合唯一
- sys_role_permission: role_id + permission_id 联合唯一
Flyway 迁移命名 V{n}__{描述}.sql；物理删除 + service 层引用校验拒绝（不做逻辑删除）。

## 4. API 规范
- 前缀 /api/v1/{resource}；响应统一 Result{code:int(0=成功), message:String, data:T}
- 业务错误码分段：10xx 用户 / 11xx 角色 / 12xx 权限 / 13xx 模块 / 14xx 认证；HTTP 401=未认证，403=未授权
- 分页参数 page(默认1)/size(默认10)；分页 data={list,total,page,size}
- 时间统一 ISO-8601 字符串（UTC+8）

## 5. 认证设计
登录 POST /api/v1/auth/login（username+password）→ JWT(HS256)，payload{uid, username, exp}，有效期默认 2h（配置项 jwt.secret/jwt.expire-hours，secret 仅来自环境变量）。

## 6. RBAC 业务硬语义（防偏差仲裁条款）
1. 权限必须归属模块（module_id 非空），不存在脱离模块的权限
2. 用户获得权限的唯一路径是角色（RBAC0）；禁止用户-权限直接授权
3. username / role.code / permission.code / module.code 全局唯一
4. 删除受引用保护：删除角色前校验 sys_user_role/sys_role_permission 引用；删除模块前校验其下权限；删除权限前校验 sys_role_permission；命中引用返回对应分段业务错误码
5. 停用（status=0）语义：停用用户不可登录、token 即时失效于下次校验；停用角色不再参与鉴权聚合；停用模块下权限视为无效
```

- [ ] **Step 2: 验证**

Run: `bash scripts/verify-harness.sh`
Expected: 「缺失 docs/01-architecture.md」消失，其余不变，仍 `CHECKS FAILED`

- [ ] **Step 3: Commit**

```bash
git add docs/01-architecture.md
git commit -m "docs: 固化系统架构与 RBAC 业务硬语义"
```

---

### Task 3: rules/coding-standards.md

**Files:**
- Create: `.harness/rules/coding-standards.md`

**Interfaces:**
- Consumes: `` `docs/01-architecture.md` ``（引用其技术栈与包结构）
- Produces: 编码硬规则清单，被 generator.md 与 evaluator.md 引用

- [ ] **Step 1: 写编码规范**

章节与规则要点（成文时展开为带简短说明的清单）：

```markdown
# 编码规范

> 硬性规则，Evaluator 按此逐条核查。技术选型见 `docs/01-architecture.md`。

## Java / Spring Boot
- 文件 ≤500 行、单方法 ≤50 行；超限必须拆分
- 禁 System.out/printStackTrace；日志 slf4j：log.debug/info/warn/error，error 必须带堆栈或上下文
- 分层单向调用：controller→service→mapper；禁止跨层跳调用、禁止反向依赖
- DTO 进出 controller，entity 不外泄；Bean Validation 注解校验请求参数
- 业务异常统一继承 common 包 BusinessException(code,message)，由全局异常处理器转 Result

## MyBatis-Plus
- 简单 CRUD 用 BaseMapper/IService 方法；自定义 SQL 写 XML 或 Wrapper，禁字符串拼接 SQL
- 分页统一 PaginationInnerInterceptor；created_at/updated_at 用 MetaObjectHandler 自动填充
- 表/列 snake_case ↔ Java camelCase；表前缀 sys_

## 安全
- 密码仅 BCrypt 编码存取；JWT secret 只从环境变量读取，application.yml 不落真实密钥
- 所有管理接口需认证；权限注解/校验以数据库权限码为准
- 禁任何 SQL 注入面（${} 拼接）、禁敏感信息入日志

## Vue3 / TypeScript
- `<script setup lang="ts">` composition API；TS strict，禁 any（第三方类型缺失用 unknown+收窄）
- API 调用集中在 src/api/，组件内禁止直接 axios；Pinia store 管理跨组件状态
- 组件 PascalCase；页面放 src/views/，公共组件放 src/components/
- eslint + vue-tsc 零告警才可交付

## 命名
类 UpperCamelCase / 方法变量 lowerCamelCase / 常量 UPPER_SNAKE / REST 路径复数小写中划线
```

- [ ] **Step 2: 验证**

Run: `bash scripts/verify-harness.sh`
Expected: 该文件的存在性检查转绿；死链检测中 `docs/01-architecture.md` 引用有效；仍 `CHECKS FAILED`（其余未建）

- [ ] **Step 3: Commit**

```bash
git add .harness/rules/coding-standards.md
git commit -m "feat: coding-standards（Java/Vue 双栈硬规则）"
```

---

### Task 4: rules/tdd-workflow.md

**Files:**
- Create: `.harness/rules/tdd-workflow.md`

**Interfaces:**
- Consumes: 无
- Produces: TDD 循环步骤 + RED 证据格式 + 两段式 commit 规范，被 generator.md/evaluator.md/task.md 引用

- [ ] **Step 1: 写 TDD 流程**

```markdown
# TDD 工作流（测试先行是硬约束）

## 测试金字塔
| 层 | 工具 | 要求 |
|----|------|------|
| 单元 | JUnit5 + Mockito | service 层业务分支与边界全覆盖，外部依赖全 mock |
| Web 层 | MockMvc(@WebMvcTest 或 @SpringBootTest+MockMvc) | 每个接口：正常/参数非法/未认证/无权限 四类用例 |
| 集成 | @SpringBootTest + Testcontainers MySQL | Mapper 与迁移脚本正确性、涉及关联表的业务流 |
| 前端 | Vitest + Vue Test Utils | store/composable 必测；登录守卫、权限菜单等关键交互必测 |

## RED → GREEN → REFACTOR（每个功能点必经）
1. 从 task.md 验收标准导出【测试清单】写入 task.md
2. 先写测试；运行确认失败（RED），将关键失败输出摘录到 task.md「RED 证据」槽位
3. 最小实现使测试转绿（GREEN）
4. 重构消除坏味道，保持全绿
5. 全量回归：后端 mvn -q verify 全绿；前端 npm run lint && npm run test && npm run build 通过

## 证据与提交
- RED 证据格式（记入 task.md）：
  ```text
  [RED] Tests run: X, Failures: Y — com.authcore.xxx.XxxTest.test行为: expected:<...> but was:<...>
  ```
- 提交推荐两段式：先 `test: <功能点>-测试先行(RED)`，后 `feat: <功能点>-最小实现(GREEN)`
- 单 commit 交付时，task.md 的 RED 证据为唯一先行证明，缺失即触发 Evaluator 一票否决

## 测试命名
方法名 `test_行为_条件_预期` 或 given_when_then 注释分节；一个用例只断言一条行为主线
```

- [ ] **Step 2: 验证**

Run: `bash scripts/verify-harness.sh && true`（忽略非零退出）
Expected: 存在性新增一行绿；仍 `CHECKS FAILED`

- [ ] **Step 3: Commit**

```bash
git add .harness/rules/tdd-workflow.md
git commit -m "feat: tdd-workflow（RED/GREEN 证据链与提交规范）"
```

---

### Task 5: rules/review-criteria.md

**Files:**
- Create: `.harness/rules/review-criteria.md`

**Interfaces:**
- Consumes: `` `.harness/rules/coding-standards.md` ``、`` `.harness/rules/tdd-workflow.md` ``、`` `docs/01-architecture.md` ``
- Produces: 评分表、一票否决 6 条、评审报告模板、BLOCKED 流转规则 —— evaluator.md 直接内嵌引用

- [ ] **Step 1: 写评审标准**

```markdown
# 评审标准（Evaluator 唯一裁决依据）

## 评分维度（每维 1-10，每维必须附证据：文件:行号 或 命令输出摘录）
| 维度 | 评判要点 |
|------|---------|
| 功能正确性 | task.md 验收标准逐条满足；边界与异常路径处理；RBAC 语义符合 `docs/01-architecture.md` 第 6 节 |
| 代码质量 | 无 Bug；可读性与命名；分层清晰；无重复代码；无过度设计 |
| 规范遵守 | `.harness/rules/coding-standards.md` 全部硬规则；行数/日志/分层/命名 |
| TDD 执行度 | `.harness/rules/tdd-workflow.md` 证据链完整：测试清单→RED 证据→全绿回归；测试质量（断言有效性，非凑数） |
| 安全性 | 越权/SQL注入/明文密码/密钥硬编码/敏感日志 五查 |

## 决策
- 通过：总分 ≥7 且无一项一票否决
- 不通过：总分 <7 或命中任一否决项 → 问题清单退回 Generator（REWORK）
- 同一功能点连续 2 次 REWORK 不通过 → registry 标 ❌ BLOCKED，登记 `.harness/context/session-state.md` 挂起区，转人工

## 一票否决项
1. 功能缺失：task.md 明确要求未实现
2. 安全漏洞：越权访问、SQL 注入、明文密码、密钥硬编码、敏感信息入日志
3. 明确 Bug：核心逻辑错误导致功能不可用
4. 无测试先行证据：缺 RED 记录且 git 提交顺序无法证明 test-first
5. RBAC 业务模型偏差：违反 `docs/01-architecture.md` 第 6 节任一条（如权限脱离模块、绕过角色的直接授权、删除保护被绕过）
6. 回归失败：mvn -q verify 或前端门禁任一不通过

## 评审报告模板（输出必须遵循）
### 验收标准核对
- [x]/[ ] 标准 — 结论（原因）
### 门禁命令实测
mvn -q verify：…；前端：…（贴关键输出）
### 评分
| 维度 | 分数 | 证据 |
…5 行…
### 决策
✅ 通过 / ❌ 不通过（REWORK 第 N 次）
### 问题列表（不通过时）
1. [严重度] 问题 → 可操作修复意见
### 改进建议（可选，不计分）

## Evaluator 反宽容守则
- 先找问题再谈优点；禁止"总体良好但…"式和稀泥
- 每个分数必须有证据支撑，无证据按最低分计
- 门禁命令必须亲自运行，不采信 Generator 自述
```

- [ ] **Step 2: 验证**

Run: `bash scripts/verify-harness.sh; true`
Expected: 存在性绿 +1；死链检测出现多条「引用有效 .harness/rules/…」；仍 FAILED

- [ ] **Step 3: Commit**

```bash
git add .harness/rules/review-criteria.md
git commit -m "feat: review-criteria（5 维评分/一票否决/反宽容守则）"
```

---

### Task 6: modules/registry.md（32 功能点预载）

**Files:**
- Create: `.harness/modules/registry.md`

**Interfaces:**
- Consumes: spec §4 表（本计划 Step 1 内嵌同表）
- Produces: 功能点唯一 ID 体系（如 infra/001），被三个 prompt 与 context 文件引用

- [ ] **Step 1: 写注册表**

结构：头部说明 + 图例 + 最后更新行 + 阶段总览表 + 九张模块表。九张表的行数据如下（ID/功能点/前置依赖/状态⬜/备注—，逐行照抄）：

infra 表 4 行：001 Maven 项目骨架 + Spring Boot 启动 + actuator 健康检查｜—；002 MySQL 接入 + MyBatis-Plus 配置 + Flyway 迁移机制｜infra/001；003 统一响应体 Result<T> + 全局异常处理 + Bean Validation｜infra/001；004 测试基础设施（Testcontainers MySQL 基座 + 测试命名/分层约定）｜infra/002

model 表 6 行：001 sys_user 表迁移+实体+Mapper｜infra/002；002 sys_role 表迁移+实体+Mapper｜infra/002；003 sys_module 表迁移+实体+Mapper｜infra/002；004 sys_permission 表迁移+实体+Mapper(FK module_id)｜model/003；005 sys_user_role+sys_role_permission 关联表｜model/001, model/002, model/004；006 种子数据迁移（内置 admin + 示例角色/权限/模块）｜model/005

auth 表 6 行：001 Spring Security 无状态基线 + BCrypt 编码器｜infra/003, model/001；002 登录接口 POST /api/v1/auth/login 签发 JWT｜auth/001；003 JWT 校验过滤器 + SecurityContext 注入｜auth/001, auth/002；004 GET /api/v1/auth/me（用户+角色+权限集合）｜auth/003, model/005；005 权限校验 API POST /api/v1/auth/check（供外部模块集成调用）｜auth/003；006 登出策略（无状态 JWT v1：接口+失效语义说明）｜auth/003

users 表 3 行：001 用户 CRUD API（分页/条件查询/创建加密/更新/启停用/删除）｜auth/003, model/001；002 用户-角色分配 API（批量设置）｜users/001, model/005；003 密码修改+管理员重置｜users/001

roles 表 2 行：001 角色 CRUD API｜auth/003, model/002；002 角色-权限分配 API（批量设置）｜roles/001, model/005

perms 表 1 行：001 权限 CRUD API（支持按模块分组查询）｜auth/003, model/004

modules 表 1 行：001 模块 CRUD API+模块下权限级联查询+删除引用保护｜auth/003, model/003

web 表 7 行：001 Vite+Vue3+TS+Pinia+Router+Element Plus 骨架+Axios 封装(token 注入/401 拦截)+Vitest 基线｜auth/002；002 登录页+路由守卫｜web/001, auth/002；003 主布局(侧边菜单/顶栏)+动态菜单渲染｜web/002, auth/004；004 用户管理页(含角色分配/启停用/重置密码)｜web/003, users/001, users/002, users/003；005 角色管理页(含权限分配树)｜web/003, roles/001, roles/002；006 权限管理页+模块管理页｜web/003, perms/001, modules/001；007 个人中心改密｜web/003, users/003

integration 表 2 行：001 第三方模块接入指南（鉴权流程/check API 契约/错误码表）｜auth/005；002 e2e 冒烟脚本（shell+curl：登录→授权→check 全链路验证）｜integration/001

总览表按模块统计：功能点数/已完成 0/进行中 0/未开始 数/备注。

- [ ] **Step 2: 验证**

Run: `bash scripts/verify-harness.sh; true`
Expected: 第 3 节出现「功能点计数 = 32」「初始状态全 ⬜」「所有前置依赖 ID 均已定义」三条绿

- [ ] **Step 3: Commit**

```bash
git add .harness/modules/registry.md
git commit -m "feat: registry 预载 32 功能点（P0-P5 全分解+依赖链）"
```

---

### Task 7: context/ 三件套（task / session-state / iteration-log）

**Files:**
- Create: `.harness/context/task.md`、`.harness/context/session-state.md`、`.harness/context/iteration-log.md`

**Interfaces:**
- Consumes: registry ID 体系、tdd-workflow 的 RED 证据格式
- Produces: task.md 模板 11 字段（Sprint ID/所属模块/需求描述/业务背景/前置依赖/验收标准/测试清单/RED 证据/实现说明/状态/评审意见）——Planner 每 Sprint 填写；session-state.md 为跨会话唯一真相源

- [ ] **Step 1: 写 task.md**

上半部：模板字段定义表（字段/填写者/说明，11 字段齐全，状态枚举 PLANNED→IN_PROGRESS→AWAITING_REVIEW→REWORK→DONE｜BLOCKED）；下半部：空槽实例（占位文本「由 Planner 在 kickoff 时填写」），验收标准区注明「每条须映射测试用例」，RED 证据区给出 tdd-workflow 规定的 text 代码块样例格式。

- [ ] **Step 2: 写 session-state.md**

```markdown
# 会话状态
> 本文件是跨会话交接的唯一真相源。任何会话开始必须先读本文件，按「下一步动作」路由（规则见 `AGENTS.md`）。

## 当前阶段
- 状态: 🟡 Harness 就绪，未启动任何 Sprint
- 当前模块: —
- 当前功能点: —

## 下一步动作
Planner kickoff：阅读 `.harness/modules/registry.md` 与 `docs/01-architecture.md`，从 infra/001 开始规划第一个 Sprint，产出 `.harness/context/task.md`。

## 挂起
（无）

## 最近更新
- 2026-08-26: 系统 — Harness 交付，registry 32 个功能点全部 ⬜
```

- [ ] **Step 3: 写 iteration-log.md**

说明行（append-only、倒序、格式 `YYYY-MM-DD: 角色 ID — 一句话结果（关键数据）`）+ 首条记录 `2026-08-26: 系统 — Harness 交付完成，scripts/verify-harness.sh 全绿`。

- [ ] **Step 4: 验证**

Run: `bash scripts/verify-harness.sh; true`
Expected: 第 4 节 task.md 11 个字段检查全绿；死链检测对 `.harness/modules/registry.md`、`docs/01-architecture.md`、`AGENTS.md`（此时 AGENTS.md 尚未建，属预期 ❌，下一任务修复）

- [ ] **Step 5: Commit**

```bash
git add .harness/context/
git commit -m "feat: context 三件套（工作单模板/会话快照/迭代日志）"
```

---

### Task 8: planner.md

**Files:**
- Create: `.harness/planner.md`

**Interfaces:**
- Consumes: `` `.harness/modules/registry.md` ``、`` `.harness/context/session-state.md` ``、`` `.harness/context/task.md` ``、`` `docs/01-architecture.md` ``
- Produces: Planner 角色指令（五章节齐全，被 README/AGENTS 路由引用）

- [ ] **Step 1: 写 prompt**

五章节（`## 角色定位 / 输入源 / 行为流程 / 约束 / 输出要求`）+ 附节「验收标准编写原则」。要点：

- 角色定位：将用户 1-4 句需求展开为单个功能点的工作单；只规划不编码
- 行为流程：①理解需求并对照 `docs/01-architecture.md` 确认业务语义 ②查 registry 校验前置依赖（未完成必须先做前置，禁止跳过）③过大拆分（>4 条验收标准或预估 >6 文件变更必须拆，一次只注册第一个子功能点）④生成 task.md（逐字段填写；每条验收标准标注对应测试意图；声明测试清单要求）⑤registry 对应行 ⬜→🔄 ⑥session-state 更新「当前阶段/当前功能点/下一步动作=Generator」⑦iteration-log 追加一行
- 约束：不写任何业务代码；不改已完成条目；需求与架构冲突时以 architecture 为准并登记 session-state
- 输出要求：列出本次更新的全部文件清单
- 验收标准编写原则：可测试、不可二义（给正反例各 1）、颗粒度适中、每条可映射测试用例（TDD）

- [ ] **Step 2: 验证**

Run: `bash scripts/verify-harness.sh; true`
Expected: 第 2 节 planner.md 五章节全绿

- [ ] **Step 3: Commit**

```bash
git add .harness/planner.md
git commit -m "feat: planner prompt（需求展开+Sprint 规划）"
```

---

### Task 9: generator.md

**Files:**
- Create: `.harness/generator.md`

**Interfaces:**
- Consumes: `` `.harness/context/task.md` ``、`` `.harness/rules/coding-standards.md` ``、`` `.harness/rules/tdd-workflow.md` ``、`` `docs/01-architecture.md` ``
- Produces: Generator 角色指令（TDD 红绿灯硬流程 + 变更清单格式）

- [ ] **Step 1: 写 prompt**

五章节齐全。要点：

- 角色定位：将 task.md 转化为可交付实现；一次只做一个功能点；测试先行不可绕过
- 行为流程：①读 task.md（Deprecated/范围不清→停下澄清）②≤200 字实现思路 ③按 tdd-workflow 执行 RED→GREEN→REFACTOR（RED 证据实时记入 task.md）④按 coding-standards 分层实现（backend: entity/mapper/service/controller/common；frontend: api/store/views/components）⑤全量回归门禁（两命令原文写入）⑥两段式 commit ⑦变更清单 ⑧session-state 更新「待评审」+ iteration-log
- 变更清单格式：新增/修改（含 :行号区间）/删除 三节
- 约束：YAGNI；不改无关代码；不加非必需依赖；发现前置缺失立即停止上报；门禁不过禁止进入评审
- 输出要求：变更清单 + 更新后的 task.md（勾验收标准）+ session-state

- [ ] **Step 2: 验证**

Run: `bash scripts/verify-harness.sh; true`
Expected: generator.md 五章节全绿

- [ ] **Step 3: Commit**

```bash
git add .harness/generator.md
git commit -m "feat: generator prompt（TDD 红绿灯实现流程）"
```

---

### Task 10: evaluator.md

**Files:**
- Create: `.harness/evaluator.md`

**Interfaces:**
- Consumes: `` `.harness/rules/review-criteria.md` ``、`` `.harness/context/task.md` ``、`` `.harness/context/session-state.md` ``、`` `.harness/context/iteration-log.md` ``
- Produces: Evaluator 角色指令（报告模板内嵌自 review-criteria；REWORK/BLOCKED 回写动作）

- [ ] **Step 1: 写 prompt**

五章节齐全。要点：

- 角色定位：严格评审者；反宽容守则原文引入（先问题后优点/证据强制/亲跑门禁/自我说情即警讯）
- 输入源：上列四文件 + Generator 变更清单 + git log/diff
- 行为流程：①核对验收标准逐条 ②核对变更范围（diff 只含应改文件）③亲自运行门禁命令并贴输出 ④按 review-criteria 五维评分（每维附证据）⑤出报告（模板原文内嵌）⑥决策回写：通过→registry 🔄→✅、session-state「下一步动作=取下一功能点(Planner)」；不通过→问题清单写回 task.md 评审意见区、状态改 REWORK、session-state「下一步动作=Generator 返工」；第 2 次 REWORK 仍败→registry ❌、挂起区登记转人工 ⑦iteration-log 追加
- 约束：只评审不修改业务代码（context 文件元数据除外）；不找茬（非功能性锦上添花写改进建议不扣分）；关键否决项绝不放过
- 输出要求：评审报告 + 回写的 context/registry 文件清单

- [ ] **Step 2: 验证**

Run: `bash scripts/verify-harness.sh; true`
Expected: evaluator.md 五章节全绿；此时仅剩 AGENTS.md/README 相关检查未绿

- [ ] **Step 3: Commit**

```bash
git add .harness/evaluator.md
git commit -m "feat: evaluator prompt（反宽容评审+BLOCKED 流转）"
```

---

### Task 11: AGENTS.md（入口锚点）

**Files:**
- Create: `AGENTS.md`

**Interfaces:**
- Consumes: session-state 锚点、三个 prompt 路径、门禁命令
- Produces: CLI 自动加载的路由规则——使 `claude "继续"` 即可正确驱动；死链检测覆盖其引用

- [ ] **Step 1: 写 AGENTS.md**

```markdown
# auth-core — AI Agent 项目地图

## 会话入口规则（最高优先级）
1. 任何会话开始必须先读 `.harness/context/session-state.md`
2. 按 session-state 的「下一步动作」加载对应角色指令：
   - 规划 → 读 `.harness/planner.md` 并严格执行
   - 实现 → 读 `.harness/generator.md` 并严格执行
   - 评审 → 读 `.harness/evaluator.md` 并严格执行
3. 用户输入「继续」时，直接按上述路由执行，无需追问

## 项目
通用权限管理系统（RBAC）：用户/角色/权限/模块管理 + 关联授权 + 外部模块接入鉴权。
技术栈与业务硬语义见 `docs/01-architecture.md`；进度全景见 `.harness/modules/registry.md`。

## 硬性规则
- TDD 强制：任何业务代码必须测试先行，流程见 `.harness/rules/tdd-workflow.md`
- 禁止跳过 Evaluator；禁止自行宣布完成
- 门禁：后端 mvn -q verify；前端 npm run lint && npm run test && npm run build
- 与 `docs/01-architecture.md` 冲突时以它为准并在 session-state 登记
- 文档与交流一律中文

## 命令速查
后端：mvn -q verify / mvn spring-boot:run（backend/）
前端：npm run lint && npm run test && npm run build / npm run dev（frontend/）
环境前置：Java 21、Maven 3.9+、Node 20+、Docker（Testcontainers）、MySQL 8
Harness 自检：bash scripts/verify-harness.sh
```

- [ ] **Step 2: 验证**

Run: `bash scripts/verify-harness.sh; true`
Expected: 第 5 节 AGENTS.md 锚点绿；死链检测中此前预期的 AGENTS.md 待建 ❌ 消失

- [ ] **Step 3: Commit**

```bash
git add AGENTS.md
git commit -m "feat: AGENTS.md 会话入口锚点与硬性规则"
```

---

### Task 12: .harness/README.md（驱动手册）+ 终验

**Files:**
- Create: `.harness/README.md`

**Interfaces:**
- Consumes: 全部既有工件的路径与用法
- Produces: 用户操作手册；verify 脚本第 5 节关键词全绿 → 整体 `ALL CHECKS PASSED`

- [ ] **Step 1: 写 README**

章节：

```markdown
# .harness — AI 驱动开发驾驭系统

概述：Planner → Generator → Evaluator 三智能体循环；CLI(Claude Code/opencode) + DeepSeek Anthropic 兼容后端运行。

## 工作流
（ASCII 图：需求→Planner→Generator→Evaluator→✅下一功能点 / ❌返工 / 连续2次❌BLOCKED 转人工）

## 文件说明
（13 工件表格：路径/用途，与实际一致）

## 首次使用
1. 环境变量（三行 export 原文，DEEPSEEK_API_KEY 由使用者提供）
2. 冒烟自检：
   - bash scripts/verify-harness.sh → ALL CHECKS PASSED
   - 干跑验证：claude "阅读 .harness/planner.md 并严格执行。需求：示例——为系统添加操作日志查询接口" → 应产出符合模板的 task.md 且 registry 出现新条目（验证后还原这些演示改动）
3. 前置软件清单

## 日常迭代（每阶段独立会话）
claude "阅读 .harness/planner.md 并严格执行。需求：<...>"
claude "阅读 .harness/generator.md 并严格执行。开始当前 Sprint。"
claude "阅读 .harness/evaluator.md 并严格执行。"
最简驱动：claude "继续"

## 状态机与异常处理
功能点：⬜→🔄→✅；旁路 ❌/⚠️；REWORK×2→BLOCKED 转人工；事故复盘回写 rules/AGENTS.md

## 关联文档
`docs/01-architecture.md`、`AGENTS.md`、spec 路径
```

- [ ] **Step 2: 全量终验**

Run: `bash scripts/verify-harness.sh`
Expected: 退出码 0，末行 `ALL CHECKS PASSED`；人工核对 spec §8 六条 DoD 逐条满足

- [ ] **Step 3: Commit**

```bash
git add .harness/README.md
git commit -m "feat: harness 驱动手册（DeepSeek 接入/冒烟自检/Sprint 循环）"
```

---

## Self-Review 记录

- Spec coverage：spec §2.1 十三项工件 ↔ Task 2-12 一一对应；§8 DoD 六条 → Task 1 脚本（1/2/3/4/5 条自动校验）+ Task 12 Step 2 人工核对第 6 条干跑；Non-goals 不产生任务 ✓
- Placeholder scan：无 TBD/TODO；所有结构化内容（表/模板/命令/规则）均给出原文或逐行数据 ✓
- Type consistency：路径引用统一反引号风格；状态标记集合、门禁命令、通过线表述在各任务间一致；registry 行数据与 spec §4 逐条一致（auth/001 依赖修正为 infra/003+model/001，与 spec 一致）✓
