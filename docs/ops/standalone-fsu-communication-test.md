# FSU 独立通信检查 (无平台依赖)

**文件**: `scripts/fsu-standalone-comm-check.sh`

## 用途

在干净远程服务器上, 不依赖平台后端, 直接测试与现场 FSU 的:
- DNS / IP / TCP 连通性
- HTTP / SOAP 路径连通性
- 可选 B接口只读 SOAP 探测 (GET_FSUINFO / GET_LOGININFO)

## 与 remote-fsu-comm-check.sh 的区别

| | standalone | remote |
|---|---|---|
| 检查平台 8080 | 否 | 是 |
| 检查 SCService | 否 | 是 |
| 检查 FSU | 是 | 是 |
| SOAP 探测 | 是 (curl直发) | 是 |
| 依赖 Java/Spring | 否 | 否 |
| 依赖 PostgreSQL | 否 | 否 |

## 最小安装

```bash
sudo apt update
sudo apt install -y bash curl netcat-openbsd iproute2 coreutils ca-certificates
```

## 部署

```bash
DEPLOY_DIR=/opt/fsu-tools/standalone-fsu-check
sudo mkdir -p ${DEPLOY_DIR}/logs
sudo chown -R $(whoami):$(whoami) ${DEPLOY_DIR}

cp scripts/fsu-standalone-comm-check.sh ${DEPLOY_DIR}/
cp scripts/fsu-standalone-comm-check.env.example ${DEPLOY_DIR}/env.example
chmod +x ${DEPLOY_DIR}/fsu-standalone-comm-check.sh

cd ${DEPLOY_DIR}
cp env.example env.remote
nano env.remote  # 修改 FSU_HOST
```

## 使用

```bash
# 基础检查
FSU_HOST=192.168.x.x ./fsu-standalone-comm-check.sh

# 开启 SOAP 探测
FSU_HOST=192.168.x.x ENABLE_SOAP_PROBE=true ./fsu-standalone-comm-check.sh

# 指定 FSUID
FSU_HOST=192.168.x.x FSUID=51051243812345 ENABLE_SOAP_PROBE=true ./fsu-standalone-comm-check.sh

# 抓包
sudo FSU_HOST=192.168.x.x ENABLE_TCPDUMP=true ./fsu-standalone-comm-check.sh
```

## 结果解读

| 现象 | 含义 |
|------|------|
| TCP 不通 | FSU 离线/路由/防火墙 |
| TCP 通但 HEAD/GET 返回 405 | gSOAP 只接受 POST, 正常 |
| TCP 通但 POST 无响应 | FSU gSOAP 服务未运行 |
| POST 500 + SOAP Fault | gSOAP 在线, 请求格式问题 |
| SOAP 有返回但无 ACK | StationName/FSUID 可能不匹配 |

## 安全

- [x] 不执行 SET
- [x] 不启 Scheduler
- [x] 不依赖平台
- [x] 不访问数据库
