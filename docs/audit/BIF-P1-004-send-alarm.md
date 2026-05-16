# BIF-P1-004：SEND_ALARM 告警上报闭环

> 对应阶段：BIF-P1-004
> 完成日期：2026-05-14
> 影响范围：SendAlarmCommandHandler（桩→真实实现）/ SendAlarmService（新建）/ SendAlarmResult（新建）/ AlarmRecordRepository（扩展 query 方法）/ SendAlarmServiceTest（20 测试）/ SendAlarmCommandHandlerTest（19 测试）

---

## 一、架构边界

### 1.1 SEND_ALARM 在完整处理链路中的位置

```
HTTP SOAP Request (SEND_ALARM)
  ↓
ScServiceController               ← HTTP/SOAP 收发
  ↓
SoapMessageHandler.parse()        ← SOAP Envelope 解析
  ↓
CommandDispatcher.dispatch()      ← 路由分发 + XmlDataParser 调用
  ↓ (BInterfacePkType.SEND_ALARM)
SendAlarmCommandHandler.handle()  ← 提取 FSUCode + AlarmTime, 校验登录, 委托 Service
  ↓
  ├─ LoginService.isLoggedIn()    ← 检查 FSU 登录态
  └─ SendAlarmService.processAlarms() ← Alarm 解析 + 产生/恢复判定 + 持久化
      ↓
      ├─ AlarmRecordRepository.save()   ← 新告警产生或状态更新
      └─ AlarmRecordRepository.findBy...() ← 恢复时查找 ACTIVE 告警
  ↓
SendAlarmResult                   ← 业务结果（含 accepted/recovered/rejected 统计）
  ↓
CommandResult                     ← 统一响应模型
  ↓
SoapMessageHandler.buildResponse()← SOAP Envelope 构造
  ↓
HTTP SOAP Response (SEND_ALARM 响应)
```

### 1.2 各层职责

| 层 | 职责 | 不允许 |
|----|------|--------|
| SendAlarmCommandHandler | 提取 Info 字段（FSUCode/AlarmTime），校验登录，委托 SendAlarmService，构造响应 | 访问数据库、直接操作实体、处理 SOAP Envelope |
| SendAlarmService | 解析 Alarm 项，产生/恢复判定，持久化，统计 | 解析 XML、构造 SOAP 响应、处理 Handler 逻辑 |
| LoginService | 提供 isLoggedIn（复用） | 不涉及 SEND_ALARM 业务 |

### 1.3 与 LOGIN / HEARTBEAT / SEND_DATA 的关系

```
LOGIN → Session (ACTIVE)
  ↓
HEARTBEAT → 保活校验
  ↓
SEND_DATA → 实时数据上报
  ↓
SEND_ALARM → 校验登录态 + 告警数据入库
```

SEND_ALARM 依赖 LOGIN 建立的 Session，复用 LoginService.isLoggedIn()。SEND_ALARM 不创建 Session，不注册 FSU，不调用 SendDataService。

SEND_ALARM 成功后不强制调用 updateLastSeen（当前设计如此，后续可追加）。

---

## 二、SEND_ALARM 协议

### 2.1 请求

```xml
<Request>
  <PK_Type>SEND_ALARM</PK_Type>
  <Info>
    <FSUCode>FSU-001</FSUCode>
    <SessionID>SESSION-FSU-001-...</SessionID>
    <AlarmTime>2026-05-13T10:33:00+08:00</AlarmTime>
  </Info>
  <xmlData>
    <Alarm>
      <SignalID>TEMP-001</SignalID>
      <AlarmCode>TEMP-HIGH</AlarmCode>
      <AlarmName>机柜温度过高</AlarmName>
      <AlarmLevel>WARN</AlarmLevel>
      <AlarmValue>62.0</AlarmValue>
      <AlarmDesc>温度超过上限60°C</AlarmDesc>
      <AlarmType>0</AlarmType>
    </Alarm>
  </xmlData>
</Request>
```

Info 字段：FSUCode（必需）、SessionID（可选）、AlarmTime（可选，ISO8601）

xmlData：零或多个 `<Alarm>` 重复项，每项字段：

| 字段 | 必需 | 说明 |
|------|------|------|
| SignalID | 是 | 测点编码 |
| AlarmCode | 是 | 告警编码 |
| AlarmLevel | 是 | 告警级别（WARN/CRITICAL 等） |
| AlarmType | 否（默认产生） | 0=产生, 1=恢复 |
| AlarmName | 否 | 告警名称 |
| AlarmValue | 否 | 告警值 |
| AlarmDesc | 否 | 告警描述 |

