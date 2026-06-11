# syntax=docker/dockerfile:1
# resume-lcode 后端镜像：多阶段构建（Maven 打包 → JRE 运行）
# 数据持久化：SQLite 单文件库写入 /app/data（由 docker-compose 挂载数据卷，重启不丢）

# ---------- 构建阶段：用 Maven 打出可执行 jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# 先拷贝 pom 预拉依赖，利用缓存层加速后续构建
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- 运行阶段：仅带 JRE，体积更小 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
# 数据目录（SQLite 库文件位置），与 docker-compose 的数据卷对应
RUN mkdir -p /app/data
ENV RESUME_DATA_DIR=/app/data
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
