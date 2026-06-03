#!/usr/bin/env bash
# ============================================================================
# fsu-standalone-comm-check.sh — FSU 独立通信检查脚本 (无平台依赖)
# ============================================================================
# 在干净远程服务器上直接测试与现场 FSU 的网络、HTTP/SOAP、只读 B接口通信。
# 不依赖 Java/Maven/Spring Boot/PostgreSQL/平台后端。
#
# 只检查:
#   - 本机基础环境
#   - 到 FSU 的 DNS/IP/TCP 连通性
#   - HTTP/SOAP 路径连通性
#   - 可选 B接口只读 SOAP 探测 (GET_FSUINFO / GET_LOGININFO)
#
# 不检查:
#   - 平台 8080 端口
#   - /services/SCService
#   - 数据库
#   - 前端页面
#
# 安全:
#   - 不执行 SET / SET_* / 控制类命令
#   - 不启动 Scheduler
#   - 不访问数据库
#   - 不硬编码 FSUID/token/密码
#
# 环境变量:
#   FSU_HOST             FSU 地址 (必填)
#   FSU_PORT             FSU 端口 (默认 80)
#   FSU_SERVICE_PATH     FSUService 路径 (默认 /services/FSUService)
#   FSU_SCHEME           http 或 https (默认 http)
#   STATION_NAME         站点名称 (默认 1)
#   FSUID                FSU 标识 (选填)
#   ENABLE_SOAP_PROBE    启用只读 SOAP 探测 (默认 false)
#   SOAP_PROBE_COMMAND   探测命令: GET_FSUINFO|GET_LOGININFO (默认 GET_FSUINFO)
#   ENABLE_TCPDUMP       启用抓包 (默认 false)
#   TCPDUMP_INTERFACE    抓包网卡 (默认 any)
#   LOG_DIR              日志目录
#   TIMEOUT_SECONDS      超时秒数 (默认 5)
#
# 运行:
#   FSU_HOST=192.168.x.x ./fsu-standalone-comm-check.sh
#   FSU_HOST=192.168.x.x ENABLE_SOAP_PROBE=true ./fsu-standalone-comm-check.sh
# ============================================================================
set -Eeuo pipefail

# ---- 默认配置 ----
FSU_HOST="${FSU_HOST:-}"
FSU_PORT="${FSU_PORT:-80}"
FSU_SERVICE_PATH="${FSU_SERVICE_PATH:-/services/FSUService}"
FSU_SCHEME="${FSU_SCHEME:-http}"
STATION_NAME="${STATION_NAME:-1}"
FSUID="${FSUID:-}"
ENABLE_SOAP_PROBE="${ENABLE_SOAP_PROBE:-false}"
SOAP_PROBE_COMMAND="${SOAP_PROBE_COMMAND:-GET_FSUINFO}"
ENABLE_TCPDUMP="${ENABLE_TCPDUMP:-false}"
TCPDUMP_INTERFACE="${TCPDUMP_INTERFACE:-any}"
LOG_DIR="${LOG_DIR:-/opt/fsu-tools/standalone-fsu-check/logs}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-5}"

ALLOWED_PROBE_COMMANDS="GET_FSUINFO GET_LOGININFO GET_DATA"
BLOCKED_COMMANDS="SET SET_POINT SET_THRESHOLD SET_FTP SET_LOGININFO SET_FSUREBOOT REBOOT UPGRADE SCHEDULER"

# ---- 全局状态 ----
PASS=0; FAIL=0; SKIP=0; WARN=0
RUN_ID="$(date +%Y%m%d-%H%M%S)"
RUN_LOG_DIR="${LOG_DIR}/${RUN_ID}"

# ---- 辅助 ----
init_log() { mkdir -p "${RUN_LOG_DIR}"; }
log_section() { echo ""; echo "===== $1 ====="; }
log_i() { echo "[INFO]  $*"; }
log_p() { echo "[PASS]  $*"; PASS=$((PASS + 1)); }
log_f() { echo "[FAIL]  $*"; FAIL=$((FAIL + 1)); }
log_s() { echo "[SKIP]  $*"; SKIP=$((SKIP + 1)); }
log_w() { echo "[WARN]  $*"; WARN=$((WARN + 1)); }

