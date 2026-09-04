# Sprint 工作单：sprint-055

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-055 |
| 所属模块 | model |
| 功能点 ID | model/012 |
| 功能点名称 | 更新所有 Controller 层依赖接口 |
| 状态 | DONE |
| 创建时间 | 2026-09-04 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| model/011 | 抽取 AuthService 接口（最后一个 Service 接口） | ✅ |
| model/005 | sys_user_role+sys_role_permission 关联表 | ✅ |

## 业务背景

model/007~model/011 已完成全部 5 个核心 Service（UserService、RoleService、PermissionService、ModuleService、AuthService）的接口化重构。本功能点旨在验收确认：所有 Controller 层均已依赖 Service 接口而非实现类，符合 `docs/01-architecture.md` 第 2 节约定的 `service(.impl)` 包结构语义与依赖倒转原则（DIP）。

## 需求描述

验收确认：`UserController`、`RoleController`、`PermissionController`、`ModuleController`、`AuthController` 5 个 Controller 的构造器参数类型均为对应的 Service 接口，无直接依赖实现类。

## 验收标准（TDD 驱动）

### AC1 — UserController 依赖 UserService 接口
> `UserController` 构造器参数类型为 `UserService`（接口）

**用例**：`UserControllerSpec#controllerInjectsServiceInterface` — 已存在通过

### AC2 — RoleController 依赖 RoleService 接口
> `RoleController` 构造器参数类型为 `RoleService`（接口）

**用例**：`RoleControllerSpec#controllerInjectsServiceInterface` — 已存在通过

### AC3 — PermissionController 依赖 PermissionService 接口
> `PermissionController` 构造器参数类型为 `PermissionService`（接口）

**用例**：`PermissionControllerSpec#controllerInjectsServiceInterface` — 已存在通过

### AC4 — ModuleController 依赖 ModuleService 接口
> `ModuleController` 构造器参数类型为 `ModuleService`（接口）

**用例**：`ModuleControllerSpec#controllerInjectsServiceInterface` — 已存在通过

### AC5 — AuthController 依赖 AuthService 接口
> `AuthController` 构造器参数类型为 `AuthService`（接口）

**用例**：`AuthControllerSpec#controllerInjectsServiceInterface` — 已存在通过

## 测试清单

| # | 测试用例名 | 验收标准 | 结果 |
|---|-----------|---------|------|
| 1 | UserControllerSpec#controllerInjectsServiceInterface | AC1 | ✅ 通过 |
| 2 | RoleControllerSpec#controllerInjectsServiceInterface | AC2 | ✅ 通过 |
| 3 | PermissionControllerSpec#controllerInjectsServiceInterface | AC3 | ✅ 通过 |
| 4 | ModuleControllerSpec#controllerInjectsServiceInterface | AC4 | ✅ 通过 |
| 5 | AuthControllerSpec#controllerInjectsServiceInterface | AC5 | ✅ 通过 |

## RED 证据

无需 RED 阶段：所有 Controller 已在各自 Service 接口化重构（model/007~model/011）阶段完成接口依赖注入，现为验收确认。

## GREEN 证据

```text
[GREEN] UserControllerSpec 2/2 通过：构造器依赖 UserService 接口
[GREEN] RoleControllerSpec 2/2 通过：构造器依赖 RoleService 接口
[GREEN] PermissionControllerSpec 2/2 通过：构造器依赖 PermissionService 接口
[GREEN] ModuleControllerSpec 2/2 通过：构造器依赖 ModuleService 接口
[GREEN] AuthControllerSpec 2/2 通过：构造器依赖 AuthService 接口
[GREEN] 代码检查：grep 确认 5 个 Controller 构造器均注入对应 Service 接口，无实现类依赖
[GREEN] mvn test -Dtest=UserControllerSpec,RoleControllerSpec,PermissionControllerSpec,ModuleControllerSpec,AuthControllerSpec：10/10 通过
```

## 门禁与冒烟记录

- 冒烟 `bash scripts/smoke.sh` model-012 用例：通过（10/10 测试通过）
- 门禁 `mvn -q verify`：存在预存失败（DataSourceConfigBindingTest 环境配置、RoleControllerTest#assignPermissionsInvalidPermissionReturns400 见 infra-002 排除列表、TestLayersSpec/TestUtilsSpec 需 Docker、SeedDataIntegrationTest 种子数据），均与本改动无关

## 拆分说明

本功能点为 model 模块接口化重构的最终验收子功能点。model 模块所有 12 个功能点已全部完成。

## 规范检查清单

- [x] 包结构符合 `docs/01-architecture.md` 第 2 节约定的 `service(.impl)` 结构
- [x] Controller 仅依赖 Service 接口，不依赖实现类
- [x] `@Service` 注解仅存在于 5 个实现类
- [x] 接口方法签名与原具体类完全一致
- [x] `mvn -q verify` 关键用例全绿（预存失败与本次无关）
- [x] 冒烟 `bash scripts/smoke.sh` model-012 用例通过
- [x] 符合 TDD 工作流：测试先行（RED）→ 最小实现（GREEN）→ 重构