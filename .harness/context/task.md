# Sprint 工作单：sprint-054

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-054 |
| 所属模块 | model |
| 功能点 ID | model/011 |
| 功能点名称 | 抽取 AuthService 接口 |
| 状态 | PLANNED |
| 创建时间 | 2026-09-04 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| model/010 | 抽取 ModuleService 接口（接口模式已确立） | ✅ |
| model/005 | sys_user_role+sys_role_permission 关联表 | ✅ |

## 业务背景

当前 `AuthService` 为具体 `@Service` 类，`AuthController` 直接依赖该具体类，违反 `docs/01-architecture.md` 第 2 节约定的 `service(.impl)` 包结构语义。model/007、model/008、model/009、model/010 已完成 `UserService`、`RoleService`、`PermissionService`、`ModuleService` 接口化重构，model/011 沿用同一模式对 `AuthService` 进行接口化。

目标：将 `AuthService` 抽象为接口，Controller 和测试依赖接口而非实现类，符合依赖倒转原则（DIP）。

## 需求描述

1. **抽取 AuthService 接口**：将 `AuthService` 的公共方法签名提取为 `com.authcore.service.AuthService` 接口
2. **创建 AuthServiceImpl 实现类**：将原 `AuthService` 逻辑迁移至 `com.authcore.service.impl.AuthServiceImpl`，实现 `AuthService` 接口
3. **更新 AuthController 依赖**：Controller 构造器参数类型为 `AuthService`（接口）
4. **更新 AuthServiceTest**：测试类注入 `AuthService`（接口）验证接口可正常注入

## 验收标准（TDD 驱动）

### AC1 — 创建 AuthService 接口，定义全部业务方法签名
> `com.authcore.service.AuthService` 为接口，包含 `getCurrentUserInfo`、`checkPermission` 方法

**用例**：`AuthServiceInterfaceSpec#authServiceInterfaceHasAllMethods`
- 反射断言 `AuthService` 接口声明了全部 2 个公共方法

### AC2 — AuthServiceImpl 实现 AuthService，原有业务逻辑不变
> `com.authcore.service.impl.AuthServiceImpl` 实现 `AuthService` 接口，所有方法体与原 `AuthService` 一致

**用例**：`AuthServiceImplSpec#authServiceImplImplementsInterface`
- 断言 `AuthServiceImpl` 实现了 `AuthService` 接口
- 断言 `AuthServiceImpl` 被 `@Service` 注解，Spring 容器可注册为 Bean

### AC3 — AuthController 依赖 AuthService 接口，编译通过
> `AuthController` 构造器参数类型为 `AuthService`（接口），Spring 可正常注入 `AuthServiceImpl`

**用例**：`AuthControllerSpec#controllerInjectsServiceInterface`
- 通过 `@SpringBootTest` 上下文加载，断言 `AuthController` 中 `AuthService` 字段类型为接口
- 断言 `AuthController` 不直接依赖 `AuthServiceImpl`

### AC4 — 单元测试全量通过（AuthService 接口注入 + 业务逻辑不变）
> `AuthServiceTest` 通过 `@Autowired` 注入 `AuthService`（接口），所有业务测试用例通过

**用例**：`AuthServiceTest#allTestsPass`
- 运行 `AuthServiceTest` 全部用例，验证聚合查询、权限校验等行为不变

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | AuthServiceInterfaceSpec#authServiceInterfaceHasAllMethods | AC1 | ✅ 通过 |
| 2 | AuthServiceImplSpec#authServiceImplImplementsInterface | AC2 | ✅ 通过 |
| 3 | AuthControllerSpec#controllerInjectsServiceInterface | AC3 | ✅ 通过 |
| 4 | AuthServiceTest#allTestsPass | AC4 | ✅ 通过 |

## RED 证据

```text
[RED] AuthServiceImpl 不存在时：AuthServiceImplSpec 编译失败（找不到符号 com.authcore.service.impl.AuthServiceImpl）
[RED] AuthService 非接口时：AuthServiceInterfaceSpec.authServiceIsInterface 断言 AuthService.class.isInterface() 失败
[RED] AuthController 依赖具体类时：AuthControllerSpec.controllerInjectsServiceInterface 断言构造器参数类型不为 AuthService 接口
```

## GREEN 证据

