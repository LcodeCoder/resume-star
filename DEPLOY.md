# 🚀 Docker 一键部署指南

## 快速开始

```bash
# 克隆项目
git clone https://github.com/Linyu-H/resume-lcode.git
cd resume-lcode

# 一键启动（首次启动会自动构建，需要 5-10 分钟）
docker compose up -d --build

# 查看启动日志
docker compose logs -f
```

访问 `http://localhost` 或 `http://your-domain.com`

## 首次部署后的必做事项

### 1. 修改管理员密码 🔴 重要
- 访问：`http://localhost/admin/login`
- 默认账号：`admin` / `admin123`
- 登录后立即修改密码

### 2. 配置 AI 功能 🟡 可选
- 进入后台 → 系统配置 → AI 配置
- 填写真实的 API Endpoint 和 API Key
- 例如：
  - Endpoint: `https://api.openai.com/v1/chat/completions`
  - Model: `gpt-4`
  - API Key: `sk-xxx...`

## 常用命令

```bash
# 停止服务（数据保留）
docker compose down

# 重启服务
docker compose restart

# 查看日志
docker compose logs -f

# 更新代码并重新部署
git pull
docker compose up -d --build

# 完全清理（包括数据，谨慎使用）
docker compose down -v
rm -rf data/
```

## 数据持久化

- 数据库文件：`./data/resume-lcode.db`
- 重启容器不会丢失数据
- 建议定期备份 `./data` 目录

## 端口说明

- `80`: Web 访问端口（前端 + 后端 API）
- 如需修改端口，编辑 `docker-compose.yml`：
  ```yaml
  ports:
    - "8080:80"  # 改为 8080 端口访问
  ```

## 生产环境建议

1. **反向代理**：使用 Nginx/Caddy 配置 HTTPS
2. **定期备份**：
   ```bash
   # 每天备份数据库
   cp -r data data_backup_$(date +%Y%m%d)
   ```
3. **监控日志**：
   ```bash
   docker compose logs --tail=100 -f
   ```

## 故障排查

### 容器无法启动
```bash
# 查看详细日志
docker compose logs

# 检查端口占用
lsof -i :80
```

### 数据库权限问题
```bash
# 确保数据目录有写权限
chmod 755 data/
```

### 前端无法访问后端
- 检查容器内网络：`docker compose ps`
- 查看 nginx 配置：`docker exec resume-lcode cat /etc/nginx/http.d/default.conf`

## 联系方式

- GitHub: https://github.com/Linyu-H/resume-lcode
- 客服电话：17742647697
