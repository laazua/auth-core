## 功能点注册表（Registry）

本表是全系统的唯一功能点台账：每个功能点拥有全局唯一 ID（如 `infra/001`），被三个 prompt 与 context 文件引用。状态图例：⬜ 未开始 / 🔄 进行中 / ✅ 完成 / ❌ 阻塞 / ⚠️ 有缺口

> 最后更新：2026-10-08（sprint-081 **web/023 ✅ PASS 9.4/10** 评审通过、047 期遗留收口完成；sprint-080 积压清零五项 ✅；modules/003 预留）

## 阶段总览

| 模块 | 功能点数 | 已完成 | 进行中 | 未开始 | 备注 |
|------|----------|--------|--------|--------|------|
| infra | 4 | 4 | 0 | 0 | 全部完成 |
| model | 12 | 12 | 0 | 0 | 承接 infra/002，为全部业务 API 提供数据层 |
| auth | 6 | 0 | 0 | 6 | 安全基线，所有业务 API 的前置依赖 |
| users | 3 | 0 | 0 | 3 | — |
| roles | 2 | 0 | 0 | 2 | — |
| perms | 1 | 0 | 0 | 1 | — |
| modules | 2 | 2 | 0 | 0 | 模块=外部服务语义；modules/001 ✅、modules/002 ✅（sprint-069 评审通过），拆分预留 modules/003/web/038 见该行备注 |
| web | 49 | 46 | 1 | 0 | **web/048 ✅ 全组收口（048a sprint-092 PASS 8.4/10 + 048b sprint-093 PASS 8.5/10，2026-10-13：GET /permissions/tree 数据源+RoleVO.permissions 回显，抽屉树与回显两断点修复，剩余提交 405 归 049/按钮门禁归 050）**；web/046 ✅ 收口（2026-10-12 sprint-090 评审通过 9.6/10：用户-角色绑定数据回显与 PUT 契约暗修）；**web/047 ✅ 收口（2026-10-13 sprint-094 评审通过 8.6/10：V8 补 user:assign-role 计数 17→18+抽屉按 userId 回显——用户报障「操作没有可以操作的地方」用户-角色绑定链路端到端闭环）**；**web/049 ✅ 收口（2026-10-14 sprint-095 评审通过 9.1/10：api/role.ts POST→PUT+7 处 readOnly 清偿——用户报障角色-权限分配三断点（048a tree/048b 回显/049 提交 405）全部闭合）**；web/045 保留 BaseInput 候选；web/044 ✅ 收口（2026-10-12 sprint-089 评审通过 9.8/10：四管理页搜索关键词 AND→OR）；web/043 ✅ 收口（2026-10-10 sprint-088 评审通过 9.9/10）；web/036 进行中（036b ✅ sprint-084 评审通过、036c 预留）；web/042 ✅ 收口（2026-10-10 sprint-087 评审通过 9.7/10）；web/041 ✅ 收口（2026-10-10 sprint-086 评审通过）；web/011 全系 ✅ 收口（011a/b/c，2026-10-08 sprint-083 评审通过）；web/011 ✅ 收口（2026-10-08 sprint-082 评审通过）；web/022 废弃⚠️；其余 36 项 ✅ |
| integration | 2 | 0 | 0 | 2 | 收尾验证，依赖全部前置就绪 |

## infra — 基础设施

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| infra/001 | Maven 项目骨架 + Spring Boot 启动 + actuator 健康检查 | — | ✅ | sprint-001 评审通过（2026-08-26，均分 9.8） |
| infra/002 | MySQL 接入 + MyBatis-Plus 配置 + Flyway 迁移机制 | infra/001 | ✅ | sprint-002 复审通过（2026-08-26，均分 8.8；配置化接入口径见挂起区） |
| infra/003 | 统一响应体 Result<T> + 全局异常处理 + Bean Validation | infra/001 | ✅ | sprint-003 评审通过（2026-08-26，均分 9.8） |
| infra/004 | 测试基础设施（Testcontainers MySQL 基座 + 测试命名/分层约定） | infra/002 | ✅ | sprint-033 评审通过（单元测试验证，集成测试需 Docker） |

## model — 数据模型

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| model/001 | sys_user 表迁移+实体+Mapper | infra/002 | ✅ | sprint-004 评审通过（2026-08-26，均分 9.8；真实库验收） |
| model/002 | sys_role 表迁移+实体+Mapper | infra/002 | ✅ | sprint-005 评审通过（2026-08-26，均分 9.8） |
| model/003 | sys_module 表迁移+实体+Mapper | infra/002 | ✅ | sprint-006 评审通过（2026-08-26，均分 10.0；集成测试基座就位） |
| model/004 | sys_permission 表迁移+实体+Mapper(FK module_id) | model/003 | ✅ | sprint-007 评审通过（2026-08-26，均分 10.0） |
| model/005 | sys_user_role+sys_role_permission 关联表 | model/001, model/002, model/004 | ✅ | sprint-008 评审通过（均分 10.0） |
| model/006 | 种子数据迁移（内置 admin + 示例角色/权限/模块） | model/005 | ✅ | sprint-009 评审通过（均分 10.0） |
| model/007 | 抽取 UserService 接口（Controller 依赖接口） | model/005 | ✅ | sprint-050 评审通过（2026-09-04，均分 9.8） |
| model/008 | 抽取 RoleService 接口（Controller 依赖接口） | model/007, model/005 | ✅ | sprint-051 评审通过（2026-09-04，均分 9.6） |
| model/009 | 抽取 PermissionService 接口（Controller 依赖接口） | model/008, model/005 | ✅ | sprint-052 评审通过（2026-09-04，均分 9.8） |
| model/010 | 抽取 ModuleService 接口（Controller 依赖接口） | model/009, model/005 | ✅ | sprint-053 评审通过（2026-09-04，均分 9.8） |
| model/011 | 抽取 AuthService 接口（Controller 依赖接口） | model/010, model/005 | ✅ | sprint-054 评审通过（2026-09-04，均分 9.8） |
| model/012 | 更新所有 Controller 层依赖接口 | model/011 | ✅ | sprint-055 验收确认（2026-09-04，已完成） |

