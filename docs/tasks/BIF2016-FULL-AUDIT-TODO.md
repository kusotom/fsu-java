# BIF2016-FULL-AUDIT-TODO

> 来源：`CODEX-AUDIT-2016-FULL-001`。  
> 边界：任务默认不执行真实 FSU，不执行 SET，不启用 Scheduler；真实调用必须显式开关和单独验收。

## P0

### BIF2016-P0-001

- 任务编号：BIF2016-P0-001
- 任务名称：GET_DATA 2016 Emerson DeviceList/TSemaphore 请求闭环
- 协议依据：`openspec/protocols/binterface-2016/commands/040-get-data.md`，`profiles/emerson-2016.md`
- 当前缺口：当前 `GetDataService` 只构造 `<SignalID>`，真实 FSU GET_DATA 返回空 DeviceList，未闭环实时数据。
- 涉及文件：`GetDataService.java`、`FsuServiceRequest.java`、`XmlDataParser.java`、`GetDataServiceTest.java`、raw-samples。
- 安全边界：只读；不启 Scheduler；不默认真实调用。
- 测试要求：DeviceList/TSemaphore 请求构造测试、响应解析测试、Stub 回归；真实 run-once 单独显式启用。
- 验收标准：401/402 legacy-2016 请求可生成，真实或 fixture 响应可解析，非空时可进入 telemetry 入库路径。

### BIF2016-P0-002

- 任务编号：BIF2016-P0-002
- 任务名称：SEND_ALARM 真实样本采集与告警入库验收
- 协议依据：`openspec/protocols/binterface-2016/commands/030-send-alarm.md`
- 当前缺口：SEND_ALARM 代码存在，但无真实上报告警样本和入库验收。
- 涉及文件：`SendAlarmServiceTest.java`、`docs/landing/raw-samples/`、观察/审计文档。
- 安全边界：只被动接收；不主动触发真实 FSU 告警；不修改 alarm_record 状态机语义。
- 测试要求：用真实脱敏 TAlarm 样本补 fixture 解析和入库测试。
- 验收标准：真实 SEND_ALARM 可保存原始报文，`alarm_record.serial_no/device_id/spid` 有值。

### BIF2016-P0-003

- 任务编号：BIF2016-P0-003
- 任务名称：真实 SOAP raw-samples 强制归档
- 协议依据：`openspec/protocols/binterface-2016/matrices/xml-sample-index.md`
- 当前缺口：LANDING-006/LANDING-015 原始 SOAP 未保存到磁盘。
- 涉及文件：`docs/landing/raw-samples/`、`SoapMessageHandlerTest.java`。
- 安全边界：脱敏保存；不包含 FTP 明文密码。
- 测试要求：样本可被 `SoapMessageHandler` 解析；敏感字段脱敏断言。
- 验收标准：LOGIN、GET_LOGININFO、GET_FTP、GET_FSUINFO、GET_DATA 至少各有 request/response 样本。

## P1

### BIF2016-P1-001

- 任务编号：BIF2016-P1-001
- 任务名称：补齐 BInterfaceCommand2016 全 15 命令与 ACK 码表
- 协议依据：`04-enums.md`、`matrices/command-code-matrix.md`
- 当前缺口：`BInterfaceCommand2016` 仅定义 GET_DATA、SEND_ALARM、GET_LOGININFO、GET_FTP、GET_FSUINFO 等少量条目。
- 涉及文件：`BInterfaceCommand2016.java`、新增/更新测试。
- 安全边界：只补码表，不执行任何命令。
- 测试要求：15 命令和 ACK 全覆盖，2016/2024 冲突命令断言。
- 验收标准：所有 2016 Code 可查，冲突时当前主线采用 2016。

### BIF2016-P1-002

- 任务编号：BIF2016-P1-002
- 任务名称：统一 2016 PK_Type 枚举和别名解析
- 协议依据：`07-command-catalog.md`
- 当前缺口：`BInterfacePkType` 缺 `LOGOUT`、`SET_LOGININFO`；`GET_HISDATA` 与 `GET_HISTORY_DATA` 命名不一致。
- 涉及文件：`BInterfacePkType.java`、`PkTypeDescriptor.java`、`CommandDispatcherTest.java`。
- 安全边界：不新增真实业务执行；未实现命令返回明确 notImplemented/forbidden。
- 测试要求：纯文本、Name+Code、别名解析全覆盖。
- 验收标准：15 个 2016 命令均可解析和分发到实现/桩/拒绝路径。

### BIF2016-P1-003

- 任务编号：BIF2016-P1-003
- 任务名称：GET_THRESHOLD/SET_THRESHOLD 2016 Code 对齐
- 协议依据：`commands/070-get-threshold.md`、`commands/080-set-threshold.md`
- 当前缺口：业务服务存在，但 1901/1902、2001/2002 未进入 2016 Code 表。
- 涉及文件：`GetThresholdService.java`、`SetThresholdService.java`、`BInterfaceCommand2016.java`、测试。
- 安全边界：SET_THRESHOLD 继续默认 disabled/dry-run。
- 测试要求：legacy-2016 请求构造和 ACK 解析测试。
- 验收标准：GET_THRESHOLD 可按 2016 发包；SET_THRESHOLD 不真实执行。

### BIF2016-P1-004

