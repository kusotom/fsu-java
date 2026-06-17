# TDevice

## 1. 来源

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | 数据结构定义 / 相关命令样例 |
| 原文位置 | 工程解释：DeviceList/Device XML样例散见LOGIN、GET_DATA、GET_THRESHOLD等命令 |

## 2. 结构用途

原文未定义名为TDevice的数据结构；本文件将DeviceList/Device元素工程化归档。

## 3. 字段定义

| 字段 | 类型 | 必填 | 说明 | 所属命令 | SPEC 编号 | 备注 |
|---|---|---|---|---|---|---|
| Device@Id | char[DEVICEID_LEN] | 是 | 资源系统设备ID | LOGIN/GET_DATA/GET_THRESHOLD等 | SPEC-2016-STRUCT-TDEVICE-001 | 工程解释 |
| Device@Code | char[DEVICECODE_LEN] | 是 | 设备编码 | LOGIN/GET_DATA/GET_THRESHOLD等 | SPEC-2016-STRUCT-TDEVICE-002 | 工程解释 |
| Id | char[ID_LENGTH] | 按命令 | 监控点ID列表 | GET_DATA/GET_HISDATA/GET_THRESHOLD | SPEC-2016-STRUCT-TDEVICE-003 |  |
| TSemaphore | TSemaphore | 按命令 | 监控点值 | GET_DATA/GET_HISDATA/SET_POINT | SPEC-2016-STRUCT-TDEVICE-004 |  |
| TThreshold | TThreshold | 按命令 | 门限值 | GET_THRESHOLD/SET_THRESHOLD | SPEC-2016-STRUCT-TDEVICE-005 |  |

## 3.1 SPEC 规则明细

### [SPEC-2016-STRUCT-TDEVICE-001] Device@Id

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；工程解释：DeviceList/Device XML样例散见LOGIN、GET_DATA、GET_THRESHOLD等命令  
原文摘要：资源系统设备ID。  
规则内容：字段 `Device@Id` 类型为 `char[DEVICEID_LEN]`，必填性：是。  
适用范围：LOGIN/GET_DATA/GET_THRESHOLD等。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：工程解释

### [SPEC-2016-STRUCT-TDEVICE-002] Device@Code

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；工程解释：DeviceList/Device XML样例散见LOGIN、GET_DATA、GET_THRESHOLD等命令  
原文摘要：设备编码。  
规则内容：字段 `Device@Code` 类型为 `char[DEVICECODE_LEN]`，必填性：是。  
适用范围：LOGIN/GET_DATA/GET_THRESHOLD等。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：工程解释

### [SPEC-2016-STRUCT-TDEVICE-003] Id

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；工程解释：DeviceList/Device XML样例散见LOGIN、GET_DATA、GET_THRESHOLD等命令  
原文摘要：监控点ID列表。  
规则内容：字段 `Id` 类型为 `char[ID_LENGTH]`，必填性：按命令。  
适用范围：GET_DATA/GET_HISDATA/GET_THRESHOLD。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TDEVICE-004] TSemaphore

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；工程解释：DeviceList/Device XML样例散见LOGIN、GET_DATA、GET_THRESHOLD等命令  
原文摘要：监控点值。  
规则内容：字段 `TSemaphore` 类型为 `TSemaphore`，必填性：按命令。  
适用范围：GET_DATA/GET_HISDATA/SET_POINT。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无

### [SPEC-2016-STRUCT-TDEVICE-005] TThreshold

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；工程解释：DeviceList/Device XML样例散见LOGIN、GET_DATA、GET_THRESHOLD等命令  
原文摘要：门限值。  
规则内容：字段 `TThreshold` 类型为 `TThreshold`，必填性：按命令。  
适用范围：GET_THRESHOLD/SET_THRESHOLD。  
影响模块：字段映射、XML解析、测试fixture。  
实现要求：按字段大小写兼容但保留raw来源。  
测试要求：字段存在、缺失、大小写变体测试。  
备注：无


## 4. XML 示例

```xml
<Device Id="000000000001" Code="000000000001">
  <Id/>
</Device>
```

## 5. 平台映射

| 协议字段 | 平台字段 | 表 / DTO / Entity | 备注 |
|---|---|---|---|
| Device@Id | `deviceid` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TDEVICE-001 |
| Device@Code | `devicecode` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TDEVICE-002 |
| Id | `id` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TDEVICE-003 |
| TSemaphore | `tsemaphore` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TDEVICE-004 |
| TThreshold | `tthreshold` | 待代码审计确认 | 后续实现需引用SPEC-2016-STRUCT-TDEVICE-005 |

## 6. 不明确事项

字段大小写、表格字段名与XML属性名不一致的项目已进入 `matrices/unknown-and-ambiguous-items.md`。

## 7. 测试要求

结构解析测试必须覆盖标准样例、空标签、大小写变体、缺失字段和非法枚举值。
