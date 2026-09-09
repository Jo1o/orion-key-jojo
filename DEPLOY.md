# 🚀 Docker 部署指南 - jojobuy.top

## 📋 部署前准备

### 1. 服务器要求

- **操作系统**: Ubuntu 20.04+ / Debian 11+ / CentOS 8+
- **CPU**: 2核以上
- **内存**: 4GB 以上
- **硬盘**: 20GB 以上
- **已安装**: Docker 和 Docker Compose

### 2. 域名配置

确保域名 `jojobuy.top` 已解析到服务器 IP：

```bash
# 添加 A 记录
jojobuy.top     → 你的服务器IP
www.jojobuy.top → 你的服务器IP
```

验证解析：
```bash
ping jojobuy.top
```

---

## 📦 部署步骤

### 第一步：上传项目到服务器

```bash
# 在本地打包项目
cd D:\workspace\orion-key-jojo
tar -czf orion-key.tar.gz .

# 上传到服务器（替换为你的服务器IP）
scp orion-key.tar.gz root@your-server-ip:/root/

# 或使用 Git
git clone https://github.com/your-repo/orion-key-jojo.git
cd orion-key-jojo
```

### 第二步：配置环境变量

```bash
# 进入项目目录
cd /root/orion-key-jojo  # 或你的项目路径

# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
nano .env
```

**必须修改的配置：**

```bash
# 数据库密码（强密码）
DB_PASSWORD=your_super_secure_password_2024

# JWT 密钥（生成新的）
JWT_SECRET=$(openssl rand -base64 64)

# Tghao 配置
TGHAO_ENABLED=true
TGHAO_APP_ID=2853
TGHAO_APP_KEY=87690A94DD4EC45B

# 邮件配置（可选）
MAIL_ENABLED=false
```

### 第三步：配置 SSL 证书

#### 方案 A：使用 Let's Encrypt 免费证书（推荐）

```bash
# 安装 Certbot
sudo apt update
sudo apt install certbot

# 获取证书
sudo certbot certonly --standalone -d jojobuy.top -d www.jojobuy.top

# 复制证书到项目
sudo mkdir -p nginx/ssl
sudo cp /etc/letsencrypt/live/jojobuy.top/fullchain.pem nginx/ssl/jojobuy.top.crt
sudo cp /etc/letsencrypt/live/jojobuy.top/privkey.pem nginx/ssl/jojobuy.top.key

# 设置权限
sudo chmod 644 nginx/ssl/jojobuy.top.crt
sudo chmod 600 nginx/ssl/jojobuy.top.key
```

#### 方案 B：临时自签名证书（测试用）

```bash
mkdir -p nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/jojobuy.top.key \
  -out nginx/ssl/jojobuy.top.crt \
  -subj "/CN=jojobuy.top"
```

### 第四步：构建并启动服务

```bash
# 构建镜像（首次部署或代码更新后）
docker-compose build

# 启动所有服务
docker-compose up -d

# 查看启动日志
docker-compose logs -f
```

### 第五步：初始化数据库

数据库会自动初始化（通过 docker-entrypoint-initdb.d）。

**验证数据库：**

```bash
# 进入数据库容器
docker exec -it orion-postgres psql -U postgres -d db

# 查询管理员账号
SELECT username, password_hash FROM users WHERE username = 'admin';

# 退出
\q
```

**如果需要手动初始化：**

```bash
docker exec -i orion-postgres psql -U postgres -d db < apps/api/src/main/resources/data.sql
docker exec -i orion-postgres psql -U postgres -d db < apps/api/src/main/resources/tghao_migration.sql
```

### 第六步：验证部署

```bash
# 检查所有容器状态
docker-compose ps

# 应该看到 4 个容器运行中：
# orion-postgres  (健康)
# orion-api       (健康)
# orion-web       (健康)
# orion-nginx     (健康)
```

**测试访问：**

1. HTTP（会自动跳转 HTTPS）: http://jojobuy.top
2. HTTPS: https://jojobuy.top
3. 管理后台: https://jojobuy.top/admin
4. API: https://jojobuy.top/api/health

---

## 🔐 登录信息

### 管理员账号

**重要：首次登录后立即修改密码！**

由于生产环境使用 BCrypt 加密（`PASSWORD_PLAIN=false`），需要重置密码：

```bash
# 方法 1: 进入容器重置
docker exec -it orion-postgres psql -U postgres -d db

# 更新为 BCrypt 加密的 admin123
UPDATE users SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE username = 'admin';

\q
```

然后使用：
- **用户名**: `admin`
- **密码**: `admin123`

