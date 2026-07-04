#!/usr/bin/env bash
# ==============================================================================
# Claude Code PreToolUse(Bash) 钩子包装脚本
#
# 作用：当 Claude 通过 Bash 执行的命令"像是在启动 Server/Gateway"时，
#       先跑 script/check-local-deps.sh；依赖未就绪则拦截该启动命令。
#
# 说明：只能拦住"经由 Claude Code Bash 执行"的启动；IntelliJ 绿色三角启动拦不到。
# 输入：stdin 收到 PreToolUse 的 JSON（含 .tool_input.command）
# 输出：未就绪 -> 打印 deny JSON 拦截；就绪 -> 打印一条 additionalContext 放行
# ==============================================================================
set -u

INPUT=$(cat)
CMD=$(printf '%s' "$INPUT" | jq -r '.tool_input.command // ""' 2>/dev/null)

# 仅在"启动应用"类命令上触发；其它 Bash 命令一律放行（静默 exit 0）
if ! printf '%s' "$CMD" | grep -qE 'spring-boot:run|YudaoServerApplication|IotGatewayServerApplication'; then
  exit 0
fi

# 仓库根 = 本脚本所在目录的上两级（script/claude-hooks/ -> repo root）
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CHECK="$ROOT/script/check-local-deps.sh"

if [ ! -x "$CHECK" ] && [ ! -f "$CHECK" ]; then
  # 检查脚本不存在则不阻塞，放行
  exit 0
fi

OUT=$(bash "$CHECK" 2>&1)
RC=$?
# 去掉 ANSI 颜色码，便于在 reason/context 里干净显示
OUT_PLAIN=$(printf '%s' "$OUT" | sed $'s/\033\\[[0-9;]*m//g')

if [ "$RC" -ne 0 ]; then
  REASON=$(printf '本地依赖未就绪，已拦截应用启动。请先处理下列标 ✘ 的项后重试：\n%s' "$OUT_PLAIN")
  jq -n --arg r "$REASON" '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"deny",permissionDecisionReason:$r}}'
  exit 0
fi

jq -n '{hookSpecificOutput:{hookEventName:"PreToolUse",additionalContext:"[deps-check] 本地依赖(MySQL/Redis/EMQX/TDengine)已就绪，放行启动。"}}'
exit 0
