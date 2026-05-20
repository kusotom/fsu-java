# BIF-P4-020：GET_ACTIVEALARM + ActiveAlarmDiff 编排审计

> 基于 BIF-P4-016 GET_ACTIVEALARM + BIF-P4-018 ActiveAlarmDiff + BIF-P4-019 LocalAlarmSnapshot
> 完成日期：2026-05-19

---

## 一、本阶段目标

将已有的 GET_ACTIVEALARM 能力与 ActiveAlarmDiffService 串联为只读审计编排服务，统一输出 FSU 活动告警与平台 alarm_record 的差异，不自动修复/写入/恢复告警。

---

## 二、协议依据

| 项目 | 值 |
|------|-----|
| 协议 | B接口 2024 标准 |
| 命令 | GET_ACTIVEALARM (Code=603) |
| 方向 | SC → FSU |
| xmlData | TAlarm 列表 (SerialNo, DeviceID, SPID, AlarmLevel, AlarmFlag, StartTime, EndTime, TriggerVal, AlarmDesc) |
| WSDL 端点 | /services/FSUService (RPC/encoded invoke) |

---

## 三、架构设计

```
ActiveAlarmConsistencyAuditService (Orchestrator)
  ├── GetActiveAlarmService (FSU BInterface 查询)
  │     ├── FsuServiceClient.call()
  │     └── XmlDataModel 解析 → List<ActiveAlarmItem>
  ├── LocalActiveAlarmSnapshotService (本地只读投影)
  │     ├── FsuDeviceRepository.findByFsuCode()
  │     ├── AlarmRecordRepository.findByFsuId()
  │     └── AlarmRecordEntity → LocalAlarmSnapshot 映射
  └── ActiveAlarmDiffService (差异核对引擎)
        ├── SerialNo 匹配 → DeviceID+SPID+AlarmFlag 降级匹配
        ├── 字段比较 (AlarmLevel, AlarmFlag, TriggerVal, DeviceID, SPID, AlarmDesc)
        └── 四类差异输出: MATCHED / FSU_ONLY / LOCAL_ONLY / FIELD_MISMATCH
```

### 双模式编排

| 模式 | 方法 | 访问 FSU | 适用场景 |
|------|------|---------|---------|
| 快照模式 | `auditWithProvidedSnapshot()` | 否 | 已有 FSU 快照数据直接 diff |
| 查询模式 | `auditByQueryingFsu()` | 是 (Stub/Real) | 完整链路: FSU查询→本地→diff |

---

## 四、涉及文件

### 核心文件 (BIF-P4-020)

| 文件 | 说明 |
|------|------|
| `binterface/service/ActiveAlarmConsistencyAuditService.java` | 编排服务: 双模式审计 |
| `binterface/service/ActiveAlarmConsistencyAuditResult.java` | 审计结果模型: fsuCount/localCount/matched/fsuOnly/localOnly/mismatch |

### 依赖文件 (前序阶段)

| 文件 | 阶段 | 说明 |
|------|------|------|
| `binterface/service/GetActiveAlarmService.java` | BIF-P4-016 | FSU 活动告警查询 |
| `binterface/service/GetActiveAlarmResult.java` | BIF-P4-016 | 查询结果 + ActiveAlarmItem |
| `binterface/service/ActiveAlarmDiffService.java` | BIF-P4-018 | 差异核对引擎 |
| `binterface/service/ActiveAlarmDiffResult.java` | BIF-P4-018 | 差异结果 + DiffItem/DiffType |
| `binterface/service/LocalActiveAlarmSnapshotService.java` | BIF-P4-019 | alarm_record 只读投影 |
| `alarm/repository/AlarmRecordRepository.java` | — | 告警记录 Repository (只读) |

### 测试文件

| 文件 | 测试数 |
|------|--------|
| `ActiveAlarmConsistencyAuditServiceTest.java` | 13 |
| `GetActiveAlarmServiceTest.java` | 11 |
| `ActiveAlarmDiffServiceTest.java` | 14 |
| `LocalActiveAlarmSnapshotServiceTest.java` | 8 |

---

## 五、B接口协议一致性

- GET_ACTIVEALARM 使用 BInterfacePkType.GET_ACTIVEALARM (Code=603)
- xmlData 解析兼容 TAlarm 标准字段 (SerialNo, DeviceID, SPID, AlarmLevel, AlarmFlag, TriggerVal, AlarmDesc)
- Info XML 格式: `<SUID>{fsuCode}</SUID>`
- FSU 通信走 FsuServiceClient → FsuServiceRpcAdapter → SOAP RPC/encoded invoke
- 无自定义协议或私有字段

---

## 六、只读安全边界

| 检查项 | 状态 |
|--------|------|
| 不调用 alarmRepo.save() | 测试验证 |
| 不调用 alarmRepo.delete() | 测试验证 |
| 不自动恢复/清除告警 | 编排逻辑无写入 |
| 不访问真实 FSU (Stub 模式) | real-call-enabled=false |
| 不启用 Scheduler | scheduler-enabled=false |
| 不执行 SET 命令 | 白名单外命令拒绝 |
| Repository 只使用 findBy 查询方法 | AlarmRecordRepository 只读 |

---

## 七、错误处理矩阵

| 场景 | 处理 | 覆盖 |
|------|------|------|
| SUID null/empty | 返回 invalid 结果 | 测试 |
| FSU 返回非 0 ResultCode | 返回 fsuQueryFailed | 测试 |
| GET_ACTIVEALARM 调用异常 | 返回 fsuQueryFailed + 错误信息 | 测试 |
| xmlData 解析失败 | GetActiveAlarmService 统计 invalidCount | 测试 |
| 本地 Repository 异常 | 返回 fsuQueryFailed | 测试 |
| FSU 返回空活动告警 | 正常返回, fsuCount=0 | 测试 |
| 本地无活动告警 | 正常返回, localCount=0 | 测试 |
| 两边都为空 | 正常返回, 无差异 | 测试 |
| 字段不一致 | FIELD_MISMATCH diff | 测试 |

---

## 八、测试结果

ActiveAlarm 相关测试: **46 tests, 0 failures**
- ActiveAlarmConsistencyAuditServiceTest: 13 (含 FSU 失败路径 3)
- GetActiveAlarmServiceTest: 11
- ActiveAlarmDiffServiceTest: 14
- LocalActiveAlarmSnapshotServiceTest: 8

全量回归: 见工程记忆。

---

## 九、遗留问题

- 全量测试中 DcimPlatformApplicationTests.contextLoads 存在预存 Bean 冲突 Error (BInterfaceMessageLogRepository 重复定义)
- FSU 管理方未提供真实点位表, 未生成 seed SQL
- alarm_record Entity 缺少 serialNo/deviceId 字段, Snapshot 映射为 null, 限制 SerialNo 匹配精度

---

## 十、下一步建议

- BIF-P4-021: Scheduler 定时 GET_ACTIVEALARM 差异审计
- 或等待 FSU 管理方点位表 → seed SQL → 真实点位联调
- 或继续 2024 命令实现 (SET_SCIP, 配置系列等)
