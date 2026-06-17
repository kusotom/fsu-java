# CODEX-AUDIT-2016-FULL-001 审计报告

## 1. 本次目标

按 `openspec/protocols/binterface-2016/` 全部规范文件，对当前 FSU-JAVA 项目做 B接口2016 全版本只读审计，覆盖协议架构、SOAP/XML、SCService 入站、FSUService 出站、FTP、初始化、自动升级、SC 心跳、门限、15 个命令、数据结构、枚举、Entity/Repository/Service/Controller/Test、2016/Emerson/2024 差异、SET 安全边界、Scheduler、原始报文留痕、测试覆盖和真实 FSU 实测差异。

本次不修改 Java、不修改 SQL、不执行真实 FSU 调用、不执行 SET、不启用 Scheduler。

## 2. 读取的规范文件清单

已执行并读取 `find openspec/protocols/binterface-2016 -type f | sort` 输出的全部 39 个文件：

- `openspec/protocols/binterface-2016/00-overview.md`
- `openspec/protocols/binterface-2016/01-interface-architecture.md`
- `openspec/protocols/binterface-2016/02-message-envelope.md`
- `openspec/protocols/binterface-2016/03-constants.md`
- `openspec/protocols/binterface-2016/04-enums.md`
- `openspec/protocols/binterface-2016/05-device-code-table.md`
- `openspec/protocols/binterface-2016/06-data-structures.md`
- `openspec/protocols/binterface-2016/07-command-catalog.md`
- `openspec/protocols/binterface-2016/08-xml-style-and-compatibility.md`
- `openspec/protocols/binterface-2016/09-ftp-capability.md`
- `openspec/protocols/binterface-2016/10-fsu-initialization.md`
- `openspec/protocols/binterface-2016/11-fsu-auto-upgrade.md`
- `openspec/protocols/binterface-2016/12-sc-heartbeat.md`
- `openspec/protocols/binterface-2016/13-threshold-configuration.md`
- `openspec/protocols/binterface-2016/README.md`
- `openspec/protocols/binterface-2016/audit-template.md`
- `openspec/protocols/binterface-2016/commands/010-login.md`
- `openspec/protocols/binterface-2016/commands/020-logout.md`
- `openspec/protocols/binterface-2016/commands/030-send-alarm.md`
- `openspec/protocols/binterface-2016/commands/040-get-data.md`
- `openspec/protocols/binterface-2016/commands/050-get-hisdata.md`
- `openspec/protocols/binterface-2016/commands/060-set-point.md`
- `openspec/protocols/binterface-2016/commands/070-get-threshold.md`
- `openspec/protocols/binterface-2016/commands/080-set-threshold.md`
- `openspec/protocols/binterface-2016/commands/090-get-logininfo.md`
- `openspec/protocols/binterface-2016/commands/100-set-logininfo.md`
- `openspec/protocols/binterface-2016/commands/110-get-ftp.md`
- `openspec/protocols/binterface-2016/commands/120-set-ftp.md`
- `openspec/protocols/binterface-2016/commands/130-time-check.md`
- `openspec/protocols/binterface-2016/commands/140-get-fsuinfo.md`
- `openspec/protocols/binterface-2016/commands/150-set-fsureboot.md`
- `openspec/protocols/binterface-2016/matrices/command-code-matrix.md`
- `openspec/protocols/binterface-2016/matrices/field-mapping-matrix.md`
- `openspec/protocols/binterface-2016/matrices/implementation-status-matrix.md`
- `openspec/protocols/binterface-2016/matrices/test-coverage-matrix.md`
- `openspec/protocols/binterface-2016/matrices/xml-sample-index.md`
- `openspec/protocols/binterface-2016/profiles/emerson-2016.md`
- `openspec/protocols/binterface-2016/profiles/future-2024-compatibility.md`
- `openspec/protocols/binterface-2016/profiles/standard-2016.md`

同时读取/尝试读取：

- `../docs/PROJECT_ENGINEERING_RULES.md`：项目内 `docs/PROJECT_ENGINEERING_RULES.md` 不存在，父目录存在并已读取。
- `docs/rules/CLAUDE_PROJECT_RULES.md`
- `docs/memory/README.md`
- `docs/memory/WORKING-MEMORY.md`
- `openspec/project.md`
- `docs/audit/PROTOCOL-BASELINE-001-binterface-2016-primary.md`
- `docs/audit/PROTOCOL-AUDIT-2016-001-binterface-code-format-audit.md`
- `docs/audit/SPEC-2016-SPLIT-001-binterface-2016-full-spec-split.md`
- `docs/memory/2026-05-21-SPEC-2016-SPLIT-001-binterface-2016-full-spec-split.md`：不存在，`docs/memory/README.md` 中索引项也指向了不存在的文件名。

## 3. 审计方法

1. 读取 B接口2016 全量规范、profile、矩阵和既有基线审计。
2. 执行用户指定的 4 组 `grep -R` 搜索，覆盖命令、分发器、SOAP/FSU 客户端、数据结构、SET/Scheduler/real-call 安全词。
3. 读取关键 Java 文件：`BInterfacePkType`、`BInterfaceCommand2016`、`CommandDispatcher`、SCService 入站、FSUService 出站、GET/SEND/SET 相关 Service、Scheduler、安全门禁、核心 Entity。
4. 对照 15 个 2016 命令逐项判断协议要求、当前实现、测试覆盖、真实 FSU 实测、缺口和风险。
5. 仅生成审计文档、TODO 和 memory，不运行真实 FSU 测试。

