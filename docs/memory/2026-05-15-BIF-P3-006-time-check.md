---
name: BIF-P3-006-time-check
description: TIME_CHECK 时间同步闭环，新增 TimeCheckService/TimeCheckResult，Stub→Real Handler，新增 39 测试，全量 610 通过
metadata:
  type: project
---

# BIF-P3-006：TIME_CHECK 时间同步闭环

> 对应阶段：BIF-P3-006
> 完成日期：2026-05-15
> 操作代理：Claude Code
> 影响范围：2 新生产文件 + 2 修改生产文件 + 2 测试文件（+39 测试）

**关联记忆：** [[BIF-P3-005-signal-polling-target]], [[BIF-P3-001-fsu-service-client-get-data]], [[BIF-P0-004-command-dispatcher]]

---

## 任务目标

将 TimeCheckCommandHandler 从桩（notImplemented）替换为真实实现，建立 TIME_CHECK 完整的 CommandHandler→Service→FsuServiceClient 三层架构。核心约束：
- 不修改系统时间，不修改 FSU 时间
- 不访问真实设备（Stub 模式）
- 不集成到慢数据轮询
- 不新增数据库表

## 架构判断

```
TimeCheckCommandHandler
  → LoginService.isLoggedIn(fsuCode)  [校验登录态]
  → TimeCheckService.execute(fsuCode, null, standardTime)
    → FsuServiceClient.call(TIME_CHECK request)
    → parse FSUTime from response Info
  → CommandResult(Info: ResultCode + FSUTime)
```

与 HEARTBEAT 的区别：HEARTBEAT 仅更新本地状态，TIME_CHECK 需要调 FSU 查时间。

## 协议一致性判断

- 完全符合 B接口协议 2016 TIME_CHECK 定义
- 请求：FSUCode + StandardTime 在 Info，无 xmlData
- 响应：ResultCode + FSUTime 在 Info，无 xmlData
- 未自造字段，未扩展协议

## 修改前论证

### 为什么 TIME_CHECK 是独立闭环

TIME_CHECK 不同于 GET_DATA/GET_THRESHOLD：它不需要信号 ID 列表，不涉及 xmlData，不属于轮询类命令。SC 按需发起时间同步校验。

### 如何复用现有基础设施

- BInterfacePkType.TIME_CHECK 枚举已存在
- CommandDispatcher 已注册 TIME_CHECK 路由
- FsuServiceClient 接口已存在（可扩展）
- SoapMessageHandler/XmlDataParser 可复用
- LoginService 可直接注入校验登录态

### 与 HEARTBEAT 和服务端时间同步区分

HEARTBEAT 是 FSU→SC 的心跳上报（含 ServerTime），TIME_CHECK 是 SC→FSU 的时间查询（含 FSUTime），方向不同，不可混用。

## 写入前验证

- [x] TimeCheckResult 模型含 success/resultCode/resultDesc/fsuCode/fsuTime/standardTime
- [x] TimeCheckService 通过 FsuServiceClient 调用 FSU
- [x] StubFsuServiceClient 新增 TIME_CHECK 分支 + 默认 SOAP 响应
- [x] TimeCheckCommandHandler 提取 FSUCode + StandardTime，校验登录，调 Service
- [x] 成功响应 Info 含 ResultCode + FSUTime
- [x] 异常/空值返回对应错误码
- [x] 不修改系统时间
- [x] 不集成到慢数据轮询
- [x] extractStandardTime 正则兼容大小写/空白
- [x] extractFsuTime 正则兼容大小写/空白

## 新增/修改文件

### 新增生产文件（2 个）

| 文件 | 说明 |
|------|------|
| `.../service/TimeCheckResult.java` | 时间同步结果模型（7 字段） |
| `.../service/TimeCheckService.java` | 时间同步服务（execute/extractFsuTime） |

### 修改生产文件（2 个）

| 文件 | 说明 |
|------|------|
| `.../command/TimeCheckCommandHandler.java` | 桩→真实实现（注入 LoginService + TimeCheckService） |
| `.../service/fsu/StubFsuServiceClient.java` | 新增 TIME_CHECK 分支 + DEFAULT_TIME_CHECK_RESPONSE |

### 新增测试（2 个）

| 文件 | 测试数 | 说明 |
|------|--------|------|
| `.../service/TimeCheckServiceTest.java` | 20 | 服务单元测试 |
| `.../command/TimeCheckCommandHandlerTest.java` | 19 | Handler 单元测试 |

## 核心改动

### 1. TimeCheckResult + TimeCheckService

新增两个文件，遵循 GetDataResult/GetDataService 的 patterns。

### 2. TimeCheckCommandHandler 重构

原：`return CommandResult.notImplemented(TIME_CHECK)`  → 返回 ResultCode=1
新：提取 FSUCode+StandardTime → 校验登录 → 调 TimeCheckService → 构建响应

### 3. StubFsuServiceClient 扩展

- `DEFAULT_TIME_CHECK_RESPONSE`：硬编码 SOAP 响应（FSUTime=2026-05-13T10:42:01+08:00）
- `handleTimeCheck()`：解析并返回预设响应

## 测试结果

```
[INFO] Tests run: 610, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

注：DcimPlatformApplicationTests.contextLoads 因预存的 BInterfaceMessageLogRepository 重复 Bean 问题失败，与本阶段无关。

## 是否修改业务代码等项目

| 项目 | 状态 |
|------|------|
| 修改业务代码 | 是（TimeCheckCommandHandler 重构） |
| 新增业务代码 | 是（TimeCheckResult + TimeCheckService + Stub 扩展） |
| 涉及数据库 | 否 |
| 涉及前端 | 否 |
| 涉及 DSC/RDS | 否 |
| 访问真实设备 | 否（Stub 模式） |
| 启用 Scheduler/轮询 | 否 |
| 修改系统时间 | 否 |
| 修改 FSU 时间 | 否 |

## 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| StubFSU 返回固定 FSUTime | 低 | Stub 模式，不访问真实设备 |
| FSUTime 解析依赖正则 | 低 | 与 extractFsuCode 模式一致 |
| 不验证 SessionID | 低 | 当前 INFO 不传 SessionID 到 FSU |

## 遗留问题

1. 不集成到慢数据轮询（TIME_CHECK 非轮询类命令）
2. 不计算时间偏移量（由调用方计算）
3. 不自动校准系统时间
4. StubFSU 不模拟 FSUTime 变化

## 下一步建议

**BIF-P4-001**：慢数据通道启用确认 — 评估产出、确认配置、考虑是否在开发环境启用轮询。

---

## Git diff 摘要

### 新增文件
- `.../service/TimeCheckResult.java` — ~80 行
- `.../service/TimeCheckService.java` — ~130 行
- `.../service/TimeCheckServiceTest.java` — ~250 行，20 测试
- `.../command/TimeCheckCommandHandlerTest.java` — ~240 行，19 测试

### 修改文件
- `.../command/TimeCheckCommandHandler.java` — ~180 行完全重写（桩→真实）
- `.../service/fsu/StubFsuServiceClient.java` — 新增 TIME_CHECK 响应常量 + handleTimeCheck + extractFsuTime（+~55 行）
