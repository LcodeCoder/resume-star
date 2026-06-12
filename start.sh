#!/bin/bash
set -e

echo "🚀 启动 resume-lcode 智能简历制作平台..."
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: Docker 未安装"
    echo "请先安装 Docker Desktop: https://www.docker.com/products/docker-desktop"
    exit 1
fi

# 检查 Docker Compose 是否可用
if ! docker compose version &> /dev/null; then
    echo "❌ 错误: Docker Compose 不可用"
    exit 1
fi

echo "✅ Docker 环境检查通过"
echo ""

# 停止旧容器（如果存在）
if docker ps -a | grep -q resume-lcode; then
    echo "🔄 检测到旧容器，正在停止..."
    docker compose down
    echo ""
fi

# 构建并启动
echo "📦 构建并启动服务（首次启动需要几分钟）..."
docker compose up -d --build

# 等待服务启动
echo ""
echo "⏳ 等待服务启动..."
sleep 10

# 检查容器状态
if docker ps | grep -q resume-lcode; then
    echo ""
    echo "✅ 启动成功！"
    echo ""
    echo "📝 访问地址："
    echo "   前端: http://localhost"
    echo "   后台: http://localhost/admin"
    echo "   接口文档: http://localhost/api/swagger-ui.html"
    echo ""
    echo "👤 默认账号："
    echo "   普通用户: demo / demo123"
    echo "   管理员: admin / admin123"
    echo ""
    echo "💡 提示："
    echo "   - 查看日志: docker compose logs -f"
    echo "   - 停止服务: docker compose down"
    echo "   - 重启服务: docker compose restart"
    echo ""
else
    echo ""
    echo "❌ 启动失败，请查看日志:"
    docker compose logs --tail=50
    exit 1
fi
