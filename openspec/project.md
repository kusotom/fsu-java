# OpenSpec Project Context — FSU 动环监控平台

> 本文件为 OpenSpec 规格驱动层提供项目级上下文。
> 最后更新：2026-05-20（OPENSPEC-001）

---

## 1. 项目概述

**FSU-JAVA** 是机房动环监控平台（FSU — Field Supervision Unit），基于 Java 17 + Spring Boot 3.2.5 构建。平台通过 B接口协议 2016/2024 与 FSU 设备进行 SOAP/XMLData 通信，实现设备注册、心跳、数据采集、告警上报、门限管理、遥控遥调等功能。

## 2. 项目位置

```
项目根: /home/tom/桌面/FSU/fsu-platform-java/
```

## 3. 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2.5 |
| 持久化 | JPA / Hibernate |
| 数据库 | PostgreSQL 16 (Docker, 容器名 `dcim-postgres`) |
| 测试 | JUnit 5 + Maven Surefire |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus |
| 协议 | B接口 2016/2024, SOAP 1.1, WSDL 1.1, RPC/encoded style |
| 构建 | Maven |

## 4. 协议概要

### 4.1 B接口协议

B接口协议 2016 是本项目设备上报、采集、交互、管理的唯一协议标准。

**快数据通道 (SCService)**：
- WSDL 端点：`/services/SCService`
- 方向：FSU → 平台（入站）
- 命令：LOGIN(101), HEARTBEAT, SEND_DATA, SEND_ALARM(501)

**慢数据通道 (FSUService)**：
- WSDL 端点：`/services/FSUService`
- 方向：平台 → FSU（出站）
- 命令：GET_DATA(401), GET_THRESHOLD, GET_ACTIVEALARM(603), GET_LOGININFO(1501), GET_SUINFO, GET_SUFTP, GET_SPCONFIGOPTION, TIME_CHECK, SET_THRESHOLD, SET_TIME, SET_POINT 等

### 4.2 关键约定

- SOAP 1.1, RPC/encoded style, `operation=invoke`
- PK_Type 支持两种格式：Name+Code (2024) 和纯文本 (2016)
- ACK 响应使用与请求同名的 PK_Type（非 `_ACK` 后缀）
- 所有设备上报、采集、交互必须以 B接口 SOAP/XMLData 为准

## 5. 项目架构

```
Controller / Handler
  → Application Service / Orchestrator
    → Domain Service
      → Repository / Adapter
        → Protocol / XML / SOAP 工具层
```

## 6. 当前安全配置

```yaml
real-call-enabled: false
scheduler-enabled: false
set-threshold.enabled: false
offline-detection.enabled: false
active-alarm-audit.scheduler-enabled: false
set-command-safety.scheduler-forbidden: true
```

## 7. 当前阶段

项目最新阶段为 **LANDING-008**（被动上报接收联调准备）。

| 阶段 | 内容 | 状态 |
|------|------|------|
| BIF-P0 | XMLData 解析 + CommandDispatcher | ✅ |
| BIF-P1 | LOGIN/HEARTBEAT/SEND_DATA/SEND_ALARM 闭环 | ✅ |
| BIF-P2 | 离线检测 + 慢数据通道审计 | ✅ |
| BIF-P3 | FSU SOAP 客户端 + GET_DATA/THRESHOLD/TIME_CHECK 等 | ✅ |
| BIF-P4 | 真实 FSU 联调 + 2024 协议迁移 + SET 安全 | ✅ |
| BIF-P4-FIX-001 | Codex 审计阻断修复 | ✅ |
| BIF-P4-021~023 | 活动告警审计 Scheduler/API/持久化 | ✅ |
| LANDING-001 | 真实点位表模型补强 | ✅ |
| LANDING-002 | B接口2024附录标准化配置模板 | ✅ |
| LANDING-003 | 真实 FSU 只读联调 | ✅ |
| LANDING-004 | 真实 FSU 兼容性重试 | ✅ |
| LANDING-005 | 真实 FSUID 只读重试 | ✅ |
| LANDING-006 | B接口2016 码表验证 | ✅ |
| LANDING-007 | GET_DATA 2016 真实 DeviceID 查询 | ✅ |
| LANDING-008 | 被动上报接收联调准备 | ✅ |

## 8. 测试基线

以最新 memory/audit 记录为准。最近一次全量测试记录（LANDING-008）为：

```
1164 tests, 0 failures, 0 errors, 5 skipped
contextLoads: PASS
```

## 9. 安全边界

| 边界 | 状态 | 说明 |
|------|------|------|
| 真实 FSU 访问 | 默认关闭 | `@Tag("real-fsu")` + `@EnabledIfSystemProperty` |
| Scheduler | 默认关闭 | `scheduler-enabled: false` |
| SET 命令 | 默认禁止 | 必须经过安全门禁 + confirmationToken + 审计 |
| alarm_record 写入 | 默认只读 | 活动告警审计类任务只读 |

## 10. 禁止事项

- 禁止重新引入 DSC / RDS 作为业务主协议
- 禁止绕过 B接口 XML/SOAP 的私有采集协议
- 禁止使用临时 JSON 结构代替 B接口 XMLData
- 禁止自造 `MsgType`、`SignalID`、`DeviceID`、`FsuCode`、`ResultCode`
- 禁止在未论证的情况下扩展协议字段
- 禁止在主业务代码中混入 legacy/research 协议逻辑
- 禁止绕过安全门禁执行 SET 命令
- 禁止默认测试访问真实 FSU

## 11. 规则文件

| 文件 | 用途 |
|------|------|
| `docs/rules/CLAUDE_PROJECT_RULES.md` | 最高优先级规则（20节） |
| `docs/rules/CLAUDE_OPENSPEC_RULES.md` | OpenSpec 规格驱动层规则 |
| `openspec/project.md` | 本文件 |
| `docs/memory/README.md` | 工程记忆索引 |
| `docs/memory/WORKING-MEMORY.md` | 当前工作记忆 |
| `docs/audit/` | 审计文档 |

## 12. 当前阻塞与遗留

1. FSU 不支持 GET_* 出站查询（所有 GET_* 返回空响应）
2. FSU Code 分配表与 2024 标准不一致（501→SEND_ALARM 而非 GET_DATA）
3. 真实 SUID 未知
4. 管理方未提供 DeviceID/SPID 点位表
5. `BInterfaceMessageLogService` 待实现（报文日志）
6. `alarm_record` 缺少 `spid` 列
7. Codex 集中复审暂缓（累积项：BIF-P4-FIX-001 ~ LANDING-008）
