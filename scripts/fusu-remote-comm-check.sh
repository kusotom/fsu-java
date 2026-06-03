#!/usr/bin/env bash
# ============================================================================
# fusu-remote-comm-check.sh — FSU 远程通信检查脚本
# ============================================================================
# 用途: 部署到远程服务器后，一键检查平台服务器与现场 FSU 的网络、HTTP/SOAP、
#       B接口只读命令通信是否正常。
#
# 安全边界:
#   - 不执行任何 SET / SET_* / 控制类命令
#   - 不启动 Scheduler
#   - 不修改业务数据库
#   - 不硬编码真实 FSU 密码/token/FSUID
#
# 环境变量:
#   PLATFORM_HOST        平台服务器地址 (默认 127.0.0.1)
#   PLATFORM_PORT        平台端口 (默认 8080)
#   PLATFORM_SC_PATH     SCService 被动上报入口路径 (默认 /services/SCService)
#   FSU_HOST             FSU 地址 (必填，用于 FSU 通信检查)
#   FSU_PORT             FSU 端口 (默认 80)
#   FSU_SERVICE_PATH     FSUService 路径 (默认 /services/FSUService)
#   STATION_NAME         站点名称 (默认 1)
#   FSUID                FSU 标识 (选填)
#   AUTH_TOKEN           平台认证 token (选填)
#   ENABLE_PROTOCOL_PROBE  启用只读协议探测 (默认 false)
#   ENABLE_TCPDUMP       启用抓包 (默认 false)
#   TCPDUMP_INTERFACE    抓包网卡 (默认 any)
#   LOG_DIR              日志目录 (默认 ./logs/remote-fsu-comm)
#   TIMEOUT_SECONDS      超时秒数 (默认 5)
#
# 运行示例:
#   FSU_HOST=192.168.x.x ./scripts/fusu-remote-comm-check.sh
#   FSU_HOST=192.168.x.x ENABLE_PROTOCOL_PROBE=true ./scripts/fusu-remote-comm-check.sh
#
# 退出码: 0=全部通过或跳过, 非0=存在失败项
# ============================================================================

set -Eeuo pipefail

# ---- 默认配置 ----
PLATFORM_HOST="${PLATFORM_HOST:-127.0.0.1}"
PLATFORM_PORT="${PLATFORM_PORT:-8080}"
PLATFORM_SC_PATH="${PLATFORM_SC_PATH:-/services/SCService}"
FSU_HOST="${FSU_HOST:-}"
FSU_PORT="${FSU_PORT:-80}"
FSU_SERVICE_PATH="${FSU_SERVICE_PATH:-/services/FSUService}"
STATION_NAME="${STATION_NAME:-1}"
FSUID="${FSUID:-}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
ENABLE_PROTOCOL_PROBE="${ENABLE_PROTOCOL_PROBE:-false}"
ENABLE_TCPDUMP="${ENABLE_TCPDUMP:-false}"
TCPDUMP_INTERFACE="${TCPDUMP_INTERFACE:-any}"
LOG_DIR="${LOG_DIR:-./logs/remote-fsu-comm}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-5}"

# ---- 全局状态 ----
PASS_COUNT=0
FAIL_COUNT=0
SKIP_COUNT=0
RUN_ID="$(date +%Y%m%d-%H%M%S)"
RUN_LOG_DIR="${LOG_DIR}/${RUN_ID}"

# ---- 辅助函数 ----
init_log_dir() {
    mkdir -p "${RUN_LOG_DIR}"
    exec 3>&1 4>&2
    exec 1> >(tee -a "${RUN_LOG_DIR}/summary.txt") 2>&1
}

log_section() { echo ""; echo "===== $1 ====="; }
log_info()    { echo "[INFO]  $*"; }
log_pass()    { echo "[PASS]  $*"; PASS_COUNT=$((PASS_COUNT + 1)); }
log_fail()    { echo "[FAIL]  $*"; FAIL_COUNT=$((FAIL_COUNT + 1)); }
log_skip()    { echo "[SKIP]  $*"; SKIP_COUNT=$((SKIP_COUNT + 1)); }

save_env() {
    cat > "${RUN_LOG_DIR}/env.txt" <<ENVEOF
RUN_ID=${RUN_ID}
PLATFORM_HOST=${PLATFORM_HOST}
PLATFORM_PORT=${PLATFORM_PORT}
FSU_HOST=${FSU_HOST}
FSU_PORT=${FSU_PORT}
STATION_NAME=${STATION_NAME}
FSUID=${FSUID:-"(not set)"}
TIMEOUT_SECONDS=${TIMEOUT_SECONDS}
ENABLE_PROTOCOL_PROBE=${ENABLE_PROTOCOL_PROBE}
ENABLE_TCPDUMP=${ENABLE_TCPDUMP}
HOSTNAME=$(hostname)
USER=$(whoami)
ENVEOF
}

