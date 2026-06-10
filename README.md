# resume-lcode

resume-lcode 是一个基于 **Spring Boot 3 + Vue3 + Vite + Element Plus** 的智能简历制作与 AI 优化平台，面向求职用户提供可视化简历编辑、模板库、一键导出、AI 润色/纠错/打分/岗位适配，以及完整会员体系预留能力。

## 核心能力

- 可视化拖拽简历编辑器：组件库、画布编辑、样式面板、草稿保存、预览导出入口。
- 多行业模板库：分类筛选、模板预览、一键套用、收藏/浏览统计字段预留。
- AI 智能优化：所有 AI 请求由后端代理，前端不暴露 API Key；未配置密钥时自动使用本地模拟响应。
- 会员体系预留：会员等级、套餐、权益、额度、权限接口与页面骨架完整保留，当前不做强制拦截。
- 后台管理概览：用户、简历、模板、AI 调用、会员套餐等统计入口。

## 目录结构

```text
resume-lcode
├── backend   # Spring Boot 后端服务
├── frontend  # Vue3 + Vite 前端应用
├── docs      # 接口、部署、数据库说明
└── 任务书     # 原始需求文档
```

## 后端结构

```text
backend/src/main/java/com/resume
├── ai          # AI 请求、Prompt、配置属性
├── common      # 统一响应、错误码、枚举
├── config      # 跨域、Swagger 配置
├── controller  # REST 接口层
├── entity      # 实体、请求对象、返回对象
├── exception   # 全局异常与会员/额度预留异常
├── mapper      # MyBatis Plus 数据访问接口骨架
├── repository  # 零数据库演示用内存数据仓库
├── service     # 业务接口
├── service/impl# 业务实现
└── util        # 会员权限/额度校验预留工具
```

## 前端结构

```text
frontend/src
├── api                  # Axios 封装和模块接口
├── components           # 公共组件
│   ├── drag-resume      # 简历编辑器画布组件
│   └── member-tip       # 会员升级弹窗预留组件
├── layout               # 主布局
├── router               # 路由配置
├── store                # Pinia 状态管理
├── style                # 全局样式
└── views                # 首页、编辑器、模板库、个人中心、后台、会员中心
```

## 本地启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

默认后端地址：`http://localhost:8080/api`  
接口文档：`http://localhost:8080/api/swagger-ui.html`

> 当前为了便于零配置启动，后端默认排除了数据源自动配置，并以内存数据演示核心流程。接入 MySQL 时，按 `backend/src/main/resources/db/schema.sql` 建表后补充 `spring.datasource` 配置即可。

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：`http://localhost:5173`

## AI 安全说明

- API Key、接口地址、模型参数仅由后端配置和读取。
- 前端只调用后端 `/ai/*` 接口，不直连模型厂商。
- 后端 `com.resume.ai` 模块已封装 Prompt 构造、HTTP 请求、响应解析和本地 mock 回退。

可选环境变量：

```bash
export RESUME_AI_ENDPOINT="https://your-model-gateway/v1/chat/completions"
export RESUME_AI_API_KEY="your-api-key"
export RESUME_AI_MODEL="resume-optimizer"
```

## 会员预留说明

当前系统不实现支付、扣费、强制权限拦截；但已经在实体、接口、前端状态、页面、样式中预留：

- 会员等级、会员到期时间。
- AI 次数、导出次数、模板权限、功能额度。
- 会员套餐、权益说明、升级弹窗。
- 权限校验工具和额度校验方法骨架。

## 构建验证

```bash
cd backend && mvn -DskipTests package
cd ../frontend && npm install && npm run build
```

## 文档

- [接口说明](docs/api.md)
- [数据库说明](docs/database.md)
- [部署说明](docs/deploy.md)
