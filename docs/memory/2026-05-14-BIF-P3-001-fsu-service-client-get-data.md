---
name: BIF-P3-001-fsu-service-client-get-data
description: 新建 FSU SOAP 客户端基础设施（FsuServiceClient/Stub/Real），实现 GET_DATA 首命令闭环，新增 45 测试全量 424 通过
metadata:
  type: project
---

# BIF-P3-001：慢数据通道基础设施 + GET_DATA 首命令闭环

> 对应阶段：BIF-P3-001
> 完成日期：2026-05-14
> 操作代理：Claude Code
> 影响范围：7 新生产文件 + 5 新测试文件 + 1 配置修改 + 1 旧文件删除

**关联记忆：** [[BIF-P2-002-slow-data-channel-audit]], [[BIF-P1-005-main-flow-integration]], [[BIF-P2-001-offline-detection]]

---

## 任务目标

1. 设计 FSU SOAP 客户端抽象层（FsuServiceClient 接口 + 请求/响应模型）
2. 创建默认 StubFsuServiceClient（内置 5 信号响应，不访问网络）
3. 创建 RealHttpFsuServiceClient（JDK HttpURLConnection，配置开关控制，默认禁用）
4. 实现 GET_DATA 服务层（GetDataService/GetDataResult）
5. 实现 GET_DATA 命令处理器（GetDataCommandHandler 桩→真实逻辑）
6. 建立配置开关 + 条件 Bean + 异常安全机制
7. 新增 45 测试，回归 424 全通过

## 架构判断

涉及层次（从顶到底）：
- CommandHandler 层：GetDataCommandHandler（校验/编排）
- Service 层：GetDataService（业务逻辑/请求构造/响应解析）
- 客户端抽象层：FsuServiceClient 接口
- 实现层：StubFsuServiceClient（默认）/ RealHttpFsuServiceClient（可选）
- SOAP/XML 层：SoapMessageHandler / XmlDataParser（已有）

严格遵循 SC→FSU 慢数据通道职责划分：Handler 不做 HTTP 调用，Service 不做地址发现，Client 不做业务逻辑。

## 协议一致性判断

- GET_DATA 的 SOAP 报文结构符合 B接口协议 2016 SCService 规范
- PK_Type=GET_DATA，Signal 字段使用协议标准名称
- ResultCode 使用协议定义范围
- 未引入非 B接口协议字段或结构
- 未使用 DSC/RDS 协议

## 修改前论证

### 方案选择：单方法 vs 多方法接口

| 方案 | 优点 | 缺点 |
|------|------|------|
| 多方法（login/heartbeat/getData...） | 类型安全，IDE 友好 | 每新命令加方法，接口膨胀，与 B接口 PK_Type 枚举脱节 |
| 单方法 call(FsuServiceRequest) | 与 PK_Type 一一对应，扩展性好 | 需 Builder 模式辅助构造 |

**结论**：采用单方法 `call(FsuServiceRequest)`，与 B接口 PK_Type 设计思想一致。

### 方案选择：Stub 内置响应 vs 文件读取

| 方案 | 优点 | 缺点 |
|------|------|------|
| 内置静态字符串 | 无文件依赖，Bean 初始化无副作用 | 字符串过长 |
| 从 classpath 读取 fixture 文件 | 外部化可编辑 | 测试 fixture 不在运行时 classpath，需额外配置 |

**结论**：采用内置静态字符串（已在审计中确认测试 fixture 不在运行时 classpath）。

## 写入前验证

- [x] FsuServiceClient 接口只包含一个 call() 方法
- [x] FsuServiceRequest 使用 Builder 模式，构造时校验非空
- [x] FsuServiceResponse 提供 success/fail/error/disabled 工厂方法
- [x] StubFsuServiceClient 标注 @ConditionalOnProperty(matchIfMissing=true)
- [x] RealHttpFsuServiceClient 标注 @ConditionalOnProperty(havingValue="true")
- [x] GetDataService 校验 FSUCode/SignalID 空值
- [x] GetDataService 不调用 SendDataService/SendAlarmService
- [x] GetDataCommandHandler 调用 LoginService.isLoggedIn() 校验登录态
- [x] GetDataCommandHandler 不做 HTTP 调用
- [x] 所有测试不访问真实网络
- [x] 旧 FsuServiceClientStub 已删除

