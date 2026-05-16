# BIF-P1-002：HEARTBEAT 心跳 / 在线状态闭环

> 对应阶段：BIF-P1-002
> 完成日期：2026-05-14
> 影响范围：HeartbeatCommandHandler（桩→真实实现）/ HeartbeatCommandHandlerTest（14 测试）

---

## 一、架构边界

### 1.1 HEARTBEAT 在完整处理链路中的位置

```
HTTP SOAP Request (HEARTBEAT)
  ↓
ScServiceController               ← HTTP/SOAP 收发
  ↓
SoapMessageHandler.parse()        ← SOAP Envelope 解析
  ↓
CommandDispatcher.dispatch()      ← 路由分发 + XmlDataParser 调用
  ↓ (BInterfacePkType.HEARTBEAT)
HeartbeatCommandHandler.handle()  ← 提取 FSUCode + LoginService 校验/更新
  ↓
  ├─ LoginService.isLoggedIn()    ← 检查 FSU 登录态
  └─ LoginService.updateLastSeen()← 更新最后心跳时间
  ↓
CommandResult                     ← 统一响应模型
  ↓
SoapMessageHandler.buildResponse()← SOAP Envelope 构造
  ↓
HTTP SOAP Response (HEARTBEAT 响应)
```

### 1.2 各层职责

| 层 | 职责 | 不允许 |
|----|------|--------|
| HeartbeatCommandHandler | 从 Info 提取 FSUCode，校验登录，调用 updateLastSeen，构造响应 | 访问数据库、操作 Session 表、处理 SOAP Envelope |
| LoginService | 提供 isLoggedIn / updateLastSeen / getActiveSession（BIF-P1-001 已实现） | 解析 XML、构造 SOAP 响应 |

### 1.3 与 LOGIN 的关系

```
LOGIN → 创建 Session (ACTIVE)
        ↓
HEARTBEAT → 校验 Session (isLoggedIn) → 更新 lastSeenTime/onlineStatus
        ↓
        ... 后续 SEND_DATA/SEND_ALARM 复用同一 Session 校验 ...
        ↓
clearSession → Session → LOGOUT
```

HEARTBEAT **不创建** Session、**不注册** FSU、**不修改** loginStatus。它只做保活更新。

---

## 二、HEARTBEAT 协议

### 2.1 请求

```xml
<Request>
  <PK_Type>HEARTBEAT</PK_Type>
  <Info>
    <FSUCode>FSU-001</FSUCode>
    <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>
    <Timestamp>2026-05-13T10:31:00+08:00</Timestamp>
  </Info>
  <xmlData>
    <CPU>35</CPU>
    <Memory>62</Memory>
    <Temperature>42</Temperature>
    <RunningTime>3600</RunningTime>
  </xmlData>
</Request>
```

Info 字段：FSUCode（必需）、SessionID（可选）、Timestamp（可选）
xmlData：CPU/Memory/Temperature/RunningTime（设备状态信息，HEARTBEAT 不作处理）

### 2.2 响应

```xml
<Response>
  <PK_Type>HEARTBEAT</PK_Type>
  <Info>
    <ResultCode>0</ResultCode>
    <ServerTime>2026-05-13T10:31:05+08:00</ServerTime>
  </Info>
  <xmlData/>
</Response>
```

Info 字段：ResultCode、ServerTime
xmlData：空（符合协议 fixture）

### 2.3 ResultCode 策略

| ResultCode | 含义 | 触发条件 |
|-----------|------|---------|
| 0 | 心跳成功 | FSU 已登录，updateLastSeen 成功 |
| 1002 | FSU 未登录 | isLoggedIn 返回 false（FSU 未 LOGIN 或 Session 已过期） |
| 2001 | 缺少 FSUCode | Info 中无 FSUCode 或 Info 为空 |
| 5001 | 服务器内部错误 | updateLastSeen 抛异常或其他未预期异常 |

> **注意：** ResultCode 1002 在 HEARTBEAT 中语义为「FSU 未登录」，暂复用 LOGIN 的 ResultCode 策略。
> 待 B接口协议 2016 原文最终校验后确认是否需要独立错误码。

---

## 三、Session 保活策略

### 3.1 HEARTBEAT 更新行为

| 更新目标 | 字段 | 行为 |
|---------|------|------|
| BInterfaceFsuStatusEntity | lastHeartbeat | 设为当前时间 |
| BInterfaceFsuStatusEntity | onlineStatus | 设为 "ONLINE" |
| BInterfaceFsuStatusEntity | updatedAt | 设为当前时间 |
| FsuDeviceEntity | lastOnlineTime | 设为当前时间 |
| FsuDeviceEntity | updatedAt | 设为当前时间 |

以上行为在 LoginService.updateLastSeen() 中已实现，HEARTBEAT 直接调用。

### 3.2 不更新

| 不更新 | 原因 |
|-------|------|
| Session 的 lastActiveTime | 当前 BInterfaceSessionEntity 有此字段，HEARTBEAT 暂不更新（后续可追加） |
| loginStatus | 保持 "LOGIN" 不变 |
| 注册时间 | 仅在首次 LOGIN 时设置 |

---

## 四、测试覆盖

### HeartbeatCommandHandlerTest（14 测试）

| 类别 | 测试方法 | 验证点 |
|------|---------|--------|
| PK_Type | shouldReturnHeartbeatPkType | getSupportedPkType() = HEARTBEAT |
| 成功 | shouldHandleSuccessfulHeartbeat | ResultCode=0, implemented=true |
| 成功 | shouldUpdateLastSeenOnHeartbeat | updateLastSeen 被调用 |
| 成功 | shouldReturnServerTimeInResponse | 响应包含 ServerTime |
| 错误 | shouldFailOnNullContext | 5001 |
| 错误 | shouldFailOnNullSoapMessage | 2001 |
| 错误 | shouldFailOnMissingFsuCode | Info 中无 FSUCode → 2001 |
| 错误 | shouldFailOnNotLoggedIn | 1002 + updateLastSeen 未被调用 |
| 错误 | shouldHandleServiceException | 服务异常 → 5001 |
| extractSessionId | 5 个测试 | 正常/大小写/空白/null/空标签 |

### 全量测试

```
Tests run: 258, Failures: 0, Errors: 0, Skipped: 0
```

较 BIF-P1-001 新增 14 测试（HeartbeatCommandHandlerTest），LoginService、LoginCommandHandler 等存量测试全部通过。

---

## 五、风险与遗留问题

### 5.1 风险

| 风险 | 等级 | 说明 |
|------|------|------|
| isLoggedIn 仅检查 loginStatus 字段，不检查 Session 过期时间 | 低 | Session 过期逻辑待后续阶段实现 |
| xmlData（CPU/Memory 等设备信息）未解析入库 | 低 | 当前 HEARTBEAT 只做保活，数据采集在 SEND_DATA 阶段 |
| 无离线检测定时任务 | 低 | 需后续 BIF-P2 阶段实现 |

### 5.2 本阶段未处理

- 离线定时扫描（lastHeartbeat 超过阈值 → 置为 OFFLINE）
- Session 有效期校验（ExpireSeconds 超期判断）
- HEARTBEAT xmlData 设备状态入库
- 前端在线状态页面

### 5.3 下一步建议

1. **BIF-P1-003：SEND_DATA** — 复用 isLoggedIn / getActiveSession 做上报前校验
2. **BIF-P1-004：SEND_ALARM** — 复用同一登录态校验入口
3. **BIF-P2-001：离线检测** — 基于 lastHeartbeat 的定时扫描任务
