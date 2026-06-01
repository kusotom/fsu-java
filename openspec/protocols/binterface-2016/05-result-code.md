# Result / 返回码

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

### [SPEC-2016-RESULT-001] EnumResult定义

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L700-L713，表6 枚举定义  
原文摘要：EnumResult 报文返回结果，FAILURE=0失败，SUCCESS=1成功。  
规则内容：B接口2016标准结果字段为`Result`，取值0=失败、1=成功。  
适用范围：所有含Result的ACK。  
影响模块：响应解析、状态判断、审计。  
实现要求：不得将Result=1解释为失败；不得用2024/工程ResultCode口径覆盖2016 Result。  
测试要求：必须覆盖Result=1成功、Result=0失败、Result缺失UNKNOWN。  
备注：历史文档中ResultCode=1失败口径已修正。

### [SPEC-2016-RESULT-002] ResultCode不是本docx标准字段

规则类型：工程解释  
协议来源：主docx全文检索未发现标准响应字段`ResultCode`；各ACK表和样例使用`Result`  
原文摘要：2016原文响应结果字段均为`Result`，枚举为EnumResult。  
规则内容：平台代码若保留`ResultCode`字段，必须标注为工程兼容或2024字段，不得写作标准2016强约束。  
适用范围：DTO、数据库、前端显示、测试断言。  
影响模块：BInterfaceMessage、响应解析器、审计报告。  
实现要求：标准2016测试优先断言`Result`。  
测试要求：字段别名测试需明确2016 `Result` -> 平台兼容字段的映射。  
备注：真实Emerson GET_DATA_ACK Result=1代表协议层成功。
