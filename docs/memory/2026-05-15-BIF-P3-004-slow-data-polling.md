---
name: BIF-P3-004-slow-data-polling
description: 建立慢数据通道定时轮询框架，新增 4 生产文件 + 修改 1 配置 + 38 测试（SlowDataPollingService/Scheduler/Properties/Result），全量 558 通过
metadata:
  type: project
---

# BIF-P3-004：慢数据通道定时轮询框架

> 对应阶段：BIF-P3-004
> 完成日期：2026-05-15
> 操作代理：Claude Code
> 影响范围：4 新生产文件 + 1 修改配置文件 + 4 测试文件（+38 测试）

**关联记忆：** [[BIF-P3-003-set-threshold]], [[BIF-P3-002-get-threshold]], [[BIF-P3-001-fsu-service-client-get-data]], [[BIF-P2-001-offline-detection]]

---

## 任务目标

基于已完成的 FsuServiceClient、GET_DATA、GET_THRESHOLD、SET_THRESHOLD 安全闭环，建立慢数据通道的定时轮询框架。核心约束：
- 只实现可测试、默认关闭、Stub 模式的轮询框架
- 不允许默认访问真实设备
- 不允许真实批量采集
- 不允许真实下发控制命令
- 不允许 SET_THRESHOLD / SET_POINT 进入轮询

## 架构判断

轮询架构：
```
SlowDataPollingScheduler (默认 disabled, 只触发不写逻辑)
  → SlowDataPollingService (任务编排)
    → FsuDeviceRepository (FSU 列表)
    → LoginService.isLoggedIn() / getStatus() (状态校验)
    → GetDataService (GET_DATA 查询)
    → GetThresholdService (GET_THRESHOLD 可选查询)
    → SlowDataPollingResult (统计)
```

关键设计决策：
- Scheduler 无业务逻辑，仅触发
- PollingService 不直接调用 FsuServiceClient
- PollingService 不涉及任何 SET 类命令
- 配置与业务分离，Properties 类统一管理

## 协议一致性判断

- 轮询框架只使用 GET_DATA / GET_THRESHOLD 两种已实现的 B接口协议命令
- 不引入自定义私有协议
- 不使用 DSC/RDS
- SOAP/XMLData 路径不变（通过已有 Service 层间接调用）
- 新增的配置项不涉及协议字段

## 修改前论证

### 为什么现在可以做轮询框架

FsuServiceClient 基础设施 (BIF-P3-001)、GET_DATA (BIF-P3-001)、GET_THRESHOLD (BIF-P3-002) 均已闭环。轮询框架只是编排层，不涉及新协议实现。

### 为什么本阶段不启用真实定时轮询

- enabled=false 默认关闭，scheduler-enabled=false 默认不启动
- 仅提供 runOnce() 手动触发能力
- 生产启用需后续阶段确认

### 为什么 SET_THRESHOLD 不能进入定时轮询

SET_THRESHOLD 是高风险配置下发命令，必须有二次确认 + 安全门禁。定时任务无法提供人工确认。

### 轮询框架如何复用已有服务

- PollingService 依赖 GetDataService/GetThresholdService（非直接 FsuServiceClient）
- 与 SetThresholdService、SetThresholdSafetyGate 无调用关系
- 查询和设置在代码层面完全隔离

## 写入前验证

- [x] SlowDataPollingProperties 全部 8 项配置包含默认值
- [x] SlowDataPollingResult 包含 success/scanned/skipped/getData/getThreshold/errors/realCallEnabled
- [x] SlowDataPollingService.runOnce() 可查询 FSU 列表、过滤 OFFLINE、过滤未登录、执行 GET_DATA
- [x] 单个 FSU 失败不影响其他 FSU
- [x] 不返回 null，不抛出未捕获异常
- [x] SlowDataPollingScheduler 默认 disabled（scheduler-enabled=false）
- [x] enabled + scheduler-enabled 双重校验
- [x] 不引用 SetThresholdService / SetThresholdSafetyGate / SetThresholdResult
- [x] application.yml 新增 slow-polling 段
- [x] 测试使用 Stub，不访问网络

## 实际新增/修改文件

### 新增生产文件（4 个）

