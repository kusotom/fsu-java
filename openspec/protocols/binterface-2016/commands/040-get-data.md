# GET_DATA

## 1. 基本信息

| 项 | 值 |
|---|---|
| 命令名称 | GET_DATA |
| 请求 Code | 401 |
| 响应名称 | GET_DATA_ACK |
| 响应 Code | 402 |
| 通信方向 | SC -> FSU |
| 协议版本 | B接口2016 |
| 命令类别 | 采集 |
| 当前实现状态 | 部分实现 |
| 安全等级 | 只读 |

## 2. 协议原文引用

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | B接口报文协议 / 数据流格式定义 |
| 原文位置 | 表16-17，抽取稿 L1392-L1558；流程 L509-L520 |
| 原文摘要 | SC请求FSU监控点实时数据，按DeviceList和Id列表查询；设备Code全9表示该FSU所有设备，监控点Id全9表示该设备所有监控点。 |

## 3. SPEC 规则列表

| SPEC 编号 | 规则摘要 | 类型 |
|---|---|---|
| SPEC-2016-CMD-GET-DATA-001 | GET_DATA请求命令码401，GET_DATA_ACK响应命令码402 | 协议规定 |
| SPEC-2016-CMD-GET-DATA-002 | 请求Info包含FsuId、FsuCode、DeviceList/Device@Id/Code及Id列表 | 协议规定 |
| SPEC-2016-CMD-GET-DATA-003 | Device.Code=99999999999999时返回该FSU所有设备监控点值并忽略Id列表 | 协议规定 |
| SPEC-2016-CMD-GET-DATA-004 | Id=9999999999时返回该设备所有监控点值 | 协议规定 |
| SPEC-2016-CMD-GET-DATA-005 | 响应Values使用DeviceList/Device/TSemaphore结构 | 协议规定 |
| SPEC-2016-IMPL-GET-DATA-XMLDATA-EMERSON-001 | Emerson真实设备GET_DATA RPC调用使用escaped xmlData字符串承载Request | 实现注意事项 |

## 3.1 SPEC 规则明细

### [SPEC-2016-CMD-GET-DATA-001] GET_DATA请求命令码401，GET_DATA_ACK响应命令码402

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表16-17，抽取稿 L1392-L1558；流程 L509-L520  
原文摘要：SC请求FSU监控点实时数据，按DeviceList和Id列表查询；设备Code全9表示该FSU所有设备，监控点Id全9表示该设备所有监控点。  
规则内容：GET_DATA请求命令码401，GET_DATA_ACK响应命令码402。  
适用范围：GET_DATA / GET_DATA_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-GET-DATA-002] 请求Info包含FsuId、FsuCode、DeviceList/Device@Id/Code及Id列表

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表16-17，抽取稿 L1392-L1558；流程 L509-L520  
原文摘要：SC请求FSU监控点实时数据，按DeviceList和Id列表查询；设备Code全9表示该FSU所有设备，监控点Id全9表示该设备所有监控点。  
规则内容：请求Info包含FsuId、FsuCode、DeviceList/Device@Id/Code及Id列表。  
适用范围：GET_DATA / GET_DATA_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-GET-DATA-003] Device.Code=99999999999999时返回该FSU所有设备监控点值并忽略Id列表

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表16-17，抽取稿 L1392-L1558；流程 L509-L520  
原文摘要：SC请求FSU监控点实时数据，按DeviceList和Id列表查询；设备Code全9表示该FSU所有设备，监控点Id全9表示该设备所有监控点。  
规则内容：Device.Code=99999999999999时返回该FSU所有设备监控点值并忽略Id列表。  
适用范围：GET_DATA / GET_DATA_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-GET-DATA-004] Id=9999999999时返回该设备所有监控点值

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表16-17，抽取稿 L1392-L1558；流程 L509-L520  
原文摘要：SC请求FSU监控点实时数据，按DeviceList和Id列表查询；设备Code全9表示该FSU所有设备，监控点Id全9表示该设备所有监控点。  
规则内容：Id=9999999999时返回该设备所有监控点值。  
适用范围：GET_DATA / GET_DATA_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-GET-DATA-005] 响应Values使用DeviceList/Device/TSemaphore结构

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表16-17，抽取稿 L1392-L1558；流程 L509-L520  
原文摘要：SC请求FSU监控点实时数据，按DeviceList和Id列表查询；设备Code全9表示该FSU所有设备，监控点Id全9表示该设备所有监控点。  
规则内容：响应Values使用DeviceList/Device/TSemaphore结构。  
适用范围：GET_DATA / GET_DATA_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。

## 4. 请求 XML 结构

```xml
<Request>
  <PK_Type><Name>GET_DATA</Name><Code>401</Code></PK_Type>
  <Info>
    <FsuID/>
    <FsuCode/>
    <DeviceList>
      <Device Id="000000000001" Code="000000000001">
        <Id/>
      </Device>
    </DeviceList>
  </Info>
</Request>
```

## 5. 响应 XML 结构

```xml
<Response>
  <PK_Type><Name>GET_DATA_ACK</Name><Code>402</Code></PK_Type>
  <Info>
    <FsuId/>
    <FsuCode/>
    <Result/>
    <Values>
      <DeviceList>
        <Device Id="000000000001" Code="000000000001">
          <TSemaphore Type="" Id="" MeasuredVal="" SetupVal="" Status=""/>
        </Device>
      </DeviceList>
    </Values>
  </Info>
</Response>
```

## 6. 字段定义

