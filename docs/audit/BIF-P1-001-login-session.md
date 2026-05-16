# BIF-P1-001：LOGIN 注册 / Session 管理业务闭环

> 对应阶段：BIF-P1-001
> 完成日期：2026-05-14
> 影响范围：LoginService / LoginResult / LoginCommandHandler / LoginServiceTest / LoginCommandHandlerTest / 3 Repository Stub 重构

---

## 一、架构边界

### 1.1 LOGIN 在完整处理链路中的位置

```
HTTP SOAP Request (LOGIN)
  ↓
ScServiceController               ← HTTP/SOAP 收发
  ↓
SoapMessageHandler.parse()        ← SOAP Envelope 解析
  ↓
CommandDispatcher.dispatch()      ← 路由分发 + XmlDataParser 调用
  ↓ (BInterfacePkType.LOGIN)
LoginCommandHandler.handle()      ← FSUCode 提取 + 委托 LoginService
  ↓
LoginService.login()              ← 业务：校验 → 创建 Session → 更新状态 → 返回结果
  ↓
LoginResult                       ← 业务结果模型
  ↓
LoginCommandHandler.buildResponse() ← 构造 LOGIN 响应 Info
  ↓
CommandResult                     ← 统一响应模型
  ↓
SoapMessageHandler.buildResponse()← SOAP Envelope 构造
  ↓
HTTP SOAP Response (LOGIN 响应)
```

### 1.2 各层职责

| 层 | 职责 | 不允许 |
|----|------|--------|
| LoginCommandHandler | 从 Info 提取 FSUCode，调用 LoginService，构造响应 | 访问数据库、处理 SOAP Envelope |
| LoginService | 校验 FSU → 创建 Session → 更新状态 → 更新设备注册时间 | 解析 XML、构造 SOAP 响应 |
| LoginResult | 统一业务结果模型（success/fail 工厂方法） | 包含任何业务逻辑 |

---

## 二、LoginService 核心流程

### 2.1 `login(fsuCode, remoteAddr)` 流程图

```
开始
  ↓
fsuCode == null/空/空白 → 返回 fail("2001", "缺少 FSUCode")
  ↓
fsuCode = fsuCode.trim()
  ↓
fsuDeviceRepository.findByFsuCode(fsuCode) → 未找到 → 返回 fail("1002", "FSU 未注册")
  ↓
找到 FsuDeviceEntity
  ↓
生成 SessionId: "SESSION-" + fsuCode + "-" + UUID(12).toUpperCase()
  ↓
创建 BInterfaceSessionEntity:
  fsuId / fsuCode / sessionId / loginTime=now / status="ACTIVE" / expireSeconds=3600
  ↓
sessionRepository.save(session)
  ↓
更新 BInterfaceFsuStatusEntity:
  loginStatus="LOGIN" / onlineStatus="ONLINE" / lastLoginTime=now / sessionId / heartbeatMissCount=0
  ↓
更新 FsuDeviceEntity:
  registerTime（首次登录赋值）/ lastOnlineTime=now
  ↓
返回 success(fsuCode, sessionId, loginTime)
```

### 2.2 Session 管理方法

| 方法 | 用途 | 未来调用方 |
|------|------|-----------|
| `isLoggedIn(fsuCode)` | 检查 FSU 是否已登录（loginStatus == "LOGIN"） | HEARTBEAT / SEND_DATA / SEND_ALARM |
| `getActiveSession(fsuCode)` | 查询活动 Session（status == "ACTIVE"） | HEARTBEAT / SEND_DATA / SEND_ALARM |
| `updateLastSeen(fsuCode)` | 更新最后活跃时间和心跳时间 | HEARTBEAT |
| `clearSession(fsuCode)` | 登出：Session → "LOGOUT" / Status → "LOGOUT"+"OFFLINE" | 超时/断线处理 |

---

## 三、协议一致性

### 3.1 LOGIN 请求

```
<Request>
  <PK_Type>LOGIN</PK_Type>
  <Info>
    <FSUCode>xxx</FSUCode>
  </Info>
  <xmlData>
    <DeviceInfo>
      <DeviceID>xxx</DeviceID>
      <DeviceName>xxx</DeviceName>
    </DeviceInfo>
  </xmlData>
</Request>
```

本阶段从 Info.FSUCode 提取 FSU 标识，xmlData 中的 DeviceInfo 暂不处理（保留给后续扩展）。

### 3.2 LOGIN 响应

```
<Response>
  <PK_Type>LOGIN</PK_Type>
  <Info>
    <ResultCode>0</ResultCode>
    <SessionID>SESSION-FSU-001-XXXXXXXXXXXX</SessionID>
    <ExpireSeconds>3600</ExpireSeconds>
    <ServerTime>2026-05-14T22:00:00+08:00</ServerTime>
  </Info>
  <!-- xmlData 为空 -->
</Response>
```