save_env() {
    cat > "${RUN_LOG_DIR}/env.txt" <<EOF
RUN_ID=${RUN_ID}
FSU_HOST=${FSU_HOST}
FSU_PORT=${FSU_PORT}
FSU_SERVICE_PATH=${FSU_SERVICE_PATH}
FSU_SCHEME=${FSU_SCHEME}
STATION_NAME=${STATION_NAME}
FSUID=${FSUID:-(not set)}
TIMEOUT=${TIMEOUT_SECONDS}s
SOAP_PROBE=${ENABLE_SOAP_PROBE}
SOAP_COMMAND=${SOAP_PROBE_COMMAND}
TCPDUMP=${ENABLE_TCPDUMP}
HOSTNAME=$(hostname)
USER=$(whoami)
TIME=$(date '+%Y-%m-%d %H:%M:%S %Z')
EOF
}

mask_sensitive() { echo "$1" | sed -E 's/(Authorization|token|password|AUTH_TOKEN)=[^ ]+/\1=***/g'; }

# ---- 参数解析 ----
while [[ $# -gt 0 ]]; do
    case "$1" in
        --fsu-host) FSU_HOST="$2"; shift 2 ;;
        --fsu-port) FSU_PORT="$2"; shift 2 ;;
        --fsu-service-path) FSU_SERVICE_PATH="$2"; shift 2 ;;
        --fsu-scheme) FSU_SCHEME="$2"; shift 2 ;;
        --station-name) STATION_NAME="$2"; shift 2 ;;
        --fsuid) FSUID="$2"; shift 2 ;;
        --enable-soap-probe) ENABLE_SOAP_PROBE=true; shift ;;
        --soap-probe-command) SOAP_PROBE_COMMAND="$2"; shift 2 ;;
        --enable-tcpdump) ENABLE_TCPDUMP=true; shift ;;
        --log-dir) LOG_DIR="$2"; shift 2 ;;
        --timeout) TIMEOUT_SECONDS="$2"; shift 2 ;;
        -h|--help) sed -n '2,35p' "$0"; exit 0 ;;
        *) echo "未知选项: $1 (使用 -h)"; exit 2 ;;
    esac
done

# ---- auto-source env.remote ----
[[ -f "./env.remote" ]] && { set -a; source ./env.remote; set +a; }

init_log
save_env

exec 1> >(tee -a "${RUN_LOG_DIR}/summary.txt") 2>&1

echo "============================================"
echo " FSU 独立通信检查 (无平台依赖)"
echo " 运行 ID: ${RUN_ID}"
echo " 时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo " 日志: ${RUN_LOG_DIR}"
echo "============================================"

# ===== A. 本机基础环境 =====
log_section "A. 本机基础环境"
{
    echo "主机名: $(hostname)"
    echo "用户: $(whoami)"
    echo "时间: $(date '+%Y-%m-%d %H:%M:%S %Z')"
    echo "系统: $(lsb_release -ds 2>/dev/null || cat /etc/os-release 2>/dev/null | grep PRETTY_NAME | cut -d= -f2 | tr -d '"' || echo unknown)"
    echo "内核: $(uname -r)"
    echo "IP: $(hostname -I 2>/dev/null || ip -4 addr show 2>/dev/null | grep -oP 'inet \K[\d.]+' | head -1 || echo unknown)"
    echo "路由: $(ip route show default 2>/dev/null | head -1 || echo none)"
} > "${RUN_LOG_DIR}/system_check.txt"

log_i "主机名: $(hostname)"
log_i "用户: $(whoami)"
log_i "IP: $(hostname -I 2>/dev/null | head -1)"

for cmd in curl nc timeout; do
    if command -v "$cmd" &>/dev/null; then log_p "$cmd 可用"; else log_f "$cmd 不可用 — apt install $cmd"; fi
