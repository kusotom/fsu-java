---
name: BIF-P3-003-set-threshold
description: 实现 SET_THRESHOLD 门限设置闭环，新增 5 生产文件+修改3文件+54新增测试，全量 514 通过，含安全门禁/操作审计/二次确认
metadata:
  type: project
---

# BIF-P3-003：SET_THRESHOLD 门限设置闭环

> 对应阶段：BIF-P3-003
> 完成日期：2026-05-14
> 操作代理：Claude Code
> 影响范围：5 新生产文件 + 3 修改生产文件 + 6 测试文件（+58 测试）

**关联记忆：** [[BIF-P3-002-get-threshold]], [[BIF-P3-001-fsu-service-client-get-data]], [[BIF-P2-002-slow-data-channel-audit]]

---

## 任务目标

实现 SET_THRESHOLD 高风险配置下发命令的安全闭环。核心约束：
- 默认 stub / 默认禁止真实下发
- 必须建立安全门禁（SetThresholdSafetyGate）
- 必须建立操作审计（SetThresholdAuditService）
- 必须建立二次确认（require-confirmation=true）
- 必须实现 4 层安全架构（Handler → SafetyGate → AuditService → Service → Client）
- 所有测试不访问真实网络

## 架构判断

安全闭环架构（4 层安全控制）：

```
SetThresholdCommandHandler (编排层)
  → SetThresholdSafetyGate (第一道门禁: enabled + confirmed)
  → SetThresholdAuditService (操作审计: 全量记录)
  → SetThresholdService (业务逻辑: 请求构造/响应解析)
    → FsuServiceClient (接口抽象)
```

与 GET_THRESHOLD 的关键区别：
- GET_THRESHOLD：只读查询，无安全控制
- SET_THRESHOLD：高风险配置，必须有门禁 + 审计 + 确认

## 协议一致性判断

- SET_THRESHOLD 请求使用 `<Signal>` 结构（非 GET_THRESHOLD 的 `<SignalID>` 重复节点）
- 门限字段（AlarmUpper / AlarmLower / AlarmUpperUrgent / AlarmLowerUrgent）与 GET_THRESHOLD 一致
- 响应只有 ResultCode + Count，xmlData 为空 — 符合 B接口协议 2016
- PK_Type 使用枚举 BInterfacePkType.SET_THRESHOLD
- 未引入非 B接口协议字段或结构
- 未使用 DSC/RDS 协议

## 修改前论证

### 为什么 SET_THRESHOLD 需要独立安全架构

- SET_THRESHOLD 修改 FSU 告警门限，直接影响动环监控的告警判定
- 错误设置可能导致大量误告警或漏告警
- 安全审计要求所有高风险操作可追溯
- 二次确认防止误操作（尤其是批量下发时）

### 为什么默认 disabled

- 安全第一原则：高风险命令默认关闭，需要显式开启
- 项目当前为开发阶段，真实设备未接入
- 配置管理：`enabled: false` + `require-confirmation: true` 均为保守默认值

### 4 层安全设计的原因

1. 门禁层（SafetyGate）：快速拒绝，避免不必要的 Service 调用
2. 审计层（AuditService）：完整记录操作上下文，即使被拒绝也记录
3. 服务层（SetThresholdService）：专注业务逻辑，不关心安全策略
4. 编排层（Handler）：组合各层，单一职责

### 与 GET_THRESHOLD 的边界

- SET_THRESHOLD 有自己的 Service、Handler、Result，不与 GET_THRESHOLD 共用
- SET_THRESHOLD 的 FsuServiceClient 调用只设置，不查询
- StubFsuServiceClient 中 SET_THRESHOLD 与 GET_THRESHOLD 是独立的 switch case

## 写入前验证

- [x] SetThresholdResult 模型包含 success/resultCode/resultDesc/fsuCode/count/errors
- [x] SetThresholdSafetyGate 支持 enabled + requireConfirmation 双条件判断
- [x] SetThresholdAuditRecord Builder 包含所有必填字段（operationId/commandType/fsuCode/signalId/requestTime）
- [x] SetThresholdSafetyGate enabled=false → 3001，requireConfirmation=true + 未确认 → 3002
- [x] SetThresholdService 校验 FSUCode/SignalID 空值
- [x] SetThresholdService 使用 FsuServiceClient.call() 进行 SOAP 调用
- [x] SetThresholdService 解析 Count 字段
- [x] SetThresholdCommandHandler 拒绝后仍记录审计
- [x] SetThresholdCommandHandler 不访问 HTTP
- [x] 所有测试使用 Stub 模拟，不访问真实网络
- [x] GET_THRESHOLD 未被修改（边界完整）
- [x] application.yml 新增 `set-threshold.enabled: false`

## 实际新增/修改文件

### 新增生产文件（5 个）

