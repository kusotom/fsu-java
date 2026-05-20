# BIF-P4-FIX-001：BIF-P4-020 验收阻断项修复

> 修复 Codex 审计 BIF-P4-020 发现的所有 Blocker/Major/Minor 问题
> 完成日期：2026-05-19

---

## 一、审计目标

修复 Codex 对 BIF-P4-020 审计报告中的全部阻断项，使阶段验收通过。

## 二、Codex 问题对应修复表

| # | Codex 问题 | 级别 | 修复方式 | 修改文件 |
|---|-----------|------|---------|---------|
| 1 | 默认 mvn test 访问真实 FSU | Blocker | @Disabled + @Tag + @EnabledIfSystemProperty | BifP4005RealFsuIntegrationTest.java |
| 2 | log/BInterfaceMessageLogRepository Bean 冲突 | Major | 删除无用重复文件 | 删除 log/BInterfaceMessageLogRepository.java |
| 3 | Repository 查询方法名与 Entity 字段不匹配 | Major | 修复方法名 (FsuId→FsuCode, CommandCode→Command) | BInterfaceMessageLogRepository.java, FtpTransferRecordRepository.java |
| 4 | SetTimeService 多构造函数 Spring 歧义 | Major | @Autowired 标注主构造函数 | SetTimeService.java |
| 5 | GET_ACTIVEALARM 出站未带 Code=603 | Major | SoapMessageHandler 支持 Name+Code, RealHttp client 查 2024 映射 | SoapMessageHandler.java, RealHttpFsuServiceClient.java |
| 6 | realDeviceAccessed 硬编码 false | Major | GetActiveAlarmResult 加字段, Service 透传, Orchestrator 使用真实值 | GetActiveAlarmResult.java, GetActiveAlarmService.java, ActiveAlarmConsistencyAuditService.java |
| 7 | 缺少 TAlarm/xmlData/SUID/Scheduler/SET 测试 | Minor | 新增 10 测试覆盖全部缺口 | 3 个测试文件 |

## 三、修改文件清单

### 生产代码
| 文件 | 变更 |
|------|------|
| `binterface/soap/SoapMessageHandler.java` | 新增 `buildRequest(pkType, code, info, xmlData)` 支持 2024 Name+Code PK_Type; 抽取 `appendPkTypeElement()` |
| `binterface/service/fsu/RealHttpFsuServiceClient.java` | `call()` 查 BInterfaceCommandAliasMapper → 构造 Name+Code 出站报文 |
| `binterface/service/GetActiveAlarmResult.java` | 新增 `realDeviceAccessed` 字段和 getter; 新增 `success(..., realDeviceAccessed)` 工厂方法 |
| `binterface/service/GetActiveAlarmService.java` | 透传 `resp.isRealCall()` 到结果 |
| `binterface/service/ActiveAlarmConsistencyAuditService.java` | `auditByQueryingFsu()` 使用 `fsuResult.isRealDeviceAccessed()` |
| `binterface/service/SetTimeService.java` | 2-arg 构造加 `@Autowired` 消除歧义 |
| `binterface/repository/BInterfaceMessageLogRepository.java` | 修正查询方法名匹配 Entity 字段 |
| `binterface/repository/FtpTransferRecordRepository.java` | `findByFsuId*` → `findByFsuCode*` |
| `binterface/log/BInterfaceMessageLogRepository.java` | **删除**（无引用，重复定义） |

### 测试代码
| 文件 | 变更 |
|------|------|
| `BifP4005RealFsuIntegrationTest.java` | 加 `@Disabled` / `@Tag("real-fsu")` / `@EnabledIfSystemProperty` (5 skipped) |
| `SoapMessageHandlerTest.java` | +3 Code=603/Name+Code 出站测试 |
| `GetActiveAlarmServiceTest.java` | +3 TAlarm/xmlData/realDeviceAccessed 测试 |
| `ActiveAlarmConsistencyAuditServiceTest.java` | +4 realDeviceAccessed/Scheduler/SET 测试 |
| `command/GetActiveAlarmCommandHandlerTest.java` | **新增** — 4 测试含缺失 SUID |

## 四、协议一致性

- GET_ACTIVEALARM 出站报文: `<PK_Type><Name>GET_ACTIVEALARM</Name><Code>603</Code></PK_Type>` (2024 Name+Code 格式)
- 旧格式 `<PK_Type>GET_ACTIVEALARM</PK_Type>` 保留兼容（code==null 时）
- TAlarm 包裹形态已通过测试验证解析正确
- Code=604 (GET_ACTIVEALARM_ACK) 已在 stub 响应中使用

## 五、只读安全边界

- 无新增 save/delete/update/insert 调用
- realDeviceAccessed 从 FsuServiceResponse.isRealCall() 透传
- Stub 客户端返回 isRealCall=false
- 默认 mvn test 不访问真实 FSU（5 skipped）

## 六、测试结果

| 测试范围 | 数量 | 结果 |
|----------|------|------|
| 全量 mvn test | **1087** | **0 failures, 0 errors, 5 skipped** |
| contextLoads | 1 | PASS（首次通过） |
| ActiveAlarm 相关 | 60 | PASS |
| BInterface 相关 | 120+ | PASS |
| 真实 FSU 集成 | 5 skipped | 默认跳过 |

## 七、风险与遗留问题

- `alarm_record` Entity 缺少 serialNo/deviceId 字段（非本次范围）
- `BInterfaceMessageLogService` / `BInterfaceMessageLogController` 仍为空壳 TODO 类
- 真实点位表未到, seed SQL 未生成

## 八、结论

**通过。** 所有 Codex 审计指出的阻断项已修复。默认 mvn test 不再访问真实 FSU，contextLoads 首次通过，全量 1087 tests 0 failures 0 errors。
