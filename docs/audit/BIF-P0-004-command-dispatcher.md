# BIF-P0-004：CommandDispatcher 命令分发层收尾

> 对应阶段：BIF-P0-004
> 完成日期：2026-05-14
> 影响范围：CommandDispatcher / CommandHandler / CommandContext / CommandResult / 13 个 Handler / ScServiceController / 39 新增测试

---

## 一、架构边界

### 1.1 完整处理链路

```
HTTP SOAP Request
  ↓
ScServiceController          ← 只负责 HTTP/SOAP 收发
  ↓
SoapMessageHandler.parse()   ← 只负责 SOAP Envelope 解析
  ↓
CommandDispatcher.dispatch() ← 只负责路由 + XmlDataParser 调用 + 异常兜底
  ↓
  ├─ XmlDataParser.parse()   ← 只负责 xmlData 结构化解析
  └─ CommandHandler.handle() ← 只负责某一类业务功能
  ↓
CommandResult                ← 统一响应模型
  ↓
SoapMessageHandler.buildResponse() ← 只负责 SOAP Envelope 构造
  ↓
HTTP SOAP Response
```

### 1.2 各层职责

| 层 | 职责 | 不允许 |
|----|------|--------|
| Controller | HTTP 收发、SOAP Fault 构造 | 写命令 if/else、写业务逻辑 |
| SoapMessageHandler | SOAP Envelope 解析/构造 | 写业务分发、写 XMLData 子字段解析 |
| XmlDataParser | xmlData 结构化解析 | 写业务处理、写命令判断 |
| CommandDispatcher | 路由 + 上下文构造 + 异常兜底 | 写具体业务逻辑、写数据库 |
| CommandHandler | 业务功能 | 重复 DOM 解析、访问 HTTP 层 |
| XmlDataBuilder | xmlData XML 构造 | 写业务逻辑 |

## 二、新增/修改文件

### 新增文件 (11)

| 文件 | 说明 |
|------|------|
| `command/CommandContext.java` | 命令上下文（pkType / soapMessage / xmlData / rawSoap / attributes） |
| `command/CommandResult.java` | 命令结果（success / resultCode / toInfoXml() / toXmlDataXml() / factory methods） |
| `command/GetThresholdCommandHandler.java` | GET_THRESHOLD 桩 Handler |
| `command/SetThresholdCommandHandler.java` | SET_THRESHOLD 桩 Handler |
| `command/SetPointCommandHandler.java` | SET_POINT 桩 Handler |
| `command/GetFtpCommandHandler.java` | GET_FTP 桩 Handler |
| `command/SetFtpCommandHandler.java` | SET_FTP 桩 Handler |
| `command/GetLoginInfoCommandHandler.java` | GET_LOGININFO 桩 Handler |
| `command/TimeCheckCommandHandler.java` | TIME_CHECK 桩 Handler |
| `CommandDispatcherTest.java` | 21 个分发测试 |
| `CommandDispatcherErrorTest.java` | 6 个错误处理测试 |
| `CommandContextTest.java` | 4 个上下文测试 |
| `CommandResultTest.java` | 8 个结果模型测试 |

### 修改文件 (8)

| 文件 | 变更 |
|------|------|
| `command/CommandHandler.java` | 接口改为 `CommandResult handle(CommandContext context)` |
| `command/CommandDispatcher.java` | 集成 XmlDataParser / CommandContext / CommandResult / 异常捕获 |
| `command/LoginCommandHandler.java` | 更新为新接口，返回 notImplemented |
| `command/HeartbeatCommandHandler.java` | 同上 |
| `command/SendDataCommandHandler.java` | 同上 |
| `command/SendAlarmCommandHandler.java` | 同上 |
| `command/GetDataCommandHandler.java` | 同上 |
| `command/UnknownCommandHandler.java` | 更新为新接口，返回 ResultCode=1002 |
| `xml/XmlDataParser.java` | 添加 @Component 以支持 Spring 注入 |
| `service/sc/ScServiceController.java` | 集成 CommandDispatcher，不再硬编码 ResultCode=0 |

