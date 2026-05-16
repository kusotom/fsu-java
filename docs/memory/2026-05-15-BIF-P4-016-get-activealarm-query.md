---
name: bif-p4-016-get-activealarm-query
description: GET_ACTIVEALARM活动告警查询闭环 — 2024标准, TAlarm解析, Service+Handler+Stub, 11新测试, 959全量回归
metadata:
  type: project
---

## 任务编号 BIF-P4-016
## 任务名称 GET_ACTIVEALARM 活动告警查询闭环
## 操作时间 2026-05-15
## 核心改动
- GetActiveAlarmResult + ActiveAlarmItem: 结果模型(含TAlarm字段)
- GetActiveAlarmService: SC→FSU查询, TAlarm列表解析(SerialNo/DeviceID/SPID/StartTime/TriggerVal/AlarmLevel/AlarmFlag/AlarmDesc/AlarmFriDesc)
- GetActiveAlarmCommandHandler: SUID提取+登录校验
- BInterfacePkType: 24值 (+GET_ACTIVEALARM)
- StubFsuServiceClient: +GET_ACTIVEALARM stub (604, 1条示例活动告警)

## 测试 11/11 | 全量 959 tests, 0 failures
## 未访问真实设备, 未修改SEND_ALARM逻辑, 未自动覆盖本地告警状态
