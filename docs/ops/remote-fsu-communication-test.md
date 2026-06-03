# 远程 FSU 通信检查脚本

**文件**: `scripts/fusu-remote-comm-check.sh`

## 用途

部署到远程服务器后，一键检查：
- 平台服务本地端口是否启动
- `/services/SCService` 被动上报入口是否可访问
- 远程服务器是否能连通 FSU 的 `/services/FSUService`
- 可选执行只读 B接口命令 (GET_FSUINFO)
- 可选 tcpdump 抓包辅助排查

## 安全声明

- **不执行** SET/SET_*/控制类命令
- **不启动** Scheduler
- **不修改** 业务数据库
- 不硬编码真实 FSU 密码/token/FSUID

## 前置条件

- Ubuntu/Debian 服务器
- `curl`, `nc`, `timeout` 已安装
- `tcpdump` 用于抓包 (可选)

```bash
sudo apt install curl netcat-openbsd tcpdump -y
```

## 运行命令

### 最小: 仅检查平台本地服务

```bash
./scripts/fusu-remote-comm-check.sh
```

### 检查 FSU 连通性

```bash
FSU_HOST=192.168.x.x FSU_PORT=80 ./scripts/fusu-remote-comm-check.sh
```

### 带真实 FSUID 的完整检查

```bash
FSU_HOST=192.168.x.x \
FSU_PORT=80 \
STATION_NAME=1 \
FSUID=51051243812345 \
./scripts/fusu-remote-comm-check.sh
```

### 开启只读协议探测

```bash
FSU_HOST=192.168.x.x \
FSU_PORT=80 \
FSUID=51051243812345 \
ENABLE_PROTOCOL_PROBE=true \
./scripts/fusu-remote-comm-check.sh
```

### 开启抓包

```bash
sudo FSU_HOST=192.168.x.x \
FSU_PORT=80 \
ENABLE_TCPDUMP=true \
TCPDUMP_INTERFACE=any \
./scripts/fusu-remote-comm-check.sh
```

## 结果判断

| 输出 | 含义 |
|------|------|
| `[PASS]` | 检查通过 |
| `[FAIL]` | 检查失败, 需排查 |
| `[SKIP]` | 因配置跳过 |
| 退出码 0 | 全部通过或跳过 |
| 退出码 1 | 存在失败项 |

## 常见失败原因

| 现象 | 可能原因 |
|------|---------|
| 平台端口 TCP 不通 | 平台未启动, 8080 端口未放行 |
| FSU IP 不通 | 路由/VPN/防火墙问题 |
| FSU TCP 不通 | FSU 离线, 端口错误 |
| FSU POST 无响应 | FSU gSOAP 未运行, 路径不对 |
| FSU POST 返回 200 但无 SOAP | FSUService 不是 SOAP 端点 |
| GET_FSUINFO 无响应 | FSU 不支持 B接口2016, Code=1701 |
| StationName/FSUID 不匹配 | 检查 FSU 配置文件中的实际值 |
| tcpdump 无权限 | 使用 `sudo` 或手动执行 |
