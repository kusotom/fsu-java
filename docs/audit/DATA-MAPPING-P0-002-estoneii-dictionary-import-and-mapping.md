# DATA-MAPPING-P0-002 eStoneII-IO 标准码表导入与实时/告警映射闭环

## 1. 修改前论证

当前 `monitoring_point`、`realtime_data`、`alarm_record` 是运行态业务表，不能完整承载 eStoneII-IO 模板维度、独立 `SignalMeanings`、Event 告警字典、Controls 禁用参考、候选映射置信度和未映射观测闭环。直接把 39 个模板 Signal 自动写成真实监测点会误导现场状态，也会违反“D 类假设全集”口径。

本次采用最小闭环方案:

- 新增 eStoneII-IO Signal/Event/Control/Candidate/Unmapped 字典与观测实体，复用当前 JPA `ddl-auto=update` 项目 schema 管理方式。
- 后端实时和告警 API 在返回 DTO 时做字典增强，不把模板全集宣称为真实返回点位。
- `HIGH` 候选作为 `MAPPED_CANDIDATE`，`MEDIUM` 和 `PENDING_REAL_DATA` 保留真实数据确认标记。
- 未匹配 SPID/EventId 写入 unmapped observation，不丢弃。
- Controls 只入库为禁用参考，不开放任何 SET/控制入口。

## 2. 协议依据与资料

使用资料:

- `/home/tom/下载/eStoneII_IO_标准码表.xlsx`
- `/home/tom/下载/eStoneII_IO_标准码表_给Codex完整信息.md`
- `/home/tom/下载/EquipmentTemplateeStoneII-IO.xml`
- `/home/tom/下载/EquipmentTemplateeStoneII-IO-有中间点电压.xml`
- `/home/tom/下载/EquipmentTemplateeStoneII-IO-无中间点电压.xml`

项目内落地资源:

- `backend/src/main/resources/dictionary/estoneii/equipment-template-estoneii-io-default.xml`
- `backend/src/main/resources/dictionary/estoneii/equipment-template-estoneii-io-with-midpoint.xml`
- `backend/src/main/resources/dictionary/estoneii/equipment-template-estoneii-io-no-midpoint.xml`
- `backend/src/main/resources/dictionary/estoneii/device-signal-candidates.csv`

明确舍弃:

- `EquipmentTemplateeStoneIII-IO.xml` 已由用户明确舍弃，不进入当前 P0 映射范围。
- 本次未引入 StoneIII、`511600xx`、`ExtendField4`。

## 3. 后端实现结果

新增字典/观测实体与仓库:

- `EStoneIISignalDictionaryEntity` / `EStoneIISignalDictionaryRepository`
- `EStoneIIEventDictionaryEntity` / `EStoneIIEventDictionaryRepository`
- `EStoneIIControlReferenceEntity` / `EStoneIIControlReferenceRepository`
- `DeviceSignalCandidateEntity` / `DeviceSignalCandidateRepository`
- `UnmappedSignalObservationEntity` / `UnmappedSignalObservationRepository`

新增服务:

- `EStoneIITemplateDictionaryParser`: 解析 eStoneII-IO 模板 XML 和 23 条候选映射 CSV。
- `EStoneIIDictionaryImportService`: 幂等导入 Signal/Event/Control/Candidate。
- `EStoneIIMappingService`: 实时/告警映射解析。
- `UnmappedSignalObservationService`: 未映射项 upsert 记录。

接入链路:

- `RealtimeDataService` 和 `/api/telemetry/realtime` 返回 `RealtimePointDto`，补充 `signalName/unit/valueMeaning/mappingStatus/mappingConfidence/templateVariant/needRealDataConfirm`。
- `AlarmRecordService` 和 `/api/alarms` 返回 `AlarmDto`，补充 `signalName/eventName/alarmMeaning/eventSeverity/mappingStatus/mappingConfidence`。
- `BInterfaceFrontendReadController` 的 alarms/realtime/unmapped 接口接入字典映射和 DataScope 过滤。
- `SendDataService`、`BInterface2016GetDataService`、`SendAlarmService` 在运行态未匹配时记录 unmapped observation。

## 4. Signal 字典导入结果

- WITH_MIDPOINT: 39 个 Signal。
- NO_MIDPOINT: 37 个 Signal。
- 每个 Signal 独立保留 `signalMeaningsRaw` 和结构化 `signalMeaningsJson`。
- `510000581/510000582/510000583/510000584` 标记为派生或表达式相关点位。
- `templateVariant` 支持 `BOTH/NO_MIDPOINT/WITH_MIDPOINT/UNKNOWN`。
- `mappingConfidence` 支持 `HIGH/MEDIUM/PENDING_REAL_DATA/TEMPLATE_ONLY`。

