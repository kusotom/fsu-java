# 远程 FSU 通信检查 — 部署与运维文档

## 文件清单

| 文件 | 用途 |
|------|------|
| `scripts/fsu-clean-server-bootstrap.sh` | 干净服务器初始化 (安装依赖 + 创建目录) |
| `scripts/fsu-remote-comm-check.sh` | 主测试脚本 |
| `scripts/fsu-remote-comm-check.env.example` | 配置模板 |

## 安全声明

- **不执行** SET / SET_* / 控制类命令
- **不启动** Scheduler
- **不修改** 业务数据库
- 不硬编码真实 FSU 密码/token/FSUID
- `env.remote` 不提交 git

---

## 干净服务器完整部署流程

### 1. 本地: 上传 bootstrap 脚本

```bash
cd /home/tom/桌面/FSU/fsu-platform-java
REMOTE_USER=youruser
REMOTE_HOST=your-server-ip

scp scripts/fsu-clean-server-bootstrap.sh ${REMOTE_USER}@${REMOTE_HOST}:/tmp/
```

### 2. 远程: 执行初始化

```bash
ssh ${REMOTE_USER}@${REMOTE_HOST} \
  "chmod +x /tmp/fsu-clean-server-bootstrap.sh && sudo /tmp/fsu-clean-server-bootstrap.sh"
```

可选: 安装 tcpdump 和 Java

```bash
ssh ${REMOTE_USER}@${REMOTE_HOST} \
  "sudo INSTALL_TCPDUMP=true INSTALL_JAVA=true /tmp/fsu-clean-server-bootstrap.sh"
```

### 3. 本地: 上传测试脚本和配置

```bash
REMOTE_DIR=/opt/fsu-tools/remote-comm-check

scp scripts/fsu-remote-comm-check.sh ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/
scp scripts/fsu-remote-comm-check.env.example ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/env.example
```

### 4. 远程: 授权

```bash
ssh ${REMOTE_USER}@${REMOTE_HOST} "chmod +x ${REMOTE_DIR}/fsu-remote-comm-check.sh"
```

### 5. 远程: 创建真实配置

```bash
ssh ${REMOTE_USER}@${REMOTE_HOST}
cd /opt/fsu-tools/remote-comm-check
cp env.example env.remote
nano env.remote   # 修改 FSU_HOST, FSUID 等字段
```

### 6. 远程: 首次运行 (仅平台检查)

```bash
cd /opt/fsu-tools/remote-comm-check
set -a && source ./env.remote && set +a
./fsu-remote-comm-check.sh
```

### 7. 远程: 开启 FSU 只读协议探测

```bash
ENABLE_PROTOCOL_PROBE=true ./fsu-remote-comm-check.sh
```

### 8. 远程: 开启 tcpdump 抓包

```bash
sudo ENABLE_TCPDUMP=true ./fsu-remote-comm-check.sh
```

---

## 检查项

| 分类 | 检查内容 |
|------|---------|
| A. 系统环境 | 用户/主机名/系统版本/IP/路由/DNS/工具链 |
| B. 平台服务 | 127.0.0.1:8080 TCP, HTTP, SCService POST, actuator/health |
| C. FSU 网络 | DNS 解析, TCP 端口, HTTP/SOAP POST 可达性 |
| D. 协议探测 | GET_FSUINFO (Code=1701) 轻量 SOAP POST (需 --enable-protocol-probe) |
| E. tcpdump | 短时抓包 (需 --enable-tcpdump + sudo) |

## 常见失败原因

| 现象 | 原因 |
|------|------|
| 平台 TCP 不通 | 平台未启动 / 端口未放行 |
| FSU IP 不通 | 路由/VPN/防火墙 |
| FSU POST 无响应 | FSU gSOAP 未运行 |
| GET_FSUINFO 无响应 | 不支持 B接口2016 |
| StationName/FSUID 不匹配 | 检查 FSU 配置 |
| tcpdump 无权限 | 使用 sudo |
