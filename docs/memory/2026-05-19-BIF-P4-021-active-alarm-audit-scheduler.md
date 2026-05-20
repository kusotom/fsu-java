---
name: bif-p4-021-active-alarm-audit-scheduler
description: "BIF-P4-021 Scheduler定时审计: 10测试, 1097全量通过, 默认关闭, 不真FSU/不SET/不改库"
metadata:
  type: project
---

## 任务编号 BIF-P4-021
## 任务名称 Scheduler 定时 GET_ACTIVEALARM 差异审计
## 操作时间 2026-05-19

## 核心改动
- ActiveAlarmAuditScheduler: @Scheduled 定时调用 auditByQueryingFsu(), 默认 disabled
- ActiveAlarmAuditSchedulerProperties: 配置属性 (scheduler-enabled=false, fixed-delay-ms=300000)
- application.yml: 新增 b-interface.active-alarm-audit 配置段
- ActiveAlarmAuditSchedulerTest: 10 tests (默认关闭/启用/缺失fsuCode/异常不崩溃/不真FSU/不SET/不改库)

## BIF-P4-FIX-001 Codex 复审状态: 暂缓
本阶段基于 Claude 当前修复结果继续推进，后续仍需 Codex 集中复审。

## 测试 10/10 | 全量 1097 tests, 0 failures, 0 errors, 5 skipped
## 安全: 默认不启用 Scheduler, 默认不访问真实 FSU, 不执行 SET, 不修改 alarm_record
