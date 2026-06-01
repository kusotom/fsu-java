# DATA-MAPPING-P0-VERIFY-001 eStoneII-IO 映射闭环复验

## 1. 复验范围

本次按用户要求先读取并复核:

- `docs/audit/DATA-MAPPING-P0-002-estoneii-dictionary-import-and-mapping.md`
- `docs/tasks/DATA-MAPPING-P0-002-follow-up-todo.md`
- `docs/memory/2026-05-31-DATA-MAPPING-P0-002-estoneii-dictionary-import-and-mapping.md`
- `docs/memory/README.md`
- `docs/memory/WORKING-MEMORY.md`

代码复验覆盖后端 mapping 模块、B接口前端只读聚合 API、实时数据、告警、未映射观测、DataScope 调用点，以及前端实时、告警、点位映射、未映射和 B接口实时页面。

安全边界: 未访问真实 FSU，未执行 SET，未启 Scheduler。

## 2. 总体结论

DATA-MAPPING-P0-002 在本次小范围修正后通过 P0 复验。

当前实现只以 eStoneII-IO 为 P0 映射依据，主测点编号使用 `510000xxx` SignalId。代码与资源未把 StoneIII、`511600xx`、`ExtendField4` 引入当前映射主流程。`WITH_MIDPOINT/NO_MIDPOINT/BOTH`、`mappingConfidence`、`templateVariant`、`needRealDataConfirm` 语义与前期决策一致。

## 3. 复验发现与已修正问题

### 3.1 DataScope 小风险已修正

发现 `/api/b-interface/fsus/{fsuCode}/devices` 直接使用路径参数返回候选设备，且候选设备构造在无指定 FSU 候选时会回退全量候选字典。该路径可能被三方用户通过 URL 参数探测未授权 FSU 的候选设备。

已修正:

- `getDevices` 先调用 `canAccessFsu(fsuCode)`，未授权或不存在时返回 `FSU not found`。
- `buildKnownDevices` 和 `knownDeviceCount` 不再对空结果回退 `findAllByOrderByDeviceIdAscSignalIdAsc()`。
- `/login-info` 不再无参返回固定默认 FSU 信息，改为从当前 DataScope 可见 FSU 中选择，并使用 FSU 设备表字段。

### 3.2 EventId 未命中观测已修正

发现告警侧在 Signal 可识别、但显式 `AlarmCode/EventId` 未命中字典时，原逻辑可能通过 `SignalId -> Event` 兜底返回 eventName，但不记录 `UNKNOWN_EVENT_ID` 未映射观测。

已修正:

- `EStoneIIMappingService.resolveAlarm` 保留原始显式 EventId，显式 EventId 未命中时返回 `reason=UNKNOWN_EVENT_ID`。
- `SendAlarmService` 在 `mapped.reason()!=null` 时也写入 unmapped observation。
- `UnmappedSignalObservationService` 聚合键增加 `rawId`，避免同一 Signal 下不同未知 EventId 被合并。
- `UnmappedSignalDto` 增加 `rawId/rawName`，前端未映射页优先展示后端原始字段。

## 4. 字典口径复验

通过 `rg` 复核生产代码、前端代码和字典资源，未发现 StoneIII、`511600xx`、`ExtendField4` 进入当前 eStoneII-IO 主流程。仅 DATA-MAPPING-P0-002 审计报告中保留“明确舍弃 StoneIII”的说明。

字典资源为:

- `backend/src/main/resources/dictionary/estoneii/equipment-template-estoneii-io-default.xml`
- `backend/src/main/resources/dictionary/estoneii/equipment-template-estoneii-io-with-midpoint.xml`
- `backend/src/main/resources/dictionary/estoneii/equipment-template-estoneii-io-no-midpoint.xml`
- `backend/src/main/resources/dictionary/estoneii/device-signal-candidates.csv`

## 5. Signal 复验

通过项:

- Signal 字典实体包含 `signalId/signalName/unit/signalType/signalMeaningsRaw/signalMeaningsJson/templateVariant/mappingConfidence/needRealDataConfirm/derived` 等字段。
- `EStoneIITemplateDictionaryParser` 按每个 Signal 独立解析 `SignalMeanings`，未使用全局 DI 0/1 规则。
- `EStoneIIMappingService.resolveRealtime` 匹配顺序符合 `FSUID+DeviceID+SignalId -> FSUID+DeviceCode+SignalId -> DeviceID+SignalId -> DeviceCode+SignalId -> SignalId 字典兜底`。
- 字典兜底只标记 `TEMPLATE_ONLY`，不声明为真实采集点。

## 6. Event / Alarm 复验

通过项:

- 告警支持 `EventId -> Event`、`SPID/EventId -> Event`、`SignalId -> Event` 兜底。
- 告警 API 返回 `eventName/alarmMeaning/eventSeverity/mappingStatus/mappingConfidence/templateVariant/needRealDataConfirm`。
- 本次补齐了“显式 EventId 未命中但 Signal 可识别”的未映射观测记录。

