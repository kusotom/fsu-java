# 枚举与常量表

## 1. 来源

| 项 | 内容 |
|---|---|
| 主源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 辅助同版文档 | `/home/tom/桌面/FSU/B接口协议/B接口协议2016.docx` |
| WSDL辅助文件 | `/home/tom/桌面/FSU/WSDL协议/SCService.wsdl`；`/home/tom/桌面/FSU/WSDL协议/FSUService.wsdl` |
| 主源SHA256 | `eb049933533db97761aa1514cca449dc6f143a686c1425ba4ff087eb38574c1b` |
| 辅助docx SHA256 | `d481a1f0b3d695b3a875b75976490319c4db4278b599b0f2191c5601b3b71bbf` |
| 抽取稿 | `/tmp/binterface2016_docx_extract.md`，3297行，仅作定位索引 |


## 2. 枚举定义

| SPEC编号 | 枚举 | 取值 | 说明 | 来源 |
|---|---|---|---|---|
| SPEC-2016-ENUM-RIGHTMODE-001 | EnumRightMode | INVALID=0, LEVEL1=1, LEVEL2=2 | FSU向SC提供权限；LEVEL1只读，LEVEL2读写 | 抽取稿 L703-L710 |
| SPEC-2016-ENUM-RESULT-001 | EnumResult | FAILURE=0, SUCCESS=1 | 报文返回结果 | 抽取稿 L711-L713 |
| SPEC-2016-ENUM-TYPE-001 | EnumType | STATION=0, DEVICE=1, DI=2, AI=3, DO=4, AO=5, AREA=9 | 监控系统数据种类 | 抽取稿 L714-L723 |
| SPEC-2016-ENUM-ALARMLEVEL-001 | EnumAlarmLevel | NOALARM=0, CRITICAL=1, MAJOR=2, MINOR=3, HINT=4 | 告警等级 | 抽取稿 L724-L730 |
| SPEC-2016-ENUM-ENABLE-001 | EnumEnable | DISABLE=0, ENABLE=1 | 使能属性 | 抽取稿 L731-L733 |
| SPEC-2016-ENUM-ACCESSCMODE-001 | EnumAcceSCMode | ASK_ANSWER=0, CHANGE_TRIGGER=1, TIME_TRIGGER=2, STOP=3 | 实时数据访问方式 | 抽取稿 L734-L739 |
| SPEC-2016-ENUM-STATE-001 | EnumState | NOALARM=0, CRITICAL=1, MAJOR=2, MINOR=3, HINT=4, OPEVENT=5, INVALID=6 | 数据值状态 | 抽取稿 L740-L748 |
| SPEC-2016-ENUM-FLAG-001 | EnumFlag | BEGIN, END | 告警开始/结束 | 抽取稿 L749-L751 |
| SPEC-2016-ENUM-ALARMMODE-001 | EnumAlarmMode | NOALARM=0, CRITICAL=1, MAJOR=2, MINOR=3, HINT=4 | 告警等级设定模式 | 抽取稿 L752-L758 |
| SPEC-2016-ENUM-STATIONTYPE-001 | EnumStationType | 0特殊，1 A级，2 B级，3 C级，4 D级，5-9保留 | 局站类型 | 抽取稿 L759-L770 |
| SPEC-2016-ENUM-MODIFYTYPE-001 | EnumModifyType | ADDNONODES=0, ADDINNODES=1, DELETE=2, MODIFYNONODES=3, MODIFYINNODES=4 | 对象属性修改类型 | 抽取稿 L771-L777 |
| SPEC-2016-ENUM-DEVICETYPE-001 | EnumDeviceType | 1-38已定义，39-99预留 | 设备类型 | 抽取稿 L778-L799 |
| SPEC-2016-ENUM-DEVICECODE-001 | EnumDeviceCode | 见设备编码表 | A/B/C/D局站设备编码矩阵 | 抽取稿 L800-L859 |

## 3. 处理要求

枚举值只能按原文定义落为协议规定。平台若增加枚举别名，应标注为工程解释，并在测试中验证不会误用2024码表。
