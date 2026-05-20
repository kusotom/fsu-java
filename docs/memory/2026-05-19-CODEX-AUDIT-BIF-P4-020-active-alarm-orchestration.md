# 2026-05-19 CODEX-AUDIT BIF-P4-020 GET_ACTIVEALARM + ActiveAlarmDiff 编排审计

## 1. 任务目标

按用户指定范围对 BIF-P4-020 的 GET_ACTIVEALARM + ActiveAlarmDiff 编排进行只读审计，重点确认：

- B 接口协议一致性：GET_ACTIVEALARM Code=603、Info 使用 SUID、xmlData 字段、RPC/encoded 链路。
- 只读安全边界：生产代码不得自动修改 alarm_record。
- 真实 FSU 访问风险：默认阶段不得访问真实 FSU，不启用 Scheduler。
- SET 类命令风险：本阶段不得触发 SET_POINT/SET_THRESHOLD/SET_TIME/SET_SCIP/SET_。
- Orchestrator 职责：ActiveAlarmConsistencyAuditService 只负责编排，diff 逻辑由 ActiveAlarmDiffService 承担。
- 测试执行：按要求运行 ActiveAlarm、BInterface、全量测试。

## 2. 架构判断

ActiveAlarmConsistencyAuditService 当前职责较清晰：

- auditWithProvidedSnapshot() 校验 SUID 后读取本地活动告警快照，再调用 ActiveAlarmDiffService.diff()。
- auditByQueryingFsu() 通过 GetActiveAlarmService 查询 FSU，再读取 LocalActiveAlarmSnapshotService 快照，最后调用 ActiveAlarmDiffService.diff()。
- 未发现字段级 diff 逻辑、Repository 写入、直接拼 SOAP、直接 HTTP 调用或复杂 XML 解析。

ActiveAlarmDiffService 承担主要 diff 逻辑，包含 FSU_ONLY、LOCAL_ONLY、MATCHED、FIELD_MISMATCH、INVALID 等差异类型判断。

## 3. 协议一致性判断

正向结论：

- GetActiveAlarmService 使用 BInterfacePkType.GET_ACTIVEALARM。
- Info XML 使用 `<SUID>...</SUID>`。
- FSU 调用路径为 GetActiveAlarmService -> FsuServiceClient -> Real/Stub client；真实客户端路径继续经过 FsuServiceRpcAdapter -> SOAP RPC/encoded invoke。
- xmlData 解析字段使用 SerialNo、DeviceID、SPID、StartTime、EndTime、TriggerVal、AlarmLevel、AlarmFlag、AlarmDesc、AlarmFriDesc 等标准字段。

发现的协议缺口：

- BInterfaceCommand2024 中 GET_ACTIVEALARM 定义为 Code=603，但 SoapMessageHandler.buildRequest() 当前出站仍构造 `<PK_Type>GET_ACTIVEALARM</PK_Type>`，没有生成 `<Name>GET_ACTIVEALARM</Name><Code>603</Code>` 结构；缺少证明出站报文实际携带 Code=603 的测试。
- 测试/桩响应多使用 `<Alarm>` 包裹告警项；解析器可按字段读取，但本轮未看到专门覆盖标准 `<TAlarm>` 包裹形态的 GET_ACTIVEALARM 测试。

## 4. 修改前论证

本次任务是审计，不做业务代码修复。发现的问题需要进入后续修复任务：

- 不在审计任务内修改协议构造层，避免引入与既有 PK_Type 双格式兼容相关的回归。
- 不在审计任务内禁用真实 FSU 集成测试，先在报告中明确该风险和影响。
- 不在审计任务内合并重复 Repository，避免扩大改动范围。

## 5. 写入前验证

写入 memory 前已完成：

- 阅读项目规则：AGENTS.md、CODEX.md、docs/PROJECT_ENGINEERING_RULES.md。
- 阅读用户指定 memory 与审计文件。
- 搜索生产代码的写入关键字、真实 FSU 关键字、Scheduler 关键字、SET 关键字。
- 检查 ActiveAlarmConsistencyAuditService、GetActiveAlarmService、LocalActiveAlarmSnapshotService、ActiveAlarmDiffService、SoapMessageHandler、FsuServiceRpcAdapter、RealHttpFsuServiceClient、SlowDataPollingScheduler 等核心文件。
- 按要求执行三组 Maven 测试。

