# Integration Guide Content Verification Tests

This file contains automated content checks for `third-party-integration-guide.md`.
Run these checks to verify documentation completeness.

## AC1: 鉴权流程文档完整性

- [ ] 包含 "JWT" 关键字
- [ ] 包含 "Token" 关键字
- [ ] 包含 "登录" 或 "login" 关键字
- [ ] 包含 "Authorization" 关键字
- [ ] 包含 "Bearer" 关键字
- [ ] 包含 "过期" 或 "expire" 或 "失效" 关键字
- [ ] 包含 "流程" 或 "flow" 关键字
- [ ] 包含 "HS256" 或 "HS256" 关键字
- [ ] 包含 "payload" 关键字
- [ ] 包含 "uid" 和 "username" 和 "exp" 关键字

## AC2: Check API 契约文档完整性

- [ ] 包含 "/api/v1/auth/check" 路径
- [ ] 包含 "POST" 方法
- [ ] 包含 "Authorization: Bearer" 头
- [ ] 包含 "userId" 字段
- [ ] 包含 "permissionCode" 字段
- [ ] 包含 "hasPermission" 字段
- [ ] 包含 "Result" 响应结构
- [ ] 包含 "code=0" 成功码
- [ ] 包含 "1401" 错误码（未认证）
- [ ] 包含 "1403" 错误码（无权限）
- [ ] 包含 "1001" 错误码（用户不存在）
- [ ] 包含 "1201" 错误码（权限编码不存在）
- [ ] 包含请求示例（JSON）
- [ ] 包含响应示例（JSON）

## AC3: 错误码对照表完整性

- [ ] 包含 "10xx" 用户错误码分段
- [ ] 包含 "11xx" 角色错误码分段
- [ ] 包含 "12xx" 权限错误码分段
- [ ] 包含 "13xx" 模块错误码分段
- [ ] 包含 "14xx" 认证错误码分段
- [ ] 包含 "400" HTTP 状态码
- [ ] 包含 "401" HTTP 状态码
- [ ] 包含 "403" HTTP 状态码
- [ ] 包含 "404" HTTP 状态码
- [ ] 包含 "409" HTTP 状态码
- [ ] 包含 "排查" 或 "troubleshoot" 关键字
- [ ] 包含 "建议" 或 "suggestion" 关键字

## AC4: 多语言 SDK 接入示例完整性

- [ ] 包含 "Java" 或 "Spring Boot" 关键字
- [ ] 包含 "Filter" 或 "Interceptor" 关键字
- [ ] 包含 "Go" 或 "Gin" 关键字
- [ ] 包含 "Middleware" 关键字
- [ ] 包含 "Python" 或 "FastAPI" 关键字
- [ ] 包含 "Dependency" 关键字
- [ ] 包含 "Node.js" 或 "Express" 关键字
- [ ] 包含 "axios" 关键字
- [ ] 包含 "FeignClient" 或 "feign" 关键字
- [ ] 包含完整代码示例（包含 import、class、method）

## AC5: 最佳实践文档完整性

- [ ] 包含 "HTTPS" 关键字
- [ ] 包含 "HSTS" 关键字
- [ ] 包含 "权限码" 或 "permission code" 关键字
- [ ] 包含 "命名" 或 "naming" 关键字
- [ ] 包含 "重试" 或 "retry" 关键字
- [ ] 包含 "熔断" 或 "circuit breaker" 关键字
- [ ] 包含 "降级" 或 "fallback" 关键字
- [ ] 包含 "监控" 或 "monitoring" 关键字
- [ ] 包含 "告警" 或 "alert" 关键字
- [ ] 包含 "Prometheus" 或 "metrics" 关键字

---

## 运行检查脚本

```bash
# 检查文档是否包含所有必需内容
grep -c "JWT" docs/integration/third-party-integration-guide.md
grep -c "/api/v1/auth/check" docs/integration/third-party-integration-guide.md
grep -c "10xx" docs/integration/third-party-integration-guide.md
grep -c "Java" docs/integration/third-party-integration-guide.md
grep -c "HTTPS" docs/integration/third-party-integration-guide.md
```