# ---- 参数解析 ----
while [[ $# -gt 0 ]]; do
    case "$1" in
        --fsu-host) FSU_HOST="$2"; shift 2 ;;
        --fsu-port) FSU_PORT="$2"; shift 2 ;;
        --platform-host) PLATFORM_HOST="$2"; shift 2 ;;
        --platform-port) PLATFORM_PORT="$2"; shift 2 ;;
        --station-name) STATION_NAME="$2"; shift 2 ;;
        --fsuid) FSUID="$2"; shift 2 ;;
        --auth-token) AUTH_TOKEN="$2"; shift 2 ;;
        --enable-probe) ENABLE_PROTOCOL_PROBE=true; shift ;;
        --enable-tcpdump) ENABLE_TCPDUMP=true; shift ;;
        --timeout) TIMEOUT_SECONDS="$2"; shift 2 ;;
        -h|--help) cat <<HELP
用法: $0 [选项]

环境变量/选项:
  PLATFORM_HOST / --platform-host    平台地址 (默认 127.0.0.1)
  PLATFORM_PORT / --platform-port    平台端口 (默认 8080)
  FSU_HOST / --fsu-host              FSU 地址 (必填)
  FSU_PORT / --fsu-port              FSU 端口 (默认 80)
  STATION_NAME                      站点名称 (默认 1)
  FSUID / --fsuid                   FSU 标识
  AUTH_TOKEN / --auth-token         认证 token
  ENABLE_PROTOCOL_PROBE / --enable-probe   启用只读协议探测
  ENABLE_TCPDUMP / --enable-tcpdump        启用 tcpdump 抓包
  TIMEOUT_SECONDS / --timeout        超时秒数 (默认 5)

示例:
  FSU_HOST=192.168.x.x ./scripts/fusu-remote-comm-check.sh
  FSU_HOST=192.168.x.x ENABLE_PROTOCOL_PROBE=true ./scripts/fusu-remote-comm-check.sh

HELP
        exit 0 ;;
        *) echo "未知选项: $1 (使用 -h 查看帮助)"; exit 2 ;;
    esac
done

# ---- 主流程 ----
init_log_dir
save_env

echo "============================================"
echo " FSU 远程通信检查脚本"
echo " 运行 ID: ${RUN_ID}"
echo " 时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo " 日志: ${RUN_LOG_DIR}"
echo "============================================"

# ===================================================================
# A. 本机基础环境检查
# ===================================================================
log_section "A. 本机基础环境检查"

log_info "主机名: $(hostname)"
log_info "用户: $(whoami)"
log_info "IP 地址: $(hostname -I 2>/dev/null || ip addr show 2>/dev/null | grep 'inet ' | awk '{print $2}' || echo '无法获取')"
log_info "当前时间: $(date '+%Y-%m-%d %H:%M:%S %Z')"
log_pass "hostname"
log_pass "current_time"

for cmd in curl nc timeout; do
    if command -v "$cmd" &>/dev/null; then
        log_pass "$cmd 可用 ($(command -v "$cmd"))"
    else
        log_fail "$cmd 不可用 — 请安装: apt install $cmd"
    fi
done

if command -v tcpdump &>/dev/null; then
    log_pass "tcpdump 可用 ($(command -v tcpdump))"
else
    log_skip "tcpdump 不可用 — 抓包功能将跳过"
fi

# ===================================================================
# B. 平台服务检查
# ===================================================================
log_section "B. 平台服务检查"
{
    echo "=== 平台服务检查 ==="
    echo "目标: ${PLATFORM_HOST}:${PLATFORM_PORT}"
} > "${RUN_LOG_DIR}/platform_check.txt"

# B1. TCP 端口连通性
PLATFORM_BASE="http://${PLATFORM_HOST}:${PLATFORM_PORT}"

if nc -z -w "${TIMEOUT_SECONDS}" "${PLATFORM_HOST}" "${PLATFORM_PORT}" 2>/dev/null; then
    echo "TCP ${PLATFORM_HOST}:${PLATFORM_PORT} 连通" >> "${RUN_LOG_DIR}/platform_check.txt"
    log_pass "平台端口 ${PLATFORM_HOST}:${PLATFORM_PORT} TCP 连通"
