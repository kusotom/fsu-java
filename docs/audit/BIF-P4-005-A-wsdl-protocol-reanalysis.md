# BIF-P4-005-A：WSDL 与 B接口协议原文再审计

> 阶段：只读审计，不修改任何业务代码
> 时间：2026-05-15
> 基于：BIF-P4-004 真实 FSU 联调结果 + WSDL 权威文件 + B接口协议 2016 整理文档

---

## 一、本阶段目标

基于 BIF-P4-004 真实 FSU 联调得到的 SOAP Fault (`Method 'Request' not implemented`)，重新完整分析 FSUService.wsdl、SCService.wsdl 和 B接口协议原文，确认：

1. FSU 期望的 SOAP Body 第一层节点
2. operation 名称和 namespace
3. 当前项目 SOAP 构造与 WSDL 的差异
4. Fault 根因
5. 适配方案建议

---

## 二、BIF-P4-004 真实联调结果回顾

| 项目 | 值 |
|------|-----|
| 目标 FSU | 艾默生 2808IM, 192.168.100.100:8080 |
| FSUCode | 51051243812345 |
| Endpoint | http://192.168.100.100:8080/services/FSUService |
| 命令 | GET_LOGININFO, TIME_CHECK, GET_DATA, GET_THRESHOLD, GET_FTP |
| HTTP 状态 | 500 |
| SOAP Fault | `SOAP-ENV:Client / Method 'Request' not implemented: method name or namespace not recognized` |

---

## 三、WSDL 文件清单

### 主依据（WSDL协议/ 目录，权威来源）

| # | 文件 | 路径 | 用途 |
|---|------|------|------|
| 1 | FSUService.wsdl | `/home/tom/桌面/FSU/WSDL协议/FSUService.wsdl` | SC→FSU 慢数据通道 WSDL |
| 2 | SCService.wsdl | `/home/tom/桌面/FSU/WSDL协议/SCService.wsdl` | FSU→SC 快数据通道 WSDL |

### 辅助依据（项目内副本）

| # | 文件 | 路径 | 用途 |
|---|------|------|------|
| 3 | fsu-service.wsdl | `fsu-platform-java/backend/src/main/resources/wsdl/` | 项目构建副本 |
| 4 | sc-service.wsdl | `fsu-platform-java/backend/src/main/resources/wsdl/` | 项目构建副本 |
| 5 | reconstructed-FSUService.wsdl | `fsu-python/.../docs/b-interface/wsdl/` | Python 项目重建版 |
| 6 | reconstructed-SCService.wsdl | `fsu-python/.../docs/b-interface/wsdl/` | Python 项目重建版 |

**验证结果**：6 个 WSDL 文件内容一致（仅 Python 重建版使用 `definitions` 而非 `wsdl:definitions`，语义相同）。主依据为 #1 和 #2。

---

## 四、FSUService.wsdl 分析

### 4.1 完整字段解析

| 字段 | 值 | 来源行 |
|------|-----|--------|
| targetNamespace | `http://FSUService.chinatowercom.com` | L2 |
| service name | `FSUServiceService` | L52 |
| port name | `FSUService` | L54 |
| binding name | `FSUServiceSoapBinding` | L28 |
| portType name | `FSUService` | L16 |
| operation 列表 | **仅 1 个**: `invoke` | L18 |
| input message | `invokeRequest` | L19-20 |
| input part | `xmlData`, type=`soapenc:string` | L6 |
| output message | `invokeResponse` | L21-22 |
| output part | `invokeReturn`, type=`soapenc:string` | L12 |
| SOAP binding style | **`rpc`** | L30 |
| SOAP body use | **`encoded`** | L38, L44 |
| encodingStyle | `http://schemas.xmlsoap.org/soap/encoding/` | L38 |
| body namespace | `http://FSUService.chinatowercom.com` | L38 |
| SOAPAction | **`""`** (空字符串) | L34 |
| endpoint location | `http://127.0.0.1:8080/services/FSUService` | L56 |

