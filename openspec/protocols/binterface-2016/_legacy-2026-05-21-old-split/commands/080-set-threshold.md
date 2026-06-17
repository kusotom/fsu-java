# SET_THRESHOLD — 设置告警门限

## 1. 协议定位

- 命令名称：SET_THRESHOLD
- ACK 名称：SET_THRESHOLD_ACK
- 方向：SC→FSU
- 发起方：SC（监控中心）
- 接收方：FSU（现场监控单元）
- 功能说明：SC 向 FSU 设置信号量的告警门限参数（上限、下限、严重上限、严重下限）。

## 2. 命令码

| 项目 | Code |
|------|------|
| 请求 | 2001 |
| 响应 | 2002 |

## 3. 请求 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| FSUCode | String | 是 | FSU 编码 | `request.info.FSUCode` | FSUCode |
| SessionID | String | 是 | 会话 ID | `request.info.SessionID` | Not used in SetThresholdService |
| RequestTime | DateTime | 是 | 请求发送时间 | `request.info.RequestTime` | Not used |

## 4. 响应 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| ResultCode | Integer | 是 | 0=成功，非0=失败 | `response.info.ResultCode` | ResultCode |
| Count | Integer | 否 | 设置成功的信号量数 | `response.info.Count` | Count |

## 5. 协议 XML 样例

### 请求样例

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request xmlns="http://FSUService.chinatowercom.com">
      <PK_Type>SET_THRESHOLD</PK_Type>
      <Info>
        <FSUCode>51051243812345</FSUCode>
        <SessionID>SESSION-20260520120000</SessionID>
        <RequestTime>2026-05-20 12:00:00</RequestTime>
      </Info>
      <xmlData>
        <Signal>
          <SignalID>T1-TEMP-001</SignalID>
          <AlarmUpper>30.0</AlarmUpper>
          <AlarmLower>10.0</AlarmLower>
          <AlarmUpperUrgent>40.0</AlarmUpperUrgent>
          <AlarmLowerUrgent>0.0</AlarmLowerUrgent>
        </Signal>
      </xmlData>
    </Request>
  </soap:Body>
</soap:Envelope>
```

### 响应样例

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response xmlns="http://FSUService.chinatowercom.com">
      <PK_Type>SET_THRESHOLD_ACK</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <Count>1</Count>
      </Info>
    </Response>
  </soap:Body>
</soap:Envelope>
```

## 6. 数据结构引用

- TThreshold: `06-data-structures.md` §3 — SignalID, AlarmUpper, AlarmLower, AlarmUpperUrgent, AlarmLowerUrgent

## 7. 成功/失败判断

- 成功: ResultCode=0
- 失败: ResultCode ≠ 0
  - 2001: 参数错误（缺少 FSUCode 或 SignalID）
  - 3001: 安全门禁拒绝（功能未启用）
  - 3002: 安全门禁拒绝（缺少二次确认）
  - 5001: 服务器内部错误

## 8. 项目实现映射

| 层级 | 文件/类 | 状态 | 风险 |
|------|---------|------|------|
| Controller | `ScServiceController.java` | ✅ | 间接调用 |
| Handler | `SetThresholdCommandHandler.java` | ✅ 已实现+安全闭环 | 安全门禁不通过时返回 3001/3002 |
| Service | `SetThresholdService.java` | ✅ 已实现 | — |
| Service | `SetThresholdSafetyGate.java` | ✅ 已实现 | 默认 enabled=false, requireConfirmation=true |
| Service | `SetThresholdAuditService.java` | ✅ 已实现 | 审计日志 |
| Entity | `SetThresholdResult.java` | ✅ | — |
| Test | `SetThresholdServiceTest.java` | ✅ | — |
| Test | `SetThresholdCommandHandlerTest.java` | ✅ | — |
| Test | `SetThresholdSafetyGateTest.java` | ✅ | — |
| Test | `SetThresholdAuditServiceTest.java` | ✅ | — |
| Test | `SetThresholdSlowChannelIntegrationTest.java` | ✅ | — |
| Test | `SetThresholdSafetyGateTest.xml` | ✅ | — |

## 9. 真实 FSU 实测差异

- 未在真实 FSU 上执行 SET_THRESHOLD（安全禁用，仅执行只读命令）

## 10. 验收标准

1. 请求 XML 符合 B接口 2016 格式要求
2. 响应成功时包含 ResultCode=0 和 Count
3. 安全门禁未启用时返回 3001
4. 缺少二次确认时返回 3002
5. BInterfaceCommand2016 需增加 SET_THRESHOLD=2001 条目

## 11. 安全边界

- 禁止默认执行: `b-interface.set-threshold.enabled=false`（默认关闭）
- 需二次确认: `b-interface.set-threshold.require-confirmation=true`（默认开启）
- 所有操作需记录审计日志（`SetThresholdAuditService`）
- SET类命令需安全门禁，`safe_enabled=false`
- 不输出信号量门限原始值到未脱敏日志
