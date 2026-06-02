# DATA-MAPPING-NAME-FIX-001 设备/点位名称未确认诊断与修复

## 本次目标

排查并修复普通实时数据和告警页面仍显示“设备名称未确认 / 点位名称未确认”的问题，确认后端是否能把 `DeviceID + SignalId` 映射为业务设备名、点位名、单位和值含义，并让前端通过统一适配器消费后端结果。

## 结论摘要

问题来源不是协议资料不足，而是后端 DTO 透传和前端 adapter 兜底口径不完整：

- 映射服务已有 `deviceName/signalName` 结果，但普通实时和告警 DTO 没有完整写回。
- 候选映射在 Signal 字典暂缺时没有使用候选表 `signalName` 兜底。
- 未知 SignalId 即使 DeviceID 已在候选清单中，也没有返回设备名。
- 普通实时数据没有读取 `monitoring_point` 给历史记录补点位名/单位。
- 前端 `monitorAdapters` 对缺名称数据过早显示待确认。

## 本次修改

- `DeviceSignalCandidateRepository`：增加按 DeviceID/DeviceCode 查询候选设备的方法。
- `EStoneIIMappingService`：候选映射使用候选 `signalName` 兜底；未知点位和标准2016 fallback 可返回已知设备名；未知 SignalId 仍保持 `UNMAPPED`。
- `BInterfaceFrontendDtos`：`AlarmDto` 增加 `deviceName`；`RealtimePointDto` 增加 `pointName`。
- `RealtimeDataService`：透传 `deviceId/deviceCode/deviceName`；读取 `monitoring_point` 兜底 `pointName/unit/signalType`；保留空单位用于“单位待确认”。
- `AlarmRecordService` / `BInterfaceFrontendReadController`：告警 DTO 透传 `deviceName`。
- `monitorAdapters.ts`：普通表优先显示后端业务名称；历史数据缺名称时显示“历史设备/历史点位”；真正未知显示“待确认/待映射”。
- 新增 `AlarmRecordServiceNameMappingTest`，补强 `EStoneIIMappingServiceTest` 和 `RealtimeDataServiceMappingClassificationTest`。

## 验证结果

- `cd backend && mvn -q -DskipTests compile`：通过。
- `cd backend && mvn test -Dtest='EStoneIIMappingServiceTest,RealtimeDataServiceMappingClassificationTest,AlarmRecordServiceNameMappingTest'`：14 tests，0 failures，0 errors。
- `cd backend && mvn test -Dtest='*Mapping*Test,*Template*Test,*Realtime*Test,*Alarm*Test'`：188 tests，0 failures，0 errors。
- `cd backend && mvn test -Dtest='*Security*Test,*DataScope*Test'`：20 tests，0 failures，0 errors。
- `cd frontend && npm run build`：通过；仅有既有 `@vueuse/core` Rollup 注释 warning 和 chunk size warning。

## 安全边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未放松 DataScope。
- 未放松 raw XML / run-once / SET 权限。
- 未引入 StoneIII、`511600xx`、`ExtendField4`。

## 遗留问题

- 历史 `realtime_data` 没有 DeviceID 字段，无法可靠反推出设备实例，只能按历史/待回填处理。
- 建议进入 `DATA-MAPPING-BACKFILL-P1-001` 做只读/幂等历史回填。
- 长期应让 `realtime_data` 保留 DeviceID/DeviceCode，确保 `DeviceID + SignalId` 链路稳定。

## 输出

- [审计报告](../audit/DATA-MAPPING-NAME-FIX-001-device-point-name-diagnosis-and-fix.md)
- [后续 TODO](../tasks/DATA-MAPPING-NAME-FIX-001-follow-up-todo.md)
