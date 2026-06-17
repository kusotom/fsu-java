#!/usr/bin/env bash
# ============================================================================
# fsu-clean-server-bootstrap.sh — 干净服务器初始化
# ============================================================================
# 在全新 Ubuntu/Debian 服务器上安装依赖、创建目录。
# 不安装 Java/Maven (除非显式开启)，不启动项目服务，不访问真实 FSU。
#
# 环境变量:
#   DEPLOY_DIR         部署目录 (默认 /opt/fsu-tools/remote-comm-check)
#   INSTALL_TCPDUMP    安装 tcpdump (默认 false)
#   INSTALL_JAVA       安装 openjdk-17-jre-headless (默认 false)
#
# 运行:
#   chmod +x fsu-clean-server-bootstrap.sh
#   sudo ./fsu-clean-server-bootstrap.sh
# ============================================================================
set -Eeuo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/fsu-tools/remote-comm-check}"
INSTALL_TCPDUMP="${INSTALL_TCPDUMP:-false}"
INSTALL_JAVA="${INSTALL_JAVA:-false}"
SCRIPT_USER="${SUDO_USER:-$(whoami)}"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

while [[ $# -gt 0 ]]; do
    case "$1" in
        --deploy-dir) DEPLOY_DIR="$2"; shift 2 ;;
        --install-tcpdump) INSTALL_TCPDUMP=true; shift ;;
        --install-java) INSTALL_JAVA=true; shift ;;
        -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
        *) echo "未知选项: $1"; exit 2 ;;
    esac
done

# ===== 权限检查 =====
if [[ "$(id -u)" -ne 0 ]]; then
    log_error "需要 root 权限，请使用 sudo 运行"
    exit 1
fi

log_info "============================================"
log_info " FSU 干净服务器初始化"
log_info " 部署目录: ${DEPLOY_DIR}"
log_info " 用户: ${SCRIPT_USER}"
log_info " 时间: $(date '+%Y-%m-%d %H:%M:%S')"
log_info "============================================"

# ===== 系统信息 =====
log_info "系统: $(lsb_release -ds 2>/dev/null || cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2 | tr -d '"')"
log_info "内核: $(uname -r)"
log_info "架构: $(uname -m)"

# ===== apt update =====
log_info "apt update..."
apt update -qq

# ===== 基础包 =====
BASE_PACKAGES="bash curl ca-certificates coreutils iproute2 netcat-openbsd procps lsof"
# dnsutils (Ubuntu) 或 bind9-dnsutils (Debian)
if apt-cache show dnsutils &>/dev/null; then
    BASE_PACKAGES="$BASE_PACKAGES dnsutils"
elif apt-cache show bind9-dnsutils &>/dev/null; then
    BASE_PACKAGES="$BASE_PACKAGES bind9-dnsutils"
fi

log_info "安装基础包: ${BASE_PACKAGES}"
apt install -y -qq ${BASE_PACKAGES}

# jq (建议但不强制)
if apt-cache show jq &>/dev/null; then
    apt install -y -qq jq 2>/dev/null || log_warn "jq 安装失败(非关键)"
fi

# ===== 可选包 =====
if [[ "${INSTALL_TCPDUMP}" == "true" ]]; then
    log_info "安装 tcpdump..."
    apt install -y -qq tcpdump
fi

if [[ "${INSTALL_JAVA}" == "true" ]]; then
    log_info "安装 openjdk-17-jre-headless..."
    apt install -y -qq openjdk-17-jre-headless
fi

# ===== 创建目录 =====
log_info "创建目录: ${DEPLOY_DIR}"
mkdir -p "${DEPLOY_DIR}"
mkdir -p "${DEPLOY_DIR}/logs"
chown -R "${SCRIPT_USER}:${SCRIPT_USER}" "${DEPLOY_DIR}"
chmod 755 "${DEPLOY_DIR}" "${DEPLOY_DIR}/logs"

# ===== 环境检查 =====
echo ""
log_info "===== 环境检查 ====="
echo "  Hostname : $(hostname)"
echo "  User     : ${SCRIPT_USER}"
echo "  IP       : $(hostname -I 2>/dev/null || ip -4 addr show | grep -oP 'inet \K[\d.]+' | head -1)"
echo "  Route    : $(ip route show default 2>/dev/null | head -1 || echo '无默认路由')"

for cmd in curl nc timeout lsof; do
    if command -v "$cmd" &>/dev/null; then echo "  [OK] $cmd"; else echo "  [MISS] $cmd"; fi
done
for cmd in tcpdump java jq; do
    if command -v "$cmd" &>/dev/null; then echo "  [OK] $cmd (可选)"; else echo "  [SKIP] $cmd (未安装)"; fi
done

# ===== 完成 =====
echo ""
log_info "===== 初始化完成 ====="
log_info "部署目录: ${DEPLOY_DIR}"
log_info "日志目录: ${DEPLOY_DIR}/logs"
log_info ""
log_info "下一步:"
log_info "  1. 上传测试脚本: scp scripts/fsu-remote-comm-check.sh user@host:${DEPLOY_DIR}/"
log_info "  2. 上传配置模板: scp scripts/fsu-remote-comm-check.env.example user@host:${DEPLOY_DIR}/env.example"
log_info "  3. 远程创建配置: ssh user@host 'cd ${DEPLOY_DIR} && cp env.example env.remote && nano env.remote'"
log_info "  4. 运行测试:      ssh user@host 'cd ${DEPLOY_DIR} && source env.remote && ./fsu-remote-comm-check.sh'"
log_info ""
log_info "安全声明: 未安装项目服务，未启动 Scheduler，未访问真实 FSU，未执行 SET"
