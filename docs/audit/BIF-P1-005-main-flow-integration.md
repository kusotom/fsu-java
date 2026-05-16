# BIF-P1-005：主链路端到端集成测试与回归矩阵

> 对应阶段：BIF-P1-005
> 完成日期：2026-05-14
> 影响范围：BInterfaceMainFlowIntegrationTest（新增 23 测试）

---

## 一、本阶段目标

对已完成的 B接口主链路（LOGIN → HEARTBEAT → SEND_DATA → SEND_ALARM）进行端到端集成验证，确认四个核心流程可以按顺序协同工作，并形成长期回归测试矩阵。

本阶段不做新业务开发。

---

## 二、当前主链路架构

```
SEND_ALARM ← SEND_DATA ← HEARTBEAT ← LOGIN
                                          ↓
                                    LoginService
                                        ↓
                               ┌───────────────┐
                               │  BInterfaceSession (ACTIVE) │
                               └───────────────┘
                                        ↓
                          ┌─────────────┼─────────────┐
                          ↓             ↓             ↓
                    HEARTBEAT       SEND_DATA      SEND_ALARM
                          ↓             ↓             ↓
                  updateLastSeen  RealtimeData    AlarmRecord
                                  upsert          save/update
                          ↓             ↓             ↓
                    BInterfaceFsu  MonitoringPoint  AlarmRecord
                    StatusEntity  → RealtimeData   → Entity(ACTIVE/
                                  Entity           RECOVERED)
```

### 核心命令链路

```
SOAP Fixture → CommandDispatcher → CommandHandler → Service → Repository(Stub)
```

---

## 三、测试覆盖

### 3.1 测试类

| 测试类 | 位置 | 测试数 | 说明 |
|--------|------|--------|------|
| BInterfaceMainFlowIntegrationTest | binterface/ | 23 | 全链路集成测试 |

### 3.2 测试明细

| 序号 | 测试方法 | 类别 | 验证点 |
|------|---------|------|--------|
| 1 | loginHeartbeatSendDataSendAlarmFullFlowShouldSucceed | 全链路 | LOGIN→HEARTBEAT→SEND_DATA→SEND_ALARM 按序成功 |
| 2 | heartbeatWithoutLoginShouldFail | 顺序依赖 | 未 LOGIN 时 HEARTBEAT 返回 1002 |
| 3 | sendDataWithoutLoginShouldFail | 顺序依赖 | 未 LOGIN 时 SEND_DATA 返回 1002 |
| 4 | sendAlarmWithoutLoginShouldFail | 顺序依赖 | 未 LOGIN 时 SEND_ALARM 返回 1002 |
| 5 | loginThenHeartbeatShouldSucceed | 登录态 | LOGIN 后 HEARTBEAT 成功 |
| 6 | loginThenSendDataShouldSucceed | 登录态 | LOGIN 后 SEND_DATA 成功 |
| 7 | loginThenSendAlarmShouldSucceed | 登录态 | LOGIN 后 SEND_ALARM 成功 |
| 8 | loginShouldCreateSession | 状态变化 | LOGIN 创建 ACTIVE Session |
| 9 | heartbeatShouldUpdateLastSeen | 状态变化 | HEARTBEAT 更新 lastHeartbeat |
| 10 | sendDataShouldPersistRealtimeData | 状态变化 | SEND_DATA 写入 3 条实时数据 |
| 11 | sendAlarmShouldPersistActiveAlarms | 状态变化 | SEND_ALARM 写入 2 条 ACTIVE 告警 |
| 12 | sendAlarmRecoverShouldUpdateAlarmStatus | 状态变化 | 恢复告警将 ACTIVE→RECOVERED |
| 13 | sendDataShouldNotCreateAlarms | 数据隔离 | SEND_DATA 不创建告警 |
| 14 | sendAlarmShouldNotCreateRealtimeData | 数据隔离 | SEND_ALARM 不创建实时数据 |
| 15 | heartbeatShouldNotCreateDataOrAlarms | 数据隔离 | HEARTBEAT 不创建实时数据或告警 |
| 16 | loginShouldNotCreateDataOrAlarms | 数据隔离 | LOGIN 不创建实时数据或告警 |
| 17 | duplicateLoginShouldNotBreakSession | 边界 | 重复登录 session 仍有效 |
| 18 | duplicateHeartbeatShouldKeepUpdatingLastSeen | 边界 | 重复心跳持续更新 lastHeartbeat |
| 19 | sendDataAfterHeartbeatShouldKeepSessionValid | 边界 | HEARTBEAT 后 SEND_DATA 保持 session 有效 |
| 20 | sendAlarmAfterHeartbeatShouldKeepSessionValid | 边界 | HEARTBEAT 后 SEND_ALARM 保持 session 有效 |
| 21 | loginShouldFailOnMissingFsuCode | 缺少字段 | LOGIN 缺少 FSUCode → 2001 |
| 22 | sendDataShouldFailOnMissingFsuCode | 缺少字段 | SEND_DATA 缺少 FSUCode → 2001 |
| 23 | sendAlarmShouldFailOnMissingFsuCode | 缺少字段 | SEND_ALARM 缺少 FSUCode → 2001 |

