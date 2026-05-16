# BIF-P3-002：GET_THRESHOLD 门限查询闭环

> 对应阶段：BIF-P3-002
> 完成日期：2026-05-14
> 影响范围：新增 2 个生产文件、修改 2 个生产文件、新增 3+1 个测试文件

---

## 一、本阶段目标

基于 BIF-P3-001 已完成的 FsuServiceClient 慢数据通道基础设施，实现 GET_THRESHOLD 查询类命令闭环。

## 二、GET_THRESHOLD 协议依据

### 2.1 请求结构（SC→FSU）

- PK_Type = `GET_THRESHOLD`
- Info: `FSUCode`, `SessionID`, `RequestTime`
- xmlData: `SignalID` 重复叶子节点列表

### 2.2 响应结构（FSU→SC）

- PK_Type = `GET_THRESHOLD`
- Info: `ResultCode`, `Count`
- xmlData: `Signal` 列表，每个 Signal 包含：
  - `SignalID` — 信号标识
  - `AlarmUpper` — 告警上限
  - `AlarmLower` — 告警下限
  - `AlarmUpperUrgent` — 严重告警上限
  - `AlarmLowerUrgent` — 严重告警下限

---

## 三、慢数据通道架构

```
SC 平台内部层
  ┌───────────────────────────────────────────┐
  │ CommandDispatcher                         │  ← 已有
  │   → GetThresholdCommandHandler (真实实现)  │  ← 本阶段
  │     → GetThresholdService                 │  ← 本阶段
  │       → FsuServiceClient (接口)            │  ← BIF-P3-001
  │         → StubFsuServiceClient (默认)      │  ← 本阶段扩展
  │         → RealHttpFsuServiceClient (可选)  │  ← BIF-P3-001
  └───────────────────────────────────────────┘
```

---

## 四、GetThresholdCommandHandler 职责

| 职责 | 说明 |
|------|------|
| 参数校验 | null context → 5001, 缺少 Info → 2001 |
| FSUCode 提取 | 从 Info XML 使用正则 `<FSUCode>` 提取 |
| 登录态校验 | `loginService.isLoggedIn(fsuCode)` |
| SignalID 提取 | 从 xmlData 提取 SignalID 列表 |
| 调用 Service | `getThresholdService.execute(fsuCode, null, signalIds)` |
| 构造响应 | 根据 GetThresholdResult 构建 CommandResult |

不直接访问 HTTP，不直接调用 FsuServiceClient，不做持久化，不修改门限。

---

## 五、GetThresholdService 职责

| 职责 | 说明 |
|------|------|
| 参数校验 | FSUCode null/空 → 2001, SignalID null/空 → 2003 |
| 请求构造 | 生成 `<SignalID>...</SignalID>` XMLData + `<FSUCode>` Info |
| FSU 调用 | 构造 FsuServiceRequest(pkType=GET_THRESHOLD) 并调用 FsuServiceClient |
| 响应解析 | 从 XmlDataModel 提取 Signal 列表 → ThresholdValue |
| 异常处理 | 所有异常 → GetThresholdResult.fail("5001") |

---

## 六、FsuServiceClient 复用说明

- FsuServiceClient 接口不变，单 `call(FsuServiceRequest)` 支持所有 PK_Type
- StubFsuServiceClient 新增 `GET_THRESHOLD` case：
  - 内置完整 SOAP 响应（3 个信号：TEMP-001 / HUMI-001 / VOLT-001）
  - 每个信号包含 AlarmUpper / AlarmLower / AlarmUpperUrgent / AlarmLowerUrgent
  - 通过 SoapMessageHandler + XmlDataParser 模拟真实调用链路
- RealHttpFsuServiceClient 无需修改：通用 `call()` 已支持任意 PK_Type

## 七、Stub/Mock 与真实调用边界

| 特性 | StubFsuServiceClient (默认) | RealHttpFsuServiceClient (配置启用) |
|------|----------------------------|------------------------------------|
| 默认激活 | 是 (matchIfMissing=true) | 否 (havingValue="true") |
| 网络访问 | 否 | 是 |
| GET_THRESHOLD | 内置 3 信号响应 | 通用 SOAP/HTTP |
| 配置 | 无需配置 | b-interface.fsu-client.real-call-enabled=true |

所有测试使用 StubFsuServiceClient，不访问真实设备。

## 八、登录态/在线态校验策略

- `GetThresholdCommandHandler.handle()` → `loginService.isLoggedIn(fsuCode)` → `false` → 返回 `1002`
- 集成测试中验证了登录后查询成功、未登录/离线后拒绝

## 九、ResultCode 策略

| ResultCode | 含义 | 触发条件 |
|------------|------|---------|
| 0 | 成功 | 查询成功 |
| 1002 | FSU 未登录/离线 | loginService.isLoggedIn() = false |
| 2001 | 参数错误 | FSUCode 为空、Info 为空 |
| 2003 | 无有效信号 | SignalID 列表为空 |
| 5001 | 内部错误 | FSU 调用异常、客户端异常 |

---

## 十、测试覆盖情况

### 10.1 新增测试

| 测试文件 | 测试数 | 类型 |
|---------|--------|------|
| GetThresholdServiceTest | 15 | 单元测试（成功/校验/FSU失败/请求构造/响应解析/边界） |
| GetThresholdCommandHandlerTest | 14 | 单元测试（成功/pkType/校验/Service错误/extractFsuCode） |
| GetThresholdSlowChannelIntegrationTest | 8 | 集成测试（全链路 + 登录态 + 参数校验 + 安全） |
| StubFsuServiceClientTest (扩展) | +4 | GET_THRESHOLD 响应/字段/不访问网络 |

**新增测试合计：41**

### 10.2 测试场景覆盖

- 成功返回门限值列表（字段验证）
- 无门限数据时返回空列表
- FSUCode 校验（null/空 → 2001）
- SignalID 校验（null/空 → 2003）
- FSU 客户端错误透传
- FSU 客户端异常 → 5001
- FSU 禁用状态 → 5001
- 请求构造验证（SignalID/Info XML）
- 多信号解析
- 不修改门限（只读验证）
- 不访问网络
- null context → 5001
- 未登录 → 1002
- extractFsuCode 工具方法
- StubFsuServiceClient GET_THRESHOLD 响应

### 10.3 回归测试

| 测试范围 | 测试数 | 结果 |
|---------|--------|------|
| 全量 BInterface 测试 | 460 | 全部通过 |

---

## 十一、本阶段未处理内容

- SET_THRESHOLD（拆到 BIF-P3-003）
- 真实设备联调
- 前端页面
- 定时轮询
- 批量门限同步
- 门限数据持久化

## 十二、禁止真实设备调用说明

- `real-call-enabled: false` 默认值未变
- StubFsuServiceClient 仍为默认 Bean
- 所有新增测试验证 `isRealCall()` = false

## 十三、遗留风险

| 风险 | 等级 | 说明 |
|------|------|------|
| 门限字段均为 String 类型 | 低 | XML 解析天然为 String，上层使用方按需转换 |
| Stub 响应中门限值固定 | 低 | Stub 仅用于开发和测试 |
| GET_THRESHOLD 和 SET_THRESHOLD 共用协议字段 | 低 | 本阶段严格分离，后续 SET_THRESHOLD 以本阶段协议分析为基础 |
| FSU 地址管理未实现 | 低 | serviceUrl 传 null，由后续阶段补充 |

## 十四、下一步建议

**BIF-P3-003**：SET_THRESHOLD 门限设置命令（高风险，需二次确认 + 审计 + 安全门禁）。