| 文件 | 说明 |
|------|------|
| `.../service/slow/SlowDataPollingProperties.java` | 配置属性类（8 项配置） |
| `.../service/slow/SlowDataPollingResult.java` | 轮询结果模型（10 统计字段） |
| `.../service/slow/SlowDataPollingService.java` | 轮询编排服务 |
| `.../service/slow/SlowDataPollingScheduler.java` | 定时调度器（默认 disabled） |

### 修改生产文件（1 个）

| 文件 | 说明 |
|------|------|
| `.../resources/application.yml` | 新增 slow-polling 配置段 |

### 新增测试文件（4 个）

| 文件 | 测试数 | 说明 |
|------|--------|------|
| `.../service/slow/SlowDataPollingPropertiesTest.java` | 8 | 配置默认值 + setter |
| `.../service/slow/SlowDataPollingServiceTest.java` | 16 | Service 单元测试 |
| `.../service/slow/SlowDataPollingSchedulerTest.java` | 4 | Scheduler 行为测试 |
| `.../SlowDataPollingIntegrationTest.java` | 10 | 全链路集成测试 |

## 核心改动

### 1. SlowDataPollingProperties

```yaml
b-interface:
  slow-polling:
    enabled: false
    scheduler-enabled: false
    allow-real-call: false
    poll-get-data: true
    poll-get-threshold: false
    interval-seconds: 300
    max-fsu-per-run: 50
```

### 2. SlowDataPollingService.runOnce()

编排流程：
1. 查询所有 FSU 设备 → 2. 检查 onlineStatus → 3. 检查 isLoggedIn → 4. 执行 GET_DATA → 5. 可选执行 GET_THRESHOLD → 6. 返回结构化结果

所有异常捕获并加入 errors，不抛出。

### 3. SlowDataPollingScheduler

双重校验：`properties.isEnabled() && properties.isSchedulerEnabled()`。即使 @Scheduled 触发，配置不允许也立即返回。

## 测试结果

```
[INFO] Tests run: 558, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 是否修改业务代码

- [x] 新增业务代码：SlowDataPollingService / Properties / Result / Scheduler
- [x] 修改配置：application.yml 新增 slow-polling 段
- [x] 不修改已有 Service 层（GetDataService / GetThresholdService / LoginService）
- [x] 不修改已有 Handler 层
- [x] 不修改已有 FsuServiceClient

## 是否涉及数据库/前端/DSC/RDS

| 项目 | 状态 |
|------|------|
| 数据库 | 否 |
| 前端 | 否 |
| DSC/RDS | 否 |
| 真实设备 | 否 |
| 真实 FSU 调用 | 否（allow-real-call=false） |
| Scheduler 启用 | 否（scheduler-enabled=false） |
| SET 类命令 | 否 |

## 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| GET_DATA 无 signalId 列表 | 中 | 需后续 FSU 信号配置阶段 |
| @EnableScheduling 未全局开启 | 低 | scheduler-enabled=false 期间无影响 |
| 轮询结果未持久化 | 低 | 后续可扩展审计 |
| FSU 地址管理未实现 | 低 | serviceUrl 传 null |

## 遗留问题

1. FSU 信号列表配置未实现（当前传空 List，GetDataService 返回 2003）
2. @EnableScheduling 未全局开启
3. 轮询审计/结果持久化未实现
4. FSU 地址发现未实现

## 下一步建议

**BIF-P3-005**：慢数据通道 Signal 配置 — 为每个 FSU 配置轮询的信号列表。

---

## Git diff 摘要

### 新增文件
- `.../service/slow/SlowDataPollingProperties.java` — ~65 行
- `.../service/slow/SlowDataPollingResult.java` — ~110 行
- `.../service/slow/SlowDataPollingService.java` — ~170 行
- `.../service/slow/SlowDataPollingScheduler.java` — ~55 行
- `.../service/slow/SlowDataPollingPropertiesTest.java` — ~85 行，8 测试
- `.../service/slow/SlowDataPollingServiceTest.java` — ~310 行，16 测试
- `.../service/slow/SlowDataPollingSchedulerTest.java` — ~80 行，4 测试
- `.../SlowDataPollingIntegrationTest.java` — ~350 行，10 集成测试

### 修改文件
- `.../resources/application.yml` — +8 行（slow-polling 配置段）
