# Sprint 工作单：sprint-030

## 基本信息

| 字段 | 值 |
|------|-----|
| Sprint ID | sprint-030 |
| 所属模块 | integration |
| 功能点 ID | integration/001 |
| 功能点名称 | 第三方模块接入指南（鉴权流程/check API 契约/错误码表） |
| 状态 | PLANNED |
| 创建时间 | 2026-08-28 |

## 前置依赖

| 依赖 ID | 说明 | 状态 |
|---------|------|------|
| auth/005 | 权限校验 API POST /api/v1/auth/check | ✅ |

## 业务背景

第三方模块（微服务、外部系统）需要接入 auth-core 进行统一鉴权。需提供标准化接入文档，涵盖鉴权流程、check API 契约、错误码对照表、SDK 接入示例、最佳实践。

## 需求描述

编写第三方模块接入指南文档（Markdown 格式），包含：

1. **鉴权流程概述**：
   - JWT 无状态鉴权原理
   - Token 获取（登录接口）、携带方式（Authorization: Bearer）、校验流程
   - Token 刷新策略（可选，v1 版本不刷新、仅过期重登）

2. **Check API 契约**：
   - 接口：POST /api/v1/auth/check
   - 请求头：Authorization: Bearer <token>
   - 请求体：{ userId: Long, permissionCode: String }
   - 响应结构：Result<CheckResponse> { hasPermission: boolean, userId: Long, permissionCode: String }
   - 成功码：200 code=0
   - 错误码：401 code=1401（未认证/Token 无效）、403 code=1403（无权限）、404 code=1001（用户不存在）、400 code=1201（权限编码不存在）

3. **错误码对照表**：
   - 10xx 用户、11xx 角色、12xx 权限、13xx 模块、14xx 认证
   - 完整错误码表、HTTP 状态码映射、排查建议

4. **SDK 接入示例**：
   - Java/Spring Boot：Filter/Interceptor 自动校验、FeignClient 调用 check API
   - Go/Gin：Middleware 鉴权、HTTP Client 调用
   - Python/FastAPI：Dependency 鉴权、httpx 调用
   - Node.js/Express：Middleware 鉴权、axios 调用

5. **最佳实践**：
   - Token 存储与传输安全（HTTPS、HttpOnly Cookie 可选）
   - 权限码设计规范（模块:操作，如 user:create）
   - 错误处理策略（重试、降级、熔断）
   - 监控告警（鉴权失败率、延迟）

## 交付物

1. `docs/integration/third-party-integration-guide.md` — 完整接入指南文档

## 验收标准（TDD 驱动）

### AC1 — 鉴权流程文档完整
> 文档包含 JWT 鉴权原理、Token 获取/携带/校验流程图、Token 过期处理说明。

**用例**：`IntegrationGuideReview#authFlowDocumentComplete`
- 检查文档包含：JWT 原理、登录获取 Token、Authorization Header 格式、校验流程、过期重登

### AC2 — Check API 契约文档完整
> 文档包含接口地址、请求/响应结构、字段说明、示例请求/响应、错误码映射。

**用例**：`IntegrationGuideReview#checkApiContractComplete`
- 检查：POST /api/v1/auth/check、请求头/体、响应体、hasPermission 语义、错误码 1401/1403/1001/1201

### AC3 — 错误码对照表完整
> 文档包含 10xx-14xx 全部错误码、HTTP 状态码、排查建议。

**用例**：`IntegrationGuideReview#errorCodeTableComplete`
- 检查：10xx/11xx/12xx/13xx/14xx 分段、HTTP 400/401/403/404/409 映射、排查建议

### AC4 — 多语言 SDK 接入示例完整
> 文档包含 Java/Go/Python/Node.js 四语言接入示例代码。

**用例**：`IntegrationGuideReview#sdkExamplesComplete`
- 检查：Java Filter/Interceptor、Go Middleware、Python Dependency、Node.js Middleware、完整可运行示例

### AC5 — 最佳实践文档完整
> 文档包含 Token 安全、权限码设计、错误处理、监控告警最佳实践。

**用例**：`IntegrationGuideReview#bestPracticesComplete`
- 检查：HTTPS/HSTS、权限码命名规范、重试/熔断/降级、Prometheus 指标

## 规范检查清单（Evaluator 逐项核对）

- [ ] 文档格式：Markdown、目录结构清晰、代码块高亮
- [ ] 内容完整：五大章节全覆盖、无遗漏
- [ ] 示例可用：代码片段可直接复制运行、版本兼容性说明
- [ ] 错误码表：与后端实现一致、分段清晰
- [ ] 交叉引用：文档内链接跳转正确、外部链接有效
- [ ] 版本标识：文档版本号、适用 auth-core 版本、更新日期