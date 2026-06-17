# TASK-003: Scheduler 安全门禁增强

## 1. 时间
2026-05-21

## 2. 实现

ActiveAlarmAuditSchedulerProperties 新增: realCallEnabled(默认false), allowedSuids(默认空), isRealCallAllowed(suid).
ActiveAlarmAuditScheduler 新增: runOnceDryRun(suid), runOnceReal(suid) + 白名单门禁.

## 3. 安全默认值

| 配置 | 默认 | 说明 |
|------|------|------|
| scheduler-enabled | false | Scheduler 关闭 |
| real-call-enabled | false | 真实调用关闭 |
| allowed-suids | [] | 白名单为空 |

## 4. 测试

ActiveAlarmAuditSchedulerGateTest: 10 tests. 1297 total, 0/0/9.

## 5. 不涉及

不访问真实 FSU, 不启 Scheduler, 不修改 alarm_record.
