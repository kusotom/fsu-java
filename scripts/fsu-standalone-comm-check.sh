#!/usr/bin/env bash
# ============================================================================
# fsu-standalone-comm-check.sh — FSU 独立通信主动验证脚本 (v2)
# ============================================================================
# 不依赖平台后端。所有 env.remote 值视为 candidate，通过实际探测验证。
#
# env.remote 值状态:
#   user_supplied   — 用户填写，未经验证
#   auto_detected   — 脚本自动发现
#   verified        — 实际探测确认
#   mismatch        — 与探测结果不一致
#   unknown         — 无法确认
#   skipped         — 未检查
#
# 安全: 不执行 SET/SET_*/REBOOT/UPGRADE. 不启 Scheduler. 不访问数据库.
# ============================================================================
set -Eeuo pipefail

# ── 默认配置 (全部为 candidate) ──
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
ENABLE_FSU_DISCOVERY="${ENABLE_FSU_DISCOVERY:-false}"
FSU_SCAN_CIDR="${FSU_SCAN_CIDR:-}"
FSU_PORT_CANDIDATES="${FSU_PORT_CANDIDATES:-80,8080,8000,8081,8899}"
FSU_SERVICE_PATH_CANDIDATES="${FSU_SERVICE_PATH_CANDIDATES:-/services/FSUService,/services/FSUService?wsdl,/FSUService,/FSUService?wsdl,/services}"
LOG_DIR="${LOG_DIR:-/opt/fsu-tools/standalone-fsu-check/logs}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-5}"

ALLOWED_PROBES="GET_FSUINFO GET_LOGININFO GET_DATA"
BLOCKED_COMMANDS="SET SET_POINT SET_THRESHOLD SET_FTP SET_LOGININFO SET_FSUREBOOT REBOOT UPGRADE SCHEDULER"

# ── 状态计数器 ──
PASS=0; FAIL=0; SKIP=0; WARN=0
RUN_ID="$(date +%Y%m%d-%H%M%S)"
RUN_LOG_DIR="${LOG_DIR}/${RUN_ID}"

# ── 配置验证状态追踪 ──
declare -A CFG_STATUS
CFG_STATUS[fsu_host]="unchecked"
CFG_STATUS[fsu_port]="unchecked"
CFG_STATUS[fsu_path]="unchecked"
CFG_STATUS[station_name]="unchecked"
CFG_STATUS[fsuid]="unchecked"
declare -A CFG_DETECTED

# ── 辅助函数 ──
init_log() { mkdir -p "${RUN_LOG_DIR}"; }
log_section() { echo ""; echo "===== $1 ====="; }
log_i() { echo "[INFO]  $*"; }
log_p() { echo "[PASS]  $*"; PASS=$((PASS+1)); }
log_f() { echo "[FAIL]  $*"; FAIL=$((FAIL+1)); }
log_s() { echo "[SKIP]  $*"; SKIP=$((SKIP+1)); }
log_w() { echo "[WARN]  $*"; WARN=$((WARN+1)); }

mask_id() {
    local v="$1"
    [[ -z "$v" || ${#v} -le 6 ]] && echo "***" && return
    echo "${v:0:4}****${v: -4}"
}
mask_sensitive() { echo "$1" | sed -E 's/(token|password|AUTH_TOKEN|Authorization|Cookie)=[^[:space:];]+/\1=***/gi'; }

# ── 参数解析 ──
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
        --enable-fsu-discovery) ENABLE_FSU_DISCOVERY=true; shift ;;
        --fsu-scan-cidr) FSU_SCAN_CIDR="$2"; shift 2 ;;
        --log-dir) LOG_DIR="$2"; shift 2 ;;
        --timeout) TIMEOUT_SECONDS="$2"; shift 2 ;;
        -h|--help) sed -n '2,40p' "$0"; exit 0 ;;
        *) echo "未知: $1"; exit 2 ;;
    esac
done

[[ -f "./env.remote" ]] && { set -a; source ./env.remote; set +a; }

init_log
exec 1> >(tee -a "${RUN_LOG_DIR}/summary.txt") 2>&1