关键搜索结果：

- 命令搜索：`LOGIN/SEND_DATA/SEND_ALARM/GET_DATA/GET_THRESHOLD/SET_THRESHOLD/TIME_CHECK/GET_LOGININFO/GET_FTP/GET_FSUINFO` 有代码或测试；`LOGOUT/GET_HISDATA/SET_LOGININFO` 无 Controller/Handler/Service/Test；`SET_POINT/SET_FTP` 只有 `notImplemented` 桩；`SET_FSUREBOOT` 只有枚举和安全测试，无 Handler。
- 架构搜索：`CommandDispatcher` 自动注册 `CommandHandler`；`SoapMessageHandler` 同时支持纯文本 PK_Type 和 Name+Code；`RealHttpFsuServiceClient` 默认受 `real-call-enabled=false` 保护。
- 数据结构搜索：`TAlarm/SerialNo/DeviceID/SPID` 已映射到 `alarm_record`；`TSemaphore/TThreshold` 主要用 `SignalID` 简化结构；`TFSUStatus/CPUUsage/MEMUsage` 在 GET_FSUINFO 专用服务中解析。
- 安全搜索：`SET_POINT/SET_FTP` 桩返回 `notImplemented`；`SET_THRESHOLD` 有安全闭环；`set-command-safety.enabled=false`、`scheduler-forbidden=true`、`real-call-enabled=false`、`slow-polling.scheduler-enabled=false` 均存在。

## 4. 全命令实现矩阵

| 命令 | 2016 Code | 方向 | 当前实现状态 | 测试状态 | 实测状态 | 风险 | 优先级 |
|---|---:|---|---|---|---|---|---|
| LOGIN / LOGIN_ACK | 101/102 | FSU→SC | 已实现，`/services/SCService` 可接收并入库日志 | 有 `Login*`、SCService、日志测试 | 真实 FSU 已成功 LOGIN 并入库 | 中：Password 未校验 | P0 |
| LOGOUT / LOGOUT_ACK | 103/104 | FSU→SC | 未实现：未找到 Controller/Handler/Service/Test | 无 | 未实测 | 中：无法显式下线 | P2 |
| GET_DATA / GET_DATA_ACK | 401/402 | SC→FSU | 部分实现，简单 `SignalID` 请求 | 有 Service/Handler/慢轮询测试 | 2016 Code 可路由，但真实 FSU 返回空 DeviceList | 高：不支持 Emerson DeviceList/TSemaphore 请求 | P0 |
| GET_HISDATA / GET_HISDATA_ACK | 403/404 | SC→FSU | 未实现：未找到 Handler/Service/Test | 无 | 未实测 | 中：历史数据缺失 | P2 |
| SEND_ALARM / SEND_ALARM_ACK | 501/502 | FSU→SC | 已实现，支持 SerialNo/DeviceID/SPID 入库 | 有 Handler/Service 测试 | 只确认 501 属于 SEND_ALARM；未收到真实上报告警 | 高：真实报文未验证 | P0 |
| SET_POINT / SET_POINT_ACK | 1001/1002 | SC→FSU | 仅 stub：`SetPointCommandHandler.notImplemented` | 只有安全/桩测试 | 禁止实测 | 中：协议不完整但安全 | P1 |
| TIME_CHECK / TIME_CHECK_ACK | 1301/1302 | SC→FSU | 已实现，但依赖默认 2024/legacy 路径，2016 表缺码 | 有 Service/Handler 测试 | 未实测 | 中：2016 Code 表缺失 | P2 |
| GET_LOGININFO / GET_LOGININFO_ACK | 1501/1502 | SC→FSU | 已实现 | 有 Service/Handler/集成测试 | LANDING-006 成功，返回 DeviceList | 低：Emerson 返回结构非标准 | P1 |
| SET_LOGININFO / SET_LOGININFO_ACK | 1503/1504 | SC→FSU | 未实现：BInterfacePkType 也缺 SET_LOGININFO | 无 | 禁止实测 | 中：配置能力缺失 | P2 |
| GET_FTP / GET_FTP_ACK | 1601/1602 | SC→FSU | 已实现 | 有 Service/Handler/集成测试 | LANDING-006 成功 | 中：明文密码脱敏需加强 | P1 |
| SET_FTP / SET_FTP_ACK | 1603/1604 | SC→FSU | 仅 stub：`SetFtpCommandHandler.notImplemented` | 只有安全/桩测试 | 禁止实测 | 中：FTP 配置能力缺失 | P2 |
| GET_FSUINFO / GET_FSUINFO_ACK | 1701/1702 | SC→FSU | 已实现专用 `BInterface2016GetFsuInfoService` | 有专用单测和 run-once 测试 | LANDING-006 成功，CPU/MEM 可解析 | 低：未统一进 Dispatcher | P1 |
| SET_FSUREBOOT / SET_FSUREBOOT_ACK | 1801/1802 | SC→FSU | 安全禁用：枚举存在，无 Handler/Service | 有“无 Handler”安全测试 | 禁止实测 | 低：保持禁用合理 | P2 |
| GET_THRESHOLD / GET_THRESHOLD_ACK | 1901/1902 | SC→FSU | 已实现但 2016 Code 表缺 1901/1902，简单 SignalID 请求 | 有 Service/Handler 测试 | 真实 FSU曾返回空 | 高：2016 Code 和 DeviceList 未对齐 | P1 |
| SET_THRESHOLD / SET_THRESHOLD_ACK | 2001/2002 | SC→FSU | 已实现+安全门禁，但 2016 Code 表缺 2001/2002 | 有安全/Service/Handler 测试 | 禁止实测 | 中：2016 Code 表缺失 | P1 |

