# 部署说明

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.x（生产接入时需要；本地演示可不启用）

## 后端本地启动

```bash
cd backend
mvn spring-boot:run
```

默认地址：

- API 前缀：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/api/swagger-ui.html`

当前后端默认排除数据源自动配置，使用内存数据仓库演示接口，因此不配置 MySQL 也可以启动。

## 后端构建部署

```bash
cd backend
mvn -DskipTests package
java -jar target/resume-lcode-backend-1.0.0.jar
```

## AI 环境变量

AI 密钥、接口地址和模型参数仅由后端读取，前端不保存密钥。

```bash
export RESUME_AI_ENDPOINT="https://your-model-gateway/v1/chat/completions"
export RESUME_AI_API_KEY="your-api-key"
export RESUME_AI_MODEL="resume-optimizer"
```

未配置 `RESUME_AI_ENDPOINT` 或 `RESUME_AI_API_KEY` 时，后端自动使用本地模拟 AI 响应，便于演示和开发。

## 前端本地启动

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

Vite 已配置 `/api` 代理到 `http://localhost:8080`，本地联调时先启动后端再启动前端。

## 前端构建部署

```bash
cd frontend
npm install
npm run build
```

构建产物位于：`frontend/dist`。

## Nginx 示例

```nginx
server {
  listen 80;
  server_name resume-lcode.example.com;

  location / {
    root /var/www/resume-lcode;
    try_files $uri $uri/ /index.html;
  }

  location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

## 生产接入 MySQL

1. 执行 `backend/src/main/resources/db/schema.sql` 与 `data.sql`。
2. 配置 `spring.datasource`。
3. 移除启动类中的数据源自动配置排除。
4. 将当前内存仓库替换为 Mapper 数据访问实现。

## 会员体系上线前注意事项

当前会员体系仅预留：

- 字段：会员等级、到期时间、AI 额度、导出额度、会员模板标识。
- 接口：套餐列表、权限校验工具、额度校验工具。
- 页面：会员中心、升级弹窗、会员标签。

上线真实付费体系前，需要补充支付订单、支付回调、权限拦截、额度扣减、防刷限流和审计日志。
