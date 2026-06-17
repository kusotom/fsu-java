# Emerson 2016 厂商 Profile

本文件只记录Emerson真实FSU联调行为。所有内容均为“厂商实测”，不能写成标准B接口2016协议规定。

## 1. 实测来源

| 来源文档 | 结论 |
|---|---|
| `/home/tom/桌面/FSU/docs/audit/FAST-REALDATA-002-real-fsu-readonly-result.md` | 仅执行GET_FSUINFO/GET_DATA只读命令，GET_DATA返回空数据(Result=1) |
| `/home/tom/桌面/FSU/docs/audit/FAST-REALDATA-003B-binterface-2016-dictionary-driven-get-data-probe.md` | 6种GET_DATA策略均返回Result=1 + 空DeviceList；GET_FSUINFO返回CPU/MEM |
| `/home/tom/桌面/FSU/docs/audit/BIF2016-CONNECTION-005-get-data-registration-context-real-readonly.md` | 基于LOGIN registration context的GET_DATA返回ACK=402 Result=1，DeviceList为空 |
| `/home/tom/桌面/FSU/docs/audit/BIF2016-AUTH-003-login-devicelist-capability-parse.md` | LOGIN DeviceList解析覆盖Device.Id/Device.Code属性、子节点、大小写变体 |
| `docs/audit/SPEC-TEST-P0-001-real-fsu-run-once.md` | 2026-05-26真实FSU只读run-once：GET_DATA/GET_FTP/GET_THRESHOLD返回Result=1；GET_FSUINFO返回CPU/MEM但Result=0；GET_LOGININFO返回DeviceList但缺Result |
| `docs/audit/SPEC-TEST-P0-001-real-fsu-run-once.md` | 2026-05-26真实FSU传感器状态复测：GET_DATA携带已知Id列表时返回8个TSemaphore，MeasuredVal均为0或0.0 |
| `/home/tom/桌面/FSU/docs/audit/BIF2016-CONNECTION-007-real-get-data-readonly-retest.md` | 2026-05-27修正后 `<Id>` GET_DATA真实只读复测：all-devices返回8个TSemaphore，single-device返回2个TSemaphore，`0407102001` 返回54.1V；minimal/wildcard读取超时 |
| `/home/tom/桌面/FSU/docs/audit/SPEC-DICT-MAPPING-001-signalid-dictionary-lookup.md` | 8个真实SignalID均与标准信号字典exact匹配；`0418101001/0418102001`纠正为环境温度/环境湿度 |
| `/home/tom/桌面/FSU/docs/audit/DATA-MAPPING-007-monitoring-point-import-and-single-point-realtime-acceptance.md` | 8条monitoring_point已受控导入，`0407102001=54.1V`单点realtime_data入库验收成功 |
| `/home/tom/桌面/FSU/docs/audit/DATA-MAPPING-008-formal-get-data-realtime-data-full-chain.md` | 正式run-once API request侧链路正确，现场FSU当时超时；retry curl+proxy样本返回`0407102001=54.2V`和`0407107001=0.0` |
| `/home/tom/桌面/FSU/docs/audit/BIF2016-RPCXML-001-real-http-fsu-client-xmldata-encoding.md` | Emerson FSU要求RPC `xmlData`为escaped string，`soap:`前缀更兼容；不是标准协议强制规则 |
| `/home/tom/桌面/FSU/docs/audit/BIF2016-NETWORK-001-real-http-fsu-client-network-diagnosis.md` | Java/curl直连或代理均曾成功，后续仍可能timeout；网络/代理问题归为现场链路稳定性风险 |

## 2. SPEC 规则

### [SPEC-2016-PROFILE-EMERSON-001] 真实设备使用2016码表

