#!/usr/bin/env bash
# ==============================================================================
# 本地依赖健康检查 —— 启动 YudaoServerApplication / IotGatewayServerApplication 前先跑
#
#   用法：  bash script/check-local-deps.sh
#
# 检查 5 个后端服务是否「真的可用」（不仅端口通，还验证后端进程/认证）：
#   MySQL(3306) / Redis(6379) / RocketMQ(9876) / EMQX(1883) / TDengine(6041)
#
# 重点覆盖两个易踩的坑：
#   - TDengine：taosadapter(6041) 活着但 taosd 挂了 → 端口通但查询 auth failure
#   - EMQX：容器重启后内置库 admin 用户丢失 → 端口通但网关桥接认证被拒
# ==============================================================================
set -u

# ---------- 可按需修改的连接参数 ----------
MYSQL_HOST=127.0.0.1;    MYSQL_PORT=3306;  MYSQL_USER=root;  MYSQL_PASS=123456;  MYSQL_DB="ruoyi-vue-pro"
REDIS_HOST=127.0.0.1;    REDIS_PORT=6379
ROCKETMQ_HOST=127.0.0.1; ROCKETMQ_PORT=9876
EMQX_HOST=127.0.0.1;     EMQX_MQTT_PORT=1883;  EMQX_DASH_PORT=18083
EMQX_BRIDGE_USER=admin;  EMQX_BRIDGE_PASS=ghb420117   # 网关 emqx-1 桥接账号，须为 EMQX 内置库 superuser
TD_HOST=127.0.0.1;       TD_PORT=6041;  TD_USER=root;  TD_PASS=taosdata;  TD_DB=ruoyi_vue_pro
# ------------------------------------------

GREEN=$'\033[32m'; RED=$'\033[31m'; YELLOW=$'\033[33m'; DIM=$'\033[2m'; RST=$'\033[0m'
FAIL=0
ok()   { printf "  ${GREEN}✔${RST} %s\n" "$1"; }
bad()  { printf "  ${RED}✘ %s${RST}\n" "$1"; FAIL=1; }
warn() { printf "  ${YELLOW}!${RST} %s\n" "$1"; }
hint() { printf "    ${DIM}↳ %s${RST}\n" "$1"; }

# 端口探测（优先 nc，退化到 bash /dev/tcp）
port_open() {
  local host=$1 port=$2
  if command -v nc >/dev/null 2>&1; then
    nc -z -w 2 "$host" "$port" >/dev/null 2>&1
  else
    timeout 2 bash -c "echo > /dev/tcp/$host/$port" >/dev/null 2>&1
  fi
}

echo "════════ 本地依赖健康检查 ════════"

