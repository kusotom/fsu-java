# TAlarm

## 1. 来源

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | 数据结构定义 / 相关命令样例 |
| 原文位置 | TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335 |

## 2. 结构用途

当前告警值结构，用于SEND_ALARM上报。

## 3. 字段定义

| 字段 | 类型 | 必填 | 说明 | 所属命令 | SPEC 编号 | 备注 |
|---|---|---|---|---|---|---|
| SerialNo | char[SERIALNO_LEN] | 是 | 告警序号 | SEND_ALARM | SPEC-2016-STRUCT-TALARM-001 | 10位数字 |
| Id/ID | char[ID_LENGTH] | 是 | 监控点ID | SEND_ALARM | SPEC-2016-STRUCT-TALARM-002 |  |
| FsuId/FSUID | char[FSUID_LEN] | 是 | FSU ID | SEND_ALARM | SPEC-2016-STRUCT-TALARM-003 | 表写FSUID，XML写FsuId |
| FsuCode | char[FSUCODE_LEN] | 是 | FSU编码 | SEND_ALARM | SPEC-2016-STRUCT-TALARM-004 |  |
| DeviceId/DeviceID | char[DEVICEID_LEN] | 是 | 设备ID | SEND_ALARM | SPEC-2016-STRUCT-TALARM-005 | 大小写差异 |
| DeviceCode | char[DEVICECODE_LEN] | 是 | 设备编码 | SEND_ALARM | SPEC-2016-STRUCT-TALARM-006 | 结构表存在DEVICEICODE_LEN拼写问题 |
| AlarmTime | char[DES_LENGTH] | 是 | 告警时间 | SEND_ALARM | SPEC-2016-STRUCT-TALARM-007 | YYYY-MM-DD HH:mm:ss |
| AlarmLevel | EnumState/EnumAlarmLevel | 是 | 告警级别 | SEND_ALARM | SPEC-2016-STRUCT-TALARM-008 | 表与枚举命名不完全一致 |
| AlarmFlag | EnumFlag | 是 | 开始/结束 | SEND_ALARM | SPEC-2016-STRUCT-TALARM-009 |  |
| AlarmDesc | char[DES_LENGTH] | 是 | 40字节以内告警描述 | SEND_ALARM | SPEC-2016-STRUCT-TALARM-010 | 不得包含半角<或> |

## 3.1 SPEC 规则明细

### [SPEC-2016-STRUCT-TALARM-001] SerialNo

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335  
原文摘要：告警序号。  
规则内容：字段 `SerialNo` 类型为 `char[SERIALNO_LEN]`，必填性：是。  
适用范围：SEND_ALARM。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：10位数字

### [SPEC-2016-STRUCT-TALARM-002] Id/ID

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335  
原文摘要：监控点ID。  
规则内容：字段 `Id/ID` 类型为 `char[ID_LENGTH]`，必填性：是。  
适用范围：SEND_ALARM。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TALARM-003] FsuId/FSUID

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335  
原文摘要：FSU ID。  
规则内容：字段 `FsuId/FSUID` 类型为 `char[FSUID_LEN]`，必填性：是。  
适用范围：SEND_ALARM。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：表写FSUID，XML写FsuId

### [SPEC-2016-STRUCT-TALARM-004] FsuCode

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335  
原文摘要：FSU编码。  
规则内容：字段 `FsuCode` 类型为 `char[FSUCODE_LEN]`，必填性：是。  
适用范围：SEND_ALARM。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TALARM-005] DeviceId/DeviceID

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335  
原文摘要：设备ID。  
规则内容：字段 `DeviceId/DeviceID` 类型为 `char[DEVICEID_LEN]`，必填性：是。  
适用范围：SEND_ALARM。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：大小写差异

### [SPEC-2016-STRUCT-TALARM-006] DeviceCode

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335  
原文摘要：设备编码。  
规则内容：字段 `DeviceCode` 类型为 `char[DEVICECODE_LEN]`，必填性：是。  
适用范围：SEND_ALARM。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：结构表存在DEVICEICODE_LEN拼写问题

### [SPEC-2016-STRUCT-TALARM-007] AlarmTime

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335  
原文摘要：告警时间。  
规则内容：字段 `AlarmTime` 类型为 `char[DES_LENGTH]`，必填性：是。  
适用范围：SEND_ALARM。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：YYYY-MM-DD HH:mm:ss

### [SPEC-2016-STRUCT-TALARM-008] AlarmLevel

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335  
原文摘要：告警级别。  
规则内容：字段 `AlarmLevel` 类型为 `EnumState/EnumAlarmLevel`，必填性：是。  
适用范围：SEND_ALARM。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：表与枚举命名不完全一致

### [SPEC-2016-STRUCT-TALARM-009] AlarmFlag

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335  
原文摘要：开始/结束。  
规则内容：字段 `AlarmFlag` 类型为 `EnumFlag`，必填性：是。  
适用范围：SEND_ALARM。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TALARM-010] AlarmDesc

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；TAlarm XML说明 L321-L428；表8结构定义 L877-L889；SEND_ALARM样例 L1217-L1335  
原文摘要：40字节以内告警描述。  
规则内容：字段 `AlarmDesc` 类型为 `char[DES_LENGTH]`，必填性：是。  
适用范围：SEND_ALARM。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：不得包含半角<或>


## 4. XML 示例

```xml
<TAlarm>
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
</TAlarm>
```

## 5. 平台映射

| 协议字段 | 平台字段 | 表 / DTO / Entity | 备注 |
|---|---|---|---|
| SerialNo | `serialno` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TALARM-001 |
| Id/ID | `id` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TALARM-002 |
| FsuId/FSUID | `fsuid` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TALARM-003 |
| FsuCode | `fsucode` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TALARM-004 |
| DeviceId/DeviceID | `deviceid` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TALARM-005 |
| DeviceCode | `devicecode` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TALARM-006 |
| AlarmTime | `alarmtime` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TALARM-007 |
| AlarmLevel | `alarmlevel` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TALARM-008 |
| AlarmFlag | `alarmflag` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TALARM-009 |
| AlarmDesc | `alarmdesc` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TALARM-010 |

## 6. 不明确事项

字段大小写、表格字段名与XML属性名不一致的项目已进入 `matrices/unknown-and-ambiguous-items.md`。

## 7. 测试要求

结构解析测试必须覆盖标准样例、空标签、大小写变体、缺失字段和非法枚举值。
