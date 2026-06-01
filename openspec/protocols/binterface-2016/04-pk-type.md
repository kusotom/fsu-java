# PK_Type / 命令码

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

### [SPEC-2016-PKTYPE-001] PK_Type由Name和Code标识

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L894-L931，表9 报文类型定义；各命令XML样例  
原文摘要：报文类型表列出类型名称和类型代号；XML样例使用 `<PK_Type><Name>...</Name><Code>...</Code></PK_Type>`。  
规则内容：标准2016命令码以Name+Code为基准；仅在厂商Profile或兼容层中记录纯文本PK_Type行为。  
适用范围：所有命令。  
影响模块：BInterfaceCommand2016、BInterfacePkType、XML builder/parser。  
实现要求：2016主线不得使用2024冲突码。  
测试要求：每个命令必须断言Name和Code匹配。  
备注：表9不包含HEARTBEAT命令码。

## 3. 命令码总表

| 命令 | 请求 Code | ACK 命令 | ACK Code | 通信方向 | 规范文件 | SPEC 编号 | 当前实现状态 | 安全等级 |
|---|---:|---|---:|---|---|---|---|---|
| LOGIN | 101 | LOGIN_ACK | 102 | FSU -> SC | `commands/010-login.md` | SPEC-2016-CMD-LOGIN-001 | 部分实现 | 只读/认证 |
| LOGOUT | 103 | LOGOUT_ACK | 104 | FSU -> SC | `commands/020-logout.md` | SPEC-2016-CMD-LOGOUT-001 | 未实现 | 只读/会话 |
| HEARTBEAT | 未定义 | HEARTBEAT_ACK | 未定义 | SC -> FSU（工程心跳使用GET_FSUINFO） | `commands/030-heartbeat.md` | SPEC-2016-CMD-HEARTBEAT-001 | 工程存在/标准未定义 | 只读 |
| GET_DATA | 401 | GET_DATA_ACK | 402 | SC -> FSU | `commands/040-get-data.md` | SPEC-2016-CMD-GET-DATA-001 | 部分实现 | 只读 |
| GET_HISDATA | 403 | GET_HISDATA_ACK | 404 | SC -> FSU | `commands/050-get-hisdata.md` | SPEC-2016-CMD-GET-HISDATA-001 | 未实现 | 只读 |
| SEND_ALARM | 501 | SEND_ALARM_ACK | 502 | FSU -> SC | `commands/060-send-alarm.md` | SPEC-2016-CMD-SEND-ALARM-001 | 部分实现 | 只读/被动接收 |
| SET_POINT | 1001 | SET_POINT_ACK | 1002 | SC -> FSU | `commands/070-set-point.md` | SPEC-2016-CMD-SET-POINT-001, SPEC-2016-SECURITY-SET-POINT-001 | 安全禁用 | 控制类高风险 |
| TIME_CHECK | 1301 | TIME_CHECK_ACK | 1302 | SC -> FSU | `commands/080-time-check.md` | SPEC-2016-CMD-TIME-CHECK-001 | 部分实现 | 只读/时间修改需审计 |
| GET_LOGININFO | 1501 | GET_LOGININFO_ACK | 1502 | SC -> FSU | `commands/090-get-logininfo.md` | SPEC-2016-CMD-GET-LOGININFO-001, SPEC-2016-SECURITY-GET-LOGININFO-001 | 部分实现 | 只读/敏感信息 |
| SET_LOGININFO | 1503 | SET_LOGININFO_ACK | 1504 | SC -> FSU | `commands/100-set-logininfo.md` | SPEC-2016-CMD-SET-LOGININFO-001, SPEC-2016-SECURITY-SET-LOGININFO-001 | 未实现/安全禁用 | 写配置高风险 |
| GET_FTP | 1601 | GET_FTP_ACK | 1602 | SC -> FSU | `commands/110-get-ftp.md` | SPEC-2016-CMD-GET-FTP-001, SPEC-2016-SECURITY-GET-FTP-001 | 部分实现 | 只读/敏感信息 |
| SET_FTP | 1603 | SET_FTP_ACK | 1604 | SC -> FSU | `commands/120-set-ftp.md` | SPEC-2016-CMD-SET-FTP-001, SPEC-2016-SECURITY-SET-FTP-001 | 未实现/安全禁用 | 写配置高风险 |
| GET_FSUINFO | 1701 | GET_FSUINFO_ACK | 1702 | SC -> FSU | `commands/130-get-fsuinfo.md` | SPEC-2016-CMD-GET-FSUINFO-001 | 已实现/部分实现 | 只读 |
| SET_FSUREBOOT | 1801 | SET_FSUREBOOT_ACK | 1802 | SC -> FSU | `commands/140-set-fsureboot.md` | SPEC-2016-CMD-SET-FSUREBOOT-001, SPEC-2016-SECURITY-SET-FSUREBOOT-001 | 安全禁用 | 控制类高风险 |
| GET_THRESHOLD | 1901 | GET_THRESHOLD_ACK | 1902 | SC -> FSU | `commands/150-get-threshold.md` | SPEC-2016-CMD-GET-THRESHOLD-001 | 部分实现 | 只读 |
| SET_THRESHOLD | 2001 | SET_THRESHOLD_ACK | 2002 | SC -> FSU | `commands/160-set-threshold.md` | SPEC-2016-CMD-SET-THRESHOLD-001, SPEC-2016-SECURITY-SET-THRESHOLD-001 | 安全禁用 | 写配置高风险 |
