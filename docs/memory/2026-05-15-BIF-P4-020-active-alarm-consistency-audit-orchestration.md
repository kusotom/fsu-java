---
name: bif-p4-020-active-alarm-consistency-audit-orchestration
description: GET_ACTIVEALARM+diff编排审计 — ConsistencyAuditService双模式, FSU查询→本地→diff完整链路, 10新测试, 1070全量回归
metadata:
  type: project
---

## 任务编号 BIF-P4-020
## 任务名称 GET_ACTIVEALARM + ActiveAlarmDiff 编排审计
## 操作时间 2026-05-15
## 核心改动
- ActiveAlarmConsistencyAuditResult: 编排结果(fsuCount/localCount/matched/fsuOnly/localOnly/mismatch/realDeviceAccessed)
- ActiveAlarmConsistencyAuditService: 双模式编排
  - auditWithProvidedSnapshot: 直接diff(不访问FSU)
  - auditByQueryingFsu: GetActiveAlarmService→LocalAlarmSnapshot→diff
- ActiveAlarmConsistencyAuditServiceTest: 10 tests

## 编排链路
auditByQueryingFsu: GetActiveAlarmService → ActiveAlarmDiffService(FSUSnapshots × LocalSnapshots) → ConsistencyAuditResult

## 测试 10/10 | 全量 1070 tests, 0 failures
## 只读,未修改alarm_record,未访问真实设备