### 4.2 关键推论

1. **只有一个 operation**: `invoke`。不存在名为 `Request`、`getData`、`request` 或其他名称的 operation。
2. **RPC style**：SOAP Body 的第一个子元素必须是 operation 名称（`invoke`），而非 `<Request>`。
3. **encoded use**：参数使用 `soapenc:encoded` 编码，`xsi:type` 必须声明。
4. **单一参数**：operation 只有一个参数 `xmlData`，类型 `soapenc:string`。
5. **Body namespace**：`http://FSUService.chinatowercom.com`

### 4.3 正确的 RPC Request Body 结构

```xml
<SOAP-ENV:Body>
  <ns1:invoke xmlns:ns1="http://FSUService.chinatowercom.com">
    <xmlData xsi:type="SOAP-ENC:string">
      <!-- payload: <Request> 结构放在这里，作为 xmlData 字符串的内容 -->
    </xmlData>
  </ns1:invoke>
</SOAP-ENV:Body>
```

### 4.4 正确的 RPC Response Body 结构

```xml
<SOAP-ENV:Body>
  <ns1:invokeResponse xmlns:ns1="http://FSUService.chinatowercom.com">
    <invokeReturn xsi:type="SOAP-ENC:string">
      <!-- payload: <Response> 结构放在这里，作为 invokeReturn 字符串的内容 -->
    </invokeReturn>
  </ns1:invokeResponse>
</SOAP-ENV:Body>
```

---

## 五、SCService.wsdl 分析

### 5.1 完整字段解析

| 字段 | 值 | 来源行 |
|------|-----|--------|
| targetNamespace | `http://SCService.chinatowercom.com` | L2 |
| service name | `SCServiceService` | L52 |
| port name | `SCService` | L54 |
| binding name | `SCServiceSoapBinding` | L28 |
| portType name | `SCService` | L16 |
| operation 列表 | **仅 1 个**: `invoke` | L18 |
| input message | `invokeRequest` | L19-20 |
| input part | `xmlData`, type=`soapenc:string` | L6 |
| output message | `invokeResponse` | L21-22 |
| output part | `invokeReturn`, type=`soapenc:string` | L12 |
| SOAP binding style | **`rpc`** | L30 |
| SOAP body use | **`encoded`** | L38, L44 |
| encodingStyle | `http://schemas.xmlsoap.org/soap/encoding/` | L38 |
| body namespace | `http://SCService.chinatowercom.com` | L38 |
| SOAPAction | **`""`** (空字符串) | L34 |
| endpoint location | `http://127.0.0.1:8080/services/SCService` | L56 |

### 5.2 与 FSUService 的唯一差异

| 字段 | FSUService | SCService |
|------|-----------|-----------|
| targetNamespace | `http://FSUService.chinatowercom.com` | `http://SCService.chinatowercom.com` |
| service name | `FSUServiceService` | `SCServiceService` |
| endpoint path | `/services/FSUService` | `/services/SCService` |
| 其他 | 完全相同 | 完全相同 |

**结论**：两个 WSDL 结构完全一致，仅命名空间和服务名不同。适配一个即适配两个。

---

## 六、WSDL 分析汇总表

| WSDL文件 | targetNamespace | service | port | binding | style | use | operation | input msg | output msg | SOAPAction | Body第一层节点 | endpoint |
|----------|----------------|---------|------|---------|-------|-----|-----------|-----------|------------|------------|--------------|----------|
| FSUService.wsdl | http://FSUService.chinatowercom.com | FSUServiceService | FSUService | FSUServiceSoapBinding | **rpc** | **encoded** | **invoke**(xmlData)→invokeReturn | invokeRequest(xmlData:string) | invokeResponse(invokeReturn:string) | "" | **ns1:invoke** | /services/FSUService |
| SCService.wsdl | http://SCService.chinatowercom.com | SCServiceService | SCService | SCServiceSoapBinding | **rpc** | **encoded** | **invoke**(xmlData)→invokeReturn | invokeRequest(xmlData:string) | invokeResponse(invokeReturn:string) | "" | **ns1:invoke** | /services/SCService |

