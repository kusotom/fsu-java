# Common Fields

## 1. 来源

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | 数据结构定义 / 相关命令样例 |
| 原文位置 | 基本定义、常量定义、各命令字段表 |

## 2. 结构用途

跨命令公共字段归档。

## 3. 字段定义

| 字段 | 类型 | 必填 | 说明 | 所属命令 | SPEC 编号 | 备注 |
|---|---|---|---|---|---|---|
| FsuId/FSUID/FsuID | char[FSUID_LEN] | 按命令 | FSU资源ID | 全局 | SPEC-2016-FIELD-FSUID-001 | 大小写存在差异 |
| FsuCode | char[FSUCODE_LEN] | 按命令 | FSU编码 | 全局 | SPEC-2016-FIELD-FSUCODE-001 | 14字节 |
| DeviceId/DeviceID | char[DEVICEID_LEN] | 按命令 | 设备ID | 设备/告警 | SPEC-2016-FIELD-DEVICEID-001 | 大小写存在差异 |
| DeviceCode | char[DEVICECODE_LEN] | 按命令 | 设备编码 | 设备/告警 | SPEC-2016-FIELD-DEVICECODE-001 | 14字节 |
| Id/ID | char[ID_LENGTH] | 按命令 | 监控点ID | 采集/告警/门限 | SPEC-2016-FIELD-ID-001 | 10字节 |
| Result | EnumResult | 按ACK | 成功/失败 | ACK | SPEC-2016-FIELD-RESULT-001 | 1成功0失败 |
| DeviceList | 列表 | 按命令 | 设备列表 | 多命令 | SPEC-2016-FIELD-DEVICELIST-001 |  |
| Values/Value | 结构 | 按命令 | 值列表/写值 | 采集/SET | SPEC-2016-FIELD-VALUES-001 | GET响应用Values，SET请求用Value |

## 3.1 SPEC 规则明细

### [SPEC-2016-FIELD-FSUID-001] FsuId/FSUID/FsuID

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；基本定义、常量定义、各命令字段表  
原文摘要：FSU资源ID。  
规则内容：字段 `FsuId/FSUID/FsuID` 类型为 `char[FSUID_LEN]`，必填性：按命令。  
适用范围：全局。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：大小写存在差异

### [SPEC-2016-FIELD-FSUCODE-001] FsuCode

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；基本定义、常量定义、各命令字段表  
原文摘要：FSU编码。  
规则内容：字段 `FsuCode` 类型为 `char[FSUCODE_LEN]`，必填性：按命令。  
适用范围：全局。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：14字节

### [SPEC-2016-FIELD-DEVICEID-001] DeviceId/DeviceID

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；基本定义、常量定义、各命令字段表  
原文摘要：设备ID。  
规则内容：字段 `DeviceId/DeviceID` 类型为 `char[DEVICEID_LEN]`，必填性：按命令。  
适用范围：设备/告警。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：大小写存在差异

### [SPEC-2016-FIELD-DEVICECODE-001] DeviceCode

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；基本定义、常量定义、各命令字段表  
原文摘要：设备编码。  
规则内容：字段 `DeviceCode` 类型为 `char[DEVICECODE_LEN]`，必填性：按命令。  
适用范围：设备/告警。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：14字节

### [SPEC-2016-FIELD-ID-001] Id/ID

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；基本定义、常量定义、各命令字段表  
原文摘要：监控点ID。  
规则内容：字段 `Id/ID` 类型为 `char[ID_LENGTH]`，必填性：按命令。  
适用范围：采集/告警/门限。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：10字节

### [SPEC-2016-FIELD-RESULT-001] Result

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；基本定义、常量定义、各命令字段表  
原文摘要：成功/失败。  
规则内容：字段 `Result` 类型为 `EnumResult`，必填性：按ACK。  
适用范围：ACK。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：1成功0失败

### [SPEC-2016-FIELD-DEVICELIST-001] DeviceList

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；基本定义、常量定义、各命令字段表  
原文摘要：设备列表。  
规则内容：字段 `DeviceList` 类型为 `列表`，必填性：按命令。  
适用范围：多命令。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-FIELD-VALUES-001] Values/Value

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；基本定义、常量定义、各命令字段表  
原文摘要：值列表/写值。  
规则内容：字段 `Values/Value` 类型为 `结构`，必填性：按命令。  
适用范围：采集/SET。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：GET响应用Values，SET请求用Value


## 4. XML 示例

```xml
<!-- common fields are embedded in command XML samples -->
```

## 5. 平台映射

| 协议字段 | 平台字段 | 表 / DTO / Entity | 备注 |
|---|---|---|---|
| FsuId/FSUID/FsuID | `fsuid` | 待代码审计确认 | 后续实现需引用SPEC-2016-FIELD-FSUID-001 |
| FsuCode | `fsucode` | 待代码审计确认 | 后续实现需引用SPEC-2016-FIELD-FSUCODE-001 |
| DeviceId/DeviceID | `deviceid` | 待代码审计确认 | 后续实现需引用SPEC-2016-FIELD-DEVICEID-001 |
| DeviceCode | `devicecode` | 待代码审计确认 | 后续实现需引用SPEC-2016-FIELD-DEVICECODE-001 |
| Id/ID | `id` | 待代码审计确认 | 后续实现需引用SPEC-2016-FIELD-ID-001 |
| Result | `result` | 待代码审计确认 | 后续实现需引用SPEC-2016-FIELD-RESULT-001 |
| DeviceList | `devicelist` | 待代码审计确认 | 后续实现需引用SPEC-2016-FIELD-DEVICELIST-001 |
| Values/Value | `values` | 待代码审计确认 | 后续实现需引用SPEC-2016-FIELD-VALUES-001 |

## 6. 不明确事项

字段大小写、表格字段名与XML属性名不一致的项目已进入 `matrices/unknown-and-ambiguous-items.md`。

## 7. 测试要求

结构解析测试必须覆盖标准样例、空标签、大小写变体、缺失字段和非法枚举值。