---

## 四、回归矩阵

### 4.1 LOGIN 回归矩阵

| 测试场景 | 前置条件 | 预期 ResultCode | 预期状态变化 | 涉及 Service | 涉及 Repository | 测试方法 | 通过 |
|----------|---------|----------------|-------------|-------------|----------------|---------|------|
| 正常登录 | FSU 已注册 | 0 | 创建 ACTIVE Session | LoginService | FsuDevice, FsuStatus, Session | loginShouldCreateSession | ✓ |
| 缺少 FSUCode | Info 无 FSUCode | 2001 | 无 | — | — | loginShouldFailOnMissingFsuCode | ✓ |
| 未注册 FSU | FSU 不存在 | 1002 | 无 | LoginService | FsuDevice | LoginServiceTest | ✓ |
| 重复登录 | 已登录 | 0 | 新 Session 替换旧 | LoginService | FsuStatus, Session | duplicateLoginShouldNotBreakSession | ✓ |

### 4.2 HEARTBEAT 回归矩阵

| 测试场景 | 前置条件 | 预期 ResultCode | 预期状态变化 | 涉及 Service | 涉及 Repository | 测试方法 | 通过 |
|----------|---------|----------------|-------------|-------------|----------------|---------|------|
| 正常心跳 | 已 LOGIN | 0 | 更新 lastHeartbeat | LoginService | FsuStatus, Session | heartbeatShouldUpdateLastSeen | ✓ |
| 未登录心跳 | 未 LOGIN | 1002 | 无 | LoginService | — | heartbeatWithoutLoginShouldFail | ✓ |
| 缺少 FSUCode | Info 无 FSUCode | 2001 | 无 | — | — | HeartbeatCommandHandlerTest | ✓ |
| 重复心跳 | 已 LOGIN | 0 | 持续更新 | LoginService | FsuStatus, Session | duplicateHeartbeatShouldKeepUpdatingLastSeen | ✓ |
| 数据隔离 | 已 LOGIN | 0 | 不创建实时数据/告警 | LoginService | — | heartbeatShouldNotCreateDataOrAlarms | ✓ |

### 4.3 SEND_DATA 回归矩阵

| 测试场景 | 前置条件 | 预期 ResultCode | 预期状态变化 | 涉及 Service | 涉及 Repository | 测试方法 | 通过 |
|----------|---------|----------------|-------------|-------------|----------------|---------|------|
| 正常上报 | 已 LOGIN + 测点已配 | 0 | 写入 3 条实时数据 | SendDataService | FsuDevice, MonitorPoint, RealtimeData | sendDataShouldPersistRealtimeData | ✓ |
| 未登录上报 | 未 LOGIN | 1002 | 无 | — | — | sendDataWithoutLoginShouldFail | ✓ |
| 缺少 FSUCode | Info 无 FSUCode | 2001 | 无 | — | — | sendDataShouldFailOnMissingFsuCode | ✓ |
| 无有效数据 | items 为空 | 2003 | 无 | SendDataService | FsuDevice | SendDataServiceTest | ✓ |
| 部分成功 | 部分测点未配 | 0 (partial) | 部分写入 | SendDataService | FsuDevice, MonitorPoint, RealtimeData | SendDataServiceTest | ✓ |
| FSU 未注册 | FSU 不存在 | 1002 | 无 | SendDataService | FsuDevice | SendDataServiceTest | ✓ |
| 数据隔离 | 已 LOGIN | 0 | 不创建告警 | SendDataService | — | sendDataShouldNotCreateAlarms | ✓ |
| 登录态保持 | Heartbeat 后 | 0 | Session 仍有效 | LoginService, SendDataService | — | sendDataAfterHeartbeatShouldKeepSessionValid | ✓ |