### 4.1 逐命令审计

#### LOGIN / LOGIN_ACK

- 协议要求：`commands/010-login.md`，FSU→SC，101/102，Info 至少包含 FSUCode、Password、Timestamp，响应返回 ResultCode、SessionID、ExpireSeconds。
- 当前代码实现：`StandardScServiceController`/`ScServiceController` → `ScServiceProcessor` → `CommandDispatcher` → `LoginCommandHandler` → `LoginService`；原始报文由 `BInterfaceMessageLogService.saveInbound()` best-effort 入库。
- 当前测试覆盖：`LoginCommandHandlerTest`、`LoginServiceTest`、`StandardScServiceControllerTest`、`BInterfaceMessageLog*Test`。
- 真实 FSU 实测状态：已验证真实 FSU `POST /services/SCService`，`LOGIN` 成功，`b_interface_message_log` 已入库。
- 缺口：Password/Timestamp/FSUID/DeviceInfo 未校验或未持久化；暴力登录限制无实现。
- 风险等级：P0 中风险。
- 修复建议：增加 Password 策略开关、登录频率限制、DeviceInfo 可选落库，不改变当前真实接入默认行为。
- 优先级：P0-后续增强。

#### LOGOUT / LOGOUT_ACK

- 协议要求：`commands/020-logout.md`，FSU→SC，103/104，携带 FSUCode+SessionID，清理会话和在线状态。
- 当前代码实现：未实现。证据为未找到 `LogoutCommandHandler` / `LogoutService` / `Logout*Test`，`BInterfacePkType` 也无 `LOGOUT`。
- 当前测试覆盖：无。
- 真实 FSU 实测状态：未观察到真实 LOGOUT。
- 缺口：命令枚举、Handler、Service、Dispatcher 路由、测试全部缺失。
- 风险等级：P2。
- 修复建议：补 `BInterfacePkType.LOGOUT`，新增 `LogoutCommandHandler` 复用 `LoginService.clearSession()`，补 SOAP fixture 和 Handler/Service 测试。
- 优先级：P2。

#### GET_DATA / GET_DATA_ACK

- 协议要求：`commands/040-get-data.md`，SC→FSU，401/402，按 2016 查询实时数据，Emerson 需要关注 `DeviceList/TSemaphore` 结构。
- 当前代码实现：`GetDataCommandHandler`、`GetDataService`、`SlowDataPollingService` 已有；`GetDataService` 只构造重复 `<SignalID>`，未构造 Emerson `DeviceList><Device><TSemaphore>`。
- 当前测试覆盖：`GetDataServiceTest`、`GetDataCommandHandlerTest`、`GetDataSlowChannelIntegrationTest`、`SlowDataPolling*Test`。
- 真实 FSU 实测状态：2016 Code=401/402 可路由，但返回空 DeviceList；LANDING-007 认为需要 DeviceList/TSemaphore 格式和真实点位映射。
- 缺口：请求 XML 未对齐 Emerson DeviceList；`BInterfaceCommand2016` 有 GET_DATA=401 但无 GET_DATA_ACK 明确完整矩阵外扩；入库依赖点位映射。
- 风险等级：P0。
- 修复建议：新增 2016 GET_DATA DeviceList/TSemaphore 请求构造与响应解析，结合 DeviceID/SPID/monitoring_point 映射。
- 优先级：P0。

#### GET_HISDATA / GET_HISDATA_ACK

- 协议要求：`commands/050-get-hisdata.md`，SC→FSU，403/404，历史数据查询；协议原文不明确：位置为 `openspec/protocols/binterface-2016/commands/050-get-hisdata.md`。
- 当前代码实现：未实现。证据为未找到 `GetHisdataCommandHandler` / `GetHisdataService` / `GetHisdata*Test`；`BInterfacePkType` 只有 `GET_HISTORY_DATA`，与 2016 `GET_HISDATA` 命名不一致。
- 当前测试覆盖：无。
- 真实 FSU 实测状态：未实测。
- 缺口：枚举别名、2016 Code、Service、响应解析、限流策略均缺失。
- 风险等级：P2。
- 修复建议：先确认协议原文/真实 FSU 支持，再补只读查询实现和时间窗口限制。
- 优先级：P2。

#### SEND_ALARM / SEND_ALARM_ACK

- 协议要求：`commands/030-send-alarm.md`，FSU→SC，501/502，解析 TAlarm，产生/恢复告警入库。
- 当前代码实现：`SendAlarmCommandHandler`、`SendAlarmService` 已实现，`AlarmRecordEntity` 含 `serialNo/deviceId/spid`。
- 当前测试覆盖：`SendAlarmCommandHandlerTest`、`SendAlarmServiceTest`。
- 真实 FSU 实测状态：确认 Emerson 2016 `SEND_ALARM=501`，但 LANDING-014 未观察到真实 `SEND_ALARM` 上报，`alarm_record` 仍无真实记录。
- 缺口：缺真实 SEND_ALARM 原始样本；TAlarm 包裹/字段变体仍需用真实报文验证；重复告警/告警风暴策略不足。
- 风险等级：P0。
- 修复建议：继续被动观察或现场触发测试告警；保存 raw-samples；按真实 TAlarm 调整解析。
- 优先级：P0。

