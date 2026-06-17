# SEND_ALARM -- 告警上报

## 1. 协议定位

- 命令名称：SEND_ALARM
- ACK 名称：SEND_ALARM_ACK
- 方向：FSU --> SC
- 发起方：FSU
- 接收方：SC
- 功能说明：FSU 主动向 SC 上报告警事件。包含告警产生（AlarmType=0）和告警恢复（AlarmType=1）两种类型。

## 2. 命令码

| 项目 | Code |
|------|------|
| 请求 | 501 |
| 响应 | 502 |

## 3. 请求 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| FSUCode | String | 是 | FSU 设备编码 | `info.fsuCode` | ✅ 正则从 infoXml 提取 |
| SessionID | String | 是 | 会话 ID | `info.sessionId` | 当前未在 Handler 中校验 |
| AlarmTime | DateTime | 是 | 告警发生时间（ISO8601） | `info.alarmTime` | ✅ 正则从 infoXml 提取，解析失败时使用当前时间 |

## 4. 响应 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| ResultCode | Integer | 是 | 0=成功，非0=失败 | `resultCode` | ✅ |
| AlarmID | Integer | 否 | 服务端生成的告警 ID（最后一条告警的 ID） | `alarmId` | ✅ 取 alarmIds 列表最后一项 |

## 5. 协议 XML 样例

### 请求样例

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>SEND_ALARM</PK_Type>
      <Info>
        <FSUCode>FSU-001</FSUCode>
        <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
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
  </soap:Body>
</soap:Envelope>
```

### 响应样例

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>SEND_ALARM</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <AlarmID>10001</AlarmID>
      </Info>
      <xmlData/>
    </Response>
  </soap:Body>
</soap:Envelope>
```

## 6. 数据结构引用

- Alarm xmlData 结构：参见 `06-data-structures.md` TAlarm 结构
- AlarmType：0=告警产生，1=告警恢复
- ResultCode：参见 `03-constants.md` ResultCode 定义
- AlarmLevel：WARN（告警）、CRITICAL（严重）、INFO（提示）等

## 7. 成功/失败判断

| 条件 | ResultCode | 说明 |
|------|-----------|------|
| 至少一条告警成功处理 | 0 | 成功（含部分成功，partial 模式） |
| FSUCode 为空 | 2001 | 缺少 FSUCode |
| FSU 未注册 | 1002 | FSU 未注册 |
| xmlData 为空或 Alarm 列表为空 | 2003 | 无有效告警数据 |
| 全部告警项校验失败 | 2003 | 无有效告警数据 |
| 服务端异常 | 5001 | 处理异常 |

## 8. 项目实现映射

| 层级 | 文件/类 | 状态 | 风险 |
|------|---------|------|------|
| Controller | ScServiceController / StandardScServiceController | ✅ 已实现 | 入口接收 SOAP/XML |
| Handler | SendAlarmCommandHandler | ✅ 已实现+测试 | 校验登录态，委托 SendAlarmService |
| Service | SendAlarmService | ✅ 已实现+测试 | 解析 Alarm 项，区分产生/恢复，持久化 |
| Entity | AlarmRecordEntity | ✅ 已实现 | 告警记录（含 spid/serialNo/deviceId） |
| Repository | AlarmRecordRepository | ✅ 已实现 | 告警持久化 |
| Test | SendAlarmCommandHandlerTest | ✅ 已实现 | 单元测试覆盖 |
| Test | SendAlarmServiceTest | ✅ 已实现 | 22 项测试覆盖各种场景 |

## 9. 真实 FSU 实测差异

- LANDING-006 确认：真实 FSU (Emerson) 使用 2016 码表，SEND_ALARM Code=501（非 2024 的 601）。
- FSU 上报的 Alarm 结构包含 SerialNo、DeviceID、SPID 字段，SendAlarmService 已支持。
- 告警恢复（AlarmType=1）查找已有 ACTIVE 告警基于 fsuId + pointCode + alarmCode + status 匹配。
- LANDING-008 补充：spid 字段已写入 alarm_record；serialNo/deviceId 写入路径已实现。

## 10. 验收标准

- [ ] Fixture: `fixtures/b_interface/soap/sc_service/send_alarm.request.xml` 和 `send_alarm.response.xml` 已存在
- [ ] Handler 测试：SendAlarmCommandHandlerTest 通过
- [ ] Service 测试：SendAlarmServiceTest 通过（22 项测试）
- [ ] 正常告警：SignalID + AlarmCode + AlarmLevel 三项必填校验
- [ ] 告警产生（AlarmType=0）写入 alarm_record（status=ACTIVE）
- [ ] 告警恢复（AlarmType=1）更新已有告警（status=RECOVERED）
- [ ] AlarmID 响应返回最后一条告警的 ID

## 11. 安全边界

- 仅已登录的 FSU（isLoggedIn 检查）可以上报告警
- 不校验 Alarm 字段的合法性（长度、格式）
- 告警时间由 FSU 提供，SC 信任但不依赖（解析失败使用当前时间）
- 无告警频率限制（可能出现告警风暴）
- 不阻止重复告警（基于 alarmCode + signalId 纬度去重需业务层实现）
