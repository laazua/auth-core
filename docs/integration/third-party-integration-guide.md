# 第三方模块接入指南

> **文档版本**: v1.0.0  
> **适用版本**: auth-core v1.0.0+  
> **最后更新**: 2026-08-28  
> **维护团队**: auth-core 核心团队

---

## 目录

1. [鉴权流程概述](#1-鉴权流程概述)
2. [Check API 契约](#2-check-api-契约)
3. [错误码对照表](#3-错误码对照表)
4. [SDK 接入示例](#4-sdk-接入示例)
5. [最佳实践](#5-最佳实践)

---

## 1. 鉴权流程概述

### 1.1 JWT 无状态鉴权原理

auth-core 采用 **JWT (JSON Web Token)** 实现无状态鉴权。JWT 由三部分组成：

```
Header.Payload.Signature
```

- **Header**: 算法声明（HS256）、Token 类型
- **Payload**: 业务载荷（用户标识、过期时间等）
- **Signature**: HMAC-SHA256 签名，防篡改

### 1.2 Token 获取

#### 登录接口

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123456"
}
```

**成功响应**:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

| 字段 | 说明 |
|------|------|
| token | JWT 字符串，三段式 Base64Url 编码 |
| tokenType | 固定为 "Bearer" |
| expiresIn | 有效期（秒），默认 7200 秒（2 小时） |

### 1.3 Token 携带方式

所有受保护接口需在请求头携带 Token：

```
Authorization: Bearer <token>
```

### 1.4 校验流程

```mermaid
sequenceDiagram
    participant Client as 第三方模块
    participant Auth as auth-core
    Client->>Auth: POST /api/v1/auth/login
    Auth-->>Client: 返回 JWT Token
    Client->>Resource: 请求携带 Authorization: Bearer <token>
    Resource->>Auth: 调用 POST /api/v1/auth/check
    Auth-->>Resource: 返回 { hasPermission: true/false }
    Resource-->>Client: 返回业务响应
```

### 1.5 Token 过期处理

- Token 有效期默认 **2 小时**（7200 秒），可通过 `jwt.expire-hours` 配置
- Token 过期后，**不支持刷新**，需用户重新登录获取新 Token
- 客户端需监听 401 响应，引导用户重新登录
- 建议客户端在 Token 过期前 5 分钟提示用户续期（如有刷新机制）

### 1.6 安全注意事项

- **仅 HTTPS** 环境传输 Token，严禁 HTTP 明文传输
- **JWT Secret** 仅从环境变量读取，严禁写入代码/配置文件
- 建议启用 **HSTS** 强制 HTTPS
- Token 仅在内存存储，避免 localStorage XSS 风险（可选 HttpOnly Cookie）

---

## 2. Check API 契约

### 2.1 接口定义

| 属性 | 值 |
|------|-----|
| 接口地址 | `POST /api/v1/auth/check` |
| 请求方式 | `POST` |
| Content-Type | `application/json` |
| 认证方式 | `Authorization: Bearer <token>` |

### 2.2 请求结构

```json
{
  "userId": 1,
  "permissionCode": "user:create"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 被校验用户 ID |
| permissionCode | String | 是 | 权限编码，格式 `模块:操作` |

### 2.3 响应结构

**成功响应** (code=0):

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "hasPermission": true,
    "userId": 1,
    "permissionCode": "user:create"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| hasPermission | Boolean | 是否拥有权限 |
| userId | Long | 被校验用户 ID |
| permissionCode | String | 校验的权限编码 |

### 2.4 错误响应

| HTTP 状态码 | code | message | 说明 |
|-------------|------|---------|------|
| 401 | 1401 | 未认证/Token 无效/过期 | Token 缺失、格式错误、过期、签名无效 |
| 403 | 1403 | 无权限 | Token 有效但无该权限 |
| 404 | 1001 | 用户不存在 | userId 对应用户不存在 |
| 400 | 1201 | 权限编码不存在 | permissionCode 在系统中不存在 |

**错误响应示例**:

```json
{
  "code": 1401,
  "message": "Token 已过期",
  "data": null
}
```

### 2.5 完整调用示例

**cURL**:

```bash
curl -X POST "https://auth.example.com/api/v1/auth/check" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "permissionCode": "user:create"}'
```

**Java (Spring Boot + RestTemplate)**:

```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}

public boolean checkPermission(Long userId, String permissionCode) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);

    CheckRequest request = new CheckRequest(userId, permissionCode);
    HttpEntity<CheckRequest> entity = new HttpEntity<>(request, headers);

    ResponseEntity<Result<CheckResponse>> response = restTemplate.exchange(
        "https://auth.example.com/api/v1/auth/check",
        HttpMethod.POST,
        entity,
        new ParameterizedTypeReference<Result<CheckResponse>>() {}
    );

    return response.getBody().getData().isHasPermission();
}
```

---

## 3. 错误码对照表

### 3.1 错误码分段规则

| 分段 | 范围 | 业务域 | HTTP 状态码映射 |
|------|------|--------|-----------------|
| 用户 | 10xx | 用户管理 | 400/404/409 |
| 角色 | 11xx | 角色管理 | 400/404/409 |
| 权限 | 12xx | 权限管理 | 400/404/409 |
| 模块 | 13xx | 模块管理 | 400/404/409 |
| 认证 | 14xx | 认证授权 | 401/403 |

### 3.2 完整错误码表

| code | HTTP | message | 业务含义 | 排查建议 |
|------|------|---------|----------|----------|
| **1001** | 404 | 用户不存在 | 查询/操作的用户 ID 不存在 | 检查 userId 是否正确、用户是否被删除 |
| **1002** | 409 | 用户名已存在 | 创建用户时用户名冲突 | 更换用户名或检查是否已注册 |
| **1003** | 409 | 角色引用保护 | 删除角色时存在用户关联 | 先解除用户角色关联再删除 |
| **1004** | 400 | 角色无效 | 分配的角色不存在或已停用 | 检查 roleId 是否存在且 status=1 |
| **1005** | 400 | 旧密码错误 | 修改密码时旧密码不匹配 | 确认旧密码正确 |
| **1006** | 400 | 新旧密码相同 | 修改密码时新旧密码相同 | 使用不同的新密码 |
| **1101** | 409 | 角色名已存在 | 创建角色时名称冲突 | 更换角色名 |
| **1102** | 409 | 角色编码已存在 | 创建角色时编码冲突 | 更换角色编码 |
| **1103** | 409 | 用户引用保护 | 删除角色存在用户关联 | 先解除用户角色关联 |
| **1104** | 409 | 权限引用保护 | 删除角色存在权限关联 | 先解除角色权限关联 |
| **1201** | 409 | 权限名已存在 | 创建权限时名称冲突 | 更换权限名 |
| **1202** | 409 | 权限编码已存在 | 创建权限时编码冲突 | 更换权限编码 |
| **1203** | 409 | 权限引用保护 | 删除权限存在角色关联 | 先解除角色权限关联 |
| **1301** | 409 | 模块名已存在 | 创建模块时名称冲突 | 更换模块名 |
| **1302** | 409 | 模块编码已存在 | 创建模块时编码冲突 | 更换模块编码 |
| **1303** | 409 | 模块引用保护 | 删除模块存在权限关联 | 先删除模块下权限 |
| **1401** | 401 | 未认证/Token无效 | Token缺失/过期/签名错误/用户停用 | 重新登录获取新 Token |
| **1402** | 403 | 权限不足 | Token 有效但无该权限 | 联系管理员分配权限 |
| **1403** | 403 | 无权限操作 | 非管理员执行管理操作 | 确认操作者权限 |

### 3.3 HTTP 状态码映射表

| HTTP 状态码 | 业务场景 | 典型 code |
|-------------|----------|-----------|
| 200 | 成功 | 0 |
| 400 | 参数校验失败/业务规则违反 | 1004, 1005, 1006, 1201 |
| 401 | 认证失败 | 1401 |
| 403 | 授权失败/权限不足 | 1402, 1403 |
| 404 | 资源不存在 | 1001 |
| 409 | 资源冲突/引用保护 | 1002, 1003, 1101-1104, 1201-1203, 1301-1303 |

---

## 4. SDK 接入示例

### 4.1 Java / Spring Boot

#### 4.1.1 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

#### 4.1.2 JWT 解析工具

```java
@Component
public class JwtTokenResolver {

    @Value("${jwt.secret}")
    private String secret;

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public Long getUserId(String token) {
        return parseToken(token).get("uid", Long.class);
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}
```

#### 4.1.3 认证拦截器

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenResolver jwtTokenResolver;
    private final RestTemplate restTemplate;

    @Autowired
    public AuthInterceptor(JwtTokenResolver jwtTokenResolver, RestTemplate restTemplate) {
        this.jwtTokenResolver = jwtTokenResolver;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);
        if (jwtTokenResolver.isExpired(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 可选：调用 check API 校验具体权限
        return true;
    }
}
```

#### 4.1.4 FeignClient 调用 Check API

```java
@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthCheckClient {

    @PostMapping("/api/v1/auth/check")
    Result<CheckResponse> checkPermission(@RequestBody CheckRequest request);
}

@Data
public class CheckRequest {
    private Long userId;
    private String permissionCode;
}

@Data
public class CheckResponse {
    private Boolean hasPermission;
    private Long userId;
    private String permissionCode;
}

// 使用示例
@Service
public class PermissionService {

    @Autowired
    private AuthCheckClient authCheckClient;

    public boolean checkPermission(Long userId, String permissionCode) {
        CheckRequest request = new CheckRequest();
        request.setUserId(userId);
        request.setPermissionCode(permissionCode);

        Result<CheckResponse> result = authCheckClient.checkPermission(request);
        return result.getCode() == 0 && Boolean.TRUE.equals(result.getData().getHasPermission());
    }
}
```

### 4.2 Go / Gin

```go
package middleware

import (
    "github.com/gin-gonic/gin"
    "github.com/golang-jwt/jwt/v5"
    "net/http"
    "strings"
)

var jwtSecret = []byte(os.Getenv("JWT_SECRET"))

func AuthMiddleware() gin.HandlerFunc {
    return func(c *gin.Context) {
        authHeader := c.GetHeader("Authorization")
        if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
            c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"code": 1401, "message": "Token 缺失"})
            return
        }

        tokenString := strings.TrimPrefix(authHeader, "Bearer ")
        token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
            return jwtSecret, nil
        })

        if err != nil || !token.Valid {
            c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"code": 1401, "message": "Token 无效"})
            return
        }

        claims := token.Claims.(jwt.MapClaims)
        c.Set("userId", claims["uid"])
        c.Set("username", claims["username"])
        c.Next()
    }
}

