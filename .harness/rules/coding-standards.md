# 编码规范

> 硬性规则，Evaluator 按此逐条核查。技术选型见 `docs/01-architecture.md`。

## Java / Spring Boot

- 文件 ≤500 行、单方法 ≤50 行；超限必须拆分——按业务职责拆分类与方法，不得为规避行数而合并或压缩逻辑。
- 禁 System.out/printStackTrace；日志 slf4j：log.debug/info/warn/error，error 必须带堆栈或上下文——保证生产环境问题可定位、日志可采集。
- 分层单向调用：controller→service→mapper；禁止跨层跳调用、禁止反向依赖——上层可调用下层，下层不得感知或回调上层。
- DTO 进出 controller，entity 不外泄；Bean Validation 注解校验请求参数——对外契约与持久化模型严格隔离，非法请求在入口即被拒绝。
- 业务异常统一继承 common 包 BusinessException(code,message)，由全局异常处理器转 Result——service 层只抛业务异常，不自行拼装响应结构。

## MyBatis-Plus

- 简单 CRUD 用 BaseMapper/IService 方法；自定义 SQL 写 XML 或 Wrapper，禁字符串拼接 SQL——杜绝手写拼接带来的注入与维护风险。
- 分页统一 PaginationInnerInterceptor；created_at/updated_at 用 MetaObjectHandler 自动填充——分页与审计字段行为全库一致，不依赖各处手工赋值。
- 表/列 snake_case ↔ Java camelCase；表前缀 sys_——映射依赖框架默认驼峰转换，无需逐列注解。

## 安全

- 密码仅 BCrypt 编码存取；JWT secret 只从环境变量读取，application.yml 不落真实密钥——配置文件可能随仓库泄露，密钥只存在于运行环境。
- 所有管理接口需认证；权限注解/校验以数据库权限码为准——前端隐藏入口不作为访问控制手段。
- 禁任何 SQL 注入面（${} 拼接）、禁敏感信息入日志——密码、token 等凭据一律不得出现在日志输出。

## Vue3 / TypeScript

- `<script setup lang="ts">` composition API；TS strict，禁 any（第三方类型缺失用 unknown+收窄）——类型即文档，让错误在编译期暴露。
- API 调用集中在 src/api/，组件内禁止直接 axios；Pinia store 管理跨组件状态——便于统一拦截器、错误处理与替换实现。
- 组件 PascalCase；页面放 src/views/，公共组件放 src/components/——目录即路由与复用边界。
- eslint + vue-tsc 零告警才可交付——告警视为缺陷而非建议。

## 命名

类 UpperCamelCase / 方法变量 lowerCamelCase / 常量 UPPER_SNAKE / REST 路径复数小写中划线
