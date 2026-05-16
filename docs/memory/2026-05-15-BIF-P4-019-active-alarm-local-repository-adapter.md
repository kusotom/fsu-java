---
name: bif-p4-019-active-alarm-local-repository-adapter
description: alarm_record只读适配 — Entity→Snapshot映射, DiffAudit编排, pointCode→spid, 8新测试, 1060全量回归
metadata:
  type: project
---

## 任务编号 BIF-P4-019
## 任务名称 alarm_record 本地活动告警只读查询适配
## 操作时间 2026-05-15
## 核心改动
- LocalActiveAlarmSnapshotService: fsuCode→fsuId→alarm_record查询, ACTIVE过滤, Entity→Snapshot映射
- ActiveAlarmDiffAuditService: 编排(本地查询+diff核对)
- 字段映射: pointCode→spid, alarmStatus→alarmFlag, alarmValue→triggerVal, serialNo/deviceId→null

## 测试 8/8 | 全量 1060 tests, 0 failures
## 只读,未修改alarm_record,未访问真实设备
