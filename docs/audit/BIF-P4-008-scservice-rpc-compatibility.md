# BIF-P4-008：SCService 入站 RPC 兼容性评估

> 阶段：评估 + 测试，不修改业务代码，不访问真实设备
> 基于：BIF-P4-005-A WSDL审计 + BIF-P4-005-C FSUService RPC 真实验证
> 完成日期：2026-05-15

---

## 一、本阶段目标

基于 FSUService 出站 RPC 的真实验证结果（艾默生 2808IM 要求严格 WSDL RPC 格式），评估 FSU→SC 入站方向（SCService）是否也需要兼容 WSDL RPC/encoded 格式。

---

## 二、为什么要评估 SCService 入站 RPC

1. **SCService.wsdl 与 FSUService.wsdl 结构完全相同**（仅 namespace/服务名不同）
2. **FSUService 出站已确认**：艾默生 2808IM 拒绝 document-style，仅接受 RPC invoke(xmlData)
3. **对称性推论**：同一设备在 SCService 方向（FSU 主动上报）大概率也使用 RPC 格式
4. **当前平台 SCService**：入站解析已双向兼容，但出站响应仍为 document-style
5. **风险**：如果配置 FSU 向平台上报，FSU 可能以 RPC 格式发送请求并期望 RPC 格式响应

---

## 三、SCService WSDL 分析

### SCService.wsdl（WSDL协议/SCService.wsdl）

| 字段 | 值 |
|------|-----|
| targetNamespace | `http://SCService.chinatowercom.com` |
| service name | `SCServiceService` |
| port name | `SCService` |
| portType name | `SCService` |
| **operation** | **`invoke`**（仅 1 个） |
| input message | `invokeRequest(xmlData: soapenc:string)` |
| output message | `invokeResponse(invokeReturn: soapenc:string)` |
| **style** | **`rpc`** |
| **use** | **`encoded`** |
| **SOAPAction** | **`""`** |
| endpoint path | `/services/SCService` |
| endpoint location | `http://127.0.0.1:8080/services/SCService` |

### RPC Request 格式（FSU → SC）

```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
                   xmlns:SOAP-ENC="http://schemas.xmlsoap.org/soap/encoding/"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns:ns1="http://SCService.chinatowercom.com">
  <SOAP-ENV:Body>
    <ns1:invoke>
      <xmlData xsi:type="SOAP-ENC:string">
        <Request>
          <PK_Type>LOGIN</PK_Type>
          <Info>...</Info>
          <xmlData>...</xmlData>
        </Request>
      </xmlData>
    </ns1:invoke>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

### RPC Response 格式（SC → FSU）

```xml
<SOAP-ENV:Envelope ... xmlns:ns1="http://SCService.chinatowercom.com">
  <SOAP-ENV:Body>
    <ns1:invokeResponse>
      <invokeReturn xsi:type="SOAP-ENC:string">
        <Response>
          <PK_Type>LOGIN</PK_Type>
          <Info><ResultCode>0</ResultCode><SessionID>...</SessionID></Info>
          <xmlData/>
        </Response>
      </invokeReturn>
    </ns1:invokeResponse>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

### 与 FSUService 的唯一差异

| 字段 | SCService | FSUService |
|------|-----------|-----------|
| targetNamespace | `http://SCService.chinatowercom.com` | `http://FSUService.chinatowercom.com` |
| endpoint path | `/services/SCService` | `/services/FSUService` |
| 角色 | SC 是服务端 | FSU 是服务端 |

---

## 四、当前入站解析能力

### 4.1 入站链路（FSU → SC）

```
HTTP POST /api/b-interface/sc-service
  → ScServiceController.handleScService()
    → SoapMessageHandler.parse()          ← 解析 SOAP → BInterfaceMessage
      → CommandDispatcher.dispatch()      ← 路由到 CommandHandler
        → CommandHandler.handle()          ← 处理业务逻辑
          → CommandResult                 ← 结果
    → SoapMessageHandler.buildResponse()  ← 构造 SOAP 响应
```

### 4.2 parse() — 入站解析 ✅ 已双向兼容

BIF-P4-005-B 中已实现双向解析：

| 格式 | 支持 | 实现 |
|------|------|------|
| document-style `<Request>` | ✅ | 原有逻辑 |
| RPC `<ns1:invoke><xmlData><Request>` | ✅ | extractInnerRoot() 自动检测 |
| RPC `<ns1:invokeResponse><invokeReturn><Response>` | ✅ | 同上 |
| SOAP Fault | ✅ | 原有逻辑 |

### 4.3 buildResponse() — 出站响应 ⚠️ 仍为 document-style

当前 buildResponse() 输出：

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>LOGIN</PK_Type>
      <Info><ResultCode>0</ResultCode>...</Info>
      <xmlData/>
    </Response>
  </soap:Body>
</soap:Envelope>
```

**WSDL 要求**：

```xml
<SOAP-ENV:Envelope ... xmlns:ns1="http://SCService.chinatowercom.com">
  <SOAP-ENV:Body>
    <ns1:invokeResponse>
      <invokeReturn xsi:type="SOAP-ENC:string">
        <Response>...</Response>
      </invokeReturn>
    </ns1:invokeResponse>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

**差异**：buildResponse() 缺少 RPC wrapper (`<ns1:invokeResponse><invokeReturn>`)。

### 4.4 buildFault() — ⚠️ 同样不兼容 RPC

SOAP Fault 虽然结构标准，但如果 FSU 期望 RPC 格式的响应，应该在 `invokeResponse` 内部返回 Fault，而非顶层 Fault。

