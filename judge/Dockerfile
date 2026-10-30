# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /opt/wangoj

# su-exec：entrypoint 以 root 准备挂载目录后，降权到 app 运行
RUN apk add --no-cache su-exec \
    && addgroup -S app && adduser -S -G app app

COPY --from=build /build/target/judge.jar /opt/wangoj/judge.jar
COPY docker-entrypoint.sh /opt/wangoj/docker-entrypoint.sh
RUN chmod +x /opt/wangoj/docker-entrypoint.sh \
    && mkdir -p /opt/wangoj/source \
    && chown -R app:app /opt/wangoj

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

EXPOSE 8122

# 健康检查用镜像自带的 wget。
# 注意：运行镜像是 alpine，/bin/sh 是 busybox ash，**不支持** /dev/tcp 重定向——
# 写成 `exec 3<>/dev/tcp/...` 会得到 "can't create /dev/tcp/...: nonexistent directory"，
# 容器会一直 unhealthy。
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q -O - http://127.0.0.1:8122/ping | grep -q success || exit 1

# 以 root 进入 entrypoint（内部立即降权到 app）
ENTRYPOINT ["/opt/wangoj/docker-entrypoint.sh"]
