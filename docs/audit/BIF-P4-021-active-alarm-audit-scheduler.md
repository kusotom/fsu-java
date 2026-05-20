# BIF-P4-021：Scheduler 定时 GET_ACTIVEALARM 差异审计

> BIF-P4-FIX-001 Codex 复审暂缓；本阶段基于 Claude 当前修复结果继续推进
> 完成日期：2026-05-19

---

## 一、审计目标

将 `ActiveAlarmConsistencyAuditService.auditByQueryingFsu()` 接入受控 Scheduler，支持平台按配置周期定时执行活动告警审计。

---

## 二、背景

- BIF-P4-020 已完成：GET_ACTIVEALARM + ActiveAlarmDiff 编排审计闭环
- BIF-P4-FIX-001 已完成：Codex 审计阻断项全部修复，1087 tests 0/0/5
- BIF-P4-021 基于 Claude 当前修复结果继续推进
- Codex 复审（BIF-P4-FIX-001）暂缓，后续需集中复审

---

## 三、修改范围

### 新增文件

| 文件 | 说明 |
|------|------|
| `binterface/service/ActiveAlarmAuditScheduler.java` | Scheduler 编排类 |
| `binterface/service/ActiveAlarmAuditSchedulerProperties.java` | 配置属性类 |
| `test/.../ActiveAlarmAuditSchedulerTest.java` | 10 个 Scheduler 单元测试 |

### 修改文件

| 文件 | 说明 |
|------|------|
| `application.yml` | 新增 `b-interface.active-alarm-audit` 配置段 |

---

## 四、架构设计

```
ActiveAlarmAuditScheduler (@Scheduled)
  │
  ├─ 检查 scheduler-enabled == true ?
  ├─ 检查 fsuCode 已配置 ?
  │
  └─→ ActiveAlarmConsistencyAuditService.auditByQueryingFsu(fsuCode, null)
        │
        ├─→ GetActiveAlarmService → FsuServiceClient (Stub/Real) → SOAP RPC
        ├─→ LocalActiveAlarmSnapshotService → AlarmRecordRepository (只读)
        └─→ ActiveAlarmDiffService → ActiveAlarmDiffResult → ConsistencyAuditResult
```

Scheduler 职责边界：
- 读取配置，检查开关
- 定时触发编排
- 捕获异常，记录结果
- **不做** diff / 查询 / 写库 / SET / 拼 SOAP

配置模式复用 `SlowDataPollingScheduler` + `SlowDataPollingProperties` 成熟模式。

---

## 五、协议一致性

- GET_ACTIVEALARM Code=603 (2024 标准) → Name+Code PK_Type 出站（BIF-P4-FIX-001 已验证）
- FSU 通信: FsuServiceClient → FsuServiceRpcAdapter → SOAP RPC/encoded
- Scheduler 不直接构造协议报文

---

## 六、安全边界

| 检查项 | 默认值 | 状态 |
|--------|--------|------|
| scheduler-enabled | false | ✅ |
| real-call-enabled | false | ✅ |
| 缺失 fsuCode | skip | ✅ 测试覆盖 |
| 审计异常 | catch 不崩溃 | ✅ 测试覆盖 |
| 不修改 alarm_record | — | ✅ 测试覆盖 |
| 不执行 SET | — | ✅ 测试覆盖 |
| 不访问真实 FSU | — | ✅ 测试覆盖 |

---

## 七、测试验证

ActiveAlarmAuditSchedulerTest (10 tests):

| 测试 | 覆盖 |
|------|------|
| defaultDisabledShouldNotCallAudit | 默认不调用 |
| enabledShouldCallAuditOnce | scheduler-enabled=true 调用一次 |
| missingFsuCodeShouldSkip | fsuCode=null 跳过 |
| emptyFsuCodeShouldSkip | fsuCode 空白跳过 |
| auditExceptionShouldNotCrash | 异常不崩溃 |
| shouldRecordLastResult | 记录成功结果 |
| shouldRecordFailureResult | 记录失败结果 |
| schedulerShouldNotAccessRealFsu | 不访问真实设备 |
| schedulerShouldNotTriggerSetCommand | 不触发 SET |
| shouldNotModifyAlarmRecord | 不修改告警记录 |

全量回归：**1097 tests, 0 failures, 0 errors, 5 skipped**

---

## 八、风险与遗留问题

- `BIF-P4-FIX-001 Codex 复审暂缓`，后续需集中审计
- alarm_record Entity 缺少 serialNo/deviceId
- 真实点位表未到，seed SQL 未生成
- 审计结果仅在内存保留（重启丢失），未持久化

---

## 九、结论

**通过。** Scheduler 已实现受控定时编排，默认关闭，默认不访问真实 FSU，不执行 SET，不修改 alarm_record。