---

## 七、B接口协议原文分析

### 7.1 协议文档清单

| # | 文件 | 路径 | 性质 |
|---|------|------|------|
| 1 | B接口协议 2016 原文 | `B接口协议/B接口协议2016.docx` | 原始协议 |
| 2 | B接口协议 2024 | `B接口协议/B接口协议 2024.pdf` | 更新版 |
| 3 | b-interface-2016-handbook.md | `fsu-platform-java/docs/protocol/` | 项目整理版 |
| 4 | b-interface-2016-summary.md | `fsu-platform-java/docs/protocol/` | 概述 |
| 5 | b-interface-command-map.md | `fsu-platform-java/docs/protocol/` | 命令映射 |

### 7.2 协议手册 §3 WSDL 定义

协议手册 §3.1（SCService WSDL）和 §3.2（FSUService WSDL）明确：

```
服务端端点: POST /services/FSUService
风格: RPC
编码: soapenc:encoded
操作: invoke(xmlData: soapenc:string) → invokeReturn: soapenc:string
```

与 WSDL 原文完全一致。

### 7.3 协议手册 §4 报文结构 —— 关键发现

协议手册 §4.1 "通用消息结构" 描述的是 **逻辑结构**（payload 层），不是 SOAP 线格式（wire format）：

```
SOAP Envelope
  └── SOAP Body
       ├── Request / Response          ← 根元素
       │   ├── PK_Type                 ← 命令码
       │   ├── Info                    ← 元数据
       │   └── xmlData                 ← 业务数据
```

协议手册 §4.2 "请求报文模板" 和 §4.3 "响应报文模板" 给出的 SOAP 样例也是 document-style：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>COMMAND_CODE</PK_Type>
      ...
    </Request>
  </soap:Body>
</soap:Envelope>
```

### 7.4 WSDL 与协议手册的不一致

| 层面 | WSDL | 协议手册 §4 |
|------|------|------------|
| SOAP Body 第一层 | `<ns1:invoke>` (RPC operation 名) | `<Request>` / `<Response>` |
| style | **rpc** | 隐含 document |
| namespace | `http://FSUService.chinatowercom.com` | 未声明 |
| SOAPAction | `""` | 未提及 |
| encoding | soapenc:encoded + xsi:type | 未声明 |

**解释**：协议手册 §4 描述的是**逻辑消息结构**（即 `<Request>/<Response>` 内部内容），省略了 WSDL 要求的 RPC 封装层。真实 FSU 设备按 WSDL 校验，因此拒绝 document-style `<Request>`。

**这不意味着协议手册错误**，而是协议手册面向人类开发者描述"有效载荷"，WSDL 面向 SOAP 引擎描述"线格式"。实际实施时必须二者结合。

---

## 八、当前项目 SOAP 构造对比

### 8.1 构造位置

- `SoapMessageHandler.buildRequest()` → 供 `RealHttpFsuServiceClient` 使用（SC→FSU 出站）
- `SoapMessageHandler.buildResponse()` → 供 `ScServiceHandler` 使用（SC→FSU 入站响应）
- `StubFsuServiceClient` → 硬编码 document-style SOAP 字符串

### 8.2 当前实际 SOAP 报文

**当前 buildRequest() 输出**（document-style，修改前）：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>GET_LOGININFO</PK_Type>
      <Info>
        <FSUCode>51051243812345</FSUCode>
      </Info>
      <xmlData/>
    </Request>
  </soap:Body>