规则类型：厂商实测  
协议来源：项目审计/记忆，不属于标准docx  
原文摘要：真实FSU对GET_DATA=401/ACK=402、GET_LOGININFO=1501/1502、GET_FSUINFO=1701/1702、GET_FTP=1601/1602、SEND_ALARM=501表现为2016码表。  
规则内容：Emerson接入Profile默认采用B接口2016码表。  
适用范围：Emerson真实联调。  
影响模块：FsuServiceClient、命令码映射。  
实现要求：不得用2024 GET_DATA=501或SEND_ALARM=601覆盖Emerson 2016主线。  
测试要求：真实FSU测试必须保存raw request/response。  
备注：厂商Profile不覆盖标准协议。

### [SPEC-2016-PROFILE-EMERSON-002] GET_DATA空DeviceList行为

规则类型：厂商实测  
协议来源：FAST-REALDATA-002/003B、BIF2016-CONNECTION-005  
原文摘要：Emerson FSU对多种GET_DATA变体返回GET_DATA_ACK Code=402、Result=1、DeviceList为空。  
规则内容：该行为解释为协议层成功但设备未返回测点值；不能作为标准2016规则。  
适用范围：真实Emerson联调和诊断。  
影响模块：GET_DATA run-once、前端展示、审计报告。  
实现要求：Result=1仍按标准EnumResult解释为SUCCESS，空DeviceList作为业务无数据。  
测试要求：fixture覆盖Result=1 + empty DeviceList。  
备注：需厂家确认GET_DATA业务状态。

### [SPEC-2016-PROFILE-EMERSON-003] LOGIN DeviceList能力模型

规则类型：厂商实测  
协议来源：BIF2016-AUTH-003  
原文摘要：LOGIN注册上下文可解析Device.Id/Device.Code属性形式、子节点形式、大小写变体，Id缺失置valid=false。  
规则内容：Emerson Profile解析LOGIN DeviceList时允许属性/子节点兼容，但必须保留rawAttributes。  
适用范围：注册上下文和GET_DATA目标选择。  
影响模块：BInterfaceFsuRegistrationContextService、DeviceCapability。  
实现要求：Code缺失可保留device，Id缺失不得进入有效GET_DATA目标。  
测试要求：真实样本/脱敏样本回放。  
备注：兼容策略属于厂商Profile/工程解释。

### [SPEC-2016-PROFILE-EMERSON-004] GET_FSUINFO返回状态数据但Result为0

规则类型：厂商实测  
协议来源：docs/audit/SPEC-TEST-P0-001-real-fsu-run-once.md；raw样本 `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_FSUINFO-51051243812345-20260526-230101-response-unwrapped.xml`  
原文摘要：真实Emerson FSU返回GET_FSUINFO_ACK Code=1702，Info中包含FsuId、FsuCode、TFSUStatus/CPUUsage、TFSUStatus/MEMUsage，但Result=0。  
规则内容：该行为只能作为Emerson Profile观测差异记录；不得改写标准B接口2016 `EnumResult: FAILURE=0, SUCCESS=1`。  
适用范围：Emerson真实FSU只读run-once、联调诊断和兼容性报告。  
影响模块：真实FSU run-once测试、GET_FSUINFO诊断、Profile兼容层。  
实现要求：标准2016测试仍以Result=1为成功；真实FSU观测测试可记录Result=0并要求ACK=1702及CPU/MEM存在。  
测试要求：raw request/response必须保存在`docs/landing/raw-samples/`，文件名包含任务编号、命令、FSUCode和时间戳。  
备注：需厂家确认GET_FSUINFO Result=0在该设备上的业务含义。

### [SPEC-2016-PROFILE-EMERSON-005] GET_LOGININFO返回DeviceList但缺少Result

