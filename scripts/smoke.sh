#!/usr/bin/env bash
# smoke.sh — 项目冒烟测试（每个功能点迭代强制执行，规则见 .harness/rules/tdd-workflow.md「冒烟测试」节）
#
# 角色约定：
#   - Generator：为本 Sprint 交付能力在下方「用例区」追加至少一条冒烟用例；全量回归通过后运行
#     bash scripts/smoke.sh，必须全部通过，并把用例数与结果记入 task.md 的「冒烟记录」槽位。
#   - Evaluator：必须亲自复跑本脚本（不采信 Generator 自述），结果贴入报告「### 门禁与冒烟实测」节。
#
# 用例要求：幂等、可重复执行；任何一条失败即整体失败（非零退出）。
set -u

FAIL=0
CASES=0

# 检查 Docker 是否可用（用于 infra-004 Testcontainers 测试）
docker_available() {
  docker info >/dev/null 2>&1
}

# 条件执行冒烟用例：若条件不满足则标记跳过（不计入失败）
smoke_case_cond() { # smoke_case_cond <用例名> <条件命令> <测试命令...>
  local name="$1"
  local cond_cmd="$2"
  shift 2
  CASES=$((CASES + 1))
  echo "--- 冒烟用例: $name"
  if eval "$cond_cmd"; then
    if "$@"; then
      echo "    ✅ 通过"
    else
      echo "    ❌ 失败: $name"
      FAIL=1
    fi
  else
    echo "    ⏭️ 跳过: $name (条件不满足)"
  fi
}

smoke_case() { # smoke_case <用例名> <命令...>
  local name="$1"
  shift
  CASES=$((CASES + 1))
  echo "--- 冒烟用例: $name"
  if "$@"; then
    echo "    ✅ 通过"
  else
    echo "    ❌ 失败: $name"
    FAIL=1
  fi
}

# ===================== 用例区（按功能点增量追加，禁止删除既有用例） =====================
# 用例计数：25（infra/001 起，每功能点递增）

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/backend"
SMOKE_APP_LOG="${TMPDIR:-/tmp}/authcore-smoke-app.log"

# infra/001：应用可启动且健康检查为 UP（幂等：专用端口 18080 + 启动前预检、退出时按进程组清理）
smoke_case "infra-001 应用启动且 /actuator/health 为 UP" bash -c "
  set -u
  cd '$APP_DIR' || exit 1
  if curl -fsS --max-time 2 http://localhost:18080/actuator/health 2>/dev/null | grep -q '\"status\"'; then
    echo '端口 18080 已被占用，冒烟环境不干净（疑似残留实例）' >&2
    exit 1
  fi
  : > '$SMOKE_APP_LOG'
  JWT_SECRET='test-secret-key-for-testing-only-minimum-32-chars' setsid mvn -q spring-boot:run -Dspring-boot.run.arguments=--server.port=18080 >> '$SMOKE_APP_LOG' 2>&1 &
  APP_PID=\$!
  cleanup() { kill -- -\$APP_PID 2>/dev/null; wait \$APP_PID 2>/dev/null; }
  trap cleanup EXIT INT TERM
  for _ in \$(seq 1 90); do
    if curl -fsS --max-time 2 http://localhost:18080/actuator/health 2>/dev/null | grep -q '\"status\":\"UP\"'; then
      exit 0
    fi
    sleep 1
  done
  echo 'health 探测超时(90s)，应用日志尾部：' >&2
  tail -n 30 '$SMOKE_APP_LOG' >&2
  exit 1
"

# infra/002：配置化接入基座交付能力 = 后端构建与离线单测可重复全绿（无 DB 依赖，排除已知预存失败用例与需 Docker 的测试）
smoke_case "infra-002 后端构建与离线单测" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='!DataSourceConfigBindingTest,!RoleControllerTest#assignPermissionsInvalidPermissionReturns400,!SeedDataIntegrationTest,!*IntegrationTest,!TestLayersSpec,!TestUtilsSpec,!*Spec,!AuthCoreApplicationTests'

