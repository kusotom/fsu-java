# 错误处理

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

### [SPEC-2016-ERROR-001] ACK成功/失败以Result为准

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 表6 EnumResult L711-L713；各ACK表  
原文摘要：含Result的响应以EnumResult表达成功/失败。  
规则内容：Result=1为成功，Result=0为失败。  
适用范围：LOGIN以外多数ACK。  
影响模块：响应解析、错误处理、前端展示。  
实现要求：Result缺失时不得默认成功或失败，必须进入UNKNOWN/协议异常处理。  
测试要求：Result=1、Result=0、Result缺失、非法值测试。  
备注：LOGIN_ACK使用RightLevel/SCIP，无Result字段。

### [SPEC-2016-ERROR-002] 字段矛盾处理

规则类型：工程解释  
协议来源：原文多处样例/表格差异，见UNKNOWN矩阵  
原文摘要：如FsuID/FsuId、Version/Vervion、SET_ LOGININFO_ACK空格、SET_FTP重复PK_Type。  
规则内容：表格与XML样例冲突时，不直接擅自统一为协议规定；标准文件记录原样，工程可做兼容解析。  
适用范围：XML解析、字段别名、审计。  
影响模块：XmlDataParser、BInterfacePkType、测试fixture。  
实现要求：兼容解析必须保留rawAttributes/rawMessage，输出规范字段时标明来源。  
测试要求：大小写、空格、拼写变体测试。  
备注：所有待确认项列入 `matrices/unknown-and-ambiguous-items.md`。
