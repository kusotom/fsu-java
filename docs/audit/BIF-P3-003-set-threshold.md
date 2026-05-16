# BIF-P3-003：SET_THRESHOLD 门限设置闭环

> 对应阶段：BIF-P3-003
> 完成日期：2026-05-14
> 影响范围：新增 5 个生产文件、修改 3 个生产文件、新增 5+1 个测试文件

---

## 一、本阶段目标

实现 SET_THRESHOLD 高风险配置下发命令的安全闭环。核心要求：默认禁止真实下发、必须建立二次确认、安全门禁、操作审计和测试闭环。

## 二、SET_THRESHOLD 协议依据

### 2.1 请求结构（SC→FSU）

- PK_Type = `SET_THRESHOLD`
- Info: `FSUCode`
- xmlData: `Signal` 节点，包含：
  - `SignalID` — 信号标识
  - `AlarmUpper` — 告警上限（可选）
  - `AlarmLower` — 告警下限（可选）
  - `AlarmUpperUrgent` — 严重告警上限（可选）
  - `AlarmLowerUrgent` — 严重告警下限（可选）

### 2.2 响应结构（FSU→SC）

- PK_Type = `SET_THRESHOLD`
- Info: `ResultCode`, `Count`
- xmlData: 空节点

---

## 三、安全架构（4 层安全控制）

```
SC 平台内部层
  ┌─────────────────────────────────────────────┐
  │ CommandDispatcher                           │  ← 已有
  │   → SetThresholdCommandHandler (真实实现)    │  ← 本阶段重写
  │     → SetThresholdSafetyGate (第一道门禁)     │  ← 本阶段新建
  │     → SetThresholdAuditService (操作审计)     │  ← 本阶段新建
  │     → SetThresholdService (业务逻辑)         │  ← 本阶段新建
  │       → FsuServiceClient (接口)              │  ← BIF-P3-001
  │         → StubFsuServiceClient (默认)        │  ← 本阶段扩展
  │         → RealHttpFsuServiceClient (可选)    │  ← BIF-P3-001
  └─────────────────────────────────────────────┘
```

### 3.1 安全门禁（SetThresholdSafetyGate）

第一道防线。由两个配置开关控制：

```yaml
b-interface:
  set-threshold:
    enabled: false            # 默认关闭，必须显式开启
    require-confirmation: true # 默认要求二次确认
```

决策逻辑：

| 条件 | ResultCode | 说明 |
|------|-----------|------|
| `enabled=false` | 3001 | SET_THRESHOLD 功能未启用 |
| `requireConfirmation=true && !confirmed` | 3002 | 缺少二次确认 |
| 全部通过 | 0 | 允许 |

### 3.2 操作审计（SetThresholdAuditService）

第二道防线。每次 SET_THRESHOLD 操作记录完整审计上下文：

- operationId — 全局唯一操作 ID（UUID）
- commandType — SET_THRESHOLD
- fsuCode / signalId — 操作目标
- alarmUpper / alarmLower / alarmUpperUrgent / alarmLowerUrgent — 门限参数
- confirmed — 是否二次确认
- operator — 操作人（预留）
- requestTime / completionTime — 起止时间
- resultCode / resultDesc — 结果
- success / errors — 成功/错误详情

当前实现为内存 + SLF4J 日志，重启后丢失。通过 `SetThresholdAuditRecord.Builder` 构建记录。

### 3.3 业务服务（SetThresholdService）

第三层。核心职责：

| 职责 | 说明 |
|------|------|
| 参数校验 | FSUCode null/空 → 2001, SignalID null/空 → 2003 |
| 请求构造 | 生成 `<Signal><SignalID>...</SignalID>...</Signal>` XMLData |
| FSU 调用 | 构造 FsuServiceRequest(pkType=SET_THRESHOLD) + 调用 Client |
| 响应解析 | 从 Info XML 解析 `<Count>` 字段 |
| 异常处理 | 所有异常 → SetThresholdResult.fail("5001") |

### 3.4 命令编排（SetThresholdCommandHandler）

第四层。完整编排流程：

```
handle(context)
  ├─ null context? → 5001
  ├─ 提取/校验 FSUCode? → 2001
  ├─ loginService.isLoggedIn()? → 1002
  ├─ 提取/校验 SignalID? → 2003
  ├─ safetyGate.check(fsuCode, confirmed)?
  │     ├─ denied(3001/3002) → 记录审计 → 返回
  │     └─ allowed → 继续
  ├─ auditService.begin() + service.execute() + auditService.record()
  └─ 构建 CommandResult
```

---

## 四、SetThresholdResult 模型

| 字段 | 类型 | 说明 |
|------|------|------|
| success | boolean | 是否成功 |
| resultCode | String | 结果码 |
| resultDesc | String | 结果描述 |
| fsuCode | String | FSU 编码 |
| count | int | 设置影响信号数 |
| errors | List\<String\> | 错误列表 |

