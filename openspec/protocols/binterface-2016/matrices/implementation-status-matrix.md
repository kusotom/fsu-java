# 实现状态矩阵

| 协议功能 | 命令 | SPEC 编号 | 当前实现状态 | 代码位置 | 缺口 | 优先级 | 备注 |
|---|---|---|---|---|---|---|---|
| 登录 | LOGIN | SPEC-2016-CMD-LOGIN-001, SPEC-2016-CMD-LOGIN-002, SPEC-2016-CMD-LOGIN-003, SPEC-2016-CMD-LOGIN-004, SPEC-2016-CMD-LOGIN-005 | 部分实现 | `backend/src/main/java/com/dcim/platform/module/binterface/service/LoginService.java` | 需按本SPEC复核2016字段PaSCword/DeviceList/rawAttributes | P0 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 登录 | LOGOUT | SPEC-2016-CMD-LOGOUT-001, SPEC-2016-CMD-LOGOUT-002, SPEC-2016-CMD-LOGOUT-003 | 未实现 | `未定位到LOGOUT Handler` | 需实现或明确返回notImplemented | P2 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 运维 | HEARTBEAT | SPEC-2016-CMD-HEARTBEAT-001, SPEC-2016-CMD-HEARTBEAT-002, SPEC-2016-CMD-HEARTBEAT-003, SPEC-2016-CMD-HEARTBEAT-004 | 工程存在/标准未定义 | `backend/src/main/java/com/dcim/platform/module/binterface/command/HeartbeatCommandHandler.java` | 需标注非标准2016；SC心跳应优先GET_FSUINFO | P1 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 采集 | GET_DATA | SPEC-2016-CMD-GET-DATA-001, SPEC-2016-CMD-GET-DATA-002, SPEC-2016-CMD-GET-DATA-003, SPEC-2016-CMD-GET-DATA-004, SPEC-2016-CMD-GET-DATA-005 | 已实现/真实显式Id复测通过 | `backend/src/main/java/com/dcim/platform/module/binterface/service/BInterface2016GetDataService.java`; `backend/src/test/java/com/dcim/platform/binterface/Bif2016Connection007RealGetDataIntegrationTest.java` | minimal/wildcard 全9请求读取超时；正式入库前仍需 DeviceID / SignalID / SPID 映射验收 | P0 | BIF2016-CONNECTION-007：显式 `<Id>` GET_DATA 返回 ACK=402 Result=1 和 TSemaphore 值；request 不含 TSemaphore |
| 采集 | GET_HISDATA | SPEC-2016-CMD-GET-HISDATA-001, SPEC-2016-CMD-GET-HISDATA-002, SPEC-2016-CMD-GET-HISDATA-003, SPEC-2016-CMD-GET-HISDATA-004 | 未实现 | `未定位到专用Service` | 缺历史数据请求/响应/RecordTime测试 | P2 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 告警 | SEND_ALARM | SPEC-2016-CMD-SEND-ALARM-001, SPEC-2016-CMD-SEND-ALARM-002, SPEC-2016-CMD-SEND-ALARM-003, SPEC-2016-CMD-SEND-ALARM-004 | 部分实现 | `backend/src/main/java/com/dcim/platform/module/binterface/service/SendAlarmService.java` | 需真实SEND_ALARM样本验收 | P0 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 控制 | SET_POINT | SPEC-2016-CMD-SET-POINT-001, SPEC-2016-CMD-SET-POINT-002, SPEC-2016-SECURITY-SET-POINT-001, SPEC-2016-CMD-SET-POINT-003 | 安全禁用/桩 | `backend/src/main/java/com/dcim/platform/module/binterface/command/SetPointCommandHandler.java` | 禁止真实执行；需安全门禁 | P1 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 运维 | TIME_CHECK | SPEC-2016-CMD-TIME-CHECK-001, SPEC-2016-CMD-TIME-CHECK-002, SPEC-2016-CMD-TIME-CHECK-003 | 部分实现 | `backend/src/main/java/com/dcim/platform/module/binterface/service/TimeCheckService.java` | 需2016 Code=1301/1302和时间修改审计 | P1 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 配置读取 | GET_LOGININFO | SPEC-2016-CMD-GET-LOGININFO-001, SPEC-2016-CMD-GET-LOGININFO-002, SPEC-2016-SECURITY-GET-LOGININFO-001 | 部分实现 | `backend/src/main/java/com/dcim/platform/module/binterface/service/GetLoginInfoService.java` | 密码脱敏和DeviceList映射需复核 | P1 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 配置 | SET_LOGININFO | SPEC-2016-CMD-SET-LOGININFO-001, SPEC-2016-CMD-SET-LOGININFO-002, SPEC-2016-SECURITY-SET-LOGININFO-001, SPEC-2016-CMD-SET-LOGININFO-003 | 未实现/安全禁用 | `未定位到专用Handler` | 写注册配置高风险，禁止默认实现 | P2 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 配置读取 | GET_FTP | SPEC-2016-CMD-GET-FTP-001, SPEC-2016-CMD-GET-FTP-002, SPEC-2016-SECURITY-GET-FTP-001 | 部分实现 | `backend/src/main/java/com/dcim/platform/module/binterface/service/GetFtpService.java` | 密码脱敏需复核 | P1 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 配置 | SET_FTP | SPEC-2016-CMD-SET-FTP-001, SPEC-2016-CMD-SET-FTP-002, SPEC-2016-CMD-SET-FTP-003, SPEC-2016-SECURITY-SET-FTP-001 | 安全禁用/桩 | `backend/src/main/java/com/dcim/platform/module/binterface/command/SetFtpCommandHandler.java` | 写FTP配置高风险，禁止真实执行 | P1 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 运维 | GET_FSUINFO | SPEC-2016-CMD-GET-FSUINFO-001, SPEC-2016-CMD-GET-FSUINFO-002, SPEC-2016-CMD-GET-FSUINFO-003 | 已实现/部分实现 | `backend/src/main/java/com/dcim/platform/module/binterface/service/BInterface2016GetFsuInfoService.java` | 需作为2016 SC心跳标准路径 | P0 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 控制/运维 | SET_FSUREBOOT | SPEC-2016-CMD-SET-FSUREBOOT-001, SPEC-2016-CMD-SET-FSUREBOOT-002, SPEC-2016-CMD-SET-FSUREBOOT-003, SPEC-2016-SECURITY-SET-FSUREBOOT-001 | 安全禁用 | `未定位到Handler` | 远程重启高风险，禁止真实执行 | P1 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 门限读取 | GET_THRESHOLD | SPEC-2016-CMD-GET-THRESHOLD-001, SPEC-2016-CMD-GET-THRESHOLD-002, SPEC-2016-CMD-GET-THRESHOLD-003 | 部分实现 | `backend/src/main/java/com/dcim/platform/module/binterface/service/GetThresholdService.java` | 需按2016 1901/1902和TThreshold复核 | P1 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |
| 门限配置 | SET_THRESHOLD | SPEC-2016-CMD-SET-THRESHOLD-001, SPEC-2016-CMD-SET-THRESHOLD-002, SPEC-2016-CMD-SET-THRESHOLD-003, SPEC-2016-SECURITY-SET-THRESHOLD-001 | 安全禁用/部分实现 | `backend/src/main/java/com/dcim/platform/module/binterface/service/SetThresholdService.java` | 写门限高风险，默认禁用 | P1 | 当前矩阵仅基于文件检索，后续代码审计需逐项确认 |