规则类型：厂商实测  
协议来源：docs/audit/SPEC-TEST-P0-001-real-fsu-run-once.md；raw样本 `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_LOGININFO-51051243812345-20260526-232711-response-unwrapped.xml`  
原文摘要：真实Emerson FSU返回GET_LOGININFO_ACK Code=1502，Info中包含FsuId、FsuCode、SCIP和4个Device，但未返回Result字段。  
规则内容：该行为只能作为Emerson Profile观测差异记录；标准2016响应仍应按规范检查Result字段。  
适用范围：Emerson真实FSU只读run-once、设备能力发现、GET_DATA目标选择。  
影响模块：GET_LOGININFO诊断、DeviceList能力解析、真实FSU兼容性报告。  
实现要求：标准2016离线测试不得因该厂商差异放宽；Emerson profile路径可将ACK=1502且DeviceList存在视为能力清单可用。  
测试要求：raw request/response必须保存在`docs/landing/raw-samples/`，文件名包含任务编号、命令、FSUCode和时间戳。  
备注：需厂家确认GET_LOGININFO缺少Result字段是否为该设备固定行为。

### [SPEC-2016-PROFILE-EMERSON-006] GET_DATA显式Id查询返回TSemaphore测点值

规则类型：厂商实测  
协议来源：docs/audit/SPEC-TEST-P0-001-real-fsu-run-once.md；raw样本 `backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260526-234420-response-unwrapped.xml`、`backend/docs/landing/raw-samples/SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260527-203401-response-unwrapped.xml`  
原文摘要：真实Emerson FSU在GET_DATA请求携带4个Device及已知Id列表时，返回GET_DATA_ACK Code=402、Result=1和TSemaphore；2026-05-27样本中 `Id=0407102001` 返回 `MeasuredVal=53.9`、`Status=0`。  
规则内容：该行为证明显式Id查询路径可返回TSemaphore测点结构和值；其中 `0407102001` 可依据《中国铁塔动环监控设备信号字典表-精简版-20150416.xlsx》识别为D类机房蓄电池组总电压，单位V。其他测点仍需结合字典和现场设备映射逐项确认。  
适用范围：Emerson真实FSU测点读取、点位映射验证、传感器状态诊断。  
影响模块：GET_DATA run-once、点位映射、前端实时数据展示。  
实现要求：展示层必须依赖字典码表、厂家点位表或人工确认的DeviceID/SPID/SignalID映射；不得只凭Id数字推断测点名称。  
测试要求：raw request/response必须保存；后续应补充按SignalID读取电压、电流、温湿度的独立run-once测试。  
备注：2026-05-26样本中该点为0.0，2026-05-27样本中该点为53.9V，属于真实设备实时值变化。

### [SPEC-2016-PROFILE-EMERSON-007] 修正后Id结构GET_DATA返回真实测点值

规则类型：厂商实测  
协议来源：`docs/audit/BIF2016-CONNECTION-007-real-get-data-readonly-retest.md`；raw样本 `docs/landing/raw-samples/bif2016-connection-007-get-data-all-devices-unwrapped.xml`、`docs/landing/raw-samples/bif2016-connection-007-get-data-single-device-unwrapped.xml`  
原文摘要：2026-05-27 使用 B接口2016 GET_DATA Code=401、request Device 子节点 `<Id>signalId</Id>`、response TSemaphore 结构执行真实只读复测；all-devices 返回8个TSemaphore，single-device返回2个TSemaphore，`0407102001` 返回 `MeasuredVal=54.1`。minimal/wildcard 全9请求读取响应超时。  
规则内容：该行为证明 Emerson FSU 在显式 `<Id>` 查询路径下可返回真实 TSemaphore 值；全9通配查询超时只作为厂商实测差异记录，不能改写标准通配规则。  
适用范围：Emerson真实FSU测点读取、点位映射验收、后续入库dry-run。  
影响模块：GET_DATA run-once、点位映射、realtime_data入库验收。  
实现要求：request 继续使用2016 `GET_DATA`/401和 `<Id>` 列表；response 继续解析 TSemaphore；不要将厂商通配超时写成标准协议限制。  
测试要求：真实FSU测试默认禁用，必须显式开启，raw request/response/unwrapped必须保存。  
备注：本次未执行SET、未启Scheduler、未执行DDL/DML、未写realtime_data。

### [SPEC-2016-PROFILE-EMERSON-XMLDATA-001] RPC xmlData 使用 escaped string