// 调用 Check API
func CheckPermission(userID int64, permissionCode string) (bool, error) {
    reqBody := map[string]interface{}{
        "userId":        userID,
        "permissionCode": permissionCode,
    }

    jsonBody, _ := json.Marshal(reqBody)
    req, _ := http.NewRequest("POST", "https://auth.example.com/api/v1/auth/check", bytes.NewBuffer(jsonBody))
    req.Header.Set("Authorization", "Bearer "+token)
    req.Header.Set("Content-Type", "application/json")

    client := &http.Client{Timeout: 5 * time.Second}
    resp, err := client.Do(req)
    if err != nil {
        return false, err
    }
    defer resp.Body.Close()

    var result struct {
        Code int `json:"code"`
        Data struct {
            HasPermission bool `json:"hasPermission"`
        } `json:"data"`
    }
    json.NewDecoder(resp.Body).Decode(&result)
    return result.Data.HasPermission, nil
}
```

### 4.3 Python / FastAPI

```python
from fastapi import FastAPI, Depends, HTTPException, Header
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import jwt
import httpx
import os

app = FastAPI()
security = HTTPBearer()
JWT_SECRET = os.getenv("JWT_SECRET")
AUTH_SERVICE_URL = os.getenv("AUTH_SERVICE_URL", "https://auth.example.com")

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail={"code": 1401, "message": "Token 已过期"})
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail={"code": 1401, "message": "Token 无效"})

