---
name: BIF-P3-002-get-threshold
description: 基于 FsuServiceClient 基础设施实现 GET_THRESHOLD 查询闭环，新增 2 生产文件 + 修改 2 文件 + 41 新增测试，全量 460 通过
metadata:
  type: project
---

# BIF-P3-002：GET_THRESHOLD 门限查询闭环

> 对应阶段：BIF-P3-002
> 完成日期：2026-05-14
> 操作代理：Claude Code
> 影响范围：2 新生产文件 + 2 修改生产文件 + 4 测试文件（+41 测试）

**关联记忆：** [[BIF-P3-001-fsu-service-client-get-data]], [[BIF-P2-002-slow-data-channel-audit]]

---

## 任务目标

基于 BIF-P3-001 FsuServiceClient 慢数据通道基础设施，实现 GET_THRESHOLD 查询类命令闭环：
- 实现 GetThresholdService / GetThresholdResult
- 扩展 StubFsuServiceClient 支持 GET_THRESHOLD
- 改造 GetThresholdCommandHandler 桩→真实逻辑
- 新增 41 测试，回归 460 全通过

## 架构判断

涉及层次与 GET_DATA 完全一致：
- CommandHandler 层：GetThresholdCommandHandler（校验/编排）
- Service 层：GetThresholdService（业务逻辑，请求构造/响应解析）
- 客户端抽象层：FsuServiceClient 单方法接口
- 实现层：StubFsuServiceClient（扩展）/ RealHttpFsuServiceClient（通用）

严格遵循 Handler→Service→Client 三层分离，与 GET_DATA 架构模式完全一致。

## 协议一致性判断

- GET_THRESHOLD 响应字段（AlarmUpper / AlarmLower / AlarmUpperUrgent / AlarmLowerUrgent）严格遵循 fixtures 和 expected JSON
- Signal 结构使用 `<SignalID>` + 门限字段，符合 B接口协议 2016
- 请求使用 `<SignalID>` 重复叶子节点，与 fixtures 一致
- PK_Type 使用枚举 BInterfacePkType.GET_THRESHOLD
- 未引入非 B接口协议字段或结构
- 未使用 DSC/RDS 协议

## 修改前论证

### 为什么 GET_THRESHOLD 应先于 SET_THRESHOLD

- GET_THRESHOLD 是只读查询，风险等级低，适合作为门限相关功能的第一个闭环命令
- 通过 GET_THRESHOLD 可以验证门限协议字段结构（AlarmUpper/AlarmLower 等），为 SET_THRESHOLD 提供准确的协议参考
- 先查后设是自然的业务顺序

### 为什么本阶段不能实现 SET_THRESHOLD

- SET_THRESHOLD 是高风险配置类命令（修改 FSU 告警门限），必须有二次确认、操作审计、安全门禁
- 混写查询和设置会导致屎山代码：校验逻辑、安全控制、审计逻辑会纠缠在一起
- 按照 BIF-P2-002 审计结论，SET_THRESHOLD 定级为 P1 中高风险，需单独一个阶段实现

### GET_THRESHOLD 如何复用 FsuServiceClient

- FsuServiceClient 接口是通用的单 `call(FsuServiceRequest)` 方法
- GET_THRESHOLD 只需构建 pkType=GET_THRESHOLD 的请求
- StubFsuServiceClient 按 PK_Type switch 分发
- RealHttpFsuServiceClient 的通用 HTTP SOAP 调用无需修改

### 如何确保默认不访问真实设备

- `real-call-enabled=false` 未改动
- StubFsuServiceClient 的 @ConditionalOnProperty(matchIfMissing=true) 未改动
- 所有测试使用 Stub 或独立的 StubFsuServiceClient 内部类
- 新增测试验证 `isRealCall()` = false

### 如何避免查询类和控制类混在一起

- GET_THRESHOLD 和 SET_THRESHOLD 有独立的 Handler、Service、Result 类
- GetThresholdService 不做任何写入操作
- GetThresholdCommandHandler 不注册 SET_THRESHOLD PK_Type
- SetThresholdCommandHandler 保持 notImplemented 桩不变

## 写入前验证

