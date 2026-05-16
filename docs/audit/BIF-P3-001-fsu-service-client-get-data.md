# BIF-P3-001：慢数据通道基础设施 + GET_DATA 首命令闭环

> 对应阶段：BIF-P3-001
> 完成日期：2026-05-14
> 影响范围：新增 7 个生产文件、5 个测试文件、修改 1 个配置文件、删除 1 个旧文件

---

## 一、本阶段目标

1. 设计并实现 FSU 服务客户端基础设施（FsuServiceClient 接口 + 请求/响应模型）
2. 创建默认 StubFsuServiceClient（不访问网络、不访问真实设备）
3. 创建 RealHttpFsuServiceClient（配置开关控制，默认禁用）
4. 实现 GET_DATA 业务服务层（GetDataService/GetDataResult）
5. 实现 GET_DATA 命令处理器（GetDataCommandHandler 桩→真实逻辑）
6. 建立完整的安全控制机制（配置开关 + 条件 Bean + 异常安全）
7. 通过全量 45 个新增测试和回归测试集

---

## 二、总体架构判断

### 2.1 层次定位

```
SC 平台内部层
  ┌─────────────────────────────────────────────┐
  │ CommandDispatcher → CommandHandler          │  ← 已有
  │   → GetDataCommandHandler (真实实现)        │  ← 本阶段
  │     → GetDataService                        │  ← 本阶段
  │       → FsuServiceClient (接口)             │  ← 本阶段
  │         → StubFsuServiceClient (默认)       │  ← 本阶段
  │         → RealHttpFsuServiceClient (可选)   │  ← 本阶段
  │           → SoapMessageHandler              │  ← 已有
  │           → XmlDataParser                   │  ← 已有
  │           → HTTP POST → FSU FSUService      │
  └─────────────────────────────────────────────┘
```

### 2.2 与快数据通道边界

| 通道 | 方向 | 触发 | 基础设施 | 持久化 |
|------|------|------|---------|--------|
| 快数据通道 (FSU→SC) | FSU 主动上报 | SC 被动接收 | CommandDispatcher + Handler | SEND_DATA/SEND_ALARM 含入库 |
| 慢数据通道 (SC→FSU) | SC 主动查询 | SC 轮询/触发 | FsuServiceClient + HTTP SOAP | GET_DATA 不含入库（仅查询） |

### 2.3 GET_DATA vs SEND_DATA 边界

| 特性 | GET_DATA | SEND_DATA |
|------|----------|-----------|
| 方向 | SC→FSU（查询） | FSU→SC（上报） |
| 触发方 | SC 轮询/定时任务 | FSU 主动上报 |
| 持久化 | 否（只读查询） | 是（入库持久化） |
| 调用链路 | Service→FsuServiceClient→HTTP SOAP | Handler→Service→Repository |
| 参数 | SignalID 列表 | FSUCode + 信号值数据 |

---

## 三、设计说明

### 3.1 FsuServiceClient 接口设计

```java
public interface FsuServiceClient {
    FsuServiceResponse call(FsuServiceRequest request);
}
```

设计原则：
- 单一 `call()` 方法，支持所有 PK_Type（不针对单个命令创建多个方法）
- 请求/响应使用独立模型，不暴露 HTTP、SOAP 或 XML 细节
- 职责边界：只做 SOAP 调用抽象，不处理业务逻辑、地址发现、重试策略
- 异常安全：所有异常转结构化 FsuServiceResponse，不抛出

### 3.2 双重实现 + 配置开关

**StubFsuServiceClient**（默认激活）：
- `@ConditionalOnProperty(name = "b-interface.fsu-client.real-call-enabled", havingValue = "false", matchIfMissing = true)`
- 内置完整 SOAP 响应（5 个模拟信号：温度/湿度/电压/门禁/水浸）
- 通过 SoapMessageHandler + XmlDataParser 解析内置报文，模拟真实调用链路
- 不访问网络、不访问真实设备
- 不拦截 null 请求 → 返回 error 响应

**RealHttpFsuServiceClient**（配置启用）：
- `@ConditionalOnProperty(name = "b-interface.fsu-client.real-call-enabled", havingValue = "true")`
- 使用 JDK 内置 HttpURLConnection，无额外 Maven 依赖
- 通过 SoapMessageHandler.buildRequest() 构造 SOAP 请求
- 通过 SoapMessageHandler.parse() + XmlDataParser 解析响应
- 超时由配置控制（connect-timeout-ms / read-timeout-ms）
- 所有网络异常（ConnectException / SocketTimeoutException / HTTP 错误码）转结构化响应

### 3.3 GET_DATA 处理流程