done
for cmd in tcpdump openssl xmllint; do
    command -v "$cmd" &>/dev/null && log_p "$cmd 可用 (可选)" || log_s "$cmd 不可用 (非强制)"
done

# ===== B. FSU 参数检查 =====
log_section "B. FSU 参数检查"
FSU_URL="${FSU_SCHEME}://${FSU_HOST}:${FSU_PORT}${FSU_SERVICE_PATH}"

if [[ -z "${FSU_HOST}" ]]; then
    log_f "FSU_HOST 未设置 — 请配置环境变量: export FSU_HOST=192.168.x.x"
    echo "FSU_HOST=未设置" > "${RUN_LOG_DIR}/fsu_network_check.txt"
else
    log_p "FSU_HOST=${FSU_HOST}"
    log_i "FSU_PORT=${FSU_PORT}"
    log_i "FSU_URL=${FSU_URL}"
fi

# ===== C. DNS/IP 解析 =====
if [[ -n "${FSU_HOST}" ]]; then
log_section "C. DNS/IP 解析"
{
    echo "目标: ${FSU_HOST}"
} > "${RUN_LOG_DIR}/fsu_network_check.txt"

if [[ "${FSU_HOST}" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    log_p "FSU_HOST 是 IP 地址，跳过 DNS"
else
    if host "${FSU_HOST}" &>/dev/null || getent hosts "${FSU_HOST}" &>/dev/null; then
        log_p "FSU ${FSU_HOST} DNS 解析成功"
    else
        log_f "FSU ${FSU_HOST} DNS 解析失败 — 检查 /etc/hosts 或 DNS"
    fi
fi

# ===== D. TCP 连通性 =====
log_section "D. TCP 连通性"
TCP_OK=false
if nc -vz -w "${TIMEOUT_SECONDS}" "${FSU_HOST}" "${FSU_PORT}" 2>>"${RUN_LOG_DIR}/fsu_network_check.txt"; then
    echo "TCP ${FSU_HOST}:${FSU_PORT} OK" >> "${RUN_LOG_DIR}/fsu_network_check.txt"
    log_p "TCP ${FSU_HOST}:${FSU_PORT} 连通"
    TCP_OK=true
else
    echo "TCP ${FSU_HOST}:${FSU_PORT} FAIL" >> "${RUN_LOG_DIR}/fsu_network_check.txt"
    log_f "TCP ${FSU_HOST}:${FSU_PORT} 不通 — 检查: FSU是否在线? 路由/VPN/防火墙? 端口是否正确?"
fi

# ===== E. HTTP/SOAP 路径探测 =====
log_section "E. HTTP/SOAP 路径探测"
{
    echo "=== HTTP 探测 ==="
    echo "URL: ${FSU_URL}"
} > "${RUN_LOG_DIR}/http_probe.txt"

if [[ "${TCP_OK}" != "true" ]]; then
    log_s "TCP 不通，跳过 HTTP 探测"
else
    # HEAD
    H_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time "${TIMEOUT_SECONDS}" \
        -X HEAD "${FSU_URL}" 2>/dev/null || echo "000")
    echo "HEAD → ${H_CODE}" >> "${RUN_LOG_DIR}/http_probe.txt"
    case "${H_CODE}" in
        200) log_p "FSU HTTP HEAD 返回 200" ;;
        405) log_w "FSU HTTP HEAD 返回 405 — gSOAP 可能只接受 POST，非故障" ;;
        404) log_w "FSU HTTP HEAD 返回 404 — 路径可能不对: ${FSU_SERVICE_PATH}" ;;
        000) log_w "FSU HTTP HEAD 无响应 — gSOAP 可能不接受 HEAD" ;;
        *)   log_w "FSU HTTP HEAD 返回 ${H_CODE}" ;;
    esac

    # GET
    G_CODE=$(curl -s -o /tmp/fsu_get_${RUN_ID}.txt -w "%{http_code}" --max-time "${TIMEOUT_SECONDS}" \
        -X GET "${FSU_URL}" 2>/dev/null || echo "000")
    echo "GET → ${G_CODE}" >> "${RUN_LOG_DIR}/http_probe.txt"
    cat /tmp/fsu_get_${RUN_ID}.txt >> "${RUN_LOG_DIR}/http_probe.txt" 2>/dev/null || true
    case "${G_CODE}" in
        200) log_p "FSU HTTP GET 返回 200" ;;
        405) log_w "FSU HTTP GET 返回 405 — gSOAP 可能只接受 POST" ;;
        404) log_w "FSU HTTP GET 返回 404 — 路径可能不对" ;;
        000) log_w "FSU HTTP GET 无响应" ;;
        *)   log_w "FSU HTTP GET 返回 ${G_CODE}" ;;
    esac
    rm -f /tmp/fsu_get_${RUN_ID}.txt

    # POST 最小 SOAP 探测
    P_CODE=$(curl -s -o /tmp/fsu_post_${RUN_ID}.txt -w "%{http_code}" --max-time "${TIMEOUT_SECONDS}" \
        -X POST -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"\"" \
        -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body/></soap:Envelope>' \
        "${FSU_URL}" 2>/dev/null || echo "000")
    echo "POST → ${P_CODE}" >> "${RUN_LOG_DIR}/http_probe.txt"
    cat /tmp/fsu_post_${RUN_ID}.txt >> "${RUN_LOG_DIR}/http_probe.txt" 2>/dev/null || true

    if [[ "${P_CODE}" == "200" ]]; then
        RESP=$(cat /tmp/fsu_post_${RUN_ID}.txt 2>/dev/null || "")
        if echo "${RESP}" | grep -qi "Envelope\|soap\|SOAP"; then
            log_p "FSU HTTP POST 返回 200 + SOAP 响应 — FSUService 可达"
        else
            log_p "FSU HTTP POST 返回 200 — 响应非 SOAP，需人工确认"
        fi
    elif [[ "${P_CODE}" == "500" ]]; then
        log_w "FSU HTTP POST 返回 500 — 可能是 gSOAP 收到无效 SOAP Body 的正常响应"
    elif [[ "${P_CODE}" == "000" ]]; then
        log_f "FSU HTTP POST 无响应 — 请检查: FSU gSOAP 是否运行? 路径: ${FSU_SERVICE_PATH}?"
    else
        log_w "FSU HTTP POST 返回 ${P_CODE}"
    fi
    rm -f /tmp/fsu_post_${RUN_ID}.txt
fi
fi  # FSU_HOST check

# ===== F. 只读 SOAP 探测 (可选) =====
log_section "F. 只读 SOAP 探测"
{
    echo "=== SOAP 探测 ==="
    echo "ENABLE_SOAP_PROBE=${ENABLE_SOAP_PROBE}"
    echo "SOAP_PROBE_COMMAND=${SOAP_PROBE_COMMAND}"
} > "${RUN_LOG_DIR}/soap_probe.txt"

if [[ "${ENABLE_SOAP_PROBE}" != "true" ]]; then
    log_s "ENABLE_SOAP_PROBE=false, 跳过"
else
    # 安全门: 拒绝非白名单命令
    PROBE_CMD_UPPER=$(echo "${SOAP_PROBE_COMMAND}" | tr '[:lower:]' '[:upper:]')
    if echo "${BLOCKED_COMMANDS}" | grep -qw "${PROBE_CMD_UPPER}"; then
        log_f "SOAP_PROBE_COMMAND=${SOAP_PROBE_COMMAND} 被禁止 — 脚本不允许执行 SET 类命令"
        echo "BLOCKED: ${SOAP_PROBE_COMMAND}" >> "${RUN_LOG_DIR}/soap_probe.txt"
    elif ! echo "${ALLOWED_PROBE_COMMANDS}" | grep -qw "${PROBE_CMD_UPPER}"; then
        log_f "SOAP_PROBE_COMMAND=${SOAP_PROBE_COMMAND} 不在白名单 — 仅允许: ${ALLOWED_PROBE_COMMANDS}"
        echo "UNKNOWN: ${SOAP_PROBE_COMMAND}" >> "${RUN_LOG_DIR}/soap_probe.txt"
    elif [[ "${TCP_OK}" != "true" ]]; then
        log_s "TCP 不通，跳过 SOAP 探测"
    else
        FSU_CODE="${FSUID:-}"
        STATION="${STATION_NAME:-1}"

        case "${PROBE_CMD_UPPER}" in
            GET_FSUINFO)
                MSG_TYPE="1701"
                REQ_BODY='<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:xsd="http://www.w3.org/2001/XMLSchema"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soap:Body>
    <ns1:invoke xmlns:ns1="http://service.fsu.binterface.dcim.com">
      <xmlData>&lt;PK_Type&gt;
  &lt;FSUInfo&gt;
    &lt;FSUID&gt;'${FSU_CODE:-}'&lt;/FSUID&gt;
    &lt;FSUCode&gt;'${FSU_CODE:-}'&lt;/FSUCode&gt;
    &lt;StationName&gt;'${STATION}'&lt;/StationName&gt;
  &lt;/FSUInfo&gt;