---

## 五、document-style 与 rpc-style 差异

| 项目 | document-style（当前） | RPC-style（WSDL要求） |
|------|----------------------|---------------------|
| 入站请求解析 | ✅ parse() 支持 | ✅ parse() 支持 |
| 入站响应解析 | ✅ parse() 支持 | ✅ parse() 支持 |
| 出站请求构造 | N/A (FSUService 已适配) | ✅ FsuServiceRpcAdapter |
| **出站响应构造** | ✅ **当前行为** | ❌ **未实现** |

---

## 六、是否需要 ScServiceRpcAdapter

### 当前决策：**暂不实现，文档标记为待办**

理由：

| 因素 | 分析 |
|------|------|
| 入站解析 | ✅ 已覆盖（parse 双向兼容） |
| 出站响应 | ⚠️ 未覆盖，但未验证真实 FSU 是否拒绝 |
| 真实 FSU 入站 | 未测试（FSU 尚未配置向平台上报） |
| 风险 | 中等：如果 FSU 上报时拒收 document response，需紧急修复 |
| 实现成本 | 低：与 FsuServiceRpcAdapter 类似，构造函数传入 SC 命名空间 |
| 测试影响 | 低：可复用现有 SCService fixtures（parse 兼容） |

### 触发条件

以下任一条件满足时，实施 SCService RPC 适配：

1. 真实 FSU 入站联调时，FSU 返回 `Method 'Response' not implemented` Fault
2. FSU 管理方确认 FSU 仅接受 RPC 格式的 SCService 响应
3. Wireshark/抓包显示 FSU 上报告警/心跳的 SOAP 格式为 RPC

### 实现方案（当触发时）

**方案：扩展 SoapMessageHandler，新增 buildRpcResponse()**

```java
public String buildRpcResponseForScService(String pkType, String info, String xmlData) {
    // 构造内层 <Response> payload
    // 包装为 SCService RPC: <ns1:invokeResponse><invokeReturn>...</invokeReturn></ns1:invokeResponse>
}
```

与 FsuServiceRpcAdapter.wrapRequestPayload() 逻辑对应，但：
- namespace 用 `http://SCService.chinatowercom.com`
- 根元素用 `invokeResponse` / `invokeReturn`
- 内层用 `<Response>`（非 `<Request>`）

---

## 七、测试方案

### 新增测试：ScServiceRpcCompatibilityTest

| # | 测试 | 覆盖 |
|---|------|------|
| 1 | parse RPC LOGIN request | `<ns1:invoke><xmlData><Request><PK_Type>LOGIN</PK_Type>...` |
| 2 | parse RPC HEARTBEAT request | 同上模式，PK_Type=HEARTBEAT |
| 3 | parse RPC SEND_DATA request | 同上，含 Signal 数组 |
| 4 | parse RPC SEND_ALARM request | 同上，含 Alarm 数组 |
| 5 | RPC round-trip (LOGIN) | buildRpcResponse → parse 互为逆操作 |
| 6 | document fixtures unchanged | 现有 sc_service/*.xml 仍正常解析 |
| 7 | FSUService RPC unchanged | FsuServiceRpcAdapter 不受影响 |
| 8 | no SET command execution | 不执行 SET 类命令 |

### 回归要求

| 测试集 | 状态 |
|--------|------|
| SoapMessageHandlerTest (71) | 必须全通过 |
| FsuServiceRpcAdapterTest (27) | 必须全通过 |
| CommandDispatcherTest (21) | 必须全通过 |
| BInterfaceMainFlowIntegrationTest (23) | 必须全通过 |
| BifP4005RealFsuIntegrationTest (5) | 不访问真实设备 |
| 全量回归 (791+) | 0 failures |

---

## 八、对主链路风险

| 链路 | 风险 | 缓解 |
|------|------|------|
| LOGIN 入站 | 低 | parse() 已双向兼容 |
| HEARTBEAT 入站 | 低 | 同上 |
| SEND_DATA 入站 | 低 | 同上 |
| SEND_ALARM 入站 | 低 | 同上 |
| SC 响应出站 | **中** | 如果 FSU 拒收 document response，需增加 RPC 响应 |
| SC Fault 出站 | **中** | 同上 |
| FSUService 出站 | 低 | FsuServiceRpcAdapter 已独立，不受影响 |
| StubFsuServiceClient | 低 | 零修改 |

---

## 九、安全边界

| 检查项 | 状态 |
|--------|------|
| 访问真实设备 | ❌ 否 |
| real-call-enabled | false |
| Scheduler | false |
| SET 命令 | ❌ 否 |
| 修改前端 | ❌ 否 |

---

## 十、遗留问题

1. **SCService 出站响应格式未验证**：未测试真实 FSU 是否拒绝 document-style response
2. **FSU 入站场景未联调**：FSU 尚未配置向平台 SCService 上报
3. **SOAP Fault 兼容性未验证**：RPC 格式下的 Fault 应该放在 invokeResponse 内部还是顶层

---

## 十一、下一步建议

### 优先：BIF-P4-009（收到 FSU 管理方点位后）
- 生成 seed SQL
- 用真实 SignalID 重新联调 GET_DATA/GET_THRESHOLD

### 并行：BIF-P4-010（当 FSU 配置上报后）
- 配置 FSU 向平台 SCService 上报
- 抓包确认 FSU 入站 SOAP 格式
- 如果是 RPC 格式且 SC response 被拒绝 → 实施 buildRpcResponse()