```text
[GREEN] AuthServiceInterfaceSpec 2/2 通过：接口声明 2 个公共方法，且 AuthService 为接口类型
[GREEN] AuthServiceImplSpec 1/1 通过：AuthServiceImpl 实现了 AuthService 接口，且被 @Service 注解
[GREEN] AuthControllerSpec 2/2 通过：Controller 构造器依赖 AuthService 接口，不直接依赖 AuthServiceImpl
[GREEN] AuthServiceTest 1/1 通过：@Autowired 注入 AuthService 接口，聚合查询、权限校验等行为不变
[GREEN] AuthControllerTest 14/14 通过：Controller 端到端测试全量通过
[GREEN] 实现文件：
  - backend/src/main/java/com/authcore/service/AuthService.java（接口，2 个方法）
  - backend/src/main/java/com/authcore/service/impl/AuthServiceImpl.java（实现 @Service + @Transactional(readOnly=true)）
  - backend/src/test/java/com/authcore/service/AuthServiceInterfaceSpec.java（AC1）
  - backend/src/test/java/com/authcore/service/AuthServiceImplSpec.java（AC2）
  - backend/src/test/java/com/authcore/controller/AuthControllerSpec.java（AC3）
[GREEN] mvn test -Dtest=AuthServiceInterfaceSpec,AuthServiceImplSpec,AuthControllerSpec,AuthServiceTest,AuthControllerTest：20/20 通过
```

## 门禁与冒烟记录

- 冒烟 `bash scripts/smoke.sh` model-011 用例：通过（20/20 测试通过）
- 门禁 `mvn -q verify`：存在预存失败（DataSourceConfigBindingTest 环境配置、RoleControllerTest#assignPermissionsInvalidPermissionReturns400 见 infra-002 排除列表、TestLayersSpec/TestUtilsSpec 需 Docker、SeedDataIntegrationTest 种子数据），均与本改动无关

## 拆分说明

本功能点为 model 模块接口化重构的第五个子功能点。后续子功能点依次为：
- model/012：更新所有 Controller 层依赖接口

## 交付物（预估 ≤6 文件）

1. `backend/src/main/java/com/authcore/service/AuthService.java` — 新建接口（替换原具体类）
2. `backend/src/main/java/com/authcore/service/impl/AuthServiceImpl.java` — 新建实现类（原 AuthService 逻辑迁移）
3. `backend/src/main/java/com/authcore/controller/AuthController.java` — 注入类型改为 AuthService 接口
4. `backend/src/test/java/com/authcore/service/AuthServiceTest.java` — 注入类型改为 AuthService 接口
5. `backend/src/test/java/com/authcore/service/AuthServiceInterfaceSpec.java`（新）— AC1 用例
6. `backend/src/test/java/com/authcore/service/AuthServiceImplSpec.java`（新）— AC2 用例
7. `backend/src/test/java/com/authcore/controller/AuthControllerSpec.java`（新）— AC3 用例

## 变更清单

### 新增
- `backend/src/main/java/com/authcore/service/AuthService.java`（接口）
- `backend/src/main/java/com/authcore/service/impl/AuthServiceImpl.java`
- `backend/src/test/java/com/authcore/service/AuthServiceInterfaceSpec.java`
- `backend/src/test/java/com/authcore/service/AuthServiceImplSpec.java`
- `backend/src/test/java/com/authcore/controller/AuthControllerSpec.java`

### 修改
- `backend/src/main/java/com/authcore/controller/AuthController.java` — 无需修改，构造器已注入 AuthService 接口类型
- `backend/src/test/java/com/authcore/service/AuthServiceTest.java` — 无需修改，已注入 AuthService 接口类型

## 规范检查清单

- [x] 包结构符合 `docs/01-architecture.md` 第 2 节约定的 `service(.impl)` 结构
- [x] Controller 仅依赖 Service 接口，不依赖实现类
- [x] `@Service` 注解仅存在于实现类 `AuthServiceImpl`
- [x] 接口方法签名与原 `AuthService` 完全一致
- [x] `mvn -q verify` 关键用例全绿（预存失败与本次无关）
- [x] 冒烟 `bash scripts/smoke.sh` model-011 用例通过
- [x] 符合 TDD 工作流：测试先行（RED）→ 最小实现（GREEN）→ 重构