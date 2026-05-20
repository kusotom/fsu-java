---
name: bif-p4-fix-001-acceptance-blockers
description: "BIF-P4-FIX-001 验收阻断项全部修复: 0 failures 0 errors 1087 tests, contextLoads 首次通过"
metadata:
  type: project
---

## 任务编号 BIF-P4-FIX-001
## 任务名称 BIF-P4-020 验收阻断项修复
## 操作时间 2026-05-19

## 核心改动

### Blocker 修复
1. 真实 FSU 测试隔离: `@Disabled` + `@Tag("real-fsu")` + `@EnabledIfSystemProperty` → 默认 5 skipped
2. DcimPlatformApplicationTests.contextLoads: **首次通过**
   - 删除重复 `log/BInterfaceMessageLogRepository.java`
   - 修复 `BInterfaceMessageLogRepository` 查询方法名 (fsuId→fsuCode, commandCode→command)
   - 修复 `FtpTransferRecordRepository` 查询方法名 (fsuId→fsuCode)
   - 修复 `SetTimeService` 多构造函数 @Autowired 歧义

### Major 修复
3. GET_ACTIVEALARM Code=603: `SoapMessageHandler.buildRequest(name, code, ...)` → Name+Code 出站
4. realDeviceAccessed 透传: FsuServiceResponse.isRealCall() → GetActiveAlarmResult → AuditResult

### Minor 补齐
5. 新增 10 测试: TAlarm, xmlData, 缺失SUID, No Scheduler, No SET, Code=603 出站, realDeviceAccessed

## 测试结果
- 全量: **1087 tests, 0 failures, 0 errors, 5 skipped**
- contextLoads: PASS (首次)
- ActiveAlarm: 60 tests PASS
- BInterface: 120+ tests PASS

## 文件清单
- 删除: binterface/log/BInterfaceMessageLogRepository.java
- 修改: 9 生产文件 + 4 测试文件
- 新增: GetActiveAlarmCommandHandlerTest.java
- 新增: docs/audit/BIF-P4-FIX-001-bif-p4-020-acceptance-blockers.md

## 遗留问题
- alarm_record Entity 缺少 serialNo/deviceId
- BInterfaceMessageLogService/Controller 空壳 TODO
- 真实点位表未到

## 下一步建议
- 可提交 Codex 复审
- 通过后可进入 BIF-P4-021 Scheduler
