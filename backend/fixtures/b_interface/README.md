# B接口协议 2016 Fixtures 目录

## 用途

本目录包含 B接口协议 2016 的标准化 XML 测试夹具（fixtures），用于：

1. SOAP 报文解析测试（SoapMessageHandler）
2. XMLData 解析测试（XmlDataModel）
3. XMLData 构造测试（XmlDataModel.toXml）
4. 错误处理和边界条件测试
5. 协议兼容性回归测试

## 协议架构

B接口协议消息采用双层封装结构：

```
SOAP Envelope (WSDL: RPC style, soapenc:encoded)
  └── SOAP Body
       ├── Request / Response
       │   ├── PK_Type    — 命令码 (LOGIN, HEARTBEAT, ...)
       │   ├── Info       — 元数据 (FSUCode, ResultCode, ...)
       │   └── xmlData    — 业务数据 (各命令独有)
```

**WSDL 映射：**
- SCService: `POST /services/SCService` — `invoke(xmlData)` → `invokeReturn`
- FSUService: `POST /services/FSUService` — `invoke(xmlData)` → `invokeReturn`
- `xmlData` 参数类型为 `soapenc:string`，承载完整的 Request/Response XML 字符串

## 目录结构

```
fixtures/b_interface/
  README.md              ← 本文件
  soap/                  ← 完整 SOAP 报文 (Envelope + Body)
    sc_service/          ←   FSU→SC (快数据通道)
    fsu_service/         ←   SC→FSU (慢数据通道)
  xmldata/               ← 纯 xmlData 内容 (无 SOAP 封装)
  invalid/               ← 错误/异常报文样例
  expected/              ← 解析期望结果 (JSON)
```

## 文件命名规则

`{command}.{request|response}.xml`

- `login.request.xml` — LOGIN 请求
- `login.response.xml` — LOGIN 响应
- 全小写 + 下划线
- `request` = FSU→SC 或 SC→FSU 的请求方向
- `response` = 对应响应的方向

## Info 公共字段

所有请求 Info 包含：
- `FSUCode` — FSU 设备编码

所有响应 Info 包含：
- `ResultCode` — 0=成功, 1=失败

## 命名空间

```xml
xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
xmlns:soapenc="http://schemas.xmlsoap.org/soap/encoding/"
```

## 字段约束

- `FSUCode` / `DeviceID` / `SignalID` — 仅用于测试占位，不可用于生产
- `ResultCode` — 值域参考 B接口 2016 协议
- `MsgType` — 对应 BInterfacePkType 枚举

## 参考文档

- `WSDL协议/SCService.wsdl`
- `WSDL协议/FSUService.wsdl`
- `docs/protocol/b-interface-2016-summary.md`
- `docs/protocol/b-interface-command-map.md`
