# Auth-Core 部署指南

> 版本: v1.0.0 | 更新: 2026-08-31 | 适用: auth-core v1.0.0+

---

## 📋 前置要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 21+ | Eclipse Temurin / OpenJDK |
| Maven | 3.9+ | 后端构建 |
| Node.js | 20+ | 前端构建 |
| MySQL | 8.0+ | 数据库 |
| Nginx | 1.20+ | 反向代理/SSL 终结 |
| JDK | 21 | 运行时 (后端) |

> 💡 生产环境建议: 独立 MySQL 实例、独立 Nginx、独立应用服务器

---

## 🖥️ 方式一：主机直接部署 (裸机/系统服务)

### 1️⃣ 准备环境

```bash
# 安装依赖 (CentOS/RHEL/Rocky/AlmaLinux 示例)
sudo dnf install -y java-21-openjdk maven nginx mysql-community-server

# 或 Ubuntu/Debian
sudo apt update && apt install -y openjdk-21-jdk maven nginx mysql-server

# 创建应用用户
sudo useradd -r -s /sbin/nologin -d /opt/auth-core authcore
sudo mkdir -p /opt/auth-core/{backend,frontend,logs}
sudo chown -R authcore:authcore /opt/auth-core
```

### 2️⃣ 数据库初始化

```bash
# 创建数据库
mysql -h <host> -u root -p -e "
CREATE DATABASE IF NOT EXISTS authcore 
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'authcore'@'%' IDENTIFIED BY '${MYSQL_PASSWORD}';
GRANT ALL PRIVILEGES ON authcore.* TO 'authcore'@'%';
FLUSH PRIVILEGES;
"

# 导入 Flyway 迁移 (首次部署自动执行，也可手动)
cd /opt/auth-core/backend
MYSQL_PASSWORD=${MYSQL_PASSWORD} FLYWAY_ENABLED=true mvn -q flyway:migrate
```

### 3️⃣ 后端构建与运行

```bash
# 构建 (在 backend 目录)
cd /opt/auth-core/backend
mvn -q -DskipTests clean package

# 产物: target/auth-core-backend-0.0.1-SNAPSHOT.jar
```

**systemd 服务** `/etc/systemd/system/auth-core.service`:

```ini
[Unit]
Description=Auth Core Backend
After=network.target mysqld.service
Requires=mysqld.service

[Service]
Type=simple
User=authcore
Group=authcore
WorkingDirectory=/opt/auth-core/backend
EnvironmentFile=/opt/auth-core/.env
ExecStart=/usr/bin/java -Xms512m -Xmx1g -jar target/auth-core-backend-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
# 启动
sudo systemctl daemon-reload
sudo systemctl enable --now auth-core
sudo systemctl status auth-core
```

### 4️⃣ 前端构建与 Nginx

```bash
cd /opt/auth-core/frontend

# 安装依赖与构建
npm ci
npm run build          # 产物在 dist/

# Nginx 配置片段
server {
    listen 80;
    server_name auth.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name auth.example.com;

    ssl_certificate     /etc/ssl/certs/auth.example.com.crt;
    ssl_certificate_key /etc/ssl/private/auth.example.com.key;

    # 前端静态资源
    location / {
        root /opt/auth-core/frontend/dist;
        try_files $uri $uri/ /index.html;
        add_header Cache-Control "public, max-age=31536000, immutable";
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Actuator 仅内网
    location /actuator/ {
        allow 10.0.0.0/8; deny all;
        proxy_pass http://127.0.0.1:8080;
    }
}
```

```bash
sudo nginx -t && sudo systemctl reload nginx
```

### 5️⃣ 验证部署

```bash
# 后端健康检查
curl -fsS http://localhost:8080/actuator/health | jq .status
# 预期: "UP"

# 前端访问
curl -fsS https://auth.example.com | grep -c "auth-core"

# 完整链路测试
curl -sS -X POST https://auth.example.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123456"}' | jq .code
# 预期: 0
```

---

## 🐳 方式二：Docker 容器部署

### 1️⃣ 镜像构建

```dockerfile
# docker/Dockerfile.backend
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/auth-core-backend-*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms512m -Xmx1g"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
```

```dockerfile
# docker/Dockerfile.frontend
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

```bash
# 构建镜像
cd /opt/auth-core
docker build -f docker/Dockerfile.backend -t auth-core-backend:latest ./backend
docker build -f docker/Dockerfile.frontend -t auth-core-frontend:latest ./frontend
```

### 2️⃣ 生产环境 `docker-compose.prod.yml`

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: auth-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_DATABASE: authcore
      MYSQL_USER: authcore
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    networks: [auth-net]
    command: --default-authentication-plugin=mysql_native_password

  backend:
    image: auth-core-backend:latest
    container_name: auth-backend
    restart: always
    environment:
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRE_HOURS: 2
      FLYWAY_ENABLED: "true"
      DB_HEALTH_ENABLED: "true"
    ports: ["8080:8080"]
    depends_on: [mysql]
    networks: [auth-net]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    image: auth-core-frontend:latest
    container_name: auth-frontend
    restart: always
    ports: ["80:80", "443:443"]
    volumes:
      - ./ssl:/etc/ssl/certs:ro
    depends_on: [backend]
    networks: [auth-net]

  nginx:
    image: nginx:alpine
    container_name: auth-nginx
    restart: always
    ports: ["80:80", "443:443"]
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/ssl/certs:ro
    depends_on: [frontend, backend]
    networks: [auth-net]

volumes:
  mysql-data:

networks:
  auth-net:
    driver: bridge
```