### 4.4 SEND_ALARM 回归矩阵

| 测试场景 | 前置条件 | 预期 ResultCode | 预期状态变化 | 涉及 Service | 涉及 Repository | 测试方法 | 通过 |
|----------|---------|----------------|-------------|-------------|----------------|---------|------|
| 正常告警产生 | 已 LOGIN | 0 | 写入 2 条 ACTIVE 告警 | SendAlarmService | FsuDevice, AlarmRecord | sendAlarmShouldPersistActiveAlarms | ✓ |
| 告警恢复 | 已有 ACTIVE 告警 | 0 | ACTIVE→RECOVERED | SendAlarmService | FsuDevice, AlarmRecord | sendAlarmRecoverShouldUpdateAlarmStatus | ✓ |
| 未登录上报 | 未 LOGIN | 1002 | 无 | — | — | sendAlarmWithoutLoginShouldFail | ✓ |
| 缺少 FSUCode | Info 无 FSUCode | 2001 | 无 | — | — | sendAlarmShouldFailOnMissingFsuCode | ✓ |
| 缺少 SignalID | Alarm 项无 SignalID | 2003 | 无 | SendAlarmService | FsuDevice | SendAlarmServiceTest | ✓ |
| 缺少 AlarmCode | Alarm 项无 AlarmCode | 2003 | 无 | SendAlarmService | FsuDevice | SendAlarmServiceTest | ✓ |
| 缺少 AlarmLevel | Alarm 项无 AlarmLevel | 2003 | 无 | SendAlarmService | FsuDevice | SendAlarmServiceTest | ✓ |
| 数据隔离 | 已 LOGIN | 0 | 不创建实时数据 | SendAlarmService | — | sendAlarmShouldNotCreateRealtimeData | ✓ |
| 登录态保持 | Heartbeat 后 | 0 | Session 仍有效 | LoginService, SendAlarmService | — | sendAlarmAfterHeartbeatShouldKeepSessionValid | ✓ |

### 4.5 ResultCode 回归矩阵

| ResultCode | 含义 | LOGIN | HEARTBEAT | SEND_DATA | SEND_ALARM |
|-----------|------|-------|-----------|-----------|------------|
| 0 | 成功 | ✓ | ✓ | ✓ | ✓ |
| 1002 | FSU 未注册/未登录 | ✓ | ✓ | ✓ | ✓ |
| 2001 | 缺少 FSUCode | ✓ | ✓ | ✓ | ✓ |
| 2003 | 无有效数据 | — | — | ✓ | ✓ |
| 5001 | 服务器内部错误 | ✓ | ✓ | ✓ | ✓ |

### 4.6 Repository / Entity 影响矩阵

| Repository | LOGIN | HEARTBEAT | SEND_DATA | SEND_ALARM |
|------------|-------|-----------|-----------|------------|
| FsuDeviceRepository | 查询 fsuCode | 查询 fsuCode | 查询 fsuCode | 查询 fsuCode |
| BInterfaceFsuStatusRepository | 保存 loginStatus | 更新 lastHeartbeat | — | — |
| BInterfaceSessionRepository | 创建 ACTIVE session | 查询 ACTIVE session | — | — |
| MonitoringPointRepository | — | — | 查询 signal→point 映射 | — |
| RealtimeDataRepository | — | — | upsert pointId→data | — |
| AlarmRecordRepository | — | — | — | save/update alarm |

---

## 五、全量测试结果

```
Tests run: 359, Failures: 0, Errors: 0, Skipped: 0
```

较 BIF-P1-004 新增 23 集成测试，所有存量测试全部通过。

---

## 六、未覆盖风险

| 风险 | 说明 | 优先级 |
|------|------|--------|
| 慢数据通道命令未测试 | GET_DATA / SET_THRESHOLD 等尚未实现 | 中（BIF-P2） |
| 无数据库级事务回滚测试 | 当前 stub 不验证事务行为 | 低 |
| 无高并发场景测试 | 单线程 stub 不覆盖竞争条件 | 低 |
| 无真实 SOAP 全链路 | 当前 fixture 驱动手动构造 Context | 低 |
| 告警恢复字段待协议原文确认 | AlarmType 取值待验证 | 低 |

---

## 七、后续建议

1. BIF-P2-001：离线检测 — 定时扫描 lastHeartbeat 超时 FSU，置为 OFFLINE
2. BIF-P2-002：慢数据通道 — GET_DATA / GET_THRESHOLD / SET_THRESHOLD 等
