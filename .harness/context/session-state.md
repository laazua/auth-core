# 会话状态
> 本文件是跨会话交接的唯一真相源。任何会话开始必须先读本文件，按「下一步动作」路由（规则见 `AGENTS.md`）。

## 当前阶段
- 状态: 🟢 sprint-049 评审通过（web/025 ✅），sprint-048 待评审（web/024），sprint-047 待评审（web/023）
- 当前模块: model
- 当前功能点: model/008 ✅（评审通过），model/007 ✅（评审通过）

## 下一步动作
  Planner：取新功能点 model/009（抽取 PermissionService 接口），见 task.md 拆分说明

## 挂起
- 2026-09-03: Planner(sprint-049) — 新增 web/025（主内容区图标间距优化），前置依赖 web/003/007/008/016/024 均 ✅，不阻塞 web/017/023 评审；web/011 待规划优先级不变。
- 2026-09-03: Planner(sprint-048) — 新增 web/024（主内容区图标尺寸异常），前置依赖 web/003/007/008/016 均 ✅，不阻塞 web/017/023 评审；web/011 待规划优先级不变。
- 2026-09-02: Planner(sprint-041) — 台账盘点仍待决：web/010（sprint-035）评审并入账未完成，registry 行仍 🔄、阶段总览计数已按实际行修正；web/011 预存红项治理优先级维持。本次新增注册 web/017 不依赖二者。
- 2026-09-02: Planner(sprint-041) — 浏览器实测基线（playwright 登录 admin/admin123456）：用户下拉菜单高 419.7px、项行高 78~106px 不一（EP 图标 svg 无显式尺寸被 flex 拉伸 68~96px）；弹层 EP 默认白底/4px 圆角；暗色下弹层白底近白字不可读。基线截图 /tmp/dropdown-light.png、/tmp/dropdown-dark.png，修复后由 Generator/Evaluator 同法复测对比。
- 2026-09-02: Generator(sprint-040) — 门禁/冒烟预存失败登记（与本改动零关联，stash 对照实证）：① 前端 test 14 预存失败（LoginView.spec.ts 1 + system/IndexView.spec.ts 13，Element Plus 组件解析缺失）；② npm run lint 全量卡死/分片含预存错误；③ npm run build 350 预存 TS 错误（32 文件，属 web/011 范围）；④ 冒烟 roles-001/roles-002（RoleControllerTest#assignPermissionsInvalidPermissionReturns400 expected 400 but was 409，同 infra-002 排除列表）/web-013（LoginView 预存失败致 '19 passed' 不中）。均建议由 web/011「修复前端构建错误与失败测试」统一处理，Evaluator 评审时可对照裁决。web/017（sprint-041）继续沿用此对照口径。
- 2026-09-02: Planner(sprint-040) — web/015（sprint-039）评审并入 sprint-040 验收面：web/015 仅补 DefaultLayout.vue 渐变 var() 引用不达根因（variables.scss 未注入入口），其改动保留于工作树，由 Evaluator 在 sprint-040 交付后一并验收 web/015+web/016，不再单独评审 sprint-039。
- 2026-08-26: Planner(sprint-004) — 用户裁决：暂沿用旧口令 abc123456 不轮换（风险自担）；已注入仓库外 ~/.bashrc 与 ~/.bash_profile（export MYSQL_PASSWORD），git 历史清理事项仍待裁决。真实库 192.168.165.88:3306（MySQL 8.0.45，库 authcore 已就绪）自本 Sprint 起用于验收。
- 2026-08-26: Evaluator(sprint-002) — 真实口令 abc123456 已随 aa4b59b 入库（git 历史）：是否清理历史由用户裁决；强烈建议该口令立即轮换。另：用户 MySQL 实例已就绪（192.168.165.88），model/001 起可合并验证 DB 连通 + Flyway 迁移应用。
- 2026-08-26: Planner(sprint-002) — 用户裁决：本机不使用 Docker/Testcontainers，数据库走配置化接入（application.yml + 环境变量），真实连通与 Flyway 迁移应用验证延后至 MySQL 实例就绪；架构 §1「测试基座 Testcontainers MySQL」口径据此调整登记。影响：① infra/004（Testcontainers 基座）定义待环境就绪后由 Planner 重新规划；② DB 类冒烟用例（Flyway 迁移应用，tdd-workflow 冒烟节 model 形态）延后补入，infra/002 冒烟以「构建+全量测试」形态替代。
- 2026-08-26: Generator(sprint-002) — 新增配置开关语义备忘（非阻塞）：实例接入后须置 FLYWAY_ENABLED=true、DB_HEALTH_ENABLED=true 恢复完整生产语义；届时由 Planner 视需要登记专项验证任务。
- 2026-08-26: Generator(sprint-001) — 前端门禁暂缓适用：`frontend/` 目录属 web/001 范围尚未创建，infra 阶段仅后端可验证；非架构冲突，自 web/001 交付起恢复「后端+前端」双门禁口径。

