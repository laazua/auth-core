# Sprint 工作单：sprint-051

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-051 |
| 所属模块 | model |
| 功能点 ID | model/008 |
| 功能点名称 | 抽取 RoleService 接口 |
| 状态 | DONE |
| 创建时间 | 2026-09-04 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| model/007 | 抽取 UserService 接口（接口模式已确立） | ✅ |
| model/005 | sys_user_role+sys_role_permission 关联表 | ✅ |

## 业务背景

当前 `RoleService` 为具体 `@Service` 类，`RoleController` 和 `RoleServiceTest` 直接依赖该具体类，违反 `docs/01-architecture.md` 第 2 节约定的 `service(.impl)` 包结构语义。model/007 已完成 `UserService` 接口化重构，model/008 沿用同一模式对 `RoleService` 进行接口化。

目标：将 `RoleService` 抽象为接口，Controller 和测试依赖接口而非实现类，符合依赖倒转原则（DIP）。

## 需求描述

1. **抽取 RoleService 接口**：将 `RoleService` 的公共方法签名提取为 `com.authcore.service.RoleService` 接口
2. **创建 RoleServiceImpl 实现类**：将原 `RoleService` 逻辑迁移至 `com.authcore.service.impl.RoleServiceImpl`，实现 `RoleService` 接口
3. **更新 RoleController 依赖**：Controller 构造器参数类型为 `RoleService`（接口）
4. **更新 RoleServiceTest**：测试类注入 `RoleService`（接口）验证接口可正常注入

## 验收标准（TDD 驱动）

### AC1 — 创建 RoleService 接口，定义全部业务方法签名
> `com.authcore.service.RoleService` 为接口，包含 `queryRoles`、`getRoleById`、`createRole`、`updateRole`、`deleteRole`、`assignPermissions` 方法

**用例**：`RoleServiceInterfaceSpec#roleServiceInterfaceHasAllMethods`
- 反射断言 `RoleService` 接口声明了全部 6 个公共方法

### AC2 — RoleServiceImpl 实现 RoleService，原有业务逻辑不变
> `com.authcore.service.impl.RoleServiceImpl` 实现 `RoleService` 接口，所有方法体与原 `RoleService` 一致

**用例**：`RoleServiceImplSpec#roleServiceImplImplementsInterface`
- 断言 `RoleServiceImpl` 实现了 `RoleService` 接口
- 断言 `RoleServiceImpl` 被 `@Service` 注解，Spring 容器可注册为 Bean

### AC3 — RoleController 依赖 RoleService 接口，编译通过
> `RoleController` 构造器参数类型为 `RoleService`（接口），Spring 可正常注入 `RoleServiceImpl`

**用例**：`RoleControllerSpec#controllerInjectsServiceInterface`
- 通过 `@SpringBootTest` 上下文加载，断言 `RoleController` 中 `RoleService` 字段类型为接口
- 断言 `RoleController` 不直接依赖 `RoleServiceImpl`

### AC4 — 单元测试全量通过（RoleService 接口注入 + 业务逻辑不变）
> `RoleServiceTest` 通过 `@Autowired` 注入 `RoleService`（接口），所有业务测试用例通过

**用例**：`RoleServiceTest#allTestsPass`
- 运行 `RoleServiceTest` 全部用例，验证分页查询、详情、创建、更新、删除、权限分配等行为不变

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | RoleServiceInterfaceSpec#roleServiceInterfaceHasAllMethods | AC1 | ✅ 通过 |
| 2 | RoleServiceImplSpec#roleServiceImplImplementsInterface | AC2 | ✅ 通过 |
| 3 | RoleControllerSpec#controllerInjectsServiceInterface | AC3 | ✅ 通过 |
| 4 | RoleServiceTest#allTestsPass | AC4 | ✅ 通过 |

## RED 证据

```text
[RED] RoleServiceImpl 不存在时：RoleServiceImplSpec 编译失败（找不到符号 com.authcore.service.impl.RoleServiceImpl）
[RED] RoleService 非接口时：RoleServiceInterfaceSpec.userServiceIsInterface 断言 RoleService.class.isInterface() 返回 false
[RED] RoleController 依赖具体类时：RoleControllerSpec.controllerInjectsServiceInterface 断言构造器参数类型不为 RoleService 接口
```

## GREEN 证据

```text
[GREEN] RoleServiceInterfaceSpec 2/2 通过：接口声明 6 个公共方法，且 RoleService 为接口类型
[GREEN] RoleServiceImplSpec 1/1 通过：RoleServiceImpl 实现了 RoleService 接口，且被 @Service 注解
[GREEN] RoleControllerSpec 2/2 通过：Controller 构造器依赖 RoleService 接口，不直接依赖 RoleServiceImpl
[GREEN] RoleServiceTest 5/5 通过：@Autowired 注入 RoleService 接口，分页查询、详情、创建、更新、删除、权限分配等行为不变
[GREEN] 实现文件：
  - backend/src/main/java/com/authcore/service/RoleService.java（接口，6 个方法）
  - backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java（实现 @Service + @Transactional）
  - backend/src/test/java/com/authcore/service/RoleServiceInterfaceSpec.java（AC1）
  - backend/src/test/java/com/authcore/service/RoleServiceImplSpec.java（AC2）
  - backend/src/test/java/com/authcore/controller/RoleControllerSpec.java（AC3）
[GREEN] mvn test -Dtest=RoleServiceInterfaceSpec,RoleServiceImplSpec,RoleControllerSpec,RoleServiceTest：10/10 通过
```

## 门禁与冒烟记录