#### SET_POINT / SET_POINT_ACK

- 协议要求：`commands/060-set-point.md`，SC→FSU，1001/1002，高危遥控遥调，必须门禁/确认/审计。
- 当前代码实现：仅 stub。证据为 `SetPointCommandHandler.handle()` 返回 `CommandResult.notImplemented(BInterfacePkType.SET_POINT)`；无 `SetPointService`。
- 当前测试覆盖：`SlowChannelReadinessAuditTest`、`ReadOnlyIntegrationSafetyTest` 仅验证桩和安全边界。
- 真实 FSU 实测状态：禁止实测。
- 缺口：Service、请求 XML、二次确认集成、审计落库、点位白名单缺失。
- 风险等级：P1。
- 修复建议：保持默认 forbidden；若实现，先建立 settable 点位白名单和 dry-run 审计，再允许显式授权。
- 优先级：P1。

#### TIME_CHECK / TIME_CHECK_ACK

- 协议要求：`commands/130-time-check.md`，SC→FSU，1301/1302，仅时间比对，不设置时间。
- 当前代码实现：`TimeCheckCommandHandler`、`TimeCheckService` 已实现。
- 当前测试覆盖：`TimeCheckCommandHandlerTest`、`TimeCheckServiceTest`。
- 真实 FSU 实测状态：未实测。
- 缺口：`BInterfaceCommand2016` 缺 TIME_CHECK=1301/1302；真实 Emerson 是否接受当前请求未验证。
- 风险等级：P2。
- 修复建议：补 2016 Code 表；保持只读语义；后续 run-once 时保存 raw XML。
- 优先级：P2。

#### GET_LOGININFO / GET_LOGININFO_ACK

- 协议要求：`commands/090-get-logininfo.md`，SC→FSU，1501/1502，查询登录信息。
- 当前代码实现：`GetLoginInfoCommandHandler`、`GetLoginInfoService` 已实现，`BInterfaceCommand2016` 有 1501 但缺 ACK 1502。
- 当前测试覆盖：`GetLoginInfoServiceTest`、`GetLoginInfoCommandHandlerTest`、`GetLoginInfoFtpSlowChannelIntegrationTest`。
- 真实 FSU 实测状态：LANDING-006 成功，返回 Emerson 风格 Info/DeviceList。
- 缺口：标准 LoginInfo xmlData 与 Emerson Info 内结构差异未形成 profile 化实现；真实 raw XML 未保存为文件。
- 风险等级：P1 低。
- 修复建议：保存真实样本，明确 emerson-2016 parser，补 ACK Code 校验。
- 优先级：P1。

#### SET_LOGININFO / SET_LOGININFO_ACK

- 协议要求：`commands/100-set-logininfo.md`，SC→FSU，1503/1504；协议原文不明确：位置为 `openspec/protocols/binterface-2016/commands/100-set-logininfo.md`。
- 当前代码实现：未实现。证据为未找到 Handler/Service/Test，且 `BInterfacePkType` 缺 `SET_LOGININFO`。
- 当前测试覆盖：无。
- 真实 FSU 实测状态：禁止实测。
- 缺口：枚举、2016 Code、SET 安全接入、字段结构均缺失。
- 风险等级：P2。
- 修复建议：先完成协议字段确认和安全评审；默认 forbidden。
- 优先级：P2。

#### GET_FTP / GET_FTP_ACK

- 协议要求：`commands/110-get-ftp.md`，SC→FSU，1601/1602，查询 FTP 配置。
- 当前代码实现：`GetFtpCommandHandler`、`GetFtpService` 已实现，`BInterfaceCommand2016` 有 1601 但缺 ACK 1602。
- 当前测试覆盖：`GetFtpServiceTest`、`GetFtpCommandHandlerTest`、`GetLoginInfoFtpSlowChannelIntegrationTest`。
- 真实 FSU 实测状态：LANDING-006 成功，返回 FTP 配置，包含明文用户名/密码风险。
- 缺口：Password 未完整脱敏/持久化策略未定义；真实 raw XML 未保存文件。
- 风险等级：P1。
- 修复建议：统一日志脱敏 Password/Username，保存样本但脱敏。
- 优先级：P1。

#### SET_FTP / SET_FTP_ACK

- 协议要求：`commands/120-set-ftp.md`，SC→FSU，1603/1604，高危配置下发，含明文 Password。
- 当前代码实现：仅 stub。证据为 `SetFtpCommandHandler.handle()` 返回 `CommandResult.notImplemented(BInterfacePkType.SET_FTP)`；无 `SetFtpService`。
- 当前测试覆盖：`SlowChannelReadinessAuditTest`、`ReadOnlyIntegrationSafetyTest` 仅验证桩/安全。
- 真实 FSU 实测状态：禁止实测。
- 缺口：Service、Code、脱敏、确认令牌、审计、真实调用全部缺失。
- 风险等级：P2。
- 修复建议：保持 forbidden；实现前先完成 Password 脱敏与审计设计。
- 优先级：P2。

#### GET_FSUINFO / GET_FSUINFO_ACK

