# DATA-MAPPING-P0-VERIFY-001 eStoneII-IO 映射闭环复验记忆

## 1. 本次目标

复验 DATA-MAPPING-P0-002 的 eStoneII-IO 字典导入、实时数据映射、告警 Event 映射、Controls 禁用、未映射观测、DataScope 和前端展示口径。

边界: 未访问真实 FSU，未执行 SET，未启 Scheduler。

## 2. 结论摘要

DATA-MAPPING-P0-002 在本次小范围修正后通过 P0 复验。

最终口径: eStoneII-IO 标准码表已导入，实时数据与告警已具备基于 DeviceID + SignalId/SPID 的候选映射能力；未真实返回的模板点位仍按 `TEMPLATE_ONLY` 或 `PENDING_REAL_DATA` 管理。

## 3. 已读取与复核

先读取:

- `docs/audit/DATA-MAPPING-P0-002-estoneii-dictionary-import-and-mapping.md`
- `docs/tasks/DATA-MAPPING-P0-002-follow-up-todo.md`
- `docs/memory/2026-05-31-DATA-MAPPING-P0-002-estoneii-dictionary-import-and-mapping.md`
- `docs/memory/README.md`
- `docs/memory/WORKING-MEMORY.md`

重点复核:

- `backend/src/main/resources/dictionary/estoneii/**`
- mapping 实体、仓库、parser、import、mapping、unmapped service
- `BInterfaceFrontendReadController`
- `SendAlarmService`
- 实时/告警 Service 和 DTO
- `RealtimeDataView.vue`
- `AlarmCenterView.vue`
- `PointMappingView.vue`
- `UnmappedPointsView.vue`
- `BInterfaceRealtimeView.vue`
- `PointCard.vue`

## 4. 本次小范围修正

发现并修复 2 个 P0 小风险:

1. `/api/b-interface/fsus/{fsuCode}/devices` 缺显式 DataScope 校验，且候选设备空结果会回退全量候选字典。
   - 已增加 `canAccessFsu(fsuCode)`。
   - 已移除候选设备全量回退。
   - `/login-info` 也改为只返回 DataScope 可见 FSU，不再返回固定默认 FSU。

2. 告警显式 EventId 未命中但 Signal 可识别时，原逻辑不会记录 `UNKNOWN_EVENT_ID`。
   - `EStoneIIMappingService.resolveAlarm` 保留原始 EventId 并返回 `reason=UNKNOWN_EVENT_ID`。
   - `SendAlarmService` 对 `mapped.reason()!=null` 写入 unmapped observation。
   - unmapped 聚合键增加 `rawId`，避免不同未知 EventId 被合并。
   - DTO/前端未映射页补充 `rawId/rawName`。

## 5. 复验结果

通过项:

- eStoneII-IO 为当前唯一 P0 映射依据。
- 未引入 StoneIII、`511600xx`、`ExtendField4`。
- Signal 字典独立解析 `SignalMeanings`，前端不硬编码 SignalName、unit、0/1 含义。
- Event 告警字典支持 EventId、SPID、SignalId 多路径映射。
- Controls 只作为禁用参考，未开放控制入口。
- 未匹配 SignalId/SPID/EventId 不丢弃。
- `/api/b-interface/unmapped-signals?fsuCode=...` 带参与不带参均先 DataScope 后过滤。
- 前端 0.0 显示、未知单位、映射状态、置信度、待确认提示通过。

## 6. 验证结果

- `mvn -q -DskipTests compile`: 最终通过。
- `mvn -q test -Dtest='com.dcim.platform.mapping.*Test'`: 10 tests，0 failures。
- `mvn test -Dtest='*Security*Test,*DataScope*Test'`: 20 tests，0 failures。
- `mvn test -Dtest='*Template*Test,*Mapping*Test,*Realtime*Test,*Alarm*Test'`: 177 tests，0 failures。
- `mvn test -Dtest='*CommandHandlerTest,*SetCommandSafety*Test,*SetCommandSafetyGateTest'`: 201 tests，0 failures。
- `npm run build`: 通过，仅既有 warning。

未执行全量 `mvn test`；本次按用户列出的定向命令复验，且不触发真实 FSU。

## 7. 遗留问题

- `unmapped_signal_observation` 缺独立 `eventId` 列，当前以 `rawId` 承载未知 EventId。
- DataScope 仍是内存过滤，后续应迁移到 Repository/SQL 层。
- 字典表缺显式 migration。
- 候选映射和未映射观测缺 `tenantId/stationId`。
- `frontend/src/compat/realtimeSignalFilter.ts` 仍保留临时 FSU 常量。
- B接口实时页设备分组仍需后端驱动。

## 8. 输出

- [复验报告](../audit/DATA-MAPPING-P0-VERIFY-001-estoneii-mapping-review.md)
- [后续 TODO](../tasks/DATA-MAPPING-P0-VERIFY-001-follow-up-todo.md)
- 本工程记忆

## 9. 下一步

可以进入 FE-P1 页面建设和三方授权页面规划。建议优先做 SQL 层 DataScope、显式 migration、tenant/station 字段补齐和映射/未映射 API 拆分。