```bash
# 启动生产环境
docker compose -f docker-compose.prod.yml --env-file .env up -d

# 查看状态
docker compose ps
docker compose logs -f backend
```

### 3️⃣ 容器健康检查与日志

```bash
# 健康检查
docker compose exec backend curl -f http://localhost:8080/actuator/health

# 查看日志
docker compose logs -f --tail=100 backend

# 进入容器调试
docker compose exec backend sh
```

---

## 🔐 环境变量模板 `.env.example`

```bash
# 数据库
MYSQL_PASSWORD=your_strong_mysql_password

# JWT (必须 ≥32 字符随机串)
JWT_SECRET=your-super-secret-jwt-key-min-32-chars
JWT_EXPIRE_HOURS=2

# Flyway/健康检查
FLYWAY_ENABLED=true
DB_HEALTH_ENABLED=true
```

> ⚠️ 生产环境**严禁**使用默认值，请替换为强随机值

---

## 🔧 常用运维命令

| 操作 | 命令 |
|------|------|
| 启动服务 | `systemctl start auth-core` / `docker compose up -d` |
| 停止服务 | `systemctl stop auth-core` / `docker compose down` |
| 重启服务 | `systemctl restart auth-core` / `docker compose restart` |
| 查看日志 | `journalctl -u auth-core -f` / `docker compose logs -f backend` |
| 重新部署 | `mvn clean package && systemctl restart auth-core` / `docker compose up -d --build` |
| 备份数据库 | `mysqldump -h host -u user -p authcore > backup_$(date +%F).sql` |
| 恢复数据库 | `mysql -u root -p authcore < backup_2026-08-31.sql` |
| 证书续期 | `certbot renew --dry-run && systemctl reload nginx` |

---

## 🩺 健康检查与冒烟测试

```bash
# 后端健康检查
curl -fsS http://localhost:8080/actuator/health | jq .status
# 预期: "UP"

# 完整链路测试
curl -sS -X POST https://auth.example.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123456"}' | jq .code
# 预期: 0

# E2E 冒烟测试
bash scripts/e2e-smoke.sh
# 预期: 所有用例通过
```

---

## 📋 部署验收清单

- [ ] 后端 `/actuator/health` → `{"status":"UP"}`
- [ ] 前端首页 HTTPS 加载无 JS 错误
- [ ] 登录 → 获取 Token → 访问 `/me` → 200 返回用户信息
- [ ] Check API: 有权限→`hasPermission:true`，无权限→`false`，无Token→401
- [ ] 登出 → Token 失效 → 重定向登录页
- [ ] 所有 smoke/e2e 用例通过
- [ ] 监控大盘数据正常、告警规则生效
- [ ] SSL 证书有效、HSTS 生效、安全头完整

---

## 🆘 常见故障快速排查

| 现象 | 排查步骤 |
|------|----------|
| 启动失败 `Connection refused` | 检查 MySQL 连通性、`MYSQL_PASSWORD`、`FLYWAY_ENABLED=true` |
| 401 频发 | 核对 `JWT_SECRET` 一致性、Token 过期时间、服务器时钟同步 |
| 403 误判 | 确认权限码格式 `模块:操作`、用户角色关联 `sys_user_role` |
| 登录慢 | 检查 BCrypt cost(10)、DB 连接池、数据库索引 |
| 前端白屏 | 检查 `dist/` 完整性、Nginx `try_files`、浏览器控制台报错 |

---

## 📦 备份与恢复

```bash
# 备份 (每日全量 + binlog 增量)
mysqldump -h $DB_HOST -u $DB_USER -p$DB_PASS authcore \
  --single-transaction --routines --triggers > backup_$(date +%F).sql

# 恢复
mysql -h $DB_HOST -u $DB_USER -p$DB_PASS authcore < backup_2026-08-31.sql

# 验证恢复
mysql -h $DB_HOST -u $DB_USER -p$DB_PASS -e "SELECT COUNT(*) FROM sys_user;"
```

---

## 📞 支持与反馈

- 文档问题: 提交 Issue
- 部署故障: 查看 `journalctl -u auth-core -f` / `docker compose logs -f backend`
- 安全漏洞: 发送至 security@company.com

---

> 文档版本: v1.0.0 | 最后更新: 2026-08-31 | 维护: auth-core 团队