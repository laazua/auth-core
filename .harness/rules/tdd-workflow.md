# TDD 工作流（测试先行是硬约束）

> 每个功能点必须先有失败测试、再有实现；RED 证据链是 Evaluator 验收的硬性依据。

## 测试金字塔
| 层 | 工具 | 要求 |
|----|------|------|
| 单元 | JUnit5 + Mockito | service 层业务分支与边界全覆盖，外部依赖全 mock |
| Web 层 | MockMvc(@WebMvcTest 或 @SpringBootTest+MockMvc) | 每个接口：正常/参数非法/未认证/无权限 四类用例 |
| 集成 | @SpringBootTest + Testcontainers MySQL | Mapper 与迁移脚本正确性、涉及关联表的业务流 |
| 前端 | Vitest + Vue Test Utils | store/composable 必测；登录守卫、权限菜单等关键交互必测 |

## RED → GREEN → REFACTOR（每个功能点必经）
1. 从 task.md 验收标准导出【测试清单】写入 task.md
2. 先写测试；运行确认失败（RED），将关键失败输出摘录到 task.md「RED 证据」槽位
3. 最小实现使测试转绿（GREEN）——只补到用例通过为止，不提前扩展功能
4. 重构消除坏味道，保持全绿——只改结构不改行为，重构后必须重跑全量测试
5. 全量回归：后端 mvn -q verify 全绿；前端 npm run lint && npm run test && npm run build 通过

## 证据与提交
- RED 证据格式（记入 task.md）：
  ```text
  [RED] Tests run: X, Failures: Y — com.authcore.xxx.XxxTest.test行为: expected:<...> but was:<...>
  ```
- 提交推荐两段式：先 `test: <功能点>-测试先行(RED)`，后 `feat: <功能点>-最小实现(GREEN)`——commit 历史即 TDD 循环的物证
- 单 commit 交付时，task.md 的 RED 证据为唯一先行证明，缺失即触发 Evaluator 一票否决

## 冒烟测试（每个功能点必过）
冒烟是对已交付能力的最小端到端可用性验证，与单元/集成测试互补；功能点交付前必须通过，未执行或不通过即一票否决（见 `.harness/rules/review-criteria.md`）。
- 脚本：仓库根目录 `scripts/smoke.sh`（骨架已内置），随功能点增量维护——禁止删除既有用例
- 增量约定：Generator 必须为本次交付能力追加至少一条冒烟用例；用例幂等可重复，任何失败即非零退出
- 阶段形态参考：

  | 阶段 | 冒烟形态 |
  |------|---------|
  | infra | 应用可启动 + actuator health UP |
  | model | Flyway 迁移在空库可重复应用成功 |
  | auth/API | curl 登录→me→check 及对应资源 CRUD 最小链路 |
  | web | 生产构建产物存在 + 登录页关键交互冒烟（Vitest） |
  | integration | 全链路 e2e（对齐 registry integration/002） |

- 执行时点：Generator 在全量回归通过后运行 `bash scripts/smoke.sh` 并把用例数与结果记入 task.md「冒烟记录」槽位；Evaluator 必须亲自复跑，不采信自述

## 测试命名
方法名 `test_行为_条件_预期` 或 given_when_then 注释分节；一个用例只断言一条行为主线——命名即需求描述，失败时无需读实现即可定位行为
