# syntax=docker/dockerfile:1
# resume-lcode 前端镜像：多阶段构建（Vite 构建 → Nginx 静态托管 + 反向代理 /api 到后端）

# ---------- 构建阶段 ----------
FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---------- 运行阶段：Nginx 托管 dist 并代理 /api ----------
FROM nginx:alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