</soap:Envelope>
```

### 8.3 与 WSDL 要求的差异对比

| 项目当前字段 | 当前结构 | WSDL 要求 | 协议手册描述 | 一致？ | 差异说明 |
|-------------|---------|----------|-------------|--------|---------|
| Body 第一层 | `<Request>` | `<ns1:invoke>` | `<Request>` | **WSDL不一致** | WSDL RPC 要求 operation 名 `invoke` |
| namespace | 无 | `http://FSUService.chinatowercom.com` | 无 | **WSDL不一致** | RPC body 必须有 namespace |
| SOAPAction | 未设置 | `""` | 未提及 | **可能不一致** | HTTP header 可能缺失 |
| encoding | 无 xsi:type | `xsi:type="SOAP-ENC:string"` | 无 | **WSDL不一致** | encoded use 要求类型声明 |
| PK_Type 位置 | `<Request>/<PK_Type>` | `<invoke>/<xmlData>/<Request>/<PK_Type>` | `<Request>/<PK_Type>` | - | payload 内容相同，封装层不同 |
| Info 位置 | `<Request>/<Info>` | `<invoke>/<xmlData>/<Request>/<Info>` | `<Request>/<Info>` | - | 同上 |
| xmlData 位置 | `<Request>/<xmlData>` | `<invoke>/<xmlData>/<Request>/<xmlData>` | `<Request>/<xmlData>` | - | 同上 |

### 8.4 核心问题

**当前实现把 `<Request>` 放在 SOAP Body 第一层，而 WSDL 要求把 `<ns1:invoke>` 放在第一层，`<Request>` 要嵌套在 `<xmlData>` 参数内部。**

语义理解：
- **WSDL 层**：operation=`invoke`, RPC参数=`xmlData`(string)
- **Payload 层**：`<Request><PK_Type>...</PK_Type><Info>...</Info><xmlData>...</xmlData></Request>`
- **正确 SOAP**：`<invoke><xmlData>` + payload + `</xmlData></invoke>`

---

## 九、真实 SOAP Fault 归因

### 9.1 Fault 内容

```xml
<SOAP-ENV:Fault>
  <faultcode>SOAP-ENV:Client</faultcode>
  <faultstring>Method 'Request' not implemented: method name or namespace not recognized</faultstring>
</SOAP-ENV:Fault>
```

### 9.2 归因分析

| 判断项 | 结论 | 证据 |
|--------|------|------|
| FSU 是否把 `<Request>` 当成 operation 名？ | **是** | `Method 'Request' not implemented` |
| WSDL 中是否存在 `Request` operation？ | **否** | 唯一的 operation 是 `invoke` |
| FSU 返回该 Fault 是否合理？ | **完全合理** | RPC-style SOAP 的标准行为：无法识别的 method → SOAP Client Fault |
| 正确 operation 应该是什么？ | **`invoke`** | WSDL portType operation name |
| 正确 namespace 应该是什么？ | **`http://FSUService.chinatowercom.com`** | WSDL targetNamespace + body namespace |
| Fault code `SOAP-ENV:Client` 含义？ | 客户端请求错误 | SOAP 1.1 规范：请求格式不对 → Client fault |
| 为什么是 SOAP-ENV 前缀？ | FSU 使用 gSOAP 或类似 C/C++ SOAP 库 | gSOAP 默认使用 `SOAP-ENV` 前缀 |

### 9.3 Fault 与 WSDL 的完整一致性

```
WSDL operation name: "invoke"
Current SOAP Body first child: "Request" (no namespace)
FSU SOAP engine: looks for method "Request" → not found
FSU SOAP engine: returns SOAP-ENV:Client "Method 'Request' not implemented"
✓ 完全一致，FSU 行为正确
```

---

## 十、推荐适配方案

### 方案 A：新增 FsuServiceRpcAdapter（推荐）

**思路**：在 `RealHttpFsuServiceClient` 和 `SoapMessageHandler` 之间插入一个 RPC 适配层。

```
RealHttpFsuServiceClient
  → FsuServiceRpcAdapter.wrapRequest(innerXml)
    → SoapMessageHandler 构造 RPC SOAP Envelope
  → FsuServiceRpcAdapter.unwrapResponse(soapResponse)
    → SoapMessageHandler.parse() 提取内层 payload
```

**优点**：
- 不影响 `SoapMessageHandler` 现有行为（fixtures/stub/测试全部不变）
- 不影响 SCService 入站链路（仍然使用现有格式）
- 只影响 FSUService 出站请求
- 低风险