# ── 保存环境 ──
cat > "${RUN_LOG_DIR}/env.txt" <<EOF
RUN_ID=${RUN_ID}
FSU_HOST=$(mask_id "${FSU_HOST}")
FSU_PORT=${FSU_PORT}
TIME=$(date)
HOSTNAME=$(hostname)
USER=$(whoami)
EOF

echo "============================================"
echo " FSU 独立通信主动验证 v2"
echo " 运行 ID: ${RUN_ID}"
echo " 时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo " 日志: ${RUN_LOG_DIR}"
echo "============================================"

# ═══════════════════════════════════════════
# A. 本机环境
# ═══════════════════════════════════════════
log_section "A. 本机环境"
cat > "${RUN_LOG_DIR}/system_check.txt" <<EOF
HOSTNAME=$(hostname)
USER=$(whoami)
TIME=$(date '+%Y-%m-%d %H:%M:%S %Z')
OS=$(lsb_release -ds 2>/dev/null || cat /etc/os-release 2>/dev/null | grep PRETTY_NAME | cut -d= -f2 | tr -d '"' || echo unknown)
KERNEL=$(uname -r)
IP=$(hostname -I 2>/dev/null | head -1)
GATEWAY=$(ip route show default 2>/dev/null | awk '{print $3; exit}')
DNS=$(grep '^nameserver' /etc/resolv.conf 2>/dev/null | awk '{print $2}' | tr '\n' ' ')
EOF
log_i "主机: $(hostname)  IP: $(hostname -I 2>/dev/null | head -1)"
log_i "网关: $(ip route show default 2>/dev/null | awk '{print $3; exit}' || echo none)"

for cmd in curl nc timeout; do
    command -v "$cmd" &>/dev/null && log_p "$cmd" || log_f "$cmd 缺失"
done
for cmd in tcpdump openssl xmllint; do
    command -v "$cmd" &>/dev/null && log_p "$cmd (可选)" || log_s "$cmd 未安装"
done

# ═══════════════════════════════════════════
# B. FSU_HOST 验证
# ═══════════════════════════════════════════
log_section "B. FSU_HOST 验证"

VERIFIED_FSU_HOST=""
TCP_VERIFIED=false

cat > "${RUN_LOG_DIR}/discovery.txt" <<EOF
=== FSU Discovery ===
EOF

if [[ -z "${FSU_HOST}" ]]; then
    log_i "FSU_HOST 未设置"
    if [[ "${ENABLE_FSU_DISCOVERY}" == "true" ]] && [[ -n "${FSU_SCAN_CIDR}" ]]; then
        log_i "discovery 模式: 扫描 ${FSU_SCAN_CIDR} (仅 TCP ${FSU_PORT})"
        log_w "discovery 会尝试连接网段内所有 IP 的端口 ${FSU_PORT}"
        # 简单扫描: 取 /24 网段前 254 个 IP, 用 nc 探测
        NETWORK=$(echo "${FSU_SCAN_CIDR}" | sed 's|\.0/24||')
        found_count=0
        for i in $(seq 1 254); do
            ip="${NETWORK}.${i}"
            if nc -z -w 1 "${ip}" "${FSU_PORT}" 2>/dev/null; then
                log_p "发现候选 FSU: ${ip}:${FSU_PORT}"
                echo "CANDIDATE ${ip}:${FSU_PORT}" >> "${RUN_LOG_DIR}/discovery.txt"
                found_count=$((found_count+1))
                [[ -z "${VERIFIED_FSU_HOST}" ]] && VERIFIED_FSU_HOST="${ip}"
            fi
        done
        if [[ "${found_count}" -eq 0 ]]; then
            log_f "discovery 未发现任何 ${FSU_PORT} 端口开放的 IP"
            CFG_STATUS[fsu_host]="unreachable"
        else
            log_i "discovery 发现 ${found_count} 个候选 IP, 使用首个: ${VERIFIED_FSU_HOST}"
            CFG_STATUS[fsu_host]="verified"
            CFG_DETECTED[fsu_host]="${VERIFIED_FSU_HOST}"
            TCP_VERIFIED=true
        fi
    else
        log_f "FSU_HOST 缺失 — 设置 FSU_HOST=192.168.x.x 或 ENABLE_FSU_DISCOVERY=true FSU_SCAN_CIDR=x.x.x.0/24"
        CFG_STATUS[fsu_host]="unreachable"
    fi
