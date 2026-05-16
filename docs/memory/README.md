# 操作记忆索引

> 本目录记录每次项目操作的完整记忆。任何任务未写入操作记忆文件、未更新索引前，视为任务未闭环。
>
> 命名规则：`YYYY-MM-DD-任务编号-简短任务名.md`
>
> 索引按日期倒序排列。

- [2026-05-15 BIF-P4 全量进度同步](2026-05-15-BIF-P4-current-progress-sync.md) — 1070 tests, 真实设备联调/2024协议/SET安全/告警审计完整状态
- [2026-05-15 BIF-P4-020 GET_ACTIVEALARM+diff编排审计](2026-05-15-BIF-P4-020-active-alarm-consistency-audit-orchestration.md) — ConsistencyAuditService双模式, 10新测试
- [2026-05-15 BIF-P4-019 alarm_record只读适配](2026-05-15-BIF-P4-019-active-alarm-local-repository-adapter.md) — Entity→Snapshot映射, DiffAudit编排, 8新测试
- [2026-05-15 BIF-P4-018 活动告警差异核对审计](2026-05-15-BIF-P4-018-active-alarm-diff-audit.md) — FSU快照vs本地alarm_record只读对比, 14新测试
- [2026-05-15 BIF-P4-017 SUREADY注册状态验证闭环](2026-05-15-BIF-P4-017-suready-registration-ready.md) — FSU→SC, Code=103/104, 8新测试
- [2026-05-15 BIF-P4-SAFE-004 SET_TIME token+audit主流程](2026-05-15-BIF-P4-SAFE-004-set-time-token-audit-flow.md) — executeWithSafety完整样板, 10新测试
- [2026-05-15 BIF-P4-SAFE-003 SET审计日志与confirmationToken](2026-05-15-BIF-P4-SAFE-003-set-command-audit-confirmation.md) — Token SHA-256, 审计内存实现, 20新测试
- [2026-05-15 BIF-P4-SAFE-002 SET_TIME安全门禁集成](2026-05-15-BIF-P4-SAFE-002-set-time-safety-integration.md) — Service接入Gate, reject/dryRun/realCall三层, 9新测试
- [2026-05-15 BIF-P4-SAFE-001 SET类命令统一安全门禁](2026-05-15-BIF-P4-SAFE-001-set-command-safety-gate.md) — 8+5命令全覆盖, 默认全false, 32新测试
- [2026-05-15 BIF-P4-016 GET_ACTIVEALARM活动告警查询](2026-05-15-BIF-P4-016-get-activealarm-query.md) — 2024活动告警查询, TAlarm解析, 11新测试
- [2026-05-15 BIF-P4-015 GET_SPCONFIGOPTION配置模板查询](2026-05-15-BIF-P4-015-get-spconfigoption-query.md) — 2024配置模板查询, 5新测试
- [2026-05-15 BIF-P4-014 SET_TIME时间同步闭环](2026-05-15-BIF-P4-014-set-time-sync.md) — 2024时间同步, 安全门禁, 9新测试
- [2026-05-15 BIF-P4-013 GET_SUFTP查询闭环](2026-05-15-BIF-P4-013-get-suftp-query.md) — 2024标准FTP查询, 脱敏, 11新测试
- [2026-05-15 BIF-P4-012 GET_SUINFO在线状态闭环](2026-05-15-BIF-P4-012-get-suinfo-online-status.md) — 2024心跳命令, Service+Handler+Stub, 11新测试
- [2026-05-15 BIF-P4-011 PK_Type双格式+字段别名解析层集成](2026-05-15-BIF-P4-011-pktype-field-alias-parser-integration.md) — PkTypeDescriptor双格式, parsePkType, normalizeFields, 40新测试
- [2026-05-15 BIF-P4-010 2024命令枚举与字段别名映射](2026-05-15-BIF-P4-010-2024-command-field-alias-mapping.md) — 44条命令枚举 + 旧命名兼容映射 + 字段别名, 74新测试
- [2026-05-15 BIF-P4-009 PDF vs MD 对比审计](2026-05-15-BIF-P4-009-pdf-vs-md-diff-audit.md) — 协议规范层完全一致, 0处实质差异, 12项2016→2024差异清单
- [2026-05-15 BIF-P4-008 SCService入站RPC兼容性评估](2026-05-15-BIF-P4-008-scservice-rpc-compatibility.md) — parse()已双向兼容, buildResponse()待确认, 7个RPC解析测试通过
- [2026-05-15 BIF-P4-007-A 点位采集模板/运维协调包](2026-05-15-BIF-P4-007-A-fsu-signal-collection-template.md) — CSV模板14列, 补录规则, FSU管理方资料请求清单
- [2026-05-15 BIF-P4-006 FSU管理协调/点位对齐](2026-05-15-BIF-P4-006-fsu-management-point-alignment.md) — 固化联调结果, FSU协调清单, 点位对齐矩阵, 补录建议
- [2026-05-15 BIF-P4-005-C RPC adapter 真实 FSU 重新联调](2026-05-15-BIF-P4-005-C-rpc-integration-retest.md) — SOAP Fault 消除, GET_DATA/GET_THRESHOLD ResultCode=0, RPC 格式验证通过
- [2026-05-15 BIF-P4-005-B FsuServiceRpcAdapter 实现](2026-05-15-BIF-P4-005-B-fsu-rpc-adapter.md) — 新增 adapter + RealHttpFsuServiceClient 适配 + 27 测试, 791 全量回归通过
- [2026-05-15 BIF-P4-005-A WSDL与B接口协议再审计](2026-05-15-BIF-P4-005-A-wsdl-protocol-reanalysis.md) — 确认 RPC style/encoded use/operation=invoke, Fault 归因于 document-style 不匹配 WSDL RPC, 推荐 FsuServiceRpcAdapter
- [2026-05-15 BIF-P4-004 单台 FSU 只读联调 WSDL 端口确认版](2026-05-15-BIF-P4-004-real-fsu-readonly-wsdl-port.md) — Port 8080 确认正确，FSUService SOAP 可达，发现 SoapMessageHandler 报文格式与 WSDL RPC 约定不匹配
- [2026-05-15 BIF-P4-005 单台真实 FSU 只读联调执行](2026-05-15-BIF-P4-005-real-fsu-readonly-integration.md) — 5 命令全部 HTTP 404，网络可达但 SOAP endpoint 路径不匹配，错误处理链正确
- [2026-05-15 BIF-P4-004 单台 FSU 只读联调执行前检查](2026-05-15-BIF-P4-004-single-fsu-readonly-precheck.md) — 14 项硬边界/配置安全/白名单/黑名单/RestHttp 条件加载/Resolver 就绪/日志脱敏/回滚方案，全部通过
- [2026-05-15 BIF-P4-003 单台 FSU 只读联调计划](2026-05-15-BIF-P4-003-single-fsu-readonly-integration-plan.md) — 联调计划/白名单/黑名单/回滚方案/配置模板，23 安全测试，全量 758 通过
- [2026-05-15 BIF-P4-002 FSU 地址发现/ServiceUrl 管理](2026-05-15-BIF-P4-002-fsu-endpoint-resolver.md) — FsuEndpointResolver/FsuEndpointResult/FsuServiceEndpointService，24 测试，全量 735 通过
- [2026-05-15 BIF-P4-001 慢数据通道启用确认/安全验收](2026-05-15-BIF-P4-001-slow-channel-readiness.md) — 配置审计/命令覆盖/脱敏检查/联调清单，19 验收测试，全量 711 通过
- [2026-05-15 BIF-P3-007 GET_LOGININFO / GET_FTP 查询类闭环](2026-05-15-BIF-P3-007-get-logininfo-get-ftp.md) — Handler 桩→真实，9 新增/4 修改文件，80 新增测试，全量 692 通过
- [2026-05-15 BIF-P3-006 TIME_CHECK 时间同步闭环](2026-05-15-BIF-P3-006-time-check.md) — 新增 TimeCheckService/TimeCheckResult，Handler 桩→真实，39 测试，全量 610 通过
- [2026-05-15 BIF-P3-005 慢数据通道 Signal 轮询目标配置](2026-05-15-BIF-P3-005-signal-polling-target.md) — 复用 MonitoringPoint，新增 SignalPollingTargetService，13 测试，全量 571 通过
- [2026-05-15 BIF-P3-004 慢数据通道定时轮询框架](2026-05-15-BIF-P3-004-slow-data-polling.md) — 新增 SlowDataPollingService/Scheduler/Properties/Result，38 测试，全量 558 通过
- [2026-05-14 BIF-P3-003 SET_THRESHOLD 门限设置闭环](2026-05-14-BIF-P3-003-set-threshold.md) — 安全门禁+审计+二次确认 4 层安全架构，新增 5 生产文件 + 54 测试，全量 514 通过
- [2026-05-14 BIF-P3-002 GET_THRESHOLD 门限查询闭环](2026-05-14-BIF-P3-002-get-threshold.md) — 基于 FsuServiceClient 实现 GET_THRESHOLD 查询，新增 2 生产文件 + 41 测试，全量 460 通过
- [2026-05-14 BIF-P3-001 FSU SOAP 客户端基础设施 + GET_DATA 闭环](2026-05-14-BIF-P3-001-fsu-service-client-get-data.md) — 新建 FsuServiceClient/Stub/Real 三层架构，实现 GET_DATA 桩→真实逻辑，新增 45 测试全量 424 通过
- [2026-05-14 BIF-P2-002 慢数据通道审计与优先级规划](2026-05-14-BIF-P2-002-slow-data-channel-audit.md) — 全面审计 12 个慢数据命令，输出优先级矩阵/风险分类/阶段建议，仅只读不修改业务代码
- [2026-05-14 BIF-P2-001 离线检测 / FSU 在线状态维护](2026-05-14-BIF-P2-001-offline-detection.md) — 新建 OfflineDetectionService/OfflineDetectionResult，扩展 Repository，新增 20 测试，全量 379 测试通过
- [2026-05-14 BIF-P1-005 主链路端到端集成测试与回归矩阵](2026-05-14-BIF-P1-005-main-flow-integration.md) — 新增 BInterfaceMainFlowIntegrationTest，23 集成测试覆盖全链路/顺序依赖/数据隔离，全量 359 测试通过
- [2026-05-14 BIF-P1-004 SEND_ALARM 告警上报闭环](2026-05-14-BIF-P1-004-send-alarm.md) — SendAlarmCommandHandler 桩→真实实现，新增 SendAlarmService/SendAlarmResult，新增 39 测试，全量 336 测试通过
- [2026-05-14 BIF-P1-003 SEND_DATA 实时数据上报闭环](2026-05-14-BIF-P1-003-send-data.md) — SendDataCommandHandler 桩→真实实现，新增 SendDataService/SendDataResult，新增 39 测试，全量 297 测试通过
- [2026-05-14 BIF-P1-002 HEARTBEAT 心跳/在线状态闭环](2026-05-14-BIF-P1-002-heartbeat-session.md) — HeartbeatCommandHandler 桩→真实实现，复用 LoginService Session 管理，新增 14 测试，全量 258 测试通过
- [2026-05-14 BIF-P1-001 LOGIN 注册/Session 管理业务闭环](2026-05-14-BIF-P1-001-login-session.md) — 完成 LOGIN 业务闭环：LoginResult/LoginService/LoginCommandHandler 变更/42 新增测试/BaseStub 重构，全量 244 测试通过
- [2026-05-14 BIF-P0-004 CommandDispatcher 命令分发层](2026-05-14-BIF-P0-004-command-dispatcher.md) — 完成 CommandDispatcher 收尾，新增 CommandContext/CommandResult/7 Handler/39 测试，全量 212 测试通过
- [2026-05-14 BIF-P0-003 XMLData 解析与构造统一层](2026-05-14-BIF-P0-003-xmldata-layer.md) — 创建 XmlDataParser/Builder/Validator，重构测试至 77 用例，173 B-interface 测试全通过
- [2026-05-14 RULE-001 操作记忆写入规则](2026-05-14-RULE-001-operation-memory-rule.md) — 新增操作记忆写入规则，创建 docs/memory/ 体系，更新四份规则文件