- 协议要求：`commands/140-get-fsuinfo.md`，SC→FSU，1701/1702，查询 CPU/MEM；协议原文不明确：GET_FSUINFO xmlData 结构位置为 `openspec/protocols/binterface-2016/commands/140-get-fsuinfo.md`。
- 当前代码实现：`BInterface2016GetFsuInfoService` 专用实现，`pkTypeFormat=legacy-2016`，校验 ACK Code=1702，更新 `b_interface_fsu_status.last_heartbeat`。
- 当前测试覆盖：`BInterface2016GetFsuInfoServiceTest`、`Landing015RealFsuIntegrationTest`（真实测试默认应隔离）。
- 真实 FSU 实测状态：LANDING-006 已确认 CPU/MEM；LANDING-015 记录显示仍需现场 run-once 闭环。
- 缺口：未纳入通用 `CommandDispatcher`；状态明细 CPU/MEM 仅文本存储；raw-samples 缺失。
- 风险等级：P1。
- 修复建议：保留专用服务；后续补 run-once 样本和结构化 CPU/MEM 存储。
- 优先级：P1。

#### SET_FSUREBOOT / SET_FSUREBOOT_ACK

- 协议要求：`commands/150-set-fsureboot.md`，SC→FSU，1801/1802，高危远程重启，安全禁用。
- 当前代码实现：安全禁用。证据为 `BInterfacePkType.SET_FSUREBOOT` 存在，但未找到 `SetFsuRebootCommandHandler` / Service；安全测试断言无 Handler。
- 当前测试覆盖：`SlowChannelReadinessAuditTest`、`ReadOnlyIntegrationSafetyTest`。
- 真实 FSU 实测状态：禁止实测。
- 缺口：无实现是当前安全策略的一部分；若未来实现需独立评审。
- 风险等级：P2 低。
- 修复建议：保持未实现/forbidden；只补文档和显式拒绝返回即可。
- 优先级：P3 或安全专项。

#### GET_THRESHOLD / GET_THRESHOLD_ACK

- 协议要求：`commands/070-get-threshold.md`，SC→FSU，1901/1902，查询 TThreshold。
- 当前代码实现：`GetThresholdCommandHandler`、`GetThresholdService` 已实现，但只构造 `<SignalID>`，`BInterfaceCommand2016` 缺 1901/1902。
- 当前测试覆盖：`GetThresholdServiceTest`、`GetThresholdCommandHandlerTest`、`GetThresholdSlowChannelIntegrationTest`。
- 真实 FSU 实测状态：曾返回空数据；是否因 Code/Profile/XML 结构不匹配未闭环。
- 缺口：2016 Code 表缺失；DeviceList/TThreshold 结构未对齐 Emerson；真实样本缺失。
- 风险等级：P1。
- 修复建议：补 Code 表并增加 legacy-2016 请求测试；按真实 FSU 确认门限查询 XML。
- 优先级：P1。

#### SET_THRESHOLD / SET_THRESHOLD_ACK

- 协议要求：`commands/080-set-threshold.md`，SC→FSU，2001/2002，高危门限设置。
- 当前代码实现：`SetThresholdCommandHandler`、`SetThresholdService`、`SetThresholdSafetyGate`、`SetThresholdAuditService` 已实现，默认关闭。
- 当前测试覆盖：`SetThresholdServiceTest`、`SetThresholdCommandHandlerTest`、`SetThresholdSafetyGateTest`、`SetThresholdAuditServiceTest`、集成安全测试。
- 真实 FSU 实测状态：禁止实测。
- 缺口：`BInterfaceCommand2016` 缺 2001/2002；真实执行路径必须继续默认禁用。
- 风险等级：P1。
- 修复建议：补 2016 Code 映射和 dry-run XML 测试；禁止真实执行直到现场授权。
- 优先级：P1。

## 5. 协议架构审计

B接口2016 双通道模型在项目中基本落位：

- FSU→SC 入站：`POST /services/SCService` 与兼容 `/api/b-interface/sc-service` 共用 `ScServiceProcessor`。
- SC→FSU 出站：`FsuServiceClient` 抽象、`StubFsuServiceClient` 默认、`RealHttpFsuServiceClient` 在 `real-call-enabled=true` 时启用。

缺口是 2016 全命令模型未完整统一：部分命令是 2024 兼容遗留，部分 2016 专用服务绕过通用 Dispatcher，`BInterfaceCommand2016` 也不完整。

## 6. SOAP/XML 报文层审计

`SoapMessageHandler` 支持：

- 2016 纯文本 PK_Type。
- Name+Code 结构化 PK_Type。
- RPC invoke 与 document-style 解析。
- SCService 入站 SOAP 响应构造。

主要风险：

- `buildResponse()` 入站 ACK 使用请求同名 PK_Type，而非 `_ACK` 后缀；这符合当前文档中 SC 入站 ACK 约定，但需继续用真实 FSU 验证所有命令。
- GET_DATA/GET_THRESHOLD/SEND_DATA 的 XMLData 主要是简单 Signal 结构，未充分覆盖 Emerson DeviceList/TSemaphore/TThreshold 变体。
- raw-samples 目录缺真实 XML 文件，当前大量证据只在 audit 文档代码块中。

## 7. 命令码表审计

`BInterfaceCommand2016` 当前只有：

- GET_DATA=401
- GET_DATA_ACK=402
- SEND_ALARM=501
- SEND_ALARM_ACK=502
- GET_LOGININFO=1501
- GET_FTP=1601
- GET_FSUINFO=1701

