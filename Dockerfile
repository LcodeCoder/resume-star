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

# 配置 nginx（使用 frontend/nginx.conf 替代内联配置，便于维护）
COPY frontend/nginx.conf /etc/nginx/http.d/default.conf

# 创建数据目录
RUN mkdir -p /app/data && chmod 755 /app/data

# 启动脚本
COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80 8080
VOLUME ["/app/data"]

ENTRYPOINT ["/docker-entrypoint.sh"]