- [x] GetThresholdResult 模型包含完整的门限字段（SignalID, AlarmUpper, AlarmLower, AlarmUpperUrgent, AlarmLowerUrgent）
- [x] GetThresholdService 使用 FsuServiceClient.call() 进行 SOAP 调用
- [x] GetThresholdService 校验 FSUCode/SignalID 空值
- [x] GetThresholdService 不修改门限
- [x] StubFsuServiceClient 内置 3 信号 GET_THRESHOLD SOAP 响应
- [x] GetThresholdCommandHandler 调用 LoginService.isLoggedIn() 校验登录态
- [x] GetThresholdCommandHandler 不做 HTTP 调用
- [x] SetThresholdCommandHandler 未被修改
- [x] 所有测试不访问真实网络

## 实际新增/修改文件

### 新增生产文件（2 个）

| 文件 | 说明 |
|------|------|
| `.../service/GetThresholdResult.java` | 门限查询结果 + ThresholdValue 内部类（SignalID + 4 门限字段） |
| `.../service/GetThresholdService.java` | GET_THRESHOLD 业务服务（请求构造/响应解析/异常处理） |

### 修改生产文件（2 个）

| 文件 | 说明 |
|------|------|
| `.../service/fsu/StubFsuServiceClient.java` | 新增 DEFAULT_GET_THRESHOLD_RESPONSE + handleGetThreshold() |
| `.../command/GetThresholdCommandHandler.java` | 从 notImplemented 桩改为真实查询逻辑 |

### 新增测试文件（4 个）

| 文件 | 测试数 | 说明 |
|------|--------|------|
| `.../service/GetThresholdServiceTest.java` | 15 | Service 单元测试 |
| `.../command/GetThresholdCommandHandlerTest.java` | 14 | Handler 单元测试 |
| `.../GetThresholdSlowChannelIntegrationTest.java` | 8 | 全链路集成测试 |
| `.../service/fsu/StubFsuServiceClientTest.java` | +4 | 补充 GET_THRESHOLD 4 测试 |

## 核心改动

### 1. GetThresholdCommandHandler 从桩到真实

旧：
```java
return CommandResult.notImplemented(BInterfacePkType.GET_THRESHOLD);
```

新：完整处理链路 — 校验参数 → 校验登录态 → 提取 SignalID → 调用 GetThresholdService → 构造 CommandResult

### 2. StubFsuServiceClient 扩展

新增 `DEFAULT_GET_THRESHOLD_RESPONSE` 常量（3 信号 SOAP 响应）+ `handleGetThreshold()` 方法 + `case GET_THRESHOLD` 分支。

### 3. GetThresholdService / GetThresholdResult

遵循与 GetDataService/GetDataResult 完全相同的模式：
- 工厂方法：success() / fail()
- 解析方法：parseThresholds() 从 XmlDataModel 提取 ThresholdValue
- 请求构造：buildRequestXmlData() 生成 SignalID XML

## 测试结果

```
[INFO] Tests run: 460, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| AlarmUpper 等字段为 String | 低 | XML 解析天然为 String |
| Stub 门限值固定 | 低 | 仅开发/测试使用 |
| FSU 地址管理未实现 | 低 | 由后续阶段补充 |

## 遗留问题

1. SET_THRESHOLD 未实现（需 BIF-P3-003 独立阶段）
2. 门限数据持久化未实现（当前仅查询不存储）
3. FSU 地址发现未实现
4. GET_THRESHOLD 定时轮询触发未实现

## 下一步建议

**BIF-P3-003**：SET_THRESHOLD 门限设置命令（高风险，需二次确认 + 操作审计 + 安全门禁）。

---

## Git diff 摘要

### 新增文件
- `.../service/GetThresholdResult.java` — ~95 行，结果模型
- `.../service/GetThresholdService.java` — ~160 行，业务服务
- `.../service/GetThresholdServiceTest.java` — ~240 行，15 测试
- `.../command/GetThresholdCommandHandlerTest.java` — ~225 行，14 测试
- `.../GetThresholdSlowChannelIntegrationTest.java` — ~300 行，8 集成测试

### 修改文件
- `.../service/fsu/StubFsuServiceClient.java` — +~80 行（内置响应 + switch case + 处理方法）
- `.../command/GetThresholdCommandHandler.java` — 从 25 行桩重写为 ~190 行真实实现
- `.../service/fsu/StubFsuServiceClientTest.java` — +~70 行（4 个 GET_THRESHOLD 测试）