缺失：

- LOGIN/LOGIN_ACK=101/102
- LOGOUT/LOGOUT_ACK=103/104
- GET_HISDATA/ACK=403/404
- SET_POINT/ACK=1001/1002
- TIME_CHECK/ACK=1301/1302
- GET_LOGININFO_ACK=1502
- GET_FTP_ACK=1602
- SET_LOGININFO/ACK=1503/1504
- SET_FTP/ACK=1603/1604
- GET_FSUINFO_ACK=1702
- SET_FSUREBOOT/ACK=1801/1802
- GET_THRESHOLD/ACK=1901/1902
- SET_THRESHOLD/ACK=2001/2002

这是 P1 级协议完整性缺口。

## 8. 数据结构审计

- `TAlarm`：`AlarmRecordEntity` 已有 `serialNo/deviceId/spid/alarmCode/alarmLevel/alarmValue/alarmDesc`，基本匹配。
- `TSemaphore`：`SendDataService` 和 `GetDataService` 使用 `SignalID/Value/Quality/Status/CollectTime` 简化结构；未覆盖 Emerson `DeviceList/TSemaphore ID/MeasuredVal`。
- `TThreshold`：`GetThresholdService`/`SetThresholdService` 支持 `SignalID/AlarmUpper/AlarmLower/AlarmUpperUrgent/AlarmLowerUrgent`，但未对齐 2016 Code 表。
- `TFSUStatus`：`BInterface2016GetFsuInfoService` 解析 CPUUsage/MEMUsage，但仅写入 `statusDetail` 文本。
- `FTPConfig`：GET_FTP 内存结果可用，未持久化；密码脱敏需增强。

## 9. 入站 FSU→SC 审计

已实现：LOGIN、HEARTBEAT、SEND_DATA、SEND_ALARM。

2016 15 命令内缺失：LOGOUT。

真实 FSU 差异：

- LANDING-014 观察到真实 FSU 只重复 LOGIN。
- 未观察到 HEARTBEAT/SEND_DATA/SEND_ALARM。
- 因此 SEND_DATA/SEND_ALARM 入库链路仍缺真实样本验收。

## 10. 出站 SC→FSU 审计

已实现或部分实现：

- GET_DATA、GET_THRESHOLD、SET_THRESHOLD、TIME_CHECK、GET_LOGININFO、GET_FTP、GET_FSUINFO。

未实现或仅 stub：

- GET_HISDATA 未实现。
- SET_POINT 仅 stub。
- SET_LOGININFO 未实现且枚举缺失。
- SET_FTP 仅 stub。
- SET_FSUREBOOT 安全禁用无 Handler。

主要问题是出站通用服务默认可能走 2024 alias 或无 2016 Code，只有 GET_FSUINFO 专用服务明确 `legacy-2016`。

## 11. FTP 能力审计

GET_FTP 已实现并通过真实 FSU 验证。SET_FTP 只有 stub，未实现安全门禁集成和脱敏链路。Emerson FSU 实测返回明文 FTP 凭据，报告和日志必须脱敏。

## 12. 初始化能力审计

LOGIN 入站和 Session/状态维护已闭环，GET_LOGININFO 可用于查询 FSU 注册信息。缺口是 LOGIN 不校验 Password，DeviceInfo/FSUID 未持久化，LOGOUT 不存在。

## 13. 自动升级能力审计

协议原文不明确：位置为 `openspec/protocols/binterface-2016/11-fsu-auto-upgrade.md`。当前项目未实现自动升级。由于可能涉及 FTP 和厂商私有命令，不建议近期实现。

## 14. SC 心跳审计

主动 HEARTBEAT 入站 Handler 存在，但 Emerson FSU 实测不主动发 HEARTBEAT。当前策略转向 SC 主动 GET_FSUINFO，`BInterface2016GetFsuInfoService` 已实现 CPU/MEM 查询并更新 `last_heartbeat`。Scheduler 默认关闭。

## 15. 门限配置审计

GET_THRESHOLD/SET_THRESHOLD 业务服务存在。SET_THRESHOLD 有安全门禁，默认不可真实执行。缺口是 `BInterfaceCommand2016` 缺 1901/1902 和 2001/2002，且 GET_THRESHOLD 请求 XML 未验证 Emerson DeviceList/TThreshold 格式。

## 16. SET 类命令安全审计

安全边界总体有效：

- `b-interface.set-command-safety.enabled=false`
- `allow-real-call=false`
- `require-confirmation=true`
- `scheduler-forbidden=true`
- `dry-run-default=true`
- `RealHttpFsuServiceClient` 默认不启用。

风险点：

- `SetCommandSafetyGate.LEGACY_SET_NAMES` 把 `TIME_CHECK` 列为 legacy SET 命令，这与 2016 `TIME_CHECK` 只读时间比对语义不一致，可能造成审计语义混淆。
- SET_POINT/SET_FTP 是 stub，但没有统一的“显式 forbidden ACK”实现。

## 17. 2016 / Emerson / 2024 兼容性审计

