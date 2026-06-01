# 数据类型与常量

## 1. 来源

| 项 | 内容 |
|---|---|
| 主源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 辅助同版文档 | `/home/tom/桌面/FSU/B接口协议/B接口协议2016.docx` |
| WSDL辅助文件 | `/home/tom/桌面/FSU/WSDL协议/SCService.wsdl`；`/home/tom/桌面/FSU/WSDL协议/FSUService.wsdl` |
| 主源SHA256 | `eb049933533db97761aa1514cca449dc6f143a686c1425ba4ff087eb38574c1b` |
| 辅助docx SHA256 | `d481a1f0b3d695b3a875b75976490319c4db4278b599b0f2191c5601b3b71bbf` |
| 抽取稿 | `/tmp/binterface2016_docx_extract.md`，3297行，仅作定位索引 |


## 2. SPEC 规则

### [SPEC-2016-DATATYPE-001] 基础类型字节数

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L430-L442，表4 数据类型字节数定义  
原文摘要：Long 4字节，Short 2字节，Char 1字节，Float 4字节，枚举类型4字节。  
规则内容：协议字段长度解释应按该表，不得按Java类型长度直接反推。  
适用范围：字段校验、报文长度、DTO说明。  
影响模块：XML校验、测试fixture。  
实现要求：XML文本字段长度按协议常量限制。  
测试要求：边界长度测试应按常量定义建立。  
备注：XML样例为空标签，长度需在实际报文中校验。

### [SPEC-2016-DATATYPE-002] 关键常量

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L655-L698，表5 常量定义  
原文摘要：定义USER_LENGTH、PASSWORD_LEN、FSUID_LEN、FSUCODE_LEN、DEVICEID_LEN、DEVICECODE_LEN、ID_LENGTH、SERIALNO_LEN、TIME_LEN等。  
规则内容：FsuId/FsuCode/DeviceId/DeviceCode均为14字节，监控点Id为10字节，时间串为19字节。  
适用范围：所有命令和结构字段。  
影响模块：字段校验、数据库字段长度、DTO。  
实现要求：字段长度变更必须引用该SPEC。  
测试要求：建立字段长度边界fixture。  
备注：表5存在EVENT_LENGTH重复，进入UNKNOWN。

### [SPEC-2016-DATATYPE-003] 时间格式

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L393-L404，TAlarm说明；L861-L889，数据结构定义  
原文摘要：时间描述为YYYY-MM-DD<SPACE键>hh:mm:ss，24小时制。  
规则内容：AlarmTime、TSemaphore.RecordTime应采用`YYYY-MM-DD HH:mm:ss`。  
适用范围：告警、历史数据。  
影响模块：日期解析、数据库保存、测试fixture。  
实现要求：解析失败不得静默成功，应标记UNKNOWN或错误。  
测试要求：合法/非法时间格式测试。  
备注：TIME_CHECK使用TTime结构，不是时间字符串。

### [SPEC-2016-DATATYPE-004] 文本禁止半角尖括号

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L421-L428，TAlarm示例说明  
原文摘要：所有文本描述中不能包含`<`、`>`字符。  
规则内容：AlarmDesc等文本字段不得包含半角尖括号；写入XML时仍需做XML escape。  
适用范围：告警文本、描述文本。  
影响模块：XML构造、输入校验、raw保存。  
实现要求：入站解析保留原始报文；出站构造必须escape。  
测试要求：尖括号、引号、空白字段测试。  
备注：原文强调文本内容限制，工程仍必须遵循XML转义。
