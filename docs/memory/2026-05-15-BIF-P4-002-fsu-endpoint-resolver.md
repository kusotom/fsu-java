---
name: bif-p4-002-fsu-endpoint-resolver
description: FSU 地址发现 / ServiceUrl 管理 — 新增 FsuEndpointResolver/FsuEndpointResult/FsuServiceEndpointService，修改 6 个 Service 注入层次
metadata:
  type: project
---

## 任务编号
BIF-P4-002

## 任务名称
FSU 地址发现 / ServiceUrl 管理

## 操作时间
2026-05-15

## 操作代理
Claude Code

## 本次任务目标
补齐真实 FSU 联调前的关键前置能力：FSU 地址发现与 ServiceUrl 管理。基于 FsuDeviceEntity 已有的 ipAddr/port 字段，构建 endpoint 解析层，使 6 个慢数据 Service 在未显式传入 serviceUrl 时能从数据库自动解析。

## 规则加载确认
- [x] AGENTS.md
- [x] CLAUDE.md
- [x] docs/PROJECT_ENGINEERING_RULES.md（文件不存在，未加载）
- [x] docs/memory/README.md
- [x] BIF-P3-001 ~ BIF-P4-001 全部审计文档

## 总体架构判断
```
FsuEndpointResolver (接口)
  └── FsuServiceEndpointService (实现)
       └── FsuDeviceRepository → FsuDeviceEntity.ipAddr + port
       └── 默认路径: /services/FSUService
       └── URL 格式校验
       → 被 6 个 Service 注入使用

Service 变更:
  GetDataService          → +FsuEndpointResolver + 自动解析
  GetThresholdService     → +FsuEndpointResolver + 自动解析
  TimeCheckService        → +FsuEndpointResolver + 自动解析
  GetLoginInfoService     → +FsuEndpointResolver + 自动解析
  GetFtpService           → +FsuEndpointResolver + 自动解析
  SetThresholdService     → +FsuEndpointResolver + 自动解析
```

## B接口协议一致性判断
无协议变更。endpoint 解析仅在 serviceUrl 为 null 时触发，不改变 B接口 SOAP/XMLData 消息结构。

## 修改前论证
- 为什么需要 FSU 地址发现：所有慢数据 Service 的 execute() 参数 fsuServiceUrl 当前均为 null，导致 RealHttpFsuServiceClient 返回 "2001: FSU 服务地址为空"
- 为什么复用 FsuDeviceEntity：已有 ipAddr（String, length 45）+ port（Integer）字段
- 为什么不新建 serviceUrl 字段：ip+port+默认路径已满足 MVP，未来可在实体上扩展
- 为什么所有 6 个 Service 都改：统一入口，避免遗漏

## 写入前验证
- [x] FsuEndpointResult 模型工厂方法正确
- [x] FsuEndpointResolver 接口定义清晰
- [x] FsuServiceEndpointService 正确使用 FsuDeviceRepository
- [x] 优先级逻辑：显式 URL > ip+port > 错误
- [x] URL 格式校验正确
- [x] 所有 6 个 Service 注入 FsuEndpointResolver
- [x] 所有已有测试添加 null resolver 参数

## 实际新增/修改/删除文件

### 新增（4 个）
| 文件 | 说明 |
|------|------|
| `backend/src/main/.../fsu/FsuEndpointResult.java` | Endpoint 解析结果模型 |
| `backend/src/main/.../fsu/FsuEndpointResolver.java` | Endpoint 解析器接口 |
| `backend/src/main/.../fsu/FsuServiceEndpointService.java` | 基于 Repository 的实现 |
| `backend/src/test/.../fsu/FsuServiceEndpointServiceTest.java` | 24 个测试 |

