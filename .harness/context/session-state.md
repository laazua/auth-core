# 会话状态
> 本文件是跨会话交接的唯一真相源。任何会话开始必须先读本文件，按「下一步动作」路由（规则见 `AGENTS.md`）。

## 当前阶段
- 状态: 🟢 sprint-064 评审通过（web/033 ✅），sprint-063 实现完成待评审（web/032），sprint-062 实现完成待评审（web/031），sprint-061 待评审（web/030），sprint-060 待评审（web/029），sprint-049 评审通过（web/025 ✅），sprint-048 待评审（web/024），sprint-047 待评审（web/023），sprint-057 评审通过（web/026 ✅），sprint-059 评审通过（web/027 ✅）
- 当前模块: web
- 当前功能点: web/033 ✅（已完成，sprint-064），web/011（PLANNED），web/011a 🔄（规划中），web/017 🔄（规划中），web/023 🔄（规划中），web/026 ✅，web/027 ✅，web/029 ✅，web/030 ✅（待评审），web/031 ✅（待评审），web/032 ✅（待评审）

## 下一步动作
  Planner：取下一功能点。建议优先级：① 清理 Evaluator 积压（sprint-047/048/060/061/062/063 六项待评审，见挂起区）；② 新功能点建议 web/011a（修复前端 TS 编译错误，前置 web/010 ✅，可一并治理预存 lint 卡死/冒烟过期用例）

## 挂起
- 2026-09-28: Generator(sprint-064) — 用户裁决「基线对照推进」（问答记录）：基线冒烟/后端门禁预存红项与 Generator 约束 5 冲突，用户裁定沿 sprint-040 先例登记预存红项、照常提交并置 AWAITING_REVIEW 交 Evaluator 对照裁决。基线红项清单（实现前实测）：① `mvn -q verify` 130 用例 6 失败＝TestLayersSpec/TestUtilsSpec（无 Docker）、SeedDataIntegrationTest（真实库 sys_role 22≠2）、DataSourceConfigBindingTest ×2（环境变量绑定）、RoleControllerTest 409（已登记）；② 冒烟 45 用例 10 失败＝infra-001（健康探测 90s 超时）、model-008/010（RoleService/ModuleServiceInterfaceSpec 方法数 expected 6 was 7）、roles-001/002（409 已登记）、web-013/026（grep '19/23 passed' 计数过期，实测 22 passed）、web-020/021/022（构建产物 grep 过期，web/022 已废弃⚠️）；③ 前端全量测试 20 失败＝既有基线（LoginView 2、DefaultLayout 1、TagsView 4、auth-token 2、system/IndexView 11，合计 20，与实测逐条一致）。均与 web/033 零关联（零后端改动）。另登记本次规划/实现两处口径修订：AC4「全量 0 failed」→「零新增失败」（基线 2 failed 属 web/011）；AC3 产物路径 `index-*.css`→`dist/assets/css/*.css`（样式按组件分 chunk，实测 BaseInput-*.css）。
- 2026-09-28: Planner(sprint-064) — 用户指令「以 planner 开始依次执行」，本会话按 Planner→Generator→Evaluator 顺序推进 web/033；Evaluator 积压不变：sprint-047（web/023）、sprint-048（web/024）、sprint-060（web/029）、sprint-061（web/030）、sprint-062（web/031）、sprint-063（web/032）均待评审。
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
- 2026-09-28: Evaluator — pass sprint-064（web/033，平均分 8.9/10，基线对照豁免否决项 6）
- 2026-09-28: Generator — done sprint-064（web/033 登录页新增显示密码功能，3 用例测试先行 RED→GREEN，lint/build 通过，冒烟 web-033 通过，预存红项基线对照登记）
- 2026-09-28: Planner — kickoff sprint-064（web/033 登录页新增显示密码功能，4 条验收标准，前置依赖 web/002/web/013 均 ✅）
- 2026-09-10: Generator — done sprint-063（web/032 修复 SPA 路由刷新 404 问题，vite.config.ts historyApiFallback + nginx.conf try_files，前端构建通过，核心路由测试 45/45 通过，冒烟通过）
- 2026-09-10: Planner — kickoff sprint-063（web/032 修复 SPA 路由刷新 404 问题，3 条验收标准）
- 2026-09-09: Planner — kickoff sprint-XXX（web/028 修复登录后跳转 Dashboard 失败，1 条验收标准）
- 2026-09-04: Planner — kickoff sprint-056（web/011 修复前端构建错误与失败测试，web/011a 修复 TS 编译错误为首子功能点，前置依赖 web/010 ✅）
- 2026-09-04: Planner — kickoff sprint-055（model/012 更新所有 Controller 层依赖接口，验收确认，5 条验收标准全满足，model 模块全 12 点完成）
- 2026-09-04: Evaluator — pass sprint-054（model/011，平均分 9.8/10）
- 2026-09-04: Generator — done model/011（Service 接口化重构，门禁+冒烟通过；创建 AuthService 接口 + AuthServiceImpl 实现类，Controller 依赖接口，20/20 测试通过）
- 2026-09-04: Planner — kickoff sprint-054（model/011 抽取 AuthService 接口，4 条验收标准）
- 2026-09-04: Evaluator — pass sprint-053（model/010，平均分 9.8/10）
- 2026-09-04: Generator — done model/010（Service 接口化重构，门禁+冒烟通过；创建 ModuleService 接口 + ModuleServiceImpl 实现类，Controller 依赖接口，17/17 测试通过）
- 2026-09-04: Planner — kickoff sprint-053（model/010 抽取 ModuleService 接口，4 条验收标准）
- 2026-09-04: Evaluator — pass sprint-052（model/009，平均分 9.8/10）
- 2026-09-04: Generator — done model/009（Service 接口化重构，门禁+冒烟通过；创建 PermissionService 接口 + PermissionServiceImpl 实现类，Controller 依赖接口，11/11 测试通过）
- 2026-09-04: Planner — kickoff sprint-052（model/009 抽取 PermissionService 接口，4 条验收标准）
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