&lt;/PK_Type&gt;</xmlData>
      <msgType>1701</msgType>
    </ns1:invoke>
  </soap:Body>
</soap:Envelope>'
                ;;
            GET_LOGININFO)
                MSG_TYPE="1501"
                REQ_BODY='<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:xsd="http://www.w3.org/2001/XMLSchema"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soap:Body>
    <ns1:invoke xmlns:ns1="http://service.fsu.binterface.dcim.com">
      <xmlData>&lt;PK_Type&gt;
  &lt;FSUInfo&gt;
    &lt;FSUID&gt;'${FSU_CODE:-}'&lt;/FSUID&gt;
    &lt;FSUCode&gt;'${FSU_CODE:-}'&lt;/FSUCode&gt;
    &lt;StationName&gt;'${STATION}'&lt;/StationName&gt;
  &lt;/FSUInfo&gt;
&lt;/PK_Type&gt;</xmlData>
      <msgType>1501</msgType>
    </ns1:invoke>
  </soap:Body>
</soap:Envelope>'
                ;;
            GET_DATA)
                MSG_TYPE="401"
                REQ_BODY='<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:xsd="http://www.w3.org/2001/XMLSchema"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soap:Body>
    <ns1:invoke xmlns:ns1="http://service.fsu.binterface.dcim.com">
      <xmlData>&lt;PK_Type&gt;
  &lt;DeviceList&gt;
    &lt;DeviceID&gt;&lt;/DeviceID&gt;
  &lt;/DeviceList&gt;
