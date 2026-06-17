# LOGIN

## 1. 基本信息

| 项 | 值 |
|---|---|
| 命令名称 | LOGIN |
| 请求 Code | 101 |
| 响应名称 | LOGIN_ACK |
| 响应 Code | 102 |
| 通信方向 | FSU -> SC |
| 协议版本 | B接口2016 |
| 命令类别 | 登录 |
| 当前实现状态 | 部分实现 |
| 安全等级 | 只读/认证 |

## 2. 协议原文引用

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | B接口报文协议 / 数据流格式定义 |
| 原文位置 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 |
| 原文摘要 | FSU向SC注册，携带用户名、口令、内网IP、FSU能力和DeviceList；SC返回权限RightLevel和SCIP。 |

## 3. SPEC 规则列表

| SPEC 编号 | 规则摘要 | 类型 |
|---|---|---|
| SPEC-2016-CMD-LOGIN-001 | LOGIN请求命令码为101，响应LOGIN_ACK为102 | 协议规定 |
| SPEC-2016-CMD-LOGIN-002 | LOGIN请求Info包含UserName、PaSCword、FsuId、FsuCode、FsuIP、网络模块、FSU厂商型号、Version/DictVersion和DeviceList | 协议规定 |
| SPEC-2016-CMD-LOGIN-003 | 同一个FSU两次注册之间的最小时间间隔不小于120秒 | 协议规定 |
| SPEC-2016-CMD-LOGIN-004 | 新注册模式中LOGIN_ACK同时返回采集机IP SCIP；原注册模式需后续SET_LOGININFO下发采集机信息 | 协议规定 |
| SPEC-2016-CMD-LOGIN-005 | 样例字段Vervion与表格Version不一致，按UNKNOWN登记，不擅自归一为协议规定 | 待确认 |

## 3.1 SPEC 规则明细

### [SPEC-2016-CMD-LOGIN-001] LOGIN请求命令码为101，响应LOGIN_ACK为102

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297  
原文摘要：FSU向SC注册，携带用户名、口令、内网IP、FSU能力和DeviceList；SC返回权限RightLevel和SCIP。  
规则内容：LOGIN请求命令码为101，响应LOGIN_ACK为102。  
适用范围：LOGIN / LOGIN_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-LOGIN-002] LOGIN请求Info包含UserName、PaSCword、FsuId、FsuCode、FsuIP、网络模块、FSU厂商型号、Version/DictVersion和DeviceList

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297  
原文摘要：FSU向SC注册，携带用户名、口令、内网IP、FSU能力和DeviceList；SC返回权限RightLevel和SCIP。  
规则内容：LOGIN请求Info包含UserName、PaSCword、FsuId、FsuCode、FsuIP、网络模块、FSU厂商型号、Version/DictVersion和DeviceList。  
适用范围：LOGIN / LOGIN_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-LOGIN-003] 同一个FSU两次注册之间的最小时间间隔不小于120秒

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297  
原文摘要：FSU向SC注册，携带用户名、口令、内网IP、FSU能力和DeviceList；SC返回权限RightLevel和SCIP。  
规则内容：同一个FSU两次注册之间的最小时间间隔不小于120秒。  
适用范围：LOGIN / LOGIN_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-LOGIN-004] 新注册模式中LOGIN_ACK同时返回采集机IP SCIP；原注册模式需后续SET_LOGININFO下发采集机信息

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297  
原文摘要：FSU向SC注册，携带用户名、口令、内网IP、FSU能力和DeviceList；SC返回权限RightLevel和SCIP。  
规则内容：新注册模式中LOGIN_ACK同时返回采集机IP SCIP；原注册模式需后续SET_LOGININFO下发采集机信息。  
适用范围：LOGIN / LOGIN_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-LOGIN-005] 样例字段Vervion与表格Version不一致，按UNKNOWN登记，不擅自归一为协议规定

规则类型：待确认  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297  
原文摘要：FSU向SC注册，携带用户名、口令、内网IP、FSU能力和DeviceList；SC返回权限RightLevel和SCIP。  
规则内容：样例字段Vervion与表格Version不一致，按UNKNOWN登记，不擅自归一为协议规定。  
适用范围：LOGIN / LOGIN_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。

## 4. 请求 XML 结构

```xml
<Request>
  <PK_Type><Name>LOGIN</Name><Code>101</Code></PK_Type>
  <Info>
    <UserName>cntower</UserName>
    <PaSCword>cntower</PaSCword>
    <FsuId/>
    <FsuCode/>
    <FsuIP/>
    <MacId/>
    <ImsiId/>
    <NetworkType/>
    <LockedNetworkType/>
    <Carrier/>
    <NMVendor/>
    <NMType/>
    <Reg_Mode/>
    <FSUVendor/>
    <FSUType/>
    <FSUClass/>
    <Vervion/>
    <DictVersion/>
    <DeviceList>
      <Device Id="" Code=""/>
    </DeviceList>
  </Info>
</Request>
```

