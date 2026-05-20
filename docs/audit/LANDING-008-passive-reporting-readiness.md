# LANDING-008: B接口2016 被动上报接收联调准备与平台侧入口核对

## 审计日期
2026-05-20

## 审计类型
代码链路核对 + 文档编写 (无代码修改)

## 1. 任务目标
核对 SCService 对外接收入口、LOGIN/HEARTBEAT/SEND_DATA/SEND_ALARM Controller-Dispatcher-Handler-Service 链路、B接口2016 被动上报码表兼容性，输出集成方案、配置指南和检查清单。

## 2. SCService 接收链路核对结果

### 入口

| 项目 | 值 |
|------|-----|
| Controller | `ScServiceController` (`POST /api/b-interface/sc-service`) |
| WSDL 声明路径 | `/services/SCService` (与实际 Controller 路径不一致) |
| WSDL 文件 | `src/main/resources/wsdl/sc-service.wsdl` |
| WSDL 端点 | `GET /api/b-interface/wsdl/sc-service` |
| SOAP 解析 | `SoapMessageHandler.parse()` — 双向兼容 RPC invoke 和 document-style |
| SOAP 响应构建 | `SoapMessageHandler.buildResponse()` — document-style 格式 |

### 分发

| 项目 | 值 |
|------|-----|
| 分发器 | `CommandDispatcher` (自动发现所有 `CommandHandler` Bean) |
| 分发逻辑 | `handlerMap.get(pkType)` — 基于 BInterfacePkType 枚举 |
| 未匹配回退 | `UnknownCommandHandler` |
| 注册处理器数 | 18 个 |

### LOGIN 链路

- `LoginCommandHandler` → `LoginService.login(fsuCode, remoteAddr)`
- 生成 SessionID, 创建 session/status 记录, 更新 fsu_device
- ACK: `<PK_Type>LOGIN</PK_Type><Info><ResultCode>0</ResultCode><SessionID>...</SessionID>...`
- **注意**: ACK 使用与请求相同的 PK_Type (LOGIN), 非 LOGIN_ACK

### HEARTBEAT 链路

- `HeartbeatCommandHandler` → 复用 `LoginService.isLoggedIn()` + `updateLastSeen()`
- 无独立 HeartbeatService
- ACK: `<PK_Type>HEARTBEAT</PK_Type><Info><ResultCode>0</ResultCode><ServerTime>...</ServerTime>`

### SEND_DATA 链路

- `SendDataCommandHandler` → `SendDataService.processData(fsuCode, collectTime, xmlData)`
- 解析 SignalID/Value/Quality/Status → 匹配 monitoring_point → upsert realtime_data
- **不处理 DeviceID/SPID**: 仅按 SignalID 匹配
- ACK: `<PK_Type>SEND_DATA</PK_Type><Info><ResultCode>0</ResultCode><Count>N</Count>`

### SEND_ALARM 链路

- `SendAlarmCommandHandler` → `SendAlarmService.processAlarms(fsuCode, alarmTime, xmlData)`
- 解析 SignalID/AlarmCode/AlarmLevel/SerialNo/DeviceID/SPID
- alarmType=0 → 创建 ACTIVE alarm_record
- alarmType=1 → 查找并更新为 RECOVERED
- **SerialNo/DeviceID 已写入**, **SPID 已提取但未写入** (alarm_record 无 spid 列)
- ACK: `<PK_Type>SEND_ALARM</PK_Type><Info><ResultCode>0</ResultCode><AlarmID>...</AlarmID>`

## 3. B接口2016 被动上报码表核对

| 命令 | 2016 Code | SC 处理状态 | ACK |
|------|-----------|------------|-----|
| LOGIN | 101 | ✅ LoginCommandHandler | LOGIN (非 LOGIN_ACK) |
| HEARTBEAT | — | ✅ HeartbeatCommandHandler | HEARTBEAT |
| SEND_DATA | — | ✅ SendDataCommandHandler | SEND_DATA |
| SEND_ALARM | 501 | ✅ SendAlarmCommandHandler | SEND_ALARM |

**注意**: SC 入站 ACK 使用请求 PK_Type (非 `_ACK` 后缀), 且使用纯文本 PK_Type 格式 (非 Name+Code)。这与 FSU 出站 (SC→FSU) 的 Name+Code 格式不同。

## 4. 发现的问题

| # | 问题 | 严重程度 | 建议 |
|---|------|----------|------|
| 1 | **WSDL 路径不一致**: WSDL `/services/SCService`, Controller `/api/b-interface/sc-service` | 中 | FSU 配置时需使用实际路径; 考虑在平台侧添加路径映射 |
| 2 | **报文日志未实现**: `BInterfaceMessageLogService` 为 TODO 占位 | 高 | 下一阶段实现, 否则无法保存原始 SOAP 报文 |
| 3 | **SPID 未入库**: `SendAlarmService` 提取 SPID 但不写入 alarm_record | 中 | alarm_record 表缺少 spid 列, LANDING-001 已补强 serialNo/deviceId, spid 为下一项 |
| 4 | **SEND_DATA 无 DeviceID**: 仅按 SignalID 匹配, 无法区分同 SignalID 不同 DeviceID | 中 | 需确认真实 FSU 的 SEND_DATA 格式是否包含 DeviceID |
| 5 | **ACK 格式无 Name+Code**: 响应始终使用纯文本 PK_Type | 低 | FSU 可能期望 Name+Code 格式 ACK, 待验证 |

## 5. 新增文件

| 文件 | 说明 |
|------|------|
| `docs/landing/PASSIVE-REPORTING-INTEGRATION-PLAN.md` | 被动上报集成方案 |
| `docs/landing/FSU-SC-CONFIGURATION-GUIDE.md` | FSU 侧配置指南 |
| `docs/landing/PASSIVE-REPORTING-CHECKLIST.md` | 联调检查清单 |
| `docs/audit/LANDING-008-passive-reporting-readiness.md` | 本审计报告 |

## 6. 未修改文件

业务代码 0 修改 — 仅文档编写。

## 7. 安全边界

- [x] 未执行 SET
- [x] 未启用 Scheduler
- [x] 未主动访问真实 FSU
- [x] 未修改 alarm_record 状态机
- [x] 未生成正式 seed SQL
- [x] 全量测试无回归 (1164 tests, 0 failures)