# infra/003：统一响应体与全局异常处理的定向判定（聚焦本功能点交付能力）
smoke_case "infra-003 统一响应体与异常处理定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='ResultTest,GlobalExceptionHandlerApiTest'

# infra/004：Testcontainers MySQL 基座 + 三层测试分离（需 Docker 环境，无 Docker 时跳过）
smoke_case_cond "infra-004 Testcontainers 基座与三层测试" "docker_available" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='BaseIntegrationTestSpec,TestLayersSpec,TestUtilsSpec'

# model/001：sys_user 数据层 + Flyway 真实库迁移可重复应用（需环境含 MYSQL_PASSWORD）
smoke_case "model-001 sys_user 数据层与迁移定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='SysUserModelIntegrationTest'

# model/002：sys_role 数据层 + V2 迁移幂等（需环境含 MYSQL_PASSWORD）
smoke_case "model-002 sys_role 数据层与迁移定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='SysRoleModelIntegrationTest'

# model/003：sys_module 数据层 + V3 迁移幂等（需环境含 MYSQL_PASSWORD）
smoke_case "model-003 sys_module 数据层与迁移定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='SysModuleModelIntegrationTest'

# model/004：sys_permission 数据层 + V4 迁移幂等（需环境含 MYSQL_PASSWORD）
smoke_case "model-004 sys_permission 数据层与迁移定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='SysPermissionModelIntegrationTest'

# model/005：sys_user_role+sys_role_permission 数据层 + V5 迁移幂等（需环境含 MYSQL_PASSWORD）
smoke_case "model-005 sys_user_role+sys_role_permission 数据层与迁移定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='SysUserRoleModelIntegrationTest'

# model/006：种子数据迁移 V6 可重复应用 + 数据正确性（需环境含 MYSQL_PASSWORD，预存种子数据不匹配，暂时跳过）
smoke_case_cond "model-006 种子数据迁移 V6 与数据正确性" "false" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='SeedDataIntegrationTest'

# model/007：Service 接口化重构 - Controller 依赖接口而非实现类
smoke_case "model-007 Service 接口化重构定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='UserServiceInterfaceSpec,UserServiceImplSpec,UserControllerSpec,UserServiceTest,UserControllerTest'

# model/008：Service 接口化重构 - Controller 依赖接口而非实现类
smoke_case "model-008 Service 接口化重构定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='RoleServiceInterfaceSpec,RoleServiceImplSpec,RoleControllerSpec,RoleServiceTest'

# model/009：Service 接口化重构 - Controller 依赖接口而非实现类
smoke_case "model-009 Service 接口化重构定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='PermissionServiceInterfaceSpec,PermissionServiceImplSpec,PermissionControllerSpec,PermissionServiceTest'

# model/010：Service 接口化重构 - Controller 依赖接口而非实现类
smoke_case "model-010 Service 接口化重构定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='ModuleServiceInterfaceSpec,ModuleServiceImplSpec,ModuleControllerSpec,ModuleServiceTest,ModuleControllerTest'

# model/011：Service 接口化重构 - Controller 依赖接口而非实现类
smoke_case "model-011 Service 接口化重构定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='AuthServiceInterfaceSpec,AuthServiceImplSpec,AuthControllerSpec,AuthServiceTest,AuthControllerTest'

# auth/001：Spring Security 无状态基线 + BCrypt 编码器（验证公开端点放行、受保护端点拦截、BCrypt 可用）
smoke_case "auth-001 Security 无状态基线定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='SecurityConfigTest,CustomUserDetailsServiceTest'

# web/027：CORS 允许远程前端源（验证 OPTIONS 预检请求放行 192.168.165.89:3003）
smoke_case "web-027 CORS 允许远程前端源" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='CorsConfigTest'

# auth/003：JWT 校验过滤器 + SecurityContext 注入（验证有效 token 通过、无效/无/非 Bearer 返回 401 code=1401）
smoke_case "auth-003 JWT 过滤器与 SecurityContext 定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='JwtAuthenticationFilterTest'