&lt;/PK_Type&gt;</xmlData>
      <msgType>401</msgType>
    </ns1:invoke>
  </soap:Body>
</soap:Envelope>'
                ;;
        esac

        # 脱敏写请求日志
        echo "${REQ_BODY}" > "${RUN_LOG_DIR}/soap_request.xml"

        log_i "发送 ${SOAP_PROBE_COMMAND} (Code=${MSG_TYPE}) → ${FSU_URL}"
        PROBE_CODE=$(curl -s -o "${RUN_LOG_DIR}/soap_response.xml" -w "%{http_code}" \
            --max-time "$((TIMEOUT_SECONDS * 3))" \
            -X POST -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"\"" \
            -d "${REQ_BODY}" "${FSU_URL}" 2>/dev/null || echo "000")

        echo "HTTP ${PROBE_CODE}" >> "${RUN_LOG_DIR}/soap_probe.txt"

        if [[ "${PROBE_CODE}" == "200" ]]; then
            RESP=$(cat "${RUN_LOG_DIR}/soap_response.xml" 2>/dev/null || "")
            echo "${RESP}" >> "${RUN_LOG_DIR}/soap_probe.txt"

            HAS_ENV=$(echo "${RESP}" | grep -ci "Envelope" || true)
            HAS_ACK=$(echo "${RESP}" | grep -ci "ACK\|${MSG_TYPE}" || true)
            HAS_RESULT=$(echo "${RESP}" | grep -ci "Result\|ResultCode\|Code" || true)

            log_p "SOAP 探测 HTTP 200"
            [[ "${HAS_ENV}" -gt 0 ]] && log_p "响应包含 SOAP Envelope" || log_w "响应不含 Envelope — 可能不是 SOAP"
            [[ "${HAS_ACK}" -gt 0 ]] && log_p "响应包含 ACK/Code=${MSG_TYPE}" || log_w "响应不包含预期 ACK 码 ${MSG_TYPE}"
            [[ "${HAS_RESULT}" -gt 0 ]] && log_i "响应包含 Result/ResultCode — 协议层有返回"
        elif [[ "${PROBE_CODE}" == "500" ]]; then
            log_w "SOAP 探测 HTTP 500 — 可能是 FSU 收到无效请求, 检查 FSUID/StationName 是否正确"
        elif [[ "${PROBE_CODE}" == "000" ]]; then
            log_f "SOAP 探测无响应 — FSU TCP 通但 SOAP 不可达"
        else
            log_f "SOAP 探测 HTTP ${PROBE_CODE}"
        fi

        echo "" >> "${RUN_LOG_DIR}/soap_probe.txt"
        echo "目标: ${FSU_URL}" >> "${RUN_LOG_DIR}/soap_probe.txt"
        echo "命令: ${SOAP_PROBE_COMMAND} (Code=${MSG_TYPE})" >> "${RUN_LOG_DIR}/soap_probe.txt"
        echo "HTTP: ${PROBE_CODE}" >> "${RUN_LOG_DIR}/soap_probe.txt"
    fi