### 2.2 响应

```xml
<Response>
  <PK_Type>SEND_ALARM</PK_Type>
  <Info>
    <ResultCode>0</ResultCode>
    <AlarmID>10001</AlarmID>
  </Info>
  <xmlData/>
</Response>
```

Info 字段：ResultCode、AlarmID（最后处理的告警记录 ID）

xmlData：空（符合协议 fixture）

### 2.3 ResultCode 策略

| ResultCode | 含义 | 触发条件 |
|-----------|------|---------|
| 0 | 成功（含部分成功） | 至少一条告警处理成功 |
| 1002 | FSU 未注册或未登录 | FSU 在 fsu_device 表中不存在或 isLoggedIn=false |
| 2001 | 缺少 FSUCode | Info 中无 FSUCode 或 Info 为空 |
| 2003 | 无有效告警数据 | 所有告警均被拒绝或 items 为空或 xmlData 为空 |
| 5001 | 服务器内部错误 | 处理异常 |

> ResultCode 策略复用 BIF-P1-001~003 已有错误码体系。2003 在 SEND_DATA 中已引入，此处复用同一语义"无有效数据"。

### 2.4 告警产生 / 恢复策略

| AlarmType | 语义 | 操作 |
|-----------|------|------|
| "0"（默认） | 告警产生 | 新建 AlarmRecordEntity，alarmStatus="ACTIVE" |
| "1" | 告警恢复 | 查找 fsuId+pointCode+alarmCode+alarmStatus="ACTIVE" 的记录，更新为 "RECOVERED"，设置 clearTime；如未找到原告警，以恢复状态直接创建记录 |

> AlarmType 取值目前基于 B接口协议 2016 常见约定，待协议原文最终校验确认。
> 告警恢复目前只支持一对一匹配（按 fsuId+pointCode+alarmCode 查找未恢复告警）。

---

## 三、数据模型映射

### 3.1 Alarm 字段 → AlarmRecordEntity

| Alarm 字段 | 实体字段 | 必需 | 说明 |
|-----------|---------|------|------|
| SignalID | pointCode | 是 | 测点编码 |
| AlarmCode | alarmCode | 是 | 告警编码 |
| AlarmName | alarmName | 否 | 告警名称 |
| AlarmLevel | alarmLevel | 是 | 告警级别，Entity NOT NULL |
| AlarmValue | alarmValue | 否 | 告警值 |
| AlarmDesc | alarmDesc | 否 | 告警描述 |
| AlarmType | alarmStatus（派生） | 否 | 0→ACTIVE, 1→RECOVERED |
| AlarmTime(Info) | occurTime | 否 | 告警发生时间 |

### 3.2 使用现有告警模型

复用 `com.dcim.platform.module.alarm` 模块的完整基础设施：

- **AlarmRecordEntity** — 已有完整字段（fsuId, pointCode, alarmCode, alarmName, alarmLevel, alarmStatus, alarmValue, alarmDesc, occurTime, clearTime 等）
- **AlarmRecordRepository** — 扩展 `findByFsuIdAndPointCodeAndAlarmCodeAndAlarmStatus` 查询方法（新增一行）
- **AlarmRecordService** — 仅提供只读查询接口（list, getById, listByStatus），SEND_ALARM 不走此 Service

---

## 四、Session 校验策略

SEND_ALARM 复用 LoginService.isLoggedIn()（与 HEARTBEAT/SEND_DATA 相同），不创建、不修改 Session。

| 校验 | 实现 | 失败后果 |
|------|------|---------|
| FSU 注册 | fsuDeviceRepository.findByFsuCode() → SendAlarmService | 1002 |
| FSU 登录 | LoginService.isLoggedIn() → SendAlarmCommandHandler | 1002 |

---

## 五、测试覆盖

### SendAlarmServiceTest（20 测试）

