---
name: bif-p4-004-real-fsu-readonly-integration-wsdl-port
description: BIF-P4-004 单台真实 FSU 只读联调 — WSDL 规范端口 8080 确认正确，FSUService SOAP 可达，发现 SoapMessageHandler 报文格式与 WSDL RPC 约定不匹配
metadata:
  type: project
---

## 任务编号
BIF-P4-004

## 任务名称
单台真实 FSU 只读联调执行（WSDL 端口确认版）

## 操作时间
2026-05-15

## 操作代理
Claude Code

## 规则加载确认
- [x] AGENTS.md
- [x] CLAUDE.md
- [x] docs/PROJECT_ENGINEERING_RULES.md
- [x] 已确认 B接口协议 2016 是唯一设备协议
- [x] 已确认本次任务遵循 论证 → 验证 → 写入

## 联调目标确认

| 项目 | 值 |
|------|-----|
| 目标 IP | 192.168.100.100 |
| 目标 FSUCode | 51051243812345（数据库记录，用户提供） |
| 联调范围 | 仅限白名单 5 命令 |
| 禁止命令 | SET_THRESHOLD / SET_POINT / SET_FTP / SET_FSUREBOOT / Scheduler |

## 端口/路径来源确认

### Port = 8080

| # | 来源 | 证据 |
|---|------|------|
| 1 | `fsu-service.wsdl` L34 | `<wsdlsoap:address location="http://127.0.0.1:8080/services/FSUService"/>` |
| 2 | `sc-service.wsdl` L34 | `<wsdlsoap:address location="http://127.0.0.1:8080/services/SCService"/>` |
| 3 | `docs/protocol/b-interface-2016-handbook.md` §3.1-3.2 | 端点地址均为 `http://127.0.0.1:8080/services/...` |
| 4 | `docs/b-interface/wsdl/reconstructed-FSUService.wsdl` L29 | `<wsdlsoap:address location="http://127.0.0.1:8080/services/FSUService"/>` |

### Path = `/services/FSUService`

| # | 来源 | 证据 |
|---|------|------|
| 1 | `fsu-service.wsdl` L34 | `location=".../services/FSUService"` |
| 2 | `docs/protocol/b-interface-2016-handbook.md` §3.2 | `服务端端点: POST /services/FSUService` |
| 3 | `docs/audit/BIF-P4-002-fsu-endpoint-resolver.md` | `DEFAULT_ENDPOINT_PATH = /services/FSUService` |
| 4 | `FsuServiceEndpointService.java` L31 | `DEFAULT_ENDPOINT_PATH = "/services/FSUService"` |

**最终端点**：`http://192.168.100.100:8080/services/FSUService`

## 任务目标
对单台真实 FSU (192.168.100.100:8080, fsu_code=51051243812345, 艾默生 2808IM) 执行只读联调，按序调用 5 个白名单命令：
GET_LOGININFO → TIME_CHECK → GET_DATA → GET_THRESHOLD → GET_FTP

## 架构判断
- 业务域：B接口慢数据通道 (SC→FSU)
- 涉及层：FsuServiceClient / Service / SOAP 层 / HTTP 传输层
- 模块：binterface.service.fsu + 5 个只读 Service
- 不涉及：前端 / DSC/RDS / SET 类命令 / Scheduler

## 协议一致性判断
- 符合 B接口协议 2016 SOAP/XMLData 框架
- WSDL: RPC style, soapenc:encoded, 操作名 `invoke`
- SOAP 1.1 Envelope 格式正确
- **发现问题**：SoapMessageHandler 生成的 document-style `<Request>` 包装不匹配 WSDL RPC 约定

## 修改前论证
- 数据库 fsu_device.port 由 80 改为 8080（WSDL 规范）
- 联调测试端口由 80 改为 8080
- 手工 wiring 组件，不依赖 Spring 容器

## 写入前验证
- [x] real-call-enabled=false（已确认）
- [x] scheduler-enabled=false（已确认）
- [x] SET 类命令仍被禁止（已确认）
- [x] 端口来自 WSDL soap:address location
- [x] 路径来自 WSDL soap:address location
- [x] FSUCode 来自数据库已有记录
- [x] 无破坏性变更

## 实际修改文件

### 配置变更
| 文件 | 变更 | 说明 |
|------|------|------|
| `application.yml` | real-call-enabled: false→true→false | 临时启用，已恢复 |
| `fsu_device.port` | 80→8080 | 按 WSDL 规范修正 |

### 测试修改
| 文件 | 变更 | 说明 |
|------|------|------|
| `BifP4005RealFsuIntegrationTest.java` | SERVICE_URL 端口 80→8080 | 按 WSDL 规范 |

## 核心改动说明
1. 端口修正：WSDL 指定 8080，之前 80 为猜测端口
2. 联调测试：5 步顺序执行，手工 wiring 组件
3. 报文格式：SoapMessageHandler 生成 document-style 包装

## 联调执行结果

### 逐命令记录

| # | PK_Type | Endpoint | HTTP | SOAP Fault | 成功 |
|---|---------|----------|------|-----------|------|
| 1 | GET_LOGININFO | http://192.168.100.100:8080/services/FSUService | 500 | `Method 'Request' not implemented` | ❌ |
| 2 | TIME_CHECK | 同上 | 500 | 同上 | ❌ |
| 3 | GET_DATA | 同上 | 500 | 同上 | ❌ |
| 4 | GET_THRESHOLD | 同上 | 500 | 同上 | ❌ |
| 5 | GET_FTP | 同上 | 500 | 同上 | ❌ |

