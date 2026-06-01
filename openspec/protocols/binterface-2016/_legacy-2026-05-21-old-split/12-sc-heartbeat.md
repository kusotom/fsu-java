# 12 — SC 心跳功能

## 1. 协议定位

心跳机制用于 SC 维护 FSU 在线状态。2016 协议定义了两个心跳相关能力：

### 1.1 FSU 主动心跳（快数据通道）

FSU 定期向 SC 发送 HEARTBEAT 上报，携带 CPU/内存/温度等运行状态。

### 1.2 SC 轮询心跳（慢数据通道）

SC 通过 GET_FSUINFO (Code=1701) 主动查询 FSU 运行状态，解析 CPU/MEM 并更新 fsu_status。

## 2. 当前项目状态

| 能力 | 实现 | 实测 |
|------|------|------|
| FSU 主动 HEARTBEAT 接收 | ✅ `HeartbeatCommandHandler` | ❌ Emerson FSU 不主动发送 |
| SC 主动 GET_FSUINFO | ✅ `BInterface2016GetFsuInfoService` | ⬜ 待现场 run-once |
| fsu_status 更新 | ✅ `last_heartbeat` / `online_status` | ⬜ |
| statusDetail 写入 | ✅ CPU/MEM 文本 | ⬜ |

## 3. Emerson FSU 实测

LANDING-014 观察结论: Emerson FSU **仅持续上报 LOGIN**，不主动发送 HEARTBEAT。

因此当前策略: **SC 主动 GET_FSUINFO 轮询代替被动 HEARTBEAT 接收**。

## 4. 心跳超时

| 配置 | 默认值 | 说明 |
|------|--------|------|
| heartbeat-timeout-seconds | 300 | 心跳超时 |
| scan-interval-seconds | 60 | 扫描间隔 |

Scheduler 默认禁用 (`scheduler-enabled: false`)。