**缺点**：
- 新增一个类
- SCService 入站也可能有相同问题（当真实 FSU 以 RPC 格式发送请求给 SC 时）

### 方案 B：扩展 SoapMessageHandler（可接受）

**思路**：在 `SoapMessageHandler` 中新增 `buildRpcRequest()` 和 `buildRpcResponse()` 方法，保留原有 `buildRequest()`/`buildResponse()` 不动。

**优点**：
- 不破坏现有测试和 fixtures
- 统一管理 SOAP 构造逻辑
- 可逐步迁移

**缺点**：
- 方法膨胀
- 需要修改 `RealHttpFsuServiceClient` 调用新方法
- 中等风险

### 方案 C：全局替换 buildRequest/buildResponse（已执行但禁止推广）

**思路**：直接修改 `buildRequest()` 和 `buildResponse()` 生成 RPC 格式，`parse()` 双向兼容。

**风险**：
- 影响所有依赖 `buildRequest()`/`buildResponse()` 的代码
- 导致 StubFsuServiceClient 的硬编码 SOAP 字符串与 buildRequest 输出不一致
- 如果 FSU/SC 厂商使用不同的前缀风格，可能需要回退

### 推荐方案

**推荐方案 A（FsuServiceRpcAdapter）**，理由：
1. **最小影响面**：只修改 FSUService 出站路径
2. **完全向后兼容**：所有现有测试保持不变
3. **清晰的架构分层**：RPC 封装作为一个独立关注点
4. **便于未来适配**：不同厂商的 FSU 可能有细微的 SOAP 差异（如 namespace 前缀），adapter 可根据 endpoint 或 FSU 类型切换
5. **SCService 入站独立处理**：当需要为 SCService 入站添加 RPC 兼容时，可新增 ScServiceRpcAdapter

---

## 十一、禁止采用的方案

| 方案 | 原因 |
|------|------|
| 直接把 `<Request>` 改成 `<ns1:invoke>` | `<Request>` 是 payload 根元素，不是 SOAP operation。`<ns1:invoke>` 是 SOAP 封装层，payload 内容仍需保留 |
| 删除 `<Request>` 只用 RPC 参数名 | PK_Type/Info/xmlData 这些 B接口命令字段在 payload 中，必须保留 |
| 把 PK_Type 作为 operation 名 | WSDL 中没有多个 operation。各命令通过 payload 内的 PK_Type 区分，而非 SOAP 层 |
| 假设所有 FSU 都接受 document-style | 真实联调已证明艾默生 2808IM 不接受 |

---

## 十二、后续代码修复任务拆分

### BIF-P4-005-B-1：新增 FsuServiceRpcAdapter

**目标**：创建 RPC 适配层

```
新增文件:
  backend/src/main/java/.../binterface/service/fsu/FsuServiceRpcAdapter.java

功能:
  - wrapRequest(FsuServiceRequest) → RPC SOAP 字符串
  - unwrapResponse(String soapResponse) → FsuServiceResponse
  - 内嵌 SoapMessageHandler 用于 RPC 封装
```

### BIF-P4-005-B-2：修改 RealHttpFsuServiceClient

**目标**：注入并使用 FsuServiceRpcAdapter

```
修改文件:
  RealHttpFsuServiceClient.java
  替换直接调用 soapMessageHandler.buildRequest() → adapter.wrapRequest()
  替换直接调用 soapMessageHandler.parse() → adapter.unwrapResponse()
```

### BIF-P4-005-B-3：新增 RPC 请求/响应测试套件

**目标**：验证 RPC 适配层正确性

```
新增测试:
  FsuServiceRpcAdapterTest.java (~30 测试)
  - FSUService RPC request construction
  - FSUService RPC response parsing
  - FSUService RPC round-trip
  - TIME_CHECK RPC 格式
  - GET_DATA RPC 格式
  - GET_THRESHOLD RPC 格式
  - GET_LOGININFO RPC 格式
  - GET_FTP RPC 格式
  - RPC SOAP Fault 解析
  - SCService 入站不受影响
  - StubFsuServiceClient 不访问网络
```

