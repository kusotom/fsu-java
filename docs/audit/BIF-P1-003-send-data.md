# BIF-P1-003：SEND_DATA 实时数据上报闭环

> 对应阶段：BIF-P1-003
> 完成日期：2026-05-14
> 影响范围：SendDataCommandHandler（桩→真实实现）/ SendDataService（新建）/ SendDataResult（新建）/ SendDataServiceTest（20 测试）/ SendDataCommandHandlerTest（19 测试）

---

## 一、架构边界

### 1.1 SEND_DATA 在完整处理链路中的位置

```
HTTP SOAP Request (SEND_DATA)
  ↓
ScServiceController               ← HTTP/SOAP 收发
  ↓
SoapMessageHandler.parse()        ← SOAP Envelope 解析
  ↓
CommandDispatcher.dispatch()      ← 路由分发 + XmlDataParser 调用
  ↓ (BInterfacePkType.SEND_DATA)
SendDataCommandHandler.handle()   ← 提取 FSUCode + CollectTime, 校验登录, 委托 Service
  ↓
  ├─ LoginService.isLoggedIn()    ← 检查 FSU 登录态
  └─ SendDataService.processData() ← Signal 解析 + 测点映射 + RealtimeData 入库
      ↓
      ├─ findByFsuIdAndPointCode() ← 测点映射
      └─ upsertRealtimeData()     ← RealtimeDataEntity upsert
  ↓
SendDataResult                    ← 业务结果（含 accepted/rejected 统计）
  ↓
CommandResult                     ← 统一响应模型
  ↓
SoapMessageHandler.buildResponse()← SOAP Envelope 构造
  ↓
HTTP SOAP Response (SEND_DATA 响应)
```

### 1.2 各层职责

| 层 | 职责 | 不允许 |
|----|------|--------|
| SendDataCommandHandler | 提取 Info 字段（FSUCode/CollectTime），校验登录，委托 SendDataService，构造响应 | 访问数据库、直接操作实体、处理 SOAP Envelope |
| SendDataService | 解析 Signal 项，测点映射 upsert，统计 accepted/rejected | 解析 XML、构造 SOAP 响应、处理 Handler 逻辑 |
| LoginService | 提供 isLoggedIn（复用） | 不涉及 SEND_DATA 业务 |

### 1.3 与 LOGIN/HEARTBEAT 的关系

```
LOGIN → Session (ACTIVE)
  ↓
HEARTBEAT → 保活校验
  ↓
SEND_DATA → 校验登录态 + Signal 数据入库
  ↓
... 后续 SEND_ALARM 复用同一登录态校验入口 ...
```

SEND_DATA 依赖 LOGIN 建立的 Session，复用 LoginService.isLoggedIn()。

---

## 二、SEND_DATA 协议

### 2.1 请求

```xml
<Request>
  <PK_Type>SEND_DATA</PK_Type>
  <Info>
    <FSUCode>FSU-001</FSUCode>
    <SessionID>SESSION-FSU-001-...</SessionID>
    <CollectTime>2026-05-14T10:30:00+08:00</CollectTime>
  </Info>
  <xmlData>
    <Signal>
      <SignalID>TEMP-001</SignalID>
      <Value>25.5</Value>
      <Quality>0</Quality>
      <Status>normal</Status>
    </Signal>
    <Signal>
      <SignalID>HUMI-001</SignalID>
      <Value>60.2</Value>
      <Quality>0</Quality>
      <Status>normal</Status>
    </Signal>
  </xmlData>
</Request>
```

Info 字段：FSUCode（必需）、SessionID（可选）、CollectTime（可选，ISO8601）

xmlData：零或多个 `<Signal>` 重复项，每项含 SignalID（必需）/ Value（必需）/ Quality（可选）/ Status（可选）

### 2.2 响应

