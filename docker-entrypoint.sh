#!/bin/sh
set -e

# 启动 nginx
nginx

# 启动 Spring Boot 应用
# preferIPv4Stack=true：容器常有 IPv6 地址但无 IPv6 路由，Java 默认可能优先连 IPv6
# 导致出站连接（如云端 TTS 调外部 API）connect 超时；强制走 IPv4 规避该问题。
exec java -Djava.net.preferIPv4Stack=true -jar /app/app.jar
