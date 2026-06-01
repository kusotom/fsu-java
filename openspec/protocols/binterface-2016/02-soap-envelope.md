# SOAP / WSDL 边界

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

### [SPEC-2016-SOAP-001] WebService 与XML格式

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L209-L220，第5.2报文协议  
原文摘要：SC与FSU接口基于WebService技术，消息协议采用XML格式；SC和FSU的WSDL定义见附件。  
规则内容：B接口业务报文为XML，外层通过WebService传输。  
适用范围：SOAP接入、XMLData构造、raw留痕。  
影响模块：SOAP handler、WSDL controller、FsuServiceRpcAdapter。  
实现要求：实现不得把业务请求改为JSON。  
测试要求：SOAP请求/响应测试必须覆盖XMLData转义、嵌套XML声明容错和raw保存。  
备注：主docx未展开WSDL正文，另以WSDL辅助文件核对。

### [SPEC-2016-SOAP-002] WSDL invoke(xmlData) 模型

规则类型：协议规定  
协议来源：辅助WSDL `/home/tom/桌面/FSU/WSDL协议/SCService.wsdl`、`/home/tom/桌面/FSU/WSDL协议/FSUService.wsdl`；主docx L214-L220说明WSDL见附件  
原文摘要：SCService/FSUService均定义invokeRequest(xmlData: soapenc:string)和invokeResponse(invokeReturn: soapenc:string)，RPC/encoded，SOAPAction为空。  
规则内容：标准SOAP调用操作为 `invoke`，业务XML作为字符串参数 `xmlData`，返回字符串 `invokeReturn`。  
适用范围：SOAP适配层。  
影响模块：ScServiceWsdlController、FsuServiceWsdlTemplate、FsuServiceRpcAdapter。  
实现要求：不得用Document/Literal风格替代该WSDL；xmlData内的Request/Response按03-message-structure解析。  
测试要求：WSDL端点测试和RPC envelope测试必须保留。  
备注：该规则依赖附件WSDL；若附件与docx冲突，以原附件和真实设备联调记录双校验。

### [SPEC-2016-IMPL-SOAP-XMLDATA-ESCAPED-001] xmlData 字符串转义实现注意事项

规则类型：实现注意事项  
协议来源：辅助WSDL定义 `xmlData: soapenc:string`；真实联调来源 `docs/audit/BIF2016-RPCXML-001-real-http-fsu-client-xmldata-encoding.md`、`docs/landing/raw-samples/data-mapping-008-retry-get-data-request.xml`  
原文摘要：WSDL将业务XML承载字段定义为字符串参数；Emerson真实FSU联调确认 `<xmlData><Request>...</Request></xmlData>` 不被兼容，`<xmlData xsi:type="xsd:string">&lt;Request&gt;...&lt;/Request&gt;</xmlData>` 可对齐真实设备行为。  
规则内容：工程实现向 Emerson FSU 发起 RPC `invoke` 时，应将业务 `Request` XML 作为 `xmlData` 的 XML-escaped 字符串文本发送，并保留 raw request/response。  
适用范围：FSUService RPC Adapter、真实FSU GET_DATA/只读命令。  
影响模块：FsuServiceRpcAdapter、RealHttpFsuServiceClient、raw sample 留痕。  
实现要求：该条为实现注意事项和 Emerson 兼容要求，不得推导为“所有 B接口2016 设备都只能接受 escaped string”；标准层仍以 WSDL 字符串模型为依据。  
测试要求：RPC envelope 测试必须覆盖 escaped text，不得回退为 DOM child。  
备注：厂商行为详见 `profiles/emerson-2016.md` 的 `SPEC-2016-PROFILE-EMERSON-XMLDATA-001`。

### [SPEC-2016-IMPL-SOAP-PREFIX-EMERSON-001] Emerson RPC SOAP 前缀兼容

规则类型：实现注意事项  
协议来源：`docs/audit/BIF2016-RPCXML-001-real-http-fsu-client-xmldata-encoding.md`  
原文摘要：近期 RPC 修复确认 `SOAP-ENV:Envelope` 可能不被当前 Emerson FSU 接受，`soap:Envelope` 与真实设备行为对齐。  
规则内容：面向 Emerson FSU 的 RPC Adapter 使用 `soap:` 前缀构造 SOAP Envelope。  
适用范围：Emerson Profile 的真实 FSU 出站调用。  
影响模块：FsuServiceRpcAdapter、真实只读 run-once。  
实现要求：SOAP 前缀差异只能作为厂商兼容行为，不写成标准 B接口2016 强制规则。  
测试要求：RPC envelope fixture 应固定覆盖 `soap:` 前缀路径。  
备注：SOAP XML namespace URI 仍为 `http://schemas.xmlsoap.org/soap/envelope/`。
