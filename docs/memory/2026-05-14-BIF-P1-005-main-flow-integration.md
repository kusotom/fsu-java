---
name: BIF-P1-005-main-flow-integration
description: 完成主链路端到端集成测试与回归矩阵，新增 BInterfaceMainFlowIntegrationTest（23 测试），全量 359 测试通过
metadata:
  type: project
---

# BIF-P1-005：主链路端到端集成测试与回归矩阵

> 对应阶段：BIF-P1-005
> 完成日期：2026-05-14
> 操作代理：Claude Code
> 影响范围：BInterfaceMainFlowIntegrationTest（新增 23 测试）

**关联记忆：** [[BIF-P1-004-send-alarm]], [[BIF-P1-003-send-data]], [[BIF-P1-002-heartbeat-session]], [[BIF-P1-001-login-session]]

---

## 任务目标

对已完成的 B接口主链路（LOGIN → HEARTBEAT → SEND_DATA → SEND_ALARM）进行端到端集成验证，确认四个核心流程按顺序协同工作，形成长期回归矩阵。

## 规则加载确认

- 已读取 AGENTS.md / CLAUDE.md / docs/PROJECT_ENGINEERING_RULES.md
- 已读取 docs/memory/README.md 及前序 P0-001~P1-004 审计/记忆
- 已确认本阶段只做集成测试，不实现新业务

## 总体架构判断

LOGIN → HEARTBEAT → SEND_DATA → SEND_ALARM 形成完整的快数据通道主链路。集成测试使用真实 Handler + 真实 Service + Stub Repository，在不依赖 Spring 上下文的情况下验证全链路协同工作。

## 协议一致性判断

测试使用的字段格式（FSUCode, CollectTime, AlarmTime, SignalID, Value, AlarmCode, AlarmLevel, AlarmType）均与 fixtures 和 expected JSON 一致。

## 修改前论证

**Why 不直接进入 P2？**
在进入离线检测或慢数据通道前，必须先确认主链路稳定，防止后续开发破坏已完成的业务闭环。

**Why 使用 stub 而非真实数据库？**
与项目现有测试模式一致（所有单元测试均使用 HashMap stub），避免引入 Spring 上下文或数据库依赖。

## 写入前验证

| 验证项 | 结果 |
|--------|------|
| 四个核心 Handler 已实现 | LOGIN/HEARTBEAT/SEND_DATA/SEND_ALARM 均为真实实现 |
| LoginService 可注入 Stub | 已有 LoginServiceTest 模式 |
| SendDataService 可注入 Stub | 已有 SendDataServiceTest 模式 |
| SendAlarmService 可注入 Stub | 已有 SendAlarmServiceTest 模式 |
| 所有 Stub Repository 可用 | 复用 BaseStub 模式 |

## 实际新增文件

| 文件 | 说明 |
|------|------|
| `.../binterface/BInterfaceMainFlowIntegrationTest.java` | 23 集成测试 |
| `docs/audit/BIF-P1-005-main-flow-integration.md` | 审计文档 + 回归矩阵 |
| `docs/memory/2026-05-14-BIF-P1-005-main-flow-integration.md` | 操作记忆 |

## 核心改动

### BInterfaceMainFlowIntegrationTest（23 测试）

| 类别 | 测试数 | 覆盖内容 |
|------|--------|---------|
| 全链路 | 1 | LOGIN→HEARTBEAT→SEND_DATA→SEND_ALARM 按序成功 |
| 顺序依赖 | 3 | 未 LOGIN 时各命令返回 1002 |
| 登录态 | 3 | LOGIN 后各命令成功 |
| 状态变化 | 5 | Session创建/lastHeartbeat更新/实时数据写入/告警写入/告警恢复 |
| 数据隔离 | 4 | 各命令不跨域创建数据 |
| 边界 | 4 | 重复登录/重复心跳/心跳后 session 保持 |
| 缺少字段 | 3 | 缺少 FSUCode 返回 2001 |

## 测试命令和结果

```bash
# 全量 B 接口测试
mvn test -Dtest="com.dcim.platform.binterface.**"
# Tests run: 359, Failures: 0, Errors: 0, Skipped: 0
```

## 风险

| 风险 | 等级 | 说明 |
|------|------|------|
| 无数据库级事务验证 | 低 | Stub 不模拟事务行为 |
| 无真实 SOAP 全链路 | 低 | 手动构造 CommandContext，不经过 SoapMessageHandler |

## 遗留问题

1. 慢数据通道命令未测试（GET_DATA 等 — BIF-P2）
2. 无高并发场景测试
3. 无数据库事务回滚测试

## 下一步建议

1. BIF-P2-001：离线检测 — 定时扫描 lastHeartbeat 超时 FSU
2. BIF-P2-002：慢数据通道 — GET_DATA / GET_THRESHOLD / SET_THRESHOLD
