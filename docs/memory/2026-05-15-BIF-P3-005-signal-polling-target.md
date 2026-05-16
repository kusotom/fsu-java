---
name: BIF-P3-005-signal-polling-target
description: 建立慢数据通道 Signal 轮询目标配置，新增 SignalPollingTargetService，复用 MonitoringPoint，新增 13 测试，全量 571 通过
metadata:
  type: project
---

# BIF-P3-005：慢数据通道 Signal 配置 / 轮询目标配置

> 对应阶段：BIF-P3-005
> 完成日期：2026-05-15
> 操作代理：Claude Code
> 影响范围：2 新生产文件 + 2 修改生产文件 + 4 测试文件（+13 测试）

**关联记忆：** [[BIF-P3-004-slow-data-polling]], [[BIF-P3-002-get-threshold]], [[BIF-P3-001-fsu-service-client-get-data]]

---

## 任务目标

基于 BIF-P3-004 慢数据轮询框架，建立 Signal 轮询目标配置能力，解决 GET_DATA 传空 signalId 列表的问题。核心约束：
- 复用 MonitoringPoint 已有模型，不新增数据库表
- 支持按 fsuCode 查询 GET_DATA / GET_THRESHOLD 目标
- SlowDataPollingService 不再传空 signalId 列表
- 无配置时跳过并统计 skippedNoSignalCount
- 不启用真实设备轮询，不访问真实设备

## 架构判断

```
SlowDataPollingService
  → SignalPollingTargetService.findTargets(fsuCode, commandType)
    → FsuDeviceRepository.findByFsuCode() → fsuId
    → MonitoringPointRepository.findByFsuId() → pointCode=signalId
    → filter ACTIVE → List<SignalPollingTarget>
```

关键决策：复用已有 `MonitoringPointEntity`（含 fsuId, pointCode, status, pollingInterval），不新增表。

## 协议一致性判断

- 轮询目标配置不涉及协议字段
- GET_DATA / GET_THRESHOLD 的协议结构不变
- SignalID 来自 monitoring_point.pointCode，符合 B接口协议命名约定
- 不使用 DSC/RDS

## 修改前论证

### 为什么当前应先做 Signal 轮询目标配置

BIF-P3-004 遗留的首要问题是 GET_DATA 传空 signalId 列表。轮询框架已就绪但无目标信号可查。

### 如何复用现有模型

- MonitoringPointEntity 已有 fsuId、pointCode（=signalId）、status、pollingInterval
- MonitoringPointRepository.findByFsuId() 可直接查询
- 不需要新增数据库表或实体

### 如何避免硬编码 SignalID

所有 SignalID 来自 MonitoringPoint 配置，不在代码中写死。StubFsuServiceClient 的 fixture 信号仅用于样板响应，不作为配置依据。

## 写入前验证

- [x] SignalPollingTarget 模型含 fsuCode/deviceId/signalId/commandType/enabled
- [x] SignalPollingTargetService 通过 fsuCode → fsuId → MonitoringPoint 查询
- [x] 支持 GET_DATA / GET_THRESHOLD 命令类型
- [x] 过滤 status=ACTIVE
- [x] SET_THRESHOLD 等命令返回空列表
- [x] 异常/空值返回空列表
- [x] SlowDataPollingService 使用 SignalPollingTargetService 获取 signalId
- [x] 无 target 时跳过并统计 skippedNoSignalCount
- [x] GET_DATA 不再传空 signalId 列表
- [x] 不引用 SetThresholdService / SetThresholdSafetyGate

## 新增/修改文件

### 新增生产文件（2 个）

| 文件 | 说明 |
|------|------|
| `.../service/slow/SignalPollingTarget.java` | 轮询目标模型（5 字段） |
| `.../service/slow/SignalPollingTargetService.java` | 目标查询服务（findTargets/hasTargets） |

### 修改生产文件（2 个）

| 文件 | 说明 |
|------|------|
| `.../service/slow/SlowDataPollingResult.java` | 新增 skippedNoSignalCount |
| `.../service/slow/SlowDataPollingService.java` | 注入 SignalPollingTargetService，使用 signalId 列表 |

### 新增/修改测试（4 个）

| 文件 | 测试数 | 说明 |
|------|--------|------|
| `.../service/slow/SignalPollingTargetServiceTest.java` | 12 | 目标查询单元测试 |
| `.../service/slow/SlowDataPollingServiceTest.java` | 17 (+1) | 新增无 target 跳过测试 |
| `.../service/slow/SlowDataPollingSchedulerTest.java` | 4 (不变) | 适配 6 参构造函数 |
| `.../SlowDataPollingIntegrationTest.java` | 10 (扩展) | 适配目标配置全链路 |

## 核心改动

### 1. SignalPollingTarget + SignalPollingTargetService

新增两个文件，基于 MonitoringPoint 查询轮询目标。

### 2. SlowDataPollingService 流程变更

原：`ONLINE + loggedIn → GET_DATA(List.of())` → 必然失败
新：`ONLINE + loggedIn → queryTargets → 有 target 执行 GET_DATA(signalIds) / 无 target 跳过`

### 3. SlowDataPollingResult 新增字段

`skippedNoSignalCount` — ONLINE + 已登录但无信号配置的 FSU 数量。

## 测试结果

```
[INFO] Tests run: 571, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 是否修改业务代码等项目

| 项目 | 状态 |
|------|------|
| 修改业务代码 | 是（SlowDataPollingService + Result） |
| 新增业务代码 | 是（SignalPollingTarget + Service） |
| 涉及数据库 | 否（复用 MonitoringPoint 表） |
| 涉及前端 | 否 |
| 涉及 DSC/RDS | 否 |
| 访问真实设备 | 否 |
| 启用真实 FSU 调用 | 否 |
| 启用 Scheduler | 否 |
| 执行 SET 类命令 | 否 |

## 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| MonitoringPoint.status 语义不确定 | 低 | 以 "ACTIVE" 过滤 |
| pointCode=SignalID 隐式映射 | 低 | 如需差异化映射需后续加表 |
| 无 commandType 区分 | 低 | GET_DATA/GET_THRESHOLD 同批信号 |

## 遗留问题

1. 无 commandType 区分（GET_DATA/GET_THRESHOLD 用同一批 signal）
2. 无前端配置页面
3. 信号自动发现未实现

## 下一步建议

**BIF-P4-001**：慢数据通道启用确认 — 评估产出、确认配置、考虑在开发环境启用轮询。

---

## Git diff 摘要

### 新增文件
- `.../service/slow/SignalPollingTarget.java` — ~40 行
- `.../service/slow/SignalPollingTargetService.java` — ~90 行
- `.../service/slow/SignalPollingTargetServiceTest.java` — ~290 行，12 测试

### 修改文件
- `.../service/slow/SlowDataPollingResult.java` — 新增 skippedNoSignalCount（+~10 行）
- `.../service/slow/SlowDataPollingService.java` — 注入 SignalPollingTargetService，target 流程（~180 行重写）
- `.../service/slow/SlowDataPollingServiceTest.java` — 新增 target stub + 1 测试（~330 行重写）
- `.../service/slow/SlowDataPollingSchedulerTest.java` — 适配 6 参构造（~5 行）
- `.../SlowDataPollingIntegrationTest.java` — 新增 MonitoringPoint stub（~360 行重写）