else
    echo "TCP ${PLATFORM_HOST}:${PLATFORM_PORT} 不通" >> "${RUN_LOG_DIR}/platform_check.txt"
    log_fail "平台端口 ${PLATFORM_HOST}:${PLATFORM_PORT} TCP 不通 — 请检查: 平台是否启动? 防火墙是否放行 ${PLATFORM_PORT}?"
fi

# B2. HTTP 可达性 (HEAD 请求)
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time "${TIMEOUT_SECONDS}" \
    -X HEAD "${PLATFORM_BASE}/" 2>/dev/null || echo "000")
echo "HTTP HEAD ${PLATFORM_BASE}/ → ${HTTP_CODE}" >> "${RUN_LOG_DIR}/platform_check.txt"

if [[ "${HTTP_CODE}" =~ ^[23] ]]; then
    log_pass "平台 HTTP ${PLATFORM_BASE}/ 可达 (HTTP ${HTTP_CODE})"
elif [[ "${HTTP_CODE}" == "404" ]] || [[ "${HTTP_CODE}" == "405" ]]; then
    log_pass "平台 HTTP 可达 (HTTP ${HTTP_CODE}, 正常 — 非根路径可能无映射)"
else
    log_fail "平台 HTTP ${PLATFORM_BASE}/ 不可达 (HTTP ${HTTP_CODE}) — 请检查平台是否已启动"
fi

# B3. SCService 被动上报入口
SC_URL="${PLATFORM_BASE}${PLATFORM_SC_PATH}"
SC_RESP=$(curl -s -o /tmp/fusu_sc_check_${RUN_ID}.txt -w "%{http_code}" --max-time "${TIMEOUT_SECONDS}" \
    -X POST \
    -H "Content-Type: text/xml; charset=utf-8" \
    -H "SOAPAction: \"\"" \
    -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><test/></soap:Body></soap:Envelope>' \
    "${SC_URL}" 2>/dev/null || echo "000")
echo "HTTP POST ${SC_URL} → ${SC_RESP}" >> "${RUN_LOG_DIR}/platform_check.txt"
cat /tmp/fusu_sc_check_${RUN_ID}.txt >> "${RUN_LOG_DIR}/platform_check.txt" 2>/dev/null || true
rm -f /tmp/fusu_sc_check_${RUN_ID}.txt

if [[ "${SC_RESP}" =~ ^[23] ]]; then
    log_pass "平台 SCService ${SC_URL} 可达 (HTTP ${SC_RESP})"
elif [[ "${SC_RESP}" == "500" ]]; then
    # 500 = 服务可达但 SOAP 解析失败 (空测试包), 说明入口在线
    log_pass "平台 SCService ${SC_URL} 入口在线 (HTTP 500 — 预期: 测试包无有效 SOAP)"
else
    log_fail "平台 SCService ${SC_URL} 不可达 (HTTP ${SC_RESP}) — 请检查: 平台路径配置, 防火墙"
fi

# B4. Actuator health (如存在)
HEALTH_URL="${PLATFORM_BASE}/actuator/health"
HEALTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 \
    "${HEALTH_URL}" 2>/dev/null || echo "000")
if [[ "${HEALTH_CODE}" =~ ^[23] ]]; then
    log_pass "平台 actuator/health 可用 (HTTP ${HEALTH_CODE})"
else
    log_skip "平台 actuator/health 不可用 (HTTP ${HEALTH_CODE}) — 项目可能未配置"
fi

# ===================================================================
# C. FSU 网络检查
# ===================================================================
log_section "C. FSU 网络检查"
{
    echo "=== FSU 网络检查 ==="
    echo "目标: ${FSU_HOST}:${FSU_PORT}"
} > "${RUN_LOG_DIR}/fsu_network_check.txt"

if [[ -z "${FSU_HOST}" ]]; then
    log_skip "FSU_HOST 未设置, 跳过所有 FSU 检查"
