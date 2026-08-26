# auth-core AI 驱动开发 Harness 系统设计

- 日期：2026-08-26
- 状态：已确认（用户批准方案 2：预载 Harness）
- 交付范围：**仅 Harness 系统**。RBAC 业务代码后续由本 harness 以 Sprint 循环开发，不在本次交付内。

## 1. 背景与目标

项目目标：通用权限管理系统（用户/角色/权限/模块 CRUD + 用户↔角色、角色↔权限关联、权限挂接模块；模块 = 可独立运行接入权限系统的服务），效果参照 [api-rbac](https://github.com/laazua/api-rbac)。

技术栈（已固化）：Java 21 + Spring Boot 3.x + MyBatis-Plus + Spring Security/JWT + MySQL 8；前端 Vue3 + TypeScript + Vite + Pinia + Element Plus。

开发模式：TDD 测试先行，按功能模块迭代，各模块解耦。

交付物：一套可直接使用的 `.harness/` 三智能体开发系统（Planner → Generator → Evaluator），运行时为 Claude Code/opencode CLI + DeepSeek Anthropic 兼容后端，并附上下文驱动手册。

设计参照：
- [Anthropic: Harness design for long-running applications](https://www.anthropic.com/engineering/harness-design-long-running-apps) —— 生成/评审分离、上下文重置 + 结构化交接、Sprint 契约
- [laazua/audio-text .harness](https://gitee.com/laazua/audio-text/tree/main/.harness) —— 目录结构、registry/session-state/task 工件格式

## 2. 总体架构

### 2.1 目录结构（交付物清单）

```
auth-core/
├── AGENTS.md                      # 项目地图 + 硬性规则（CLI 自动加载的入口锚点）
├── .harness/
│   ├── README.md                  # 总览 + 驱动手册（命令模板 / DeepSeek 接入 / 冒烟自检）
│   ├── planner.md                 # Planner Agent prompt
│   ├── generator.md               # Generator Agent prompt
│   ├── evaluator.md               # Evaluator Agent prompt
│   ├── context/
│   │   ├── task.md                # 当前 Sprint 工作单（文件头含模板定义，初始为空槽状态）
│   │   ├── session-state.md       # 会话状态快照（跨会话交接唯一真相源）
│   │   └── iteration-log.md       # 迭代历史（append-only）
│   ├── modules/
│   │   └── registry.md            # 功能点注册表（预载 RBAC 全量分解，见 §4）
│   └── rules/
│       ├── coding-standards.md    # 编码规范（Java/Vue 双栈）
│       ├── review-criteria.md     # 5 维评分 + 一票否决清单
│       └── tdd-workflow.md        # RED→GREEN→REFACTOR 流程规范
└── docs/
    └── 01-architecture.md         # 固化架构：选型/分层/ER/API 规范/业务语义
```

### 2.2 上下文分层模型

| 层 | 文件 | 变更频率 | 作用 |
|----|------|---------|------|
| 指令层 | planner/generator/evaluator.md | 基本不变 | Agent 的角色定义与流程 |
| 规则层 | rules/*、docs/01-architecture.md、AGENTS.md | 低频 | 编码与评审的仲裁标准 |
| 状态层 | context/task.md、session-state.md、iteration-log.md、modules/registry.md | 每 Sprint | 当前进度与工作单 |

Agent 通过**读文件**获得全部上下文，不依赖对话历史。每次阶段切换开启新会话（context reset），由 `session-state.md` 承载结构化交接——规避长上下文退化与 context anxiety。

### 2.3 工作流与状态机

```
用户需求(1-4句)
  → Planner：查 registry 依赖 → 展开 task.md → registry ⬜→🔄 → session-state 更新
  → Generator：读 task.md → TDD 红绿灯实现 → 变更清单 → session-state 标"待评审"
  → Evaluator：核对验收标准 → 跑 mvn verify/lint → 5 维评分
      ├─ ✅ 通过(≥7 且无否决项)：registry 🔄→✅ → iteration-log → 取下一功能点(新会话)
      └─ ❌ 不通过：问题清单写回 task.md → Generator 返工(REWORK)
                └─ 同一功能点连续 2 次 REWORK → ❌ BLOCKED 转人工
```

功能点状态机：`⬜ 未开始 → 🔄 进行中 → ✅ 完成`；旁路：`❌ 阻塞`、`⚠️ 有缺口`。

## 3. Agent 职责规格

三个 prompt 文件均包含固定章节：角色定位 / 输入源 / 行为流程 / 约束 / 输出要求。

### 3.1 Planner
- 输入源：用户需求、registry.md、session-state.md、docs/01-architecture.md
- 职责：将需求展开为单个功能点的 task.md；校验前置依赖（未完成必须先做前置）；功能点过大（>4 条验收标准或预估 >6 个文件变更）必须拆分；一次只注册一个子功能点到 registry
- 验收标准编写原则：可测试、不可二义、适度颗粒度；每条验收标准必须可映射到测试用例（TDD 要求）
- 约束：不写任何业务代码；不修改已完成的 registry 条目
- 输出：更新后的 context/task.md、modules/registry.md、session-state.md

### 3.2 Generator
- 输入源：task.md、coding-standards.md、tdd-workflow.md、docs/01-architecture.md、代码仓库
- 职责：简述实现思路（≤200 字）→ 按 TDD 流程实现 → 输出变更清单
- TDD 流程（硬约束，详见 §5.3）：
  1. 从验收标准导出测试清单写入 task.md
  2. 先写测试，运行确认 RED，将失败输出摘录记入 task.md 的"RED 证据"槽位
  3. 最小实现至 GREEN
  4. 重构 + 全量回归（`mvn -q verify` 全绿 / 前端 lint+build+test 通过）
  5. 提交规范：推荐两段式 commit（`test:` 先于 `feat:`）；单 commit 时 task.md 必须含 RED 证据
- 约束：YAGNI，不改无关代码，不引入非必需依赖；前置缺失即停止并上报
- 输出：代码 + 变更清单 + 已更新的 session-state.md（标"待评审"）

### 3.3 Evaluator（反宽容调校）
- 输入源：Generator 产出 + 变更清单、task.md、review-criteria.md、coding-standards.md、iteration-log.md
- 流程：逐条核对验收标准 → 核对变更范围（只改该改的文件）→ 亲自运行门禁命令（不信任 Generator 自述）→ 5 维评分 → 报告
- 反宽容条款：先找问题再谈优点；每个维度的分数必须附具体证据（文件:行号 或 命令输出）；发现"自我说情"倾向即视为不通过信号
- 决策：总分 ≥7 且无否决项 → 通过，更新 registry/iteration-log/session-state；否则问题清单写回 task.md 并标 REWORK
- 一票否决项（详见 §5.2）

## 4. Registry 预载内容（RBAC 功能点分解）

共 32 个功能点，全部初始 ⬜。数据模型六实体：`sys_user`、`sys_role`、`sys_permission`(FK module_id NOT NULL)、`sys_module`、关联表 `sys_user_role`、`sys_role_permission`。

| ID | 功能点 | 前置依赖 |
|----|--------|---------|
| **infra — 基础设施** | | |
| infra/001 | Maven 项目骨架 + Spring Boot 启动 + actuator 健康检查 | — |
| infra/002 | MySQL 接入 + MyBatis-Plus 配置 + Flyway 迁移机制 | infra/001 |
| infra/003 | 统一响应体 Result<T> + 全局异常处理 + Bean Validation | infra/001 |
| infra/004 | 测试基础设施（Testcontainers MySQL 基座 + 测试命名/分层约定） | infra/002 |
| **model — 数据模型** | | |
| model/001 | sys_user 表迁移 + 实体 + Mapper | infra/002 |
| model/002 | sys_role 表迁移 + 实体 + Mapper | infra/002 |
| model/003 | sys_module 表迁移 + 实体 + Mapper | infra/002 |
| model/004 | sys_permission 表迁移 + 实体 + Mapper（FK module_id） | model/003 |
| model/005 | sys_user_role + sys_role_permission 关联表 | model/001, model/002, model/004 |
| model/006 | 种子数据迁移（内置 admin + 示例角色/权限/模块） | model/005 |
| **auth — 认证** | | |
| auth/001 | Spring Security 无状态基线 + BCrypt 编码器 | infra/003, model/001 |
| auth/002 | 登录接口 POST /api/v1/auth/login 签发 JWT（HS256，密钥/有效期可配） | auth/001 |
| auth/003 | JWT 校验过滤器 + SecurityContext 注入 | auth/001, auth/002 |
| auth/004 | GET /api/v1/auth/me（用户 + 角色 + 权限集合） | auth/003, model/005 |
| auth/005 | 权限校验 API POST /api/v1/auth/check（供外部模块集成调用） | auth/003 |
| auth/006 | 登出策略（无状态 JWT v1：接口 + 失效语义说明） | auth/003 |
| **users/roles/perms/modules — 管理 API** | | |
| users/001 | 用户 CRUD API（分页/条件查询/创建加密/更新/启停用/删除） | auth/003, model/001 |
| users/002 | 用户-角色分配 API（批量设置） | users/001, model/005 |
| users/003 | 密码修改 + 管理员重置 | users/001 |
| roles/001 | 角色 CRUD API | auth/003, model/002 |
| roles/002 | 角色-权限分配 API（批量设置） | roles/001, model/005 |
| perms/001 | 权限 CRUD API（支持按模块分组查询） | auth/003, model/004 |
| modules/001 | 模块 CRUD API + 模块下权限级联查询 + 删除引用保护 | auth/003, model/003 |
| **web — 前端** | | |
| web/001 | Vite+Vue3+TS+Pinia+Router+Element Plus 骨架 + Axios 封装（token 注入/401 拦截）+ Vitest 基线 | auth/002 |
| web/002 | 登录页 + 路由守卫 | web/001, auth/002 |
| web/003 | 主布局（侧边菜单/顶栏）+ 动态菜单渲染 | web/002, auth/004 |
| web/004 | 用户管理页（含角色分配/启停用/重置密码） | web/003, users/001, users/002, users/003 |
| web/005 | 角色管理页（含权限分配树） | web/003, roles/001, roles/002 |
| web/006 | 权限管理页 + 模块管理页 | web/003, perms/001, modules/001 |
| web/007 | 个人中心改密 | web/003, users/003 |
| **integration — 集成** | | |
| integration/001 | 第三方模块接入指南（鉴权流程/check API 契约/错误码表） | auth/005 |
| integration/002 | e2e 冒烟脚本（shell+curl：登录→授权→check 全链路验证） | integration/001 |

关键路径：infra → model → auth → 管理 API → web。users/perms/modules 各 API 在 auth/003 后可并行推进。

## 5. 规则层规格

### 5.1 coding-standards.md 要点
- Java 21：可用 record/text blocks/switch 表达式；禁 `System.out`，统一 slf4j；文件 ≤500 行、方法 ≤50 行；DTO/VO 与实体分离；分层单向调用 controller→service→mapper；禁止跨包反向依赖
- MyBatis-Plus：逻辑外键由 service 层维护引用校验；分页用插件统一实现
- 安全：密码仅 BCrypt；JWT 密钥来自环境变量（application.yml 不落真实密钥）；参数校验用 Bean Validation 注解
- Vue3：composition API `<script setup lang="ts">`；TS strict；组件 PascalCase；API 层独立于组件；eslint + vue-tsc 门禁
- 数据库命名 `snake_case`，Java `camelCase`，表前缀 `sys_`

### 5.2 review-criteria.md
5 维评分（每维 1-10，附证据）：功能正确性 / 代码质量 / 规范遵守 / TDD 执行度 / 安全性。
通过条件：总分 ≥7 且无一票否决项。
一票否决项：
1. 功能缺失（task.md 明确要求未实现）
2. 安全漏洞（SQL 注入、越权访问、明文密码、硬编码密钥）
3. 明确 Bug（核心逻辑错误）
4. 无测试先行证据（缺 RED 记录且 commit 顺序无法证明 test-first）
5. RBAC 业务模型偏差（权限脱离模块存在、绕过角色的直接用户授权、删除保护语义被绕过等）
6. 全量回归失败或门禁命令不通过

> 勘误（2026-08-26，终审）：总分定义为五维平均分（1-10 量表），通过线 ≥7 指平均分。

### 5.3 tdd-workflow.md
- 测试金字塔：单元测试（JUnit5+Mockito）/ Web 层（MockMvc）/ 集成测试（@SpringBootTest + Testcontainers MySQL）；前端 Vitest + Vue Test Utils（store/composable 必测，登录守卫等关键交互必测）
- RED→GREEN→REFACTOR 循环 + 证据记录规范（task.md 槽位）
- 门禁命令：后端 `mvn -q verify`；前端 `npm run lint && npm run test && npm run build`

> 增补（2026-08-26，用户要求）：引入强制冒烟机制——新增 `scripts/smoke.sh` 随功能点增量维护用例（infra→health、auth→curl 链路、web→构建产物……最终形态对齐 registry integration/002）；Generator 全量回归后必须自跑并记入 task.md 新字段「冒烟记录」，Evaluator 必须亲自复跑；冒烟未执行或不通过列入一票否决。详见 `.harness/rules/tdd-workflow.md` 冒烟节。

### 5.4 docs/01-architecture.md 要点（固化业务语义，防偏差仲裁依据）
- Monorepo：`backend/`（Spring Boot 单 Maven 模块，package-by-layer）+ `frontend/`
- 统一响应 `Result{code,message,data}`；错误码分段（0 成功 / 4xx 业务 / 401·403 HTTP 语义）；列表分页参数 `page/size`
- 业务硬语义：权限必须归属模块；用户获得权限的唯一路径是 角色（RBAC0，无直接用户-权限授权）；username/role.code/permission.code/module.code 全局唯一；删除采用物理删除 + 引用校验拒绝（有关联引用时报业务错误码）
- JWT：HS256，payload 含 uid/username，默认有效期 2h 可配

## 6. 上下文驱动手册（README.md 内容）

- DeepSeek 后端接入环境变量：`ANTHROPIC_BASE_URL=https://api.deepseek.com/anthropic`、`ANTHROPIC_AUTH_TOKEN`、`ANTHROPIC_MODEL=deepseek-chat`
- 阶段驱动命令模板（每阶段独立会话）：

```bash
claude "阅读 .harness/planner.md 并严格执行。需求：<...>"
claude "阅读 .harness/generator.md 并严格执行。开始当前 Sprint。"
claude "阅读 .harness/evaluator.md 并严格执行。"
# 最简驱动：claude "继续"   ← AGENTS.md 锚点自动路由到当前应处阶段
```

- AGENTS.md 锚点规则："任何会话开始必须先读 `.harness/context/session-state.md`，按其中『下一步动作』路由到对应角色 prompt"
- Sprint 循环图 + 功能点状态机说明 + 冒烟自检清单

## 7. 异常处理

- BLOCKED：连续 2 次 REWORK 未通过 → registry 标 ❌，session-state 挂起区登记，转人工
- 事故复盘：Agent 错误根因记入 iteration-log；若属规则缺口则回写 rules/* 或 AGENTS.md 防复发
- 上下文防护：任何 Agent 发现场景与 docs/01-architecture.md 冲突时，以 architecture 为准并在 session-state 登记，不得静默偏离

## 8. Harness 自身验收标准（本次交付的 Definition of Done）

1. §2.1 清单中所有文件存在，内容覆盖对应章节规格
2. 引用完整性：所有 prompt 中引用的文件路径真实存在；registry 所有前置依赖 ID 均有定义；无死链
3. task.md 模板字段齐全：Sprint ID/所属模块/需求描述/业务背景/验收标准 checklist/测试清单/RED 证据槽位/前置依赖/状态/评审意见区
4. AGENTS.md 锚点指向 session-state.md；README 含 DeepSeek 接入与三阶段命令模板及冒烟自检清单
5. registry 含 32 个功能点且与 §4 表一致，状态全 ⬜
6. 干跑验证：模拟 Planner 对一条样例需求执行指令，能产出符合模板的 task.md（人工冒烟，作为 README 自检清单的一项）

## 9. 明确不做（Non-goals）

- 不实现任何 RBAC 业务代码/页面（由 harness 后续迭代产出）
- 不做自动化循环脚本（Ralph 式），当前手动 CLI 驱动足够（YAGNI）
- 不引入 Redis/Docker Compose/微服务网关等 v1 非必需组件