| 类别 | 测试方法 | 验证点 |
|------|---------|--------|
| 产生 | shouldProcessSingleAlarmSuccessfully | 1 alarm → accepted=1 |
| 产生 | shouldPersistAlarmRecordOnGenerate | 字段正确映射、alarmStatus=ACTIVE |
| 产生 | shouldProcessMultipleAlarms | 3 alarms → accepted=3 |
| 恢复 | shouldRecoverActiveAlarm | ACTIVE→RECOVERED, clearTime 设置 |
| 恢复 | shouldCreateRecoveredRecordWhenNoActiveAlarmFound | 无原告警时创建 RECOVERED 记录 |
| 混合 | shouldHandleMixedGenerateAndRecover | 2 产生 + 1 恢复 |
| 校验 | shouldRejectAlarmWithMissingSignalId | 2003 |
| 校验 | shouldRejectAlarmWithMissingAlarmCode | 2003 |
| 校验 | shouldRejectAlarmWithMissingAlarmLevel | 2003 |
| FSU | shouldFailForUnknownFsu | FSU-UNKNOWN → 1002 |
| 空数据 | shouldFailForNullXmlData | null xmlData → 2003 |
| 空数据 | shouldFailForEmptyItems | 空 items → 2003 |
| 时间 | shouldUseProvidedAlarmTime | ISO8601 正确解析 |
| 时间 | shouldUseDefaultTimeForInvalidAlarmTime | 非法格式→当前时间 |
| Utility | parseAlarmTimeShouldReturnDefaultForNull | null 返回默认 |
| Utility | parseAlarmTimeShouldReturnDefaultForEmpty | "" 返回默认 |
| 部分成功 | shouldReportPartialSuccess | 1 成功 + 1 拒绝 |
| 重复 | shouldCreateSeparateRecordsForSameAlarmCode | 同 alarmCode 多次产生 |
| 最小字段 | shouldHandleAlarmWithMinimalRequiredFields | 仅 SignalID+AlarmCode+AlarmLevel |
| 自动注册 | shouldNotRegisterUnknownFsu | 不存在的 FSU 不被自动创建 |

### SendAlarmCommandHandlerTest（19 测试）

| 类别 | 测试方法 | 验证点 |
|------|---------|--------|
| PK_Type | shouldReturnSendAlarmPkType | getSupportedPkType() = SEND_ALARM |
| 成功 | shouldHandleSuccessfulSendAlarm | ResultCode=0, implemented=true |
| 成功 | shouldIncludeAlarmIdInResponse | 响应包含 AlarmID |
| 成功 | shouldPassAlarmTimeToService | AlarmTime 从 Info 提取传递至 Service |
| 成功 | shouldHandleWithXmlDataInContext | xmlData 正确传递至 Service |
| 错误 | shouldFailOnNullContext | 5001 |
| 错误 | shouldFailOnNullSoapMessage | 2001 |
| 错误 | shouldFailOnMissingFsuCode | 2001 |
| 错误 | shouldFailOnNotLoggedIn | 1002 |
| 错误 | shouldFailOnServiceError | Service fail → 透传 |
| 错误 | shouldHandleServiceException | Service 抛异常 → 5001 |
| 响应 | shouldReturnXmlDataNullInResponse | 响应 xmlData = null |
| extractAlarmTime | 7 个测试 | 正常/大小写/空白/null/空标签/不存在 |

### 全量测试

```
Tests run: 336, Failures: 0, Errors: 0, Skipped: 0
```

较 BIF-P1-003 新增 39 测试（SendAlarmServiceTest 20 + SendAlarmCommandHandlerTest 19），所有存量测试全部通过。

---

## 六、风险与遗留问题

### 6.1 风险

| 风险 | 等级 | 说明 |
|------|------|------|
| AlarmType 语义（0=产生, 1=恢复）待协议原文确认 | 低 | 当前基于常见约定实现 |
| 无 DeviceID 处理 | 低 | 当前 fixture 中 Alarm 项不含 DeviceID；如需支持，后续扩展 AlarmParseResult |
| 告警恢复仅按 fsuId+pointCode+alarmCode 匹配 | 低 | 未考虑多个 ACTIVE 同类型告警场景 |
| SEND_ALARM 不更新 lastSeen | 低 | 当前设计如此，后续可追加（SEND_ALARM 也是保活信号之一） |

### 6.2 本阶段未处理

- 告警通知推送
- 告警统计报表
- 告警确认/清除 UI
- history_data 回写
- SEND_ALARM 触发 updateLastSeen

### 6.3 下一步建议

1. **BIF-P2-001：离线检测** — 基于 lastHeartbeat 的定时扫描任务
2. **BIF-P2-002：慢数据通道** — GET_DATA / SET_THRESHOLD 等 SC→FSU 命令
3. 告警确认/清除前端页面
