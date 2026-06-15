# 智能简历制作平台 resume-lcode

基于 **Spring Boot 3 + Vue 3 + Element Plus** 的智能简历制作与 AI 优化平台，集可视化编辑、模板库、AI 优化、模拟面试、会员/额度体系、社区互动与后台运营于一体。

## ✨ 核心功能

- **可视化编辑器**：拖拽组件、实时预览、自定义样式、草稿/版本管理
- **多模板库**：分类筛选、详情预览、一键套用、收藏与浏览统计
- **AI 智能优化**：简历润色、纠错、打分、岗位适配
- **模拟面试**：AI 面试官多轮问答，会员套餐每日次数 + 额度双重判定
- **沉浸式语音面试**：AI 面试官语音提问（云端神经网络音色 / 浏览器本地 TTS）+ 语音作答（实时转写），全程免手操对话，结束给出含表达力分析的评测报告
- **会员体系**：套餐管理、权益配置、订单流水
- **额度系统（充值卡模式）**：额度套餐、兑换码批量生成/导出、额度流水
- **社区分享**：优秀案例展示、技巧文章、用户投稿（自动脱敏 + 审核）、点赞
- **站内通知**：通知铃铛、公告弹窗、未读提醒
- **后台管理**：用户/模板/会员/订单/社区/面试/公告/系统多模块统一运营

## 🚀 快速开始（Docker 一键部署）

**环境要求**：Docker 与 Docker Compose

```bash
# 克隆项目
git clone <repo-url>
cd resume-lcode

# 方式一：一键脚本启动（推荐）
./start.sh

# 方式二：手动启动
docker compose up -d --build
```

**访问地址**：
- 前端：http://localhost:9998
- 后端 API：http://localhost:9999/api
- 接口文档：http://localhost:9999/api/swagger-ui.html
- 默认账号：
  - 普通用户：`demo / demo123`
  - 管理员：`admin / admin123`（登录后请第一时间修改密码）

**数据持久化**：所有数据存储在 `./data/resume-lcode.db`（SQLite），容器重启不丢失。

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

也可登录管理后台在「系统设置 → AI 配置」中配置（持久化到数据库，优先级高于环境变量）。

## 📁 目录结构

```
resume-lcode/
├── backend/                       # Spring Boot 后端
│   ├── src/main/java/com/resume/
│   │   ├── controller/           # REST 接口（含 Admin/AI/Interview/Member/Community/SiteStats 等）
│   │   ├── service/              # 业务逻辑（QuotaService、InterviewService、MemberService…）
│   │   ├── repository/ mapper/   # 数据访问
│   │   ├── entity/               # 实体类
│   │   ├── ai/                   # AI 调用与适配
│   │   ├── member/               # 会员/额度领域
│   │   ├── common/ config/ util/ # 通用组件
│   │   └── exception/            # 全局异常
│   └── src/main/resources/db/    # 数据库脚本与迁移
├── frontend/                     # Vue 3 前端
│   ├── src/
│   │   ├── views/                # 页面（Editor/Templates/Member/Interview/Community/Profile…）
│   │   ├── views/admin/          # 后台 Tab 页面
│   │   ├── components/           # 通用组件（通知铃铛、公告弹窗等）
│   │   ├── api/                  # 接口封装
│   │   └── store/                # 状态管理（Pinia）
│   └── public/
├── Dockerfile                    # 单容器构建（前后端合并）
├── docker-compose.yml            # 端口：9998 前端 / 9999 后端
├── start.sh                      # 一键启动脚本
└── README.md
```

## 🎯 主要技术栈

**后端**：
- Spring Boot 3.3
- MyBatis Plus
- SQLite（可切换 MySQL）
- SpringDoc OpenAPI
- BCrypt + Session 鉴权

**前端**：
- Vue 3 + Vite
- Element Plus
- Pinia
- Axios

## 📝 功能说明

### 用户端
- **简历编辑器**：拖拽组件、自定义样式、实时预览、草稿/历史版本
- **模板库**：分类筛选、详情预览、一键套用、收藏统计
- **AI 优化**：润色、纠错、打分、岗位适配（消耗额度）
- **模拟面试**：AI 多轮面试，按会员套餐控制每日次数 + 消耗额度
- **沉浸式语音面试**：可在管理端开关；面试官语音提问、候选人语音作答（实时转写），默认「免手操」——说完停顿自动提交、自动进入下一题；支持云端神经网络音色（晓晓 / 云希等）与浏览器本地音色切换、说话动画与录音波形；结束后语音播报评测，并给出语速 / 口头禅 / 作答完整度的「表达力分析」。单场消耗额度、时长、是否高音质均可在后台配置
- **个人中心**：简历管理、草稿箱、收藏、操作记录、额度流水
- **会员中心**：套餐购买、兑换码兑换、额度套餐充值、权益查看
- **社区**：优秀案例浏览、技巧文章、用户投稿（自动脱敏 + 审核）、点赞互动
- **通知**：站内消息铃铛、未读提醒、公告弹窗

### 管理端
- **数据看板**：用户/简历/AI 调用/订单/营收/站点访问统计
- **用户管理**：查看、编辑、会员等级与额度调整
- **模板管理**：分类管理、模板编辑、VIP 标识
- **会员管理**：套餐配置、每日面试次数、兑换码生成
- **额度管理**：额度套餐、兑换码批量生成 / 导出
- **订单管理**：会员订单、额度充值流水
- **社区管理**：投稿审核、案例管理、文章管理
- **面试管理**：面试记录与统计；可配置面试时长、题量、每日次数、AI 人设；沉浸式语音面试开关 / 单场消耗额度 / 时长 / 云端语音 API Key / 高音质
- **公告管理**：站内公告发布与回收
- **系统设置**：AI 配置、邮件配置、支付配置

## 🔒 安全说明

- API Key 仅存储在后端，前端不直接调用第三方 API
- 用户密码使用 BCrypt 加密存储
- 用户投稿自动脱敏 + 人工审核后发布
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
3. **模拟面试 / AI 优化**会消耗额度，可在后台调整套餐配置与额度规则
4. **邮件功能**需在后台配置 SMTP 信息
5. **支付功能**需在后台配置支付接口（可选）

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