## auth — 认证授权

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| auth/001 | Spring Security 无状态基线 + BCrypt 编码器 | infra/003, model/001 | ✅ | sprint-010 评审通过（均分 10.0） |
| auth/002 | 登录接口 POST /api/v1/auth/login 签发 JWT | auth/001 | ✅ | sprint-011 评审通过（均分 10.0） |
| auth/003 | JWT 校验过滤器 + SecurityContext 注入 | auth/001, auth/002 | ✅ | sprint-012 评审通过（均分 10.0） |
| auth/004 | GET /api/v1/auth/me（用户+角色+权限集合） | auth/003, model/005 | ✅ | sprint-013 评审通过（均分 10.0） |
| auth/005 | 权限校验 API POST /api/v1/auth/check（供外部模块集成调用） | auth/003 | ✅ | sprint-014 评审通过（均分 10.0） |
| auth/006 | 登出策略（无状态 JWT v1：接口+失效语义说明） | auth/003 | ✅ | sprint-015 评审通过（均分 10.0） |

## users — 用户管理

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| users/001 | 用户 CRUD API（分页/条件查询/创建加密/更新/启停用/删除） | auth/003, model/001 | ✅ | sprint-016 评审通过（均分 10.0） |
| users/002 | 用户-角色分配 API（批量设置） | users/001, model/005 | ✅ | sprint-017 评审通过（均分 10.0） |
| users/003 | 密码修改+管理员重置 | users/001 | ✅ | sprint-018 评审通过（均分 10.0） |

## roles — 角色管理

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| roles/001 | 角色 CRUD API | auth/003, model/002 | ✅ | sprint-019 评审通过（均分 10.0） |
| roles/002 | 角色-权限分配 API（批量设置） | roles/001, model/005 | ✅ | sprint-020 评审通过（均分 10.0） |

## perms — 权限管理

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| perms/001 | 权限 CRUD API（支持按模块分组查询） | auth/003, model/004 | ✅ | sprint-021 评审通过（均分 10.0） |

## modules — 模块管理

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| modules/001 | 模块 CRUD API+模块下权限级联查询+删除引用保护 | auth/003, model/003 | ✅ | sprint-022 评审通过（均分 10.0） |
| modules/002 | 模块（外部服务）统一访问入口：`ALL /api/v1/gateway/{moduleCode}/**` 认证（有效 JWT）+ 模块级准入（§6.1 权限归属模块、§6.5 停用模块权限无效）+ 按 `sys_module.base_url` 反向转发（方法/查询串/请求体透传、剥离 Authorization、下游响应状态码与体原样透传；错误码 1304 模块不存在/1305 base_url 未配置 → 400，无权限 → 403+1403，未认证 → 401+1401）。**总体需求拆分**：本行=后端网关（sprint-069 本次）；预留 modules/003 服务级接入凭证与服务登录（前置架构 §3 字段契约仲裁或 sys_user 服务账号方案，⬜ 未注册）；预留 web/038 模块管理页「外部服务」语义适配（⬜ 未注册） | auth/003, auth/004, modules/001, model/004, model/010 | ✅ | sprint-069 评审通过（2026-09-29，均分 9.0/10，4 条 AC 全满足，测试先行 5 failed RED→6/6 GREEN，冒烟 51 用例 modules-002 通过；基线对照豁免否决项 6；语义裁量与 §4 透传口径登记见 session-state 挂起区；拆分预留 modules/003/web/038 不变） |

