## IoT 网关 Dockerfile（参考 yudao-server/Dockerfile 写法）
FROM eclipse-temurin:8-jre

## 创建目录，并使用它作为工作目录
RUN mkdir -p /iot-gateway
WORKDIR /iot-gateway
## 将网关项目的 Jar 文件，复制到镜像中
COPY ./target/yudao-module-iot-gateway.jar app.jar

## 设置 TZ 时区
ENV TZ=Asia/Shanghai
## 设置 JAVA_OPTS 环境变量，可通过 docker run -e "JAVA_OPTS=" 进行覆盖
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

## 应用参数（覆盖配置：EMQX、Redis、RocketMQ 等）
ENV ARGS=""

## 暴露网关端口：8090(EMQX HTTP Hook)、8091/8092/8093/8094(各协议)、1883(MQTT)、5683(CoAP)、48080(默认)
EXPOSE 8090 8091 8092 8093 8094 1883 5683

## 启动网关项目
CMD java ${JAVA_OPTS} -jar app.jar $ARGS
