# SEND_ALARM

## 1. 基本信息

| 项 | 值 |
|---|---|
| 命令名称 | SEND_ALARM |
| 请求 Code | 501 |
| 响应名称 | SEND_ALARM_ACK |
| 响应 Code | 502 |
| 通信方向 | FSU -> SC |
| 协议版本 | B接口2016 |
| 命令类别 | 告警 |
| 当前实现状态 | 部分实现 |
| 安全等级 | 只读/被动接收 |

## 2. 协议原文引用

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | B接口报文协议 / 数据流格式定义 |
| 原文位置 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 |
| 原文摘要 | FSU根据告警门限判断有告警需上报时，向SC上报告警信息，SC返回确认。 |

## 3. SPEC 规则列表

| SPEC 编号 | 规则摘要 | 类型 |
|---|---|---|
| SPEC-2016-CMD-SEND-ALARM-001 | SEND_ALARM请求命令码501，SEND_ALARM_ACK响应命令码502 | 协议规定 |
| SPEC-2016-CMD-SEND-ALARM-002 | 请求Info.Values.TAlarmList包含一个或多个TAlarm | 协议规定 |
| SPEC-2016-CMD-SEND-ALARM-003 | 告警序号10位数字，不足补0，0~4294967295范围，告警结束与开始序号相同 | 协议规定 |
| SPEC-2016-CMD-SEND-ALARM-004 | 所有文本描述中不能包含半角<或>字符 | 协议规定 |

## 3.1 SPEC 规则明细

### [SPEC-2016-CMD-SEND-ALARM-001] SEND_ALARM请求命令码501，SEND_ALARM_ACK响应命令码502

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428  
原文摘要：FSU根据告警门限判断有告警需上报时，向SC上报告警信息，SC返回确认。  
规则内容：SEND_ALARM请求命令码501，SEND_ALARM_ACK响应命令码502。  
适用范围：SEND_ALARM / SEND_ALARM_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-SEND-ALARM-002] 请求Info.Values.TAlarmList包含一个或多个TAlarm

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428  
原文摘要：FSU根据告警门限判断有告警需上报时，向SC上报告警信息，SC返回确认。  
规则内容：请求Info.Values.TAlarmList包含一个或多个TAlarm。  
适用范围：SEND_ALARM / SEND_ALARM_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-SEND-ALARM-003] 告警序号10位数字，不足补0，0~4294967295范围，告警结束与开始序号相同

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428  
原文摘要：FSU根据告警门限判断有告警需上报时，向SC上报告警信息，SC返回确认。  
规则内容：告警序号10位数字，不足补0，0~4294967295范围，告警结束与开始序号相同。  
适用范围：SEND_ALARM / SEND_ALARM_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。
### [SPEC-2016-CMD-SEND-ALARM-004] 所有文本描述中不能包含半角<或>字符

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428  
原文摘要：FSU根据告警门限判断有告警需上报时，向SC上报告警信息，SC返回确认。  
规则内容：所有文本描述中不能包含半角<或>字符。  
适用范围：SEND_ALARM / SEND_ALARM_ACK。  
影响模块：协议解析、XML构造、命令处理、测试fixture。  
实现要求：实现必须按本命令文件的Code、ACK Code、XML层级和字段大小写处理；如为待确认或工程解释，不得写成标准强约束。  
测试要求：建立请求/响应fixture、字段校验、错误返回和raw message留痕测试。  
备注：厂商差异只记录到Profile，不覆盖标准2016。

## 4. 请求 XML 结构

```xml
<Request>
  <PK_Type><Name>SEND_ALARM</Name><Code>501</Code></PK_Type>
  <Info><Values><TAlarmList><TAlarm>
    <SerialNo/>
    <Id/>
    <FsuId/>
    <FsuCode/>
    <DeviceId/>
    <DeviceCode/>
    <AlarmTime/>
    <AlarmLevel/>
    <AlarmFlag/>
    <AlarmDesc/>
  </TAlarm></TAlarmList></Values></Info>
</Request>
```

## 5. 响应 XML 结构

```xml
<Response>
  <PK_Type><Name>SEND_ALARM_ACK</Name><Code>502</Code></PK_Type>
  <Info><Result/></Info>
</Response>
```

## 6. 字段定义

| 字段 | 类型 | 必填 | 来源位置 | 说明 | 备注 |
|---|---|---|---|---|---|
| SerialNo | char[SERIALNO_LEN] | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | 告警序号 |  |
| Id | char[ID_LENGTH] | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | 监控点ID |  |
| FsuId | char[FSUID_LEN] | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | FSU ID |  |
| FsuCode | char[FSUCODE_LEN] | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | FSU编码 |  |
| DeviceId | char[DEVICEID_LEN] | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | 设备ID |  |
| DeviceCode | char[DEVICECODE_LEN] | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | 设备编码 |  |
| AlarmTime | char[DES_LENGTH] | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | 告警时间 |  |
| AlarmLevel | EnumState/EnumAlarmLevel | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | 告警级别，结构表写EnumState |  |
| AlarmFlag | EnumFlag | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | 开始/结束 |  |
| AlarmDesc | char[DES_LENGTH] | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | 40字节以内告警描述 |  |
| Result | EnumResult | 是 | 表14-15，抽取稿 L1217-L1382；TAlarm定义 L321-L428 | 接收确认结果 |  |

## 7. 处理规则

平台处理 `SEND_ALARM` 时必须先解析SOAP `xmlData`，再校验业务根节点、PK_Type Name+Code、Info层级和字段大小写。业务处理不得覆盖原始报文，raw request/response 必须留痕。

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
| SerialNo | `serialNo` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Id | `id` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| FsuId | `fsuId` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| FsuCode | `fsuCode` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| DeviceId | `deviceId` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| DeviceCode | `deviceCode` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| AlarmTime | `alarmTime` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| AlarmLevel | `alarmLevel` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| AlarmFlag | `alarmFlag` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| AlarmDesc | `alarmDesc` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |
| Result | `result` | 待代码审计确认 | 协议字段映射占位，后续实现必须引用SPEC |

## 11. XML 样例索引

| 样例类型 | 文件 / 原文位置 | 说明 |
|---|---|---|
| XML-2016-005 | matrices/xml-sample-index.md | 原文样例索引 |
| XML-2016-006 | matrices/xml-sample-index.md | 原文样例索引 |

## 12. 已知厂商差异

真实SEND_ALARM样本仍需补齐；已有工程fixture不能替代真实厂商Profile。

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