关键验证:

- `510000250` 烟感: `0=无告警, 1=有告警`。
- `510000260` 空调故障: `0=有告警, 1=无告警`。
- 前端不再用全局 DI 0/1 规则替代每个 Signal 自己的含义。

## 5. Event 告警字典导入结果

- WITH_MIDPOINT: 13 个 Event。
- NO_MIDPOINT: 11 个 Event。
- 支持 `EventId -> Event`、`SignalId -> Event`、`SPID/EventId -> Event`。
- 告警 API 返回 `eventName/alarmMeaning/eventSeverity`，同时保留 `signalName` 供协议排障。

## 6. Controls 禁用处理

- 4 个 Control 已导入 `estoneii_control_reference`。
- 入库字段强制:
  - `enabledForControl=false`
  - `controlAccess=disabled`
  - `source=template_reference_only`
- 未新增任何可点击控制按钮。
- 未改动 SET 安全门。
- `SetCommandSafetyGateTest` 覆盖 `SET_POINT/SET_THRESHOLD/SET_FTP/SET_FSUREBOOT` 等默认拒绝。

## 7. DeviceSignalCandidate 导入结果

- 共 23 条候选映射。
- HIGH: 4 条，导入为 `MAPPED_CANDIDATE`。
- MEDIUM: 6 条，保留 `needRealDataConfirm=true`。
- PENDING_REAL_DATA: 13 条，导入为待真实数据确认。
- 未把 23 条候选全部当作真实已确认点位。

## 8. 实时数据映射逻辑

匹配顺序:

1. `FSUID + DeviceID + SignalId`
2. `FSUID + DeviceCode + SignalId`
3. `DeviceID + SignalId`
4. `DeviceCode + SignalId`
5. `SignalId` 字典兜底

字典兜底只用于展示名称、单位和值含义，不代表现场点位已确认。未知 SignalId 进入 `unmapped_signal_observation`，reason 为 `UNKNOWN_SIGNAL_ID` 或对应设备/点位组合原因。

API 返回字段包含 `signalName`、`unit`、`valueMeaning`、`mappingStatus`、`mappingConfidence`、`templateVariant`、`needRealDataConfirm`、`verifiedByRealData`、`derived`、`source`。

## 9. 告警映射逻辑

告警映射支持:

- `SPID -> SignalId -> Signal 字典`
- `SPID/EventId -> Event 字典`
- `EventId -> Event 字典`
- `SignalId -> Event 字典`
- `SignalId` 先映射 `signalName`，再补充 `eventName/alarmMeaning/eventSeverity`

未匹配 EventId/SPID 进入 unmapped observation，不丢弃。

## 10. 未映射处理逻辑

新增 `unmapped_signal_observation` 记录:

- `fsuId/deviceId/deviceCode/spid/signalId/rawId/rawName/rawValue/unit`
- `sourceCommand/messageLogId/rawSampleId/firstSeenAt/lastSeenAt/seenCount/reason`

当前 B接口未映射 API 返回两类数据:

- D 类假设全集候选，reason=`D_CLASS_ASSUMED_TEMPLATE_CANDIDATE`
- 真实运行态未匹配观测，reason=`UNKNOWN_SIGNAL_ID/UNKNOWN_EVENT_ID/UNKNOWN_DEVICE_SIGNAL_PAIR/PARSE_ERROR` 等

## 11. 前端展示调整

调整页面:

- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/alarm/AlarmCenterView.vue`
- `frontend/src/views/resource/PointMappingView.vue`
- `frontend/src/views/resource/UnmappedPointsView.vue`
- `frontend/src/views/binterface/BInterfaceRealtimeView.vue`
- `frontend/src/views/binterface/PointCard.vue`

结果:

- 实时页展示点位名称、单位、值含义、映射状态、映射置信度和待真实确认标记。
- 告警页展示告警名称、告警含义、等级、映射状态和映射置信度。
- 未映射页展示原因、原始值、来源命令、状态和置信度。
- B接口实时卡片直接消费后端映射字段，不再用前端固定 SignalID 白名单构造点位视图。
- 保持 `0.0` 可显示。
- DI 不硬编码 0/1 语义，使用后端 `valueMeaning`。
- 未知单位显示“待确认/单位待确认”。

## 12. DataScope 验证

- `/api/telemetry/realtime`、`/api/alarms`、`/api/b-interface/realtime-points`、`/api/b-interface/alarms` 均在返回前走 `DataScopeService.filterByFsuScope`。
- `/api/b-interface/unmapped-signals` 已修正带 `fsuCode` 参数时仍先 DataScope 后参数过滤，避免 URL 参数绕过。
- 模板字典本身是全局数据；设备实例候选、实时、告警、未映射观测均按 FSU 范围过滤。

当前仍是内存过滤最小闭环，后续生产化应迁移到 Repository/SQL 层过滤，避免分页、总数、排序和性能风险。

## 13. SET / Controls 安全验证

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未放松权限。
- 未改 B接口2016 主线。
- 未引入 B接口2024 行为。
- Controls 只作为禁用参考入库。
- 前端未新增任何 Controls 可点击入口。

## 14. 测试命令和结果

已执行:

```bash
mvn -q -DskipTests compile
mvn -q test -Dtest='com.dcim.platform.mapping.*Test'
mvn test -Dtest='*Security*Test,*DataScope*Test'
mvn test -Dtest='*Template*Test,*Mapping*Test,*Realtime*Test,*Alarm*Test'
mvn test -Dtest='*CommandHandlerTest,*SetCommandSafety*Test,*SetCommandSafetyGateTest'
npm run build
```

结果:

- 映射包测试: 8 tests，0 failures。
- Security + DataScope: 20 tests，0 failures。
- Template/Mapping/Realtime/Alarm 集合: 176 tests，0 failures。
- CommandHandler + SET Safety 集合: 201 tests，0 failures。
- 前端 build 通过；仅存在既有 CSS `//` 注释警告和 chunk size 警告。

未执行全量 `mvn test`。原因: 当前任务限定不访问真实 FSU，项目既有全量测试历史存在协议类 pre-existing failures，本次以映射、权限、DataScope、SET 安全和前端 build 为验证边界。

## 15. 修改文件摘要

主要新增:

- `backend/src/main/java/com/dcim/platform/module/mapping/**`
- `backend/src/main/resources/dictionary/estoneii/**`
- `backend/src/test/java/com/dcim/platform/mapping/**`

主要修改:

- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/dto/BInterfaceFrontendDtos.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/service/BInterface2016GetDataService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/service/SendDataService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/service/SendAlarmService.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/controller/RealtimeDataController.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/service/RealtimeDataService.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/controller/AlarmRecordController.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/service/AlarmRecordService.java`
- 前端实时、告警、映射、未映射、B接口实时相关页面。

## 16. 未解决问题

- 当前 schema 依赖项目既有 Hibernate `ddl-auto=update`，未引入 Flyway/Liquibase migration；生产化前建议显式 DDL。
- 字典导入为 lazy import，首次访问触发；后续建议改为启动导入或后台管理接口。
- `mappingStatus=VERIFIED_BY_REAL_DATA` 机制已预留，但真实数据确认写回尚未完整实现。
- 设备实例候选只按 `fsuScope` 隔离，未补充 `tenantId/stationId` 字段。
- 未映射 API 目前同时返回模板候选和真实未映射观测，后续应拆分为“候选映射”和“真实未映射”两个接口。
- 前端 B接口实时页仍按蓄电池/环境做粗分组，后续应由后端 `deviceName/deviceType` 驱动。

## 17. 下一步建议

1. 补显式 schema migration，并对新增 5 张表加索引。
2. 建立 `VERIFIED_BY_REAL_DATA` 写回流程: 真实 GET_DATA/SEND_DATA/SEND_ALARM 返回后按 DeviceID+SignalId 更新候选映射状态。
3. 拆分映射管理 API: 字典查询、候选映射、真实未映射观测、人工确认。
4. 给候选映射和 unmapped observation 补 `tenantId/stationId`，把 DataScope 迁移到 SQL 层。
5. 继续 FE-P1 点位映射与未映射处理页面建设。

## 18. 最终结论

eStoneII-IO 标准码表已导入，实时数据与告警已具备基于 DeviceID + SignalId/SPID 的候选映射能力；未真实返回的模板点位仍按 TEMPLATE_ONLY 或 PENDING_REAL_DATA 管理。

不得据此声明现场 FSU 所有实时点位已完全确认，也不得声明 D 类全部点位已真实返回。Controls 不可用，仍保持禁用参考。