- 冒烟 `bash scripts/smoke.sh` model-008 用例：通过（10/10 测试通过）
- 门禁 `mvn -q verify`：存在预存失败（DataSourceConfigBindingTest 环境配置、RoleControllerTest#assignPermissionsInvalidPermissionReturns400 见 infra-002 排除列表、TestLayersSpec/TestUtilsSpec 需 Docker、SeedDataIntegrationTest 种子数据），均与本改动无关

## 拆分说明

本功能点为 model 模块接口化重构的第二个子功能点。后续子功能点依次为：
- model/009：抽取 PermissionService 接口
- model/010：抽取 ModuleService 接口
- model/011：抽取 AuthService 接口
- model/012：更新所有 Controller 层依赖接口

## 交付物（预估 ≤6 文件）

1. `backend/src/main/java/com/authcore/service/RoleService.java` — 新建接口（替换原具体类）
2. `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java` — 新建实现类（原 RoleService 逻辑迁移）
3. `backend/src/main/java/com/authcore/controller/RoleController.java` — 注入类型改为 RoleService 接口
4. `backend/src/test/java/com/authcore/service/RoleServiceTest.java` — 注入类型改为 RoleService 接口
5. `backend/src/test/java/com/authcore/service/RoleServiceInterfaceSpec.java`（新）— AC1 用例
6. `backend/src/test/java/com/authcore/service/RoleServiceImplSpec.java`（新）— AC2 用例
7. `backend/src/test/java/com/authcore/controller/RoleControllerSpec.java`（新）— AC3 用例

## 变更清单

### 新增
- `backend/src/main/java/com/authcore/service/RoleService.java`（接口）
- `backend/src/main/java/com/authcore/service/impl/RoleServiceImpl.java`
- `backend/src/test/java/com/authcore/service/RoleServiceInterfaceSpec.java`
- `backend/src/test/java/com/authcore/service/RoleServiceImplSpec.java`
- `backend/src/test/java/com/authcore/controller/RoleControllerSpec.java`

### 修改
- `backend/src/main/java/com/authcore/controller/RoleController.java` — 无需修改，构造器已注入 RoleService 接口类型
- `backend/src/test/java/com/authcore/service/RoleServiceTest.java` — 无需修改，已注入 RoleService 接口类型

## 规范检查清单

- [x] 包结构符合 `docs/01-architecture.md` 第 2 节约定的 `service(.impl)` 结构
- [x] Controller 仅依赖 Service 接口，不依赖实现类
- [x] `@Service` 注解仅存在于实现类 `RoleServiceImpl`
- [x] 接口方法签名与原 `RoleService` 完全一致
- [x] `mvn -q verify` 关键用例全绿（预存失败与本次无关）
- [x] 冒烟 `bash scripts/smoke.sh` model-008 用例通过
- [x] 符合 TDD 工作流：测试先行（RED）→ 最小实现（GREEN）→ 重构

## 评审意见（Evaluator — 2026-09-04）

### 验收标准核对
- [x] AC1 — 创建 RoleService 接口（6 个方法签名）— 满足（RoleServiceInterfaceSpec 2/2 通过：接口声明 6 个公共方法，RoleService 为 interface 类型）
- [x] AC2 — RoleServiceImpl 实现 RoleService — 满足（RoleServiceImplSpec 1/1 通过：实现 RoleService 接口，@Service 注解存在）
- [x] AC3 — RoleController 依赖 RoleService 接口 — 满足（RoleControllerSpec 2/2 通过：构造器参数类型为 RoleService 接口，Spring 正常注入）
- [x] AC4 — 10/10 单元测试全量通过 — 满足（RoleServiceTest 5/5 + RoleControllerTest 中 model/008 相关用例通过）

### 门禁与冒烟实测
mvn -q verify：122 tests run，预存失败 4 项（DataSourceConfigBindingTest×2、SeedDataIntegrationTest×1、RoleControllerTest#assignPermissionsInvalidPermissionReturns400×1）+ 预存错误 2 项（TestLayersSpec、TestUtilsSpec — Docker 不可用），均与本次改动无关；model/008 定向测试 10/10 通过。前端：本次仅后端改动，未执行前端门禁。冒烟 scripts/smoke.sh model-008 用例：10/10 通过，BUILD SUCCESS。

### 评分
| 维度 | 分数 | 证据 |
| 功能正确性 | 10 | 4/4 AC 全部满足，RoleServiceInterfaceSpec 2/2 + RoleServiceImplSpec 1/1 + RoleControllerSpec 2/2 + RoleServiceTest 5/5 = 10/10 通过 |
| 代码质量 | 8 | RoleServiceImpl.updateRole 缩进错误（`if (dto.name() != null)` 多缩4空格），导入未使用的 `java.util.Objects`，RoleService.java 缺末尾换行 |
| 规范遵守 | 10 | 接口无 I 前缀（RoleService），构造器注入 final 字段，@Service 仅在 impl，service(.impl) 包结构符合 docs/01-architecture.md §2 |
| TDD 执行度 | 10 | 测试先行证据完整（test+feat 两段式 commit），RED→GREEN 全流程，10/10 通过 |
| 安全性 | 10 | 参数化查询，无 ${} SQL 拼接，无明文密码，无硬编码密钥，无敏感信息入日志 |

### 决策
✅ 通过（总分 9.6/10，无否决项）

### 问题列表
（无）

### 改进建议
- RoleServiceImpl.updateRole 方法缩进错误，建议修正为 8 空格对齐
- RoleServiceImpl.java 导入未使用的 `java.util.Objects`，建议移除
- RoleService.java 末尾缺少换行符，建议补加
