---
name: bif-p3-007-get-logininfo-get-ftp
description: GET_LOGININFO / GET_FTP 查询类闭环实现，包括 Handler、Service、Result、Stub 支持、单元测试和集成测试
metadata:
  type: project
---

## 任务目标

将 GetLoginInfoCommandHandler 和 GetFtpCommandHandler 从 `notImplemented()` 桩变为完整的真实查询流程。

## 架构判断

- **业务域**: B-Interface 协议
- **层次**: CommandHandler → Service → FsuServiceClient
- **模块**: binterface
- **影响**: CommandDispatcher 的路由表、StubFsuServiceClient 的 switch

## 协议一致性判断

符合 B接口协议 2016 规范：
- GET_LOGININFO 查询登录/在线状态，请求包含 FSUCode，响应含 LoginInfo 包裹元素
- GET_FTP 查询 FTP 配置参数，请求包含 FSUCode + 可选 FileType，响应含 FTPConfig 包裹元素
- 均使用 SOAP/XMLData 消息结构，无私有协议

## 修改前论证

- 两个 Handler 原为 `notImplemented()` 桩，必须实现为真实查询
- 遵循 GetDataCommandHandler 的三层 CommandHandler→Service→FsuServiceClient 模式
- 遵循 GetThresholdCommandHandler 的 Stub/Test 模式
- 敏感字段（Username）需脱敏输出，遵循最小暴露原则
- 严格分离 GET_FTP 与 SET_FTP：本阶段不实现 SET_FTP

## 写入前验证

- [x] GetLoginInfoResult 工厂方法/字段/toString 无密码暴露
- [x] GetFtpResult 工厂方法/字段/getMaskedUsername/toString 无明文 Username
- [x] GetLoginInfoService 异常处理（null FSUCode、client 失败、空响应）
- [x] GetFtpService 异常处理（null FSUCode、client 失败、空响应）
- [x] GetLoginInfoCommandHandler 登录态校验/参数提取/错误传播
- [x] GetFtpCommandHandler 登录态校验/参数提取（含 FileType）/错误传播
- [x] StubFsuServiceClient 新增 enum case 和对应 handler
- [x] StubFsuServiceClientTest 原有 GET_FTP 测试改为 SET_FTP

## 实际修改文件

### 新增（9 个）
- `backend/src/main/java/.../service/GetLoginInfoResult.java`
- `backend/src/main/java/.../service/GetLoginInfoService.java`
- `backend/src/main/java/.../service/GetFtpResult.java`
- `backend/src/main/java/.../service/GetFtpService.java`
- `backend/src/test/.../service/GetLoginInfoServiceTest.java`
- `backend/src/test/.../service/GetFtpServiceTest.java`
- `backend/src/test/.../command/GetLoginInfoCommandHandlerTest.java`
- `backend/src/test/.../command/GetFtpCommandHandlerTest.java`
- `backend/src/test/.../GetLoginInfoFtpSlowChannelIntegrationTest.java`

### 修改（4 个）
- `backend/src/main/java/.../service/fsu/StubFsuServiceClient.java`
- `backend/src/main/java/.../command/GetLoginInfoCommandHandler.java`
- `backend/src/main/java/.../command/GetFtpCommandHandler.java`
- `backend/src/test/.../service/fsu/StubFsuServiceClientTest.java`

## 核心改动

1. **GetLoginInfoCommandHandler**: 从 `notImplemented()` 改为注入 LoginService + GetLoginInfoService，提取 FSUCode，校验登录态，调用 service，构建 CommandResult 含 Info + XmlData（LoginInfo 包裹）
2. **GetFtpCommandHandler**: 从 `notImplemented()` 改为注入 LoginService + GetFtpService，提取 FSUCode + 可选 FileType，校验登录态，调用 service，构建 CommandResult 含 Info + XmlData（FTPConfig 包裹）
3. **GetLoginInfoService**: execute(fsuCode, fsuServiceUrl) → 构造 Info → 调用 FsuServiceClient → 解析 LoginInfo
4. **GetFtpService**: execute(fsuCode, fsuServiceUrl, fileType) → 构造 Info（含可选 FileType）→ 调用 FsuServiceClient → 解析 FTPConfig
5. **StubFsuServiceClient**: 新增 GET_LOGININFO / GET_FTP 分支和内置 SOAP 响应

## 测试命令和结果

```
mvn test -Dtest="com.dcim.platform.binterface.**"
Tests run: 692, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 风险

- 低：StubFsuServiceClientTest 中原有 `shouldReturnErrorForUnsupportedPkType` 使用 GET_FTP，已改为 SET_FTP（GET_FTP 现在被支持）
- 低：CommandDispatcher 已有 GET_LOGININFO / GET_FTP 路由注册

## 遗留问题

- 无 `parsed_get_logininfo.request.json` / `parsed_get_logininfo.response.json` 在 fixtures 目录
- 无 `parsed_get_ftp.request.json` 在 fixtures 目录（仅有 response）
- 这些 json 文件为 fixture 加载器使用，不影响当前功能

## 下一步建议

- BIF-P3-008: SET_FTP 安全配置类闭环（高风险，需安全门控、审计日志、操作确认）
- BIF-P3-009: SET_POINT / SET_FSUREBOOT 操作类闭环（更高风险，需多重确认）
