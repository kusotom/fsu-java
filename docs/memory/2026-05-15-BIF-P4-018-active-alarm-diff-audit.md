---
name: bif-p4-018-active-alarm-diff-audit
description: 活动告警差异核对审计 — FSU快照vs本地alarm_record只读对比, SerialNo优先+降级匹配, 14新测试, 1052全量回归
metadata:
  type: project
---

## 任务编号 BIF-P4-018
## 任务名称 GET_ACTIVEALARM 与本地 alarm_record 差异核对审计
## 操作时间 2026-05-15
## 核心改动
- ActiveAlarmDiffResult: 差异结果(MATCHED/FSU_ONLY/LOCAL_ONLY/FIELD_MISMATCH/INVALID)
- ActiveAlarmDiffItem: 单条差异(serialNo, deviceId, spid, mismatchFields, suggestion)
- ActiveAlarmDiffService: 差异核对引擎(SerialNo优先→DeviceID+SPID+AlarmFlag降级)
- LocalAlarmSnapshot: 本地告警只读投影
- ActiveAlarmDiffServiceTest: 14 tests

## 差异类型
- FSU_ONLY: FSU有活动告警,本地无 → 建议核查是否漏报SEND_ALARM
- LOCAL_ONLY: 本地有,FSU无 → 建议核查是否恢复报文丢失
- FIELD_MISMATCH: 两边都有但字段不一致(AlarmLevel/Flag/TriggerVal/SPID/Desc)
- MATCHED: 一致

## 测试 14/14 | 全量 1052 tests, 0 failures
## 只读审计,未访问真实设备,未修改本地告警状态