规则类型：厂商实测  
协议来源：`docs/audit/BIF2016-RPCXML-001-real-http-fsu-client-xmldata-encoding.md`；raw样本 `docs/landing/raw-samples/data-mapping-008-retry-get-data-request.xml`  
原文摘要：错误格式为 `<xmlData><Request>...</Request></xmlData>`；Emerson兼容格式为 `<xmlData xsi:type="xsd:string">&lt;Request&gt;...&lt;/Request&gt;</xmlData>`。  
规则内容：当前 Emerson FSU 出站 RPC 调用要求把业务 `Request` XML 作为 escaped string 放入 `xmlData`，而不是作为 DOM 子节点嵌入。  
适用范围：Emerson FSUService `invoke(xmlData)` 出站调用。  
影响模块：FsuServiceRpcAdapter、RealHttpFsuServiceClient、正式run-once API。  
实现要求：Emerson Profile 路径必须保持 escaped string；标准协议层只能引用 WSDL 字符串模型，不能扩大为所有 B接口2016 设备强制行为。  
测试要求：RPC envelope 测试必须断言 `xmlData` 含 `&lt;Request&gt;`，且不含未转义 DOM child。  
备注：`data-mapping-008-retry-get-data-response.xml` 的 `invokeReturn` 同样为 escaped Response 字符串。

### [SPEC-2016-PROFILE-EMERSON-SOAP-PREFIX-001] RPC Envelope 使用 soap: 前缀

规则类型：厂商实测  
协议来源：`docs/audit/BIF2016-RPCXML-001-real-http-fsu-client-xmldata-encoding.md`；raw样本 `docs/landing/raw-samples/data-mapping-008-retry-get-data-request.xml`  
原文摘要：修复中发现 `SOAP-ENV:Envelope` 可能不被当前 Emerson FSU 接受，`soap:Envelope` 可对齐真实设备行为。  
规则内容：Emerson FSU RPC Adapter 使用 `soap:` 前缀构造 Envelope，namespace URI仍为SOAP 1.1。  
适用范围：Emerson FSUService 出站调用。  
影响模块：FsuServiceRpcAdapter、SOAP raw sample。  
实现要求：该前缀行为只作为厂商兼容行为，不写成标准 B接口2016 强制规则。  
测试要求：RPC envelope fixture 覆盖 `soap:Envelope`。  
备注：真实 FSU 响应可能仍返回 `SOAP-ENV:Envelope`，解析端应按 namespace URI 而非前缀硬编码。

### [SPEC-2016-PROFILE-EMERSON-008] 8个真实SignalID标准字典exact匹配

规则类型：厂商实测 + 标准字典匹配  
协议来源：`docs/audit/SPEC-DICT-MAPPING-001-signalid-dictionary-lookup.md`；raw样本 `docs/landing/raw-samples/bif2016-connection-007-get-data-all-devices-unwrapped.xml`  
原文摘要：Emerson真实GET_DATA返回8个TSemaphore，均可按中国铁塔标准信号字典exact匹配。  
规则内容：8个真实SignalID的名称、类型、单位按标准字典解释；其中DI点位单位列为空，按“无单位”展示，0/1编码仍待现场复核；`0407107001`原始Excel与CSV单位列均为空，单位仍待确认，不得补造为V。  
适用范围：Emerson点位映射、monitoring_point导入、realtime_data展示。  
影响模块：点位映射、前端实时数据、字典索引。  
实现要求：`0418101001/0418102001`必须解释为环境温度/环境湿度，不得沿用历史候选中的CPU/内存误读。  
测试要求：字典反查和字段映射矩阵覆盖8/8 exact。  
备注：具体点位见 `dictionaries/standard-signal-dictionary-index.md`；单位复核见 `SPEC-UNIT-COMPLETE-001`。

### [SPEC-2016-PROFILE-EMERSON-011] 真实点位单位按标准字典复核