## 6. 实际修改文件

仅文档闭环：

- 新增 docs/memory/2026-05-19-CODEX-AUDIT-BIF-P4-020-active-alarm-orchestration.md
- 更新 docs/memory/README.md

未修改生产代码、测试代码、配置代码。

## 7. 核心改动

- 记录本次 Codex 审计过程、结论、测试结果和风险。
- 将本次审计记录加入 docs/memory/README.md 索引。

## 8. 测试命令和结果

命令 1：

```bash
mvn test -Dtest='*ActiveAlarm*'
```

第一次在沙箱内执行失败，原因是 Mockito/Byte Buddy inline mock maker 无法 self-attach 当前 JVM；随后使用已批准的 Maven 测试权限重跑通过：

- Tests run: 46
- Failures: 0
- Errors: 0
- Skipped: 0

命令 2：

```bash
mvn test -Dtest='*BInterface*'
```

结果：

- Tests run: 116
- Failures: 0
- Errors: 0
- Skipped: 0

命令 3：

```bash
mvn test
```

结果：

- Tests run: 1073
- Failures: 0
- Errors: 1
- Skipped: 0

唯一 Error 为 DcimPlatformApplicationTests.contextLoads，根因是两个 BInterfaceMessageLogRepository 同名 Spring Bean 冲突。

额外风险：全量测试中 BifP4005RealFsuIntegrationTest 执行了 5 个真实 FSU 调用，目标为 `http://192.168.100.100:8080/services/FSUService`，虽测试断言通过，但日志显示 SocketException、unexpected EOF、connection reset 等网络错误。这违反当前阶段“不真实访问 FSU”的要求。

## 9. 风险

- 默认 `mvn test` 并非纯离线：BifP4005RealFsuIntegrationTest 会手动实例化 RealHttpFsuServiceClient 并访问真实 FSU 地址。
- ActiveAlarmConsistencyAuditService 返回结果中的 realDeviceAccessed 当前硬编码为 false，无法真实反映 FsuServiceResponse.isRealCall()。
- 出站 GET_ACTIVEALARM 报文未被测试证明携带 Code=603。
- 全量测试未通过，不能写成全量通过。

## 10. 遗留问题

- BInterfaceMessageLogRepository Bean 冲突仍存在，来源文件：
  - backend/src/main/java/com/dcim/platform/module/binterface/log/BInterfaceMessageLogRepository.java
  - backend/src/main/java/com/dcim/platform/module/binterface/repository/BInterfaceMessageLogRepository.java
- alarm_record 缺少 serialNo/deviceId，LocalActiveAlarmSnapshotService 只能使用 null 映射，影响与 FSU 活动告警按 SerialNo/DeviceID 精确比对。
- 缺少 GET_ACTIVEALARM 出站 Code=603 的直接断言。
- 缺少标准 `<TAlarm>` 包裹、xmlData 解析失败、CommandHandler 缺失 SUID、BIF-P4-020 专属不触发 Scheduler/SET 的直接测试。

## 11. 下一步建议

- 将 BifP4005RealFsuIntegrationTest 移出默认单元测试路径，改为 JUnit tag、Failsafe integration-test 阶段或显式环境开关。
- 合并或重命名重复的 BInterfaceMessageLogRepository，不建议通过允许 bean override 掩盖问题。
- 让 SoapMessageHandler 或 PK_Type 构造层在 2024 命令出站时携带 Code=603，并补充断言。
- 将 FsuServiceResponse.isRealCall() 透传到 GetActiveAlarmResult 和 ActiveAlarmConsistencyAuditResult。
- 补齐 GET_ACTIVEALARM 缺口测试。
- 评估为 alarm_record 增加 serialNo/deviceId 或建立只读映射，提升 diff 精度。

## 12. git diff 摘要

本次审计阶段未改业务代码。预期 diff 只包含：

- 新增本 memory 文件。
- 更新 docs/memory/README.md 索引。

## 13. git status 摘要

写入前工作区已存在多项未提交变更，包括测试报告、ActiveAlarmConsistencyAuditServiceTest.java、docs/memory/README.md、docs/memory/WORKING-MEMORY.md 以及若干未跟踪审计文件。本次仅追加新的 Codex 审计 memory 与 README 索引，不回退、不覆盖既有变更。
