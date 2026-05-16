---
name: BIF-P1-002-heartbeat-session
description: 完成 HEARTBEAT 心跳/在线状态闭环，HeartbeatCommandHandler 从桩变真实实现，复用 LoginService Session 管理能力，新增 14 测试
metadata:
  type: project
---

# BIF-P1-002：HEARTBEAT 心跳 / 在线状态闭环

> 对应阶段：BIF-P1-002
> 完成日期：2026-05-14
> 操作代理：Claude Code
> 影响范围：HeartbeatCommandHandler（桩→真实实现）/ HeartbeatCommandHandlerTest（14 测试）

**关联记忆：** [[BIF-P1-001-login-session]], [[BIF-P0-004-command-dispatcher]], [[BIF-P0-003-xmldata-layer]]

---

## 任务目标

在 LOGIN Session 已建立的基础上，闭环 HEARTBEAT 命令：接收心跳、校验登录态、更新最后活跃时间、返回协议响应。不重复实现 Session 管理，不复写 LoginService。

## 规则加载确认

- 已读取 AGENTS.md / CLAUDE.md / docs/PROJECT_ENGINEERING_RULES.md
- 已读取 docs/memory/README.md 及前序 P0-001~P1-001 审计/记忆
- 已确认 B接口协议 2016 是唯一设备协议
- 已确认本阶段遵循 论证 → 验证 → 写入
- 已确认本阶段完成后写入 docs/memory/

## 总体架构判断

HEARTBEAT 位于命令处理层，是 LOGIN 之后的第一个业务命令。它不创建新 Session，只对已有 Session 做保活校验和更新时间更新。

## 协议一致性判断

符合 B接口协议 2016 HEARTBEAT 规范：
- 请求 Info.FSUCode → 标识提取
- 响应 Info 包含 ResultCode/ServerTime
- xmlData 在 HEARTBEAT 响应中为空
- ResultCode 复用 B接口标准值（0/1002/2001/5001）

## 修改前论证

**Why HEARTBEAT 在 SEND_DATA/SEND_ALARM 前实现？**
HEARTBEAT 是最简单的保活命令，只需校验登录态 + 更新时间，不涉及数据入库。先做 HEARTBEAT 可以验证 Session 管理基础设施的完备性，再进入数据上报的复杂逻辑。

**Why 不新增 HeartbeatResult？**
CommandResult 和 LoginService 现有方法已足以表达 HEARTBEAT 业务语义，无意义新增模型只会增加维护成本。

**Why 不复写 LoginService？**
LoginService.updateLastSeen() / isLoggedIn() / getActiveSession() 在 BIF-P1-001 已设计为供 HEARTBEAT/SEND_DATA/SEND_ALARM 复用，不需要重复实现。

## 写入前验证

| 验证项 | 结果 |
|--------|------|
| HeartbeatCommandHandler 当前状态 | 为 `return notImplemented` 桩 |
| LoginService 是否有 updateLastSeen/isLoggedIn/getActiveSession | 有（BIF-P1-001 已实现） |
| XmlDataModel 是否能解析 HEARTBEAT | 可解析（xmlData 已通过 SoapMessageHandler 传入） |
| HEARTBEAT fixtures 可用 | 是（soap + xmldata + expected 均存在） |
| 当前 ResultCode 定义 | 0/1002/2001/5001 已可用 |
| 当前测试框架 | JUnit 5 + Maven Surefire |
| DSC/RDS 污染风险 | 无（完全不涉及） |

## 实际修改文件

### 修改文件
| 文件 | 说明 |
|------|------|
| `.../command/HeartbeatCommandHandler.java` | 桩 → 真实实现：提取 FSUCode → isLoggedIn → updateLastSeen → 构建响应 + extractSessionId |

### 新增文件
| 文件 | 说明 |
|------|------|
| `.../binterface/HeartbeatCommandHandlerTest.java` | 14 测试：成功/失败/异常/extractSessionId |
| `docs/audit/BIF-P1-002-heartbeat-session.md` | 审计文档 |
| `docs/memory/2026-05-14-BIF-P1-002-heartbeat-session.md` | 操作记忆 |

### 不修改
LoginService、LoginResult、CommandResult、CommandDispatcher、SoapMessageHandler、任何数据库实体

## 核心改动

### HeartbeatCommandHandler.handle()
```
context == null → 5001
infoXml == null/empty → 2001
FSUCode 为空 → 2001
isLoggedIn == false → 1002 (updateLastSeen 不被调用)
updateLastSeen → 更新 lastHeartbeat/onlineStatus/lastOnlineTime
成功 → ResultCode=0 + ServerTime
异常 → 5001
```

### 新增 extractSessionId()
`public static`，正则 `<SessionID[^>]*>(.*?)</SessionID>` 大小写不敏感

## 测试命令和结果

```bash
# 全量 B 接口测试
mvn test -Dtest="com.dcim.platform.binterface.**"
# Tests run: 258, Failures: 0, Errors: 0, Skipped: 0
```

## 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| isLoggedIn 仅查 loginStatus 字段 | 低 | Session 过期逻辑后续补充 |
| 无离线检测定时任务 | 低 | 待 BIF-P2 阶段实现 |

## 遗留问题

1. HEARTBEAT 不更新 Session.lastActiveTime（当前设计如此，仅 BInterfaceFsuStatusEntity.lastHeartbeat 更新）
2. 无 Session 有效期主动校验

## 下一步建议

1. BIF-P1-003：SEND_DATA — 复用 isLoggedIn / getActiveSession 做上报前校验
2. BIF-P1-004：SEND_ALARM

## Git diff 摘要

### HeartbeatCommandHandler.java
- 移除 `return CommandResult.notImplemented(BInterfacePkType.HEARTBEAT)`
- 新增 LoginService 注入
- 新增 handle() 实现：null 检查 → extractFsuCode → isLoggedIn → updateLastSeen → buildSuccessResponse
- 新增 extractSessionId() 工具方法
- 新增 buildSuccessResponse() 构造响应 Info
