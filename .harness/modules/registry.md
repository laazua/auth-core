## 功能点注册表（Registry）

本表是全系统的唯一功能点台账：每个功能点拥有全局唯一 ID（如 `infra/001`），被三个 prompt 与 context 文件引用。状态图例：⬜ 未开始 / 🔄 进行中 / ✅ 完成 / ❌ 阻塞 / ⚠️ 有缺口

> 最后更新：2026-09-29（sprint-073 规划，注册子功能点 web/038d「我的模块」菜单页（🔄，6 文件顶格 4 AC，纯前端）；前序 sprint-072 web/038c ✅、sprint-071 web/038b ✅、sprint-070 web/038a ✅；038e 与 modules/003、web/039 预留）

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
| web | 37 | 31 | 5 | 0 | web/011/017/023/036/038 进行中；web/022 废弃⚠️；其余 31 项 ✅（2026-09-29 sprint-070 注册 web/038） |
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
| web/011 | 修复前端构建错误与失败测试 | web/010 | 🔄 | sprint-056 规划中（2026-09-04 kickoff，web/011a 修复 TS 编译错误为首子功能点） |
| web/012 | 前端区域背景色差异化优化（登录页/侧边栏/顶栏/主内容区视觉层级区分） | web/008 | ✅ | sprint-036 评审中 |
| web/013 | 修复登录页密码输入框默认明文显示问题 | web/002 | ✅ | sprint-037 完成 |
| web/014 | 修复用户下拉菜单功能（个人中心/设置/修改密码/退出登录点击无效） | web/003, web/007 | ✅ | sprint-038 完成 |
| web/015 | 修复 DefaultLayout.vue 缺失渐变背景导致页面纯白问题 | web/001, web/008 | ✅ | sprint-040 随 web/016 一并评审通过（均分 9.6/10） |
| web/016 | 修复 CSS 主题变量源 variables.scss 未注入样式入口导致全站 UI 纯白 | web/001, web/008 | ✅ | sprint-040 评审通过（均分 9.6/10，浏览器实证渐变恢复） |
| web/017 | 修复顶栏用户下拉菜单 UI 突兀与尺寸异常（图标拉伸成 68~96px 巨图致行高 78/106px 不一、弹层 EP 默认白底 4px 圆角未主题化、暗色弹层白底近白字不可读） | web/014, web/008, web/016 | 🔄 | sprint-041 规划中（2026-09-02 kickoff，4 条验收标准，浏览器实测基线见 task.md） |
| web/018 | 修复侧边栏菜单图标不显示（图标字符串未解析为组件） | web/001, web/003, web/008 | ✅ | sprint-042 评审通过（2026-09-02，4 条验收标准，main.ts 全局注册 Element Plus 图标） |
| web/019 | 修复侧边栏菜单图标尺寸过大（SVG 图标缺少显式 width/height，font-size 不生效） | web/001, web/003, web/008, web/018 | ✅ | sprint-043 评审通过（2026-09-02，4 条验收标准，Sidebar.vue 三处 .sidebar__icon 显式定宽 16×16px） |
| web/020 | 移除侧边栏底部折叠按钮，仅保留顶栏左侧折叠按钮 | web/003, web/019 | ✅ | sprint-044 评审通过（2026-09-03，4 条验收标准） |
| web/021 | 移除标签页操作区域（关闭其他标签、关闭所有标签），仅保留刷新按钮 | web/003, web/020 | ✅ | sprint-045 评审通过（2026-09-03，4 条验收标准） |
| web/022 | 移除标签页操作区域整个红框部分（含刷新按钮），仅保留标签页栏 | web/003, web/021 | ⚠️ | sprint-046 实现有误（需求理解偏差，已废弃） |
| web/023 | 移除整个标签页栏（TagsView），仅保留面包屑导航 | web/003, web/020 | 🔄 | sprint-047 规划中（2026-09-03 kickoff，4 条验收标准） |
| web/024 | 修复主内容区图标尺寸异常（ElButton icon prop 等 EP 图标缺少显式尺寸被 flex 拉伸） | web/003, web/007, web/008, web/016 | ✅ | sprint-048 评审中（2026-09-03 实现完成，3 条验收标准，ProfileView.vue BaseButton+slot 16×16px + header-icon 20×20px，Breadcrumb.vue breadcrumb__icon 13×13px） |
| web/025 | 优化主内容区图标与文字间距（ProfileView header、action buttons、Breadcrumb 等） | web/003, web/007, web/008, web/016, web/024 | ✅ | sprint-049 评审通过（2026-09-03，4 条验收标准，平均分 9.8/10，header gap 24px、action gap 16px、breadcrumb margin-right 12px） |
| web/026 | 修复登录成功后不跳转首页（API 基础路径缺少 /v1 导致 /auth/me 404） | web/002, web/011a | ✅ | sprint-057 评审通过（2026-09-07，4 条验收标准，门禁+冒烟通过） |
| web/027 | 修复远程开发环境 CORS 与代理配置导致的 403 错误 | web/011a, web/026 | ✅ | sprint-059 评审通过（2026-09-08，3 条验收标准，门禁+冒烟通过） |
| web/029 | 修复侧边栏菜单导航404问题（动态路由未加载与菜单路径错误） | web/010, web/026, web/027 | ✅ | sprint-060 评审通过（2026-09-09，平均分 8.6/10，门禁+冒烟通过，router.spec.ts 9 passed, useMenu.spec.ts 10 passed, system-menu-path-fix.spec.ts 6 passed） |
| web/030 | 修复权限管理页模块下拉选项接口 404（/modules/permissions/all 后端接口缺失） | web/006, modules/001, perms/001 | ✅ | sprint-061 实现完成（2026-09-09，4 条验收标准，测试先行 4/4 通过，后端核心测试全绿，冒烟 4 用例通过） |
| web/031 | 修复用户管理新增/编辑抽屉确认按钮不可点击问题（:loading 绑定抽屉可见性而非提交状态） | web/004, web/011 | ✅ | sprint-062 实现完成（2026-09-10，3 条验收标准，单文件修复，测试先行 4/4 通过，前端构建通过） |
| web/032 | 修复 SPA 路由刷新页面 404 问题（History 模式回退配置） | web/001 | ✅ | sprint-063 实现完成（2026-09-10，3 条验收标准，vite.config.ts historyApiFallback + nginx.conf try_files，前端构建通过，核心路由测试全绿） |
| web/033 | 登录页新增显示密码功能（密码框可见性切换按钮，可切换明文/掩码显示） | web/002, web/013 | ✅ | sprint-064 评审通过（2026-09-28，平均分 8.9/10，4 条验收标准全满足，测试先行 3/3，冒烟 web-033 通过；预存红项基线对照） |
| web/034 | 系统管理菜单用户/角色路由迁移至 /system/users 与 /system/roles（修复访问 404，统一 URL 体系） | web/003, web/004, web/005, web/010, web/029 | ✅ | sprint-065 评审通过（2026-09-28，平均分 8.8/10，4 条 AC 全满足，测试先行 9 RED→28/28 GREEN，冒烟 web-034 通过；基线对照豁免否决项 6） |
| web/035 | 修复登录后首次导航受保护路由落 404（动态路由首载 next({ ...to }) 携带 NotFound name 陷阱） | web/001, web/029, web/034 | ✅ | sprint-066 评审通过（2026-09-28，平均分 9.1/10，4 条 AC 全满足，测试先行 2 failed RED→14/14 GREEN，冒烟 web-035 通过；基线对照豁免否决项 6） |
| web/036 | 暗色模式全站 UI 协调治理（根因：自研 `--color-*` data-theme 与 Element Plus `--el-*` 两套变量体系只切一套，EP 暗色 css-vars 未接入、html.dark 未挂）。**拆分**：web/036a 接入 EP 暗色变量体系 ✅ / web/036b 局部白底缺陷修复（BaseTable 遮罩失效选择器、settings 非法 variant prop、组件级覆盖补漏、暗色系协调映射）/ web/036c 打磨与口径治理（冗余覆盖与死代码清理、smoke 过期 grep 同步） | web/001, web/003, web/008, web/016 | 🔄 | sprint-067 评审通过（2026-09-29，**子功能点 web/036a ✅**，均分 9.3/10，4 条 AC 全满足，2 failed RED→27/27 GREEN，冒烟 web-036a 通过；036b/036c 待后续 Sprint；与 web/017 同源可一并对照） |
| web/037 | 面包屑导航间距优化（与顶栏/内容卡片留白）+ 面包屑图标改横排（图标置于文字左侧；根因：`.el-breadcrumb__inner` 块化后非 flex 容器叠加全局 `svg{display:block}` 致图标独占一行；`.layout__content` 顶距 0、`.breadcrumb` 底距 16px 致与 head/main 紧凑） | web/003, web/025 | ✅ | sprint-068 评审通过（2026-09-29，平均分 9.5/10，4 条 AC 全满足，测试先行 3 failed RED→3/3 GREEN，冒烟 web-037 通过、50 用例 9 失败为基线子集零新增；单 `:deep()` 规避串联失效预存缺陷） |
| web/038 | 模块服务统一入口与内嵌访问（设计文档 docs/superpowers/specs/2026-09-29-module-iframe-access-design.md，用户逐节确认）。**拆分**：web/038a 后端内嵌支撑——JWT Cookie 双承载（登录下发/登出清除/过滤器读取）+ 网关内嵌响应头处理（sprint-070 本次，✅）/ web/038b GET /modules/accessibles 可访问模块列表（sprint-071 评审通过，✅）/ web/038c baseUrl/createTime 字段错位修复·硬前置（sprint-072 评审通过，✅）/ web/038d 「我的模块」菜单页（sprint-073 REWORK-1 完成待复审：问题 1-3 全闭合（DefaultLayout 包裹+失败态+行数更正），评审记录见 task.md；前置 038b+038c 已齐）/ web/038e iframe 内嵌视图+管理页「进入」点击闭环（预留 ⬜，前置 038a/038d）。外部服务相对路径资源部署为使用前提；modules/003 服务凭证与本组无关另议 | auth/002, auth/003, auth/006, modules/002, web/002, web/003, web/006 | 🔄 | sprint-073 规划（2026-09-29，**子功能点 web/038d 🔄**，「我的模块」菜单页：/mymodules 路由+全员可见菜单+getAccessibles+卡片/空态/导航四用例，6 文件顶格纯前端，AC3 点卡目标 /workspace/module/:code 由 038e 注册断言导航意图级）；sprint-072 评审通过（2026-09-29，均分 9.0/10，4 条 AC 全满足，3 failed RED→21/21 GREEN，门禁 148 用例 4F+2E 零新增，冒烟 54 用例 43✅+9❌基线+2⏭️且 web-038c 通过，方案 B 与 +2 Javadoc 两项用户裁定在案；038d/e 硬前置解除）；sprint-072 规划（2026-09-29，模块域 JSON 契约收敛 camel：DTO 入参 baseUrl、VO 出参 createTime，前端零改动；同源 system 缺陷另立 web/039 占位）；sprint-071 评审通过（2026-09-29，**子功能点 web/038b ✅**，均分 9.0/10，4 条 AC 全满足，3 failed RED→11/11 GREEN，冒烟 53 用例 web-038b 通过、9 失败为基线子集零新增，测试类 499 行守 500 红线；基线对照豁免否决项 6）；sprint-070 评审通过（2026-09-29，**子功能点 web/038a ✅**，均分 9.0/10，4 条 AC 全满足，4 failed RED→25/25 GREEN，冒烟 52 用例 web-038a 通过、9 失败为基线子集零新增；AC4 两阶段口径修订与 SecurityConfig +1 行用户裁定见 task.md，Cookie/XFO 两处口径登记见 session-state 挂起区；基线对照豁免否决项 6；038b~e 待后续 Sprint，038c 为 038d/038e 硬前置） |
| web/039 | 创建时间字段全站对齐（user/role/permission 域）——后端 `UserVO/RoleVO/PermissionVO` 输出 `createdAt` 与前端全站类型（`types/user.ts`、`types/role.ts`、`types/permission.ts`）及列表列读 `createTime` 系统性错位，三张列表页创建时间恒显 `—`（2026-09-29 web/038c 规划调研发现；模块域由 web/038c 先行修复，本行为同源遗留；方案待定：后端改输出或前端改读取，届时 Planner 拆分） | web/004, web/005, web/006, web/038c | ⬜ | 2026-09-29 Planner 注册占位（防遗漏登记；非 web/038 组内拆分，独立功能点） |


## integration — 集成与验收

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| integration/001 | 第三方模块接入指南（鉴权流程/check API 契约/错误码表） | auth/005 | ✅ | sprint-030 评审通过（均分 10.0） |
| integration/002 | e2e 冒烟脚本（shell+curl：登录→授权→check 全链路验证） | integration/001 | ✅ | sprint-031 评审通过（均分 10.0） |