| 字段 | 类型 | 必填 | 来源位置 | 说明 | 备注 |
|---|---|---|---|---|---|
| FsuId/FsuID | char[FSUID_LEN] | 是 | 表16-17，抽取稿 L1392-L1558；流程 L509-L520 | 请求样例使用FsuID，其他处多用FsuId，大小写差异进入UNKNOWN | 详见UNKNOWN |
| FsuCode | char[FSUCODE_LEN] | 是 | 表16-17，抽取稿 L1392-L1558；流程 L509-L520 | FSU编码 |  |
| Device@Id | char[DEVICEID_LEN] | 是 | 表16-17，抽取稿 L1392-L1558；流程 L509-L520 | 资源系统设备ID |  |
| Device@Code | char[DEVICECODE_LEN] | 是 | 表16-17，抽取稿 L1392-L1558；流程 L509-L520 | 设备编码，支持14位全9通配 |  |
| Id | char[ID_LENGTH] | 是 | 表16-17，抽取稿 L1392-L1558；流程 L509-L520 | 监控点ID，支持10位全9通配 |  |
| Result | EnumResult | 是 | 表16-17，抽取稿 L1392-L1558；流程 L509-L520 | 请求数据成功与否 |  |
| TSemaphore | TSemaphore | 否 | 表16-17，抽取稿 L1392-L1558；流程 L509-L520 | 实时监控点数据 |  |

## 7. 处理规则

平台处理 `GET_DATA` 时必须先解析SOAP `xmlData`，再校验业务根节点、PK_Type Name+Code、Info层级和字段大小写。业务处理不得覆盖原始报文，raw request/response 必须留痕。

### 7.1 Emerson 实测 RPC 承载要求

`GET_DATA` 的业务层结构仍按本文件：请求 Code=401，响应 ACK Code=402，请求 Device 子节点使用 `<Id>signalId</Id>`，响应 `Values/DeviceList/Device` 下使用 `TSemaphore`。

Emerson 真实 FSU 的 SOAP/RPC 承载层要求记录为实现注意事项：`xmlData` 使用 `xsi:type="xsd:string"` 的 escaped string，即 `&lt;Request&gt;...&lt;/Request&gt;`；SOAP Envelope 使用 `soap:` 前缀。该要求来源于 `BIF2016-RPCXML-001` 和 `data-mapping-008-retry-get-data-request.xml`，不得写成标准 B接口2016 的通用强制规则。

### 7.2 快慢数据归属

`GET_DATA` 属于 B接口2016 慢数据查询通道：SC 作为客户端轮询 FSU，FSU 作为 FSUService 服务端返回测点实时值。该归属来自通信模型中“SC轮询FSU获取温湿度、电压、电流、电量、频率、开关状态等慢数据”的定义。

`GET_DATA` 依赖有效注册上下文，但不触发 `LOGIN`，也不继承 `LOGIN` 的 120 秒注册间隔作为请求频率限制。工程上可以使用最近一次 `LOGIN` 的 `FsuIP`、`DeviceList` 和在线状态作为请求目标和能力来源；若 120 秒内重复 `LOGIN` 覆盖 registration context，应作为实现风险处理，而不是把 `GET_DATA` 改为快数据或暂停 120 秒。

## 8. 成功条件

- 请求Code与ACK Code符合本文件。
- XML层级符合Request/Response + PK_Type + Info。
- 响应中如有Result，则按EnumResult解释：1=SUCCESS，0=FAILURE。
- 对待确认字段仅做兼容解析，不写成标准强约束。

## 9. 失败条件

- PK_Type Name或Code不匹配。
- 必填字段缺失且无法按明确兼容规则解析。
- Result=0。
- XML格式非法、SOAP Fault、业务根节点错误。

## 10. 平台字段映射

| 协议字段 | 平台字段 | 数据表 / DTO / 实体 | 说明 |
|---|---|---|---|
| FsuId/FsuID | `fsuId/FsuID` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| FsuCode | `fsuCode` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Device@Id | `device@Id` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Device@Code | `device@Code` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Id | `id` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Result | `result` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| TSemaphore | `tSemaphore` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |

## 11. XML 样例索引

| 样例类型 | 文件 / 原文位置 | 说明 |
|---|---|---|
| XML-2016-007 | matrices/xml-sample-index.md | 原文样例索引 |
| XML-2016-008 | matrices/xml-sample-index.md | 原文样例索引 |

## 12. 已知厂商差异

Emerson实测：
- 旧结构或部分策略曾返回 GET_DATA_ACK 402 + Result=1 但 DeviceList 为空，只记入厂商Profile。
- 修正 `<Id>` request 后，显式 Id 查询已返回 8 个 TSemaphore；`0407102001` 返回 54.1/54.2V，`0407107001` 返回 0.0。
- 全 9 minimal/wildcard 读取超时和网络/代理间歇性可达属于厂商现场/运维现象，不改写标准通配规则。

## 13. 安全边界

只读或被动接收；仍需保存raw message并校验来源。

## 14. 测试要求

- fixture：标准请求、标准响应、缺字段、非法Code、Result=0/1。
- 单元测试：解析、构造、字段大小写、通配符和UNKNOWN分支。
- 集成测试：SOAP invoke(xmlData) envelope、raw message保存。
- 真实FSU测试：默认禁用，必须显式开关；SET类禁止真实执行。

## 15. 实现检查清单

- [ ] 命令码正确
- [ ] ACK 码正确
- [ ] XML 层级正确
- [ ] 字段大小写正确
- [ ] 必填字段校验
- [ ] 错误返回处理
- [ ] raw message 留痕
- [ ] 单元测试覆盖
- [ ] 集成测试覆盖
- [ ] 如涉及真实 FSU，保存 raw request/response
