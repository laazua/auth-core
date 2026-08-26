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

smoke_case() { # smoke_case <用例名> <命令...>
  local name="$1"
  shift
  echo "--- 冒烟用例: $name"
  if "$@"; then
    echo "    ✅ 通过"
  else
    echo "    ❌ 失败: $name"
    FAIL=1
  fi
}

# ===================== 用例区（按功能点增量追加，禁止删除既有用例） =====================
# 当前状态：骨架阶段，尚无用例。infra/001 交付时必须追加第一条用例并保持后续每个功能点递增。
#
# 参考形态（按阶段启用/扩展；最终全链路形态对齐 registry integration/002）：
#   infra 阶段：
#     smoke_case "应用启动+health" bash -c 'curl -fsS http://localhost:8080/actuator/health | grep -q "\"status\":\"UP\""'
#   model 阶段：
#     smoke_case "Flyway 空库迁移可重复应用" mvn -q -f backend/pom.xml flyway:migrate flyway:validate
#   auth/API 阶段：
#     smoke_case "登录→me 链路" bash -c 'TOKEN=$(curl -fsS -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"<种子密码>\"}" | sed -n "s/.*\"token\":\"\([^\"]*\)\".*/\1/p") && curl -fsS http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN" >/dev/null'
#   web 阶段：
#     smoke_case "前端构建产物存在" test -d frontend/dist
# ====================================================================================

if [ "$FAIL" -eq 0 ]; then
  echo "SMOKE PASSED（骨架阶段 0 用例属正常；首个功能点起必须有递增用例）"
else
  echo "SMOKE FAILED"
fi
exit $FAIL