async def check_permission(user_id: int, permission_code: str) -> bool:
    async with httpx.AsyncClient() as client:
        response = await client.post(
            f"{AUTH_SERVICE_URL}/api/v1/auth/check",
            headers={"Authorization": f"Bearer {token}"},
            json={"userId": user_id, "permissionCode": permission_code},
            timeout=5.0
        )
    result = response.json()
    return result.get("code") == 0 and result.get("data", {}).get("hasPermission", False)

@app.get("/api/users")
async def get_users(current_user: dict = Depends(get_current_user)):
    if not await check_permission(current_user["uid"], "user:view"):
        raise HTTPException(status_code=403, detail={"code": 1403, "message": "权限不足"})
    return {"users": []}
```

### 4.4 Node.js / Express

```javascript
const express = require('express');
const jwt = require('jsonwebtoken');
const axios = require('axios');

const app = express();
const JWT_SECRET = process.env.JWT_SECRET;
const AUTH_SERVICE_URL = process.env.AUTH_SERVICE_URL || 'https://auth.example.com';

// 认证中间件
function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ code: 1401, message: 'Token 缺失' });
  }

  const token = authHeader.substring(7);
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({ code: 1401, message: 'Token 无效或已过期' });
  }
}

// 权限校验中间件
function requirePermission(permissionCode) {
  return async (req, res, next) => {
    try {
      const response = await axios.post(
        `${process.env.AUTH_SERVICE_URL}/api/v1/auth/check`,
        { userId: req.user.uid, permissionCode },
        { headers: { Authorization: `Bearer ${req.headers.authorization.split(' ')[1]}` } }
      );

      if (response.data.code === 0 && response.data.data.hasPermission) {
        next();
      } else {
        res.status(403).json({ code: 1403, message: '权限不足' });
      }
    } catch (err) {
      res.status(500).json({ code: 500, message: '鉴权服务异常' });
    }
  };
}