## 三、命令分发覆盖清单

| PK_Type | Handler | 实现状态 | 注册方式 |
|---------|---------|----------|---------|
| LOGIN | LoginCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| HEARTBEAT | HeartbeatCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| SEND_DATA | SendDataCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| SEND_ALARM | SendAlarmCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| GET_DATA | GetDataCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| GET_THRESHOLD | GetThresholdCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| SET_THRESHOLD | SetThresholdCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| SET_POINT | SetPointCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| GET_FTP | GetFtpCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| SET_FTP | SetFtpCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| GET_LOGININFO | GetLoginInfoCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| TIME_CHECK | TimeCheckCommandHandler | 桩 (notImplemented) | @Component 自动注入 |
| GET_HISTORY_DATA | — | 未注册 → UNKNOWN 兜底 | 无 |
| GET_FSUINFO | — | 未注册 → UNKNOWN 兜底 | 无 |
| SET_DATA | — | 未注册 → UNKNOWN 兜底 | 无 |
| SET_FSUREBOOT | — | 未注册 → UNKNOWN 兜底 | 无 |
| UNKNOWN | UnknownCommandHandler | 实现 (ResultCode=1002) | 显式注入 |

## 四、错误处理策略

| 场景 | 处理方式 | ResultCode |
|------|----------|------------|
| xmlData 解析失败 | CommandResult.error | 2001 |
| Handler 抛异常 | Dispatcher 捕获 → CommandResult.error | 5001 |
| Handler 返回 null | Dispatcher 检测 → CommandResult.error | 5001 |
| PK_Type 未注册 | UnknownCommandHandler.handle | 1002 |
| null request | UnknownCommandHandler 兜底 | 1002 |
| null PK_Type | UnknownCommandHandler 兜底 | 1002 |

所有错误场景均不抛异常到 Controller，不返回裸 null。

## 五、测试覆盖

### 5.1 新增测试统计

| 测试类 | 测试数 | 覆盖内容 |
|--------|--------|----------|
| CommandDispatcherTest | 21 | 12 命令分发 + 区分测试 + UNKNOWN + null + hasHandler |
| CommandDispatcherErrorTest | 6 | 畸形 XML + Handler 异常 + null Handler + 未注册 + UNKNOWN |
| CommandContextTest | 4 | 创建 / attributes / null / toString |
| CommandResultTest | 8 | 三种工厂方法 + toInfoXml + toXmlDataXml + errors + toString |

### 5.2 全量测试结果

```
Tests run: 212, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

包含：前序 P0-001~P0-003 的 173 测试 + P0-004 新增 39 测试。

## 六、未处理内容

- Handler 真实业务逻辑（待 BIF-P1-001~005）
- GET_HISTORY_DATA / GET_FSUINFO / SET_DATA / SET_FSUREBOOT Handler 注册（协议命令，但无 fixture，待补充）
- SET_FSUREBOOT 安全开关（safe_enabled=FALSE）
- 数据库持久化

## 七、遗留风险

- 所有 13 个 Handler 当前均为占位返回（notImplemented），生产环境中会返回 ResultCode=1
- ScServiceController 的 CommandDispatcher 集成未在生产环境全链路验证

## 八、下一步建议

1. **BIF-P1-001** — LOGIN 业务逻辑（FSU 注册/Session 管理）
2. **BIF-P1-002** — HEARTBEAT 业务逻辑（在线状态更新）
3. **BIF-P1-003** — SEND_DATA 业务逻辑（实时数据入库）
4. **BIF-P1-004** — SEND_ALARM 业务逻辑（告警入库）
5. 补充 GET_HISTORY_DATA / GET_FSUINFO / SET_DATA / SET_FSUREBOOT 的 fixture 和 Handler
