# BIF-P4-022：活动告警审计结果查询 API

> BIF-P4-FIX-001 与 BIF-P4-021 Codex 复审暂缓；本阶段基于 Claude 当前修复结果继续推进
> 完成日期：2026-05-19

---

## 一、审计目标

为 BIF-P4-021 Scheduler 提供只读状态查询接口 `GET /api/binterface/active-alarm-audit/status`，返回 Scheduler 配置摘要和最近一次审计结果。

---

## 二、背景

- BIF-P4-021: Scheduler 定时审计已实现，结果保存在内存（`ActiveAlarmAuditScheduler` volatile 字段 + 数据库 `active_alarm_audit_record` 表）
- BIF-P4-022: 新增只读 API 查询当前状态，不触发新审计
- Codex 复审暂缓

---

## 三、修改范围

### 新增文件

| 文件 | 说明 |
|------|------|
| `binterface/dto/ActiveAlarmAuditStatusResponse.java` | 只读状态 DTO |
| `binterface/service/ActiveAlarmAuditStatusService.java` | 只读查询服务 |
| `binterface/controller/ActiveAlarmAuditController.java` | REST Controller |
| `test/.../ActiveAlarmAuditStatusApiTest.java` | 9 个 API 测试 |

### 不修改范围

- 不修改 Scheduler / 编排 / 协议 / 安全门禁
- 不新增数据库持久化（已有 BIF-P4-021 持久化足够）
- 不修改前端

---

## 四、API 架构

```
GET /api/binterface/active-alarm-audit/status
  │
  └─→ ActiveAlarmAuditController.status()
        │
        └─→ ActiveAlarmAuditStatusService.getStatus()
              │
              ├─→ ActiveAlarmAuditScheduler (读取内存状态)
              │     ├─ getLastResult()
              │     ├─ getLastRunTime()
              │     ├─ isLastSuccess()
              │     └─ getLastError()
              │
              └─→ ActiveAlarmAuditSchedulerProperties (读取配置)
                    ├─ isSchedulerEnabled()
                    ├─ getFsuCode()
                    ├─ getFixedDelayMs()
                    └─ getInitialDelayMs()
```

API 不调用 `auditByQueryingFsu()`，不访问 FSU，不执行 SET，不修改 alarm_record。

## 五、协议一致性

API 是纯 HTTP REST 查询接口，不触发 B接口协议调用。

## 六、安全边界

| 检查项 | 状态 |
|--------|------|
| API 不触发审计 | ✅ 测试验证 |
| API 不访问真实 FSU | ✅ 测试验证 |
| API 不启用 Scheduler | ✅ scheduler-enabled 保持在配置文件定义的值 |
| API 不执行 SET | ✅ 无 SET service 引用 |
| API 不修改 alarm_record | ✅ 无 AlarmRecordRepository 依赖 |

## 七、测试验证

ActiveAlarmAuditStatusApiTest (9 tests):

| 测试 | 覆盖 |
|------|------|
| getStatusShouldNotTriggerAudit | 不调用 auditByQueryingFsu |
| getStatusShouldNotAccessRealFsu | 不访问真实设备 |
| defaultSchedulerDisabledShouldBeReflected | schedulerEnabled=false |
| enabledSchedulerShouldBeReflected | schedulerEnabled=true + fsuCode |
| lastSuccessResultShouldBeQueryable | 成功结果可查 |
| lastFailureResultShouldBeQueryable | 失败结果可查 |
| noResultYetShouldReturnNullRunTime | 未运行时空值 |
| shouldNotModifyAlarmRecord | 不依赖 AlarmRecordRepository |
| shouldNotTriggerSetCommand | 不引用 SET |

全量：**1108 tests, 0 failures, 0 errors, 5 skipped**

## 八、风险与遗留问题

- BIF-P4-FIX-001 / BIF-P4-021 / BIF-P4-022 Codex 复审暂缓
- 当前结果仍为内存态（重启丢失），持久化已由 BIF-P4-021 实现（`active_alarm_audit_record` 表）

## 九、结论

**通过。** 只读 API 已实现，不触发审计、不访问 FSU、不执行 SET、不修改 alarm_record。
