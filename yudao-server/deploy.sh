#!/bin/bash
set -e

## yudao-server（主服务）生产部署脚本（prod profile）
##
## 前置：
## ① 项目根目录执行 mvn clean package -Dmaven.test.skip=true，产出 target/yudao-server.jar
## ② 本目录执行 docker build -t yudao-server .
## ③ 中间件已启动（见 script/docker/middleware/docker-compose.yml）
## ④ 复制 deploy.env.example 为 deploy.env 并填写真实密码（deploy.env 切勿提交 Git！）
##
## 说明：--network=host → 容器内 127.0.0.1 即宿主机，连通中间件无需改地址；
##       密钥通过 --env-file 注入，不写进镜像、不出现在 docker inspect 的 CMD 里。

ENV_FILE="${ENV_FILE:-./deploy.env}"
if [ ! -f "$ENV_FILE" ]; then
  echo "❌ 缺少环境变量文件 $ENV_FILE"
  echo "   请执行：cp deploy.env.example deploy.env 并填写真实密码"
  exit 1
fi

## 第一步：删除老容器
echo "开始删除 yudao-server 容器"
docker stop yudao-server || true
docker rm   yudao-server || true
echo "完成删除 yudao-server 容器"

## 第二步：启动新容器
echo "开始启动 yudao-server 容器"
docker run -d \
  --name yudao-server \
  --network=host \
  --restart=always \
  --env-file "$ENV_FILE" \
  -e "SPRING_PROFILES_ACTIVE=prod" \
  -e "TZ=Asia/Shanghai" \
  -e 'JAVA_OPTS=-Xms1024m -Xmx1024m -Djava.security.egd=file:/dev/./urandom' \
  -v /work/projects/yudao-server/logs:/root/logs \
  yudao-server
echo "正在启动 yudao-server 容器，约需 60 秒，可用 docker logs -f yudao-server 查看"
