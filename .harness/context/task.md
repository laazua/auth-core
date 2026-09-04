# Sprint 工作单：sprint-053

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-053 |
| 所属模块 | model |
| 功能点 ID | model/010 |
| 功能点名称 | 抽取 ModuleService 接口 |
| 状态 | PLANNED |
| 创建时间 | 2026-09-04 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| model/009 | 抽取 PermissionService 接口（接口模式已确立） | ✅ |
| model/005 | sys_user_role+sys_role_permission 关联表 | ✅ |

## 业务背景

当前 `ModuleService` 为具体 `@Service` 类，`ModuleController` 直接依赖该具体类，违反 `docs/01-architecture.md` 第 2 节约定的 `service(.impl)` 包结构语义。model/007、model/008、model/009 已完成 `UserService`、`RoleService`、`PermissionService` 接口化重构，model/010 沿用同一模式对 `ModuleService` 进行接口化。

目标：将 `ModuleService` 抽象为接口，Controller 和测试依赖接口而非实现类，符合依赖倒转原则（DIP）。

## 需求描述

1. **抽取 ModuleService 接口**：将 `ModuleService` 的公共方法签名提取为 `com.authcore.service.ModuleService` 接口
2. **创建 ModuleServiceImpl 实现类**：将原 `ModuleService` 逻辑迁移至 `com.authcore.service.impl.ModuleServiceImpl`，实现 `ModuleService` 接口
3. **更新 ModuleController 依赖**：Controller 构造器参数类型为 `ModuleService`（接口）
4. **更新 ModuleServiceTest**：测试类注入 `ModuleService`（接口）验证接口可正常注入

## 验收标准（TDD 驱动）

### AC1 — 创建 ModuleService 接口，定义全部业务方法签名
> `com.authcore.service.ModuleService` 为接口，包含 `queryModules`、`getModuleById`、`createModule`、`updateModule`、`queryModulePermissions`、`deleteModule` 方法

**用例**：`ModuleServiceInterfaceSpec#moduleServiceInterfaceHasAllMethods`
- 反射断言 `ModuleService` 接口声明了全部 6 个公共方法

### AC2 — ModuleServiceImpl 实现 ModuleService，原有业务逻辑不变
> `com.authcore.service.impl.ModuleServiceImpl` 实现 `ModuleService` 接口，所有方法体与原 `ModuleService` 一致

**用例**：`ModuleServiceImplSpec#moduleServiceImplImplementsInterface`
- 断言 `ModuleServiceImpl` 实现了 `ModuleService` 接口
- 断言 `ModuleServiceImpl` 被 `@Service` 注解，Spring 容器可注册为 Bean

### AC3 — ModuleController 依赖 ModuleService 接口，编译通过
> `ModuleController` 构造器参数类型为 `ModuleService`（接口），Spring 可正常注入 `ModuleServiceImpl`

**用例**：`ModuleControllerSpec#controllerInjectsServiceInterface`
- 通过 `@SpringBootTest` 上下文加载，断言 `ModuleController` 中 `ModuleService` 字段类型为接口
- 断言 `ModuleController` 不直接依赖 `ModuleServiceImpl`

### AC4 — 单元测试全量通过（ModuleService 接口注入 + 业务逻辑不变）
> `ModuleServiceTest` 通过 `@Autowired` 注入 `ModuleService`（接口），所有业务测试用例通过

**用例**：`ModuleServiceTest#allTestsPass`
- 运行 `ModuleServiceTest` 全部用例，验证分页查询、详情、创建、更新、删除等行为不变

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | ModuleServiceInterfaceSpec#moduleServiceInterfaceHasAllMethods | AC1 | ✅ 通过 |
| 2 | ModuleServiceImplSpec#moduleServiceImplImplementsInterface | AC2 | ✅ 通过 |
| 3 | ModuleControllerSpec#controllerInjectsServiceInterface | AC3 | ✅ 通过 |
| 4 | ModuleServiceTest#allTestsPass | AC4 | ✅ 通过 |

## RED 证据

```text
[RED] ModuleServiceImpl 不存在时：ModuleServiceImplSpec 编译失败（找不到符号 com.authcore.service.impl.ModuleServiceImpl）
[RED] ModuleService 非接口时：ModuleServiceInterfaceSpec.moduleServiceIsInterface 断言 ModuleService.class.isInterface() 失败
[RED] ModuleController 依赖具体类时：ModuleControllerSpec.controllerInjectsServiceInterface 断言构造器参数类型不为 ModuleService 接口
```

