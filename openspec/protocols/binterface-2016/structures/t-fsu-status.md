# TFSUStatus

## 1. 来源

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | 数据结构定义 / 相关命令样例 |
| 原文位置 | 表8 数据结构定义 L890-L892；GET_FSUINFO_ACK样例 L3041-L3102 |

## 2. 结构用途

FSU状态参数结构，用于GET_FSUINFO_ACK。

## 3. 字段定义

| 字段 | 类型 | 必填 | 说明 | 所属命令 | SPEC 编号 | 备注 |
|---|---|---|---|---|---|---|
| CPUUsage | float | 是 | CPU使用率 | GET_FSUINFO_ACK | SPEC-2016-STRUCT-TFSUSTATUS-001 |  |
| MEMUsage | float | 是 | 内存使用率 | GET_FSUINFO_ACK | SPEC-2016-STRUCT-TFSUSTATUS-002 |  |

## 3.1 SPEC 规则明细

### [SPEC-2016-STRUCT-TFSUSTATUS-001] CPUUsage

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义 L890-L892；GET_FSUINFO_ACK样例 L3041-L3102  
原文摘要：CPU使用率。  
规则内容：字段 `CPUUsage` 类型为 `float`，必填性：是。  
适用范围：GET_FSUINFO_ACK。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TFSUSTATUS-002] MEMUsage

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义 L890-L892；GET_FSUINFO_ACK样例 L3041-L3102  
原文摘要：内存使用率。  
规则内容：字段 `MEMUsage` 类型为 `float`，必填性：是。  
适用范围：GET_FSUINFO_ACK。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无


## 4. XML 示例

```xml
<TFSUStatus>
  <CPUUsage/>
  <MEMUsage/>
</TFSUStatus>
```

## 5. 平台映射

| 协议字段 | 平台字段 | 表 / DTO / Entity | 备注 |
|---|---|---|---|
| CPUUsage | `cpuusage` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TFSUSTATUS-001 |
| MEMUsage | `memusage` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TFSUSTATUS-002 |

## 6. 不明确事项

字段大小写、表格字段名与XML属性名不一致的项目已进入 `matrices/unknown-and-ambiguous-items.md`。

## 7. 测试要求

结构解析测试必须覆盖标准样例、空标签、大小写变体、缺失字段和非法枚举值。