## 5. 响应 XML 结构

```xml
<Response>
  <PK_Type><Name>LOGIN_ACK</Name><Code>102</Code></PK_Type>
  <Info>
    <SCIP/>
    <RightLevel/>
  </Info>
</Response>
```

## 6. 字段定义

| 字段 | 类型 | 必填 | 来源位置 | 说明 | 备注 |
|---|---|---|---|---|---|
| UserName | USER_LENGTH | 是 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 用户名 |  |
| PaSCword | PASSWORD_LEN | 是 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 口令，原文拼写为PaSCword |  |
| FsuId | char[FSUID_LEN] | 是 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 资源系统FSU ID |  |
| FsuCode | char[FSUID_LEN]/FSUCODE_LEN | 是 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | FSU编码，表格类型与常量名不一致 | 详见UNKNOWN |
| FsuIP | IP_LENGTH | 是 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | FSU内网IP |  |
| MacId | MAC_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 无线模块MAC |  |
| ImsiId | IMSI_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | IMSI卡号 |  |
| NetworkType | NETWORKTYPE_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 当前网络制式 |  |
| LockedNetworkType | NETWORKTYPE_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 锁定网络制式 |  |
| Carrier | CARRIER_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 运营商CT/CM/CU |  |
| NMVendor | NMVENDOR_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 上网模块厂商 |  |
| NMType | NMTYPE_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 上网模块型号 |  |
| Reg_Mode | REG_MODE_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 注册模式 |  |
| FSUVendor | FSUVENDOR_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | FSU厂家 |  |
| FSUType | FSUTYPE_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | FSU型号 |  |
| FSUClass | FSUCLASS_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | FSU应用类型 |  |
| Version/Vervion | VERSION_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 软件版本，样例拼写为Vervion |  |
| DictVersion | DICTVERSION_LENGTH | 否 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | 信号字典版本 |  |
| DeviceList/Device@Id/Code | n*DEVICEID_LEN | 是 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | FSU所接设备列表 |  |
| RightLevel | EnumRightMode | 是 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | SC返回权限 |  |
| SCIP | IP_LENGTH | 是 | 表10-11，抽取稿 L946-L1125；流程 L485-L495、L3291-L3297 | SC采集机IP |  |

## 7. 处理规则

平台处理 `LOGIN` 时必须先解析SOAP `xmlData`，再校验业务根节点、PK_Type Name+Code、Info层级和字段大小写。业务处理不得覆盖原始报文，raw request/response 必须留痕。

### 7.1 注册 120 秒间隔边界

`LOGIN` 流程中的客户端为 FSU，服务端为 SC；`FsuIP` 是 FSU 注册时上报的内网 IP，可作为 SC 后续访问 FSUService 的 registration context 来源。

同一个 FSU 两次注册之间的最小时间间隔不小于 120 秒，该限制约束 `LOGIN` 注册行为，不是 `GET_DATA` 慢数据查询频率限制。工程实现应对 120 秒内重复 `LOGIN` 做幂等处理或节流记录，避免无条件覆盖稳定的 endpoint、DeviceList 或 session 状态；同时不得因为重复注册风险而放宽 `GET_DATA` 的协议校验。

2026-05-28 只读 `b_interface_message_log` 发现真实 FSU `51051243812345` 存在 32/33 秒间隔的重复 `LOGIN`，低于协议要求。该现象登记为实现/运维风险，不能写成 `GET_DATA` 必须等待 120 秒的协议规则。

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
| UserName | `userName` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| PaSCword | `paSCword` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| FsuId | `fsuId` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| FsuCode | `fsuCode` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| FsuIP | `fsuIP` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| MacId | `macId` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| ImsiId | `imsiId` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| NetworkType | `networkType` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| LockedNetworkType | `lockedNetworkType` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Carrier | `carrier` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| NMVendor | `nMVendor` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| NMType | `nMType` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |

## 11. XML 样例索引

| 样例类型 | 文件 / 原文位置 | 说明 |
|---|---|---|
| XML-2016-001 | matrices/xml-sample-index.md | 原文样例索引 |
| XML-2016-002 | matrices/xml-sample-index.md | 原文样例索引 |

## 12. 已知厂商差异

Emerson实测：LOGIN可返回DeviceList能力，项目已解析Device.Id/Device.Code属性形式；详见 profiles/emerson-2016.md。

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