### BIF-P4-005-B-4：重新联调真实 FSU

**目标**：用新 RPC adapter 重新联调 192.168.100.100:8080

### BIF-P4-005-B-5：SCService 入站 RPC 兼容评估（另开阶段）

**目标**：评估是否需要为 FSU→SC 入站请求增加 RPC 解析兼容

---

## 十三、测试计划

### 必须新增的测试

| # | 测试类/方法 | 覆盖内容 |
|---|-----------|---------|
| 1 | FsuServiceRpcAdapterTest | RPC adapter 全部逻辑 |
| 2 | shouldBuildFsuInvokeRpcRequest | FSUService invoke RPC 请求构造 |
| 3 | shouldParseFsuInvokeRpcResponse | FSUService invokeResponse RPC 解析 |
| 4 | shouldBuildTimeCheckRpcRequest | TIME_CHECK RPC 格式 |
| 5 | shouldBuildGetDataRpcRequest | GET_DATA + SignalID 列表 RPC 格式 |
| 6 | shouldBuildGetThresholdRpcRequest | GET_THRESHOLD RPC 格式 |
| 7 | shouldBuildGetLoginInfoRpcRequest | GET_LOGININFO RPC 格式 |
| 8 | shouldBuildGetFtpRpcRequest | GET_FTP RPC 格式 |
| 9 | shouldParseFsuRpcSoapFault | SOAP Fault → 结构化错误 |
| 10 | shouldNotAffectScServiceInbound | SCService 入站链路不受 adapter 影响 |
| 11 | shouldNotAffectStubClient | StubFsuServiceClient 行为不变 |
| 12 | realCallDisabledByDefault | RealHttpFsuServiceClient 默认不加载 |
| 13 | noSetCommandExecution | SET 类命令不可通过 adapter 执行 |
| 14 | RPC round-trip test | build → call(Stub) → parse 完整链路 |

### 回归测试范围

- SoapMessageHandlerTest (71 tests) — 应全部通过（未修改）
- StubFsuServiceClientTest (23 tests) — 应全部通过（未修改）
- BifP4005RealFsuIntegrationTest (5 tests) — adapter 上线后重跑
- BInterfaceMainFlowIntegrationTest (23 tests) — 确认主链路不变
- 全量测试 (~770) — target: 0 failures, 1 pre-existing Error

---

## 十四、风险清单

| # | 风险 | 等级 | 缓解 |
|---|------|------|------|
| 1 | FSU 响应 RPC 格式与预期有细微偏差 | 中 | adapter 中做宽松解析 |
| 2 | 不同厂商 FSU 的 namespace 前缀不一致 | 低 | namespace URI 匹配，不匹配前缀 |
| 3 | SCService 入站链路也需要 RPC 适配 | 中 | 另开阶段评估，不混入本次 |
| 4 | adapter 引入性能开销 | 低 | 仅增加一层 XML DOM 操作 |
| 5 | Fixtures 与 RPC 输出不一致 | 低 | 不修改 fixtures，adapter 独立测试 |

---

## 十五、遗留问题

1. B接口协议 2016 原文 (.docx) 未逐字比对，依赖项目整理版手册
2. SCService 入站 RPC 兼容性未评估（真实 FSU 可能以 RPC 格式向 SC 发送 LOGIN/HEARTBEAT）
3. FSU 响应 payload 结构可能与协议手册描述有差异（BIF-P4-004 中 GET_DATA 返回 ResultCode=0 但无信号数据）

---

## 十六、建议下一步

1. **BIF-P4-005-B**：基于本审计结论，实施方案 A（FsuServiceRpcAdapter）
2. **BIF-P4-005-C**：用新 adapter 重新联调 192.168.100.100:8080
3. **BIF-P4-006**：SCService 入站 RPC 兼容评估（独立阶段，不混入 FSUService 出站修改）