### 3.3 ResultCode 策略

| ResultCode | 含义 | 触发条件 |
|-----------|------|---------|
| 0 | 登录成功 | 校验通过、Session 建立 |
| 1002 | FSU 未注册 | fsu_device 表中不存在该 FSUCode |
| 2001 | 缺少 FSUCode | FSUCode 为 null/空/空白 |
| 5001 | 服务器内部错误 | Session 创建或状态更新异常 |

---

## 四、测试覆盖

### 4.1 LoginServiceTest（27 测试）

| 类别 | 测试方法 | 验证点 |
|------|---------|--------|
| 登录成功 | shouldLoginSuccessfully | isSuccess=true, ResultCode=0, sessionId 非空 |
| 登录成功 | shouldCreateSessionOnLogin | Session 创建、status=ACTIVE |
| 登录成功 | shouldUpdateFsuStatusOnLogin | loginStatus=LOGIN, onlineStatus=ONLINE |
| 重复登录 | shouldUpdateSessionOnRepeatedLogin | 两次生成不同 sessionId |
| FSUCode 校验 | shouldFailOnNull/Empty/BlankFsuCode | 均返回 2001 |
| 未知 FSU | shouldFailOnUnknownFsu | 返回 1002 + "未注册" |
| Session 管理 | isLoggedInBefore/AfterLogin | 登录前后状态正确 |
| Session 管理 | updateLastSeenShouldUpdateTimestamp | heartbeat 更新 |
| Session 管理 | clearSessionShouldLogout | status=LOGOUT/OFFLINE |
| Session 管理 | clearSessionShouldUpdateSessionStatus | 无 ACTIVE session |
| Session 管理 | getActiveSessionBeforeLogin | 返回 empty |
| 边界情况 | updateLastSeen/clearSession/getActiveSession 对未知 FSU | 安静跳过/返回 empty |
| FSU 设备 | loginShouldUpdateFsuDeviceRegisterTime | registerTime/lastOnlineTime 更新 |

### 4.2 LoginCommandHandlerTest（15 测试）

| 类别 | 测试方法 | 验证点 |
|------|---------|--------|
| PK_Type | shouldReturnLoginPkType | getSupportedPkType() = LOGIN |
| 成功 | shouldHandleSuccessfulLogin | ResultCode=0, 响应包含 SessionID/ExpireSeconds/ServerTime |
| 成功 | shouldPassRemoteAddrToService | remoteAddr 被传递到 LoginService |
| 错误 | shouldReturnErrorOnNullContext | 5001 |
| 错误 | shouldReturnErrorOnNullSoapMessage | 2001 |
| 错误 | shouldReturnErrorOnMissingFsuCode | Info 中无 FSUCode → 2001 |
| 错误 | shouldReturnErrorOnEmptyFsuCode | FSUCode 为空标签 → 2001 |
| 错误 | shouldReturnServiceErrorCode | 服务返回 1002 |
| 错误 | shouldHandleServiceException | 服务异常 → 5001 |
| extractFsuCode | 6 个测试 | 正常/大小写/空白/null/空/空标签 |

### 4.3 全量测试

```
Tests run: 244, Failures: 0, Errors: 0, Skipped: 0
```

---

## 五、风险与遗留问题

### 5.1 引入的风险

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| LoginService 使用 @Transactional，桩测试不验证事务回滚 | 低 | 事务在单表操作下影响小，后续集成测试覆盖 |
| extractFsuCode 使用正则而非 DOM 解析 | 低 | LOGIN Info 结构简单，正则性能更优 |
| Stub Repository 需随 Spring Data 版本升级添加新抽象方法 | 中 | 已重构为 BaseStub 基类，新增方法只需改基类 |

### 5.2 遗留问题

| 问题 | 原因 | 计划 |
|------|------|------|
| xmlData.DeviceInfo 未解析（厂商/型号/固件版本） | 协议中对 DeviceInfo 的使用未明确定义 | 在 BIF-P1-003 SEND_DATA 或 BIF-P1-004 设备注册时按需解析 |
| LOGIN 成功响应未包含 xmlData | 协议中 LOGIN 响应 xmlData 为空 | 符合协议要求 |

### 5.3 下一步建议

1. **BIF-P1-002：HEARTBEAT** — 复用 LoginService.updateLastSeen()，检测 Session 有效期
2. **BIF-P1-003：SEND_DATA** — 需要 Session 有效性检查，可复用 LoginService.isLoggedIn()
3. **BIF-P1-004：SEND_ALARM** — 同 SEND_DATA
4. **Repository 桩重构** — 当前 BaseStub 基于「需要什么加什么」，若后续新增 Repository 测试，可考虑统一测试用内存数据库