遗留: `unmapped_signal_observation` 目前以 `rawId` 承载未知 EventId，尚无独立 `eventId` 列。当前不丢失信息，但生产化建议补字段。

## 7. Controls / SET 复验

Controls 仅入库为禁用参考:

- `enabledForControl=false`
- `controlAccess=disabled`
- `source=template_reference_only`

未发现前端映射、实时、告警、未映射页面存在 Controls 可点击入口。SET 安全门测试覆盖 `SET_POINT/SET_THRESHOLD/SET_FTP/SET_LOGININFO/SET_FSUREBOOT` 默认拒绝。本次未新增 SET 执行路径，未绕过 `SetCommandSafetyGate`。

## 8. Unmapped Observation 复验

通过项:

- 未匹配 SignalId/SPID/EventId 不丢弃，写入 unmapped observation。
- 记录字段包含 `fsuId/deviceId/deviceCode/spid/signalId/rawId/rawName/rawValue/unit/sourceCommand/messageLogId/rawSampleId/firstSeenAt/lastSeenAt/seenCount/reason`。
- 重复观测按 `fsuId + deviceId + signalId + rawId + sourceCommand + reason` 聚合。
- `/api/b-interface/unmapped-signals` 不带和带 `fsuCode` 均先做 DataScope，再做参数过滤。

## 9. 前端复验

通过项:

- `RealtimeDataView.vue`、`AlarmCenterView.vue`、`PointMappingView.vue`、`UnmappedPointsView.vue`、`BInterfaceRealtimeView.vue` 均消费后端返回的 `signalName/eventName/unit/valueMeaning/alarmMeaning`。
- 0.0 展示使用显式 `null/undefined/''` 判定，不会被误判为空。
- DI 页面文案提示“按后端字典显示/需现场复核”，未硬编码 0/1 含义。
- `mappingStatus/mappingConfidence/needRealDataConfirm` 已在核心页面展示。

遗留:

- `frontend/src/compat/realtimeSignalFilter.ts` 仍保留临时 FSU 常量，当前只有 `BInterfaceRealtimeView.vue` 使用 `HARDCODED_REALTIME_FSU_CODE`，未再使用 SignalID 白名单过滤函数。后续应由路由或站点上下文传入 FSU。
- B接口实时页仍有蓄电池/环境分组的静态布局，后续应完全由后端 `deviceName/deviceType` 驱动。

## 10. API 字段复验

实时数据 DTO 已覆盖:

- `signalName`
- `unit`
- `valueMeaning`
- `mappingStatus`
- `mappingConfidence`
- `templateVariant`
- `needRealDataConfirm`

告警 DTO 已覆盖:

- `eventName`
- `alarmMeaning`
- `eventSeverity`
- `mappingStatus`
- `mappingConfidence`
- `templateVariant`
- `needRealDataConfirm`

未映射 DTO 已覆盖:

- `fsuCode`
- `deviceId`
- `signalId/spid`
- `rawId/rawName`
- `source/rawCommand`
- `rawValue`
- `firstSeenAt/lastSeenAt/seenCount`
- `mappingStatus`

## 11. 测试结果

已执行:

- `mvn -q -DskipTests compile`: 最终通过。
- `mvn -q test -Dtest='com.dcim.platform.mapping.*Test'`: 10 tests，0 failures，0 errors。
- `mvn test -Dtest='*Security*Test,*DataScope*Test'`: 20 tests，0 failures，0 errors。
- `mvn test -Dtest='*Template*Test,*Mapping*Test,*Realtime*Test,*Alarm*Test'`: 177 tests，0 failures，0 errors。
- `mvn test -Dtest='*CommandHandlerTest,*SetCommandSafety*Test,*SetCommandSafetyGateTest'`: 201 tests，0 failures，0 errors。
- `npm run build`: 通过；仅存在既有 CSS `//` 注释 warning、Rollup PURE 注释 warning 和 chunk size warning。

未执行全量 `mvn test`。本次任务要求的覆盖命令已执行，且安全边界禁止访问真实 FSU；全量历史上存在协议类 pre-existing failures，本次复验不以全量作为阻塞项。

## 12. 结论

1. eStoneII-IO 字典口径通过。
2. Signal 字典导入与实时映射通过。
3. Event / Alarm 字典映射在本次修正后通过。
4. Controls 禁用参考和 SET 安全边界通过。
5. Unmapped observation 在本次修正后满足 P0 闭环。
6. `/api/b-interface/unmapped-signals?fsuCode=...` 带参 DataScope 复验通过。
7. 本次发现的两个 P0 风险均已小范围修正，当前未保留开放 P0。
8. 可以进入 FE-P1 页面建设和三方授权页面规划，但 P1 中的 SQL 层 DataScope、显式 migration、tenant/station 字段仍应优先补齐。

最终口径: eStoneII-IO 标准码表已导入，实时数据与告警已具备基于 DeviceID + SignalId/SPID 的候选映射能力；未真实返回的模板点位仍按 `TEMPLATE_ONLY` 或 `PENDING_REAL_DATA` 管理。
