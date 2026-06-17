# LANDING-008 Plan：B接口2016 被动上报接收联调准备

> 计划版本：1.0
> 创建日期：2026-05-20（SPEC-INIT-001）
> 对应 Spec：`openspec/specs/landing-008-passive-reporting-readiness.md`

---

## 1. 读取资料

执行前必须按顺序加载以下文件：

| # | 文件 | 用途 |
|---|------|------|
| 1 | `docs/rules/CLAUDE_PROJECT_RULES.md` | 最高优先级规则 |
| 2 | `docs/PROJECT_ENGINEERING_RULES.md` | 工程规则（如存在） |
| 3 | `docs/memory/WORKING-MEMORY.md` | 当前工作记忆 |
| 4 | `docs/landing/PASSIVE-REPORTING-INTEGRATION-PLAN.md` | 被动上报集成方案 |
| 5 | `docs/audit/LANDING-006-binterface-2016-code-retry.md` | 2016 码表兼容审计 |
| 6 | `docs/audit/LANDING-007-binterface-2016-get-data-real-deviceid.md` | GET_DATA DeviceID 审计 |
| 7 | `docs/audit/LANDING-008-passive-reporting-readiness.md` | 被动上报链路核对审计 |
| 8 | `openspec/specs/landing-008-passive-reporting-readiness.md` | LANDING-008 规格 |

## 2. 核对协议

### 2.1 B接口2016 被动上报命令

| 命令 | 2016 Code | ACK Code | 核对状态 |
|------|-----------|----------|----------|
| LOGIN | 101 | LOGIN (同名) | ✅ 已核对 |
| HEARTBEAT | — | HEARTBEAT (同名) | ✅ 已核对 |
| SEND_ALARM | 501 | SEND_ALARM (同名) | ✅ 已核对 |
| SEND_DATA | 待确认 | SEND_DATA (同名) | ✅ 已核对（2016 存在 SEND_DATA） |
| GET_DATA | 401 | GET_DATA_ACK 402 | ✅ 已核对（出站查询，非被动上报） |

### 2.2 关键协议约定

- SC 入站 ACK 使用 **与请求同名的 PK_Type**（非 `_ACK` 后缀）
- SC 入站 ACK 使用 **纯文本 PK_Type**（非 Name+Code 格式）
- FSU 出站（GET_*）使用 Name+Code 格式

## 3. 核对代码链路

### 3.1 Controller 层

- [x] `ScServiceController` — `POST /api/b-interface/sc-service`
- [x] SOAP 解析入口：`SoapMessageHandler.parse()`
- [x] 响应构建：`SoapMessageHandler.buildResponse()`
- [x] 命令分发：`CommandDispatcher.dispatch()`

### 3.2 CommandHandler 层

- [x] `LoginCommandHandler` — PK_Type=LOGIN → `LoginService.login()`
- [x] `HeartbeatCommandHandler` — PK_Type=HEARTBEAT → `LoginService` 复用
- [x] `SendDataCommandHandler` — PK_Type=SEND_DATA → `SendDataService.processData()`
- [x] `SendAlarmCommandHandler` — PK_Type=SEND_ALARM → `SendAlarmService.processAlarms()`

### 3.3 Service 层

- [x] `LoginService` — 会话创建/状态维护/设备更新
- [x] `SendDataService` — SignalID 匹配 → realtime_data upsert
- [x] `SendAlarmService` — 告警解析 → alarm_record 创建/更新

### 3.4 数据层

- [x] `BInterfaceMessageLog` — Entity 已定义，**Service 已实现** ✅
- [x] `AlarmRecordEntity` — serialNo/deviceId 已补强，**spid 列已补** ✅
- [x] `monitoring_point` — 表结构已就绪，**数据待管理方提供**

### 3.5 协议层

- [x] `BInterfaceCommand2016` — 2016 命令名→Code 映射表
- [x] `BInterfacePkType` — 18 个命令枚举
- [x] `CommandDispatcher` — 自动发现所有 CommandHandler Bean

## 4. 输出文档

| # | 文件 | 说明 | 状态 |
|---|------|------|------|
| 1 | `docs/landing/PASSIVE-REPORTING-INTEGRATION-PLAN.md` | 被动上报集成方案 (12 节) | ✅ LANDING-008 |
| 2 | `docs/landing/FSU-SC-CONFIGURATION-GUIDE.md` | FSU 侧 SC 配置指南 | ✅ LANDING-008 |
| 3 | `docs/landing/PASSIVE-REPORTING-CHECKLIST.md` | 联调检查清单 (12 节) | ✅ LANDING-008 |
| 4 | `docs/audit/LANDING-008-passive-reporting-readiness.md` | 审计报告 | ✅ LANDING-008 |
| 5 | `docs/memory/2026-05-20-LANDING-008-passive-reporting-readiness.md` | 工程记忆 | ✅ LANDING-008 |
| 6 | `openspec/specs/landing-008-passive-reporting-readiness.md` | OpenSpec 规格 | ← SPEC-INIT-001 创建 |
| 7 | `openspec/plans/landing-008-passive-reporting-readiness-plan.md` | 执行计划 | ← 本文件 |
| 8 | `docs/memory/2026-05-20-SPEC-INIT-001-openspec-superpowers-workflow.md` | SPEC-INIT-001 工程记忆 | ← SPEC-INIT-001 创建 |
| 9 | `docs/audit/SPEC-INIT-001-openspec-superpowers-workflow.md` | SPEC-INIT-001 审计文档 | ← SPEC-INIT-001 创建 |

## 5. 验证

### 5.1 如果只新增文档（当前情况）

```bash
git diff -- openspec docs/landing docs/audit docs/memory
git status --short
```

### 5.2 如果修改 Java

```bash
mvn test -Dtest='*SendData*'
mvn test -Dtest='*SendAlarm*'
mvn test -Dtest='*BInterface*'
mvn test
```

## 6. 完成条件

| # | 条件 | 状态 |
|---|------|------|
| 1 | spec 已创建 | ✅ `openspec/specs/landing-008-passive-reporting-readiness.md` |
| 2 | plan 已创建 | ✅ 本文件 |
| 3 | 后续执行必须按 spec / plan | ✅ 规则已写入 CLAUDE_OPENSPEC_RULES.md |
| 4 | memory 已更新 | ✅ 原有 LANDING-008 + 新增 SPEC-INIT-001 |
| 5 | README 已更新 | ✅ `openspec/README.md` + `project.md` |
| 6 | Java 生产代码修改范围受控 | ✅ (3 生产文件 + 2 测试文件) |
| 7 | 未访问真实 FSU | ✅ |
| 8 | 未执行 SET | ✅ |
| 9 | 未启用 Scheduler | ✅ |
| 10 | 全量测试 1177 | ✅ (0/0/5) |

## 7. 后续执行指南

当需要执行 LANDING-008 的具体实施步骤（如实现 BInterfaceMessageLogService、补充 spid 列等）时：

1. 先读取 `openspec/specs/landing-008-passive-reporting-readiness.md`
2. 先读取本 plan 文件
3. 先读取 `docs/memory/WORKING-MEMORY.md` 获取最新状态
4. 按 CLAUDE_PROJECT_RULES.md 第13节输出修改前论证
5. 按 TDD 流程：先写测试 → 再写实现 → 回归全量
6. 完成后写入 memory + audit
