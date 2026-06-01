# TSemaphore

## 1. 来源

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | 数据结构定义 / 相关命令样例 |
| 原文位置 | 表8 数据结构定义，抽取稿 L861-L889；GET_DATA/GET_HISDATA响应样例 L1480-L1748 |

## 2. 结构用途

信号量的值结构，用于实时数据、历史数据和SET_POINT。

## 3. 字段定义

| 字段 | 类型 | 必填 | 说明 | 所属命令 | SPEC 编号 | 备注 |
|---|---|---|---|---|---|---|
| Type | EnumType | 是 | 数据类型 | GET_DATA/GET_HISDATA/SET_POINT | SPEC-2016-STRUCT-TSEMAPHORE-001 |  |
| Id/ID | char[ID_LENGTH] | 是 | 监控点ID | GET_DATA/GET_HISDATA/SET_POINT | SPEC-2016-STRUCT-TSEMAPHORE-002 | 结构表写ID，XML属性写Id |
| MeasuredVal | float | 否 | 实测值 | GET_DATA/GET_HISDATA | SPEC-2016-STRUCT-TSEMAPHORE-003 |  |
| SetupVal | float | 否 | 设置值 | SET_POINT/GET_DATA | SPEC-2016-STRUCT-TSEMAPHORE-004 |  |
| Status | EnumState | 是 | 状态 | GET_DATA/GET_HISDATA/SET_POINT | SPEC-2016-STRUCT-TSEMAPHORE-005 |  |
| RecordTime | char[DES_LENGTH] | 历史数据必填 | 历史记录时间 | GET_HISDATA_ACK | SPEC-2016-STRUCT-TSEMAPHORE-006 | 实时数据无该属性 |

## 3.1 SPEC 规则明细

### [SPEC-2016-STRUCT-TSEMAPHORE-001] Type

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_DATA/GET_HISDATA响应样例 L1480-L1748  
原文摘要：数据类型。  
规则内容：字段 `Type` 类型为 `EnumType`，必填性：是。  
适用范围：GET_DATA/GET_HISDATA/SET_POINT。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TSEMAPHORE-002] Id/ID

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_DATA/GET_HISDATA响应样例 L1480-L1748  
原文摘要：监控点ID。  
规则内容：字段 `Id/ID` 类型为 `char[ID_LENGTH]`，必填性：是。  
适用范围：GET_DATA/GET_HISDATA/SET_POINT。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：结构表写ID，XML属性写Id

### [SPEC-2016-STRUCT-TSEMAPHORE-003] MeasuredVal

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_DATA/GET_HISDATA响应样例 L1480-L1748  
原文摘要：实测值。  
规则内容：字段 `MeasuredVal` 类型为 `float`，必填性：否。  
适用范围：GET_DATA/GET_HISDATA。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TSEMAPHORE-004] SetupVal

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_DATA/GET_HISDATA响应样例 L1480-L1748  
原文摘要：设置值。  
规则内容：字段 `SetupVal` 类型为 `float`，必填性：否。  
适用范围：SET_POINT/GET_DATA。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TSEMAPHORE-005] Status

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_DATA/GET_HISDATA响应样例 L1480-L1748  
原文摘要：状态。  
规则内容：字段 `Status` 类型为 `EnumState`，必填性：是。  
适用范围：GET_DATA/GET_HISDATA/SET_POINT。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TSEMAPHORE-006] RecordTime

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_DATA/GET_HISDATA响应样例 L1480-L1748  
原文摘要：历史记录时间。  
规则内容：字段 `RecordTime` 类型为 `char[DES_LENGTH]`，必填性：历史数据必填。  
适用范围：GET_HISDATA_ACK。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：实时数据无该属性


## 4. XML 示例

```xml
<TSemaphore Type="" Id="" MeasuredVal="" SetupVal="" Status="" RecordTime=""/>
```

## 5. 平台映射

| 协议字段 | 平台字段 | 表 / DTO / Entity | 备注 |
|---|---|---|---|
| Type | `type` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TSEMAPHORE-001 |
| Id/ID | `id` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TSEMAPHORE-002 |
| MeasuredVal | `measuredval` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TSEMAPHORE-003 |
| SetupVal | `setupval` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TSEMAPHORE-004 |
| Status | `status` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TSEMAPHORE-005 |
| RecordTime | `recordtime` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TSEMAPHORE-006 |

## 6. 不明确事项

字段大小写、表格字段名与XML属性名不一致的项目已进入 `matrices/unknown-and-ambiguous-items.md`。

## 7. 测试要求

结构解析测试必须覆盖标准样例、空标签、大小写变体、缺失字段和非法枚举值。
