# TThreshold

## 1. 来源

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | 数据结构定义 / 相关命令样例 |
| 原文位置 | 表8 数据结构定义，抽取稿 L861-L889；GET_THRESHOLD/SET_THRESHOLD样例 L1977-L2364 |

## 2. 结构用途

信号量门限值结构，用于门限查询和门限设置。

## 3. 字段定义

| 字段 | 类型 | 必填 | 说明 | 所属命令 | SPEC 编号 | 备注 |
|---|---|---|---|---|---|---|
| Type | EnumType | 是 | 数据类型 | GET_THRESHOLD/SET_THRESHOLD | SPEC-2016-STRUCT-TTHRESHOLD-001 |  |
| Id/ID | char[ID_LENGTH] | 是 | 监控点ID | GET_THRESHOLD/SET_THRESHOLD | SPEC-2016-STRUCT-TTHRESHOLD-002 |  |
| Threshold | float | 是 | 门限值 | GET_THRESHOLD/SET_THRESHOLD | SPEC-2016-STRUCT-TTHRESHOLD-003 |  |
| AbsoluteVal | float | 否 | 绝对阀值 | GET_THRESHOLD/SET_THRESHOLD | SPEC-2016-STRUCT-TTHRESHOLD-004 | 原文写阀值 |
| RelativeVal | float | 否 | 百分比阀值 | GET_THRESHOLD/SET_THRESHOLD | SPEC-2016-STRUCT-TTHRESHOLD-005 |  |
| Status | EnumState | 是 | 状态 | GET_THRESHOLD/SET_THRESHOLD | SPEC-2016-STRUCT-TTHRESHOLD-006 |  |

## 3.1 SPEC 规则明细

### [SPEC-2016-STRUCT-TTHRESHOLD-001] Type

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_THRESHOLD/SET_THRESHOLD样例 L1977-L2364  
原文摘要：数据类型。  
规则内容：字段 `Type` 类型为 `EnumType`，必填性：是。  
适用范围：GET_THRESHOLD/SET_THRESHOLD。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TTHRESHOLD-002] Id/ID

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_THRESHOLD/SET_THRESHOLD样例 L1977-L2364  
原文摘要：监控点ID。  
规则内容：字段 `Id/ID` 类型为 `char[ID_LENGTH]`，必填性：是。  
适用范围：GET_THRESHOLD/SET_THRESHOLD。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TTHRESHOLD-003] Threshold

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_THRESHOLD/SET_THRESHOLD样例 L1977-L2364  
原文摘要：门限值。  
规则内容：字段 `Threshold` 类型为 `float`，必填性：是。  
适用范围：GET_THRESHOLD/SET_THRESHOLD。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TTHRESHOLD-004] AbsoluteVal

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_THRESHOLD/SET_THRESHOLD样例 L1977-L2364  
原文摘要：绝对阀值。  
规则内容：字段 `AbsoluteVal` 类型为 `float`，必填性：否。  
适用范围：GET_THRESHOLD/SET_THRESHOLD。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：原文写阀值

### [SPEC-2016-STRUCT-TTHRESHOLD-005] RelativeVal

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_THRESHOLD/SET_THRESHOLD样例 L1977-L2364  
原文摘要：百分比阀值。  
规则内容：字段 `RelativeVal` 类型为 `float`，必填性：否。  
适用范围：GET_THRESHOLD/SET_THRESHOLD。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TTHRESHOLD-006] Status

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L889；GET_THRESHOLD/SET_THRESHOLD样例 L1977-L2364  
原文摘要：状态。  
规则内容：字段 `Status` 类型为 `EnumState`，必填性：是。  
适用范围：GET_THRESHOLD/SET_THRESHOLD。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无


## 4. XML 示例

```xml
<TThreshold Type="" Id="" Threshold="" AbsoluteVal="" RelativeVal="" Status=""/>
```

## 5. 平台映射

| 协议字段 | 平台字段 | 表 / DTO / Entity | 备注 |
|---|---|---|---|
| Type | `type` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TTHRESHOLD-001 |
| Id/ID | `id` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TTHRESHOLD-002 |
| Threshold | `threshold` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TTHRESHOLD-003 |
| AbsoluteVal | `absoluteval` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TTHRESHOLD-004 |
| RelativeVal | `relativeval` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TTHRESHOLD-005 |
| Status | `status` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TTHRESHOLD-006 |

## 6. 不明确事项

字段大小写、表格字段名与XML属性名不一致的项目已进入 `matrices/unknown-and-ambiguous-items.md`。

## 7. 测试要求

结构解析测试必须覆盖标准样例、空标签、大小写变体、缺失字段和非法枚举值。
