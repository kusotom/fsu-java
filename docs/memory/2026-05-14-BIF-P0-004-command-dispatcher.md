---
name: BIF-P0-004-command-dispatcher
description: 完成 CommandDispatcher 命令分发层，新增 CommandContext/CommandResult/7个Handler，变更接口，集成 ScServiceController
metadata:
  type: project
---

# BIF-P0-004：CommandDispatcher 命令分发层收尾

> 对应阶段：BIF-P0-004
> 完成日期：2026-05-14
> 操作代理：Claude Code
> 影响范围：CommandDispatcher / CommandHandler / CommandContext / CommandResult / 13 Handler / ScServiceController / 39 测试

**关联记忆：** [[BIF-P0-003-xmldata-layer]], [[BIF-P0-002-soap-wsdl]], [[BIF-P0-001-command-mapping]], [[RULE-001-operation-memory-rule]]

---

## 任务目标

在已完成的 SOAP 层（P0-002）和 XMLData 层（P0-003）之上，建立统一命令分发基础设施：CommandDispatcher 路由 + CommandContext/CommandResult 模型 + 全部 13 个 Handler 占位。

## 规则加载确认

- 已读取 AGENTS.md / CLAUDE.md / docs/PROJECT_ENGINEERING_RULES.md
- 已读取 docs/memory/README.md 及前序 P0-001~P0-003 审计/记忆
- 已确认 B接口协议 2016 是唯一设备协议
- 已确认本阶段遵循 论证 → 验证 → 写入
- 已确认本阶段完成后写入 docs/memory/

## 总体架构判断

CommandDispatcher 位于处理链路中间：
Controller → SoapMessageHandler → **CommandDispatcher** (含 XmlDataParser) → CommandHandler → CommandResult → SoapMessageHandler.buildResponse()

Dispatcher 职责边界：只做路由 + 上下文构造 + 异常兜底，不写业务逻辑。

## 协议一致性判断

完全符合 B接口协议 2016。所有 16 个 PK_Type（含 UNKNOWN）映射正确，SEND_DATA≠GET_DATA，SEND_ALARM≠SET_POINT。

## 修改前论证

**Why CommandContext/CommandResult 新模型？**
- 旧接口 handle(BInterfaceMessage) → BInterfaceMessage 不携带 xmlData 解析结果
- 新接口 handle(CommandContext) → CommandResult 让 Handler 接收结构化 XmlDataModel，返回统一结果
- CommandResult 内置 toInfoXml()/toXmlDataXml() 可直接供 SoapMessageHandler.buildResponse() 使用

**Why 变更所有 Handler 接口？**
- 旧接口 6 个 Handler 均为返回 ResultCode=0 的桩，无真实业务逻辑，变更成本低
- 新接口统一 notImplemented 占位返回，待 BIF-P1 阶段替换为真实 Handler

**Why ScServiceController 集成？**
- 消除 Controller 中硬编码的 ResultCode=0 响应
- 让 Controller 只做 HTTP 收发 + SOAP Fault 构造

## 写入前验证

| 验证项 | 结果 |
|--------|------|
| 编译通过 | 通过 |
| 全部 212 B-interface 测试通过 | 通过 |
| 12 个已注册 PK_Type 正确分发 | 通过 |
| SEND_DATA ≠ GET_DATA 不同 Handler | 通过 |
| SEND_ALARM ≠ SET_POINT 不同 Handler | 通过 |
| 未注册 PK_Type → UNKNOWN 兜底 | 通过 |
| Handler 异常 → CommandResult(5001) | 通过 |
| Handler 返回 null → CommandResult(5001) | 通过 |
| 畸形 xmlData → CommandResult(2001) | 通过 |
| Dispatcher 不返回 null | 通过 |
| hasHandler() 正确判断 | 通过 |
| 前序 P0-001~003 测试未回归 | 通过 |

## 实际修改文件

### 新增文件 (11)