## web — Web 前端

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| web/001 | Vite+Vue3+TS+Pinia+Router+Element Plus 骨架+Axios 封装(token 注入/401 拦截)+Vitest 基线 | auth/002 | ✅ | sprint-023 评审通过（均分 10.0） |
| web/002 | 登录页+路由守卫 | web/001, auth/002 | ✅ | sprint-024 评审通过（均分 10.0） |
| web/003 | 主布局(侧边菜单/顶栏)+动态菜单渲染 | web/002, auth/004 | ✅ | sprint-025 评审通过（均分 10.0） |
| web/004 | 用户管理页(含角色分配/启停用/重置密码) | web/003, users/001, users/002, users/003 | ✅ | sprint-026 评审通过（均分 10.0） |
| web/005 | 角色管理页(含权限分配树) | web/003, roles/001, roles/002 | ✅ | sprint-027 评审通过（均分 10.0） |
| web/006 | 权限管理页+模块管理页 | web/003, perms/001, modules/001 | ✅ | sprint-028 评审通过（均分 10.0） |
| web/007 | 个人中心改密 | web/003, users/003 | ✅ | sprint-029 评审通过（均分 10.0） |
| web/008 | UI现代化优化+白天/晚上模式切换 | web/001, web/003 | ✅ | sprint-032 评审通过（v3 精致现代化，不视觉疲劳） |
| web/009 | 修复侧边栏系统管理菜单 404 问题（路由权限与菜单权限不一致） | web/003, web/004, web/005, web/006 | ✅ | sprint-034 评审通过 |
| web/010 | 修复侧边栏系统管理菜单 404 问题（路由未注册到路由器） | web/009 | ✅ | sprint-035 评审通过（2026-09-03，4 条验收标准） |
| web/011 | 修复前端构建错误与失败测试 | web/010 | ✅ | **sprint-082 规划（2026-10-08）：子功能点 web/011b 前端失败测试收敛**（15 failed→0：auth-token 2/LoginView 2/IndexView 11，4 AC/5-6 文件顶格）；011a 修复 TS 编译错误已达成（build ✓，web/026/027 以其为前置均 ✅，sprint-056 kickoff 在案）；预留 011c=全量 lint 卡死（EXIT=124 超时）治理；**sprint-082 评审通过 9.3/10（2026-10-08 Evaluator：子功能点 web/011b ✅，4 AC 亲核全满足，test 15→0 failed|308 纯测试侧 5 文件零产品改动，RED 整树复现 10 failed=RED-2 精确，冒烟 63=54✅+7❌+2⏭️ 零回归且 web-011b ✅，断言 7 删 7 等强替换，六否决项零命中；行范围「构建错误+失败测试」双面收口——011a build ✓+011b test 0；**011c 全量 lint 卡死治理为预留独立项**（124 超时+预存 196 归其，不属本行标题语义）** |
| web/011c | 全量 lint 门禁恢复——`eslint .` 卡死（EXIT=124）治理 + 全量 Error 清零（规划实测 4559 Error/60 文件：prettier 4536 自动 + 手工 23；any 171 Warning 登记 011d 预留）。**由 web/011 行预留独立转正注册**（011 行 ✅ 约束不改），**案B 单波豁免用户裁决 2026-10-08 问答在案**（≈66 文件超 6 熔断：机械段 diff -w 空 + 手工段 11 文件逐条 + 三线门禁证零语义） | web/011 | ✅ | sprint-083 实现完成待评审（2026-10-08 Generator：R1 钉死根因=无 ignorePatterns 扫 dist 29 bundle/1.9M；4 AC 已勾——lint:check exit 0（0 error/165 warning，Error 4559→0）、案A 等价证明 50 机械+8 手工+1 配置零夹带、test 308/308、冒烟 64=55✅+7❌+2⏭️ 零回归；59 文件单波（案B 豁免 ≤70 边界）、提交 9e82973 test→ec7bbc4 feat；any 165=171−6 归 011d；评审通过 9.3/10（2026-10-08 Evaluator：4 AC 亲核全满足，lint:check 11s exit 0 零 dist、0 error/165 warning 零新增 any、RED 复现 124/4559 规则逐条、案A 抽查 3 文件等价+手工 117 行复核、冒烟 64=55✅+7❌+2⏭️ 零回归 web-011c ✅、六否决零命中；59 文件案B 豁免边界内；any 165 归 011d） |
| web/012 | 前端区域背景色差异化优化（登录页/侧边栏/顶栏/主内容区视觉层级区分） | web/008 | ✅ | sprint-036 评审中 |
| web/013 | 修复登录页密码输入框默认明文显示问题 | web/002 | ✅ | sprint-037 完成 |
| web/014 | 修复用户下拉菜单功能（个人中心/设置/修改密码/退出登录点击无效） | web/003, web/007 | ✅ | sprint-038 完成 |
| web/015 | 修复 DefaultLayout.vue 缺失渐变背景导致页面纯白问题 | web/001, web/008 | ✅ | sprint-040 随 web/016 一并评审通过（均分 9.6/10） |
| web/016 | 修复 CSS 主题变量源 variables.scss 未注入样式入口导致全站 UI 纯白 | web/001, web/008 | ✅ | sprint-040 评审通过（均分 9.6/10，浏览器实证渐变恢复） |
| web/017 | 修复顶栏用户下拉菜单 UI 突兀与尺寸异常（图标拉伸成 68~96px 巨图致行高 78/106px 不一、弹层 EP 默认白底 4px 圆角未主题化、暗色弹层白底近白字不可读） | web/014, web/008, web/016 | ✅ | **sprint-080 评审通过 9.3/10**（补评审关闭 sprint-041 欠账；RED fe0e402+GREEN a232011 两段实证，dropdown-style.spec AC1-4 源级断言全绿亲跑；task 快照被覆盖；2026-09-29） |
| web/018 | 修复侧边栏菜单图标不显示（图标字符串未解析为组件） | web/001, web/003, web/008 | ✅ | sprint-042 评审通过（2026-09-02，4 条验收标准，main.ts 全局注册 Element Plus 图标） |
| web/019 | 修复侧边栏菜单图标尺寸过大（SVG 图标缺少显式 width/height，font-size 不生效） | web/001, web/003, web/008, web/018 | ✅ | sprint-043 评审通过（2026-09-02，4 条验收标准，Sidebar.vue 三处 .sidebar__icon 显式定宽 16×16px） |
| web/020 | 移除侧边栏底部折叠按钮，仅保留顶栏左侧折叠按钮 | web/003, web/019 | ✅ | sprint-044 评审通过（2026-09-03，4 条验收标准） |
| web/021 | 移除标签页操作区域（关闭其他标签、关闭所有标签），仅保留刷新按钮 | web/003, web/020 | ✅ | sprint-045 评审通过（2026-09-03，4 条验收标准） |
| web/022 | 移除标签页操作区域整个红框部分（含刷新按钮），仅保留标签页栏 | web/003, web/021 | ⚠️ | sprint-046 实现有误（需求理解偏差，已废弃） |
| web/023 | 移除整个标签页栏（TagsView），仅保留面包屑导航 | web/003, web/020 | ✅ | **sprint-081 PASS 9.4/10**（2026-10-08 Evaluator 亲测：mvn 154 4F+2E=基线、test 15\|293 逐条零新增、lint 124=基线+定向 0 新、build ✓、冒烟 62=53✅7❌2⏭️ 精确、RED worktree 整树复现 7 failed\|11 passed；6 文件=删孤儿 TagsView.vue 420 行；047 期 4 AC 重建口径评审；评估 9.5/9.5/9/9.5/9.5） |
| web/024 | 修复主内容区图标尺寸异常（ElButton icon prop 等 EP 图标缺少显式尺寸被 flex 拉伸） | web/003, web/007, web/008, web/016 | ✅ | **sprint-080 评审通过 8.4/10**（补评审关闭 sprint-048 欠账；**考古风险登记**：无独立实现 commit（被 2ac5581/1d934de 大杂烩吸收），以当前态考古认定实现存活（Breadcrumb 13×13、profile__header-icon、audit spec 1/1 绿）；判退规则未触发；TDD 7.5/文档 7.5=证据链断裂扣分；2026-09-29） |
| web/025 | 优化主内容区图标与文字间距（ProfileView header、action buttons、Breadcrumb 等） | web/003, web/007, web/008, web/016, web/024 | ✅ | sprint-049 评审通过（2026-09-03，4 条验收标准，平均分 9.8/10，header gap 24px、action gap 16px、breadcrumb margin-right 12px） |
| web/026 | 修复登录成功后不跳转首页（API 基础路径缺少 /v1 导致 /auth/me 404） | web/002, web/011a | ✅ | sprint-057 评审通过（2026-09-07，4 条验收标准，门禁+冒烟通过） |
| web/027 | 修复远程开发环境 CORS 与代理配置导致的 403 错误 | web/011a, web/026 | ✅ | sprint-059 评审通过（2026-09-08，3 条验收标准，门禁+冒烟通过） |
| web/029 | 修复侧边栏菜单导航404问题（动态路由未加载与菜单路径错误） | web/010, web/026, web/027 | ✅ | sprint-060 评审通过（2026-09-09，平均分 8.6/10，门禁+冒烟通过，router.spec.ts 9 passed, useMenu.spec.ts 10 passed, system-menu-path-fix.spec.ts 6 passed） |
| web/030 | 修复权限管理页模块下拉选项接口 404（/modules/permissions/all 后端接口缺失） | web/006, modules/001, perms/001 | ✅ | **sprint-080 评审通过 9.2/10**（补评审关闭 sprint-061 欠账；task 快照 1d934de 完整 AWAITING_REVIEW，接口双端存活 ModuleController:151+api/module.ts:36；TDD 8.5=大杂烩提交扣分；2026-09-29） |
| web/031 | 修复用户管理新增/编辑抽屉确认按钮不可点击问题（:loading 绑定抽屉可见性而非提交状态） | web/004, web/011 | ✅ | **sprint-080 评审通过 9.1/10**（补评审关闭 sprint-062 欠账；`:loading="submitting"` 实证 UserFormDrawer:288、spec 4/4 亲跑绿；task 快照被 063 覆盖 AC 从 registry 重建；2026-09-29） |
| web/032 | 修复 SPA 路由刷新页面 404 问题（History 模式回退配置） | web/001 | ✅ | **sprint-080 评审通过 9.1/10**（补评审关闭 sprint-063 欠账；task 快照 c49bed0 完整，historyApiFallback:43+try_files:29 存活；2026-09-29） |
| web/033 | 登录页新增显示密码功能（密码框可见性切换按钮，可切换明文/掩码显示） | web/002, web/013 | ✅ | sprint-064 评审通过（2026-09-28，平均分 8.9/10，4 条验收标准全满足，测试先行 3/3，冒烟 web-033 通过；预存红项基线对照） |
| web/034 | 系统管理菜单用户/角色路由迁移至 /system/users 与 /system/roles（修复访问 404，统一 URL 体系） | web/003, web/004, web/005, web/010, web/029 | ✅ | sprint-065 评审通过（2026-09-28，平均分 8.8/10，4 条 AC 全满足，测试先行 9 RED→28/28 GREEN，冒烟 web-034 通过；基线对照豁免否决项 6） |
| web/035 | 修复登录后首次导航受保护路由落 404（动态路由首载 next({ ...to }) 携带 NotFound name 陷阱） | web/001, web/029, web/034 | ✅ | sprint-066 评审通过（2026-09-28，平均分 9.1/10，4 条 AC 全满足，测试先行 2 failed RED→14/14 GREEN，冒烟 web-035 通过；基线对照豁免否决项 6） |
| web/036 | 暗色模式全站 UI 协调治理（根因：自研 `--color-*` data-theme 与 Element Plus `--el-*` 两套变量体系只切一套，EP 暗色 css-vars 未接入、html.dark 未挂）。**拆分**：web/036a 接入 EP 暗色变量体系 ✅ / web/036b 局部白底缺陷修复（BaseTable 遮罩失效选择器、settings 非法 variant prop、组件级覆盖补漏、暗色系协调映射）/ web/036c 打磨与口径治理——**已拆分：036c1 口径治理 ✅ **sprint-085 评审通过（2026-10-09，均分 9.5/10，4 AC 亲核全满足，RED 四路独立复现 4×EXIT=1+变异 true/false，门禁五线亲跑=基线，冒烟 66=60✅+4❌+2⏭️ ❌恰=登记四条，六否决零命中，提交 7e32938 test→66bf809 feat→bbbb147 docs）**** / 036c2 冗余覆盖与死代码清理+model-008/010（预留，>6 文件问案B） / 036c3 串联 :deep 42处（068 登记，预留，>6 文件问案B） | web/001, web/003, web/008, web/016 | 🔄 | sprint-067 评审通过（2026-09-29，**子功能点 web/036a ✅**，均分 9.3/10，4 条 AC 全满足，2 failed RED→27/27 GREEN，冒烟 web-036a 通过；036b **✅ sprint-084 评审通过（2026-10-09，均分 9.5/10，4 AC 全满足，六否决零命中，提交 `2fdc450 test`→`742341d feat`→`7c7ad83 docs`）**：R1 全局下沉（删 BaseTable 死选择器遮罩块、reset.css 遮罩对 0.9）+ R2 settings 双处 ElButton→BaseButton、反模式 3 处清零、**案A 校准 AC3③ 计数≥10 且死形态=0**（基线 11 含死形态 1 条，用户裁决）、冒烟 65=56✅+7❌+2⏭️ 含 web-036b ✅、连带 web-011b 计数断言语义同步；036c 待后续 Sprint；与 web/017 同源可一并对照） |
| web/037 | 面包屑导航间距优化（与顶栏/内容卡片留白）+ 面包屑图标改横排（图标置于文字左侧；根因：`.el-breadcrumb__inner` 块化后非 flex 容器叠加全局 `svg{display:block}` 致图标独占一行；`.layout__content` 顶距 0、`.breadcrumb` 底距 16px 致与 head/main 紧凑） | web/003, web/025 | ✅ | sprint-068 评审通过（2026-09-29，平均分 9.5/10，4 条 AC 全满足，测试先行 3 failed RED→3/3 GREEN，冒烟 web-037 通过、50 用例 9 失败为基线子集零新增；单 `:deep()` 规避串联失效预存缺陷） |
| web/038 | 模块服务统一入口与内嵌访问（设计文档 docs/superpowers/specs/2026-09-29-module-iframe-access-design.md，用户逐节确认）。**拆分**：web/038a 后端内嵌支撑——JWT Cookie 双承载（登录下发/登出清除/过滤器读取）+ 网关内嵌响应头处理（sprint-070 本次，✅）/ web/038b GET /modules/accessibles 可访问模块列表（sprint-071 评审通过，✅）/ web/038c baseUrl/createTime 字段错位修复·硬前置（sprint-072 评审通过，✅）/ web/038d 「我的模块」菜单页（**✅ 9.0/10** REWORK-1 复审通过 2026-09-29：/mymodules DefaultLayout 布局链+失败态闭环，评审记录见 task.md；前置 038b+038c 已齐）/ web/038e iframe 内嵌视图+管理页「进入」点击闭环（sprint-074 评审通过 2026-09-29，✅ 9.3/10，前置 038a/038c/038d 已齐；**038 组五段全部收口**）。外部服务相对路径资源部署为使用前提；modules/003 服务凭证与本组无关另议 | auth/002, auth/003, auth/006, modules/002, web/002, web/003, web/006 | ✅ | sprint-074 评审通过（2026-09-29，**子功能点 web/038e ✅ 9.3/10**，4 条 AC 全满足，4 用例 test-first RED `6c7d0af`→GREEN `0b8418b`，门禁 mvn 148 4F+2E 零新增 + test 20 failed\|288 passed + 冒烟 56=45✅9❌2⏭️ 且 web-038e ✅、038d 回归 5/5，六否决项零命中；**038 组五段全部收口 038a-e ✅**）；sprint-074 规划（2026-09-29，子功能点 web/038e：iframe 视图（src=/api/v1/gateway/{code}/ 相对路径+加载态）+/workspace/module/:code 布局链路由（镜像 /mymodules 教训）+管理页「进入」按钮（baseUrl&&status=1 条件，历史债文件最小增量用户裁定）；038d 路由闭环 AC3）；sprint-073 规划（2026-09-29，**子功能点 web/038d**（状态由上条 sprint-073 评审通过条目承载），「我的模块」菜单页：/mymodules 路由+全员可见菜单+getAccessibles+卡片/空态/导航四用例，6 文件顶格纯前端，AC3 点卡目标 /workspace/module/:code 由 038e 注册断言导航意图级）；sprint-072 评审通过（2026-09-29，均分 9.0/10，4 条 AC 全满足，3 failed RED→21/21 GREEN，门禁 148 用例 4F+2E 零新增，冒烟 54 用例 43✅+9❌基线+2⏭️且 web-038c 通过，方案 B 与 +2 Javadoc 两项用户裁定在案；038d/e 硬前置解除）；sprint-072 规划（2026-09-29，模块域 JSON 契约收敛 camel：DTO 入参 baseUrl、VO 出参 createTime，前端零改动；同源 system 缺陷另立 web/039 占位）；sprint-071 评审通过（2026-09-29，**子功能点 web/038b ✅**，均分 9.0/10，4 条 AC 全满足，3 failed RED→11/11 GREEN，冒烟 53 用例 web-038b 通过、9 失败为基线子集零新增，测试类 499 行守 500 红线；基线对照豁免否决项 6）；sprint-070 评审通过（2026-09-29，**子功能点 web/038a ✅**，均分 9.0/10，4 条 AC 全满足，4 failed RED→25/25 GREEN，冒烟 52 用例 web-038a 通过、9 失败为基线子集零新增；AC4 两阶段口径修订与 SecurityConfig +1 行用户裁定见 task.md，Cookie/XFO 两处口径登记见 session-state 挂起区；基线对照豁免否决项 6；038b~e 待后续 Sprint，038c 为 038d/038e 硬前置） |
| web/039 | 创建时间字段全站对齐（user/role/permission 域）——后端 `UserVO/RoleVO/PermissionVO` 输出 `createdAt` 与前端全站类型（`types/user.ts`、`types/role.ts`、`types/permission.ts`）及列表列读 `createTime` 系统性错位，三张列表页创建时间恒显 `—`（2026-09-29 web/038c 规划调研发现；模块域由 web/038c 先行修复，本行为同源遗留；**方案已定（sprint-075 Planner 定案）**：038c 方案 B 同构延伸——三 VO 加 `@JsonProperty("createTime")` 后端对齐前端读法、前端零改动，5~6 文件**不拆分**，与 038c 用户裁定先例一致） | web/004, web/005, web/006, web/038c | ✅ | sprint-075 评审通过（2026-09-29，均分 9.4/10，4 条 AC 全满足，2 failed RED→2/2 GREEN，mvn 150 用例 4F+2E=基线失败集、前端零改动、冒烟 57 web/039 ✅，6 文件顶格；**同族新发现 updateTime 错位建议立 web/040**；2026-09-29 Planner 注册占位） |
| web/040 | 更新时间字段全站对齐（updateTime 族）——四 VO 输出 `updatedAt` 与前端读 `updateTime` 错位（2026-09-29 web/039 评审 Evaluator 改进建议②新发现：`profile/IndexView.vue:92`、`types/auth.ts:10`、`types/user.ts:12` 读后端不存在的键，个人中心更新时间恒空；当前 JSON 混杂 `{createTime, updatedAt}`）。**用户裁决（2026-09-29 问答在案）**：四 VO 全站对称统一输出 `{createTime, updateTime}`、方案 B 同构（后端 `@JsonProperty("updateTime")` 前端零改动）。**拆分**：web/040a 三域 User/Role/Permission updateTime（sprint-076 本次，6 文件顶格，含 AC2 me 面收窄）/ web/040c **me 面**——`dto.auth.UserVO`（6 参无时间字段，与 dto.user.UserVO 同名异类，075 评审误判登记更正）+ `AuthServiceImpl.toUserVO` 补时间，修复个人中心创建/更新时间恒空（2-3 文件，用户 2026-09-29 裁决拆出）/ **web/040b1 ✅ sprint-078 评审通过 9.7/10**（2026-09-29，模块域 ModuleVO updateTime 收尾=四 VO 对称收口，RED 复跑实证+mvn 154=基线+1+冒烟 60=49✅+9❌+2⏭️ 逐条=基线+前端零改动；yml expire-hours 漂移插曲=非本单回归，用户裁决恢复）/ **web/040b2 ✅ sprint-079 评审通过 9.8/10**（2026-09-29，R1 案A 文档勘误收口：五处 `{list,total,page,size}`→实测五字段 `{records,total,size,current,pages}`，RED 5命中复跑+残留零命中+mvn154/冒烟61/前端三线逐条基线；**web/040 组全收口**（040a 9.4/040c 9.5/040b1 9.7/040b2 9.8），R1 问题单关闭）。前置 web/040b1 ✅ 9.7/10 | web/039, web/038c | ✅ | **web/040a sprint-076 评审通过（2026-09-29，均分 9.4/10，AC 4/4（AC2 经用户裁决收窄）、6 文件顶格）**；**web/040c sprint-077 评审通过（2026-09-29，均分 9.5/10，AC 3/3、5+16+3+11 定向全绿、mvn 153 4F+2E=基线+1、前端零改动、冒烟 59 web/040c ✅，4 文件，个人中心两时间恒空修复落地）**；040b 待后续（ModuleVO+R1 案A 文档勘误）；updateTime/案A 两项用户裁决在案 |
| web/041 | 修复用户/角色/权限/模块四管理页工具栏筛选下拉框点击无法展开——根因：`BaseSelect` click-outside 同事件竞态（根 div `handleClick` 置 isOpen=true 后 Vue flushJobs 在 click 传播到 document 前完成渲染，`handleClickOutside` 以 `dropdownRef.contains(target=trigger)` 误判 outside → 开→关；Chromium 四采样点实录 A/B/C 开、D 关；抽屉因冒泡未达 document 侥幸可用；四页 spec 均 mock 组件故测试盲区；jsdom 完整传播不复现，AC 采用渲染后重放 outside 监听器同构形态） | web/001, web/004, web/005, web/006, web/011 | ✅ | **sprint-086 评审通过（2026-10-10，均分 9.7/10，4 AC 亲核全满足，RED 独立复现 checkout 4bd759f → 1 failed，门禁五线亲跑=基线（mvn 154 4F+2E 五类逐条、lint 0E\|165W、test 317/317、build ✓17.44s），冒烟 67=61✅+4❌+2⏭️ ❌恰=登记四条零新增、web-041 ✅，R3 四页浏览器 hitRatio 36/36+六路径定向复测 PASS，六否决零命中，变更 7 文件=清单零夹带，提交 `4bd759f test`→`2870511 feat`→`0fdb02c docs`）**；Planner 2026-10-10：4 条验收标准，用户报障直提；≤6 文件不拆分；修法=R2 outside 判定改根元素包含性 rootRef.contains + 删死引用 dropdownRef；报障面口径=四页 toolbar 下拉整体含权限页模块筛选，见 task R4 |


