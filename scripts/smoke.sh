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
# 用例计数：8（infra/001 起，每功能点递增）

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

# infra/002：配置化接入基座交付能力 = 后端构建与全量测试可重复全绿（离线、无 DB 依赖）
smoke_case "infra-002 后端构建与全量测试" mvn -q -f "$APP_DIR/pom.xml" verify

# infra/003：统一响应体与全局异常处理的定向判定（聚焦本功能点交付能力）
smoke_case "infra-003 统一响应体与异常处理定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='ResultTest,GlobalExceptionHandlerApiTest'

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

# model/006：种子数据迁移 V6 可重复应用 + 数据正确性（需环境含 MYSQL_PASSWORD）
smoke_case "model-006 种子数据迁移 V6 与数据正确性" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='SeedDataIntegrationTest'

# auth/001：Spring Security 无状态基线 + BCrypt 编码器（验证公开端点放行、受保护端点拦截、BCrypt 可用）
smoke_case "auth-001 Security 无状态基线定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='SecurityConfigTest,CustomUserDetailsServiceTest'

# auth/003：JWT 校验过滤器 + SecurityContext 注入（验证有效 token 通过、无效/无/非 Bearer 返回 401 code=1401）
smoke_case "auth-003 JWT 过滤器与 SecurityContext 定向测试" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='JwtAuthenticationFilterTest'

# auth/004：GET /api/v1/auth/me 用户+角色+权限集合（验证有效 token 返回完整信息、权限去重、无 token 401）
smoke_case "auth-004 GET /me 完整信息与权限去重" mvn -q -f "$APP_DIR/pom.xml" test -Dtest='AuthControllerTest#meReturnsUserRolesPermissions,AuthControllerTest#mePermissionsDeduplicated,AuthControllerTest#meWithoutAuthReturns401,AuthServiceTest'
# ====================================================================================

if [ "$FAIL" -eq 0 ]; then
  echo "SMOKE PASSED（$CASES 用例）"
else
  echo "SMOKE FAILED（$CASES 用例）"
fi
exit $FAIL
