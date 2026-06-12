# 智能简历制作平台 resume-lcode

基于 **Spring Boot 3 + Vue3 + Element Plus** 的智能简历制作与 AI 优化平台，提供可视化编辑、模板库、AI 优化、会员体系等完整功能。

## ✨ 核心功能

- **可视化编辑器**：拖拽组件、实时预览、自定义样式
- **多模板库**：按行业分类，一键套用，收藏/浏览统计
- **AI 智能优化**：简历润色、纠错、打分、岗位适配
- **会员体系**：套餐管理、额度控制、兑换码系统
- **社区分享**：优秀案例展示、技巧文章、简历投稿
- **后台管理**：用户管理、模板管理、数据统计、系统配置

## 🚀 快速开始（Docker 一键部署）

**环境要求**：Docker 和 Docker Compose

```bash
# 克隆项目
git clone <repo-url>
cd resume-lcode

# 方式一：一行命令启动（推荐）
./start.sh

# 方式二：手动启动
docker compose up -d --build
```

**访问地址**：
- 前端：http://localhost
- 后端接口文档：http://localhost/api/swagger-ui.html
- 默认账号：
  - 普通用户：`demo / demo123`
  - 管理员：`admin / admin123`

**数据持久化**：所有数据存储在 `./data/resume-lcode.db`（SQLite），重启容器不丢失。

**常用命令**：
```bash
docker compose logs -f         # 查看日志
docker compose restart         # 重启服务
docker compose down            # 停止（保留数据）
docker compose down -v         # 停止并清空数据（慎用）
```

## 🔧 本地开发

### 后端

```bash
cd backend
mvn spring-boot:run
```

- 地址：http://localhost:8080
- 接口文档：http://localhost:8080/api/swagger-ui.html

### 前端

```bash
cd frontend
npm install
npm run dev
```

- 地址：http://localhost:5173

## 🤖 AI 配置（可选）

后端默认使用模拟响应，接入真实 AI 需配置环境变量：

**方式一：Docker 部署**

根目录创建 `.env` 文件：
```bash
RESUME_AI_ENDPOINT=https://api.openai.com/v1/chat/completions
RESUME_AI_API_KEY=sk-your-api-key
RESUME_AI_MODEL=gpt-4
```

**方式二：本地开发**

```bash
export RESUME_AI_ENDPOINT="https://api.openai.com/v1/chat/completions"
export RESUME_AI_API_KEY="sk-your-api-key"
export RESUME_AI_MODEL="gpt-4"
```

或登录管理后台在「系统设置」中配置（持久化到数据库）。

## 📁 目录结构

```
resume-lcode/
├── backend/              # Spring Boot 后端
│   ├── src/main/java/com/resume/
│   │   ├── controller/  # REST 接口
│   │   ├── service/     # 业务逻辑
│   │   ├── repository/  # 数据访问
│   │   ├── entity/      # 实体类
│   │   └── ai/          # AI 功能
│   └── src/main/resources/
│       └── db/          # 数据库脚本
├── frontend/            # Vue3 前端
│   ├── src/
│   │   ├── views/      # 页面
│   │   ├── components/ # 组件
│   │   ├── api/        # 接口
│   │   └── store/      # 状态管理
│   └── public/
├── Dockerfile           # Docker 构建
├── docker-compose.yml   # 编排配置
└── README.md
```

## 🎯 主要技术栈

**后端**：
- Spring Boot 3.3
- MyBatis Plus
- SQLite（可切换 MySQL）
- SpringDoc OpenAPI

**前端**：
- Vue 3 + Vite
- Element Plus
- Pinia
- Axios

## 📝 功能说明

### 用户端
- 简历编辑器：拖拽组件、自定义样式、实时预览
- 模板库：分类筛选、模板预览、一键套用
- AI 优化：润色、纠错、打分、岗位适配
- 个人中心：简历管理、草稿箱、收藏、操作记录
- 会员中心：套餐购买、兑换码兑换、权益查看
- 社区：优秀案例浏览、技巧文章、简历投稿

### 管理端
- 数据统计：用户、简历、AI 调用、订单概览
- 用户管理：查看、编辑、会员调整
- 模板管理：分类管理、模板编辑、VIP 标识
- 会员管理：套餐配置、兑换码生成
- 社区管理：投稿审核、案例管理、文章管理
- 系统设置：AI 配置、邮件配置、支付配置

## 🔒 安全说明

- API Key 仅存储在后端，前端不直接调用第三方 API
- 用户密码使用 BCrypt 加密存储
- 所有敏感配置支持环境变量或后台配置

## 📦 构建打包

```bash
# 后端打包
cd backend
mvn clean package -DskipTests

# 前端打包
cd frontend
npm run build
```

产物：
- 后端：`backend/target/*.jar`
- 前端：`frontend/dist/`

## 🔄 数据迁移

数据文件位于 `./data/resume-lcode.db`（Docker 部署）或 `backend/data/resume-lcode.db`（本地开发）。

**备份**：
```bash
cp ./data/resume-lcode.db ./data/resume-lcode.backup.db
```

**迁移**：直接复制 `.db` 文件到新环境的 `./data/` 目录。

## 💡 提示

1. **首次部署后**请立即修改管理员密码
2. **AI 功能**需配置真实 API，否则使用模拟响应
3. **邮件功能**需在后台配置 SMTP 信息
4. **支付功能**需在后台配置支付接口（可选）

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