| 文件 | 说明 |
|------|------|
| `.../service/SetThresholdResult.java` | 设置结果模型（success/fail 工厂方法 + count） |
| `.../service/SetThresholdSafetyDecision.java` | 安全门禁决策模型（allow/deny 工厂方法） |
| `.../service/SetThresholdAuditRecord.java` | 审计记录模型（Builder 模式，13 字段） |
| `.../service/SetThresholdAuditService.java` | 审计服务（begin/record/getRecords/clear） |
| `.../service/SetThresholdSafetyGate.java` | 安全门禁（enabled + requireConfirmation 配置驱动） |
| `.../service/SetThresholdService.java` | 设置业务服务（请求构造/响应解析/异常处理） |

### 修改生产文件（3 个）

| 文件 | 说明 |
|------|------|
| `.../command/SetThresholdCommandHandler.java` | 从 notImplemented 桩重写为 4 层安全闭环 |
| `.../service/fsu/StubFsuServiceClient.java` | 新增 SET_THRESHOLD case + handleSetThreshold() |
| `.../resources/application.yml` | 新增 set-threshold.enabled + require-confirmation |

### 新增测试文件（6 个）

| 文件 | 测试数 | 说明 |
|------|--------|------|
| `.../service/SetThresholdServiceTest.java` | 16 | Service 单元测试 |
| `.../service/SetThresholdSafetyGateTest.java` | 6 | 安全门禁单元测试 |
| `.../service/SetThresholdAuditServiceTest.java` | 7 | 审计服务单元测试 |
| `.../command/SetThresholdCommandHandlerTest.java` | 15 | Handler 单元测试 |
| `.../SetThresholdSlowChannelIntegrationTest.java` | 10 | 全链路集成测试 |
| `.../service/fsu/StubFsuServiceClientTest.java` | +4 | 补充 SET_THRESHOLD 4 测试 |

## 核心改动

### 1. SetThresholdCommandHandler 从桩到安全闭环

旧：`return CommandResult.notImplemented(BInterfacePkType.SET_THRESHOLD);`

新：安全编排 — 参数校验 → 登录态校验 → 安全门禁 → 开始审计 → 调用 Service → 完成审计 → 构造结果

### 2. 安全门禁模式

```java
public SetThresholdSafetyDecision check(String fsuCode, boolean confirmed) {
    if (!enabled) return deny("3001", "SET_THRESHOLD 功能未启用");
    if (requireConfirmation && !confirmed) return deny("3002", "缺少二次确认");
    return allow();
}
```

### 3. 审计记录模式

```java
SetThresholdAuditRecord.Builder builder = auditService.begin();
// ... 设置业务字段 ...
SetThresholdAuditRecord record = builder.build();
auditService.record(record);
```

失败时安全关审计：

```java
auditService.record(builder.resultCode("3001").success(false).build());
```

### 4. StubFsuServiceClient 扩展

新增 `DEFAULT_SET_THRESHOLD_RESPONSE` 常量 + `handleSetThreshold()` + `case SET_THRESHOLD`。

## 测试结果

```
[INFO] Tests run: 514, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| 审计数据内存存储 | 中 | 后续改为数据库持久化 |
| 门限字段为 String | 低 | XML 解析天然为 String |
| Stub 响应固定 | 低 | 仅开发/测试使用 |
| FSU 地址管理未实现 | 低 | serviceUrl 传 null |
| 无操作回滚 | 中 | SET_THRESHOLD 不可撤销 |

## 遗留问题

1. 审计数据持久化（当前内存存储，重启丢失）
2. FSU 地址发现未实现
3. 批量门限设置（当前一次一个信号）
4. SET_POINT / SET_FTP / SET_FSUREBOOT 未实现
5. 操作回滚机制未实现

## 下一步建议

**BIF-P3-004**：慢数据通道定时轮询 — 实现 GET_DATA / GET_THRESHOLD 定时轮询触发。

---

## Git diff 摘要

### 新增文件
- `.../service/SetThresholdResult.java` — ~80 行，结果模型
- `.../service/SetThresholdSafetyDecision.java` — ~65 行，安全决策模型
- `.../service/SetThresholdAuditRecord.java` — ~120 行，审计记录 Builder
- `.../service/SetThresholdAuditService.java` — ~70 行，审计服务
- `.../service/SetThresholdSafetyGate.java` — ~60 行，安全门禁
- `.../service/SetThresholdService.java` — ~160 行，业务服务
- `.../service/SetThresholdServiceTest.java` — ~260 行，16 测试
- `.../service/SetThresholdSafetyGateTest.java` — ~120 行，6 测试
- `.../service/SetThresholdAuditServiceTest.java` — ~140 行，7 测试
- `.../command/SetThresholdCommandHandlerTest.java` — ~275 行，15 测试
- `.../SetThresholdSlowChannelIntegrationTest.java` — ~320 行，10 集成测试

### 修改文件
- `.../command/SetThresholdCommandHandler.java` — ~200 行完整重写
- `.../service/fsu/StubFsuServiceClient.java` — +~90 行（内置响应 + switch case + 处理方法）
- `.../resources/application.yml` — +3 行（set-threshold 配置）
- `.../service/fsu/StubFsuServiceClientTest.java` — +~70 行（4 个 SET_THRESHOLD 测试）
