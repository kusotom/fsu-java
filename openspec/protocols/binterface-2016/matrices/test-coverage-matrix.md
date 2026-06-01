# 测试覆盖矩阵

| SPEC 编号 | 命令 / 结构 | 应测点 | 已有测试 | 缺失测试 | 是否需要真实 FSU | 备注 |
|---|---|---|---|---|---|---|
| SPEC-2016-CMD-LOGIN-001 | LOGIN | LOGIN请求命令码为101，响应LOGIN_ACK为102 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-LOGIN-002 | LOGIN | LOGIN请求Info包含UserName、PaSCword、FsuId、FsuCode、FsuIP、网络模块、FSU厂商型号、Version/DictVersion和DeviceList | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-LOGIN-003 | LOGIN | 同一个FSU两次注册之间的最小时间间隔不小于120秒 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-LOGIN-004 | LOGIN | 新注册模式中LOGIN_ACK同时返回采集机IP SCIP；原注册模式需后续SET_LOGININFO下发采集机信息 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-LOGIN-005 | LOGIN | 样例字段Vervion与表格Version不一致，按UNKNOWN登记，不擅自归一为协议规定 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：待确认 |
| SPEC-2016-CMD-LOGOUT-001 | LOGOUT | LOGOUT请求命令码103，LOGOUT_ACK响应命令码104 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-LOGOUT-002 | LOGOUT | LOGOUT表格定义Info.FsuId，但XML样例为<Info/>，字段是否必填进入UNKNOWN | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：待确认 |
| SPEC-2016-CMD-LOGOUT-003 | LOGOUT | 登出成功后FSU主动拆除IPSec/L2TP隧道连接 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-HEARTBEAT-001 | HEARTBEAT | B接口2016原文未在报文类型表定义HEARTBEAT命令码 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：待确认 |
| SPEC-2016-CMD-HEARTBEAT-002 | HEARTBEAT | SC心跳功能要求定期获取FSU状态信息作为应用层心跳 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-HEARTBEAT-003 | HEARTBEAT | FSU收到SC心跳报文需判断FSUID一致，一致返回成功，否则失败 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-HEARTBEAT-004 | HEARTBEAT | 当前工程若存在HEARTBEAT入站命令，只能作为工程兼容/历史实现，不能作为标准2016命令 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：工程解释 |
| SPEC-2016-CMD-GET-DATA-001 | GET_DATA | GET_DATA请求命令码401，GET_DATA_ACK响应命令码402 | `Bif2016Connection007RealGetDataIntegrationTest`、`BInterface2016GetDataServiceTest`、`BInterface2016ReadOnlyRunOnceProbeIntegrationTest` | minimal/wildcard 超时原因需专项排查 | 需要显式只读run-once | BIF2016-CONNECTION-007 all-devices/single-device 真实 FSU 返回 ACK=402 |
| SPEC-2016-CMD-GET-DATA-002 | GET_DATA | 请求Info包含FsuId、FsuCode、DeviceList/Device@Id/Code及Id列表 | `Bif2016Connection007RealGetDataIntegrationTest` 保存 root raw request；请求使用 `<Id>signalId</Id>` 且不含 `<TSemaphore>` | 正式入库前需映射验收 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-DATA-003 | GET_DATA | Device.Code=99999999999999时返回该FSU所有设备监控点值并忽略Id列表 | `Bif2016Connection007RealGetDataIntegrationTest` 已尝试 minimal/wildcard | 真实 FSU 对全 9 通配读取超时，需独立排查 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-DATA-004 | GET_DATA | Id=9999999999时返回该设备所有监控点值 | `Bif2016Connection007RealGetDataIntegrationTest` 已尝试 `<Id>9999999999</Id>` | 真实 FSU 对全 9 通配读取超时，需独立排查 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-DATA-005 | GET_DATA | 响应Values使用DeviceList/Device/TSemaphore结构 | `Bif2016Connection007RealGetDataIntegrationTest` all-devices 返回 8 个 TSemaphore，single-device 返回 2 个 TSemaphore | 映射到 monitoring_point / realtime_data 待后续验收 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-DICT-SIGNAL-001 | 标准信号字典 | 真实 GET_DATA 返回的 8 个 TSemaphore@Id 应能按标准字典反查名称、类型、单位和导入草案 | `SPEC-DICT-MAPPING-001` 文档审计；`SPEC-UNIT-COMPLETE-001` 原始 Excel/CSV 单位复核；`standard-signal-dictionary-dry-run.csv` 484 rows 全量读取；8/8 exact | DI 0/1 编码、`0407107001` 单位、DeviceID 实例绑定仍需现场/厂家复核 | 否 | 只读字典反查；未访问 FSU，未写数据库；0407107001原始字典单位为空 |
| SPEC-2016-IMPL-SOAP-XMLDATA-ESCAPED-001 | SOAP/RPC | Emerson出站RPC `xmlData` 使用 escaped string 而非DOM child | `BIF2016-RPCXML-001`；`mvn test -Dtest='*BInterface*'` 355 tests reported PASS；raw `data-mapping-008-retry-get-data-request.xml` | 需防止后续重构回退 | 否 | 标准层引用WSDL字符串模型，厂商行为见Emerson Profile |
| SPEC-2016-PROFILE-EMERSON-SOAP-PREFIX-001 | SOAP/RPC | Emerson RPC Adapter 使用 `soap:` 前缀 | `BIF2016-RPCXML-001`；raw request 使用 `soap:Envelope` | 需保留解析端按namespace URI处理响应 `SOAP-ENV` | 否 | 厂商兼容行为，不是标准强制规则 |
| SPEC-2016-PROFILE-EMERSON-008 / 011 | GET_DATA/字典映射/单位 | 8个真实SignalID标准字典exact，0418101001/0418102001纠偏为环境温湿度，单位按原始Excel复核 | `SPEC-DICT-MAPPING-001`；`SPEC-UNIT-COMPLETE-001`；`DATA-MAPPING-007`；`DATA-MAPPING-008-RETRY` | DI 0/1编码、0407107001单位仍待复核 | 否 | 已生成monitoring_point导入与realtime_data验收记录；DI显示无单位，0407107001显示单位待确认 |
| UNKNOWN-2016-OPS-NETWORK-001 | 运维/网络 | FSU/代理链路间歇性可达，不归为协议规则 | `BIF2016-NETWORK-001`；`DATA-MAPPING-008` | 需网络稳定后复验正式API全链路 | 需要显式只读run-once | 不用curl替代Java正式采集链路 |
| SPEC-2016-CMD-GET-HISDATA-001 | GET_HISDATA | GET_HISDATA请求命令码403，GET_HISDATA_ACK响应命令码404 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-HISDATA-002 | GET_HISDATA | 请求增加StartTime和EndTime字段 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-HISDATA-003 | GET_HISDATA | 历史数据按轮询周期1小时，一个轮询周期只取1个点 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-HISDATA-004 | GET_HISDATA | 响应TSemaphore包含RecordTime属性 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-SEND-ALARM-001 | SEND_ALARM | SEND_ALARM请求命令码501，SEND_ALARM_ACK响应命令码502 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-SEND-ALARM-002 | SEND_ALARM | 请求Info.Values.TAlarmList包含一个或多个TAlarm | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-SEND-ALARM-003 | SEND_ALARM | 告警序号10位数字，不足补0，0~4294967295范围，告警结束与开始序号相同 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-SEND-ALARM-004 | SEND_ALARM | 所有文本描述中不能包含半角<或>字符 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 不默认需要 | 类型：协议规定 |
| SPEC-2016-CMD-SET-POINT-001 | SET_POINT | SET_POINT请求命令码1001，SET_POINT_ACK响应命令码1002 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-CMD-SET-POINT-002 | SET_POINT | SET_POINT请求Value.DeviceList下携带TSemaphore设置值 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-SECURITY-SET-POINT-001 | SET_POINT | SET_POINT为遥控/遥调控制类高风险命令，当前阶段默认禁用 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：工程解释 |
| SPEC-2016-CMD-SET-POINT-003 | SET_POINT | SET_POINT_ACK按Device返回SuccessList和FailList | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-CMD-TIME-CHECK-001 | TIME_CHECK | TIME_CHECK请求命令码1301，TIME_CHECK_ACK响应命令码1302 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-TIME-CHECK-002 | TIME_CHECK | 请求Info.Time使用TTime结构Years/Month/Day/Hour/Minute/Second | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-TIME-CHECK-003 | TIME_CHECK | TIME_CHECK会更新FSU时间，虽非SET命名，仍需审计和防误触真实设备 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：工程解释 |
| SPEC-2016-CMD-GET-LOGININFO-001 | GET_LOGININFO | GET_LOGININFO请求命令码1501，GET_LOGININFO_ACK响应命令码1502 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-LOGININFO-002 | GET_LOGININFO | 响应包含IPSecUser/IPSecPWD/IPSecIP/SCIP/DeviceList/Result | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-SECURITY-GET-LOGININFO-001 | GET_LOGININFO | GET_LOGININFO返回密码类敏感信息，日志和前端展示必须脱敏 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：工程解释 |
| SPEC-2016-CMD-SET-LOGININFO-001 | SET_LOGININFO | SET_LOGININFO请求命令码1503，SET_LOGININFO_ACK响应命令码1504 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-CMD-SET-LOGININFO-002 | SET_LOGININFO | 原有注册模式中LOGIN_ACK后由SET_LOGININFO下发采集机信息 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-SECURITY-SET-LOGININFO-001 | SET_LOGININFO | SET_LOGININFO修改注册/网络配置，默认禁用且必须授权审计 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：工程解释 |
| SPEC-2016-CMD-SET-LOGININFO-003 | SET_LOGININFO | 表格和样例中SET_ LOGININFO存在空格，标准命令名按报文类型表为SET_LOGININFO，差异入UNKNOWN | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：待确认 |
| SPEC-2016-CMD-GET-FTP-001 | GET_FTP | GET_FTP请求命令码1601，GET_FTP_ACK响应命令码1602 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-FTP-002 | GET_FTP | GET_FTP_ACK响应包含UserName、Password、Result | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-SECURITY-GET-FTP-001 | GET_FTP | FTP密码必须脱敏处理，原始报文保存应受限访问 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：工程解释 |
| SPEC-2016-CMD-SET-FTP-001 | SET_FTP | SET_FTP请求命令码1603，SET_FTP_ACK响应命令码1604 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-CMD-SET-FTP-002 | SET_FTP | 请求包含UserName和Password字段 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-CMD-SET-FTP-003 | SET_FTP | SET_FTP样例出现重复<PK_Type>SET_FTP</PK_Type>和结构化PK_Type，进入UNKNOWN | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：待确认 |
| SPEC-2016-SECURITY-SET-FTP-001 | SET_FTP | SET_FTP修改设备FTP配置，默认禁用且必须授权审计 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：工程解释 |
| SPEC-2016-CMD-GET-FSUINFO-001 | GET_FSUINFO | GET_FSUINFO请求命令码1701，GET_FSUINFO_ACK响应命令码1702 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-FSUINFO-002 | GET_FSUINFO | 响应包含TFSUStatus结构和Result | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-FSUINFO-003 | GET_FSUINFO | SC心跳功能可通过定期获取FSU状态信息实现 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-SET-FSUREBOOT-001 | SET_FSUREBOOT | SET_FSUREBOOT请求命令码1801，SET_FSUREBOOT_ACK响应命令码1802 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-CMD-SET-FSUREBOOT-002 | SET_FSUREBOOT | 重启命令返回成功标志后FSU重启 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-CMD-SET-FSUREBOOT-003 | SET_FSUREBOOT | 自动升级流程可通过FTP/USB上传升级文件后重启完成 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-SECURITY-SET-FSUREBOOT-001 | SET_FSUREBOOT | SET_FSUREBOOT为远程重启高风险命令，当前阶段默认禁用 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：工程解释 |
| SPEC-2016-CMD-GET-THRESHOLD-001 | GET_THRESHOLD | GET_THRESHOLD请求命令码1901，GET_THRESHOLD_ACK响应命令码1902 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-THRESHOLD-002 | GET_THRESHOLD | Device.Code=99999999999999时返回该FSU所有设备监控点门限数据并忽略Id列表 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-GET-THRESHOLD-003 | GET_THRESHOLD | 响应Values使用TThreshold结构 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | 需要显式只读run-once | 类型：协议规定 |
| SPEC-2016-CMD-SET-THRESHOLD-001 | SET_THRESHOLD | SET_THRESHOLD请求命令码2001，SET_THRESHOLD_ACK响应命令码2002 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-CMD-SET-THRESHOLD-002 | SET_THRESHOLD | SET_THRESHOLD请求Value.DeviceList下携带TThreshold门限值 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-CMD-SET-THRESHOLD-003 | SET_THRESHOLD | SET_THRESHOLD_ACK按Device返回SuccessList和FailList | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：协议规定 |
| SPEC-2016-SECURITY-SET-THRESHOLD-001 | SET_THRESHOLD | SET_THRESHOLD修改设备门限，默认禁用且必须授权审计 | 待逐项代码审计 | fixture/边界/错误/RAW留痕测试需补齐 | SET类禁止真实FSU | 类型：工程解释 |
| SPEC-2016-STRUCT-TSEMAPHORE-001 | TSemaphore | 解析字段：数据类型 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TSEMAPHORE-002 | TSemaphore | 解析字段：监控点ID | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 结构表写ID，XML属性写Id |
| SPEC-2016-STRUCT-TSEMAPHORE-003 | TSemaphore | 解析字段：实测值 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TSEMAPHORE-004 | TSemaphore | 解析字段：设置值 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TSEMAPHORE-005 | TSemaphore | 解析字段：状态 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TSEMAPHORE-006 | TSemaphore | 解析字段：历史记录时间 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 实时数据无该属性 |
| SPEC-2016-STRUCT-TTHRESHOLD-001 | TThreshold | 解析字段：数据类型 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TTHRESHOLD-002 | TThreshold | 解析字段：监控点ID | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TTHRESHOLD-003 | TThreshold | 解析字段：门限值 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TTHRESHOLD-004 | TThreshold | 解析字段：绝对阀值 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 原文写阀值 |
| SPEC-2016-STRUCT-TTHRESHOLD-005 | TThreshold | 解析字段：百分比阀值 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TTHRESHOLD-006 | TThreshold | 解析字段：状态 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TALARM-001 | TAlarm | 解析字段：告警序号 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 10位数字 |
| SPEC-2016-STRUCT-TALARM-002 | TAlarm | 解析字段：监控点ID | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TALARM-003 | TAlarm | 解析字段：FSU ID | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 表写FSUID，XML写FsuId |
| SPEC-2016-STRUCT-TALARM-004 | TAlarm | 解析字段：FSU编码 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TALARM-005 | TAlarm | 解析字段：设备ID | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 大小写差异 |
| SPEC-2016-STRUCT-TALARM-006 | TAlarm | 解析字段：设备编码 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 结构表存在DEVICEICODE_LEN拼写问题 |
| SPEC-2016-STRUCT-TALARM-007 | TAlarm | 解析字段：告警时间 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | YYYY-MM-DD HH:mm:ss |
| SPEC-2016-STRUCT-TALARM-008 | TAlarm | 解析字段：告警级别 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 表与枚举命名不完全一致 |
| SPEC-2016-STRUCT-TALARM-009 | TAlarm | 解析字段：开始/结束 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TALARM-010 | TAlarm | 解析字段：40字节以内告警描述 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 不得包含半角<或> |
| SPEC-2016-STRUCT-TFSUSTATUS-001 | TFSUStatus | 解析字段：CPU使用率 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TFSUSTATUS-002 | TFSUStatus | 解析字段：内存使用率 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TDEVICE-001 | TDevice | 解析字段：资源系统设备ID | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 工程解释 |
| SPEC-2016-STRUCT-TDEVICE-002 | TDevice | 解析字段：设备编码 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 工程解释 |
| SPEC-2016-STRUCT-TDEVICE-003 | TDevice | 解析字段：监控点ID列表 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TDEVICE-004 | TDevice | 解析字段：监控点值 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TDEVICE-005 | TDevice | 解析字段：门限值 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TSIGNAL-001 | TSignal | 解析字段：同类信号顺序号 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 低位到高位定义 |
| SPEC-2016-STRUCT-TSIGNAL-002 | TSignal | 解析字段：设备中具体信号流水号00-99，省增信号从70开始 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TSIGNAL-003 | TSignal | 解析字段：0 DI, 1 AI, 2 DO, 3 AO | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TSIGNAL-004 | TSignal | 解析字段：设备类型 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 见设备/系统类型编码表 |
| SPEC-2016-STRUCT-TSIGNAL-005 | TSignal | 解析字段：局站类型 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-STRUCT-TSIGNAL-006 | TSignal | 解析字段：预留扩展，暂固定为0 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-FIELD-FSUID-001 | Common Fields | 解析字段：FSU资源ID | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 大小写存在差异 |
| SPEC-2016-FIELD-FSUCODE-001 | Common Fields | 解析字段：FSU编码 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 14字节 |
| SPEC-2016-FIELD-DEVICEID-001 | Common Fields | 解析字段：设备ID | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 大小写存在差异 |
| SPEC-2016-FIELD-DEVICECODE-001 | Common Fields | 解析字段：设备编码 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 14字节 |
| SPEC-2016-FIELD-ID-001 | Common Fields | 解析字段：监控点ID | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 10字节 |
| SPEC-2016-FIELD-RESULT-001 | Common Fields | 解析字段：成功/失败 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | 1成功0失败 |
| SPEC-2016-FIELD-DEVICELIST-001 | Common Fields | 解析字段：设备列表 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 |  |
| SPEC-2016-FIELD-VALUES-001 | Common Fields | 解析字段：值列表/写值 | 待逐项代码审计 | 大小写/缺失/非法值测试 | 否 | GET响应用Values，SET请求用Value |
| SPEC-2016-STRUCT-TTIME-001 | TTime | TIME_CHECK 年字段 | 待逐项代码审计 | TTime完整字段和越界测试 | 否 |  |
| SPEC-2016-STRUCT-TTIME-002 | TTime | TIME_CHECK 月字段 | 待逐项代码审计 | TTime完整字段和越界测试 | 否 |  |
| SPEC-2016-STRUCT-TTIME-003 | TTime | TIME_CHECK 日字段 | 待逐项代码审计 | TTime完整字段和越界测试 | 否 |  |
| SPEC-2016-STRUCT-TTIME-004 | TTime | TIME_CHECK 时字段 | 待逐项代码审计 | TTime完整字段和越界测试 | 否 |  |
| SPEC-2016-STRUCT-TTIME-005 | TTime | TIME_CHECK 分字段 | 待逐项代码审计 | TTime完整字段和越界测试 | 否 |  |
| SPEC-2016-STRUCT-TTIME-006 | TTime | TIME_CHECK 秒字段 | 待逐项代码审计 | TTime完整字段和越界测试 | 否 |  |
| SPEC-2016-STRUCT-TGPS-001 | TGPS | GPS FSUID字段 | 无 | GPS XML样例缺失 | 否 | 当前未实现 |
| SPEC-2016-STRUCT-TGPS-002 | TGPS | GPS Lag字段 | 无 | 字段拼写确认测试 | 否 | 待确认 |
| SPEC-2016-STRUCT-TGPS-003 | TGPS | GPS Lat字段 | 无 | GPS XML样例缺失 | 否 | 当前未实现 |
