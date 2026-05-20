# FSU 被动上报接收联调方案

> LANDING-008 | 2026-05-20

## 1. 为什么需要准备被动上报链路

LANDING-006 确认真实 FSU 使用 **B接口2016 码表**。LANDING-007 使用正确协议格式和真实 DeviceID 执行 GET_DATA (Code=401) 查询，FSU 返回 GET_DATA_ACK (Code=402) + Result=1 但 DeviceList 始终为空 — FSU 当前无主动查询的测量数据可返回。

因此，获取真实监控数据的可行路径是将平台配置为 **FSU 数据上报的接收端 (SC)**，由 FSU 通过 **SEND_DATA / SEND_ALARM** 主动推送上行数据。

## 2. LANDING-006/007 已确认的 2016 协议事实

| 项目 | 确认值 |
|------|--------|
| FSU 协议版本 | B接口2016 |
| FSUID / FsuCode | `51051243812345` |
| StationName | `1` |
| SCIP (FSU 侧配置) | `192.168.100.123` |
| PK_Type 格式 | Name+Code (结构化) |
| GET_DATA 码 | 401 → ACK 402 |
| GET_LOGININFO 码 | 1501 → ACK 1502 |
| GET_FSUINFO 码 | 1701 → ACK 1702 |
| GET_FTP 码 | 1601 → ACK 1602 |
| SEND_ALARM 码 | 501 (2016) = 601 (2024) |
| 已获取 DeviceID | 4 个 (见 LANDING-006) |
| GET_DATA 数据 | 当前无测量数据 |

## 3. 主动 GET_DATA 当前状态

- 协议格式已核对正确 (DeviceList in Info, Code=401)
- 请求路由正确 (GET_DATA_ACK Code=402, Result=1)
- DeviceID 有效 (FsuId/FsuCode 回显正确)
- **阻塞**: FSU 当前无测量点位数据，无法通过 GET_DATA 获取 SPID/SignalID/Value

## 4. FSU → SCService 被动上报链路图

```
FSU (192.168.100.100)                   SC 平台 (本系统)
     │                                        │
     ├─ LOGIN ──────────────────────────────→│ POST /api/b-interface/sc-service
     │  PK_Type: LOGIN (Code=101)            │ → SoapMessageHandler.parse()
     │  Info: FsuCode + ...                  │ → CommandDispatcher.dispatch(LOGIN)
     │                                        │ → LoginCommandHandler
     │                                        │ → LoginService.login()
     │←───────────── LOGIN ACK ──────────────┤ SessionID, ResultCode=0
     │                                        │
     ├─ HEARTBEAT ──────────────────────────→│ → HeartbeatCommandHandler
     │  PK_Type: HEARTBEAT                   │ → LoginService.isLoggedIn()
     │  Info: FsuCode                        │ → LoginService.updateLastSeen()
     │←─────────── HEARTBEAT ACK ────────────┤ ResultCode=0
     │                                        │
     ├─ SEND_DATA ──────────────────────────→│ → SendDataCommandHandler
     │  PK_Type: SEND_DATA (Code=501?)       │ → SendDataService.processData()
     │  xmlData: Signal items                │ → RealtimeDataEntity upsert
     │←─────────── SEND_DATA ACK ────────────┤ ResultCode=0, Count=N
     │                                        │
     ├─ SEND_ALARM ─────────────────────────→│ → SendAlarmCommandHandler
     │  PK_Type: SEND_ALARM (Code=501)       │ → SendAlarmService.processAlarms()
     │  xmlData: Alarm items                 │ → AlarmRecordEntity create/update
     │←────────── SEND_ALARM ACK ────────────┤ ResultCode=0, AlarmID
```

## 5. LOGIN 处理链路

- **入口**: `ScServiceController.handleScService()` (`POST /api/b-interface/sc-service`)
- **解析**: `SoapMessageHandler.parse()` → `BInterfaceMessage` (PK_Type=LOGIN)
- **分发**: `CommandDispatcher.dispatch()` → `LoginCommandHandler`
- **处理**: `LoginService.login(fsuCode, remoteAddr)`
  - 校验 FSUCode 是否存在 (fsu_device 表)
  - 生成 SessionID (格式: `SESSION-{fsuCode}-{uuid}`)
  - 创建/更新 `b_interface_session` (status=ACTIVE)
  - 创建/更新 `b_interface_fsu_status` (loginStatus=LOGIN, onlineStatus=ONLINE)
  - 更新 `fsu_device` 注册/在线时间
- **响应**: `<PK_Type>LOGIN</PK_Type><Info><ResultCode>0</ResultCode><SessionID>...</SessionID><ExpireSeconds>3600</ExpireSeconds><ServerTime>...</ServerTime></Info>`
- **ACK 格式**: 使用与请求相同的 PK_Type (LOGIN)，非 LOGIN_ACK

