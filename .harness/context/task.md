# Sprint 工作单：sprint-028

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-028 |
| 所属模块 | web |
| 功能点 ID | web/006 |
| 功能点名称 | 权限管理页+模块管理页 |
| 状态 | PLANNED |
| 创建时间 | 2026-08-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| web/003 | 主布局+动态菜单渲染 | ✅ |
| perms/001 | 权限 CRUD API | ✅ |
| modules/001 | 模块 CRUD API | ✅ |

## 业务背景

权限管理页和模块管理页是 RBAC 系统的基础配置页面。权限管理提供权限的增删改查、按模块分组查询；模块管理提供模块的增删改查、级联权限查询、删除引用保护。两者共用同一页面结构，通过标签页切换。

## 需求描述

实现权限管理页与模块管理页（标签页切换）：

### 权限管理标签页 (`/permissions`)

1. **权限列表**：
   - 工具栏：搜索框（权限名/编码）、模块筛选、新增按钮
   - 表格：权限名、编码、所属模块、描述、操作列
   - 分页、搜索/筛选、Loading、空状态

2. **新增/编辑权限抽屉**：
   - 表单：权限名、编码、所属模块（下拉选模块）、描述
   - 表单验证：权限名/编码唯一、模块必选、编码格式（如 perm:*）
   - 编辑时编码禁用

3. **操作**：
   - 编辑：回填数据
   - 删除：确认弹窗 → 删除（引用保护：sys_role_permission 存在则拒绝，code=1203）

### 模块管理标签页 (`/modules`)

1. **模块列表**：
   - 工具栏：搜索框（模块名/编码）、状态筛选、新增按钮
   - 表格：模块名、编码、基础URL、描述、状态、操作列
   - 分页、搜索/筛选、Loading、空状态

2. **新增/编辑模块抽屉**：
   - 表单：模块名、编码、基础URL、描述、状态
   - 表单验证：模块名/编码唯一、编码格式
   - 编辑时编码禁用

3. **操作**：
   - 编辑：回填数据
   - 启停用：切换状态
   - 级联查询权限：点击查看 → 跳转权限标签页并筛选该模块
   - 删除：确认弹窗 → 删除（引用保护：sys_permission 存在则拒绝，code=1303）

3. **标签页切换**：顶部 Tab 切换权限/模块管理，状态独立保持

## 交付物

1. `src/views/system/IndexView.vue` — 权限/模块管理页（标签页切换）
2. `src/views/system/PermissionFormDrawer.vue` — 权限新增/编辑抽屉
3. `src/views/system/ModuleFormDrawer.vue` — 模块新增/编辑抽屉
4. 更新 `src/api/permission.ts`、`src/api/module.ts` — 复用 API
5. 更新 `src/router/routes.ts` — 添加 `/system/permissions`、`/system/modules` 路由
5. 测试用例

## 验收标准（TDD 驱动）

### AC1 — 权限列表分页查询与多条件筛选
> 访问 `/system/permissions`，支持权限名/编码搜索、模块筛选、分页切换正常。

**用例**：`IndexView.spec.ts#queryPermissionsPaginationAndFilters`
- 搜索关键词 → 列表筛选正确
- 模块筛选 → 列表筛选正确
- 翻页 → 页码、每页条数切换正常

### AC2 — 权限创建唯一校验 + 模块必选
> POST 权限 {name, code, module_id} 返回 200，重复 name/code 返回 409 code=1201/1202，缺 module_id 返回 400。

**用例**：`IndexView.spec.ts#createPermissionUniqueAndModuleRequired`
- 操作：POST {name:"新权限", code:"perm:new", module_id:1}
- 断言：status=200、id 非空、重复 name 返回 409 code=1201、重复 code 返回 409 code=1202、无 module_id 返回 400

### AC3 — 权限更新 code 不可改
> PUT 权限 {name, description, module_id} 返回 200，code 不变。

**用例**：`IndexView.spec.ts#updatePermissionCodeImmutable`
- 操作：PUT {name:"新权限名", module_id:2}
- 断言：code 与原值一致、code 输入框禁用

### AC4 — 权限删除引用保护
> DELETE 权限若 sys_role_permission 存在则 409 code=1203，否则 200 物理删除。

**用例**：`IndexView.spec.ts#deletePermissionWithReferenceProtection`
- 操作：创建权限并分配给角色 → DELETE → 409 code=1203
- 断言：无引用时删除成功、有引用时被拒

### AC5 — 模块列表分页查询与条件筛选
> 访问 `/system/modules`，支持模块名/编码搜索、状态筛选，分页切换正常。

**用例**：`IndexView.spec.ts#queryModulesPaginationAndFilters`
- 搜索关键词 → 列表筛选正确
- 状态筛选 → 列表筛选正确
- 翻页 → 页码、每页条数切换正常

### AC6 — 模块创建唯一校验
> POST 模块 {name, code, base_url, description, status} 返回 200，重复 name/code 返回 409 code=1301/1302。

**用例**：`IndexView.spec.ts#createModuleUniqueNameAndCode`
- 操作：POST {name:"新模块", code:"NEW_MOD", base_url:"http://new", status:1}
- 断言：status=200、id 非空、重复 name 返回 409 code=1301、同 code 返回 409 code=1302

### AC7 — 模块更新 code 不可改 + 级联查询权限
> PUT 模块 {name, base_url, description, status} 返回 200，code 不变；点击查看权限 → 跳转权限标签页并自动筛选该模块。

**用例**：`IndexView.spec.ts#updateModuleCodeImmutableAndCascadeQuery`
- 操作：PUT {name:"新模块名", status:0}
- 断言：code 不变、点击查看权限 → 跳转权限标签页并自动筛选 module_id

### AC8 — 模块启停用、删除引用保护
> 启停用切换生效、删除引用保护（sys_permission 存在则 409 code=1303）。

**用例**：`IndexView.spec.ts#statusToggleDeleteReferenceProtection`
- 点击启用/停用 → 状态切换、后端同步
- 创建模块并创建权限 → DELETE → 409 code=1303
- 断言：无引用时删除成功、有引用时被拒

### AC9 — 标签页切换状态保持
> 在权限/模块标签页切换，各自的搜索/筛选/分页状态独立保持。

**用例**：`IndexView.spec.ts#tabSwitchStatePreserved`
- 权限标签页搜索 "user"、翻到第 2 页 → 切换到模块标签页 → 切回权限标签页 → 搜索词、页码保持

## 规范检查清单（Evaluator 逐项核对）

- [ ] 页面结构：标签页切换、工具栏、表格、分页、抽屉/弹窗完整
- [ ] 表格：列定义、排序、选择、操作列
- [ ] 表单验证：必填、格式、唯一、长度、即时反馈
- [ ] 抽屉/弹窗：打开/关闭、表单重置、数据回填、提交 Loading
- [ ] 权限控制：仅管理员可见新增/编辑/删除按钮
- [ ] API 对接：复用 perms/001 + modules/001 API、错误码映射
- [ ] 状态管理：复用 authStore 权限判断
- [ ] 响应式：抽屉移动端全屏、表格横向滚动
- [ ] 跨标签页交互：模块查看权限跳转、标签页状态保持
- [ ] 测试覆盖：权限/模块列表查询、创建唯一、更新 code 不变、删除引用保护、标签页切换
- [ ] `npm run lint && npm run test && npm run build` 全绿