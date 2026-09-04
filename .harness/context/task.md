# Sprint 工作单：sprint-052

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-052 |
| 所属模块 | model |
| 功能点 ID | model/009 |
| 功能点名称 | 抽取 PermissionService 接口 |
| 状态 | PLANNED |
| 创建时间 | 2026-09-04 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| model/008 | 抽取 RoleService 接口（接口模式已确立） | ✅ |
| model/005 | sys_user_role+sys_role_permission 关联表 | ✅ |

## 业务背景

当前 `PermissionService` 为具体 `@Service` 类，`PermissionController` 直接依赖该具体类，违反 `docs/01-architecture.md` 第 2 节约定的 `service(.impl)` 包结构语义。model/007、model/008 已完成 `UserService`、`RoleService` 接口化重构，model/009 沿用同一模式对 `PermissionService` 进行接口化。

目标：将 `PermissionService` 抽象为接口，Controller 和测试依赖接口而非实现类，符合依赖倒转原则（DIP）。

## 需求描述

1. **抽取 PermissionService 接口**：将 `PermissionService` 的公共方法签名提取为 `com.authcore.service.PermissionService` 接口
2. **创建 PermissionServiceImpl 实现类**：将原 `PermissionService` 逻辑迁移至 `com.authcore.service.impl.PermissionServiceImpl`，实现 `PermissionService` 接口
3. **更新 PermissionController 依赖**：Controller 构造器参数类型为 `PermissionService`（接口）
4. **更新 PermissionServiceTest**：测试类注入 `PermissionService`（接口）验证接口可正常注入

## 验收标准（TDD 驱动）

### AC1 — 创建 PermissionService 接口，定义全部业务方法签名
> `com.authcore.service.PermissionService` 为接口，包含 `queryPermissions`、`queryPermissionsGroupedByModule`、`getPermissionById`、`createPermission`、`updatePermission`、`deletePermission` 方法

**用例**：`PermissionServiceInterfaceSpec#permissionServiceInterfaceHasAllMethods`
- 反射断言 `PermissionService` 接口声明了全部 6 个公共方法

### AC2 — PermissionServiceImpl 实现 PermissionService，原有业务逻辑不变
> `com.authcore.service.impl.PermissionServiceImpl` 实现 `PermissionService` 接口，所有方法体与原 `PermissionService` 一致

**用例**：`PermissionServiceImplSpec#permissionServiceImplImplementsInterface`
- 断言 `PermissionServiceImpl` 实现了 `PermissionService` 接口
- 断言 `PermissionServiceImpl` 被 `@Service` 注解，Spring 容器可注册为 Bean

### AC3 — PermissionController 依赖 PermissionService 接口，编译通过
> `PermissionController` 构造器参数类型为 `PermissionService`（接口），Spring 可正常注入 `PermissionServiceImpl`

**用例**：`PermissionControllerSpec#controllerInjectsServiceInterface`
- 通过 `@SpringBootTest` 上下文加载，断言 `PermissionController` 中 `PermissionService` 字段类型为接口
- 断言 `PermissionController` 不直接依赖 `PermissionServiceImpl`

### AC4 — 单元测试全量通过（PermissionService 接口注入 + 业务逻辑不变）
> `PermissionServiceTest` 通过 `@Autowired` 注入 `PermissionService`（接口），所有业务测试用例通过

**用例**：`PermissionServiceTest#allTestsPass`
- 运行 `PermissionServiceTest` 全部用例，验证分页查询、分组查询、详情、创建、更新、删除等行为不变

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | PermissionServiceInterfaceSpec#permissionServiceInterfaceHasAllMethods | AC1 | 待写 |
| 2 | PermissionServiceImplSpec#permissionServiceImplImplementsInterface | AC2 | 待写 |
| 3 | PermissionControllerSpec#controllerInjectsServiceInterface | AC3 | 待写 |
| 4 | PermissionServiceTest#allTestsPass | AC4 | 待写 |

## RED 证据

待 Generator 实现阶段填写。核心 RED 场景：若 `PermissionServiceImpl` 不存在，`PermissionControllerSpec` 和 `PermissionServiceTest` 因 Spring 无法为 `PermissionService` 接口找到实现 Bean 而上下文加载失败；若 `PermissionService` 非接口，`PermissionServiceInterfaceSpec.permissionServiceIsInterface` 断言失败。

## GREEN 证据

待 Generator 实现阶段填写。

## 门禁与冒烟记录

待 Generator 实现阶段填写。

## 拆分说明

本功能点为 model 模块接口化重构的第三个子功能点。后续子功能点依次为：
- model/010：抽取 ModuleService 接口
- model/011：抽取 AuthService 接口
- model/012：更新所有 Controller 层依赖接口

## 交付物（预估 ≤6 文件）

1. `backend/src/main/java/com/authcore/service/PermissionService.java` — 新建接口（替换原具体类）
2. `backend/src/main/java/com/authcore/service/impl/PermissionServiceImpl.java` — 新建实现类（原 PermissionService 逻辑迁移）
3. `backend/src/main/java/com/authcore/controller/PermissionController.java` — 注入类型改为 PermissionService 接口
4. `backend/src/test/java/com/authcore/service/PermissionServiceTest.java` — 注入类型改为 PermissionService 接口
5. `backend/src/test/java/com/authcore/service/PermissionServiceInterfaceSpec.java`（新）— AC1 用例
6. `backend/src/test/java/com/authcore/service/PermissionServiceImplSpec.java`（新）— AC2 用例
7. `backend/src/test/java/com/authcore/controller/PermissionControllerSpec.java`（新）— AC3 用例

## 变更清单

### 新增
- `backend/src/main/java/com/authcore/service/PermissionService.java`（接口）
- `backend/src/main/java/com/authcore/service/impl/PermissionServiceImpl.java`
- `backend/src/test/java/com/authcore/service/PermissionServiceInterfaceSpec.java`
- `backend/src/test/java/com/authcore/service/PermissionServiceImplSpec.java`
- `backend/src/test/java/com/authcore/controller/PermissionControllerSpec.java`

### 修改
- `backend/src/main/java/com/authcore/controller/PermissionController.java` — 无需修改，构造器已注入 PermissionService 接口类型
- `backend/src/test/java/com/authcore/service/PermissionServiceTest.java` — 无需修改，已注入 PermissionService 接口类型

## 规范检查清单

- [ ] 包结构符合 `docs/01-architecture.md` 第 2 节约定的 `service(.impl)` 结构
- [ ] Controller 仅依赖 Service 接口，不依赖实现类
- [ ] `@Service` 注解仅存在于实现类 `PermissionServiceImpl`
- [ ] 接口方法签名与原 `PermissionService` 完全一致
- [ ] `mvn -q verify` 关键用例全绿（预存失败与本次无关）
- [ ] 冒烟 `bash scripts/smoke.sh` model-009 用例通过
- [ ] 符合 TDD 工作流：测试先行（RED）→ 最小实现（GREEN）→ 重构