fi

# ===== G. tcpdump =====
log_section "G. tcpdump"
if [[ "${ENABLE_TCPDUMP}" != "true" ]]; then
    log_s "ENABLE_TCPDUMP=false"
elif [[ -z "${FSU_HOST}" ]]; then
    log_s "FSU_HOST 未设置"
elif ! command -v tcpdump &>/dev/null; then
    log_s "tcpdump 不可用 — apt install tcpdump"
else
    PCAP="${RUN_LOG_DIR}/tcpdump.pcap"
    if [[ "$(id -u)" -eq 0 ]] || sudo -n true 2>/dev/null; then
        log_i "抓包 10s: host ${FSU_HOST} port ${FSU_PORT}"
        timeout 12 tcpdump -i "${TCPDUMP_INTERFACE}" "host ${FSU_HOST} and port ${FSU_PORT}" \
            -w "${PCAP}" -c 200 2>/dev/null || true
        [[ -s "${PCAP}" ]] && log_p "抓包完成: ${PCAP}" || log_w "未捕获到数据包"
    else
        log_s "无 sudo 权限 — 手动: sudo tcpdump -i any host ${FSU_HOST} -w ${PCAP}"
    fi
fi

# ===== 汇总 =====
log_section "汇总"
TOTAL=$((PASS + FAIL + SKIP + WARN))
echo ""
echo "============================================"
echo " FSU 独立通信检查完成"
echo "============================================"
echo " 总计: ${TOTAL} 项"
echo " PASS: ${PASS}"
echo " FAIL: ${FAIL}"
echo " SKIP: ${SKIP}"
echo " WARN: ${WARN}"
echo " 日志: ${RUN_LOG_DIR}"
echo "============================================"

{
    echo "TOTAL=${TOTAL}"
    echo "PASS=${PASS}"; echo "FAIL=${FAIL}"
    echo "SKIP=${SKIP}"; echo "WARN=${WARN}"
} >> "${RUN_LOG_DIR}/summary.txt"

[[ "${FAIL}" -gt 0 ]] && { echo ""; echo "存在 ${FAIL} 项失败，检查日志: ${RUN_LOG_DIR}"; exit 1; }
exit 0
