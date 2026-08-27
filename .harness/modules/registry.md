## 功能点注册表（Registry）

本表是全系统的唯一功能点台账：每个功能点拥有全局唯一 ID（如 `infra/001`），被三个 prompt 与 context 文件引用。状态图例：⬜ 未开始 / 🔄 进行中 / ✅ 完成 / ❌ 阻塞 / ⚠️ 有缺口

> 最后更新：2026-08-26（系统初始化：32 个功能点全部 ⬜）

## 阶段总览

| 模块 | 功能点数 | 已完成 | 进行中 | 未开始 | 备注 |
|------|----------|--------|--------|--------|------|
| infra | 4 | 0 | 0 | 4 | 全链关键路径起点（骨架→存储→响应体） |
| model | 6 | 0 | 0 | 6 | 承接 infra/002，为全部业务 API 提供数据层 |
| auth | 6 | 0 | 0 | 6 | 安全基线，所有业务 API 的前置依赖 |
| users | 3 | 0 | 0 | 3 | — |
| roles | 2 | 0 | 0 | 2 | — |
| perms | 1 | 0 | 0 | 1 | — |
| modules | 1 | 0 | 0 | 1 | — |
| web | 7 | 0 | 0 | 7 | 进度受后端 API 完成度制约 |
| integration | 2 | 0 | 0 | 2 | 收尾验证，依赖全部前置就绪 |

## infra — 基础设施

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| infra/001 | Maven 项目骨架 + Spring Boot 启动 + actuator 健康检查 | — | ✅ | sprint-001 评审通过（2026-08-26，均分 9.8） |
| infra/002 | MySQL 接入 + MyBatis-Plus 配置 + Flyway 迁移机制 | infra/001 | ✅ | sprint-002 复审通过（2026-08-26，均分 8.8；配置化接入口径见挂起区） |
| infra/003 | 统一响应体 Result<T> + 全局异常处理 + Bean Validation | infra/001 | ✅ | sprint-003 评审通过（2026-08-26，均分 9.8） |
| infra/004 | 测试基础设施（Testcontainers MySQL 基座 + 测试命名/分层约定） | infra/002 | ⬜ | — |

## model — 数据模型

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| model/001 | sys_user 表迁移+实体+Mapper | infra/002 | ✅ | sprint-004 评审通过（2026-08-26，均分 9.8；真实库验收） |
| model/002 | sys_role 表迁移+实体+Mapper | infra/002 | ✅ | sprint-005 评审通过（2026-08-26，均分 9.8） |
| model/003 | sys_module 表迁移+实体+Mapper | infra/002 | ✅ | sprint-006 评审通过（2026-08-26，均分 10.0；集成测试基座就位） |
| model/004 | sys_permission 表迁移+实体+Mapper(FK module_id) | model/003 | ✅ | sprint-007 评审通过（2026-08-26，均分 10.0） |
| model/005 | sys_user_role+sys_role_permission 关联表 | model/001, model/002, model/004 | 🔄 | sprint-008 规划中 |
| model/006 | 种子数据迁移（内置 admin + 示例角色/权限/模块） | model/005 | ⬜ | — |

## auth — 认证授权

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| auth/001 | Spring Security 无状态基线 + BCrypt 编码器 | infra/003, model/001 | ⬜ | — |
| auth/002 | 登录接口 POST /api/v1/auth/login 签发 JWT | auth/001 | ⬜ | — |
| auth/003 | JWT 校验过滤器 + SecurityContext 注入 | auth/001, auth/002 | ⬜ | — |
| auth/004 | GET /api/v1/auth/me（用户+角色+权限集合） | auth/003, model/005 | ⬜ | — |
| auth/005 | 权限校验 API POST /api/v1/auth/check（供外部模块集成调用） | auth/003 | ⬜ | — |
| auth/006 | 登出策略（无状态 JWT v1：接口+失效语义说明） | auth/003 | ⬜ | — |

## users — 用户管理

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| users/001 | 用户 CRUD API（分页/条件查询/创建加密/更新/启停用/删除） | auth/003, model/001 | ⬜ | — |
| users/002 | 用户-角色分配 API（批量设置） | users/001, model/005 | ⬜ | — |
| users/003 | 密码修改+管理员重置 | users/001 | ⬜ | — |

## roles — 角色管理

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| roles/001 | 角色 CRUD API | auth/003, model/002 | ⬜ | — |
| roles/002 | 角色-权限分配 API（批量设置） | roles/001, model/005 | ⬜ | — |

## perms — 权限管理

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| perms/001 | 权限 CRUD API（支持按模块分组查询） | auth/003, model/004 | ⬜ | — |

## modules — 模块管理

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| modules/001 | 模块 CRUD API+模块下权限级联查询+删除引用保护 | auth/003, model/003 | ⬜ | — |

## web — Web 前端

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| web/001 | Vite+Vue3+TS+Pinia+Router+Element Plus 骨架+Axios 封装(token 注入/401 拦截)+Vitest 基线 | auth/002 | ⬜ | — |
| web/002 | 登录页+路由守卫 | web/001, auth/002 | ⬜ | — |
| web/003 | 主布局(侧边菜单/顶栏)+动态菜单渲染 | web/002, auth/004 | ⬜ | — |
| web/004 | 用户管理页(含角色分配/启停用/重置密码) | web/003, users/001, users/002, users/003 | ⬜ | — |
| web/005 | 角色管理页(含权限分配树) | web/003, roles/001, roles/002 | ⬜ | — |
| web/006 | 权限管理页+模块管理页 | web/003, perms/001, modules/001 | ⬜ | — |
| web/007 | 个人中心改密 | web/003, users/003 | ⬜ | — |

## integration — 集成与验收

| ID | 功能点 | 前置依赖 | 状态 | 备注 |
|----|--------|----------|------|------|
| integration/001 | 第三方模块接入指南（鉴权流程/check API 契约/错误码表） | auth/005 | ⬜ | — |
| integration/002 | e2e 冒烟脚本（shell+curl：登录→授权→check 全链路验证） | integration/001 | ⬜ | — |
