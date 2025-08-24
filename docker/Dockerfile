# 使用 JDK 17 的基础镜像
FROM openjdk:17-jdk

VOLUME /tmp

CMD ["--server.port=8081"]

# 定义工作目录
WORKDIR /app

# 将 Maven 构建的 JAR 文件复制到容器中
COPY DReaderServer.jar app.jar

# 暴露应用的端口
EXPOSE 8081

# 运行 Spring Boot 应用
ENTRYPOINT ["java", "-jar", "app.jar"]