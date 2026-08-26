# 编码规范（高质量交付标准）

> 本文件是 Generator 编码遵循、Evaluator「规范遵守」维度逐条核查的硬规则。技术选型见 `docs/01-architecture.md`。
> 质量目标：Agent 写出的每一行代码达到资深工程师评审可直接通过的水准——正确、清晰、一致、可测、安全。
> 未尽事宜依次按 Google Java Style Guide / Vue3 官方风格指南（Priority A/B）/ TypeScript strict 惯例执行；与 `docs/01-architecture.md` 冲突时以后者为准。

## 0. 总原则（规则冲突时按此排序裁决）

1. **正确性优先**：业务语义正确 > 性能优化 > 代码简洁；拿不准时回到 `docs/01-architecture.md` 第 6 节 RBAC 硬语义
2. **最小惊讶**：命名即行为说明；代码结构符合读者预期，禁止炫技式写法
3. **单一职责**：一个类/方法/组件/store 只做一件事；一个方法只抽象一层逻辑
4. **DRY 有度**：同样代码出现三处才考虑抽象；禁止为复用制造牵强耦合
5. **从源头消除错误**：用类型与状态设计让非法状态不可表示，而非层层判空防御

## 1. 格式与结构

- 源文件 UTF-8；4 空格缩进（禁 Tab）；单行 ≤120 字符
- K&R 大括号风格且任何分支不可省略大括号；无通配符 import；每个顶级类一个源文件
- 文件 ≤500 行、方法 ≤50 行、方法圈复杂度 ≤10、控制流嵌套 ≤3 层——超限必须拆分，不得为规避行数而合并压缩逻辑
- 类成员之间、方法内逻辑分组之间用一个空行分隔；保留字与左括号间加空格；二元运算符两侧加空格

## 2. Java 21 语言层

| 标识符 | 风格 | 示例 |
|--------|------|------|
| 包名 | 全小写连拼 | `com.authcore.service` |
| 类/接口 | UpperCamelCase | `UserService` |
| 方法/变量 | lowerCamelCase | `getUserById` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 枚举值 | UPPER_SNAKE_CASE | `Status.PENDING` |

- 测试类 `<被测类>Test`；抽象类 `Abstract` 前缀；接口不加 `I` 前缀；实现类 `Impl` 后缀
- `record` 用于不可变数据载体（DTO/VO）；数据库实体仍用常规 POJO（MyBatis-Plus 兼容）
- `Optional` 仅作返回类型表达"可能无值"；禁止作字段或参数类型；禁止未检查的 `get()`，优先 `orElseThrow`
- `@Override` 必须使用；`@SuppressWarnings` 必须附注释说明原因
- 方法引用优先于 lambda（`String::isEmpty`）；lambda 体 >3 行必须提取方法；Stream 禁止副作用
- 时间一律 `java.time`（LocalDateTime/DateTimeFormatter）；禁止共享 `SimpleDateFormat`
- 返回集合/数组时返回空集合而非 `null`
- 公共类与公共方法必须有 Javadoc（`@param`/`@return`/`@throws`），注释写 why 不写 what

## 3. Spring Boot 工程层

- 分层单向调用 controller→service→mapper，禁止跨层调用与反向依赖；DTO 进出 controller，entity 不外泄
- **controller 只做三件事**：接收参数（Bean Validation 校验）→ 委托 service → 包装 Result 返回；任何 if 业务判断、数据组装都不允许出现在 controller
- 构造器注入（Lombok `@RequiredArgsConstructor`）；禁止字段注入（`@Autowired` 直接标字段）
- `@Transactional` 只置于 service 实现方法上：查询方法标 `readOnly = true`；禁止在 controller 开事务；注意同类自调用导致事务失效——需要时拆分类或经代理调用
- RESTful 语义正确：GET 幂等无副作用、POST 创建、PUT 全量更新、DELETE 删除；资源名复数小写中划线（`/api/v1/user-roles`）
- 配置绑定用 `@ConfigurationProperties` 类型安全类；业务代码内禁止散落 `@Value`

## 4. MyBatis-Plus 与数据层

- 简单 CRUD 用 BaseMapper/IService 内置方法；自定义 SQL 写 XML 或 Wrapper，**禁止字符串拼接 SQL**
- `${}` 仅限动态表名等白名单场景且必须代码级白名单校验，其余参数一律 `#{}`
- 列表查询必须分页（PaginationInnerInterceptor 统一实现），**禁止无界全量查询**
- 明确列清单代替 `select *`；大字段（TEXT/JSON）独立查询或延迟加载
- **禁止 N+1**：循环体内查库一律改为批量查询 + 内存组装
- 批量写入分批提交（每批 ≤1000 条），禁止循环单条 insert/update
- 表设计必备：`id BIGINT` 主键、`created_at`/`updated_at`（MetaObjectHandler 自动填充）；唯一性由唯一索引保证，不能只靠应用层校验；关联用逻辑外键 + service 层维护引用完整性（删除保护语义见 `docs/01-architecture.md` 第 6 节）
- `is_` 开头的布尔列名禁止（语义歧义），布尔语义用 `status` 或 `xxx_enabled` 表达