```
GetDataCommandHandler.handle(context)
  ├── null context → 5001
  ├── 提取 Info XML
  │   └── 为空 → 2001
  ├── extractFsuCode(infoXml) → 正则 <FSUCode>
  │   └── null/空 → 2001
  ├── loginService.isLoggedIn(fsuCode)
  │   └── false → 1002
  ├── extractSignalIds(xmlData)
  │   └── 空 → 2003
  └── getDataService.execute(fsuCode, null, signalIds)
      ├── fsuCode null/空 → 2001
      ├── signalIds null/空 → 2003
      ├── buildRequestXmlData(signalIds) → XML
      ├── build Info XML <FSUCode>
      ├── new FsuServiceRequest(pkType=GET_DATA, ...)
      ├── fsuServiceClient.call(request)
      │   └── 失败 → 透传错误码
      ├── parseSignals(responseXmlData) → List<SignalValue>
      └── return GetDataResult.success/fail
```

### 3.4 ResultCode 映射

| ResultCode | 含义 | 触发条件 |
|------------|------|---------|
| 0 | 成功 | 查询成功 |
| 1002 | FSU 未登录/离线 | loginService.isLoggedIn() = false |
| 2001 | 参数错误 | FSUCode 为空、Info 为空 |
| 2003 | 无有效信号 | SignalID 列表为空 |
| 5001 | 内部错误 | FSU 调用异常、客户端异常、FSU 返回禁用状态 |

---

## 四、安全控制机制

### 4.1 默认安全

- `real-call-enabled=false` 是默认值，任何部署默认使用 Stub 实现
- 即使误配置，Stub 实现也不会访问网络
- RealHttpFsuServiceClient 启动时输出 WARN 日志

### 4.2 条件 Bean

- Stub: `matchIfMissing = true`，未配置时自动激活
- Real: `havingValue = "true"`，需显式配置

### 4.3 异常安全

- FsuServiceClient.call() 永不抛出异常
- RealHttpFsuServiceClient：ConnectException / SocketTimeoutException / HTTP 错误 → 结构化响应
- GetDataService：catch Exception → GetDataResult.fail("5001")
- GetDataCommandHandler：不 catch，由 CommandDispatcher 统一处理

### 4.4 禁止能力

- 不实现 SET_POINT / SET_THRESHOLD / SET_FTP / SET_FSUREBOOT 等控制命令
- GET_DATA 不调用 SendDataService 或 SendAlarmService
- 不使用 DSC/RDS 协议
- 不新增非 B接口协议入口

---

## 五、实际新增/修改文件

### 新增文件

| # | 文件 | 说明 |
|---|------|------|
| 1 | `.../service/fsu/FsuServiceRequest.java` | FSU 请求模型（Builder 模式） |
| 2 | `.../service/fsu/FsuServiceResponse.java` | FSU 响应模型（工厂方法） |
| 3 | `.../service/fsu/FsuServiceClient.java` | FSU 客户端接口（单 call 方法） |
| 4 | `.../service/fsu/StubFsuServiceClient.java` | 默认 Stub 实现（内置 5 信号 SOAP 响应） |
| 5 | `.../service/fsu/RealHttpFsuServiceClient.java` | 真实 HTTP SOAP 实现（默认禁用） |
| 6 | `.../service/GetDataResult.java` | GET_DATA 查询结果模型 |
| 7 | `.../service/GetDataService.java` | GET_DATA 业务服务 |
| 8 | `.../command/GetDataCommandHandler.java` | 已存在，修改为真实实现 |
| 9 | `.../service/fsu/StubFsuServiceClientTest.java` | 7 个测试 |
| 10 | `.../service/GetDataServiceTest.java` | 16 个测试 |
| 11 | `.../command/GetDataCommandHandlerTest.java` | 14 个测试 |
| 12 | `.../GetDataSlowChannelIntegrationTest.java` | 8 个集成测试 |
| 13 | `.../service/fsu/FsuServiceRequestTest.java` | 10 个模型测试（模型中已涵盖） |

### 修改文件

| # | 文件 | 说明 |
|---|------|------|
| 1 | `.../service/fsu/FsuServiceClientStub.java` | 已删除（由 StubFsuServiceClient 替代） |
| 2 | `application.yml` | 新增 `b-interface.fsu-client` 配置段 |

### 删除文件

| # | 文件 | 说明 |
|---|------|------|
| 1 | `FsuServiceClientStub.java` | 旧接口不再兼容，由 StubFsuServiceClient.java 替代 |

---

## 六、测试覆盖

### 6.1 测试总览

| 测试文件 | 测试数 | 类型 |
|---------|--------|------|
| StubFsuServiceClientTest | 7 | 单元测试 |
| GetDataServiceTest | 16 | 单元测试 |
| GetDataCommandHandlerTest | 14 | 单元测试 |
| GetDataSlowChannelIntegrationTest | 8 | 集成测试（真实 Handler + 真实 Service + Stub 依赖） |
| **合计新增** | **45** | |