- 任务编号：BIF2016-P1-004
- 任务名称：2016 / Emerson / 2024 Profile 边界硬化
- 协议依据：`profiles/standard-2016.md`、`profiles/emerson-2016.md`、`profiles/future-2024-compatibility.md`
- 当前缺口：部分出站服务可能走 2024 alias，真实 FSU 主线需要明确 2016。
- 涉及文件：`FsuServiceRequest.java`、`RealHttpFsuServiceClient.java`、相关 Service 测试。
- 安全边界：不删除 2024 代码；不默认真实调用。
- 测试要求：同一命令在 standard-2016/emerson-2016/future-2024 下的 Code 断言。
- 验收标准：真实 FSU 默认 profile 明确为 emerson-2016/legacy-2016。

## P2

### BIF2016-P2-001

- 任务编号：BIF2016-P2-001
- 任务名称：LOGOUT 入站命令实现
- 协议依据：`commands/020-logout.md`
- 当前缺口：无枚举、Handler、Service、测试。
- 涉及文件：`BInterfacePkType.java`、`LogoutCommandHandler.java`、`LoginService.java`、测试。
- 安全边界：只清理当前 FSU 会话，不删除历史记录。
- 测试要求：SessionID 有效/无效、FSUCode 不匹配、状态更新。
- 验收标准：LOGOUT 返回 ResultCode，状态更新为 LOGOUT/OFFLINE。

### BIF2016-P2-002

- 任务编号：BIF2016-P2-002
- 任务名称：GET_HISDATA 协议确认与只读查询设计
- 协议依据：`commands/050-get-hisdata.md`
- 当前缺口：协议原文不明确，无实现。
- 涉及文件：openspec 协议文档、`GetHisdataService` 草案、测试。
- 安全边界：只读；限制时间范围；不默认真实调用。
- 测试要求：起止时间校验、空响应、限量策略。
- 验收标准：协议结构确认后再进入实现。

### BIF2016-P2-003

- 任务编号：BIF2016-P2-003
- 任务名称：SET_LOGININFO 安全识别与默认拒绝
- 协议依据：`commands/100-set-logininfo.md`
- 当前缺口：`BInterfacePkType` 缺 SET_LOGININFO，无 Handler。
- 涉及文件：`BInterfacePkType.java`、`SetLoginInfoCommandHandler.java`、安全测试。
- 安全边界：默认 forbidden，不真实调用。
- 测试要求：全局关闭、缺确认令牌、未知字段拒绝。
- 验收标准：命令可识别但默认安全拒绝。

### BIF2016-P2-004

- 任务编号：BIF2016-P2-004
- 任务名称：SET_FTP 安全桩与密码脱敏
- 协议依据：`commands/120-set-ftp.md`
- 当前缺口：`SetFtpCommandHandler` 仅 notImplemented，无脱敏/审计设计。
- 涉及文件：`SetFtpCommandHandler.java`、安全门禁、测试。
- 安全边界：默认 forbidden；Password 全链路脱敏。
- 测试要求：密码不进日志、不进报告、不真实调用。
- 验收标准：SET_FTP 明确安全拒绝，敏感字段脱敏。

### BIF2016-P2-005

- 任务编号：BIF2016-P2-005
- 任务名称：TIME_CHECK 2016 Code 与真实 run-once 验证
- 协议依据：`commands/130-time-check.md`
- 当前缺口：服务存在但 `BInterfaceCommand2016` 缺 1301/1302，真实 FSU 未验证。
- 涉及文件：`TimeCheckService.java`、`BInterfaceCommand2016.java`、测试。
- 安全边界：只读，不修改 FSU 时间。
- 测试要求：legacy-2016 请求和 ACK 测试；真实 run-once 需显式开关。
- 验收标准：TIME_CHECK 使用 1301/1302，返回 FSUTime 可解析。

## P3

### BIF2016-P3-001

- 任务编号：BIF2016-P3-001
- 任务名称：规则与 memory 索引路径修正
- 协议依据：工程规则和 memory 规则。
- 当前缺口：项目内 `docs/PROJECT_ENGINEERING_RULES.md` 不存在；memory README 引用不存在的 SPEC-2016 memory 文件。
- 涉及文件：`docs/memory/README.md`、规则文档。
- 安全边界：文档修正，不改业务。
- 测试要求：`find`/链接路径核对。
- 验收标准：索引指向真实存在文件或明确外部路径。

### BIF2016-P3-002

- 任务编号：BIF2016-P3-002
- 任务名称：CPU/MEM、FTPConfig、TThreshold 结构化持久化评估
- 协议依据：`06-data-structures.md`
- 当前缺口：多个协议字段仅在内存 Result 或 statusDetail 文本中存在。
- 涉及文件：Entity/DDL 设计文档、后续迁移。
- 安全边界：先评估，不直接生成正式 seed SQL。
- 测试要求：schema/entity 对齐测试。
- 验收标准：形成可执行的数据模型方案。

### BIF2016-P3-003

- 任务编号：BIF2016-P3-003
- 任务名称：SET_FSUREBOOT 显式 forbidden 文档与测试
- 协议依据：`commands/150-set-fsureboot.md`
- 当前缺口：无 Handler 是安全策略，但对外返回路径不明确。
- 涉及文件：文档、安全测试，必要时补 forbidden Handler。
- 安全边界：禁止真实执行。
- 测试要求：任何入口均不能真实执行 SET_FSUREBOOT。
- 验收标准：命令可审计、可拒绝、不可执行。
