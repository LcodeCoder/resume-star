# Multi-stage build for resume-lcode
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline
COPY backend/src ./src
RUN mvn clean package -DskipTests

FROM node:18-alpine AS frontend-build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 安装 nginx 用于前端
RUN apk add --no-cache nginx

# 拷贝后端 jar
COPY --from=backend-build /app/target/*.jar app.jar

# 拷贝前端构建产物到 nginx 目录
COPY --from=frontend-build /app/dist /usr/share/nginx/html

# 配置 nginx
RUN echo 'server { \
    listen 80; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    location /api { \
        proxy_pass http://localhost:8080; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto $scheme; \
    } \
}' > /etc/nginx/http.d/default.conf

# 创建数据目录
RUN mkdir -p /app/data && chmod 755 /app/data

# 启动脚本
COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80
VOLUME ["/app/data"]

ENTRYPOINT ["/docker-entrypoint.sh"]