## 实际新增/修改文件

### 新增生产文件（7 个）

| 文件 | 行数 | 说明 |
|------|------|------|
| `.../service/fsu/FsuServiceRequest.java` | ~90 | 请求模型 + Builder |
| `.../service/fsu/FsuServiceResponse.java` | ~120 | 响应模型 + 工厂方法 |
| `.../service/fsu/FsuServiceClient.java` | ~30 | 接口定义 |
| `.../service/fsu/StubFsuServiceClient.java` | ~128 | 默认 Stub（内置 SOAP 响应） |
| `.../service/fsu/RealHttpFsuServiceClient.java` | ~155 | 真实 HTTP SOAP 实现 |
| `.../service/GetDataResult.java` | ~100 | 查询结果 + SignalValue 内部类 |
| `.../service/GetDataService.java` | ~160 | GET_DATA 业务服务 |

### 修改生产文件（1 个）

| 文件 | 说明 |
|------|------|
| `.../command/GetDataCommandHandler.java` | 从 stub 返回 notImplemented 改为真实逻辑 |
| `application.yml` | 新增 `b-interface.fsu-client` 配置段 |

### 删除文件（1 个）

| 文件 | 说明 |
|------|------|
| `.../service/fsu/FsuServiceClientStub.java` | 旧接口不再兼容 |

### 新增测试文件（5 个）

| 文件 | 测试数 | 说明 |
|------|--------|------|
| `.../service/fsu/StubFsuServiceClientTest.java` | 7 | Stub 客户端单元测试 |
| `.../service/GetDataServiceTest.java` | 16 | GET_DATA 服务单元测试 |
| `.../command/GetDataCommandHandlerTest.java` | 14 | Handler 单元测试 |
| `.../GetDataSlowChannelIntegrationTest.java` | 8 | 全链路集成测试 |

## 核心改动

### 1. FsuServiceClient 重新设计

```
旧接口（多方法）：
  login(fsuCode), heartbeat(fsuCode), getData(fsuCode, pointIds), ...

新接口（单方法）：
  call(FsuServiceRequest) → FsuServiceResponse
```

### 2. GET_DATA 处理从桩到真实

旧：`return CommandResult.notImplemented(BInterfacePkType.GET_DATA)`

新：完整处理链路 — 校验参数 → 校验登录态 → 提取 SignalID → 调用 GetDataService → 构造 CommandResult

### 3. 配置安全控制

- `real-call-enabled: false` 为默认值
- Stub 实现 `matchIfMissing=true`，未配置时自动激活
- Real 实现需显式设置 `havingValue="true"` 启用

## 测试结果

```
[INFO] Tests run: 424, Failures: 0, Errors: 0, Skipped: 0
```

## 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| FSU 地址管理未实现 | 低 | serviceUrl 传 null，由后续阶段补充 |
| Real 实现在测试中未覆盖 | 中 | 配置开关 + 条件 Bean 隔离，部署前人工验证 |
| Stub 响应与真实 FSU 差异 | 低 | 仅开发和测试使用，生产切换 Real |
| 定时轮询触发未实现 | 低 | 本阶段范围仅命令处理，不含触发器 |

## 遗留问题

1. FSU 地址发现 — 当前 GetDataService.execute() 的 serviceUrl 传 null，需上层根据 FSUCode 查询设备地址
2. GET_DATA 定时轮询 — 需在 CommandDispatcher 上层实现定时任务调度
3. Real 实现集成测试 — 需部署环境配置 real-call-enabled=true 验证
4. 多余信号数据持久化 — 若需存储查询结果，需在 Handler 后增加持久化步骤

## 下一步建议

**BIF-P3-002**：GET_THRESHOLD / SET_THRESHOLD — 告警阈值管理，复用 FsuServiceClient 和当前架构模式。