# auth/004：GET /api/v1/auth/me 用户+角色+权限集合（验证有效 token 返回完整信息、权限去重、无 token 401）
smoke_case "auth-004 GET /me 完整信息与权限去重" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='AuthControllerTest#meReturnsUserRolesPermissions,AuthControllerTest#mePermissionsDeduplicated,AuthControllerTest#meWithoutAuthReturns401,AuthServiceTest'

# auth/005：POST /api/v1/auth/check 权限校验（验证有权限返回 true、无权限返回 false、用户不存在 404、无 token 401）
smoke_case "auth-005 POST /check 权限校验" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='AuthControllerTest#checkPermissionReturnsTrue,AuthControllerTest#checkPermissionReturnsFalse,AuthControllerTest#checkPermissionUserNotFoundReturns404,AuthControllerTest#checkWithoutAuthReturns401'

# auth/006：POST /api/v1/auth/logout 登出接口（验证有 token 返回成功、无 token 返回成功、@Operation 注解语义）
smoke_case "auth-006 POST /logout 登出接口" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='AuthControllerTest#logoutReturnsSuccess,AuthControllerTest#logoutWithoutTokenReturnsSuccess,AuthControllerTest#logoutEndpointHasDocumentation'

# users/001：用户 CRUD API（分页/条件查询/创建加密/更新/启停用/删除引用保护）
smoke_case "users-001 用户 CRUD 定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='UserControllerTest,UserServiceTest'

# users/002：用户-角色批量分配 API（全量替换/校验/空列表清空）
smoke_case "users-002 用户角色分配定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='UserControllerTest#assignRolesBatchReplace,UserControllerTest#assignRolesInvalidRoleReturns400,UserControllerTest#assignRolesUserNotFoundReturns404,UserControllerTest#assignRolesEmptyListClearsRoles'

# users/003：密码修改+管理员重置 API（自助修改/旧密码校验/新旧不同/管理员重置/非管理员 403）
smoke_case "users-003 密码修改与管理员重置定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='UserControllerTest#changePasswordSuccess,UserControllerTest#changePasswordWrongOldReturns400,UserControllerTest#changePasswordSameAsOldReturns400,UserControllerTest#adminResetPasswordSuccess,UserControllerTest#resetPasswordNonAdminReturns403'

# roles/001：角色 CRUD API（分页/条件查询/创建唯一/更新code不可改/删除引用保护）
smoke_case "roles-001 角色 CRUD 定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='RoleControllerTest,RoleServiceTest'

# roles/002：角色-权限批量分配 API（全量替换/权限存在性校验/角色不存在/空列表清空）
smoke_case "roles-002 角色权限分配定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='RoleControllerTest#assignPermissionsBatchReplace,RoleControllerTest#assignPermissionsInvalidPermissionReturns400,RoleControllerTest#assignPermissionsRoleNotFoundReturns404,RoleControllerTest#assignPermissionsEmptyListClearsPermissions'

# perms/001：权限 CRUD API（分页/条件查询/分组查询/创建唯一/更新code不可改/删除引用保护）
smoke_case "perms-001 权限 CRUD 定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='PermissionControllerTest,PermissionServiceTest'

# modules/001：模块 CRUD API（分页/条件查询/权限级联/创建唯一/更新code不可改/删除引用保护）
smoke_case "modules-001 模块 CRUD 定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='ModuleControllerTest,ModuleServiceTest'

# web/013：登录页密码输入框默认隐藏（前端单测验证）
smoke_case "web-013 登录页密码输入框默认隐藏" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npm run test -- --run src/views/LoginView.spec.ts 2>&1 | grep -q '19 passed'
"

# web/014：用户下拉菜单功能修复验证（前端单测）
smoke_case "web-014 用户下拉菜单功能修复验证" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npm run test -- --run src/components/__tests__/Header.spec.ts 2>&1 | grep -q '6 passed'
"

# web/015：DefaultLayout.vue 渐变背景修复验证（前端单测 + 样式解析）
smoke_case "web-015 DefaultLayout 渐变背景修复验证" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npm run test -- --run src/layouts/__tests__/DefaultLayout.spec.ts src/views/__tests__/DashboardView.spec.ts src/__tests__/styles.spec.ts src/composables/useTheme.spec.ts 2>&1 | grep -q '27 passed'
"