---

## 📊 日常运维

### 查看日志

```bash
# 所有服务日志
docker-compose logs -f

# 单个服务日志
docker-compose logs -f api
docker-compose logs -f web
docker-compose logs -f postgres
docker-compose logs -f nginx
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启单个服务
docker-compose restart api
docker-compose restart web
```

### 停止服务

```bash
# 停止所有服务
docker-compose stop

# 完全删除（保留数据）
docker-compose down

# 完全删除（包括数据，危险！）
docker-compose down -v
```

### 更新代码

```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建镜像
docker-compose build

# 3. 重启服务
docker-compose up -d

# 4. 查看更新状态
docker-compose logs -f
```

### 数据库备份

```bash
# 备份数据库
docker exec orion-postgres pg_dump -U postgres db > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i orion-postgres psql -U postgres -d db < backup_20240909.sql
```

---

## 🔧 配置调整

### 修改端口

编辑 `docker-compose.yml`：

```yaml
services:
  nginx:
    ports:
      - "8080:80"    # HTTP 改为 8080
      - "8443:443"   # HTTPS 改为 8443
```

### 扩展资源限制

编辑 `docker-compose.yml`，添加资源限制：

```yaml
services:
  api:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### 配置邮件通知

编辑 `.env`：

```bash
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

重启后端：
```bash
docker-compose restart api
```

---

## 🛡️ 安全建议

### 1. 防火墙配置

```bash
# 只开放必要端口
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp  # SSH
sudo ufw enable
```

### 2. SSL 证书自动续期

```bash
# 添加 cron 任务
sudo crontab -e

# 每月1号自动续期
0 0 1 * * certbot renew --quiet && cp /etc/letsencrypt/live/jojobuy.top/fullchain.pem /root/orion-key-jojo/nginx/ssl/jojobuy.top.crt && cp /etc/letsencrypt/live/jojobuy.top/privkey.pem /root/orion-key-jojo/nginx/ssl/jojobuy.top.key && docker-compose restart nginx
```

### 3. 定期备份

```bash
# 添加每日备份脚本
cat > /root/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/root/backups"
mkdir -p $BACKUP_DIR
DATE=$(date +%Y%m%d_%H%M%S)

# 备份数据库
docker exec orion-postgres pg_dump -U postgres db > $BACKUP_DIR/db_$DATE.sql

# 保留最近30天的备份
find $BACKUP_DIR -name "db_*.sql" -mtime +30 -delete
EOF

chmod +x /root/backup.sh

# 添加到 cron（每天凌晨2点）
echo "0 2 * * * /root/backup.sh" | sudo crontab -
```

---

## 🐛 故障排查

### 容器无法启动

```bash
# 查看详细日志
docker-compose logs api
docker-compose logs web

# 检查配置文件语法
docker-compose config
```

### 数据库连接失败

```bash
# 检查数据库容器
docker-compose ps postgres
docker-compose logs postgres

# 测试连接
docker exec orion-postgres pg_isready -U postgres
```

### 网站无法访问

```bash
# 检查 Nginx 配置
docker exec orion-nginx nginx -t

# 查看 Nginx 日志
docker-compose logs nginx

# 检查端口占用
sudo netstat -tlnp | grep 80
sudo netstat -tlnp | grep 443
```

### 内存不足

```bash
# 查看容器资源使用
docker stats

# 清理未使用的镜像和容器
docker system prune -a
```

---

## 📈 性能优化

### 1. 启用 Redis 缓存（可选）

添加到 `docker-compose.yml`：

```yaml
  redis:
    image: redis:7-alpine
    container_name: orion-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
```

### 2. Nginx 缓存优化

已在 `nginx/conf.d/jojobuy.conf` 中配置：
- 静态资源缓存 7 天
- Next.js 静态文件缓存 1 年

### 3. 数据库连接池

已在后端配置（application.yml）：
- 最大连接数: 20
- 最小空闲连接: 10

---

## 🎉 部署完成检查清单

- [ ] 域名已解析到服务器
- [ ] SSL 证书已配置
- [ ] 环境变量已配置（.env）
- [ ] 所有容器运行正常
- [ ] 网站可以正常访问
- [ ] 管理员可以登录后台
- [ ] 数据库备份已配置
- [ ] 防火墙已配置
- [ ] SSL 自动续期已配置

---

## 📞 获取帮助

如果遇到问题：

1. 查看日志: `docker-compose logs -f`
2. 检查配置: `docker-compose config`
3. 重启服务: `docker-compose restart`
4. 查看本文档的故障排查章节

祝部署顺利！🎊
