# LANDING-014 Spec：真实 FSU HEARTBEAT/SEND_DATA/SEND_ALARM 连续上报观察与入库验证

> 规格版本：1.0 | 创建日期：2026-05-21

## 1. 背景

LANDING-013-FIX-001 修复后，真实 FSU LOGIN 链路已完整闭环（SOAP ACK + ResultCode=0 + SessionID + BInterfaceMessageLog 入库 id=399/400）。

现需观察 FSU 后续是否持续上报 HEARTBEAT / SEND_DATA / SEND_ALARM，验证各业务链路入库。

## 2. 目标

- 观察 HEARTBEAT 是否持续上报，验证 fsu_status 更新
- 观察 SEND_DATA 是否上报，记录报文格式
- 观察 SEND_ALARM 是否上报，验证 alarm_record.serialNo/deviceId/spid 写入
- 导出四类命令的原始报文样本
- 记录未映射 DeviceID/SPID

## 3. 非目标

不执行 SET，不启用 Scheduler，不主动 GET_*，不修改代码。

## 4. 验收标准

| # | 验收项 |
|---|--------|
| 1 | 确认收到 HEARTBEAT（或记录未收到原因） |
| 2 | 确认收到 SEND_DATA（或记录未收到原因） |
| 3 | 确认收到 SEND_ALARM（或记录未收到原因） |
| 4 | 原始报文样本已导出 |
| 5 | 未映射 DeviceID/SPID 已记录 |
| 6 | 测试基线保持 |

## 5. 安全边界

SET/Scheduler/GET_*/alarm_record状态机: 不涉及。Codex复审: 暂缓。