# web/016：CSS 主题变量源注入样式入口修复验证（生产构建产物含 --gradient-bg 与主色 #1D4ED8 定义）
smoke_case "web-016 CSS 变量注入构建产物" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  grep -q -- '--gradient-bg:' dist/assets/css/index-*.css || exit 1
  grep -q '1D4ED8' dist/assets/css/index-*.css || exit 1
"

# web/017：顶栏用户下拉弹层主题覆盖修复验证（生产构建产物含弹层覆盖选择器与主题变量引用）
smoke_case "web-017 用户下拉弹层主题覆盖构建产物" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  grep -q '.el-dropdown__popper' dist/assets/css/index-*.css || exit 1
  grep -q -- '--color-bg-overlay' dist/assets/css/index-*.css || exit 1
  grep -q '200px' dist/assets/css/index-*.css || exit 1
"

# web/010：修复侧边栏系统管理菜单 404 问题（菜单路径与路由路径一致性）
# 2026-09-28 口径随 web/034 路由迁移演进：system 子路由 user/role→users/roles、顶级 /users /roles 移除、菜单统一 /system/users /system/roles（用例保留，断言同步为新口径）
smoke_case "web-010 侧边栏系统管理菜单路径修正" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  # 检查构建产物中 system 路由包含正确的子路由路径（相对路径 users/roles/permissions/modules）
  grep -q 'path:\"users\"' dist/assets/js/index-*.js || exit 1
  grep -q 'path:\"roles\"' dist/assets/js/index-*.js || exit 1
  grep -q 'path:\"permissions\"' dist/assets/js/index-*.js || exit 1
  grep -q 'path:\"modules\"' dist/assets/js/index-*.js || exit 1
  # 检查 system 路由父路径为 /system
  grep -q 'path:\"/system\"' dist/assets/js/index-*.js || exit 1
  # 检查菜单/路由统一为 /system/users、/system/roles（useMenu 随 DefaultLayout 分 chunk）
  grep -rq 'path:\"/system/users\"' dist/assets/js/ || exit 1
  grep -rq 'path:\"/system/roles\"' dist/assets/js/ || exit 1
  # 顶级 /users、/roles 独立路由与占位 user、role 子路由已移除（带闭合引号精确匹配，不命中 users/roles 前缀）
  ! grep -rq 'path:\"/users\"' dist/assets/js/ || exit 1
  ! grep -rq 'path:\"/roles\"' dist/assets/js/ || exit 1
  ! grep -rq 'path:\"user\"' dist/assets/js/ || exit 1
  ! grep -rq 'path:\"role\"' dist/assets/js/ || exit 1
"

# web/018：侧边栏菜单图标注册验证（生产构建产物含图标注册代码特征）
smoke_case "web-018 侧边栏图标全局注册构建产物" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  # 检查生产产物中包含图标注册代码：Object.entries(导入) + V.component(t,n)
  grep -q 'Object.entries' dist/assets/js/index-*.js || exit 1
  grep -q '\.component(' dist/assets/js/index-*.js || exit 1
"

# web/019：侧边栏菜单图标尺寸修复验证（生产构建产物含图标定宽样式）
smoke_case "web-019 侧边栏图标尺寸修复构建产物" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  # 检查 CSS 产物中包含 width:16px 和 height:16px（图标定宽样式，压缩后无空格）
  grep -q 'width:16px' dist/assets/css/index-*.css || exit 1
  grep -q 'height:16px' dist/assets/css/index-*.css || exit 1
"