| web/042 | 修复权限管理页模块筛选——初始锁死 `moduleId=0` 空列表（permissionQuery 初值 0 → 后端 eq(module_id,0) 恒空，打开即空表）+ moduleOptions 缺「全部模块」选项无法清除筛选（`Number(v)||0` 把空串转 0 回锁死）。**范围锁定**：仅权限页；用户/角色/模块三页状态筛选实测正常（「选停用按钮变启用」=行级可执行动作的 RBAC 正确语义），不改码，证据见 task 业务背景表 | web/006, web/030, web/041, web/011 | ✅ | sprint-087 规划（2026-10-10 Planner，用户报障「筛选没达到功能」直提+调查实锤双缺陷，4 条验收标准，≤6 文件不拆分；修法=初值 undefined/空值不过滤 + moduleOptions 增「全部模块」（R1 序列化口径二选一）、红线后端不动；AC1/AC2 spec 必红、AC3 护栏含抽屉联动 503 行、AC4 冒烟 web-042）；**sprint-087 评审通过（2026-10-10，均分 9.7/10，4 AC 亲核全满足，RED 独立复现 checkout b7833d6 → 2 failed，门禁五线亲跑=基线（mvn 154 4F+2E 五类逐条、lint 0E|165W、test 320/320、build ✓26.75s）、冒烟 68 ❌恰=登记四条零新增+web-042 ✅+增量 +1 取证，R3 浏览器四动作=初始无 moduleId total16/选模块 id 过滤/全部模块清回16/路由联动 id3，六否决零命中，提交 `b7833d6 test`→`ecd387e feat`→`4a78d63 docs`）**；Planner 2026-10-10：4 条验收标准，用户报障调查后直提；≤6 文件不拆分、后端不动；AC2 口径=筛选专用 permissionFilterOptions 不污染表单必选语义 |
| web/043 | 修复模块管理页状态切换按钮永不可见——根因：前端门禁 `canToggleModuleStatus`（system/IndexView.vue:245）检查 `module:toggle-status`，但后端 V6 种子 16 权限无此码（`grep backend/` 零命中）、ROLE_ADMIN 全量授权亦不含 → admin `hasPermission` 恒 false → 按钮 `.module-table__action-status` 永不渲染（headless 实测启用 4 行 0 在场；前端 spec 280/356/1494 mock 注入故单测盲区）。**修法**：V7 迁移补权限（挂 module_mgmt，§6.1）+ ROLE_ADMIN 授权（INSERT IGNORE 幂等，禁改 V6 checksum）；后端 Java 与前端业务代码零改动 | web/006, model/006, auth/004, modules/001, web/011 | ✅ | sprint-088 规划（2026-10-10 Planner，用户状态筛选报障核验派生：用户/角色页实测=用户确认期望收口，模块页发现按钮永不可见实锤，用户裁定「开单修复（补权限种子）」；4 条验收标准，≤6 文件不拆分；TDD=种子/me 断言先于 V7 天然 RED、三处 16→17 过期口径（SeedData×2/AuthControllerTest）独立方法防 22≠2 基线掩盖、冒烟 web-043 五形态；AC4 R3=按钮在场+点停用翻转+筛停用按钮显启用（用户确认口径），研究项 R1-R7 见 task）；**sprint-088 评审通过（2026-10-10，均分 9.9/10，4 AC 亲核全满足，RED 独立复现 worktree@62ca041+空库 → 5 failed，门禁五线亲跑=基线（mvn 156 4F+2E 五类逐条、lint 0E|165W、test 320/320、build ✓17.78s）、冒烟 69=63✅+4❌恰基线+2⏭️ 零新增且 web-043 ✅+增量 +1 取证，R3 探针 13/13（按钮在场 4=修复前 0、停用翻转、筛停用显启用、还原零残留），六否决零命中，提交 `62ca041 test`→`4e6b419 feat`→`a9fc8f4 docs`）** |
| web/044 | 修复用户/角色/权限/模块四管理页搜索关键词不命中——后端四域列表查询把多关键词字段各自 `like` **AND 叠加**（`UserServiceImpl` 四字段、`Role/Permission/ModuleServiceImpl` name∧code），前端 `handleSearch` 同值复制（意图 OR）→ 关键词须同时出现在所有字段，交集恒空（9998 只读探针实锤：`?username=admin&nickname=admin&email=admin&phone=admin`→0、`?name=ROLE_ADMIN&code=ROLE_ADMIN`→0、`name=用户&code=用户`→0、`name=user_mgmt&code=user_mgmt`→0，单字段均命中）。**修法**=四 ServiceImpl 非空关键词字段进同一 OR 组（status/moduleId 保持外层 AND），新增单类四方法契约测试 + 冒烟 web-044；前端/Controller/DTO/迁移零改动；范围外登记=BaseInput clear 不触发重查（候选后续单） | users/001, roles/001, perms/001, modules/001, web/004, web/005, web/006, web/041, web/042, web/043 | ✅ | **sprint-089 评审通过（2026-10-12，均分 9.8/10，4 AC 亲核全满足，RED 独立复现 worktree@fa9aaf2+测试文件 → 4 failed（total=0×4），门禁五线亲跑=基线（mvn 160 4F+2E 五类逐条、lint 0E\|165W、test 320/320、build ✓17.79s）、冒烟亲跑 70=64✅+4❌恰基线+2⏭️ 零新增且 web/044 ✅、变更 6 文件=清单零夹带（两段式 `8fe16f5 test`→`e142290 feat`）、六否决零命中）**；Planner 2026-10-12 规划：4 条验收标准，用户报障「筛选框不准确」直提+四页排查实锤 AND 根因（9998 探针四域同值查询全 0），6 文件顶格不拆分；范围外登记=BaseInput clear 不触发重查（候选 web/045） |
| web/046 | 用户-角色绑定·数据回显与提交契约暗修（用户报障「三类绑定无操作入口」第一单）——① `UserVO.java` 8 参**无 roles 字段**：列表角色列 formatter（users/IndexView.vue:64-72 读 `row.roles`）恒 `—`、抽屉回显无数据源（前端 types/user.ts:14 已声明 `roles?: RoleVO[]`，契约已备只等后端）；② 前端 `api/user.ts:42` `assignRoles` 发 **POST** vs 后端 `UserController.java:167` **PUT** `/users/{id}/roles` → 405。**修法**=UserVO 增 roles（复用 RoleVO）+ ServiceImpl toVO 填充（列表防 N+1）+ api 一行 POST→PUT + 双端契约测试先 RED；**暗修不放行**（V8 补 `user:assign-role` 种子/计数联动/RoleAssignDrawer 回显归 web/047，按钮门禁继续拦截零恶化）。**拆分总盘**（三绑定 ≥25 文件强制拆 6 单，session-state 挂起区 2026-10-12 登记）：046 本次 ✅ / 047 用户-角色放行+抽屉回显（**✅ sprint-047 评审通过 8.6/10**）/ 048 角色-权限数据源（tree 接口+RoleVO.permissions）/ 049 角色-权限提交契约（**✅ sprint-095 评审通过 9.1/10**）/ 049 角色-权限 PUT+抽屉消费 / 050 role:assign-permission 门禁与授权收口 / 051 权限-模块 moduleId 契约（待实测）；web/045 保留 BaseInput 候选 | users/001, users/002, web/004, web/041, web/044 | ✅ | **sprint-090 评审通过（2026-10-12，均分 9.6/10，4 AC 亲核全满足，RED 独立复现 worktree@f0f128b+application.yml → 后端 2 failed（$.data.roles 缺失×2）+前端 put calls=0+冒烟 G1 拒绝，门禁五线亲跑=基线（mvn 162 4F+2E 五类逐条、lint 0E\|165W、test 321/321、build ✓17.67s）、冒烟亲跑 71=65✅+4❌恰基线+2⏭️ 零新增且 web/046 ✅、变更 10 文件=清单零夹带（两段式 `f0f128b test`→`16893bd feat`）、六否决零命中）**；Planner 2026-10-12 规划：4 条验收标准，用户报障三绑定无操作入口直提+全仓只读探查实锤三域根因分层与 git 考古零删除（入口从未有后端配套）；不违 §3（VO 属 API 层零表变更，038c/039/040 先例）、§4 方法语义未定以既有后端 PUT 为准对齐 |
| web/047 | 用户-角色绑定·放行与回显闭环（三绑定拆分总盘后续①；用户报障「操作没有可以操作的地方」剩余两断点）——① `users/IndexView.vue:123` 门禁 `user:assign-role` 在 V6/V7 种子 17 码零命中→按钮永不可见（与 module:toggle-status 缺码同型 043 已修先例）；② `RoleAssignDrawer.vue:141` watch 打开 `assignedRoleIds=[]` 硬编码无按 userId 拉取→配合 PUT 全量替换提交会清空已有角色；③ 消费源已就位（046 `UserVO.roles`+`api/user.ts:17` getById）。**修法**=V8 迁移补码挂 user_mgmt+ROLE_ADMIN 幂等授权（镜像 V7）+SeedData/AuthController 计数 17→18+抽屉 fetchAssignedRoles 回显+spec+smoke=6 顶格；**放行非暗修**（按钮门禁逻辑已有，码到位即放行，用户-角色绑定端到端可完成） | users/001, users/002, web/046, web/044, perms/001 | ✅ | **sprint-094 评审通过（2026-10-13 Evaluator，均分 8.6/10：4 AC 亲核全满足——V8 镜像 V7 INSERT IGNORE+SeedData 收敛至仅 22≠2 预存红+me contains 新码+抽屉 getById 回显 spec 绿；门禁亲测 mvn 168 4F+2E 五类逐条/lint 0E|166W/test 40文件322/build ✓17.15s/冒烟 74=68✅+4❌恰基线+2⏭ 含 web-047 ✅，六否决零命中，提交 `5a615c8 test`→`e23191b feat`；用户报障用户-角色绑定链路端到端三闭环收口；失分=spec 仅 happy path+两请求串行；readOnly 连续三单挂起 049 必列）**；sprint-094 规划（2026-10-13 Planner，4 AC=V8 迁移与计数 18/me 放行面 contains 新码/抽屉回显 spec/冒烟 74；后端主代码零改动纯迁移+测试+前端；SeedData 方法整体 22≠2 基线红豁免（GREEN 判定=fail 收敛至仅该条）；readOnly 补标归 049 登记挂起区；不违 §3/§4 计数演进 V7 先例/§6.2 绑定可操作性强化） |
| web/049 | 角色-权限绑定·提交契约与 readOnly 收口（三绑定拆分总盘后续②；用户报障「角色 权限分配接口报错」剩余断点）——① `api/role.ts:32` assignPermissions 发 POST vs 后端 PUT → 405（046 同型先例）；② 消费侧已就位零界面改动；③ readOnly 补标 7 处（RoleServiceImpl 3+PermissionServiceImpl 4，连续三单挂起升格强制列入）。**修法**=api 一行 POST→PUT+spec RED+7 处方法级 readOnly（镜像 ModuleServiceImpl:212）+smoke web-049 =5 文件 | web/048a, web/048b, roles/001, web/044, web/047 | ✅ | **sprint-095 评审通过（2026-10-14 Evaluator，均分 9.1/10：4 AC 亲核全满足——spec put 断言绿+diff 亲核恰 7 处 readOnly 零外溢+G3 后端契约绿+冒烟 75=69✅+4❌恰基线+2⏭ 含 web-049 ✅；门禁亲测 mvn 168 4F+2E 五类/lint 0E|166W/test 41文件323/build ✓17.68s，六否决零命中，提交 `98b5ddc test`→`b307b94 feat`；用户报障角色-权限分配三断点全部闭合；readOnly 清偿+防再犯=后续新查询方法须标注）**；sprint-095 规划（2026-10-13 Planner，4 AC；零界面改动纯契约对齐；不违 §3/§6） |
| web/048 | 角色-权限绑定·数据源暗修（用户报障「角色 权限分配接口报错」直提，9998 实证 `GET /permissions/tree` 落 `GET /{id}` → `MethodArgumentTypeMismatchException("tree")` 500）——① `PermissionController` **无 /tree 映射**（仅 /、/grouped、/{id}）；② `/grouped` 返回 `Map<ModuleVO.toString, List>` 对象键串**不可被前端消费**（`PermissionTreeNode extends PermissionVO { children }` 期望嵌套数组）；③ `RoleVO` 无 `permissions` 字段（抽屉 :102-115 读 `role.permissions` 恒 undefined）；④ 消费侧已就位（`api/permission.ts:15-16` getTree 已声明、`PermissionAssignDrawer.vue:73` 已消费——与 046 同构契约已备只等后端）。**超限熔断重切（sprint-091 Generator 熔断 8>6，零业务写入）**：**048a（本次 sprint-092）**=tree 数据源（Controller+PermissionService 接口+Impl+PermissionVO(+children)+契约类+smoke=6 顶格，R1 平铺方案定夺：模块父节点会致 sys_module.id/sys_permission.id 值域重叠 key 冲突+check-change 父 id 污染 049 提交载荷）/ **048b ✅ sprint-093 评审通过（2026-10-13，均分 8.5/10，4 AC 亲核全满足，RED 2F=No value at JSON path→GREEN 3/3，mvn 168 4F+2E 逐条基线+冒烟 73=67✅+4❌+2⏭️ 含 web-048b ✅，六否决零命中，提交 `3b0a75c test`→`34d3d35 feat`→`01bc19c docs`；批量两查防 N+1 吸取 048a 教训、AC3 自建临时角色免测试库污染；readOnly 重复性未修入改进建议）**；**暗修不放行**（api/role.ts POST→PUT 归 049、role:assign-permission 门禁归 050）。web/047 顺延不变 | perms/001, web/005, web/046, sprint-090, sprint-091 | ✅ | **sprint-092 评审通过（2026-10-13 Evaluator，**子功能点 web/048a ✅ 均分 8.4/10**：4 AC 亲核全满足，RED 2F=500→GREEN 3/3，五线亲测=基线 mvn 165 4F+2E 五类逐条/lint 0E|165W/test 321/build ✓/冒烟 72=66✅+4❌(model-008/010、roles-001/002 基线)+2⏭️ 含 web-048a ✅，六否决零命中，提交 `fdaef61 test`→`f25e532 feat`→`dd588e1 docs`；失分入改进建议=N+1 新增路径+readOnly 未标+/tree 缺未认证用例；048b 角色回显待后续 Sprint）**；sprint-092 实现（2026-10-13 Generator：GET /permissions/tree 平铺上线（PermissionVO children 自引用+8 参重载零连锁+listAllForTree 分层委托），RED 2F=500 复现报障→GREEN 3/3，门禁五线 mvn 165=162+3 4F+2E 逐条一致/lint 0E|165W/test 321/build ✓/冒烟 72=66✅+4❌(model-008/010、roles-001/002 基线吻合)+2⏭️；Spec 计数 6→7 过期同步；提交 fdaef61/f25e532，待 Evaluator）**；sprint-091 规划与熔断登记（2026-10-12：用户报障直提+取证 tree 500/grouped 不可消费/前端消费侧已就位，预估 6 漏算 Service 层两文件与 children 载体实际 8） |
## integration — 集成与验收

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| integration/001 | 第三方模块接入指南（鉴权流程/check API 契约/错误码表） | auth/005 | ✅ | sprint-030 评审通过（均分 10.0） |
| integration/002 | e2e 冒烟脚本（shell+curl：登录→授权→check 全链路验证） | integration/001 | ✅ | sprint-031 评审通过（均分 10.0） |