## 5. 异常与日志

- 异常分层：业务失败抛 `BusinessException(code, message)`（common 包统一机制）；系统异常由全局异常处理器兜底翻译为 Result
- 禁止空 catch 块或仅 `e.printStackTrace()`；禁止用异常做流程控制；finally 中禁止 return
- 日志统一 slf4j，四规则：
  - 占位符 `{}` 输出，禁止字符串拼接
  - `error`/`warn` 必须带上下文数据与堆栈（`log.error("xxx, id={}", id, e)`）
  - 循环体内禁止 info 及以上级别日志
  - 敏感信息（密码、token、密钥、身份证等凭据）永不入日志

## 6. 安全

- 密码仅 BCrypt 编码存取；JWT secret 只从环境变量读取，application.yml 不落真实密钥
- 所有管理接口默认需要认证；授权判定以数据库权限码为准——前端隐藏入口不作为访问控制手段
- 入口即校验：所有外部输入 Bean Validation 注解校验 + service 层业务规则双校验；越权防护在 service 层做资源归属/引用校验
- 引入新依赖必须说明用途并确认无可替代的既有依赖；禁止引入存在已知高危 CVE 且有替代品的库

## 7. TypeScript（前端全局）

- tsconfig 严格族全开：`strict`、`noUnusedLocals`、`noUnusedParameters`、`noImplicitReturns`、`noFallthroughCasesInSwitch`
- **绝对禁止 `any`**：未知类型用 `unknown` + 类型守卫收窄；宽松字典用 `Record<string, unknown>`；第三方缺型用声明补齐
- 导出的公共函数显式标注参数与返回类型；async 函数返回 `Promise<T>`
- 对象形态优先 `interface`（联合/交叉/映射类型用 `type`）；接口禁 `I` 前缀
- 纯类型导入用 `import type`；深属性访问用 `?.` + `??` 组合；非空断言 `!` 仅在能证明非空处使用并附注释
- 错误处理统一走 Axios 拦截器（toast + 401 重定向）；catch 中 error 按 `unknown` 收窄后使用

## 8. Vue3 组件

- `<script setup lang="ts">` composition API；SFC 块顺序 script→template→style；禁止 Options API
- 多单词组件名；基础组件 `Base` 前缀、单例组件 `The` 前缀；可复用逻辑抽 composable（`useXxx`，放 src/composables/）
- Props 用 `defineProps<接口>` 泛型定义 + `withDefaults` 给默认值；事件用 `defineEmits` 显式签名；事件名 kebab-case
- `v-for` 必带稳定唯一 key，可重排列表禁止 index 作 key；`v-if` 与 `v-for` 禁止作用于同一元素；条件渲染默认 v-if，高频切换才 v-show
- 模板表达式保持简单——一切派生数据用 `computed`；元素属性超过 3 个逐行书写
- 单组件 ≤300 行（不含样式），超限拆子组件
- API 调用集中在 src/api/ 并与后端 DTO 对齐类型定义；组件内禁止直接 axios
- Pinia setup store 一域一 store；action 内不做 UI 逻辑（toast/跳转留在组件层）
- 路由懒加载 + `meta: { requiresAuth, title }`；路由参数经 `props: true` 传入页面
- 副作用清理：定时器/事件监听在 `onUnmounted` 移除；大数据列表 `shallowRef` 或虚拟滚动；重组件 `defineAsyncComponent`

## 9. 测试代码质量（测试也是交付物）

- 测试代码同样遵守本文件全部格式与命名规则；JUnit5 用 `@DisplayName` 中文说明行为
- AAA 结构（Arrange-Act-Assert / Given-When-Then）分节；一个用例只断言一条行为主线
- 断言具体预期值，禁止 `assertNotNull` 式凑数断言；关键路径同时验证状态与协作（verify）
- 测试相互独立：禁止依赖执行顺序、共享可变状态、`Thread.sleep` 同步
- mock 只用于外部依赖（DB/HTTP/时钟），禁止 mock 被测对象内部方法
- 测试数据用工厂方法/Builder 构造，禁止巨型字面量在多个用例间复制粘贴

## 10. 提交纪律

- 两段式 commit 规范见 `.harness/rules/tdd-workflow.md`；commit message 一句话说清"做了什么、为什么"
- 变更范围 = 当前功能点最小集；无关文件的顺手格式化、重构禁止混入同一提交

## 11. Evaluator 快速核查清单（规范遵守维度逐项核对）

- [ ] 行数/方法长度/圈复杂度/嵌套深度达标（第 1 节）
- [ ] 无 System.out/printStackTrace/吞异常；日志占位符+堆栈+敏感信息三合规（第 5 节）
- [ ] 分层单向、controller 三件事、构造器注入、事务位置与 readOnly 正确（第 3 节）
- [ ] 无 select */无 N+1/批量分批/分页无界检查（第 4 节）
- [ ] 无硬编码密钥/明文密码/${} 注入面；越权防护到位（第 6 节）
- [ ] TS 无 any/strict 全开/import type；Vue script setup/scoped/key/composable 归位（第 7-8 节）
- [ ] 测试 AAA 结构/断言有效/独立运行/mock 边界正确（第 9 节）