规则类型：厂商实测 + 标准字典复核  
协议来源：`docs/audit/SPEC-UNIT-COMPLETE-001-standard-signal-dictionary-unit-completion.md`；原始 Excel `B接口协议/中国铁塔动环监控设备信号字典表-精简版-20150416.xlsx`；CSV `docs/mapping/dry-run/standard-signal-dictionary-dry-run.csv`  
原文摘要：8个真实 SignalID 均为 exact；`0418101001=℃`、`0418102001=%RH`、`0407102001=V`；4个 DI 点位单位列为空，应解释为无工程单位；`0407107001` 原始 Excel 行70和CSV单位列均为空。  
规则内容：Emerson真实点位展示单位必须来自标准字典；DI空单位显示为“无单位”，AI空单位显示为“单位待确认”。  
适用范围：monitoring_point unit、前端实时数据展示、字段映射矩阵。  
影响模块：点位映射、realtime_data展示、前端单位渲染规则。  
实现要求：不得凭“后半组电压”名称把 `0407107001` 补为 `V`；不得把 DI 点位补为数值单位；不得改变 DI 0/1 语义。  
测试要求：前端展示规则应覆盖 8 个真实点位，`0407107001` 继续显示单位待确认。  
备注：`UNKNOWN-2016-DICT-0407107001-UNIT` 保留，但状态更新为“已复核原始字典仍为空”。

### [SPEC-2016-PROFILE-EMERSON-009] 现场FSU/代理链路间歇性可达

规则类型：运维注意事项  
协议来源：`docs/audit/BIF2016-NETWORK-001-real-http-fsu-client-network-diagnosis.md`、`docs/audit/DATA-MAPPING-008-formal-get-data-realtime-data-full-chain.md`、`docs/landing/data-mapping/data-mapping-008-retry-result.md`  
原文摘要：curl代理、Java HttpURLConnection、JDK HttpClient、raw socket均曾成功；后续同一类请求仍可能502或timeout。  
规则内容：当前判断为FSU/代理/现场链路间歇性可达，不是单一Header、Content-Length、SOAPAction、User-Agent或客户端类型问题。  
适用范围：真实FSU验收、运维排障、正式API全链路验收。  
影响模块：RealHttpFsuServiceClient、run-once API、运维监控。  
实现要求：不得把网络/代理现象写成协议规则；不得建议用curl替代Java作为正式采集链路。  
测试要求：真实FSU验收失败时必须记录HTTP/SOAP/ACK阶段和raw request。  
备注：登记为 `UNKNOWN-2016-OPS-NETWORK-001`。

### [SPEC-2016-PROFILE-EMERSON-010] GET_DATA 为慢数据通道且受注册上下文间接影响

规则类型：厂商实测 + 实现注意事项  
协议来源：`docs/audit/SPEC-FAST-SLOW-DATA-001-fast-slow-data-and-register-interval-analysis.md`；`matrices/fast-slow-data-matrix.md`  
原文摘要：B接口2016 定义 SC 轮询 FSU 获取温湿度、电压、电流、电量、频率、开关状态等慢数据；FSU 主动上报告警、状态切换等事件为快数据。  
规则内容：当前 Emerson GET_DATA 真实链路归属慢数据查询通道，使用 SC -> FSU `FSUService` 调用，不属于 FSU -> SC 主动快数据。  
适用范围：Emerson GET_DATA run-once、正式采集、timeout/502 分析。  
影响模块：ReadOnlyRunOnce、RealHttpFsuServiceClient、registrationContext、message_log 运维诊断。  
实现要求：GET_DATA 可读取最新 LOGIN registration context，但不得在 GET_DATA 前主动触发 LOGIN；若真实 FSU 120 秒内重复 LOGIN，应作为注册态/现场链路风险记录，不得改写 GET_DATA 协议归属。  
测试要求：GET_DATA 测试报告必须标注慢数据通道、FSUService endpoint、是否使用 registrationContext。  
备注：2026-05-28 message_log 显示该 FSU 存在 32/33 秒重复 LOGIN，但当前缺少同表 GET_DATA 成败时间线，不能证明 timeout/502 由快慢通道混用直接导致。