## GREEN 证据

```text
[GREEN] ModuleServiceInterfaceSpec 2/2 通过：接口声明 6 个公共方法，且 ModuleService 为接口类型
[GREEN] ModuleServiceImplSpec 1/1 通过：ModuleServiceImpl 实现了 ModuleService 接口，且被 @Service 注解
[GREEN] ModuleControllerSpec 2/2 通过：Controller 构造器依赖 ModuleService 接口，不直接依赖 ModuleServiceImpl
[GREEN] ModuleServiceTest 6/6 通过：@Autowired 注入 ModuleService 接口，分页查询、详情、创建、更新、删除等行为不变
[GREEN] ModuleControllerTest 6/6 通过：Controller 端到端测试全量通过
[GREEN] 实现文件：
  - backend/src/main/java/com/authcore/service/ModuleService.java（接口，6 个方法）
  - backend/src/main/java/com/authcore/service/impl/ModuleServiceImpl.java（实现 @Service + @Transactional）
  - backend/src/test/java/com/authcore/service/ModuleServiceInterfaceSpec.java（AC1）
  - backend/src/test/java/com/authcore/service/ModuleServiceImplSpec.java（AC2）
  - backend/src/test/java/com/authcore/controller/ModuleControllerSpec.java（AC3）
[GREEN] mvn test -Dtest=ModuleServiceInterfaceSpec,ModuleServiceImplSpec,ModuleControllerSpec,ModuleServiceTest,ModuleControllerTest：17/17 通过
```

## 门禁与冒烟记录

- 冒烟 `bash scripts/smoke.sh` model-010 用例：通过（17/17 测试通过）
- 门禁 `mvn -q verify`：存在预存失败（DataSourceConfigBindingTest 环境配置、RoleControllerTest#assignPermissionsInvalidPermissionReturns400 见 infra-002 排除列表、TestLayersSpec/TestUtilsSpec 需 Docker、SeedDataIntegrationTest 种子数据），均与本改动无关

## 拆分说明

本功能点为 model 模块接口化重构的第四个子功能点。后续子功能点依次为：
- model/011：抽取 AuthService 接口
- model/012：更新所有 Controller 层依赖接口

## 交付物（预估 ≤6 文件）

1. `backend/src/main/java/com/authcore/service/ModuleService.java` — 新建接口（替换原具体类）
2. `backend/src/main/java/com/authcore/service/impl/ModuleServiceImpl.java` — 新建实现类（原 ModuleService 逻辑迁移）
3. `backend/src/main/java/com/authcore/controller/ModuleController.java` — 注入类型改为 ModuleService 接口
4. `backend/src/test/java/com/authcore/service/ModuleServiceTest.java` — 注入类型改为 ModuleService 接口
5. `backend/src/test/java/com/authcore/service/ModuleServiceInterfaceSpec.java`（新）— AC1 用例
6. `backend/src/test/java/com/authcore/service/ModuleServiceImplSpec.java`（新）— AC2 用例
7. `backend/src/test/java/com/authcore/controller/ModuleControllerSpec.java`（新）— AC3 用例

## 变更清单

### 新增
- `backend/src/main/java/com/authcore/service/ModuleService.java`（接口）
- `backend/src/main/java/com/authcore/service/impl/ModuleServiceImpl.java`
- `backend/src/test/java/com/authcore/service/ModuleServiceInterfaceSpec.java`
- `backend/src/test/java/com/authcore/service/ModuleServiceImplSpec.java`
- `backend/src/test/java/com/authcore/controller/ModuleControllerSpec.java`

### 修改
- `backend/src/main/java/com/authcore/controller/ModuleController.java` — 无需修改，构造器已注入 ModuleService 接口类型
- `backend/src/test/java/com/authcore/service/ModuleServiceTest.java` — 无需修改，已注入 ModuleService 接口类型

## 规范检查清单

- [x] 包结构符合 `docs/01-architecture.md` 第 2 节约定的 `service(.impl)` 结构
- [x] Controller 仅依赖 Service 接口，不依赖实现类
- [x] `@Service` 注解仅存在于实现类 `ModuleServiceImpl`
- [x] 接口方法签名与原 `ModuleService` 完全一致
- [x] `mvn -q verify` 关键用例全绿（预存失败与本次无关）
- [x] 冒烟 `bash scripts/smoke.sh` model-010 用例通过
- [x] 符合 TDD 工作流：测试先行（RED）→ 最小实现（GREEN）→ 重构
