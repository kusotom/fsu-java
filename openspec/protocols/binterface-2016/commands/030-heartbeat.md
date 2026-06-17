# HEARTBEAT

## 1. 基本信息

| 项 | 值 |
|---|---|
| 命令名称 | HEARTBEAT |
| 请求 Code | 未定义 |
| 响应名称 | HEARTBEAT_ACK |
| 响应 Code | 未定义 |
| 通信方向 | SC -> FSU（工程心跳使用GET_FSUINFO） |
| 协议版本 | B接口2016 |
| 命令类别 | 运维 |
| 当前实现状态 | 工程存在/标准未定义 |
| 安全等级 | 只读 |

## 2. 协议原文引用

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | B接口报文协议 / 数据流格式定义 |
| 原文位置 | 第9章SC心跳功能，抽取稿 L3251-L3254；GET_FSUINFO表36-37 L2992-L3102 |
| 原文摘要 | 原文未定义HEARTBEAT PK_Type；SC心跳功能通过WebService定期获取FSU状态信息实现，并要求FSUID一致才成功。工程上应映射到GET_FSUINFO 1701/1702或另列非标准兼容命令。 |

## 3. SPEC 规则列表

| SPEC 编号 | 规则摘要 | 类型 |
|---|---|---|
| SPEC-2016-CMD-HEARTBEAT-001 | B接口2016原文未在报文类型表定义HEARTBEAT命令码 | 待确认 |
| SPEC-2016-CMD-HEARTBEAT-002 | SC心跳功能要求定期获取FSU状态信息作为应用层心跳 | 协议规定 |
| SPEC-2016-CMD-HEARTBEAT-003 | FSU收到SC心跳报文需判断FSUID一致，一致返回成功，否则失败 | 协议规定 |
| SPEC-2016-CMD-HEARTBEAT-004 | 当前工程若存在HEARTBEAT入站命令，只能作为工程兼容/历史实现，不能作为标准2016命令 | 工程解释 |

## 3.1 SPEC 规则明细

### [SPEC-2016-CMD-HEARTBEAT-001] B接口2016原文未在报文类型表定义HEARTBEAT命令码

规则类型：待确认  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；第9章SC心跳功能，抽取稿 L3251-L3254；GET_FSUINFO表36-37 L2992-L3102  
原文摘要：原文未定义HEARTBEAT PK_Type；SC心跳功能通过WebService定期获取FSU状态信息实现，并要求FSUID一致才成功。工程上应映射到GET_FSUINFO 1701/1702或另列非标准兼容命令。  
规则内容：B接口2016原文未在报文类型表定义HEARTBEAT命令码。  
适用范围：HEARTBEAT / HEARTBEAT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-HEARTBEAT-002] SC心跳功能要求定期获取FSU状态信息作为应用层心跳

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；第9章SC心跳功能，抽取稿 L3251-L3254；GET_FSUINFO表36-37 L2992-L3102  
原文摘要：原文未定义HEARTBEAT PK_Type；SC心跳功能通过WebService定期获取FSU状态信息实现，并要求FSUID一致才成功。工程上应映射到GET_FSUINFO 1701/1702或另列非标准兼容命令。  
规则内容：SC心跳功能要求定期获取FSU状态信息作为应用层心跳。  
适用范围：HEARTBEAT / HEARTBEAT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-HEARTBEAT-003] FSU收到SC心跳报文需判断FSUID一致，一致返回成功，否则失败

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；第9章SC心跳功能，抽取稿 L3251-L3254；GET_FSUINFO表36-37 L2992-L3102  
原文摘要：原文未定义HEARTBEAT PK_Type；SC心跳功能通过WebService定期获取FSU状态信息实现，并要求FSUID一致才成功。工程上应映射到GET_FSUINFO 1701/1702或另列非标准兼容命令。  
规则内容：FSU收到SC心跳报文需判断FSUID一致，一致返回成功，否则失败。  
适用范围：HEARTBEAT / HEARTBEAT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-HEARTBEAT-004] 当前工程若存在HEARTBEAT入站命令，只能作为工程兼容/历史实现，不能作为标准2016命令

规则类型：工程解释  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；第9章SC心跳功能，抽取稿 L3251-L3254；GET_FSUINFO表36-37 L2992-L3102  
原文摘要：原文未定义HEARTBEAT PK_Type；SC心跳功能通过WebService定期获取FSU状态信息实现，并要求FSUID一致才成功。工程上应映射到GET_FSUINFO 1701/1702或另列非标准兼容命令。  
规则内容：当前工程若存在HEARTBEAT入站命令，只能作为工程兼容/历史实现，不能作为标准2016命令。  
适用范围：HEARTBEAT / HEARTBEAT_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。

## 4. 请求 XML 结构

```xml
<!-- 标准2016原文未提供HEARTBEAT XML样例；心跳建议引用GET_FSUINFO请求结构。 -->
```

## 5. 响应 XML 结构

```xml
<!-- 标准2016原文未提供HEARTBEAT_ACK XML样例；心跳建议引用GET_FSUINFO_ACK响应结构。 -->
```

## 6. 字段定义

| 字段 | 类型 | 必填 | 来源位置 | 说明 | 备注 |
|---|---|---|---|---|---|
| FsuId | char[FSUID_LEN] | 是 | 第9章SC心跳功能，抽取稿 L3251-L3254；GET_FSUINFO表36-37 L2992-L3102 | 用于FSUID一致性判断；心跳报文字段未单独定义 |  |
| FsuCode | char[FSUCODE_LEN] | 待确认 | 第9章SC心跳功能，抽取稿 L3251-L3254；GET_FSUINFO表36-37 L2992-L3102 | GET_FSUINFO含该字段 | 详见UNKNOWN |

## 7. 处理规则

平台处理 `HEARTBEAT` 时必须先解析SOAP `xmlData`，再校验业务根节点、PK_Type Name+Code、Info层级和字段大小写。业务处理不得覆盖原始报文，raw request/response 必须留痕。

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

## 11. XML 样例索引

| 样例类型 | 文件 / 原文位置 | 说明 |
|---|---|---|
| 无标准样例 | matrices/unknown-and-ambiguous-items.md | 原文未提供独立XML样例 |

## 12. 已知厂商差异

Emerson实测采用GET_FSUINFO 1701/1702作为只读状态/心跳轮询；未观察到标准HEARTBEAT命令码。

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
