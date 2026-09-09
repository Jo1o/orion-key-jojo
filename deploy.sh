#!/bin/bash

# ================================
# 一键部署脚本 - jojobuy.top
# ================================

set -e

echo "================================"
echo "Orion Key 自动部署脚本"
echo "域名: jojobuy.top"
echo "================================"
echo ""

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then
    echo "❌ 请使用 root 用户运行此脚本"
    echo "   sudo ./deploy.sh"
    exit 1
fi

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装"
    echo "   安装命令: curl -fsSL https://get.docker.com | bash"
    exit 1
fi

# 检查 Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose 未安装"
    echo "   安装命令: apt install docker-compose-plugin"
    exit 1
fi

echo "✅ Docker 环境检查通过"
echo ""

# 检查配置文件
if [ ! -f .env ]; then
    echo "⚠️  未找到 .env 文件"
    echo "   正在从模板创建..."
    cp .env.example .env

    echo ""
    echo "📝 请编辑 .env 文件配置以下内容："
    echo "   1. DB_PASSWORD (数据库密码)"
    echo "   2. JWT_SECRET (JWT密钥)"
    echo "   3. TGHAO_APP_ID 和 TGHAO_APP_KEY"
    echo ""
    read -p "按回车继续..."
    nano .env
fi

echo "✅ 配置文件检查通过"
echo ""

# 检查 SSL 证书
if [ ! -f nginx/ssl/jojobuy.top.crt ]; then
    echo "⚠️  未找到 SSL 证书"
    echo ""
    echo "请选择 SSL 证书方案："
    echo "1) Let's Encrypt 免费证书（推荐）"
    echo "2) 自签名证书（仅测试用）"
    echo "3) 稍后手动配置"
    read -p "请选择 [1-3]: " ssl_choice

    case $ssl_choice in
        1)
            echo "正在申请 Let's Encrypt 证书..."
            apt update
            apt install -y certbot

            # 停止可能占用 80 端口的服务
            docker-compose down 2>/dev/null || true

            certbot certonly --standalone -d jojobuy.top -d www.jojobuy.top

            mkdir -p nginx/ssl
            cp /etc/letsencrypt/live/jojobuy.top/fullchain.pem nginx/ssl/jojobuy.top.crt
            cp /etc/letsencrypt/live/jojobuy.top/privkey.pem nginx/ssl/jojobuy.top.key
            chmod 644 nginx/ssl/jojobuy.top.crt
            chmod 600 nginx/ssl/jojobuy.top.key

            echo "✅ SSL 证书申请成功"
            ;;
        2)
            echo "生成自签名证书..."
            mkdir -p nginx/ssl
            openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
                -keyout nginx/ssl/jojobuy.top.key \
                -out nginx/ssl/jojobuy.top.crt \
                -subj "/CN=jojobuy.top"
            echo "✅ 自签名证书已生成"
            ;;
        3)
            echo "⚠️  请手动将证书放置到 nginx/ssl/ 目录："
            echo "   - nginx/ssl/jojobuy.top.crt"
            echo "   - nginx/ssl/jojobuy.top.key"
            read -p "完成后按回车继续..."
            ;;
    esac
fi

echo ""
echo "================================"
echo "开始构建 Docker 镜像..."
echo "================================"

# 构建镜像
docker-compose build

echo ""
echo "================================"
echo "启动服务..."
echo "================================"

# 启动服务
docker-compose up -d

echo ""
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "================================"
echo "服务状态检查"
echo "================================"

docker-compose ps

echo ""
echo "================================"
echo "🎉 部署完成！"
echo "================================"
echo ""
echo "📊 访问信息："
echo "   前台: https://jojobuy.top"
echo "   后台: https://jojobuy.top/admin"
echo "   API: https://jojobuy.top/api"
echo ""
echo "🔐 管理员登录："
echo "   用户名: admin"
echo "   密码: admin123 (首次登录请修改)"
echo ""
echo "📝 常用命令："
echo "   查看日志: docker-compose logs -f"
echo "   重启服务: docker-compose restart"
echo "   停止服务: docker-compose down"
echo ""
echo "📖 详细文档: 查看 DEPLOY.md"
echo ""

# 显示容器日志
echo "正在显示服务日志（Ctrl+C 退出）..."
sleep 3
docker-compose logs -f