else
    # C1. DNS/IP 解析
    if host "${FSU_HOST}" &>/dev/null || getent hosts "${FSU_HOST}" &>/dev/null; then
        log_pass "FSU ${FSU_HOST} DNS 解析成功"
    else
        if [[ "${FSU_HOST}" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            log_pass "FSU ${FSU_HOST} 是 IP 地址, 跳过 DNS 解析"
        else
            log_fail "FSU ${FSU_HOST} DNS 解析失败 — 请检查 /etc/hosts 或 DNS 配置"
        fi
    fi

    # C2. TCP 端口连通性
    if nc -z -w "${TIMEOUT_SECONDS}" "${FSU_HOST}" "${FSU_PORT}" 2>/dev/null; then
        echo "TCP ${FSU_HOST}:${FSU_PORT} 连通" >> "${RUN_LOG_DIR}/fsu_network_check.txt"
        log_pass "FSU ${FSU_HOST}:${FSU_PORT} TCP 连通"
    else
        echo "TCP ${FSU_HOST}:${FSU_PORT} 不通" >> "${RUN_LOG_DIR}/fsu_network_check.txt"
        log_fail "FSU ${FSU_HOST}:${FSU_PORT} TCP 不通 — 请检查: FSU 是否在线? 路由/VPN/防火墙?"
    fi

    # C3. HTTP/SOAP 路径连通性
    FSU_URL="http://${FSU_HOST}:${FSU_PORT}${FSU_SERVICE_PATH}"

    # POST 探测 (gSOAP 通常只接受 POST)
    FSU_POST_CODE=$(curl -s -o /tmp/fusu_fsu_post_${RUN_ID}.txt -w "%{http_code}" \
        --max-time "${TIMEOUT_SECONDS}" \
        -X POST \
        -H "Content-Type: text/xml; charset=utf-8" \
        -H "SOAPAction: \"\"" \
        -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><test/></soap:Body></soap:Envelope>' \
        "${FSU_URL}" 2>/dev/null || echo "000")
    echo "HTTP POST ${FSU_URL} → ${FSU_POST_CODE}" >> "${RUN_LOG_DIR}/fsu_network_check.txt"
    cat /tmp/fusu_fsu_post_${RUN_ID}.txt >> "${RUN_LOG_DIR}/fsu_network_check.txt" 2>/dev/null || true
    rm -f /tmp/fusu_fsu_post_${RUN_ID}.txt

    if [[ "${FSU_POST_CODE}" =~ ^[23] ]]; then
        log_pass "FSU FSUService ${FSU_URL} POST 可达 (HTTP ${FSU_POST_CODE})"
    elif [[ "${FSU_POST_CODE}" == "500" ]]; then
        log_pass "FSU FSUService ${FSU_URL} POST 可达 (HTTP 500 — 预期: gSOAP 收到无效包)"
    elif [[ "${FSU_POST_CODE}" == "000" ]]; then
        log_fail "FSU FSUService ${FSU_URL} 无响应 (超时/拒绝连接) — 请检查: FSU_gSOAP 服务是否运行?"
    else
        log_fail "FSU FSUService ${FSU_URL} POST 异常 (HTTP ${FSU_POST_CODE})"
    fi
fi

# ===================================================================
# D. 只读协议探测 (可选)
# ===================================================================
log_section "D. 只读协议探测"
{
    echo "=== 只读协议探测 ==="
} > "${RUN_LOG_DIR}/protocol_probe.txt"

if [[ "${ENABLE_PROTOCOL_PROBE}" != "true" ]]; then
    log_skip "ENABLE_PROTOCOL_PROBE=false, 跳过只读协议探测"
elif [[ -z "${FSU_HOST}" ]]; then
    log_skip "FSU_HOST 未设置, 跳过只读协议探测"
else
    # B接口2016 GET_FSUINFO 请求体 (SOAP RPC/Encoded)
    FSU_CODE="${FSUID:-51051243812345}"
    GET_FSUINFO_BODY='<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:xsd="http://www.w3.org/2001/XMLSchema"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soap:Body>
    <ns1:invoke xmlns:ns1="http://service.fsu.binterface.dcim.com">
      <xmlData>&lt;PK_Type&gt;
  &lt;FSUInfo&gt;
    &lt;FSUID&gt;'${FSU_CODE}'&lt;/FSUID&gt;
    &lt;FSUCode&gt;'${FSU_CODE}'&lt;/FSUCode&gt;
    &lt;StationName&gt;'${STATION_NAME}'&lt;/StationName&gt;
  &lt;/FSUInfo&gt;
&lt;/PK_Type&gt;</xmlData>
      <msgType>1701</msgType>
    </ns1:invoke>
  </soap:Body>
</soap:Envelope>'

    FSU_URL="http://${FSU_HOST}:${FSU_PORT}${FSU_SERVICE_PATH}"
    PROBE_CODE=$(curl -s -o /tmp/fusu_probe_${RUN_ID}.txt -w "%{http_code}" \
        --max-time "$((TIMEOUT_SECONDS * 2))" \
        -X POST \
        -H "Content-Type: text/xml; charset=utf-8" \
        -H "SOAPAction: \"\"" \
        -d "${GET_FSUINFO_BODY}" \
        "${FSU_URL}" 2>/dev/null || echo "000")

    echo "GET_FSUINFO (Code=1701) → HTTP ${PROBE_CODE}" >> "${RUN_LOG_DIR}/protocol_probe.txt"
    cat /tmp/fusu_probe_${RUN_ID}.txt >> "${RUN_LOG_DIR}/protocol_probe.txt" 2>/dev/null || true

    if [[ "${PROBE_CODE}" =~ ^[23] ]]; then
        RESP_BODY=$(cat /tmp/fusu_probe_${RUN_ID}.txt 2>/dev/null || echo "")
        if echo "${RESP_BODY}" | grep -qi "FSUINFO\|ACK\|1702\|CPU\|MEM"; then
            log_pass "GET_FSUINFO 协议探测成功 — FSU 返回有效 SOAP ACK (HTTP ${PROBE_CODE})"
        else
            log_pass "GET_FSUINFO HTTP 可达 (HTTP ${PROBE_CODE}) — SOAP 响应需人工分析"
        fi
    elif [[ "${PROBE_CODE}" == "000" ]]; then
        log_fail "GET_FSUINFO 协议探测无响应 — 请检查 FSU 是否支持 B接口2016 命令"
    else
        log_fail "GET_FSUINFO 协议探测失败 (HTTP ${PROBE_CODE})"
    fi

    rm -f /tmp/fusu_probe_${RUN_ID}.txt
fi

# ===================================================================
# E. tcpdump 辅助抓包 (可选)
# ===================================================================
log_section "E. tcpdump 辅助抓包"

if [[ "${ENABLE_TCPDUMP}" != "true" ]]; then
    log_skip "ENABLE_TCPDUMP=false, 跳过抓包"
elif [[ -z "${FSU_HOST}" ]]; then
    log_skip "FSU_HOST 未设置, 跳过抓包"
elif ! command -v tcpdump &>/dev/null; then
    log_skip "tcpdump 不可用, 跳过抓包 — 手动执行: sudo tcpdump -i any host ${FSU_HOST} -w capture.pcap"
else
    PCAP_FILE="${RUN_LOG_DIR}/tcpdump_${RUN_ID}.pcap"
    if [[ "$(id -u)" -eq 0 ]] || sudo -n true 2>/dev/null; then
        log_info "开始抓包 10 秒: ${FSU_HOST}:${FSU_PORT}, 网卡 ${TCPDUMP_INTERFACE}"
        if [[ "$(id -u)" -eq 0 ]]; then
            timeout 12 tcpdump -i "${TCPDUMP_INTERFACE}" host "${FSU_HOST}" and port "${FSU_PORT}" \
                -w "${PCAP_FILE}" -c 200 2>"${RUN_LOG_DIR}/tcpdump_stderr.txt" || true
        else
            sudo timeout 12 tcpdump -i "${TCPDUMP_INTERFACE}" host "${FSU_HOST}" and port "${FSU_PORT}" \
                -w "${PCAP_FILE}" -c 200 2>"${RUN_LOG_DIR}/tcpdump_stderr.txt" || true
        fi
        if [[ -f "${PCAP_FILE}" ]] && [[ -s "${PCAP_FILE}" ]]; then
            log_pass "tcpdump 完成 — $(wc -c < "${PCAP_FILE}") bytes → ${PCAP_FILE}"
        else
            log_fail "tcpdump 未捕获到数据包 — 请检查 FSU 是否可达"
        fi
    else
        log_skip "无 sudo 权限, 跳过 tcpdump — 手动执行: sudo tcpdump -i ${TCPDUMP_INTERFACE} host ${FSU_HOST} -w ${PCAP_FILE}"
    fi
fi

# ===================================================================
# 汇总
# ===================================================================
log_section "汇总"

TOTAL=$((PASS_COUNT + FAIL_COUNT + SKIP_COUNT))
echo ""
echo "============================================"
echo " 检查完成"
echo "============================================"
echo " 总计: ${TOTAL} 项"
echo " PASS: ${PASS_COUNT}"
echo " FAIL: ${FAIL_COUNT}"
echo " SKIP: ${SKIP_COUNT}"
echo " 日志: ${RUN_LOG_DIR}"
echo "============================================"

{
    echo "TOTAL=${TOTAL}"
    echo "PASS=${PASS_COUNT}"
    echo "FAIL=${FAIL_COUNT}"
    echo "SKIP=${SKIP_COUNT}"
} >> "${RUN_LOG_DIR}/summary.txt"

if [[ "${FAIL_COUNT}" -gt 0 ]]; then
    echo ""
    echo "存在 ${FAIL_COUNT} 项失败，请检查日志: ${RUN_LOG_DIR}"
    exit 1
fi

exit 0
