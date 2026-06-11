#!/bin/sh
set -e

# 启动 nginx
nginx

# 启动 Spring Boot 应用
exec java -jar /app/app.jar