- `command/CommandContext.java` — 命令上下文模型
- `command/CommandResult.java` — 命令结果模型（含 toInfoXml/toXmlDataXml）
- `command/GetThresholdCommandHandler.java` — GET_THRESHOLD 桩
- `command/SetThresholdCommandHandler.java` — SET_THRESHOLD 桩
- `command/SetPointCommandHandler.java` — SET_POINT 桩
- `command/GetFtpCommandHandler.java` — GET_FTP 桩
- `command/SetFtpCommandHandler.java` — SET_FTP 桩
- `command/GetLoginInfoCommandHandler.java` — GET_LOGININFO 桩
- `command/TimeCheckCommandHandler.java` — TIME_CHECK 桩
- `test/.../CommandDispatcherTest.java` — 21 分发测试
- `test/.../CommandDispatcherErrorTest.java` — 6 错误处理测试
- `test/.../CommandContextTest.java` — 4 上下文模型测试
- `test/.../CommandResultTest.java` — 8 结果模型测试

### 修改文件 (10)

- `command/CommandHandler.java` — 接口变更
- `command/CommandDispatcher.java` — 集成 XmlDataParser + 异常捕获
- `command/LoginCommandHandler.java` — 新接口 + notImplemented
- `command/HeartbeatCommandHandler.java` — 新接口 + notImplemented
- `command/SendDataCommandHandler.java` — 新接口 + notImplemented
- `command/SendAlarmCommandHandler.java` — 新接口 + notImplemented
- `command/GetDataCommandHandler.java` — 新接口 + notImplemented
- `command/UnknownCommandHandler.java` — 新接口 + ResultCode=1002
- `xml/XmlDataParser.java` — 添加 @Component
- `service/sc/ScServiceController.java` — 集成 CommandDispatcher

## 核心改动

1. **CommandContext**: 封装 pkType / soapMessage / xmlData(XmlDataModel) / rawSoap / rawXmlData / attributes
2. **CommandResult**: 统一结果模型，success/resultCode/implemented + toInfoXml()/toXmlDataXml() 工厂方法
3. **CommandHandler 接口变更**: 从 `handle(BInterfaceMessage)` 改为 `handle(CommandContext)` → `CommandResult`
4. **CommandDispatcher**: 内部调用 XmlDataParser.parse()，异常全部捕获为结构化 CommandResult
5. **ScServiceController**: 注入 CommandDispatcher，替换硬编码 ResultCode=0 响应
6. **全部 13 Handler**: 均使用 `CommandResult.notImplemented()` 占位，待 BIF-P1 阶段实现

## 测试命令和结果

```bash
mvn test -Dtest='*BInterface*,*Command*,*XmlData*,*Soap*,*Wsdl*'
Tests run: 212, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 是否修改业务代码

是 — 接口变更 + 集成 Controller。但所有 Handler 均为占位（notImplemented），无真实业务逻辑。

## 是否涉及数据库

否。

## 是否涉及前端

否。

## 是否涉及 DSC/RDS

否 — 完全基于 B接口协议 2016。

## 风险点

- 所有 13 个 Handler 为 notImplemented 占位，生产环境返回 ResultCode=1
- ScServiceController 集成未在生产环境全链路测试
- XmlDataParser 添加 @Component 后成为 Spring 管理的单例，线程安全（无状态）

## 遗留问题

- GET_HISTORY_DATA/GET_FSUINFO/SET_DATA/SET_FSUREBOOT 无 fixture，未注册 Handler
- SET_FSUREBOOT 安全开关（safe_enabled=FALSE）未实现

## 下一步建议

1. **BIF-P1-001** — LOGIN 注册/Session 管理业务逻辑
2. **BIF-P1-002** — HEARTBEAT 在线状态更新
3. **BIF-P1-003** — SEND_DATA 实时数据入库
4. **BIF-P1-004** — SEND_ALARM 告警入库

## Git diff 摘要

项目非 git 仓库，以文件变更记录替代。

## Git status 摘要

项目非 git 仓库。
