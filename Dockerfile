# 第一阶段：构建阶段
FROM maven:3.9-amazoncorretto-21 AS builder

# 设置工作目录
WORKDIR /app

# 复制pom.xml和src目录
COPY pom.xml .
COPY src ./src

# 构建项目，跳过测试
RUN mvn clean package -DskipTests

# 第二阶段：运行阶段
FROM amazoncorretto:21-alpine

# 更新包索引并安装Node.js和npm
RUN apk update && apk add --no-cache nodejs npm

# 设置工作目录
WORKDIR /app

# 创建非root用户并设置权限
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 复制构建阶段生成的jar文件
COPY --from=builder /app/target/ai-agent-0.0.1-SNAPSHOT.jar ./app.jar

# 更改文件所有权
RUN chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# 暴露端口（微信云托管默认使用8080或80，这里保持原项目的8123端口）
EXPOSE 8123

# 运行应用
CMD ["java", "-jar", "./app.jar"]