```xml
<Response>
  <PK_Type>SEND_DATA</PK_Type>
  <Info>
    <ResultCode>0</ResultCode>
    <Count>5</Count>
  </Info>
  <xmlData/>
</Response>
```

Info 字段：ResultCode、Count（实际处理成功数）

xmlData：空（符合协议 fixture）

### 2.3 ResultCode 策略

| ResultCode | 含义 | 触发条件 |
|-----------|------|---------|
| 0 | 成功（含部分成功） | 至少一条 Signal 处理成功 |
| 1002 | FSU 未注册 | FSU 在 fsu_device 表中不存在 |
| 1002 | FSU 未登录 | isLoggedIn 返回 false |
| 2001 | 缺少 FSUCode | Info 中无 FSUCode 或 Info 为空 |
| 2003 | 无有效测点数据 | 所有 Signal 均被拒绝或 items 为空或 xmlData 为空 |
| 5001 | 服务器内部错误 | 处理异常 |

### 2.4 Signal 处理规则

单条 Signal 项的处理结果：

| 条件 | 结果 |
|------|------|
| SignalID 为空 | rejected，错误"缺少 SignalID" |
| Value 为空 | rejected，错误"缺少 Value" |
| SignalID 在 monitoring_point 无对应 | rejected，错误"未找到对应测点" |
| parseSignal 异常 | rejected，错误"Signal处理异常" |
| 全部通过 | accepted，upsert 到 realtime_data |

Value 的 numeric 解析：`new BigDecimal()` 成功则写入 valueNumber，失败（非数值）则 valueNumber = null，valueText 始终保留原始字符串。

---

## 三、数据模型映射

### 3.1 Signal 字段 → RealtimeDataEntity

| Signal 字段 | 目标字段 | 说明 |
|------------|---------|------|
| SignalID | pointCode | 传递测点编码 |
| Value | valueText | 原始字符串，始终保留 |
| Value | valueNumber | BigDecimal 解析，非数值时为 null |
| Quality | quality | 透传 |
| Status | valueStatus | 透传 |

### 3.2 映射关系

```
Signal.SignalID → MonitoringPoint.pointCode → MonitoringPoint.id
                                                      ↓
                                            RealtimeData.pointId = MonitoringPoint.id
```

### 3.3 Upsert 策略

`RealtimeDataRepository.findByPointId()` 查找：
- 存在 → 更新字段（collectTime, valueText, valueNumber, quality, status, receiveTime, updatedAt）
- 不存在 → 新建实体（设置所有字段，包括 createdAt）

---

## 四、Session 校验策略

SEND_DATA 复用 LoginService.isLoggedIn()（与 HEARTBEAT 相同），不创建、不修改 Session。

| 校验 | 实现 | 失败后果 |
|------|------|---------|
| FSU 注册 | fsuDeviceRepository.findByFsuCode() → SendDataService | 1002 |
| FSU 登录 | LoginService.isLoggedIn() → SendDataCommandHandler | 1002 |

---

## 五、测试覆盖

### SendDataServiceTest（20 测试）