### 修改（22 个）
| 文件 | 说明 |
|------|------|
| `backend/src/main/.../service/GetDataService.java` | +FsuEndpointResolver 注入 + 自动解析 |
| `backend/src/main/.../service/GetThresholdService.java` | 同上 |
| `backend/src/main/.../service/TimeCheckService.java` | 同上 |
| `backend/src/main/.../service/GetLoginInfoService.java` | 同上 |
| `backend/src/main/.../service/GetFtpService.java` | 同上 |
| `backend/src/main/.../service/SetThresholdService.java` | 同上 |
| `backend/src/test/.../service/GetDataServiceTest.java` | resolver=null 构造参数 |
| `backend/src/test/.../service/GetThresholdServiceTest.java` | 同上 |
| `backend/src/test/.../service/TimeCheckServiceTest.java` | 同上 |
| `backend/src/test/.../service/GetLoginInfoServiceTest.java` | 同上 |
| `backend/src/test/.../service/GetFtpServiceTest.java` | 同上 |
| `backend/src/test/.../service/SetThresholdServiceTest.java` | 同上 |
| `backend/src/test/.../command/GetDataCommandHandlerTest.java` | super(null, null) |
| `backend/src/test/.../command/GetThresholdCommandHandlerTest.java` | 同上 |
| `backend/src/test/.../command/GetLoginInfoCommandHandlerTest.java` | 同上 |
| `backend/src/test/.../command/GetFtpCommandHandlerTest.java` | 同上 |
| `backend/src/test/.../command/SetThresholdCommandHandlerTest.java` | 同上 |
| `backend/src/test/.../command/TimeCheckCommandHandlerTest.java` | 同上 |
| `backend/src/test/.../SlowDataPollingIntegrationTest.java` | super(null, null) |
| `backend/src/test/.../slow/SlowDataPollingServiceTest.java` | super(null, null) |
| `backend/src/test/.../GetDataSlowChannelIntegrationTest.java` | resolver=null + ip/port preset |
| `backend/src/test/.../GetThresholdSlowChannelIntegrationTest.java` | 同上 |
| `backend/src/test/.../GetLoginInfoFtpSlowChannelIntegrationTest.java` | 同上 |
| `backend/src/test/.../SetThresholdSlowChannelIntegrationTest.java` | 同上 |

## 核心改动说明
1. **3 个新生产文件**：FsuEndpointResult（模型+工厂方法）、FsuEndpointResolver（接口）、FsuServiceEndpointService（Repository 实现+URL 校验）
2. **6 个 Service 注入变更**：每个 Service 新增 FsuEndpointResolver 字段，在 execute() 开头添加 null→解析逻辑
3. **24 个新测试**：覆盖正常解析、设备未找到、IP/端口缺失、buildServiceUrl、isValidUrl、模型工厂
4. **22 个已有测试文件修正**：所有直接构造 Service 的地方传递 null resolver

## 执行的测试命令
```
mvn test -Dtest="com.dcim.platform.binterface.**"
```

## 测试结果
```
Tests run: 735, Failures: 0, Errors: 0, Skipped: 0
```
较 BIF-P4-001 的 711 测试新增 24 个 FsuServiceEndpointServiceTest。

## 是否修改业务代码
是。新增 3 个生产文件，修改 6 个 Service 注入层次。

## 是否涉及数据库
否。复用 FsuDeviceEntity 现有 ipAddr/port 字段。

## 是否涉及前端
否。

## 是否涉及 DSC/RDS
否。

## 是否访问真实设备
否。所有测试使用 Stub/Mock，resolver 仅在 Stub 模式外有效。

## 是否启用真实 FSU 调用
否。real-call-enabled 默认 false。resolver 仅解析地址，不触发网络调用。

## 是否启用 Scheduler
否。

## 是否执行 SET 类命令
否。

## 风险点
| 风险 | 等级 | 说明 |
|------|------|------|
| /services/FSUService 默认路径可能不对 | 低 | 联调时需根据 FSU 厂商调整 |
| 6 个 Service 构造方法签名变更 | 低 | 所有内部测试已更新，外部调用者需补 null |
| resolver 为 null 时降级为原行为 | 低 | 向前兼容 |

## 遗留问题
1. /services/FSUService 路径需真实联调确认
2. FsuDeviceEntity 尚无显式 serviceUrl 字段（预留）
3. 未支持 https 的严格证书校验

## 下一步建议
1. 真实联调：使用 FSU 测试设备验证 endpoint 解析和 Service 调用
2. 如有需要，为 FsuDeviceEntity 增加显式 serviceUrl 字段
3. 考虑将 DEFAULT_ENDPOINT_PATH 提取为可配置项

## Git diff 摘要
大量文件变更，核心为 3 新生产文件 + 6 个 Service 注入 + 大量测试适配。无业务逻辑变更（endpoint 解析为新增功能，已有逻辑不改变）。

## Git status 摘要
```
A  backend/src/main/.../fsu/FsuEndpointResult.java
A  backend/src/main/.../fsu/FsuEndpointResolver.java
A  backend/src/main/.../fsu/FsuServiceEndpointService.java
A  backend/src/test/.../fsu/FsuServiceEndpointServiceTest.java
A  docs/audit/BIF-P4-002-fsu-endpoint-resolver.md
A  docs/memory/2026-05-15-BIF-P4-002-fsu-endpoint-resolver.md
M  backend/src/main/.../service/GetDataService.java
M  backend/src/main/.../service/GetThresholdService.java
M  backend/src/main/.../service/TimeCheckService.java
M  backend/src/main/.../service/GetLoginInfoService.java
M  backend/src/main/.../service/GetFtpService.java
M  backend/src/main/.../service/SetThresholdService.java
M  +22 test files
```
