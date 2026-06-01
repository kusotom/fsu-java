# 审计文档：LANDING-014 真实 FSU 连续上报观察

## 1. 审计日期
2026-05-21

## 2. 审计类型
现场观察 + 入库验证（零代码修改）

## 3. 当前基线

真实 FSU LOGIN 已闭环: POST /services/SCService → HTTP 200 + SOAP ACK + ResultCode=0 + SessionID + BInterfaceMessageLog 入库 (id=399/400)。

## 4. 观察项

| 观察项 | 方法 | 预期 |
|--------|------|------|
| HEARTBEAT 上报 | API query command=HEARTBEAT | content 非空 |
| SEND_DATA 上报 | API query command=SEND_DATA | content 非空 |
| SEND_ALARM 上报 | API query command=SEND_ALARM | content 非空 |
| fsu_status 更新 | SQL 查 b_interface_fsu_status | last_heartbeat 有值 |
| alarm_record 写入 | SQL 查 alarm_record | serialNo/deviceId/spid 有值 |

## 5. 安全边界

SET/Scheduler/GET_*: 未执行。Codex复审: 暂缓。

## 6. 测试

1204 tests, 0/0/5 — 基线保持。Java 代码: 0 修改。

## 7. 待现场补充

- 各命令 totalElements 计数
- 原始报文样本文件
- 未映射 DeviceID/SPID 清单