// 使用示例
app.get('/api/users', authMiddleware, requirePermission('user:view'), (req, res) => {
  res.json({ users: [] });
});

app.listen(3000);
```

---

## 5. 最佳实践

### 5.1 Token 存储与传输安全

| 场景 | 推荐方案 | 说明 |
|------|----------|------|
| Web 单页应用 | **内存存储 + HttpOnly Cookie 可选** | 避免 localStorage XSS，短 Token 配合刷新机制 |
| 移动端 App | **Keychain/Keystore 安全存储** | 利用系统级加密存储 |
| 服务间调用 | **内存 + 短期缓存** | 避免频繁登录，缓存 Token 并提前刷新 |
| 传输层 | **强制 HTTPS + HSTS** | 防止中间人攻击、SSL 剥离 |

```nginx
# Nginx HSTS 配置示例
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
```

### 5.2 权限码设计规范

| 规范 | 示例 | 说明 |
|------|------|------|
| 格式 | `模块:操作` | `user:create`、`role:delete` |
| 模块名 | 小写、复数 | `user`、`role`、`permission` |
| 操作名 | 小写、动词 | `create`、`read`、`update`、`delete`、`assign`、`export` |
| 通配符 | 不支持 | 每个操作需显式授权 |

**推荐权限码清单**:

| 模块 | 权限码 | 说明 |
|------|--------|------|
| user | `user:view` `user:create` `user:update` `user:delete` `user:assign` `user:reset_password` |
| role | `role:view` `role:create` `role:update` `role:delete` `role:assign` |
| permission | `perm:view` `perm:create` `perm:update` `perm:delete` |
| module | `module:view` `module:create` `module:update` `module:delete` |

### 5.3 错误处理策略

| 场景 | 策略 | 实现建议 |
|------|------|----------|
| Token 过期 | 重定向登录页 | 前端拦截 401，清除本地状态，跳转登录页 |
| 网络异常/超时 | 指数退避重试 | 指数退避（1s, 2s, 4s...）+ 最大重试 3 次 |
| 鉴权服务降级 | 熔断/降级 | Hystrix/Resilience4j/Sentinel，降级放行/拒绝 |
| 权限变更延迟 | 缓存失效通知 | Redis Pub/Sub 广播权限变更，客户端主动刷新 |

```java
// 重试策略示例
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
public boolean checkPermissionWithRetry(Long userId, String permissionCode) {
    return authCheckClient.checkPermission(new CheckRequest(userId, permissionCode))
        .getData().isHasPermission();
}
```

### 5.4 监控告警

**关键指标**:

| 指标名 | 类型 | 告警阈值 | 说明 |
|--------|------|----------|------|
| `auth_login_total` | Counter | - | 登录总次数 |
| `auth_login_failed_total` | Counter | > 10/min | 登录失败率异常 |
| `auth_check_total` | Counter | - | 权限校验总次数 |
| `auth_check_denied_total` | Counter | > 50/min | 权限拒绝率异常 |
| `auth_token_expired_total` | Counter | - | Token 过期次数 |
| `auth_check_latency_seconds` | Histogram | P99 > 500ms | 校验接口延迟 |

**Prometheus 告警规则示例**:

```yaml
groups:
- name: auth-alerts
  rules:
  - alert: AuthHighFailureRate
    expr: rate(auth_login_failed_total[5m]) > 10
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "认证失败率过高"
      description: "最近 5 分钟登录失败率超过 10 次/分"

  - alert: AuthCheckHighLatency
    expr: histogram_quantile(0.99, rate(auth_check_latency_seconds_bucket[5m])) > 0.5
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "权限校验延迟过高"
      description: "P99 延迟超过 500ms"
```

### 5.5 部署检查清单

上线前请确认：

- [ ] JWT Secret 已配置为高强度随机字符串（≥ 32 字符）
- [ ] `jwt.expire-hours` 已按业务需求配置
- [ ] 所有接口强制 HTTPS，已配置 HSTS
- [ ] CORS 策略已限制可信域名
- [ ] 限流已配置（登录接口、check 接口）
- [ ] 审计日志已开启（登录、权限变更、敏感操作）
- [ ] 监控大盘已配置（登录成功率、鉴权延迟、错误率）
- [ ] 告警规则已配置并验证触发
- [ ] 灾备演练已完成（auth-core 单点故障切换）

---

## 附录

### A. 版本历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v1.0.0 | 2026-08-28 | 初版发布 | auth-core 团队 |

### B. 相关文档

- [auth-core 架构设计文档](../01-architecture.md)
- [API 规范文档](../api-spec.md)
- [部署运维手册](../deployment.md)

### C. 联系支持

- **技术支持**: auth-core@company.com
- **问题反馈**: GitHub Issues
- **安全漏洞**: security@company.com

---

*文档结束*