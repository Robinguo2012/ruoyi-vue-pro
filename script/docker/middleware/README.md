# yudao 中间件一键部署

在阿里云 ECS（Linux）单机启动 yudao 后端所需的全部中间件。所有服务用 `network_mode: host`，
与 `deploy.sh`（`--network=host`）一致，`yudao-server` / `iot-gateway` 用 `127.0.0.1` 直连，
仓库现成的 `local` profile **无需改动**即可连通。

## 组件与端口

| 服务 | 端口 | 凭据（= local profile 默认） | 说明 |
|------|------|------|------|
| MySQL 8 | 3306 | root / 123456，库 `ruoyi-vue-pro` | 首次启动自动导入 `sql/mysql/ruoyi-vue-pro.sql` |
| Redis 6 | 6379 | 无密码 | AOF 持久化 |
| TDengine | 6041(WS)/6030 | root / taosdata | IoT 时序数据 |
| RocketMQ NameSrv | 9876 | — | — |
| RocketMQ Broker | 10911/8081 | — | `broker.conf` 已设 `brokerIP1=127.0.0.1` |
| EMQX | 1883 / 18083 | Dashboard admin/public | 仅 IoT 网关需要 |

## 使用

```bash
cd script/docker/middleware
docker compose up -d          # 启动全部
docker compose ps             # 查看状态
docker compose logs -f mysql  # 看某个服务日志
docker compose down           # 停止（保留数据卷 ./data）
docker compose down -v        # 停止并删除容器（数据仍在 ./data，需手动 rm 才清空）
```

数据默认落在本目录 `./data/` 下，删库请手动 `rm -rf ./data`。

## 常见事项

- **只部署 admin、不跑 IoT**：删除 `tdengine` 和 `emqx` 两段即可（或把 iot 模块从根 pom 注释后重新打包）。
- **RocketMQ 内存**：已通过 `JAVA_OPT_EXT` 限制堆到 256m/512m，避免默认 8G 撑爆小内存机器。
- **生产安全**：改掉所有默认密码并同步修改 `application-*.yaml`；安全组切勿对公网开放
  3306/6379/9876/6041/18083，仅按需开放 48080、80/443、以及设备接入用的 1883。
- **EMQX 认证/ACL/Webhook**：起来后需在 Dashboard 配置 HTTP Hook 指向网关 8090，设备才能接入。
