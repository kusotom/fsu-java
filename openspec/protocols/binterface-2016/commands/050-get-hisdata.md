# GET_HISDATA

## 1. 基本信息

| 项 | 值 |
|---|---|
| 命令名称 | GET_HISDATA |
| 请求 Code | 403 |
| 响应名称 | GET_HISDATA_ACK |
| 响应 Code | 404 |
| 通信方向 | SC -> FSU |
| 协议版本 | B接口2016 |
| 命令类别 | 采集 |
| 当前实现状态 | 未实现 |
| 安全等级 | 只读 |

## 2. 协议原文引用

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | B接口报文协议 / 数据流格式定义 |
| 原文位置 | 表18-19，抽取稿 L1573-L1748；流程 L522-L532 |
| 原文摘要 | SC请求指定时间段内的监控点历史状态信息；原文说明按轮询周期1小时，一个轮询周期只取1个点。 |

## 3. SPEC 规则列表

| SPEC 编号 | 规则摘要 | 类型 |
|---|---|---|
| SPEC-2016-CMD-GET-HISDATA-001 | GET_HISDATA请求命令码403，GET_HISDATA_ACK响应命令码404 | 协议规定 |
| SPEC-2016-CMD-GET-HISDATA-002 | 请求增加StartTime和EndTime字段 | 协议规定 |
| SPEC-2016-CMD-GET-HISDATA-003 | 历史数据按轮询周期1小时，一个轮询周期只取1个点 | 协议规定 |
| SPEC-2016-CMD-GET-HISDATA-004 | 响应TSemaphore包含RecordTime属性 | 协议规定 |

## 3.1 SPEC 规则明细

### [SPEC-2016-CMD-GET-HISDATA-001] GET_HISDATA请求命令码403，GET_HISDATA_ACK响应命令码404

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表18-19，抽取稿 L1573-L1748；流程 L522-L532  
原文摘要：SC请求指定时间段内的监控点历史状态信息；原文说明按轮询周期1小时，一个轮询周期只取1个点。  
规则内容：GET_HISDATA请求命令码403，GET_HISDATA_ACK响应命令码404。  
适用范围：GET_HISDATA / GET_HISDATA_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-GET-HISDATA-002] 请求增加StartTime和EndTime字段

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表18-19，抽取稿 L1573-L1748；流程 L522-L532  
原文摘要：SC请求指定时间段内的监控点历史状态信息；原文说明按轮询周期1小时，一个轮询周期只取1个点。  
规则内容：请求增加StartTime和EndTime字段。  
适用范围：GET_HISDATA / GET_HISDATA_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-GET-HISDATA-003] 历史数据按轮询周期1小时，一个轮询周期只取1个点

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表18-19，抽取稿 L1573-L1748；流程 L522-L532  
原文摘要：SC请求指定时间段内的监控点历史状态信息；原文说明按轮询周期1小时，一个轮询周期只取1个点。  
规则内容：历史数据按轮询周期1小时，一个轮询周期只取1个点。  
适用范围：GET_HISDATA / GET_HISDATA_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-GET-HISDATA-004] 响应TSemaphore包含RecordTime属性

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表18-19，抽取稿 L1573-L1748；流程 L522-L532  
原文摘要：SC请求指定时间段内的监控点历史状态信息；原文说明按轮询周期1小时，一个轮询周期只取1个点。  
规则内容：响应TSemaphore包含RecordTime属性。  
适用范围：GET_HISDATA / GET_HISDATA_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。

## 4. 请求 XML 结构

```xml
<Request>
  <PK_Type><Name>GET_HISDATA</Name><Code>403</Code></PK_Type>
  <Info>
    <FsuId/>
    <FsuCode/>
    <StartTime/>
    <EndTime/>
    <DeviceList><Device Id="000000000001" Code="000000000001"><Id/></Device></DeviceList>
  </Info>
</Request>
```

## 5. 响应 XML 结构

```xml
<Response>
  <PK_Type><Name>GET_HISDATA_ACK</Name><Code>404</Code></PK_Type>
  <Info><FsuId/><FsuCode/><Result/><Values><DeviceList><Device Id="000000000001" Code="000000000001"><TSemaphore Type="" Id="" MeasuredVal="" SetupVal="" Status="" RecordTime=""/></Device></DeviceList></Values></Info>
</Response>
```

## 6. 字段定义

| 字段 | 类型 | 必填 | 来源位置 | 说明 | 备注 |
|---|---|---|---|---|---|
| FsuId | char[FSUID_LEN] | 是 | 表18-19，抽取稿 L1573-L1748；流程 L522-L532 | FSU ID |  |
| FsuCode | char[FSUCODE_LEN] | 是 | 表18-19，抽取稿 L1573-L1748；流程 L522-L532 | FSU编码 |  |
| StartTime | char[TIME_LEN] | 是 | 表18-19，抽取稿 L1573-L1748；流程 L522-L532 | 开始时间 |  |
| EndTime | char[TIME_LEN] | 是 | 表18-19，抽取稿 L1573-L1748；流程 L522-L532 | 结束时间 |  |
| Device@Id | char[DEVICEID_LEN] | 是 | 表18-19，抽取稿 L1573-L1748；流程 L522-L532 | 资源系统ID |  |
| Device@Code | char[DEVICECODE_LEN] | 是 | 表18-19，抽取稿 L1573-L1748；流程 L522-L532 | 设备编码 |  |
| Id | char[ID_LENGTH] | 是 | 表18-19，抽取稿 L1573-L1748；流程 L522-L532 | 监控点ID |  |
| RecordTime | char[DES_LENGTH] | 是 | 表18-19，抽取稿 L1573-L1748；流程 L522-L532 | 历史记录时间 |  |

## 7. 处理规则

平台处理 `GET_HISDATA` 时必须先解析SOAP `xmlData`，再校验业务根节点、PK_Type Name+Code、Info层级和字段大小写。业务处理不得覆盖原始报文，raw request/response 必须留痕。

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
| FsuId | `fsuId` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| FsuCode | `fsuCode` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| StartTime | `startTime` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| EndTime | `endTime` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Device@Id | `device@Id` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Device@Code | `device@Code` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Id | `id` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| RecordTime | `recordTime` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |

## 11. XML 样例索引

| 样例类型 | 文件 / 原文位置 | 说明 |
|---|---|---|
| XML-2016-009 | matrices/xml-sample-index.md | 原文样例索引 |
| XML-2016-010 | matrices/xml-sample-index.md | 原文样例索引 |

## 12. 已知厂商差异

未记录Emerson GET_HISDATA实测。

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