# ---------- MySQL ----------
echo "▸ MySQL  (${MYSQL_HOST}:${MYSQL_PORT})"
if port_open "$MYSQL_HOST" "$MYSQL_PORT"; then
  if command -v mysql >/dev/null 2>&1; then
    if mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASS" -e "USE \`$MYSQL_DB\`;" >/dev/null 2>&1; then
      ok "端口通、认证成功、库 $MYSQL_DB 可访问"
    else
      bad "端口通但认证/选库失败（检查 用户名/密码/库名 $MYSQL_DB）"
    fi
  else
    ok "端口通 ${DIM}(未装 mysql 客户端，跳过认证校验)${RST}"
  fi
else
  bad "端口不通 —— MySQL 未启动"; hint "启动 MySQL 后重试"
fi

# ---------- Redis ----------
echo "▸ Redis  (${REDIS_HOST}:${REDIS_PORT})"
if port_open "$REDIS_HOST" "$REDIS_PORT"; then
  if command -v redis-cli >/dev/null 2>&1; then
    if [ "$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping 2>/dev/null)" = "PONG" ]; then
      ok "端口通、PING=PONG"
    else
      warn "端口通但 PING 无响应（可能设了密码）"
    fi
  else
    ok "端口通 ${DIM}(未装 redis-cli，跳过 PING)${RST}"
  fi
else
  bad "端口不通 —— Redis 未启动"
fi

# ---------- RocketMQ（非致命：producer 懒加载，未启动只打日志不阻塞启动）----------
echo "▸ RocketMQ NameServer (${ROCKETMQ_HOST}:${ROCKETMQ_PORT})"
if port_open "$ROCKETMQ_HOST" "$ROCKETMQ_PORT"; then
  ok "NameServer 端口通"
else
  warn "端口不通 —— RocketMQ 未启动（不阻塞启动，但用到 MQ 的功能会失效）"
fi

# ---------- EMQX ----------
echo "▸ EMQX   (mqtt:${EMQX_MQTT_PORT} dashboard:${EMQX_DASH_PORT})"
if port_open "$EMQX_HOST" "$EMQX_MQTT_PORT"; then
  if command -v python3 >/dev/null 2>&1; then
    RC=$(python3 - "$EMQX_HOST" "$EMQX_MQTT_PORT" "$EMQX_BRIDGE_USER" "$EMQX_BRIDGE_PASS" <<'PY'
import socket,struct,sys
host,port,user,pw=sys.argv[1],int(sys.argv[2]),sys.argv[3],sys.argv[4]
def es(s): b=s.encode(); return struct.pack("!H",len(b))+b
def el(n):
    o=b""
    while True:
        d=n%128;n//=128
        if n>0:d|=0x80
        o+=bytes([d])
        if n==0:break
    return o
try:
    vh=es("MQTT")+bytes([0x04,0xC2])+struct.pack("!H",60)
    rem=vh+es("dep-healthcheck")+es(user)+es(pw)
    pkt=bytes([0x10])+el(len(rem))+rem
    s=socket.create_connection((host,port),timeout=5);s.sendall(pkt)
    r=s.recv(4);s.close()
    print(r[3] if len(r)>=4 and r[0]==0x20 else 99)
except Exception:
    print(98)
PY
)
    case "$RC" in
      0) ok "MQTT 端口通，桥接账号 ${EMQX_BRIDGE_USER} 认证成功 (CONNACK 0)";;
      4) bad "认证失败：用户名或密码错 (CONNACK 4)"; hint "EMQX 内置库 ${EMQX_BRIDGE_USER} 密码 ≠ ${EMQX_BRIDGE_PASS}";;
      5) bad "认证失败：未授权 (CONNACK 5) —— 内置库很可能丢了 ${EMQX_BRIDGE_USER} 用户"
         hint "容器重启后 admin 常丢失，需重新添加 superuser（见项目记忆 local-dev-infra）";;
      98) bad "MQTT 连接异常（端口通但握手失败）";;
      *) warn "MQTT 返回码 $RC，请人工确认";;
    esac
  else
    ok "MQTT 端口通 ${DIM}(无 python3，跳过认证校验)${RST}"
  fi
  port_open "$EMQX_HOST" "$EMQX_DASH_PORT" && ok "Dashboard(${EMQX_DASH_PORT}) 端口通" || warn "Dashboard(${EMQX_DASH_PORT}) 端口不通"
else
  bad "MQTT 端口不通 —— EMQX 未启动"
fi

# ---------- TDengine ----------
echo "▸ TDengine (${TD_HOST}:${TD_PORT})"
if port_open "$TD_HOST" "$TD_PORT"; then
  if command -v curl >/dev/null 2>&1; then
    RESP=$(curl -s -u "$TD_USER:$TD_PASS" "http://$TD_HOST:$TD_PORT/rest/sql" -d 'show databases' 2>/dev/null)
    if echo "$RESP" | grep -q '"code":0'; then
      if echo "$RESP" | grep -q "\"$TD_DB\""; then
        ok "taosadapter+taosd 正常，库 $TD_DB 存在"
      else
        warn "TDengine 正常但库 $TD_DB 不存在"; hint "首次需创建库：CREATE DATABASE $TD_DB;"
      fi
    else
      bad "端口通但查询失败 —— taosadapter 活着但 taosd 挂了（经典坑）"
      hint "解决：docker restart tdengine-tsdb"
      hint "返回：$RESP"
    fi
  else
    ok "端口通 ${DIM}(无 curl，跳过 taosd 校验)${RST}"
  fi
else
  bad "端口不通 —— TDengine 未启动"
fi

echo "═════════════════════════════════"
if [ "$FAIL" -eq 0 ]; then
  printf "${GREEN}全部依赖就绪，可以启动应用 ✅${RST}\n"; exit 0
else
  printf "${RED}存在未就绪的依赖，请先处理上面标 ✘ 的项 ❌${RST}\n"; exit 1
fi
