# 字段映射矩阵

| 协议字段 | 所属命令 / 结构 | 类型 | 必填 | 平台字段 | 数据表 / DTO / Entity | SPEC 编号 | 备注 |
|---|---|---|---|---|---|---|---|
| UserName | LOGIN | USER_LENGTH | 是 | username | 待代码审计确认 | SPEC-2016-FIELD-USERNAME-001 | 用户名 |
| PaSCword | LOGIN | PASSWORD_LEN | 是 | pascword | 待代码审计确认 | SPEC-2016-FIELD-PASCWORD-001 | 口令，原文拼写为PaSCword |
| FsuId | LOGIN | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | 资源系统FSU ID |
| FsuCode | LOGIN | char[FSUID_LEN]/FSUCODE_LEN | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码，表格类型与常量名不一致 |
| FsuIP | LOGIN | IP_LENGTH | 是 | fsuip | 待代码审计确认 | SPEC-2016-FIELD-FSUIP-001 | FSU内网IP |
| MacId | LOGIN | MAC_LENGTH | 否 | macid | 待代码审计确认 | SPEC-2016-FIELD-MACID-001 | 无线模块MAC |
| ImsiId | LOGIN | IMSI_LENGTH | 否 | imsiid | 待代码审计确认 | SPEC-2016-FIELD-IMSIID-001 | IMSI卡号 |
| NetworkType | LOGIN | NETWORKTYPE_LENGTH | 否 | networktype | 待代码审计确认 | SPEC-2016-FIELD-NETWORKTYPE-001 | 当前网络制式 |
| LockedNetworkType | LOGIN | NETWORKTYPE_LENGTH | 否 | lockednetworktype | 待代码审计确认 | SPEC-2016-FIELD-LOCKEDNETWORKTYPE-001 | 锁定网络制式 |
| Carrier | LOGIN | CARRIER_LENGTH | 否 | carrier | 待代码审计确认 | SPEC-2016-FIELD-CARRIER-001 | 运营商CT/CM/CU |
| NMVendor | LOGIN | NMVENDOR_LENGTH | 否 | nmvendor | 待代码审计确认 | SPEC-2016-FIELD-NMVENDOR-001 | 上网模块厂商 |
| NMType | LOGIN | NMTYPE_LENGTH | 否 | nmtype | 待代码审计确认 | SPEC-2016-FIELD-NMTYPE-001 | 上网模块型号 |
| Reg_Mode | LOGIN | REG_MODE_LENGTH | 否 | reg_mode | 待代码审计确认 | SPEC-2016-FIELD-REGMODE-001 | 注册模式 |
| FSUVendor | LOGIN | FSUVENDOR_LENGTH | 否 | fsuvendor | 待代码审计确认 | SPEC-2016-FIELD-FSUVENDOR-001 | FSU厂家 |
| FSUType | LOGIN | FSUTYPE_LENGTH | 否 | fsutype | 待代码审计确认 | SPEC-2016-FIELD-FSUTYPE-001 | FSU型号 |
| FSUClass | LOGIN | FSUCLASS_LENGTH | 否 | fsuclass | 待代码审计确认 | SPEC-2016-FIELD-FSUCLASS-001 | FSU应用类型 |
| Version/Vervion | LOGIN | VERSION_LENGTH | 否 | version_vervion | 待代码审计确认 | SPEC-2016-FIELD-VERSION-001 | 软件版本，样例拼写为Vervion |
| DictVersion | LOGIN | DICTVERSION_LENGTH | 否 | dictversion | 待代码审计确认 | SPEC-2016-FIELD-DICTVERSION-001 | 信号字典版本 |
| DeviceList/Device@Id/Code | LOGIN | n*DEVICEID_LEN | 是 | devicelist_device_id_code | 待代码审计确认 | SPEC-2016-FIELD-DEVICELIST-001 | FSU所接设备列表 |
| RightLevel | LOGIN | EnumRightMode | 是 | rightlevel | 待代码审计确认 | SPEC-2016-FIELD-RIGHTLEVEL-001 | SC返回权限 |
| SCIP | LOGIN | IP_LENGTH | 是 | scip | 待代码审计确认 | SPEC-2016-FIELD-SCIP-001 | SC采集机IP |
| FsuId | LOGOUT | char[FSUID_LEN] | 待确认 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | 表格定义但样例缺失 |
| Result | LOGOUT | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 登出成功/失败 |
| FsuId | HEARTBEAT | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | 用于FSUID一致性判断；心跳报文字段未单独定义 |
| FsuCode | HEARTBEAT | char[FSUCODE_LEN] | 待确认 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | GET_FSUINFO含该字段 |
| FsuId/FsuID | GET_DATA | char[FSUID_LEN] | 是 | fsuid_fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | 请求样例使用FsuID，其他处多用FsuId，大小写差异进入UNKNOWN |
| FsuCode | GET_DATA | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| Device@Id | GET_DATA | char[DEVICEID_LEN] | 是 | device_id | 待代码审计确认 | SPEC-2016-FIELD-ID-001 | 资源系统设备ID |
| Device@Code | GET_DATA | char[DEVICECODE_LEN] | 是 | device_code | 待代码审计确认 | SPEC-2016-FIELD-CODE-001 | 设备编码，支持14位全9通配 |
| Id | GET_DATA | char[ID_LENGTH] | 是 | id | 待代码审计确认 | SPEC-2016-FIELD-ID-001 | 监控点ID，支持10位全9通配 |
| Result | GET_DATA | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 请求数据成功与否 |
| TSemaphore | GET_DATA | TSemaphore | 否 | tsemaphore | 待代码审计确认 | SPEC-2016-FIELD-TSEMAPHORE-001 | 实时监控点数据 |
| FsuId | GET_HISDATA | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU ID |
| FsuCode | GET_HISDATA | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| StartTime | GET_HISDATA | char[TIME_LEN] | 是 | starttime | 待代码审计确认 | SPEC-2016-FIELD-STARTTIME-001 | 开始时间 |
| EndTime | GET_HISDATA | char[TIME_LEN] | 是 | endtime | 待代码审计确认 | SPEC-2016-FIELD-ENDTIME-001 | 结束时间 |
| Device@Id | GET_HISDATA | char[DEVICEID_LEN] | 是 | device_id | 待代码审计确认 | SPEC-2016-FIELD-ID-001 | 资源系统ID |
| Device@Code | GET_HISDATA | char[DEVICECODE_LEN] | 是 | device_code | 待代码审计确认 | SPEC-2016-FIELD-CODE-001 | 设备编码 |
| Id | GET_HISDATA | char[ID_LENGTH] | 是 | id | 待代码审计确认 | SPEC-2016-FIELD-ID-001 | 监控点ID |
| RecordTime | GET_HISDATA | char[DES_LENGTH] | 是 | recordtime | 待代码审计确认 | SPEC-2016-FIELD-RECORDTIME-001 | 历史记录时间 |
| SerialNo | SEND_ALARM | char[SERIALNO_LEN] | 是 | serialno | 待代码审计确认 | SPEC-2016-FIELD-SERIALNO-001 | 告警序号 |
| Id | SEND_ALARM | char[ID_LENGTH] | 是 | id | 待代码审计确认 | SPEC-2016-FIELD-ID-001 | 监控点ID |
| FsuId | SEND_ALARM | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU ID |
| FsuCode | SEND_ALARM | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| DeviceId | SEND_ALARM | char[DEVICEID_LEN] | 是 | deviceid | 待代码审计确认 | SPEC-2016-FIELD-DEVICEID-001 | 设备ID |
| DeviceCode | SEND_ALARM | char[DEVICECODE_LEN] | 是 | devicecode | 待代码审计确认 | SPEC-2016-FIELD-DEVICECODE-001 | 设备编码 |
| AlarmTime | SEND_ALARM | char[DES_LENGTH] | 是 | alarmtime | 待代码审计确认 | SPEC-2016-FIELD-ALARMTIME-001 | 告警时间 |
| AlarmLevel | SEND_ALARM | EnumState/EnumAlarmLevel | 是 | alarmlevel | 待代码审计确认 | SPEC-2016-FIELD-ALARMLEVEL-001 | 告警级别，结构表写EnumState |
| AlarmFlag | SEND_ALARM | EnumFlag | 是 | alarmflag | 待代码审计确认 | SPEC-2016-FIELD-ALARMFLAG-001 | 开始/结束 |
| AlarmDesc | SEND_ALARM | char[DES_LENGTH] | 是 | alarmdesc | 待代码审计确认 | SPEC-2016-FIELD-ALARMDESC-001 | 40字节以内告警描述 |
| Result | SEND_ALARM | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 接收确认结果 |
| FsuId | SET_POINT | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU ID |
| FsuCode | SET_POINT | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| Value/TSemaphore | SET_POINT | TSemaphore | 是 | value_tsemaphore | 待代码审计确认 | SPEC-2016-FIELD-VALUE-001 | 设置值 |
| SuccessList/Id | SET_POINT | long/ID_LENGTH | 否 | successlist_id | 待代码审计确认 | SPEC-2016-FIELD-SUCCESSLIST-001 | 写成功ID列表 |
| FailList/Id | SET_POINT | long/ID_LENGTH | 否 | faillist_id | 待代码审计确认 | SPEC-2016-FIELD-FAILLIST-001 | 写失败ID列表 |
| Result | SET_POINT | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 写成功/失败 |
| Years | TIME_CHECK | short | 是 | years | 待代码审计确认 | SPEC-2016-FIELD-YEARS-001 | 年 |
| Month | TIME_CHECK | char | 是 | month | 待代码审计确认 | SPEC-2016-FIELD-MONTH-001 | 月 |
| Day | TIME_CHECK | char | 是 | day | 待代码审计确认 | SPEC-2016-FIELD-DAY-001 | 日 |
| Hour | TIME_CHECK | char | 是 | hour | 待代码审计确认 | SPEC-2016-FIELD-HOUR-001 | 时 |
| Minute | TIME_CHECK | char | 是 | minute | 待代码审计确认 | SPEC-2016-FIELD-MINUTE-001 | 分 |
| Second | TIME_CHECK | char | 是 | second | 待代码审计确认 | SPEC-2016-FIELD-SECOND-001 | 秒 |
| Result | TIME_CHECK | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 同步成功/失败 |
| FsuId | GET_LOGININFO | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU ID |
| FsuCode | GET_LOGININFO | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| IPSecUser | GET_LOGININFO | USER_LENGTH | 是 | ipsecuser | 待代码审计确认 | SPEC-2016-FIELD-IPSECUSER-001 | IPSec用户名 |
| IPSecPWD | GET_LOGININFO | PASSWORD_LEN | 是 | ipsecpwd | 待代码审计确认 | SPEC-2016-FIELD-IPSECPWD-001 | IPSec密码 |
| IPSecIP | GET_LOGININFO | IP_LENGTH | 是 | ipsecip | 待代码审计确认 | SPEC-2016-FIELD-IPSECIP-001 | IPSec服务器IP |
| SCIP | GET_LOGININFO | IP_LENGTH | 是 | scip | 待代码审计确认 | SPEC-2016-FIELD-SCIP-001 | SC IP |
| DeviceList | GET_LOGININFO | n*DEVICEID_LEN | 是 | devicelist | 待代码审计确认 | SPEC-2016-FIELD-DEVICELIST-001 | DeviceID列表 |
| Result | GET_LOGININFO | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 成功/失败 |
| FsuId | SET_LOGININFO | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU ID |
| FsuCode | SET_LOGININFO | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| IPSecUser | SET_LOGININFO | USER_LENGTH | 是 | ipsecuser | 待代码审计确认 | SPEC-2016-FIELD-IPSECUSER-001 | IPSec用户名 |
| IPSecPWD | SET_LOGININFO | PASSWORD_LEN | 是 | ipsecpwd | 待代码审计确认 | SPEC-2016-FIELD-IPSECPWD-001 | IPSec密码 |
| IPSecIP | SET_LOGININFO | IP_LENGTH | 是 | ipsecip | 待代码审计确认 | SPEC-2016-FIELD-IPSECIP-001 | IPSec服务器IP |
| SCIP | SET_LOGININFO | IP_LENGTH | 是 | scip | 待代码审计确认 | SPEC-2016-FIELD-SCIP-001 | SC IP |
| DeviceList | SET_LOGININFO | n*DEVICEID_LEN | 是 | devicelist | 待代码审计确认 | SPEC-2016-FIELD-DEVICELIST-001 | DeviceID列表 |
| Result | SET_LOGININFO | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 设置成功/失败 |
| FsuId | GET_FTP | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU ID |
| FsuCode | GET_FTP | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| UserName | GET_FTP | USER_LENGTH | 是 | username | 待代码审计确认 | SPEC-2016-FIELD-USERNAME-001 | FTP登录名 |
| Password | GET_FTP | PASSWORD_LEN | 是 | password | 待代码审计确认 | SPEC-2016-FIELD-PASSWORD-001 | FTP密码 |
| Result | GET_FTP | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 成功/失败 |
| FsuId | SET_FTP | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU ID |
| FsuCode | SET_FTP | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| UserName | SET_FTP | USER_LENGTH | 是 | username | 待代码审计确认 | SPEC-2016-FIELD-USERNAME-001 | FTP登录名 |
| Password | SET_FTP | PASSWORD_LEN | 是 | password | 待代码审计确认 | SPEC-2016-FIELD-PASSWORD-001 | FTP密码 |
| Result | SET_FTP | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 设置成功/失败 |
| FsuId/FSUID | GET_FSUINFO | char[FSUID_LEN] | 是 | fsuid_fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | 请求使用FsuId，响应表格写FSUID，样例FsuId |
| FsuCode | GET_FSUINFO | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| CPUUsage | GET_FSUINFO | float | 是 | cpuusage | 待代码审计确认 | SPEC-2016-FIELD-CPUUSAGE-001 | CPU使用率 |
| MEMUsage | GET_FSUINFO | float | 是 | memusage | 待代码审计确认 | SPEC-2016-FIELD-MEMUSAGE-001 | 内存使用率 |
| Result | GET_FSUINFO | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 成功/失败 |
| FsuId/FSUID | SET_FSUREBOOT | char[FSUID_LEN] | 是 | fsuid_fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU ID，表格写FSUID，样例FsuId |
| FsuCode | SET_FSUREBOOT | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| Result | SET_FSUREBOOT | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 成功/失败 |
| FsuId | GET_THRESHOLD | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU ID |
| FsuCode | GET_THRESHOLD | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| Device@Id | GET_THRESHOLD | char[DEVICEID_LEN] | 是 | device_id | 待代码审计确认 | SPEC-2016-FIELD-ID-001 | 资源系统ID |
| Device@Code | GET_THRESHOLD | char[DEVICECODE_LEN] | 是 | device_code | 待代码审计确认 | SPEC-2016-FIELD-CODE-001 | 设备编码，全9通配 |
| Id | GET_THRESHOLD | char[ID_LENGTH] | 是 | id | 待代码审计确认 | SPEC-2016-FIELD-ID-001 | 监控点ID，全9通配 |
| TThreshold | GET_THRESHOLD | TThreshold | 否 | tthreshold | 待代码审计确认 | SPEC-2016-FIELD-TTHRESHOLD-001 | 门限数据 |
| Result | GET_THRESHOLD | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 成功/失败 |
| FsuId | SET_THRESHOLD | char[FSUID_LEN] | 是 | fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU ID |
| FsuCode | SET_THRESHOLD | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码 |
| TThreshold | SET_THRESHOLD | TThreshold | 是 | tthreshold | 待代码审计确认 | SPEC-2016-FIELD-TTHRESHOLD-001 | 门限值 |
| SuccessList/Id | SET_THRESHOLD | long/ID_LENGTH | 否 | successlist_id | 待代码审计确认 | SPEC-2016-FIELD-SUCCESSLIST-001 | 写成功ID列表 |
| FailList/Id | SET_THRESHOLD | long/ID_LENGTH | 否 | faillist_id | 待代码审计确认 | SPEC-2016-FIELD-FAILLIST-001 | 写失败ID列表 |
| Result | SET_THRESHOLD | EnumResult | 是 | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 写成功/失败 |
| Type | TSemaphore | EnumType | 是 | type | 待代码审计确认 | SPEC-2016-STRUCT-TSEMAPHORE-001 | 数据类型 |
| Id/ID | TSemaphore | char[ID_LENGTH] | 是 | id_id | 待代码审计确认 | SPEC-2016-STRUCT-TSEMAPHORE-002 | 监控点ID；结构表写ID，XML属性写Id |
| MeasuredVal | TSemaphore | float | 否 | measuredval | 待代码审计确认 | SPEC-2016-STRUCT-TSEMAPHORE-003 | 实测值 |
| SetupVal | TSemaphore | float | 否 | setupval | 待代码审计确认 | SPEC-2016-STRUCT-TSEMAPHORE-004 | 设置值 |
| Status | TSemaphore | EnumState | 是 | status | 待代码审计确认 | SPEC-2016-STRUCT-TSEMAPHORE-005 | 状态 |
| RecordTime | TSemaphore | char[DES_LENGTH] | 历史数据必填 | recordtime | 待代码审计确认 | SPEC-2016-STRUCT-TSEMAPHORE-006 | 历史记录时间；实时数据无该属性 |
| Type | TThreshold | EnumType | 是 | type | 待代码审计确认 | SPEC-2016-STRUCT-TTHRESHOLD-001 | 数据类型 |
| Id/ID | TThreshold | char[ID_LENGTH] | 是 | id_id | 待代码审计确认 | SPEC-2016-STRUCT-TTHRESHOLD-002 | 监控点ID |
| Threshold | TThreshold | float | 是 | threshold | 待代码审计确认 | SPEC-2016-STRUCT-TTHRESHOLD-003 | 门限值 |
| AbsoluteVal | TThreshold | float | 否 | absoluteval | 待代码审计确认 | SPEC-2016-STRUCT-TTHRESHOLD-004 | 绝对阀值；原文写阀值 |
| RelativeVal | TThreshold | float | 否 | relativeval | 待代码审计确认 | SPEC-2016-STRUCT-TTHRESHOLD-005 | 百分比阀值 |
| Status | TThreshold | EnumState | 是 | status | 待代码审计确认 | SPEC-2016-STRUCT-TTHRESHOLD-006 | 状态 |
| SerialNo | TAlarm | char[SERIALNO_LEN] | 是 | serialno | 待代码审计确认 | SPEC-2016-STRUCT-TALARM-001 | 告警序号；10位数字 |
| Id/ID | TAlarm | char[ID_LENGTH] | 是 | id_id | 待代码审计确认 | SPEC-2016-STRUCT-TALARM-002 | 监控点ID |
| FsuId/FSUID | TAlarm | char[FSUID_LEN] | 是 | fsuid_fsuid | 待代码审计确认 | SPEC-2016-STRUCT-TALARM-003 | FSU ID；表写FSUID，XML写FsuId |
| FsuCode | TAlarm | char[FSUCODE_LEN] | 是 | fsucode | 待代码审计确认 | SPEC-2016-STRUCT-TALARM-004 | FSU编码 |
| DeviceId/DeviceID | TAlarm | char[DEVICEID_LEN] | 是 | deviceid_deviceid | 待代码审计确认 | SPEC-2016-STRUCT-TALARM-005 | 设备ID；大小写差异 |
| DeviceCode | TAlarm | char[DEVICECODE_LEN] | 是 | devicecode | 待代码审计确认 | SPEC-2016-STRUCT-TALARM-006 | 设备编码；结构表存在DEVICEICODE_LEN拼写问题 |
| AlarmTime | TAlarm | char[DES_LENGTH] | 是 | alarmtime | 待代码审计确认 | SPEC-2016-STRUCT-TALARM-007 | 告警时间；YYYY-MM-DD HH:mm:ss |
| AlarmLevel | TAlarm | EnumState/EnumAlarmLevel | 是 | alarmlevel | 待代码审计确认 | SPEC-2016-STRUCT-TALARM-008 | 告警级别；表与枚举命名不完全一致 |
| AlarmFlag | TAlarm | EnumFlag | 是 | alarmflag | 待代码审计确认 | SPEC-2016-STRUCT-TALARM-009 | 开始/结束 |
| AlarmDesc | TAlarm | char[DES_LENGTH] | 是 | alarmdesc | 待代码审计确认 | SPEC-2016-STRUCT-TALARM-010 | 40字节以内告警描述；不得包含半角<或> |
| CPUUsage | TFSUStatus | float | 是 | cpuusage | 待代码审计确认 | SPEC-2016-STRUCT-TFSUSTATUS-001 | CPU使用率 |
| MEMUsage | TFSUStatus | float | 是 | memusage | 待代码审计确认 | SPEC-2016-STRUCT-TFSUSTATUS-002 | 内存使用率 |
| Device@Id | TDevice | char[DEVICEID_LEN] | 是 | device_id | 待代码审计确认 | SPEC-2016-STRUCT-TDEVICE-001 | 资源系统设备ID；工程解释 |
| Device@Code | TDevice | char[DEVICECODE_LEN] | 是 | device_code | 待代码审计确认 | SPEC-2016-STRUCT-TDEVICE-002 | 设备编码；工程解释 |
| Id | TDevice | char[ID_LENGTH] | 按命令 | id | 待代码审计确认 | SPEC-2016-STRUCT-TDEVICE-003 | 监控点ID列表 |
| TSemaphore | TDevice | TSemaphore | 按命令 | tsemaphore | 待代码审计确认 | SPEC-2016-STRUCT-TDEVICE-004 | 监控点值 |
| TThreshold | TDevice | TThreshold | 按命令 | tthreshold | 待代码审计确认 | SPEC-2016-STRUCT-TDEVICE-005 | 门限值 |
| digits1-3 | TSignal | 数字 | 是 | digits1-3 | 待代码审计确认 | SPEC-2016-STRUCT-TSIGNAL-001 | 同类信号顺序号；低位到高位定义 |
| digits4-5 | TSignal | 数字 | 是 | digits4-5 | 待代码审计确认 | SPEC-2016-STRUCT-TSIGNAL-002 | 设备中具体信号流水号00-99，省增信号从70开始 |
| digit6 | TSignal | 枚举数字 | 是 | digit6 | 待代码审计确认 | SPEC-2016-STRUCT-TSIGNAL-003 | 0 DI, 1 AI, 2 DO, 3 AO |
| digits7-8 | TSignal | 数字 | 是 | digits7-8 | 待代码审计确认 | SPEC-2016-STRUCT-TSIGNAL-004 | 设备类型；见设备/系统类型编码表 |
| digit9 | TSignal | 数字 | 是 | digit9 | 待代码审计确认 | SPEC-2016-STRUCT-TSIGNAL-005 | 局站类型 |
| digit10 | TSignal | 数字 | 是 | digit10 | 待代码审计确认 | SPEC-2016-STRUCT-TSIGNAL-006 | 预留扩展，暂固定为0 |
| FsuId/FSUID/FsuID | Common Fields | char[FSUID_LEN] | 按命令 | fsuid_fsuid_fsuid | 待代码审计确认 | SPEC-2016-FIELD-FSUID-001 | FSU资源ID；大小写存在差异 |
| FsuCode | Common Fields | char[FSUCODE_LEN] | 按命令 | fsucode | 待代码审计确认 | SPEC-2016-FIELD-FSUCODE-001 | FSU编码；14字节 |
| DeviceId/DeviceID | Common Fields | char[DEVICEID_LEN] | 按命令 | deviceid_deviceid | 待代码审计确认 | SPEC-2016-FIELD-DEVICEID-001 | 设备ID；大小写存在差异 |
| DeviceCode | Common Fields | char[DEVICECODE_LEN] | 按命令 | devicecode | 待代码审计确认 | SPEC-2016-FIELD-DEVICECODE-001 | 设备编码；14字节 |
| Id/ID | Common Fields | char[ID_LENGTH] | 按命令 | id_id | 待代码审计确认 | SPEC-2016-FIELD-ID-001 | 监控点ID；10字节 |
| Result | Common Fields | EnumResult | 按ACK | result | 待代码审计确认 | SPEC-2016-FIELD-RESULT-001 | 成功/失败；1成功0失败 |
| DeviceList | Common Fields | 列表 | 按命令 | devicelist | 待代码审计确认 | SPEC-2016-FIELD-DEVICELIST-001 | 设备列表 |
| Values/Value | Common Fields | 结构 | 按命令 | values_value | 待代码审计确认 | SPEC-2016-FIELD-VALUES-001 | 值列表/写值；GET响应用Values，SET请求用Value |
| Years | TTime | short | 是 | years | 待代码审计确认 | SPEC-2016-STRUCT-TTIME-001 | 年 |
| Month | TTime | char | 是 | month | 待代码审计确认 | SPEC-2016-STRUCT-TTIME-002 | 月 |
| Day | TTime | char | 是 | day | 待代码审计确认 | SPEC-2016-STRUCT-TTIME-003 | 日 |
| Hour | TTime | char | 是 | hour | 待代码审计确认 | SPEC-2016-STRUCT-TTIME-004 | 时 |
| Minute | TTime | char | 是 | minute | 待代码审计确认 | SPEC-2016-STRUCT-TTIME-005 | 分 |
| Second | TTime | char | 是 | second | 待代码审计确认 | SPEC-2016-STRUCT-TTIME-006 | 秒 |
| FSUID | TGPS | long | 待确认 | gps_fsu_id | 待设计 | SPEC-2016-STRUCT-TGPS-001 | FSU ID，类型与FSUID_LEN字符串差异 |
| Lag | TGPS | float | 待确认 | longitude | 待设计 | SPEC-2016-STRUCT-TGPS-002 | 经度，原文拼写Lag |
| Lat | TGPS | float | 待确认 | latitude | 待设计 | SPEC-2016-STRUCT-TGPS-003 | 纬度 |

