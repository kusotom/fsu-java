# 字典码表不明确 / 待确认事项

本文件仅记录码表、字典、枚举、命令码、信号编码相关的不明确项。协议流程和字段结构的不明确项仍保留在 `unknown-and-ambiguous-items.md`。

| 编号 | 原文位置 | 问题描述 | 影响范围 | 当前处理方式 | 需谁确认 | 备注 |
|---|---|---|---|---|---|---|
| DICT-UNKNOWN-001 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；表5 常量定义；抽取稿 L665-L697 | EVENT_LENGTH在表5出现两次，原始名称和码值均相同。 | 常量定义唯一性和生成代码常量时的去重策略 | 在dictionary-code-matrix.md保留两条原文行；实现时不得因去重丢失来源。 | 协议负责人 | 重复项位置：表5第4行和第14行 |
| DICT-UNKNOWN-002 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；表7 设备编码表；抽取稿 L854 | 室外配电设备序号39的C类局站设备编码原文为229，与同表编码规律和B类局站229重复。 | 设备编码解析、DeviceCode生成、字典导入 | dictionary-code-matrix.md按原文保留229；不得自动改为339。 | 协议负责人/厂家 |  |
| DICT-UNKNOWN-003 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；表6 EnumDeviceType L760-L799 与 表7 设备编码表 L802-L859 | EnumDeviceType将39~99标为预留；设备编码表又定义39室外配电设备和99非智能门禁。 | 设备类型枚举和设备编码字典一致性 | 两处均保留，标记冲突待确认。 | 协议负责人 |  |
| DICT-UNKNOWN-004 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；表9 报文类型定义；抽取稿 L913 | GET_HISDATA_ACK原文数据流方向为SC—>FSU，但作为响应从命名和上下文看应由FSU返回SC。 | 历史数据响应方向、联调请求路由 | dictionary-code-matrix.md保留原文方向SC—>FSU；命令规范中列为待确认。 | 协议负责人/厂家 |  |
| DICT-UNKNOWN-005 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；表10 LOGIN字段；抽取稿 L965-L968 | Reg_Mode描述中原文写作“新的注册械”，疑似“新的注册模式”但不能自行修正。 | 注册模式显示和文档展示 | 保留原文写法，规范化写法进入待确认。 | 协议负责人 |  |
| DICT-UNKNOWN-006 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx全文检索；openspec/protocols/binterface-2016/05-result-code.md | 2016 docx标准字段为Result，未定义ResultCode；平台存在ResultCode工程兼容字段。 | 协议标准判断、测试断言、前端展示 | dictionary-code-matrix.md将ResultCode列为工程解释，不作为协议规定。 | 项目负责人 |  |
| DICT-UNKNOWN-DUP-SIGNAL-0316005001 | /home/tom/桌面/中国铁塔动环监控设备信号字典表-精简版-20150416.xlsx；工作表「设备信号字典表」；信号ID=0316005001 | 同一信号ID在字典表中出现多次。 | SignalID唯一性、点位导入主键设计 | dictionary-code-matrix.md保留每条原始记录；导入平台前需确认唯一键策略。 | 项目负责人 | Excel行82 C类机房 第1路(市电)电压过低告警; Excel行83 C类机房 第2路(油机)有电压告警 |
| DICT-UNKNOWN-007 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；流程图/标题；抽取稿 L603-L615 | GET_FTP/SET_FTP在流程文字中出现GET_ FTP _ACK、SET_ FTP、SET_ FTP _ACK空格变体。 | PK_Type名称标准化、兼容解析 | 保留原文变体；标准命令矩阵采用表9和报文样例的GET_FTP/GET_FTP_ACK/SET_FTP/SET_FTP_ACK。 | 协议负责人 |  |
| DICT-UNKNOWN-008 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；SET_LOGININFO标题和表；抽取稿 L2517-L2601 | SET_LOGININFO在原文中出现SET_ LOGININFO、SET_ LOGININFO_ACK空格变体。 | PK_Type名称标准化、兼容解析 | 保留原文变体；标准命令矩阵采用表9的SET_LOGININFO/SET_LOGININFO_ACK。 | 协议负责人 |  |
| DICT-UNKNOWN-009 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；LOGIN样例；抽取稿 L983-L1007 | LOGIN字段表写Version，XML样例写<Vervion/>。 | LOGIN字段解析、XML fixture校验 | 两个原文写法都保留；实现可兼容但不能将Vervion写成协议唯一字段。 | 协议负责人/厂家 |  |
| DICT-UNKNOWN-010 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；LOGIN字段表；抽取稿 L952-L970 | LOGIN字段表写PaSCword，大小写异常；常见写法可能为Password，但原文未写Password。 | 登录认证字段解析 | 保留PaSCword原文；如兼容Password必须标注工程解释。 | 协议负责人/厂家 |  |
| DICT-UNKNOWN-011 | /home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；表8 数据结构定义；抽取稿 L877-L886 | TAlarm DeviceCode字段类型写char[DEVICEICODE_LEN]，常量表定义为DEVICECODE_LEN。 | TAlarm结构字段类型、代码生成 | 保留DEVICEICODE_LEN原文并在结构UNKNOWN中标注；实现引用DEVICECODE_LEN需说明工程解释。 | 协议负责人 |  |
