# 工作单（当前 Sprint）：sprint-003

> 本文件是 Generator 实现与 Evaluator 验收的唯一工作契约。字段语义见下方「模板字段定义」；空槽模板与历史工作单见 git 历史。

## 模板字段定义

| 字段 | 填写者 | 说明 |
|------|--------|------|
| Sprint ID | Planner | 本工作单的全局唯一标识（如 `sprint-001`），用于日志与评审引用 |
| 所属模块 | Planner | 对应 `.harness/modules/registry.md` 中的模块名（infra/model/auth/users/roles/perms/modules/web/integration） |
| 需求描述 | Planner | 本 Sprint 要实现的功能点一句话描述，须与 registry 行语义一致 |
| 业务背景 | Planner | 为什么做：业务价值、RBAC 语义依据（仲裁见 `docs/01-architecture.md`） |
| 前置依赖 | Planner | 依赖的 registry 功能点 ID 列表（如 `model/001`）；无则填「—」 |
| 验收标准 | Planner | 可判定的验收条目清单；每条须映射测试用例（条目 ↔ 测试一一对应） |
| 测试清单 | Generator | 从验收标准导出的用例列表，先于实现写出（规则见 `.harness/rules/tdd-workflow.md`） |
| RED 证据 | Generator | 测试先行的失败输出摘录，格式须符合 `.harness/rules/tdd-workflow.md` 规定 |
| 实现说明 | Generator | 最小实现的落点摘要：改了哪些类/文件、为何这样改、重构内容 |
| 冒烟记录 | Generator 记录；Evaluator 复核 | 本次追加的冒烟用例数与 `scripts/smoke.sh` 执行结果（Evaluator 侧记复跑结论） |
| 状态 | Planner 初填；Generator/Evaluator 流转更新 | 枚举流转：PLANNED→IN_PROGRESS→AWAITING_REVIEW→REWORK→DONE｜BLOCKED |
| 评审意见 | Evaluator | 按 `.harness/rules/review-criteria.md` 五维评分的结论与整改要求 |

> 状态枚举说明：PLANNED 已规划未开工；IN_PROGRESS 实现中；AWAITING_REVIEW 待 Evaluator 评审；REWORK 评审退回返工；DONE 评审通过；BLOCKED 被依赖或评审阻塞（连续 2 次 REWORK 不通过即置 BLOCKED 并登记挂起区）。

---

## 基本信息
- Sprint ID: sprint-003
- 所属模块: infra
- 需求描述: 统一响应体 Result&lt;T&gt; + 全局异常处理 + Bean Validation（registry ID：infra/003）
- 业务背景: 全部 `/api/v1/{resource}` 业务接口的响应契约基座（`docs/01-architecture.md` §4）：Result{code:int(0=成功), message:String, data:T}；业务错误码分段 10xx 用户/11xx 角色/12xx 权限/13xx 模块/14xx 认证；HTTP 语义固定 401=未认证、403=未授权。分层口径依规范 §3（controller 显式包装 Result）、异常口径依规范 §5（业务失败抛 BusinessException(code,message)，系统异常全局兜底）。**规格空白的 Planner 定夺（供评审仲裁）**：§4 分段未覆盖的通用错误不占用资源段——参数校验失败 → HTTP 400 + code=400；未捕获兜底 → HTTP 500 + code=500 且 message 使用固定文案不泄露内部细节。
- 前置依赖: infra/001（✅ sprint-001）
- 状态: AWAITING_REVIEW

## 验收标准
<!-- 每条映射测试用例；Generator 按 tdd-workflow 先写测试确认 RED 再最小实现 -->

