# SET_POINT

## 1. 基本信息

| 项 | 值 |
|---|---|
| 命令名称 | SET_POINT |
| 请求 Code | 1001 |
| 响应名称 | SET_POINT_ACK |
| 响应 Code | 1002 |
| 通信方向 | SC -> FSU |
| 协议版本 | B接口2016 |
| 命令类别 | 控制 |
| 当前实现状态 | 安全禁用 |
| 安全等级 | 控制类高风险 |

## 2. 协议原文引用

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | B接口报文协议 / 数据流格式定义 |
| 原文位置 | 表20-21，抽取稿 L1763-L1951；流程 L534-L544 |
| 原文摘要 | SC向FSU写监控点设置值，响应返回成功/失败Id列表及Result。 |

## 3. SPEC 规则列表

| SPEC 编号 | 规则摘要 | 类型 |
|---|---|---|
| SPEC-2016-CMD-SET-POINT-001 | SET_POINT请求命令码1001，SET_POINT_ACK响应命令码1002 | 协议规定 |
| SPEC-2016-CMD-SET-POINT-002 | SET_POINT请求Value.DeviceList下携带TSemaphore设置值 | 协议规定 |
| SPEC-2016-SECURITY-SET-POINT-001 | SET_POINT为遥控/遥调控制类高风险命令，当前阶段默认禁用 | 工程解释 |
| SPEC-2016-CMD-SET-POINT-003 | SET_POINT_ACK按Device返回SuccessList和FailList | 协议规定 |

## 3.1 SPEC 规则明细

### [SPEC-2016-CMD-SET-POINT-001] SET_POINT请求命令码1001，SET_POINT_ACK响应命令码1002

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表20-21，抽取稿 L1763-L1951；流程 L534-L544  
原文摘要：SC向FSU写监控点设置值，响应返回成功/失败Id列表及Result。  
规则内容：SET_POINT请求命令码1001，SET_POINT_ACK响应命令码1002。  
适用范围：SET_POINT / SET_POINT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-SET-POINT-002] SET_POINT请求Value.DeviceList下携带TSemaphore设置值

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表20-21，抽取稿 L1763-L1951；流程 L534-L544  
原文摘要：SC向FSU写监控点设置值，响应返回成功/失败Id列表及Result。  
规则内容：SET_POINT请求Value.DeviceList下携带TSemaphore设置值。  
适用范围：SET_POINT / SET_POINT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### 安全引用：SPEC-2016-SECURITY-SET-POINT-001 SET_POINT为遥控/遥调控制类高风险命令，当前阶段默认禁用

规则类型：工程解释  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表20-21，抽取稿 L1763-L1951；流程 L534-L544  
原文摘要：SC向FSU写监控点设置值，响应返回成功/失败Id列表及Result。  
规则内容：SET_POINT为遥控/遥调控制类高风险命令，当前阶段默认禁用。  
适用范围：SET_POINT / SET_POINT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-SET-POINT-003] SET_POINT_ACK按Device返回SuccessList和FailList

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表20-21，抽取稿 L1763-L1951；流程 L534-L544  
原文摘要：SC向FSU写监控点设置值，响应返回成功/失败Id列表及Result。  
规则内容：SET_POINT_ACK按Device返回SuccessList和FailList。  
适用范围：SET_POINT / SET_POINT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。

## 4. 请求 XML 结构

```xml
<Request>
  <PK_Type><Name>SET_POINT</Name><Code>1001</Code></PK_Type>
  <Info><FsuId/><FsuCode/><Value><DeviceList><Device Id="000000000001" Code="000000000001"><TSemaphore Type="" Id="" MeasuredVal="" SetupVal="" Status=""/></Device></DeviceList></Value></Info>
</Request>
```

## 5. 响应 XML 结构

```xml
<Response>
  <PK_Type><Name>SET_POINT_ACK</Name><Code>1002</Code></PK_Type>
  <Info><FsuId/><FsuCode/><Result/><DeviceList><Device Id="000000000001" Code="000000000001"><SuccessList><Id/></SuccessList><FailList><Id/></FailList></Device></DeviceList></Info>
</Response>
```

## 6. 字段定义

| 字段 | 类型 | 必填 | 来源位置 | 说明 | 备注 |
|---|---|---|---|---|---|
| FsuId | char[FSUID_LEN] | 是 | 表20-21，抽取稿 L1763-L1951；流程 L534-L544 | FSU ID |  |
| FsuCode | char[FSUCODE_LEN] | 是 | 表20-21，抽取稿 L1763-L1951；流程 L534-L544 | FSU编码 |  |
| Value/TSemaphore | TSemaphore | 是 | 表20-21，抽取稿 L1763-L1951；流程 L534-L544 | 设置值 |  |
| SuccessList/Id | long/ID_LENGTH | 否 | 表20-21，抽取稿 L1763-L1951；流程 L534-L544 | 写成功ID列表 |  |
| FailList/Id | long/ID_LENGTH | 否 | 表20-21，抽取稿 L1763-L1951；流程 L534-L544 | 写失败ID列表 |  |
| Result | EnumResult | 是 | 表20-21，抽取稿 L1763-L1951；流程 L534-L544 | 写成功/失败 |  |

## 7. 处理规则

平台处理 `SET_POINT` 时必须先解析SOAP `xmlData`，再校验业务根节点、PK_Type Name+Code、Info层级和字段大小写。业务处理不得覆盖原始报文，raw request/response 必须留痕。

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
| Value/TSemaphore | `value/TSemaphore` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| SuccessList/Id | `successList/Id` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| FailList/Id | `failList/Id` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Result | `result` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |

## 11. XML 样例索引

| 样例类型 | 文件 / 原文位置 | 说明 |
|---|---|---|
| XML-2016-011 | matrices/xml-sample-index.md | 原文样例索引 |
| XML-2016-012 | matrices/xml-sample-index.md | 原文样例索引 |

## 12. 已知厂商差异

未执行Emerson SET_POINT，禁止用实测推断。

## 13. 安全边界

默认禁用；必须显式授权、二次确认、操作审计、raw报文保存，禁止默认调度执行。

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