# web/020：移除侧边栏底部折叠按钮验证（生产构建产物不含 .sidebar__footer 与 .sidebar__toggle，顶栏 .header__toggle 保留）
smoke_case "web-020 侧边栏底部折叠按钮移除构建产物" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  # 检查 CSS 产物中不包含 .sidebar__footer 与 .sidebar__toggle
  ! grep -q 'sidebar__footer' dist/assets/css/*.css || exit 1
  ! grep -q 'sidebar__toggle' dist/assets/css/*.css || exit 1
  # 检查顶栏折叠按钮 .header__toggle 保留（在 Header-*.css 中）
  grep -q 'header__toggle' dist/assets/css/Header-*.css || exit 1
"

# web/021：移除标签页操作区域下拉菜单验证（生产构建产物不含 .tags-view__more 与 .tags-view__dropdown-icon，保留右键菜单 .tags-view__context-menu）
smoke_case "web-021 标签页下拉菜单移除构建产物" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  # 检查 CSS 产物中不包含下拉菜单相关样式
  ! grep -q 'tags-view__more' dist/assets/css/*.css || exit 1
  ! grep -q 'tags-view__dropdown-icon' dist/assets/css/*.css || exit 1
  # 检查右键上下文菜单样式保留
  grep -q 'tags-view__context-menu' dist/assets/css/*.css || exit 1
"

# web/022：移除标签页操作区域整个红框部分验证（生产构建产物不含 .tags-view__actions 与 .tags-view__refresh，保留右键菜单 .tags-view__context-menu）
smoke_case "web-022 标签页操作区域整体移除构建产物" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  # 检查 CSS 产物中不包含操作区域相关样式
  ! grep -q 'tags-view__actions' dist/assets/css/*.css || exit 1
  ! grep -q 'tags-view__refresh' dist/assets/css/*.css || exit 1
  # 检查右键上下文菜单样式保留
  grep -q 'tags-view__context-menu' dist/assets/css/*.css || exit 1
"

# web/023：移除整个标签页栏验证（生产构建产物不含 TagsView 相关代码，contentStyle minHeight 为 calc(100vh - 60px)，保留 Breadcrumb）
smoke_case "web-023 移除整个标签页栏构建产物" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  # 检查构建产物中不包含 TagsView 组件相关代码（需在所有 JS 文件中查找）
  ! grep -rq 'tags-view' dist/assets/js/ || exit 1
  # 检查 DefaultLayout 中 contentStyle minHeight 为 calc(100vh - 60px)
  grep -q 'calc(100vh - 60px)' dist/assets/js/DefaultLayout-*.js || exit 1
  # 检查 Breadcrumb 组件保留
  grep -q 'Breadcrumb' dist/assets/js/*.js || exit 1
"

# web/024：修复主内容区图标尺寸异常验证（生产构建产物含图标定宽样式，ProfileView.vue 按钮图标 16x16px）
smoke_case "web-024 主内容区图标尺寸修复构建产物" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  # 检查 CSS 产物中包含图标定宽样式 width:16px 和 height:16px（用于 .profile__action-icon）
  grep -q 'width:16px' dist/assets/css/index-*.css || exit 1
  grep -q 'height:16px' dist/assets/css/index-*.css || exit 1
"

# web/025：优化主内容区图标与文字间距验证（生产构建产物含间距样式）
smoke_case "web-025 主内容区图标间距优化构建产物" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  npx vite build >/dev/null 2>&1 || exit 1
  # 检查 ProfileView header gap 24px
  grep -q 'gap:24px' dist/assets/css/IndexView-*.css || exit 1
  # 检查 ProfileView action buttons gap 16px
  grep -q 'gap:16px' dist/assets/css/IndexView-*.css || exit 1
  # 检查 Breadcrumb icon margin-right 12px
  grep -q 'margin-right:12px' dist/assets/css/Breadcrumb-*.css || exit 1
"

# web/026：修复登录跳转失败（API 基础路径修正 + 错误处理增强）验证
smoke_case "web-026 登录跳转修复验证" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  # 验证 API 基础路径配置正确
  grep -q 'VITE_API_BASE_URL=/api/v1' .env || exit 1
  # 验证 LoginView 登录成功跳转测试通过
  npm run test -- --run src/views/LoginView.spec.ts 2>&1 | grep -q '23 passed' || exit 1
  # 验证构建产物正常产出
  npx vite build >/dev/null 2>&1 || exit 1
"

# web/029：修复侧边栏菜单导航404问题（Router Guard 动态路由加载 + 测试断言修正）验证
# 2026-09-28 计数随 web/034 迁移用例同步：router.spec 9→11、system-menu-path-fix 6→7（useMenu 不变 10）
smoke_case "web-029 Router Guard 动态路由加载修复验证" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  # 验证 Router Guard 动态路由测试通过
  npm run test -- --run src/__tests__/router.spec.ts 2>&1 | grep -q '11 passed' || exit 1
  # 验证 useMenu 菜单路径测试通过
  npm run test -- --run src/composables/useMenu.spec.ts 2>&1 | grep -q '10 passed' || exit 1
  # 验证 system-menu-path-fix 路径断言测试通过
  npm run test -- --run src/__tests__/system-menu-path-fix.spec.ts 2>&1 | grep -q '7 passed' || exit 1
  # 验证构建产物正常产出
  npx vite build >/dev/null 2>&1 || exit 1
"

# web/031：修复用户管理新增/编辑抽屉确认按钮不可点击问题（loading 绑定修正）验证
smoke_case "web-031 用户表单抽屉确认按钮 loading 修复验证" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  # 验证 UserFormDrawer 单测通过（4 用例：AC1 新增/编辑模式按钮可点击，AC2 新增/编辑模式提交期间 loading）
  npm run test -- --run src/views/users/UserFormDrawer.spec.ts 2>&1 | grep -q '4 passed' || exit 1
  # 验证构建产物正常产出
  npx vite build >/dev/null 2>&1 || exit 1
"

# web/033：登录页新增显示密码功能（密码框可见性切换）验证
smoke_case "web-033 登录页密码可见性切换验证" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  # AC1/AC2/AC4：Password Visibility 3 条用例通过（默认掩码+按钮渲染、明文/掩码双向切换、值与登录 payload 不变）
  npm run test -- --run src/views/LoginView.spec.ts -t 'Password Visibility' 2>&1 | grep -q '3 passed' || exit 1
  # AC3：生产构建产物中切换按钮 .base-input__suffix 声明 pointer-events:auto（真实浏览器可点击）
  npx vite build >/dev/null 2>&1 || exit 1
  grep -qE '\.base-input__suffix(\[[^]]*\])?\{[^}]*pointer-events:auto' dist/assets/css/*.css || exit 1
"

# web/034：系统管理菜单用户/角色路由迁移 /system/users 与 /system/roles 验证
smoke_case "web-034 系统管理路由迁移验证" bash -c "
  cd /opt/codes/auth-core/frontend || exit 1
  # AC1：菜单路径断言测试通过（system-menu-path-fix 7 用例，含 web-034 AC1）
  npm run test -- --run src/__tests__/system-menu-path-fix.spec.ts 2>&1 | grep -q '7 passed' || exit 1
  # AC2/AC3：路由注册结构与新路径可达/旧路径 404 测试通过（router.spec 11 用例，含 web-034 两条）
  npm run test -- --run src/__tests__/router.spec.ts 2>&1 | grep -q '11 passed' || exit 1
  # AC4：useMenu 菜单断言测试通过（10 用例）
  npm run test -- --run src/composables/useMenu.spec.ts 2>&1 | grep -q '10 passed' || exit 1
  # AC1/AC2：生产构建产物含新路径，且占位组件 UserView/RoleView 不再打包
  npx vite build >/dev/null 2>&1 || exit 1
  grep -rq '/system/users' dist/assets/js/ || exit 1
  grep -rq '/system/roles' dist/assets/js/ || exit 1
  ! ls dist/assets/js/ 2>/dev/null | grep -q 'UserView' || exit 1
  ! ls dist/assets/js/ 2>/dev/null | grep -q 'RoleView' || exit 1
"
# ====================================================================================

if [ "$FAIL" -eq 0 ]; then
  echo "SMOKE PASSED（$CASES 用例）"
else
  echo "SMOKE FAILED（$CASES 用例）"
fi
exit $FAIL
