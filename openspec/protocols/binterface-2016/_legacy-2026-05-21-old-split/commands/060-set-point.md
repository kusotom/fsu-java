# SET_POINT -- 遥控遥调

## 1. 协议定位

- 命令名称：SET_POINT
- ACK 名称：SET_POINT_ACK
- 方向：SC --> FSU
- 发起方：SC
- 接收方：FSU
- 功能说明：SC 向 FSU 下发遥控/遥调命令，设置指定信号量的目标值。属于高危操作，必须经过安全门禁和二次确认。

## 2. 命令码

| 项目 | Code |
|------|------|
| 请求 | 1001 |
| 响应 | 1002 |

## 3. 请求 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| FSUCode | String | 是 | FSU 设备编码 | `info.fsuCode` | ⚠️ 桩实现，未提取 |
| SessionID | String | 是 | 会话 ID | `info.sessionId` | ⚠️ 桩实现，未校验 |
| RequestTime | DateTime | 是 | 请求时间（ISO8601） | `info.requestTime` | ⚠️ 桩实现，未使用 |

## 4. 响应 Info 字段

| 字段 | 类型/长度 | 必填 | 说明 | 平台字段建议 | 当前项目实现 |
|---|---|---|---|---|---|
| ResultCode | Integer | 是 | 0=成功，非0=失败 | `resultCode` | ⚠️ 桩实现，返回 notImplemented |
| SignalID | String | 是 | 被控制的信号量 ID | `signalId` | ⚠️ 桩实现，未返回 |

## 5. 协议 XML 样例

### 请求样例

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>SET_POINT</PK_Type>
      <Info>
        <FSUCode>FSU-001</FSUCode>
        <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
        <RequestTime>2026-05-13T10:38:00+08:00</RequestTime>
      </Info>
      <xmlData>
        <Signal>
          <SignalID>VOLT-001</SignalID>
          <SetValue>230.0</SetValue>
          <Operator>admin</Operator>
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
    <Response>
      <PK_Type>SET_POINT</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <SignalID>VOLT-001</SignalID>
      </Info>
      <xmlData/>
    </Response>
  </soap:Body>
</soap:Envelope>
```

## 6. 数据结构引用

- Signal 结构（请求 xmlData）：SignalID, SetValue, Operator
- ResultCode：参见 `03-constants.md` ResultCode 定义
- 安全门禁：参见 `13-threshold-configuration.md` 安全门禁设计（可复用 SET_THRESHOLD 的安全模式）

## 7. 成功/失败判断

| 条件 | ResultCode | 说明 |
|------|-----------|------|
| 遥控成功，FSU 执行设定值 | 0 | 成功 |
| 缺少必填参数 | 2001 | FSUCode 或 SignalID 缺失 |
| FSU 未登录或 Session 过期 | 1001 | 会话无效 |
| FSU 未注册 | 1002 | FSU 未注册 |
| SignalID 不存在或不可控 | 2003 | 参数错误 |
| 操作被安全门禁拒绝 | 403 / 自定义 | 安全策略拦截 |
| FSU 服务端异常 | 5001 | 执行异常 |

## 8. 项目实现映射

| 层级 | 文件/类 | 状态 | 风险 |
|------|---------|------|------|
| Handler | SetPointCommandHandler | ⚠️ 桩实现 | 仅返回 CommandResult.notImplemented() |
| Safety | ConfirmationToken / ConfirmationTokenService | ✅ 已实现 | 二次确认令牌机制 |
| Safety | SetCommandSafetyGate / SetCommandSafetyProperties | ✅ 已实现 | 安全门禁（控制 SET 命令开关） |
| Safety | SetCommandAuditService / SetCommandAuditRecord | ✅ 已实现 | SET 操作审计记录 |
| Safety | SetCommandRiskLevel | ✅ 已实现 | 风险等级定义 |
| Safety | SetCommandSafetyDecision | ✅ 已实现 | 安全决策模型 |
| Service | --（需实现 SetPointService） | ❌ 未实现 | 需新建 |
| Client | FsuServiceClient / RealHttpFsuServiceClient | ✅ 接口已定义 | 可复用现有出站调用框架 |
| Model | BInterfacePkType.SET_POINT | ✅ 枚举已定义 | 命令枚举 |
| Test | -- | ❌ 未实现 | 需在实现后补充 |

> **注意**：项目已具备完整的 SET 命令安全体系（ConfirmationToken、SafetyGate、AuditService），但 SetPointCommandHandler 当前仅返回 notImplemented。后续实现时可直接集成现有安全框架。

## 9. 真实 FSU 实测差异

- **未对真实 FSU 执行 SET_POINT 测试**。LANDING-003/005/006 明确禁止所有 SET_ 命令以保证安全。
- 真实 FSU (Emerson) 的 SET_POINT (1001/1002) 支持情况待确认。
- FSU 端可能对 SignalID 的可控性有独立校验（非所有信号量都支持遥控）。
- Operator 字段在 FSU 端可能用于操作日志记录。

## 10. 验收标准

- [ ] Fixture: `fixtures/b_interface/soap/fsu_service/set_point.request.xml` 和 `set_point.response.xml` 已存在
- [ ] 实现 SetPointCommandHandler 真实业务逻辑
- [ ] 实现 SetPointService 委托 FsuServiceClient 出站调用
- [ ] 集成 ConfirmationToken 二次确认机制
- [ ] 集成 SetCommandSafetyGate 安全门禁（默认 disabled=false，禁止执行）
- [ ] 集成 SetCommandAuditService 记录 SET 操作审计日志
- [ ] Signal 请求中 SignalID + SetValue 校验
- [ ] 响应返回被控制的 SignalID
- [ ] 测试覆盖：成功路径、安全门禁拒绝、Token 无效、FSU 通信失败

## 11. 安全边界

- **禁止默认执行**：safe_enabled 必须为 false，需显式确认后才可开启
- **需 confirmationToken + 安全门禁**：二次确认令牌机制，防止误操作
- **Operator 字段必须记录**：操作人信息写入审计日志，追溯可查
- **高危 SignalID 白名单**：仅允许操作已标记为 settable 的信号量
- **频率限制**：SET 命令频率应低于只读查询，防止"误操作风暴"
- **执行前快照记录**：SET 前应记录当前值，便于回滚参考
- **FSU 端可能不支持实时响应**：SET 操作可能为非阻塞（命令已接收但未执行完成）
- SET_POINT 不应与 SET_THRESHOLD、SET_FTP 等命令混用同一处理路径
