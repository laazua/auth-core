# auth-core 系统架构

> 本文档（`docs/01-architecture.md`）是所有 Agent 的仲裁依据：任何实现与本文件冲突时，一律以本文件为准，并同步在 .harness/context/session-state.md 中登记冲突与处理结果，不得静默偏离。

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

上表选型已固化：任何任务不得替换、升级或引入表外技术栈；确需变更时，必须先修订本文档并在会话状态中登记，不得在实现中私自偏离。

## 2. Monorepo 结构

仓库采用前后端同仓的 monorepo 组织方式，仅包含两个顶层目录：

- `backend/` —— Spring Boot 单 Maven 模块，承载全部后端功能；
- `frontend/` —— Vue3 应用，承载全部前端功能。

后端代码按 package-by-layer 方式组织，包结构固定为 `com.authcore.config` / `controller` / `service(.impl)` / `mapper` / `entity` / `dto` / `common` 七个业务包，外加启动类 `AuthCoreApplication`；不得引入其他顶层包。

## 3. 数据模型（六实体）

系统数据模型共六个实体，字段定义如下（字段名与约束为契约，不得增删改名）：

- `sys_user`: id PK, username UNIQUE NOT NULL, password(BCrypt) NOT NULL, nickname, email, phone, status(1启用/0停用), created_at, updated_at
- `sys_role`: id, name, code UNIQUE, status, created_at, updated_at
- `sys_module`: id, name, code UNIQUE, base_url(可空,模块服务地址), description, status, created_at, updated_at
- `sys_permission`: id, module_id NOT NULL(逻辑外键→sys_module), name, code UNIQUE, description, created_at, updated_at
- `sys_user_role`: user_id + role_id 联合唯一
- `sys_role_permission`: role_id + permission_id 联合唯一

数据库结构变更一律通过 Flyway 迁移脚本完成，命名规则为 `V{n}__{描述}.sql`。删除策略为物理删除，不做逻辑删除；所有删除动作必须在 service 层执行引用校验，命中引用即拒绝删除。

## 4. API 规范

- 所有 REST 接口统一使用前缀 `/api/v1/{resource}`；响应体统一为 Result 结构：Result{code:int(0=成功), message:String, data:T}，即 code 为 int 型业务码且 0 表示成功，message 为 String，data 为泛型载荷 T。
- 业务错误码按资源分段：10xx 用户 / 11xx 角色 / 12xx 权限 / 13xx 模块 / 14xx 认证；HTTP 状态码语义固定为 401=未认证，403=未授权。
- 分页参数为 page(默认1)/size(默认10)；分页接口的 data 固定为 {list,total,page,size} 四个字段。
- 时间字段统一使用 ISO-8601 字符串（UTC+8）表示与传输。

## 5. 认证设计

登录接口为 POST `/api/v1/auth/login`，请求体携带 username+password；认证成功后签发 JWT(HS256)，payload{uid, username, exp}，token 有效期默认 2h。相关配置项为 jwt.secret 与 jwt.expire-hours，其中 secret 仅允许来自环境变量，严禁写入代码库或提交到配置文件。

## 6. RBAC 业务硬语义（防偏差仲裁条款）

以下五条为 RBAC 领域的硬性语义，是防止实现偏差的仲裁条款，任何实现不得违反；若认为条款需要调整，必须走文档仲裁登记流程，不得静默变通：

1. 权限必须归属模块（module_id 非空），不存在脱离模块的权限
2. 用户获得权限的唯一路径是角色（RBAC0）；禁止用户-权限直接授权
3. username / role.code / permission.code / module.code 全局唯一
4. 删除受引用保护：删除角色前校验 sys_user_role/sys_role_permission 引用；删除模块前校验其下权限；删除权限前校验 sys_role_permission；命中引用返回对应分段业务错误码
5. 停用（status=0）语义：停用用户不可登录、token 即时失效于下次校验；停用角色不再参与鉴权聚合；停用模块下权限视为无效
