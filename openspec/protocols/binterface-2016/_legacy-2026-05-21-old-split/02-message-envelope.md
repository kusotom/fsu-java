# 02 — SOAP/XML 报文结构

## 1. SOAP 约束

| 特征 | 值 |
|------|-----|
| SOAP 版本 | 1.1 |
| WSDL 版本 | 1.1 |
| 风格 | RPC（非 Document） |
| 编码 | soapenc:encoded（非 literal） |
| SOAPAction | 空字符串 `""` |
| 操作 | `invoke(xmlData: soapenc:string) → invokeReturn: soapenc:string` |

## 2. 命名空间

| 前缀 | 命名空间 | 用途 |
|------|---------|------|
| soap | `http://schemas.xmlsoap.org/soap/envelope/` | SOAP Envelope |
| soapenc | `http://schemas.xmlsoap.org/soap/encoding/` | SOAP 编码 |
| impl | `http://SCService.chinatowercom.com` | SC 命名空间 |
| impl | `http://FSUService.chinatowercom.com` | FSU 命名空间 |

## 3. 通用消息结构

```
SOAP Envelope
  └── SOAP Body
       ├── Request / Response          ← 根元素
       │   ├── PK_Type                 ← 命令码
       │   ├── Info                    ← 元数据
       │   └── xmlData                 ← 业务数据
```

## 4. 请求报文模板

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>COMMAND_CODE</PK_Type>
      <Info>
        <FSUCode>FSU-XXX</FSUCode>
        <!-- 命令特有字段 -->
      </Info>
      <xmlData>
        <!-- 命令特有业务数据 -->
      </xmlData>
    </Request>
  </soap:Body>
</soap:Envelope>
```

## 5. 响应报文模板

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Response>
      <PK_Type>COMMAND_CODE</PK_Type>
      <Info>
        <ResultCode>0</ResultCode>
        <!-- 命令特有字段 -->
      </Info>
      <xmlData>
        <!-- 命令特有返回数据 -->
      </xmlData>
    </Response>
  </soap:Body>
</soap:Envelope>
```

## 6. 消息方向标识

- **Request** — SOAP Body 根元素为 `<Request>`
- **Response** — SOAP Body 根元素为 `<Response>`

## 7. PK_Type 格式

### 7.1 2016 标准格式（纯文本）

```xml
<PK_Type>LOGIN</PK_Type>
```

### 7.2 2024 Name+Code 格式

```xml
<PK_Type>
  <Name>LOGIN</Name>
  <Code>101</Code>
</PK_Type>
```

### 7.3 Emerson FSU 实测兼容

Emerson FSU 接受 Name+Code 格式（`legacy-2016` profile）。LANDING-006 验证通过。

## 8. SOAP Fault

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <soap:Fault>
      <faultcode>soap:Server</faultcode>
      <faultstring>服务器内部错误</faultstring>
      <detail>
        <error>
          <code>5001</code>
          <message>数据库连接失败</message>
        </error>
      </detail>
    </soap:Fault>
  </soap:Body>
</soap:Envelope>
```

| 字段 | 说明 |
|------|------|
| faultcode | soap:Server / soap:Client / soap:MustUnderstand |
| faultstring | 人类可读错误信息 |
| detail | 业务错误码和消息 |

## 9. 平台实现

| 组件 | 文件 | 说明 |
|------|------|------|
| SOAP 解析 | `SoapMessageHandler.java` | 双向兼容 RPC invoke 和 document-style |
| SOAP 构建 | `SoapMessageHandler.buildResponse()` | document-style 格式 |
| SCService 入站 | `ScServiceController.java` | 接收 FSU 上报 |
| FSUService 出站 | `FsuServiceClient.call()` | 调用 FSU |
