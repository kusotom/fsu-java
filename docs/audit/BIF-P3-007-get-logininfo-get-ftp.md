# BIF-P3-007: GET_LOGININFO / GET_FTP 查询类闭环

## 任务目标

将 GetLoginInfoCommandHandler 和 GetFtpCommandHandler 从 `notImplemented()` 桩变为完整的真实查询流程，包括 Service 层、Result 模型、Stub 支持、单元测试和集成测试。

## 变更内容

### 新增文件

| 文件 | 说明 |
|------|------|
| `GetLoginInfoResult.java` | GET_LOGININFO 查询结果模型（success/resultCode/fsuCode/loginStatus/onlineStatus/sessionId/loginTime/lastHeartbeat） |
| `GetLoginInfoService.java` | GET_LOGININFO 查询服务（构造请求 → 调用 FsuServiceClient → 解析 LoginInfo） |
| `GetFtpResult.java` | GET_FTP 查询结果模型（success/resultCode/fsuCode/host/port/username/passiveMode/basePath + getMaskedUsername） |
| `GetFtpService.java` | GET_FTP 查询服务（构造请求 → 调用 FsuServiceClient → 解析 FTPConfig） |
| `GetLoginInfoServiceTest.java` | Service 层 13 个测试 |
| `GetFtpServiceTest.java` | Service 层 17 个测试 |
| `GetLoginInfoCommandHandlerTest.java` | Handler 层 11 个测试 |
| `GetFtpCommandHandlerTest.java` | Handler 层 20 个测试 |
| `GetLoginInfoFtpSlowChannelIntegrationTest.java` | 集成测试 11 个 |

### 修改文件

| 文件 | 变更 |
|------|------|
| `StubFsuServiceClient.java` | 新增 `DEFAULT_GET_LOGININFO_RESPONSE` / `DEFAULT_GET_FTP_RESPONSE` 常量 + handleGetLoginInfo/handleGetFtp 方法 |
| `GetLoginInfoCommandHandler.java` | `notImplemented()` → 注入 LoginService + GetLoginInfoService，完整校验和查询流程 |
| `GetFtpCommandHandler.java` | `notImplemented()` → 注入 LoginService + GetFtpService，完整校验和查询流程（含 FileType 提取） |
| `StubFsuServiceClientTest.java` | 原有 GET_FTP 不支持的测试改为 SET_FTP；新增 8 个 GET_LOGININFO/GET_FTP 验证测试 |

## 架构设计

```
SC SOAP Request
    → CommandDispatcher (pkType 路由)
        → GetLoginInfoCommandHandler / GetFtpCommandHandler
            → LoginService.isLoggedIn() — 登录态校验
            → GetLoginInfoService / GetFtpService
                → FsuServiceClient.call(FsuServiceRequest) — SOAP 调用
            ← GetLoginInfoResult / GetFtpResult
        ← CommandResult (Info + XmlData)
    ← SC SOAP Response
```

## ResultCode 策略

| Code | 含义 | 触发条件 |
|------|------|----------|
| 0 | 成功 | 查询正常返回 |
| 1002 | 未登录/离线 | LoginService.isLoggedIn 返回 false |
| 2001 | 缺少参数 | FSUCode 为 null/空 |
| 2003 | 无有效数据 | 响应 xmlData 为空或无 LoginInfo/FTPConfig |
| 5001 | 内部错误 | 未知异常 |

## 测试覆盖

- **GetLoginInfoServiceTest** (13): 成功响应、字段解析、null/空 FSUCode、client 错误/异常/禁用、请求构造、空响应
- **GetFtpServiceTest** (17): 成功响应、字段解析、可选 FileType、null/空 FSUCode、client 错误/异常/禁用、请求构造、toString 脱敏、空响应
- **GetLoginInfoCommandHandlerTest** (11): 成功、pkType、null context、null soap、缺少 FSUCode、未登录、service 错误、extractFsuCode 多种场景
- **GetFtpCommandHandlerTest** (20): 成功、xmlData、无 FileType、pkType、null context、null soap、缺少 FSUCode、未登录、service 错误、extractFsuCode/extractFileType 多种场景
- **GetLoginInfoFtpSlowChannelIntegrationTest** (11): 已登录查询、字段返回、未登录拒绝、离线拒绝、参数校验、状态不修改、无网络访问
- **StubFsuServiceClientTest** (+8): GET_LOGININFO/GET_FTP 成功响应、字段验证、无网络、原始 SOAP

## 敏感字段处理

- GetFtpResult.getMaskedUsername(): 首字符 + **** + 末字符
- GetFtpResult.toString() 使用 getMaskedUsername()
- GetFtpService 日志使用首字符 + ****
- 当前 fixtures 不包含密码字段

## 测试结果

所有 692 个 binterface 测试全部通过 (Failures: 0, Errors: 0, Skipped: 0)。