## SPEC-EMERSON-UPDATE-001 实现状态补充

| 功能 | SPEC 编号 | 当前状态 | 依据 | 缺口 / 风险 | 备注 |
|---|---|---|---|---|---|
| GET_DATA 请求构造 | SPEC-2016-CMD-GET-DATA-002 | 已修复 | BIF2016-CONNECTION-006/007 | minimal/wildcard 真实链路超时 | request 使用 `<Id>signalId</Id>`，response 保留 `TSemaphore` |
| xmlData escaped string | SPEC-2016-IMPL-SOAP-XMLDATA-ESCAPED-001, SPEC-2016-PROFILE-EMERSON-XMLDATA-001 | 已实现 | BIF2016-RPCXML-001；DATA-MAPPING-008 retry raw | 需持续防回退为 DOM child | Emerson兼容要求，不写成标准强制规则 |
| SOAP 前缀兼容 | SPEC-2016-IMPL-SOAP-PREFIX-EMERSON-001, SPEC-2016-PROFILE-EMERSON-SOAP-PREFIX-001 | 已实现 | BIF2016-RPCXML-001 | 仅适用于Emerson Profile | 使用 `soap:` 前缀，解析响应不得按前缀硬编码 |
| 真实 TSemaphore 返回 | SPEC-2016-PROFILE-EMERSON-007 | 已验证 | BIF2016-CONNECTION-007；DATA-MAPPING-008 retry | 现场网络不稳定会影响复验 | all-devices 8个，single-device 2个，retry 2个 |
| 标准字典匹配 | SPEC-2016-PROFILE-EMERSON-008, SPEC-2016-PROFILE-EMERSON-011 | 8/8 exact；单位已复核 | SPEC-DICT-MAPPING-001；SPEC-UNIT-COMPLETE-001 | DI 0/1编码待复核；0407107001原始字典单位为空，继续待确认 | 0418101001/0418102001已纠偏为环境温湿度；DI单位为空按无单位展示 |
| monitoring_point 导入 | SPEC-2016-DICT-SIGNAL-* | 已完成 8 条 | DATA-MAPPING-007 | 需厂家复核实例绑定 | 保留旧演示点位，ON CONFLICT DO NOTHING |
| realtime_data 入库 | SPEC-2016-DICT-SIGNAL-0407102001, SPEC-2016-DICT-SIGNAL-0407107001 | 已完成单点和后半组电压验证 | DATA-MAPPING-007/008 retry | 正式API全链路受网络稳定性影响 | 0407102001=54.1/54.2，0407107001=0.0 |
| Java HTTP 客户端 | UNKNOWN-2016-OPS-NETWORK-001 | 代码链路可用，受现场网络稳定性影响 | BIF2016-NETWORK-001；DATA-MAPPING-008 | 同一链路仍可能timeout/502 | 不建议以curl替代正式Java链路 |
| 正式 API 全链路 | UNKNOWN-2016-OPS-NETWORK-001 | 待网络稳定后最终验收 | DATA-MAPPING-008 | FSU当时超时；request/message_log链路正常 | 不属于协议规则 |
| 快慢数据通道分类 | SPEC-2016-COMM-005, SPEC-2016-PROFILE-EMERSON-010 | 已完成规范分类 | SPEC-FAST-SLOW-DATA-001；`matrices/fast-slow-data-matrix.md` | 代码枚举/前端显示尚未显式落地 | GET_DATA 属慢数据；SEND_ALARM 属快数据；LOGIN 为注册/会话 |
| 注册 120 秒间隔治理 | SPEC-2016-CMD-LOGIN-003, UNKNOWN-2016-OPS-REGISTER-INTERVAL-001 | 发现风险，未完成治理 | SPEC-FAST-SLOW-DATA-001；message_log 只读查询 | 真实 FSU 存在 32/33 秒重复 LOGIN；需实现幂等/节流记录和 registerIntervalSeconds 展示 | 不作为 GET_DATA 频率限制，不作为 timeout/502 单一根因 |
