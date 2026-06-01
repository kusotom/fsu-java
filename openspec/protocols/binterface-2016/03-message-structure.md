# 报文结构

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

### [SPEC-2016-MSG-001] Request / Response 基本结构

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L223-L234，表1 基本报文格式定义  
原文摘要：请求报文一级节点Request，二级节点PK_Type和Info；响应报文一级节点Response，二级节点PK_Type和Info。  
规则内容：所有标准2016业务报文必须以Request或Response为业务根，且包含PK_Type和Info两个二级节点。  
适用范围：所有命令。  
影响模块：XML parser/builder、fixture、审计。  
实现要求：不能把业务字段直接置于SOAP Body而绕过Request/Response。  
测试要求：每个命令fixture均检查根节点、PK_Type、Info层级。  
备注：SET_FTP样例出现重复PK_Type，作为样例异常登记UNKNOWN。

### [SPEC-2016-MSG-002] 对象模型可无区域

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L237-L246，对象模型  
原文摘要：在监控中心和FSU间可以没有区域。  
规则内容：平台字段映射不能强制要求区域层存在。  
适用范围：站点/设备/FSU对象模型。  
影响模块：资源建模、导入、前端树。  
实现要求：区域为空时仍允许FSU和设备接入。  
测试要求：对象树/导入测试覆盖无区域场景。  
备注：图像为对象模型示意，未形成更多字段规则。

### [SPEC-2016-IMPL-MSG-XMLDATA-BOUNDARY-001] SOAP xmlData 与业务 Request/Response 边界

规则类型：实现注意事项  
协议来源：`SPEC-2016-SOAP-002`；`docs/audit/BIF2016-RPCXML-001-real-http-fsu-client-xmldata-encoding.md`；`docs/landing/raw-samples/data-mapping-008-retry-get-data-request.xml`  
原文摘要：SOAP `invoke(xmlData)` 承载业务XML字符串；业务层根节点仍是 `Request` 或 `Response`。  
规则内容：解析链路必须先处理 SOAP RPC `xmlData/invokeReturn` 字符串，再解析内部业务 `Request/Response`。向 Emerson FSU 发送时，`xmlData` 内的 `Request` 应作为 escaped string；收到 `invokeReturn` 时应解码后再按本文件的业务报文结构解析。  
适用范围：SOAP adapter、raw message 留痕、GET_DATA/GET_FSUINFO 等出站命令。  
影响模块：FsuServiceRpcAdapter、SoapMessageHandler、XMLData parser。  
实现要求：不得把 `xmlData` 的 SOAP 承载格式与业务报文层级混为一谈；`Request/Response + PK_Type + Info` 仍是业务层规则。  
测试要求：覆盖 escaped `xmlData` request、escaped `invokeReturn` response、业务根节点校验。  
备注：该条为实现边界，不新增标准业务字段。
