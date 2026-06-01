# GET_THRESHOLD -- 获取告警门限

## 1. 协议定位

- 命令名称：GET_THRESHOLD
- ACK 名称：GET_THRESHOLD_ACK
- 方向：SC --> FSU
- 发起方：SC
- 接收方：FSU
- 功能说明：SC 主动向 FSU 查询指定信号量的告警门限参数（上限、下限、严重上限、严重下限）。属于慢数据通道只读查询命令。

## 2. 命令码

| 项目 | Code |
|------|------|
| 请求 | 1901 |
| 响应 | 1902 |

## 3. 请求 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| FSUCode | String | 是 | FSU 设备编码 | `info.fsuCode` | ✅ 正则从 infoXml 提取 |
| SessionID | String | 是 | 会话 ID | `info.sessionId` | 当前未在 Handler 中单独校验 |
| RequestTime | DateTime | 是 | 请求时间（ISO8601） | `info.requestTime` | 未使用 |

## 4. 响应 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| ResultCode | Integer | 是 | 0=成功，非0=失败 | `resultCode` | ✅ |
| Count | Integer | 否 | 返回的门限数量 | `count` | ✅ 来自 GetThresholdResult.getCount() |

## 5. 协议 XML 样例

### 请求样例

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>GET_THRESHOLD</PK_Type>
      <Info>
        <FSUCode>FSU-001</FSUCode>
        <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
        <RequestTime>2026-05-13T10:40:00+08:00</RequestTime>
      </Info>
      <xmlData>
        <SignalID>TEMP-001</SignalID>
        <SignalID>HUMI-001</SignalID>
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
      <PK_Type>GET_THRESHOLD</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <Count>2</Count>
      </Info>
      <xmlData>
        <Signal>
          <SignalID>TEMP-001</SignalID>
          <AlarmUpper>60.0</AlarmUpper>
          <AlarmLower>-5.0</AlarmLower>
          <AlarmUpperUrgent>70.0</AlarmUpperUrgent>
          <AlarmLowerUrgent>-10.0</AlarmLowerUrgent>
        </Signal>
        <Signal>
          <SignalID>HUMI-001</SignalID>
          <AlarmUpper>90.0</AlarmUpper>
          <AlarmLower>10.0</AlarmLower>
          <AlarmUpperUrgent>95.0</AlarmUpperUrgent>
          <AlarmLowerUrgent>5.0</AlarmLowerUrgent>
        </Signal>
      </xmlData>
    </Response>
  </soap:Body>
</soap:Envelope>
```

## 6. 数据结构引用

- Signal 结构（响应 xmlData）：SignalID, AlarmUpper, AlarmLower, AlarmUpperUrgent, AlarmLowerUrgent
- 请求 xmlData：SignalID 重复叶子节点列表（可选，空则查全部）
- ResultCode：参见 `03-constants.md` ResultCode 定义
- 门限结构定义：参见 `06-data-structures.md` TThreshold 结构

## 7. 成功/失败判断

| 条件 | ResultCode | 说明 |
|------|-----------|------|
| 查询成功，返回门限数据 | 0 | 成功（可能为空列表） |
| FSUCode 为空 | 2001 | 缺少 FSUCode |
| FSU 未登录 | 1002 | FSU 未登录或已离线 |
| 缺少 SignalID | 2003 | 请求 xmlData 中无 SignalID |
| FSU endpoint 解析失败 | 对应错误码 | 无法连接 FSU |
| FSU 服务端异常 | 5001 | 查询异常 |

## 8. 项目实现映射

| 层级 | 文件/类 | 状态 | 风险 |
|------|---------|------|------|
| Handler | GetThresholdCommandHandler | ✅ 已实现+测试 | 校验登录态、提取 SignalID、委托 GetThresholdService |
| Service | GetThresholdService | ✅ 已实现+测试 | 构造 SOAP 请求、调用 FSU、解析响应 Threshold |
| Client | FsuServiceClient / RealHttpFsuServiceClient | ✅ 接口已定义 | 可复用于真实调用 |
| Result | GetThresholdResult | ✅ 已实现 | ThresholdValue 内部类承载门限数据 |
| Model | BInterfacePkType.GET_THRESHOLD | ✅ 枚举已定义 | 命令枚举 |
| Compat | BInterfaceCommand2016 | ⚠️ 缺少 1901 条目 | NAME_TO_CODE_2016 中未包含 GET_THRESHOLD |
| Test | GetThresholdCommandHandlerTest | ✅ 已实现 | 单元测试覆盖 |
| Test | GetThresholdServiceTest | ✅ 已实现 | 单元测试覆盖 |
| Test | BifP4005RealFsuIntegrationTest | ✅ 已实现 | 真实 FSU 集成测试 |

> **已知缺陷**：BInterfaceCommand2016.NAME_TO_CODE_2016 中缺少 GET_THRESHOLD(1901/1902) 条目。当前仅 2024 码表中定义了 GET_THRESHOLD，但 2016 协议中确实存在该命令。需补充。

## 9. 真实 FSU 实测差异

- BIF-P4-005 验证：真实 FSU (Emerson, IP=192.168.100.100:8080, FSUID=51051243812345) 的 GET_THRESHOLD 返回空数据（Count=0）。
- LANDING-006 确认：2016 码表中未建立 GET_THRESHOLD 映射（BInterfaceCommand2016 缺少 1901）。
- 当前 GetThresholdService 使用 2024 格式的 PK_Type 名称调用，真实 FSU 可能因命令码不匹配而返回空响应。
- 根因分析同 GET_DATA：Emerson FSU 使用 DeviceList 格式而非简单 SignalID 列表格式，需确认 FSU 端门槛查询的正确 XML 结构。

## 10. 验收标准

- [ ] Fixture: `fixtures/b_interface/soap/fsu_service/get_threshold.request.xml` 和 `get_threshold.response.xml` 已存在
- [ ] Handler 测试：GetThresholdCommandHandlerTest 通过
- [ ] Service 测试：GetThresholdServiceTest 通过
- [ ] 请求中 SignalID 列表正确拼接到 xmlData
- [ ] 响应门限列表正确解析（SignalID + AlarmUpper/Lower/UpperUrgent/LowerUrgent）
- [ ] 支持空 SignalID 列表（查询全部）-- 当前实现要求至少一个 SignalID
- [ ] BInterfaceCommand2016 补充 1901/1902 条目
- [ ] 集成测试：通过 FsuServiceClient 向真实/Stub FSU 发送请求并验证

## 11. 安全边界

- 仅查询已登录 FSU 的门限数据
- GET_THRESHOLD 为只读操作，不修改 FSU 门限参数
- 门限数据可能暴露 FSU 的告警策略配置，属敏感信息
- 请求频率受 SlowDataPollingScheduler 控制（当前未接入 GET_THRESHOLD 轮询）
