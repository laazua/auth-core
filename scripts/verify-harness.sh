#!/usr/bin/env bash
# verify-harness.sh — harness 交付完整性校验（spec §8 DoD 的可执行形式）
set -u
cd "$(dirname "$0")/.."
FAIL=0
ok()  { echo "✅ $1"; }
bad() { echo "❌ $1"; FAIL=1; }
has() { grep -qF -- "$2" "$1" 2>/dev/null; }

REQUIRED_FILES=(
  AGENTS.md .harness/README.md
  .harness/planner.md .harness/generator.md .harness/evaluator.md
  .harness/context/task.md .harness/context/session-state.md .harness/context/iteration-log.md
  .harness/modules/registry.md
  .harness/rules/coding-standards.md .harness/rules/review-criteria.md .harness/rules/tdd-workflow.md
  docs/01-architecture.md
)

echo "== 1. 文件存在性 =="
for f in "${REQUIRED_FILES[@]}"; do
  [ -f "$f" ] && ok "存在 $f" || bad "缺失 $f"
done

echo "== 2. Prompt 章节完整性 =="
for p in .harness/planner.md .harness/generator.md .harness/evaluator.md; do
  for sec in "角色定位" "输入源" "行为流程" "约束" "输出要求"; do
    has "$p" "## $sec" && ok "$p 含「$sec」" || bad "$p 缺「$sec」"
  done
done

echo "== 3. Registry 完整性 =="
REG=.harness/modules/registry.md
if [ -f "$REG" ]; then
  ids_defined=$(grep -ohE '^\| *(infra|model|auth|users|roles|perms|modules|web|integration)/[0-9]{3}' "$REG" | tr -d '| ' | sort -u)
  count=$(printf '%s\n' "$ids_defined" | grep -c . )
  [ "$count" -eq 32 ] && ok "功能点计数 = 32" || bad "功能点计数 = $count（期望 32）"
  pending=$(grep -cE '^\| *(infra|model|auth|users|roles|perms|modules|web|integration)/[0-9]{3}.*⬜' "$REG")
  [ "$pending" -eq 32 ] && ok "初始状态全 ⬜" || bad "⬜ 行数 = $pending（期望 32）"
  deps_raw=$(awk -F'|' '/^\| *(infra|model|auth|users|roles|perms|modules|web|integration)\/[0-9]{3}/{print $4}' "$REG")
  orphan=0
  while IFS= read -r dep_cell; do
    [ -z "$dep_cell" ] && continue
    dep_cell=${dep_cell//、/,}
    IFS=',' read -ra tokens <<< "$dep_cell"
    for t in "${tokens[@]}"; do
      t="${t#"${t%%[![:space:]]*}"}"; t="${t%"${t##*[![:space:]]}"}"
      [ -z "$t" ] && continue
      if [ "$t" != "—" ] && ! [[ "$t" =~ ^(infra|model|auth|users|roles|perms|modules|web|integration)/[0-9]{3}$ ]]; then
        bad "畸形依赖 token: $t"
        continue
      fi
      [ "$t" = "—" ] && continue
      printf '%s\n' "$ids_defined" | grep -qx "$t" || { bad "依赖未定义：$t"; orphan=1; }
    done
  done <<< "$deps_raw"
  [ "$orphan" -eq 0 ] && ok "所有前置依赖 ID 均已定义且格式合法"
else
  bad "缺失 $REG"
fi

echo "== 4. task.md 模板字段 =="
TASK=.harness/context/task.md
for field in "Sprint ID" "所属模块" "需求描述" "业务背景" "前置依赖" "验收标准" "测试清单" "RED 证据" "实现说明" "状态" "评审意见"; do
  has "$TASK" "$field" && ok "task.md 含「$field」" || bad "task.md 缺「$field」"
done

echo "== 4.5. 路由结构完整性 =="
SS=.harness/context/session-state.md
has "$SS" "## 下一步动作" && ok "session-state 含「## 下一步动作」" || bad "session-state 缺「## 下一步动作」"
has "$SS" "## 挂起" && ok "session-state 含「## 挂起」" || bad "session-state 缺「## 挂起」"
has AGENTS.md ".harness/planner.md" && has AGENTS.md ".harness/generator.md" && has AGENTS.md ".harness/evaluator.md" \
  && ok "AGENTS.md 三角色路由齐全（planner/generator/evaluator）" || bad "AGENTS.md 缺三角色路由之一"
has .harness/rules/review-criteria.md "一票否决" && has .harness/rules/review-criteria.md "平均分" \
  && ok "review-criteria 含「一票否决」与「平均分」评分口径" || bad "review-criteria 缺「一票否决」或「平均分」"

echo "== 5. 入口锚点与驱动手册 =="
has AGENTS.md ".harness/context/session-state.md" && ok "AGENTS.md 锚点指向 session-state" || bad "AGENTS.md 缺锚点"
for kw in "planner.md" "generator.md" "evaluator.md" "ANTHROPIC_BASE_URL=https://api.deepseek.com/anthropic" "ANTHROPIC_AUTH_TOKEN" "ANTHROPIC_MODEL=deepseek-chat"; do
  has .harness/README.md "$kw" && ok "README 含「$kw」" || bad "README 缺「$kw」"
done

echo "== 6. 引用路径死链检测 =="
refs=$(cat .harness/*.md .harness/context/*.md .harness/modules/*.md .harness/rules/*.md AGENTS.md docs/01-architecture.md 2>/dev/null \
  | grep -ohE '`(\.harness/[A-Za-z0-9_./-]+|docs/01-architecture\.md|AGENTS\.md|scripts/verify-harness\.sh)`' \
  | tr -d '`' | sort -u)
while IFS= read -r r; do
  [ -z "$r" ] && continue
  if [ -e "$r" ]; then ok "引用有效 $r"; else bad "死链 $r"; fi
done <<< "$refs"

echo ""
if [ "$FAIL" -eq 0 ]; then echo "ALL CHECKS PASSED"; else echo "CHECKS FAILED"; fi
exit $FAIL