### SOAP Fault 完整内容
```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Body>
    <SOAP-ENV:Fault>
      <faultcode>SOAP-ENV:Client</faultcode>
      <faultstring>Method 'Request' not implemented: method name or namespace not recognized</faultstring>
    </SOAP-ENV:Fault>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

### 关键发现

1. **Port 8080 确认正确**：FSU 返回 SOAP Fault（非 HTTP 404），证明 FSUService 存在于该端口
2. **Path `/services/FSUService` 确认正确**：SOAP 引擎接收并解析了请求
3. **SOAP 报文格式不匹配**：FSU 期望 WSDL 定义的 RPC-style `invoke` 调用，但 SoapMessageHandler 发送的是 document-style `<Request>` 包装
4. **FSU 是真实的 B接口 SOAP 服务**：FSU 正确解析了 SOAP Envelope 并进行了方法名校验

### 根因分析

B接口 WSDL 定义：
```xml
<wsdl:portType name="FSUService">
  <wsdl:operation name="invoke" parameterOrder="xmlData">
    <wsdl:input message="impl:invokeRequest" name="invokeRequest"/>
    <wsdl:output message="impl:invokeResponse" name="invokeResponse"/>
  </wsdl:operation>
</wsdl:portType>
```

WSDL 要求 SOAP RPC style，操作名 `invoke`，参数 `xmlData`。FSU 的 SOAP 引擎期望：
```xml
<soap:Body>
  <ns1:invoke xmlns:ns1="http://FSUService.chinatowercom.com">
    <xmlData xsi:type="soapenc:string">...</xmlData>
  </ns1:invoke>
</soap:Body>
```

但当前 SoapMessageHandler 生成：
```xml
<soap:Body>
  <Request>
    <PK_Type>...</PK_Type>
    <Info>...</Info>
    <xmlData>...</xmlData>
  </Request>
</soap:Body>
```

B接口协议文档中的 `<Request>` 包装是"逻辑结构"，实际 SOAP 封装需要符合 WSDL RPC 约定。

## 测试命令
```bash
# 运行联调测试
mvn test -Dtest="BifP4005RealFsuIntegrationTest" -DfailIfNoTests=false

# 全量回归
mvn test
```

## 测试结果

### 联调测试
```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```
5 测试全部执行无崩溃，错误处理链正确。

### 全量回归
```
Tests run: 764, Failures: 0, Errors: 1, Skipped: 0
```
- Errors=1: DcimPlatformApplicationTests.contextLoads (已有 Bean 冲突，非本次引入)
- 零新增失败，零回归

## 安全检查

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 是否执行 SET 类命令 | ❌ 否 | SET_THRESHOLD.enabled=false, SET_POINT/SET_FTP 为桩 |
| 是否启用 Scheduler | ❌ 否 | scheduler-enabled=false |
| 是否批量联调 | ❌ 否 | 仅 1 台 FSU |
| 是否输出明文敏感信息 | ❌ 否 | GET_FTP username 已脱敏 |
| 是否已恢复 real-call-enabled=false | ✅ 是 | 已恢复 |
| 端口是否来自 WSDL 规范 | ✅ 是 | 8080 |
| 路径是否来自 WSDL 规范 | ✅ 是 | /services/FSUService |

## 协议一致性检查
- 符合 B接口协议 2016 框架
- SOAP 1.1 Envelope 格式正确
- PK_Type / Info / xmlData 字段符合协议
- **报文封装格式需修正**：WSDL RPC 约定 vs 当前 document-style
- 未使用私有 JSON/UDP 协议
- 未自造 MsgType/SignalID/DeviceID/FSUCode/ResultCode
- 未触碰 DSC/RDS

## 架构一致性检查
- FsuServiceClient 接口抽象正确
- RealHttpFsuServiceClient HTTP 层正确
- HTTP 500 SOAP Fault 被正确捕获和传播
- 各 Service 层错误包装正确
- 未引入重复逻辑或技术债

## 是否触碰 DSC/RDS 主流程
否。所有操作均在 B接口 SOAP/XMLData 框架内。

## 遗留问题
1. **SoapMessageHandler 报文格式不匹配 WSDL RPC 约定**：需重构为 RPC-style `invoke` 调用
2. FSUCode 未在 LOGIN 中注册：当前直接查询，FSU 可能需要先 LOGIN 建立 Session
3. FSU 可能需要 SessionID：白名单命令中（除 GET_LOGININFO）可能要求携带有 SessionID
4. Bean 冲突仍未修复：BInterfaceMessageLogRepository 重复定义

## 建议下一步

### BIF-P4-005：SoapMessageHandler RPC 适配
**优先级：最高**

将 SoapMessageHandler 从 document-style 改为 WSDL 约定的 RPC-style：
```java
// 当前（错误）:
<Request><PK_Type>...</PK_Type><Info>...</Info><xmlData>...</xmlData></Request>

// 目标（正确）:
<ns1:invoke xmlns:ns1="http://FSUService.chinatowercom.com">
  <xmlData xsi:type="soapenc:string"><!-- 完整 XML 作为字符串 --></xmlData>
</ns1:invoke>
```

涉及变更：
- `SoapMessageHandler.buildRequest()` — 改用 RPC-style 封装
- `SoapMessageHandler.parse()` — 适配 RPC-style 响应解析
- BInterface 协议 handbook §4 报文结构更新
- 所有 fixtures 可能需要更新（SOAP 报文格式变更）
- 所有基于 SOAP 报文的测试更新

预计影响测试数：~200（SOAP 相关 + 集成测试）
