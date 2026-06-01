# 协议不明确 / 待确认事项

| 编号 | 原文位置 | 问题描述 | 影响范围 | 当前处理方式 | 需谁确认 | 备注 |
|---|---|---|---|---|---|---|
| UNKNOWN-2016-001 | 抽取稿 L214-L220 | 主docx写WSDL见附件，但正文未展开WSDL内容；本次使用外部WSDL辅助文件 | SOAP/WSDL实现 | 按辅助WSDL和真实设备双校验 | 协议负责人/厂家 | 不得仅凭旧手册写入 |
| UNKNOWN-2016-002 | 抽取稿 L664-L698 | 常量表中EVENT_LENGTH重复定义 | 字段长度校验 | 保留原文，避免重复生成两个不同常量 | 协议负责人 |  |
| UNKNOWN-2016-003 | 抽取稿 L952-L956 | LOGIN表中FsuCode类型写char[FSUID_LEN]而非常量FSUCODE_LEN | LOGIN字段校验 | 兼容14字节，记录差异 | 协议负责人 |  |
| UNKNOWN-2016-004 | 抽取稿 L1037-L1040 | LOGIN样例字段为<Vervion/>，表格字段为Version | LOGIN解析 | 兼容解析Version/Vervion，标准不擅自改写 | 厂家/协议负责人 |  |
| UNKNOWN-2016-005 | 抽取稿 L1129-L1165 | LOGOUT表定义Info.FsuId，但XML样例为<Info/> | LOGOUT实现 | 兼容空Info和FsuId，需确认必填性 | 厂家 |  |
| UNKNOWN-2016-006 | 抽取稿 L1392-L1432 | GET_DATA请求样例为<FsuID/>，多数其他位置为<FsuId/> | GET_DATA解析 | 兼容大小写，输出保留raw | 厂家/协议负责人 |  |
| UNKNOWN-2016-007 | 抽取稿 L742-L777, L877-L889 | TAlarm.AlarmLevel结构表写EnumState，但枚举表有EnumAlarmLevel | SEND_ALARM字段类型 | 解析按枚举数值兼容，文档保留差异 | 协议负责人 |  |
| UNKNOWN-2016-008 | 抽取稿 L880-L883 | TAlarm结构表中DeviceCode类型写DEVICEICODE_LEN，常量为DEVICECODE_LEN | TAlarm字段长度 | 按DEVICECODE_LEN工程解释，原拼写保留 | 协议负责人 |  |
| UNKNOWN-2016-009 | 抽取稿 L2512-L2620 | SET_LOGININFO表/样例出现SET_ LOGININFO或SET_ LOGININFO_ACK空格 | SET_LOGININFO命令名 | PK_Type表以SET_LOGININFO/ACK为主，兼容空格变体需测试 | 厂家 |  |
| UNKNOWN-2016-010 | 抽取稿 L2762-L2821 | SET_FTP请求样例出现重复PK_Type：纯文本和Name+Code结构 | SET_FTP XML构造 | 不得照搬重复结构真实执行；需厂家确认 | 厂家 |  |
| UNKNOWN-2016-011 | 抽取稿 L3251-L3254 | 第9章SC心跳功能未定义HEARTBEAT PK_Type和XML样例 | 心跳实现 | 标准路径暂按GET_FSUINFO；工程HEARTBEAT标非标准 | 协议负责人/厂家 |  |
| UNKNOWN-2016-012 | 抽取稿 L3257-L3282 | 门限值、回差、延时具体参数引用统一信号字典表，docx未展开 | 门限配置/告警规则 | 引用外部字典基线，不在2016 docx中擅自补值 | 标准字典负责人 |  |
| UNKNOWN-2016-013 | 抽取稿 L3297 | “新的注册模式”段首误写“原有注册模式是...” | 注册模式说明 | 按上下文理解为新注册模式，但不改原文 | 协议负责人 |  |
| UNKNOWN-2016-014 | docx媒体/嵌入对象 | Visio/EMF图中可能存在不可抽取文字 | 流程图细节 | 已按相邻正文和抽取文字整理；不从不可见图像推断强规则 | 协议负责人 |  |
| UNKNOWN-2016-015 | 抽取稿 L887-L890 | TGPS字段Lag说明为经度，疑似Longitude拼写问题；FSUID类型long与其他FSU ID字符串定义不一致 | GPS扩展能力 | 保留原文Lag并建立t-gps.md，不进入主链路 | 协议负责人/厂家 |  |
| UNKNOWN-2016-OPS-NETWORK-001 | BIF2016-NETWORK-001 / DATA-MAPPING-008 | FSU/代理链路间歇性可达：curl代理、Java HttpURLConnection/JDK HttpClient/raw socket均曾成功，后续同一类请求仍可能502或timeout | 真实FSU验收、正式API全链路 | 归入运维风险和实现注意事项，不写成协议规则；保留Java正式链路，不以curl替代 | 运维/现场网络负责人 | 非单一Header、Content-Length、SOAPAction、User-Agent或客户端类型问题 |
| UNKNOWN-2016-OPS-REGISTER-INTERVAL-001 | SPEC-FAST-SLOW-DATA-001 / `b_interface_message_log` 只读查询 | 真实 FSU `51051243812345` 存在 32/33 秒间隔的重复 LOGIN，低于 B接口2016 同一 FSU 两次注册最小间隔 120 秒要求；但当前日志未记录同时间线 GET_DATA 成败，无法证明 timeout/502 主要由重复注册导致 | LOGIN 注册态、registrationContext、GET_DATA 真实验收解释 | 归为中等相关风险：建议实现 120 秒内重复 LOGIN 幂等/节流记录，message_log 增加 registerIntervalSeconds；不得把 120 秒写成 GET_DATA 查询频率限制 | 运维/现场网络负责人/后端负责人 | GET_DATA 仍归属慢数据查询通道；该问题不是快慢数据通道混用的直接证据 |
| UNKNOWN-2016-DICT-DI-VALUE-001 | SPEC-DICT-MAPPING-001 / DATA-MAPPING-007 | 4个DI点位有normal/alarm文本，但真实TSemaphore MeasuredVal=0的业务显示语义仍待现场复核 | 前端展示、告警解释 | 不编造0/1含义；导入点位时保留待复核备注 | 厂家/现场人员 | 涉及烟雾、温度告警、湿度告警、水浸告警 |
| UNKNOWN-2016-DICT-0407107001-UNIT | SPEC-DICT-MAPPING-001 / SPEC-UNIT-COMPLETE-001 | `0407107001` 后半组电压在原始 Excel 行70与标准CSV中单位均为空 | 点位单位、前端展示 | 名称和类型按exact导入，单位继续标记待确认，不补造为V | 标准字典负责人/厂家 | 已复核原始字典仍为空；DATA-MAPPING-008 retry 已返回值0.0并入库 |