| 类别 | 测试方法 | 验证点 |
|------|---------|--------|
| 成功 | shouldProcessAllSignalsSuccessfully | 5 signals → accepted=5, rejected=0 |
| 成功 | shouldPersistRealtimeDataOnSuccess | 单条 upsert 正确性 |
| 成功 | shouldUpsertExistingRealtimeData | 已存在数据时更新而非新建 |
| 成功 | shouldParseNumericValuesCorrectly | 数值/非数值字段正确 |
| 部分成功 | shouldReportPartialSuccessWhenSomeSignalsFail | 3/5 → accepted=3, rejected=2 |
| 部分成功 | shouldIncludeErrorMessagesInPartialResult | errors 列表包含 SignalID |
| 全量失败 | shouldFailWhenAllSignalsRejected | 0/5 → resultCode=2003 |
| 全量失败 | shouldFailForNullXmlData | null xmlData → 2003 |
| 全量失败 | shouldFailForEmptyItems | 空 items → 2003 |
| 缺少字段 | shouldRejectSignalWithMissingSignalId | 无 SignalID → 2003 |
| 缺少字段 | shouldRejectSignalWithMissingValue | 无 Value → 2003 |
| 未知 FSU | shouldFailForUnknownFsu | FSU-UNKNOWN → 1002 |
| CollectTime | shouldUseProvidedCollectTime | ISO8601 时间正确解析 |
| CollectTime | shouldUseDefaultTimeForInvalidCollectTime | 非 ISO8601 → 使用当前时间 |
| Utility | parseNumericShouldReturnNullForNonNumeric | abc/空/null → null |
| Utility | parseNumericShouldReturnBigDecimalForValidNumber | 25.5/0/-1.5 正确解析 |
| Utility | parseCollectTimeShouldReturnDefaultForNull | null 返回默认 |
| Utility | parseCollectTimeShouldReturnDefaultForEmpty | "" 返回默认 |
| Quality/Status | shouldPreserveQualityAndStatus | Quality/Status 透传入库 |
| 混合 | shouldHandleMultipleSignalsWithPartialErrors | 5 条混合结果 → accepted=2, rejected=3 |

### SendDataCommandHandlerTest（19 测试）

| 类别 | 测试方法 | 验证点 |
|------|---------|--------|
| PK_Type | shouldReturnSendDataPkType | getSupportedPkType() = SEND_DATA |
| 成功 | shouldHandleSuccessfulSendData | ResultCode=0, implemented=true |
| 成功 | shouldIncludeCountInResponse | 响应包含 Count=3 |
| 成功 | shouldPassCollectTimeToService | CollectTime 从 Info 提取传递至 Service |
| 成功 | shouldHandleWithXmlDataInContext | xmlData 正确传递至 Service |
| 错误 | shouldFailOnNullContext | 5001 |
| 错误 | shouldFailOnNullSoapMessage | 2001 |
| 错误 | shouldFailOnMissingFsuCode | Info 中无 FSUCode → 2001 |
| 错误 | shouldFailOnNotLoggedIn | 1002 |
| 错误 | shouldFailOnServiceError | Service 返回 fail → 透传错误码 |
| 错误 | shouldHandleServiceException | Service 抛异常 → 5001 |
| 响应格式 | shouldReturnXmlDataNullInResponse | 响应 xmlData = null |
| extractCollectTime | 7 个测试 | 正常/大小写/空白/null/空标签/不存在 |

### 全量测试

```
Tests run: 297, Failures: 0, Errors: 0, Skipped: 0
```

较 BIF-P1-002 新增 39 测试（SendDataServiceTest 20 + SendDataCommandHandlerTest 19），所有存量测试全部通过。

---

## 六、风险与遗留问题

### 6.1 风险

| 风险 | 等级 | 说明 |
|------|------|------|
| SendDataService 直接访问 telemetry 模块的 RealtimeDataEntity | 低 | binterface → telemetry 单向依赖，目前可接受 |
| 对非数值 Value 不做报警/校验 | 低 | 当前仅透传 valueText，后续可在 SEND_DATA 增强阶段补充 |
| 无 SignalID 前缀校验 | 低 | 未对 SignalID 格式做校验，完全依赖 monitoring_point 映射 |

### 6.2 本阶段未处理

- 历史数据归档（history_data 写入）
- 批量 Signal 的事务粒度优化（当前为单事务全量处理）
- Signal Value 超阈值报警触发
- Quality/Status 的业务语义校验
- SignalID 前缀/格式校验

### 6.3 下一步建议

1. **BIF-P1-004：SEND_ALARM** — 告警数据上报闭环
2. **BIF-P2-001：离线检测** — 基于 lastHeartbeat 的定时扫描任务
3. **BIF-P2-002：慢数据通道** — GET_DATA / SET_THRESHOLD 等 SC→FSU 命令