- 2016 是当前主协议；Emerson 使用 2016 Code，但接受 Name+Code `legacy-2016` 格式。
- 2024 代码仍存在，主要包括 `BInterfaceCommand2024`、GET_SUINFO、GET_SUFTP、GET_ACTIVEALARM、SUREADY 等兼容能力。
- 发现 2024 误用风险：GET_THRESHOLD、SET_THRESHOLD、TIME_CHECK 等已实现服务未全部明确走 `legacy-2016` Code；`BInterfaceCommandAliasMapper` 仍把 SET_POINT/SET_FTP/SET_FSUREBOOT 映射到 2024 对应命令，需在真实 2016 主线中谨慎隔离。

## 18. Entity / Database 映射审计

- `b_interface_message_log`：Entity 不含历史 `command_code`，LANDING-013-FIX-001 已通过 DROP NOT NULL 解决真实 LOGIN 入库。
- `alarm_record`：Entity 包含 `serial_no/device_id/spid`，与 2016 TAlarm 对齐。
- `realtime_data`：满足 SignalID→pointCode 的简化实时数据入库；DeviceID/SPID 维度未体现在实时数据表。
- `b_interface_fsu_status`：支持 login/online/session/last_heartbeat/statusDetail；CPU/MEM 没有结构化列。
- 多个 Result DTO 只在内存承载协议字段，未持久化 FTPConfig、TThreshold、LoginInfo 全量信息。

## 19. 测试覆盖审计

覆盖较强：

- LOGIN、HEARTBEAT、SEND_DATA、SEND_ALARM、GET_DATA、GET_THRESHOLD、SET_THRESHOLD、TIME_CHECK、GET_LOGININFO、GET_FTP、GET_FSUINFO 均有测试。
- SET/Scheduler/真实调用默认关闭有测试。

缺口：

- LOGOUT、GET_HISDATA、SET_LOGININFO 无测试。
- SET_POINT、SET_FTP 只有桩/安全测试，无业务测试。
- GET_DATA/GET_THRESHOLD 缺 2016 Emerson DeviceList 真实结构测试。
- raw XML fixture 缺口明显。

本次未运行 `mvn test`，遵循用户“不运行真实 FSU 测试”的边界；仅读取现有 surefire 报告和文档基线。

## 20. 原始报文留痕审计

运行时 `BInterfaceMessageLogService` 已能保存真实 LOGIN 原始 SOAP。长期样本管理仍不足：

- `docs/landing/raw-samples/landing006-*` 为空。
- LANDING-015 run-once request/response 未形成稳定 raw-samples 文件。
- 报文日志表解决了运行时审计，仍需要将关键真实样本脱敏后固化到文件，供协议回归测试使用。

## 21. 缺口清单

### P0 = 影响真实接入闭环

- GET_DATA 未支持 Emerson DeviceList/TSemaphore 请求结构，真实 FSU 返回空数据仍未闭环。
- SEND_ALARM 未收到真实上报，告警入库没有真实样本验收。
- 原始真实 SOAP 样本未落盘，影响协议回归和故障复现。

### P1 = 影响核心协议完整性

- `BInterfaceCommand2016` 不完整，15 命令及 ACK Code 未全量定义。
- `BInterfacePkType` 缺 `LOGOUT`、`SET_LOGININFO`，`GET_HISDATA` 使用 `GET_HISTORY_DATA` 命名不一致。
- GET_THRESHOLD/SET_THRESHOLD/TIME_CHECK 未完整声明 2016 Code 路径。
- 2016/2024 alias 层边界不够硬，存在误用 2024 Code 的风险。

### P2 = 影响运维/配置能力

- LOGOUT 未实现。
- GET_HISDATA 未实现且协议原文不明确。
- SET_LOGININFO 未实现。
- SET_FTP 未实现。
- TIME_CHECK 未实测。
- FTP Password 脱敏策略需加强。
- CPU/MEM、FTPConfig、TThreshold 等协议数据未结构化持久化。

### P3 = 文档/前端/优化

- `docs/PROJECT_ENGINEERING_RULES.md` 项目内缺失，实际规则在父目录。
- `docs/memory/README.md` 引用了不存在的 `SPEC-2016-SPLIT` memory 文件。
- `openspec/project.md` 中部分“下一步方向”与 LANDING-015 后状态有时序差异。
- SET_FSUREBOOT 建议保持不实现，但可补显式 forbidden 文档和测试。

## 22. 推荐任务拆分

### BIF2016-P0-001

- 任务名称：GET_DATA 2016 Emerson DeviceList/TSemaphore 请求闭环
- 协议依据：`commands/040-get-data.md`、`profiles/emerson-2016.md`
- 当前缺口：当前只发 `<SignalID>`，真实 FSU 返回空 DeviceList。
- 涉及文件：`GetDataService`、`FsuServiceRequest`、`XmlDataParser`、`GetDataServiceTest`、raw-samples。
- 安全边界：只读，不启 Scheduler，不默认真实调用。
- 测试要求：新增 DeviceList 请求构造、响应解析、Stub 回归；真实 run-once 必须显式开关。
- 验收标准：可生成 401/402 legacy-2016 DeviceList 请求，保存 raw XML，解析非空时写入 telemetry。

### BIF2016-P0-002

- 任务名称：SEND_ALARM 真实样本采集与告警入库验收
- 协议依据：`commands/030-send-alarm.md`
- 当前缺口：无真实 SEND_ALARM 入库样本。
- 涉及文件：`SendAlarmServiceTest`、raw-samples、观察文档。
- 安全边界：被动接收，不主动触发 SET，不修改 alarm 状态机语义。
- 测试要求：用真实脱敏样本补 fixture 测试。
- 验收标准：真实 TAlarm 可入库，serialNo/deviceId/spid 有值。