- [x] AC1 Result 契约：`Result<T>` 为不可变 record，静态工厂 `ok(data)` 产生 code=0、`error(code,message)` 产生非 0 业务码；Jackson 序列化输出恰含 code/message/data 三键 ← 用例 `ResultTest#test_成功工厂_序列化含codeMessageData且code为0`
- [x] AC2 业务异常路径：controller 抛 `BusinessException(1001,"用户已存在")` 时由全局处理器翻译为 HTTP 400，响应体 {code:1001, message:"用户已存在", data:null} ← 用例 `GlobalExceptionHandlerApiTest#test_业务异常_翻译为400与原样业务码`
- [x] AC3 Bean Validation 路径：`@Valid @RequestBody` 校验失败时返回 HTTP 400 且 {code:400, message 含字段级提示, data:null} ← 用例 `GlobalExceptionHandlerApiTest#test_参数校验失败_返回400与code400`
- [x] AC4 兜底路径：未捕获 `RuntimeException` 返回 HTTP 500 且 {code:500, message 为固定文案不含异常细节, data:null}，并以 error 级日志记录带堆栈 ← 用例 `GlobalExceptionHandlerApiTest#test_未捕获异常_返回500固定文案不泄露细节`

## 实现要点（Planner 提示，Generator 裁量落地）
- 落位 `com.authcore.common`（七业务包之一）：`Result` / `BusinessException` / `GlobalExceptionHandler` 三个类，不做 ErrorCode 枚举等超前抽象（YAGNI，资源段常量随各业务功能点引入）
- Web 层测试采用测试夹具内探针 controller + MockMvc standaloneSetup + 注册切面，禁止为了测试向主代码添加任何业务端点
- 冒烟增量：追加 1 条聚焦本功能点能力的用例（如定向运行 ResultTest 与 GlobalExceptionHandlerApiTest），幂等可重复
- 日志合规（规范 §5）：error 带堆栈上下文、占位符输出、循环外

## 测试清单
1. `ResultTest#test_成功工厂_序列化含codeMessageData且code为0` —— ok/error 工厂契约 + JSON 三键断言（覆盖 AC1）
2. `GlobalExceptionHandlerApiTest#test_业务异常_翻译为400与原样业务码` —— BusinessException → 400/1001/原样 message/data 空（覆盖 AC2）
3. `GlobalExceptionHandlerApiTest#test_参数校验失败_返回400与code400` —— @NotBlank 空值 → 400/code=400/message 含「名称不能为空」（覆盖 AC3）
4. `GlobalExceptionHandlerApiTest#test_未捕获异常_返回500固定文案不泄露细节` —— RuntimeException → 500/code=500/固定文案且响应不含 jdbc 细节/ListAppender 断言 ERROR 级带堆栈（覆盖 AC4）

## RED 证据
<!-- Generator 先写测试运行确认失败后实时摘录 -->
```text
[RED-A] COMPILATION ERROR — ResultTest 先行时 Result 不存在：
        backend/src/test/java/com/authcore/common/ResultTest.java:[28,49]/[35,9] 找不到符号 类 Result
[RED-B] COMPILATION ERROR — GlobalExceptionHandlerApiTest 先行时两类均不存在：
        找不到符号 类 GlobalExceptionHandler / 类 BusinessException
        （backend/src/test/java/com/authcore/common/GlobalExceptionHandlerApiTest.java:[35,38]/[51,27]/[104,65]）
```

## 实现说明
- 新增 `common/Result.java`：record 承载不可变契约（规范 §2 DTO/VO 用 record），ok/error 两工厂；message 成功态固定 "success"。
- 新增 `common/BusinessException.java`：仅携带 int code + message，HTTP 映射交由处理器，保持认证语义（401/403）留给 auth 模块扩展。
- 新增 `common/GlobalExceptionHandler.java`：@RestControllerAdvice 三路径（业务 400/校验 400+字段提示/兜底 500 固定文案），error 日志按规范 §5 带完整堆栈。
- 测试侧：探针 controller 与 DTO 全部内嵌于测试类（未向主代码添加任何端点）；standaloneSetup 显式注入 LocalValidatorFactoryBean 保证校验行为确定性。
- REFACTOR：各类 ≤60 行单一职责，无坏味道，重构零操作，重跑保持全绿。

## 冒烟记录
- Generator：本次追加 1 条用例（`infra-003 统一响应体与异常处理定向测试`，定向运行 ResultTest + GlobalExceptionHandlerApiTest）；`bash scripts/smoke.sh` 全组通过（exit=0，`SMOKE PASSED（3 用例）`）。
- Evaluator 复核结论：（待 Evaluator 复跑填写）

## 评审意见
- （由 Evaluator 评审时填写）