## 最近更新
- 2026-09-04: Evaluator — pass sprint-051（model/008，平均分 9.6/10）
- 2026-09-04: Generator — done model/008（Service 接口化重构，门禁+冒烟通过；创建 RoleService 接口 + RoleServiceImpl 实现类，Controller 依赖接口，10/10 测试通过）
- 2026-09-04: Planner — kickoff sprint-051（model/008 抽取 RoleService 接口，4 条验收标准）
- 2026-09-04: Evaluator — pass sprint-050（model/007，平均分 9.8/10）
- 2026-09-03: Planner — kickoff sprint-049（web/025，4 条验收标准）
- 2026-09-03: Planner — kickoff sprint-048（web/024，3 条验收标准）
- 2026-08-31: Generator — done sprint-032（web/008，UI现代化优化+白天/晚上模式切换，前端测试 44/44 通过）
- 2026-08-28: Evaluator — pass sprint-031（integration/002，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-030（integration/001，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-029（web/007，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-028（web/006，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-027（web/005，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-026（web/004，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-025（web/003，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-024（web/002，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-023（web/001，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-022（modules/001，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-021（perms/001，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-020（roles/002，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-019（roles/001，平均分 10.0/10）
- 2026-08-28: Evaluator — pass sprint-018（users/003，平均分 10.0/10）
- 2026-08-27: Evaluator — pass sprint-017（users/002，平均分 10.0/10）
- 2026-08-27: Evaluator — pass sprint-016（users/001，平均分 10.0/10）
- 2026-08-27: Evaluator — pass sprint-015（auth/006，平均分 10.0/10）
- 2026-08-27: Evaluator — pass sprint-014（auth/005，平均分 10.0/10）
- 2026-08-27: Evaluator — pass sprint-013（auth/004，平均分 10.0/10）
- 2026-08-27: Evaluator — pass sprint-012（auth/003，平均分 10.0/10）
- 2026-08-27: Evaluator — pass sprint-011（auth/002，平均分 10.0/10）
- 2026-08-27: Evaluator — pass sprint-010（auth/001，平均分 10.0/10）
- 2026-08-27: Evaluator — pass sprint-009（model/006，平均分 10.0/10）
- 2026-08-27: Evaluator — pass sprint-008（model/005，平均分 10.0/10）
- 2026-08-26: Generator — done sprint-008（model/005，后端门禁 mvn -q verify 全绿 28 用例 + 冒烟 8 用例通过，真实库验收）
- 2026-08-26: Planner — kickoff sprint-008（model/005，4 条验收标准）
- 2026-08-26: 系统 — Harness 增补强制冒烟机制（smoke.sh 接线六处流程，registry 状态不受影响仍全 ⬜）
- 2026-08-26: 系统 — Harness 交付，registry 32 个功能点全部 ⬜