else
    log_i "user_supplied FSU_HOST=${FSU_HOST}"

    # DNS/IP 检查
    if [[ "${FSU_HOST}" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        log_p "FSU_HOST 是 IP, 跳过 DNS"
    elif host "${FSU_HOST}" &>/dev/null || getent hosts "${FSU_HOST}" &>/dev/null; then
        log_p "DNS 解析成功: $(getent hosts "${FSU_HOST}" | awk '{print $1; exit}')"
    else
        log_f "DNS 解析失败: ${FSU_HOST}"
        CFG_STATUS[fsu_host]="unreachable"
    fi

    # TCP 验证
    if nc -vz -w "${TIMEOUT_SECONDS}" "${FSU_HOST}" "${FSU_PORT}" 2>/dev/null; then
        log_p "TCP ${FSU_HOST}:${FSU_PORT} 连通"
        VERIFIED_FSU_HOST="${FSU_HOST}"
        CFG_STATUS[fsu_host]="verified"
        TCP_VERIFIED=true
    else
        # 候选端口探测
        log_f "TCP ${FSU_HOST}:${FSU_PORT} 不通"
        log_i "尝试候选端口: ${FSU_PORT_CANDIDATES}"
        IFS=',' read -ra PORTS <<< "${FSU_PORT_CANDIDATES}"
        detected_port=""
        for p in "${PORTS[@]}"; do
            p="${p// /}"
            [[ "$p" == "${FSU_PORT}" ]] && continue
            if nc -z -w 1 "${FSU_HOST}" "$p" 2>/dev/null; then
                log_w "候选端口 $p 可达 (user_supplied=${FSU_PORT})"
                detected_port="$p"
                break
            fi
        done
        if [[ -n "${detected_port}" ]]; then
            CFG_STATUS[fsu_host]="verified"
            CFG_STATUS[fsu_port]="mismatch"
            CFG_DETECTED[fsu_port]="${detected_port}"
            FSU_PORT="${detected_port}"
            TCP_VERIFIED=true
        else
            CFG_STATUS[fsu_host]="unreachable"
        fi
    fi
fi

# ═══════════════════════════════════════════
# C. FSU_PORT 验证
# ═══════════════════════════════════════════
log_section "C. FSU_PORT 验证"
FSU_URL="${FSU_SCHEME}://${VERIFIED_FSU_HOST:-${FSU_HOST}}:${FSU_PORT}${FSU_SERVICE_PATH}"

if [[ "${TCP_VERIFIED}" != "true" ]]; then
    log_s "TCP 不通, FSU_PORT 无法验证"
    CFG_STATUS[fsu_port]="${CFG_STATUS[fsu_port]:-unreachable}"
else
    if [[ "${CFG_STATUS[fsu_port]}" == "mismatch" ]]; then
        log_w "FSU_PORT mismatch: user=${FSU_PORT_CANDIDATES%%:*}, detected=${FSU_PORT}"
    else
        log_p "FSU_PORT=${FSU_PORT} TCP verified"
        CFG_STATUS[fsu_port]="verified"
    fi
fi

# ═══════════════════════════════════════════
# D. FSU_SERVICE_PATH 验证
# ═══════════════════════════════════════════
log_section "D. FSU_SERVICE_PATH 验证"
cat > "${RUN_LOG_DIR}/endpoint_candidates.txt" <<EOF
=== Endpoint Candidates ===
user_supplied: ${FSU_SERVICE_PATH}
EOF

PATH_VERIFIED=false
VERIFIED_PATH=""

if [[ "${TCP_VERIFIED}" != "true" ]]; then
    log_s "TCP 不通, 跳过路径验证"
    CFG_STATUS[fsu_path]="unreachable"
else
    IFS=',' read -ra PATHS <<< "${FSU_SERVICE_PATH_CANDIDATES}"
    for path in "${PATHS[@]}"; do
        path="${path// /}"
        test_url="${FSU_SCHEME}://${VERIFIED_FSU_HOST:-${FSU_HOST}}:${FSU_PORT}${path}"
        echo "  testing: ${test_url}" >> "${RUN_LOG_DIR}/endpoint_candidates.txt"

        # POST 最小 SOAP
        code=$(curl -s -o /tmp/fsu_path_${RUN_ID}.txt -w "%{http_code}" --max-time "${TIMEOUT_SECONDS}" \
            -X POST -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"\"" \
            -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body/></soap:Envelope>' \
            "${test_url}" 2>/dev/null || echo "000")
        resp=$(cat /tmp/fsu_path_${RUN_ID}.txt 2>/dev/null || echo "")
        echo "  POST → ${code}" >> "${RUN_LOG_DIR}/endpoint_candidates.txt"

        if [[ "${code}" == "200" ]]; then
            if echo "${resp}" | grep -qi "Envelope\|soap\|SOAP\|wsdl\|definitions"; then
                log_p "路径 ${path} 返回 HTTP 200 + SOAP/WSDL → verified"
                VERIFIED_PATH="${path}"
                PATH_VERIFIED=true
                break
            else
                log_w "路径 ${path} 返回 HTTP 200 但非 SOAP"
            fi
        elif [[ "${code}" == "500" ]] && echo "${resp}" | grep -qi "Envelope\|soap\|Fault"; then
            log_w "路径 ${path} 返回 HTTP 500 + SOAP Fault → probable endpoint"
            if [[ "${PATH_VERIFIED}" != "true" ]]; then
                VERIFIED_PATH="${path}"
                PATH_VERIFIED=true
            fi
            break
        elif [[ "${code}" == "405" ]]; then
            log_i "路径 ${path} 返回 405 — 方法不支持"
        fi
    done
    rm -f /tmp/fsu_path_${RUN_ID}.txt

    if [[ "${PATH_VERIFIED}" == "true" ]]; then
        FSU_SERVICE_PATH="${VERIFIED_PATH}"
        FSU_URL="${FSU_SCHEME}://${VERIFIED_FSU_HOST:-${FSU_HOST}}:${FSU_PORT}${FSU_SERVICE_PATH}"
        if [[ "${VERIFIED_PATH}" != "${FSU_SERVICE_PATH_CANDIDATES%%,*}" ]]; then
            CFG_STATUS[fsu_path]="mismatch"
            CFG_DETECTED[fsu_path]="${VERIFIED_PATH}"
            log_w "PATH mismatch: user_supplied=${FSU_SERVICE_PATH_CANDIDATES%%,*}, detected=${VERIFIED_PATH}"
        else
            CFG_STATUS[fsu_path]="verified"
            log_p "FSU_SERVICE_PATH=${FSU_SERVICE_PATH} verified"
        fi
    else
        log_w "无法确认 SOAP 端点路径, 保留 user_supplied=${FSU_SERVICE_PATH}"
        CFG_STATUS[fsu_path]="unknown"
    fi
fi

# ═══════════════════════════════════════════
# E. HTTP/SOAP 连通性 (使用最终 URL)
# ═══════════════════════════════════════════
log_section "E. HTTP/SOAP 连通性"
cat > "${RUN_LOG_DIR}/http_probe.txt" <<EOF
=== HTTP Probe ===
URL: ${FSU_URL}
EOF

if [[ "${TCP_VERIFIED}" != "true" ]]; then
    log_s "TCP 不通, 跳过 HTTP 探测"
else
    P_CODE=$(curl -s -o /tmp/fsu_hp_${RUN_ID}.txt -w "%{http_code}" --max-time "${TIMEOUT_SECONDS}" \
        -X POST -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"\"" \
        -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body/></soap:Envelope>' \
        "${FSU_URL}" 2>/dev/null || echo "000")
    RESP=$(cat /tmp/fsu_hp_${RUN_ID}.txt 2>/dev/null || "")
    echo "POST → ${P_CODE}" >> "${RUN_LOG_DIR}/http_probe.txt"
    echo "${RESP}" >> "${RUN_LOG_DIR}/http_probe.txt"
    rm -f /tmp/fsu_hp_${RUN_ID}.txt

    if [[ "${P_CODE}" == "200" ]]; then
        log_p "HTTP POST 200 — ${FSU_URL}"
        echo "${RESP}" | grep -qi "Envelope\|soap\|SOAP" && log_p "响应含 SOAP Envelope" || log_w "响应非 SOAP"
    elif [[ "${P_CODE}" == "500" ]]; then
        log_w "HTTP POST 500 — gSOAP 可能在线, 响应无效 Body"
    elif [[ "${P_CODE}" == "000" ]]; then
        log_f "HTTP POST 无响应 — FSU gSOAP 未运行?"
    else
        log_w "HTTP POST ${P_CODE}"
    fi
fi

# ═══════════════════════════════════════════
# F. 只读 SOAP 探测 + 身份验证
# ═══════════════════════════════════════════
log_section "F. SOAP 探测与身份验证"
cat > "${RUN_LOG_DIR}/identity_validation.txt" <<EOF
=== Identity Validation ===
STATION_NAME(user_supplied)=${STATION_NAME}
FSUID(user_supplied)=$(mask_id "${FSUID}")
EOF

if [[ "${ENABLE_SOAP_PROBE}" != "true" ]]; then
    log_s "ENABLE_SOAP_PROBE=false"
    CFG_STATUS[station_name]="skipped"
    CFG_STATUS[fsuid]="skipped"
else
    PROBE_UPPER=$(echo "${SOAP_PROBE_COMMAND}" | tr '[:lower:]' '[:upper:]')
    if echo "${BLOCKED_COMMANDS}" | grep -qw "${PROBE_UPPER}"; then
        log_f "SOAP_PROBE_COMMAND=${SOAP_PROBE_COMMAND} 被禁止 — 拒绝执行"
        echo "BLOCKED: ${SOAP_PROBE_COMMAND}" >> "${RUN_LOG_DIR}/soap_probe.txt"
    elif ! echo "${ALLOWED_PROBES}" | grep -qw "${PROBE_UPPER}"; then
        log_f "SOAP_PROBE_COMMAND=${SOAP_PROBE_COMMAND} 不在白名单 (${ALLOWED_PROBES})"
        echo "UNKNOWN: ${SOAP_PROBE_COMMAND}" >> "${RUN_LOG_DIR}/soap_probe.txt"
    elif [[ "${TCP_VERIFIED}" != "true" ]]; then
        log_s "TCP 不通, 跳过 SOAP"
    else
        FSU_CODE="${FSUID:-}"
        STATION="${STATION_NAME:-1}"

        case "${PROBE_UPPER}" in
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
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
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
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
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

        echo "${REQ_BODY}" > "${RUN_LOG_DIR}/soap_request.xml"

        log_i "发送 ${SOAP_PROBE_COMMAND} (Code=${MSG_TYPE}) → ${FSU_URL}"
        PCODE=$(curl -s -o "${RUN_LOG_DIR}/soap_response.xml" -w "%{http_code}" \
            --max-time "$((TIMEOUT_SECONDS * 3))" \
            -X POST -H "Content-Type: text/xml; charset=utf-8" -H "SOAPAction: \"\"" \
            -d "${REQ_BODY}" "${FSU_URL}" 2>/dev/null || echo "000")
        PRESP=$(cat "${RUN_LOG_DIR}/soap_response.xml" 2>/dev/null || echo "")

        {
            echo "=== SOAP Probe ==="
            echo "command=${SOAP_PROBE_COMMAND}"
            echo "code=${MSG_TYPE}"
            echo "http_status=${PCODE}"
            echo "--- response (first 2000 chars, masked) ---"
            echo "${PRESP}" | head -c 2000
        } > "${RUN_LOG_DIR}/soap_probe.txt"

        # 分层分析
        HAS_ENV=0; HAS_ACK=0; HAS_RESULT=0; HAS_STATION=0; HAS_FSUID_RESP=0
        DETECTED_STATION=""; DETECTED_FSUID=""

        echo "${PRESP}" | grep -qi "Envelope" && HAS_ENV=1
        echo "${PRESP}" | grep -qi "ACK\|${MSG_TYPE}" && HAS_ACK=1
        echo "${PRESP}" | grep -qi "Result\|ResultCode\|Code" && HAS_RESULT=1

        # 提取 StationName
        STATION_MATCH=$(echo "${PRESP}" | grep -oiP 'StationName[^<]*<[^>]*>\K[^<]+' | head -1 || true)
        [[ -z "${STATION_MATCH}" ]] && STATION_MATCH=$(echo "${PRESP}" | grep -oiP 'StationID[^<]*<[^>]*>\K[^<]+' | head -1 || true)
        [[ -n "${STATION_MATCH}" ]] && HAS_STATION=1 && DETECTED_STATION="${STATION_MATCH}"

        # 提取 FSUID
        FSUID_MATCH=$(echo "${PRESP}" | grep -oiP 'FSUId[^<]*<[^>]*>\K[^<]+' | head -1 || true)
        [[ -z "${FSUID_MATCH}" ]] && FSUID_MATCH=$(echo "${PRESP}" | grep -oiP 'FSUID[^<]*<[^>]*>\K[^<]+' | head -1 || true)
        [[ -z "${FSUID_MATCH}" ]] && FSUID_MATCH=$(echo "${PRESP}" | grep -oiP 'FsuCode[^<]*<[^>]*>\K[^<]+' | head -1 || true)
        [[ -z "${FSUID_MATCH}" ]] && FSUID_MATCH=$(echo "${PRESP}" | grep -oiP 'SUId[^<]*<[^>]*>\K[^<]+' | head -1 || true)
        [[ -n "${FSUID_MATCH}" ]] && HAS_FSUID_RESP=1 && DETECTED_FSUID="${FSUID_MATCH}"

        # 网络层
        [[ "${PCODE}" == "200" ]] && log_p "network: HTTP 200" || log_f "network: HTTP ${PCODE}"
        # SOAP 层
        [[ "${HAS_ENV}" -eq 1 ]] && log_p "soap: Envelope detected" || log_w "soap: no Envelope"
        # B接口层
        [[ "${HAS_ACK}" -eq 1 ]] && log_p "binterface: ACK/${MSG_TYPE}" || log_w "binterface: no ACK"

        # 身份验证
        {
            echo "network: $([[ "${PCODE}" == "200" ]] && echo ok || echo failed)"
            echo "soap: $([[ "${HAS_ENV}" -eq 1 ]] && echo envelope_detected || echo no_envelope)"
            echo "binterface: $([[ "${HAS_ACK}" -eq 1 ]] && echo ack_detected || echo no_ack)"
        } >> "${RUN_LOG_DIR}/identity_validation.txt"

        # STATION_NAME
        if [[ "${HAS_STATION}" -eq 1 ]]; then
            if [[ "${DETECTED_STATION}" == "${STATION_NAME}" ]]; then
                log_p "identity: StationName verified (${DETECTED_STATION})"
                CFG_STATUS[station_name]="verified"
            else
                log_w "identity: StationName mismatch (user=${STATION_NAME}, fsu=${DETECTED_STATION})"
                CFG_STATUS[station_name]="mismatch"
                CFG_DETECTED[station_name]="${DETECTED_STATION}"
            fi
        else
            log_i "identity: StationName unknown (SOAP 响应不含此字段)"
            CFG_STATUS[station_name]="unknown"
        fi

        # FSUID
        if [[ "${HAS_FSUID_RESP}" -eq 1 ]]; then
            if [[ -z "${FSUID}" ]]; then
                log_p "identity: FSUID auto_detected=$(mask_id "${DETECTED_FSUID}")"
                CFG_STATUS[fsuid]="auto_detected"
                CFG_DETECTED[fsuid]="${DETECTED_FSUID}"
            elif [[ "${DETECTED_FSUID}" == "${FSUID}" ]]; then
                log_p "identity: FSUID verified"
                CFG_STATUS[fsuid]="verified"
            else
                log_w "identity: FSUID mismatch (user=$(mask_id "${FSUID}"), fsu=$(mask_id "${DETECTED_FSUID}"))"
                CFG_STATUS[fsuid]="mismatch"
                CFG_DETECTED[fsuid]="${DETECTED_FSUID}"
            fi
        elif [[ -z "${FSUID}" ]]; then
            log_i "identity: FSUID unknown (SOAP 响应不含识别字段)"
            CFG_STATUS[fsuid]="unknown"
        else
            log_i "identity: FSUID user_supplied=$(mask_id "${FSUID}"), 无法从响应验证"
            CFG_STATUS[fsuid]="unverified"
        fi
    fi
fi

# ═══════════════════════════════════════════
# G. tcpdump
# ═══════════════════════════════════════════
log_section "G. tcpdump"
if [[ "${ENABLE_TCPDUMP}" != "true" ]]; then
    log_s "ENABLE_TCPDUMP=false"
elif [[ -z "${VERIFIED_FSU_HOST}" ]]; then
    log_s "无可用 FSU IP, 跳过"
elif ! command -v tcpdump &>/dev/null; then
    log_s "tcpdump 未安装"
else
    PCAP="${RUN_LOG_DIR}/tcpdump.pcap"
    if timeout 12 tcpdump -i "${TCPDUMP_INTERFACE}" "host ${VERIFIED_FSU_HOST}" \
        -w "${PCAP}" -c 200 2>/dev/null || true; then
        [[ -s "${PCAP}" ]] && log_p "tcpdump: ${PCAP} ($(wc -c < "${PCAP}") bytes)" || log_w "tcpdump: 无数据包"
    else
        log_s "tcpdump 需要 root 权限 — sudo $0"
    fi
fi

# ═══════════════════════════════════════════
# 配置验证摘要
# ═══════════════════════════════════════════
log_section "配置验证摘要"
{
    echo "=== Config Validation ==="
    echo "FSU_HOST        user_supplied=${FSU_HOST} status=${CFG_STATUS[fsu_host]} detected=${CFG_DETECTED[fsu_host]:-}"
    echo "FSU_PORT        user_supplied=${FSU_PORT_CANDIDATES%%,*} status=${CFG_STATUS[fsu_port]} detected=${CFG_DETECTED[fsu_port]:-}"
    echo "FSU_PATH        user_supplied=${FSU_SERVICE_PATH_CANDIDATES%%,*} status=${CFG_STATUS[fsu_path]} detected=${CFG_DETECTED[fsu_path]:-}"
    echo "STATION_NAME    user_supplied=${STATION_NAME} status=${CFG_STATUS[station_name]} detected=${CFG_DETECTED[station_name]:-}"
    echo "FSUID           user_supplied=$(mask_id "${FSUID}") status=${CFG_STATUS[fsuid]} detected=$(mask_id "${CFG_DETECTED[fsuid]:-}")"
} | tee "${RUN_LOG_DIR}/config_validation.txt"

echo ""
for key in fsu_host fsu_port fsu_path station_name fsuid; do
    status="${CFG_STATUS[${key}]}"
    case "${status}" in
        verified)    icon="[OK]" ;;
        auto_detected) icon="[DET]" ;;
        mismatch)    icon="[MIS]" ;;
        unknown|unverified) icon="[???]" ;;
        skipped)     icon="[SKP]" ;;
        *)           icon="[FAIL]" ;;
    esac
    echo "  ${icon} ${key}: ${status}"
done

# ═══════════════════════════════════════════
# 汇总
# ═══════════════════════════════════════════
log_section "汇总"
TOTAL=$((PASS+FAIL+SKIP+WARN))
echo ""
echo "============================================"
echo " FSU 独立通信主动验证完成"
echo "============================================"
echo " 总计: ${TOTAL}  PASS: ${PASS}  FAIL: ${FAIL}  SKIP: ${SKIP}  WARN: ${WARN}"
echo " 日志: ${RUN_LOG_DIR}"
echo "============================================"

{
    echo "TOTAL=${TOTAL}"; echo "PASS=${PASS}"; echo "FAIL=${FAIL}"
    echo "SKIP=${SKIP}"; echo "WARN=${WARN}"
} >> "${RUN_LOG_DIR}/summary.txt"

[[ "${FAIL}" -gt 0 ]] && { echo ""; echo "存在 ${FAIL} 项失败: ${RUN_LOG_DIR}"; exit 1; }
exit 0