## 6. HEARTBEAT 处理链路

- **入口**: 同上 Controller
- **分发**: `HeartbeatCommandHandler` (PK_Type=HEARTBEAT)
- **处理**: 复用 `LoginService.isLoggedIn()` + `LoginService.updateLastSeen()`
- **响应**: `<PK_Type>HEARTBEAT</PK_Type><Info><ResultCode>0</ResultCode><ServerTime>...</ServerTime></Info>`

## 7. SEND_DATA 处理链路

- **入口**: 同上 Controller
- **分发**: `SendDataCommandHandler` (PK_Type=SEND_DATA)
- **处理**: `SendDataService.processData(fsuCode, collectTime, xmlData)`
  - 遍历 xmlData 中的 Signal items
  - 提取 SignalID, Value, Quality, Status
  - 查找匹配的 `monitoring_point` (按 fsuId + signalId)
  - Upsert `realtime_data` 表
- **DeviceID/SPID 处理**: 当前 `SendDataService` **不解析 DeviceID 或 SPID**，仅处理 SignalID
- **响应**: `<PK_Type>SEND_DATA</PK_Type><Info><ResultCode>0</ResultCode><Count>N</Count></Info>`

## 8. SEND_ALARM 处理链路

- **入口**: 同上 Controller
- **分发**: `SendAlarmCommandHandler` (PK_Type=SEND_ALARM)
- **处理**: `SendAlarmService.processAlarms(fsuCode, alarmTime, xmlData)`
  - 遍历 xmlData 中的 Alarm items
  - 提取: SignalID, AlarmCode, AlarmLevel, AlarmName, AlarmValue, AlarmDesc, AlarmType
  - **2016 专用字段**: SerialNo, DeviceID (已提取并写入), SPID (已提取但**未写入** alarm_record)
  - alarmType=0 (生成): 创建 alarm_record (ACTIVE)
  - alarmType=1 (恢复): 查找并更新已有告警为 RECOVERED
- **响应**: `<PK_Type>SEND_ALARM</PK_Type><Info><ResultCode>0</ResultCode><AlarmID>lastId</AlarmID></Info>`

## 9. 未映射 DeviceID/SPID 处理策略

**SEND_DATA**:
- 未匹配 monitoring_point 的 SignalID: **静默丢弃** (item rejected, 记录在 rejectedCount 中)
- 建议: 日志输出 rejected 的 SignalID 列表供排查

**SEND_ALARM**:
- SerialNo, DeviceID: 已写入 alarm_record (serialNo, deviceId 列)
- **SPID**: 已从 xmlData 提取但 **未写入 alarm_record** (alarm_record 表无 spid 列)
- 建议: LANDING-001 已补强 serialNo/deviceId 列，SPID 列为后续待补

**临时策略**: 未映射的 DeviceID/SPID 记录在 `BInterfaceMessageLog` 原始报文中供事后解析 (待 BInterfaceMessageLogService 实现后)。

## 10. 原始报文保存策略

**当前状态**: `BInterfaceMessageLogService` 为 **占位 TODO**，原始 SOAP 报文**未被保存**。

**建议策略** (下一阶段实现):
1. 在 `ScServiceController` 入口处记录 FSU→SC 方向原始报文
2. 存储到 `b_interface_message_log` 表
3. 字段: direction="FSU→SC", command, fsuCode, messageType="Request", rawMessage
4. 保留原始 SOAP 不修改 (用于协议分析和审计)
5. 异步记录，不阻塞主处理链路

## 11. 与真实点位表 / seed SQL 的关系

- 当前 `monitoring_point` 表为空 (无真实点位)
- SEND_DATA 无法匹配 SignalID → 所有数据被 rejected
- **必须先由管理方提供 DeviceID/SPID 映射表** → 导入 monitoring_point → SEND_DATA 才能正确入库
- SEND_ALARM 不依赖 monitoring_point 匹配 (直接写入 alarm_record)
- **候选点位表** (LANDING-003/004/005/007 均未生成) 仍需管理方提供

## 12. 仍需管理方确认的信息

1. FSU 是否已配置上报目标地址 (SC 平台 IP:Port)?
2. FSU 上报周期 (HEARTBEAT/SEND_DATA/SEND_ALARM 频率)?
3. FSU 上报命令类型 (是否包含 LOGIN/HEARTBEAT/SEND_DATA/SEND_ALARM)?
4. 完整 DeviceID → SPID/SignalID 映射表
5. 告警码表 (AlarmCode → 告警名称/等级/描述)
6. 点位属性 (信号名称/单位/量程/阈值)