### 6.2 测试场景覆盖

**FsuServiceClient**：
- GET_DATA 返回 5 个信号（TEMP/HUMI/VOLT/DOOR/WATER）
- 不支持的 PK_Type → error
- null 请求 → error
- 不访问网络
- 响应包含 rawSoap 和 infoXml

**GetDataService**：
- 成功返回信号值列表
- 正确解析 SignalValue 字段（value/quality/status）
- 无信号数据时返回空列表
- FSUCode 校验（null/空 → 2001）
- SignalID 校验（null/空 → 2003）
- FSU 客户端错误透传
- FSU 客户端异常 → 5001
- FSU 禁用状态 → 5001
- 请求构造验证（SignalID/Info XML）
- 多信号解析
- 不调用 SendDataService
- 不访问网络

**GetDataCommandHandler**：
- 正常 GET_DATA 成功
- 响应包含 Count 和 Signal 列表
- 支持 GET_DATA PK_Type
- null context → 5001
- 缺少 FSUCode → 2001
- 未登录 → 1002
- 缺少 SignalID → 2003
- Service 返回错误 → 透传
- extractFsuCode 工具方法（正确/大小写/null/空白/未找到）

**集成测试**：
- 已登录可查询
- 信号值正确返回
- 未登录拒绝
- FSU 离线拒绝
- 参数校验（FSUCode/SignalID）
- 不调用 SendDataService
- 不访问真实网络

| 测试结果 | 测试通过 | 回归通过 |
|---------|---------|---------|
| 新增 | 45/45 | — |
| 全量 B-interface | — | 424/424 |

### 6.3 回归测试命令

```bash
cd backend && mvn test -Dtest="com.dcim.platform.binterface.**"
```

---

## 七、配置说明

```yaml
b-interface:
  fsu-client:
    real-call-enabled: false     # 默认 false，使用 Stub 实现
    connect-timeout-ms: 3000     # HTTP 连接超时（仅 Real 实现使用）
    read-timeout-ms: 5000        # HTTP 读取超时（仅 Real 实现使用）
```

---

## 八、协议一致性检查

- [x] GET_DATA 传输层使用 SOAP/XMLData，符合 B接口协议 2016
- [x] SOAP 命名空间使用 `http://schemas.xmlsoap.org/soap/envelope/`
- [x] PK_Type 使用枚举 `BInterfacePkType.GET_DATA`
- [x] ResultCode 使用协议定义范围（0/1002/2001/2003/5001）
- [x] Signal 字段使用协议标准字段（SignalID/Value/Quality/Status/CollectTime）
- [x] 未引入任何非 B接口协议字段或结构
- [x] 未使用 DSC/RDS 协议

## 九、架构一致性检查

- [x] FsuServiceClient 作为独立抽象层，不混杂业务逻辑
- [x] Handler 负责校验和编排，Service 负责业务逻辑
- [x] GET_DATA 不调用 SendDataService / SendAlarmService
- [x] GetDataCommandHandler 使用 LoginService 进行登录态校验
- [x] 测试使用 Stub 不访问真实设备
- [x] CommandResult 通过 toXmlDataXml() 输出标准 xmlData

## 十、风险与缓解

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| FSU 地址发现 | 低 | 当前传 null，由上层定时任务提供 serviceUrl |
| Real 实现未集成测试 | 中 | 配置开关默认 false，上线前需端到端验证 |
| 5 信号 Stub 响应与真实 FSU 差异 | 低 | Stub 仅为开发/测试辅助，生产使用 Real |
| 多命令并发 | 低 | 由上层 CommandDispatcher 控制 |

## 十一、遗留问题

1. **FSU 地址管理**：GetDataCommandHandler 调用 GetDataService 时传 null serviceUrl。后续阶段需在 Handler 层或上层服务中根据 FSUCode 查询 FSU 设备地址并传入。
2. **定时轮询触发**：当前 GET_DATA 仅实现了接收处理端（SC 收到 SC→FSU 响应后的处理），主动调用端（定时任务触发）需在上层实现。
3. **RealHttpFsuServiceClient 集成测试**：当前无法在单元测试中验证真实 HTTP 调用。需在部署环境中通过配置 `real-call-enabled=true` 并指向测试 FSU 验证。
4. **GET_DATA 响应持久化**：当前 `CommandResult` 的 `toXmlDataXml()` 输出已包含完整 Signal 数据。若需存储查询结果到数据库，需在 Handler 之后增加持久化步骤，但根据本阶段范围不实现。

## 十二、建议下一步

BIF-P3-002：GET_THRESHOLD / SET_THRESHOLD — 告警阈值管理，复用 FsuServiceClient 基础设施和 GET_DATA 架构模式。