工厂方法：`success(fsuCode, count)` / `fail(resultCode, desc)` / `fail(resultCode, desc, fsuCode)`

## 五、ResultCode 策略

| ResultCode | 含义 | 触发条件 |
|------------|------|---------|
| 0 | 成功 | 设置成功 |
| 1002 | FSU 未登录/离线 | loginService.isLoggedIn() = false |
| 2001 | 缺少 FSUCode | FSUCode 为空、Info 为空 |
| 2003 | 缺少 SignalID | SignalID 为空 |
| 3001 | 安全门禁禁止 | set-threshold.enabled=false |
| 3002 | 缺少二次确认 | require-confirmation=true 且未确认 |
| 5001 | 内部错误 | FSU 调用异常、Handler 内部异常 |

---

## 六、默认 stub / 禁止真实设备调用

- `set-threshold.enabled: false` — 配置默认关闭
- StubFsuServiceClient 仍为默认 Bean（matchIfMissing=true）
- RealHttpFsuServiceClient 需要 `real-call-enabled=true` 才激活
- 所有测试使用 StubFsuServiceClient 或独立的 Stub 内部类
- 所有测试验证不访问真实网络

---

## 七、测试覆盖情况

### 7.1 新增测试

| 测试文件 | 测试数 | 类型 |
|---------|--------|------|
| SetThresholdServiceTest | 16 | 单元测试（成功/校验/异常/请求构造/parseCount） |
| SetThresholdSafetyGateTest | 6 | 单元测试（enabled/confirmed 组合覆盖） |
| SetThresholdAuditServiceTest | 7 | 单元测试（Builder/记录/多次/清空/边界） |
| SetThresholdCommandHandlerTest | 15 | 单元测试（成功/校验/门禁/Service错误/extractFsuCode） |
| SetThresholdSlowChannelIntegrationTest | 10 | 集成测试（全链路 + 门禁 + 审计 + 登录态 + 安全） |
| StubFsuServiceClientTest (扩展) | +4 | SET_THRESHOLD 成功/Count/不访问网络/原始 SOAP |

**新增测试合计：54+4 = 58**

### 7.2 测试场景覆盖

- 成功设置门限（安全门禁通过 + 已登录 + 已确认）
- 操作审计记录（成功/失败均记录）
- 安全门禁禁用（enabled=false → 3001）
- 缺少二次确认（confirmed=false → 3002）
- 未登录（isLoggedIn=false → 1002）
- FSU 离线/会话过期（→ 1002）
- 缺少 FSUCode（null/空 → 2001）
- 缺少 SignalID（null/空 → 2003）
- 安全门禁拒绝 + 仍记录审计
- null context → 5001
- FSU 客户端错误透传
- FSU 客户端异常 → 5001
- 请求字段验证（SignalID / AlarmUpper / AlarmLower / AlarmUpperUrgent / AlarmLowerUrgent）
- Info 字段验证（FSUCode）
- parseCount 解析（正常/null/空/未找到）
- 不访问真实网络
- 不涉及 SET_POINT

### 7.3 回归测试

| 测试范围 | 测试数 | 结果 |
|---------|--------|------|
| 全量 BInterface 测试 | 514 | 全部通过 |

---

## 八、FsuServiceClient 扩展

StubFsuServiceClient 新增：
- `DEFAULT_SET_THRESHOLD_RESPONSE` 常量 — SOAP 响应（ResultCode=0, Count=1, 空 xmlData）
- `case SET_THRESHOLD` 分支
- `handleSetThreshold()` 方法 — 解析 SignalID 并构建响应
- `extractResultCode()` 工具方法

---

## 九、本阶段未处理内容

- 真实设备联调（始终禁止，参见 3.1 enabled=false）
- 审计数据持久化（当前为内存，重启丢失）
- FSU 地址发现（serviceUrl 传 null）
- SET_POINT / SET_FTP / SET_FSUREBOOT（不在本阶段范围）
- 批量门限设置（当前一次只设置一个信号）
- 前端页面/API 接口

## 十、遗留风险

| 风险 | 等级 | 说明 |
|------|------|------|
| 审计数据内存存储 | 中 | 重启丢失，后续需改为数据库持久化 |
| 门限字段均为 String 类型 | 低 | XML 解析天然为 String |
| Stub 响应固定 | 低 | Stub 仅用于开发和测试 |
| FSU 地址管理未实现 | 低 | serviceUrl 传 null，由后续阶段补充 |
| 无操作回滚机制 | 中 | SET_THRESHOLD 成功设置后不可撤销 |

## 十一、下一步建议

**BIF-P3-004**：慢数据通道定时轮询 — 实现基于 ScheduledExecutorService 的 GET_DATA / GET_THRESHOLD 定时轮询触发。
