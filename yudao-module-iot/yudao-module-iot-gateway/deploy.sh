#!/bin/bash
set -e

## yudao-module-iot-gateway（IoT 网关，独立 Spring Boot 应用）生产部署脚本（prod profile）
## ⚠️ 本项目特有，官方《Docker 部署》文档未覆盖，需额外部署
##
## 前置：
## ① 项目根目录执行 mvn clean package -Dmaven.test.skip=true，产出 target/yudao-module-iot-gateway.jar
## ② 本目录执行 docker build -t iot-gateway .
## ③ 中间件已启动：Redis(6379)、EMQX(1883)、RocketMQ(9876)
## ④ EMQX 已配置 HTTP 认证/ACL/Webhook 指向本机 8090
## ⑤ 复制 deploy.env.example 为 deploy.env 并填写真实密码（切勿提交 Git！）
##
## 说明：--network=host → 容器内 127.0.0.1 直连宿主机 EMQX/Redis/RocketMQ；
##       密钥（EMQX 账号密码、Token 密钥）通过 --env-file 注入。

ENV_FILE="${ENV_FILE:-./deploy.env}"
if [ ! -f "$ENV_FILE" ]; then
  echo "❌ 缺少环境变量文件 $ENV_FILE"
  echo "   请执行：cp deploy.env.example deploy.env 并填写真实密码"
  exit 1
fi

## 第一步：删除老容器
echo "开始删除 iot-gateway 容器"
docker stop iot-gateway || true
docker rm   iot-gateway || true
echo "完成删除 iot-gateway 容器"

## 第二步：启动新容器
echo "开始启动 iot-gateway 容器"
docker run -d \
  --name iot-gateway \
  --network=host \
  --restart=always \
  --env-file "$ENV_FILE" \
  -e "SPRING_PROFILES_ACTIVE=prod" \
  -e "TZ=Asia/Shanghai" \
  -e 'JAVA_OPTS=-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom' \
  -v /work/projects/iot-gateway/logs:/root/logs \
  iot-gateway
echo "正在启动 iot-gateway 容器，约需 30 秒，可用 docker logs -f iot-gateway 查看"