## SPEC-DICT-MAPPING-001 实测 SignalID 映射补充

| SPEC 编号 | 实测字段 | 协议/字典字段 | monitoring_point 字段 | 来源 | 结论 |
|---|---|---|---|---|---|
| SPEC-2016-DICT-SIGNAL-0418002001 | `TSemaphore@Id=0418002001` | TSignal / 标准信号字典 | `point_code/signal_id/spid=0418002001`, `point_name=烟雾告警`, `point_type=DI`, `unit=''` | `standard-signal-dictionary-dry-run.csv` Excel 行 102；原始 Excel 单位列为空 | exact；DI无工程单位，0/1编码待复核 |
| SPEC-2016-DICT-SIGNAL-0418004001 | `TSemaphore@Id=0418004001` | TSignal / 标准信号字典 | `point_code/signal_id/spid=0418004001`, `point_name=温度告警`, `point_type=DI`, `unit=''` | Excel 行 104；原始 Excel 单位列为空 | exact；标准signal_name为温度，logic_category为温度告警 |
| SPEC-2016-DICT-SIGNAL-0418007001 | `TSemaphore@Id=0418007001` | TSignal / 标准信号字典 | `point_code/signal_id/spid=0418007001`, `point_name=湿度告警`, `point_type=DI`, `unit=''` | Excel 行 107；原始 Excel 单位列为空 | exact；标准signal_name为湿度，logic_category为湿度告警 |
| SPEC-2016-DICT-SIGNAL-0418101001 | `TSemaphore@Id=0418101001` | TSignal / 标准信号字典 | `point_code/signal_id/spid=0418101001`, `point_name=环境温度`, `point_type=AI`, `unit=℃` | Excel 行 109 | exact；纠正旧 CPU 误读 |
| SPEC-2016-DICT-SIGNAL-0418102001 | `TSemaphore@Id=0418102001` | TSignal / 标准信号字典 | `point_code/signal_id/spid=0418102001`, `point_name=环境湿度`, `point_type=AI`, `unit=%RH` | Excel 行 110 | exact；纠正旧内存误读 |
| SPEC-2016-DICT-SIGNAL-0418001001 | `TSemaphore@Id=0418001001` | TSignal / 标准信号字典 | `point_code/signal_id/spid=0418001001`, `point_name=水浸告警`, `point_type=DI`, `unit=''` | Excel 行 101；原始 Excel 单位列为空 | exact；DI无工程单位，0/1编码待复核 |
| SPEC-2016-DICT-SIGNAL-0407102001 | `TSemaphore@Id=0407102001` | TSignal / 标准信号字典 | `point_code/signal_id/spid=0407102001`, `point_name=总电压`, `point_type=AI`, `unit=V` | Excel 行 67 | exact；已入库验收 |
| SPEC-2016-DICT-SIGNAL-0407107001 | `TSemaphore@Id=0407107001` | TSignal / 标准信号字典 | `point_code/signal_id/spid=0407107001`, `point_name=后半组电压`, `point_type=AI`, `unit=待确认` | Excel 行 70；原始 Excel 与 CSV 单位列均为空 | exact，但单位不可补造；已入库验收 |

## SPEC-UNIT-COMPLETE-001 单位字段映射补充

| 场景 | monitoring_point.unit 建议 | 前端展示建议 | 依据 |
|---|---|---|---|
| DI/DO 字典单位列为空 | 保持空字符串或 NULL，不补数值单位 | 显示“无单位”，同时显示“DI编码待复核” | 原始 Excel 行 101/102/104/107 单位列为空；DI为开关/告警量 |
| AI/AO 字典单位列有值 | 写入字典单位 | 显示数值 + 字典单位 | `0418101001=℃`、`0418102001=%RH`、`0407102001=V` |
| AI/AO 字典单位列为空 | 保持“单位待确认”，不得凭名称推断 | 显示“单位待确认” | `0407107001` 原始 Excel 行 70 与 CSV 单位列均为空 |