### BIF2016-P1-001

- 任务名称：补齐 BInterfaceCommand2016 全 15 命令与 ACK 码表
- 协议依据：`04-enums.md`、`matrices/command-code-matrix.md`
- 当前缺口：2016 Code 表只有 7 条。
- 涉及文件：`BInterfaceCommand2016.java`、`BInterfaceCommand2016Test`。
- 安全边界：只补码表，不执行命令。
- 测试要求：15 命令+ACK 全覆盖，2016/2024 冲突断言。
- 验收标准：所有 2016 Code 可查，冲突命令默认采用 2016。

### BIF2016-P1-002

- 任务名称：统一 2016 PK_Type 枚举和命名别名
- 协议依据：`07-command-catalog.md`
- 当前缺口：缺 LOGOUT/SET_LOGININFO，GET_HISDATA 命名不一致。
- 涉及文件：`BInterfacePkType.java`、`PkTypeDescriptor.java`、`CommandDispatcherTest`。
- 安全边界：不新增真实业务执行。
- 测试要求：纯文本、Name+Code、别名解析测试。
- 验收标准：15 命令可解析，未实现命令返回明确 notImplemented/forbidden。

### BIF2016-P1-003

- 任务名称：GET_THRESHOLD/SET_THRESHOLD 2016 Code 对齐
- 协议依据：`commands/070-get-threshold.md`、`commands/080-set-threshold.md`
- 当前缺口：服务存在但 2016 Code 缺失。
- 涉及文件：`GetThresholdService`、`SetThresholdService`、`BInterfaceCommand2016`、测试。
- 安全边界：SET_THRESHOLD 保持默认 disabled/dry-run。
- 测试要求：1901/1902、2001/2002 legacy-2016 请求测试。
- 验收标准：只读查询可按 2016 发包；SET 不真实执行。

### BIF2016-P2-001

- 任务名称：LOGOUT 入站命令实现
- 协议依据：`commands/020-logout.md`
- 当前缺口：无枚举、Handler、Service、测试。
- 涉及文件：`BInterfacePkType`、`LogoutCommandHandler`、`LoginService`、测试。
- 安全边界：只清理当前 FSU 会话，不删除历史数据。
- 测试要求：SessionID 有效/无效、FSUCode 不匹配、状态更新。
- 验收标准：LOGOUT 返回 ResultCode，状态变更为 LOGOUT/OFFLINE。

### BIF2016-P2-002

- 任务名称：SET_FTP/SET_LOGININFO 安全桩与 forbidden ACK
- 协议依据：`commands/100-set-logininfo.md`、`commands/120-set-ftp.md`
- 当前缺口：SET_LOGININFO 无枚举，SET_FTP 仅 notImplemented。
- 涉及文件：`BInterfacePkType`、相关 Handler、安全测试。
- 安全边界：默认 forbidden，不真实调用。
- 测试要求：缺确认令牌/全局禁用/明文密码脱敏。
- 验收标准：命令可识别但安全拒绝，日志不泄漏密码。

### BIF2016-P2-003

- 任务名称：GET_HISDATA 协议确认与只读查询设计
- 协议依据：`commands/050-get-hisdata.md`
- 当前缺口：协议原文不明确，无实现。
- 涉及文件：openspec、Service/Handler 草案、测试。
- 安全边界：只读，限制时间范围，不默认真实调用。
- 测试要求：时间窗口校验、空响应、分页/限量。
- 验收标准：协议结构确认后才进入实现。

### BIF2016-P3-001

- 任务名称：真实 SOAP raw-samples 归档
- 协议依据：`matrices/xml-sample-index.md`
- 当前缺口：真实样本未落盘。
- 涉及文件：`docs/landing/raw-samples/`、fixture 测试。
- 安全边界：脱敏保存，不包含 FTP 明文密码。
- 测试要求：样本可被 `SoapMessageHandler` 解析。
- 验收标准：LOGIN/GET_LOGININFO/GET_FTP/GET_FSUINFO/GET_DATA 至少各有 request/response。

## 23. 不建议立即做的事项

- 不建议实现或真实执行 SET_FSUREBOOT。
- 不建议默认启用 Scheduler。
- 不建议删除 2024 兼容代码。
- 不建议生成正式 seed SQL，DeviceID/SPID 仍需现场确认。
- 不建议在没有真实样本前大改 XML parser。
- 不建议将自动升级作为近期任务，协议原文不明确。

## 24. 结论

当前项目已能支撑真实 FSU LOGIN 接入、2016 GET_LOGININFO/GET_FTP/GET_FSUINFO 只读实测，以及 GET_DATA/GET_THRESHOLD/SET_THRESHOLD 等基础服务和安全框架。但距离“B接口2016 全规范完整实现”仍有明显缺口：

- P0 重点是 GET_DATA 真实数据闭环和 SEND_ALARM 真实告警入库验证。
- P1 重点是 2016 全命令码表、枚举、profile 边界和 2016/2024 隔离。
- P2 重点是 LOGOUT、GET_HISDATA、SET_LOGININFO、SET_FTP 等配置/运维命令。

本次审计未发现需要立即修改 Java 的生产故障；发现的核心风险是协议完整性和真实 FSU 兼容差异，而不是当前 LOGIN 接入链路。
