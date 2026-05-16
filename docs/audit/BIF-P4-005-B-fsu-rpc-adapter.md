# BIF-P4-005-B：FsuServiceRpcAdapter 实现

> 基于 BIF-P4-005-A WSDL 与协议再审计结论
> 完成日期：2026-05-15

---

## 一、本阶段目标

基于 BIF-P4-005-A 审计结论，新增 `FsuServiceRpcAdapter`，使 SC→FSU 出站真实 HTTP SOAP 调用符合 FSUService.wsdl 的 rpc/encoded 约定。

---

## 二、BIF-P4-005-A 结论摘要

- FSUService.wsdl: operation=`invoke`, style=`rpc`, use=`encoded`, SOAPAction=`""`
- 当前项目错误格式: `<soap:Body><Request>...</Request></soap:Body>`
- 正确格式: `<soap:Body><ns1:invoke><xmlData xsi:type="string"><Request>...</Request></xmlData></ns1:invoke></soap:Body>`
- Fault: `Method 'Request' not implemented` — FSU 把 `<Request>` 当操作名，WSDL 中不存在

---

## 三、FsuServiceRpcAdapter 设计

### 文件: `FsuServiceRpcAdapter.java`

职责：FSUService WSDL RPC 线格式封装/解包。

核心方法:
- `wrapRequestPayload(String requestPayloadXml)` → 完整 RPC SOAP Envelope
- `unwrapResponsePayload(String rpcResponseSoap)` → document-style SOAP Envelope
- `isRpcFault(String soap)` → boolean
- `extractFaultCode(String soap)` → String
- `extractFaultString(String soap)` → String

### RPC Request 格式

```
SOAP-ENV:Envelope (xmlns:SOAP-ENV, SOAP-ENC, xsi, ns1)
  SOAP-ENV:Body
    ns1:invoke (xmlns:ns1="http://FSUService.chinatowercom.com")
      xmlData (xsi:type="SOAP-ENC:string")
        <Request>    ← SoapMessageHandler.buildRequest() 产出
          PK_Type / Info / xmlData
        </Request>
```

### RPC Response 解包策略

```
SOAP-ENV:Envelope → Body
  ├── SOAP-ENV:Fault → wrapFaultInEnvelope()
  └── ns1:invokeResponse
       └── invokeReturn (xsi:type="SOAP-ENC:string")
            └── <Response> → wrapInMinimalEnvelope()
                 ResultCode / xmlData / ...
```

解包后产出 document-style SOAP Envelope，由 SoapMessageHandler.parse() 处理。

### SOAP Fault 处理

- `isRpcFault()` — 检测 Envelope/Body/Fault 结构
- `extractFaultCode/String()` — 提取 faultcode 和 faultstring
- Fault fixture 已记录真实 FSU 返回的 Fault

---

## 四、RealHttpFsuServiceClient 修改

### 调用链变更

```
旧: soapMessageHandler.buildRequest() → doHttpPost() → soapMessageHandler.parse()
新: soapMessageHandler.buildRequest() → rpcAdapter.wrapRequestPayload() → doHttpPost() → rpcAdapter.unwrapResponsePayload() → soapMessageHandler.parse()
```

### 构造函数变更

新增 `FsuServiceRpcAdapter` 依赖注入。

### HTTP 变更

- 新增 `SOAPAction: ""` header

---

## 五、与 StubFsuServiceClient 边界

**StubFsuServiceClient 零修改。**

- stub 使用硬编码 document-style SOAP 字符串，通过 soapMessageHandler.parse() 解析
- parse() 双向兼容，同时支持 RPC 和 document-style
- stub 不经过 FsuServiceRpcAdapter

---

## 六、与 SCService 入站链路边界

**SCService 入站链零修改。**

- buildResponse() 保持原 document-style 完整 SOAP 输出
- SCService 入站请求/响应仍使用 document-style
- FsuServiceRpcAdapter 仅用于 FSUService 出站

---

## 七、测试覆盖

| 测试类 | 测试数 | 状态 |
|--------|--------|------|
| FsuServiceRpcAdapterTest | 27 | ✅ 全部通过 |
| SoapMessageHandlerTest | 71 | ✅ 全部通过 |
| StubFsuServiceClientTest | 23 | ✅ 全部通过 |
| BifP4005RealFsuIntegrationTest | 5 | ✅ 全部通过 |
| BInterfaceMainFlowIntegrationTest | 23 | ✅ 全部通过 |
| 全量回归 | 791 | 0 失败, 1 已有 Error |

### FsuServiceRpcAdapterTest 覆盖

- wrapRequestPayload 15 个测试：SOAP Envelope, Body, ns1:invoke, namespace, xmlData, xsi:type, payload 保留, 边界/异常
- unwrapResponsePayload 5 个测试：RPC 解包, ResultCode 保留, LoginInfo 保留, Fault 处理, document 兼容
- Fault 检测/提取 5 个测试：isRpcFault, extractFaultCode, extractFaultString
- 真实 Fault fixture 3 个测试：加载, 解析, 归因验证

---

## 八、未访问真实设备说明

- real-call-enabled=false（默认）
- BifP4005RealFsuIntegrationTest 运行但不发起真实网络请求（manual wiring, no Spring context）
- 所有测试均在本地 JVM 中执行

---

## 九、遗留风险

1. FSU 响应 payload 结构与预期可能有差异（字段缺失、额外命名空间等）
2. CDATA 编码的 payload 未充分测试（当前实现支持文本内容回退）
3. 不同厂商 FSU 可能使用不同的 SOAP 前缀风格

---

## 十、下一步建议

**BIF-P4-005-C**：用 RPC adapter 重新联调真实 FSU (192.168.100.100:8080)
- 启用 real-call-enabled=true
- 运行 BifP4005RealFsuIntegrationTest
- 预期：不再出现 "Method 'Request' not implemented" SOAP Fault
