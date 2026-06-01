# 2026-06-01 FE-MAPPING-DIAG-001 监控页面未映射诊断

## 任务目标

排查前端监控页面“监控项目仍显示未映射”的真实原因，判断是后端真实映射未命中、前端显示误判、旧数据遗留，还是 B接口实时页硬编码遗留。

## 执行边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改后端业务代码。
- 仅对明确的前端显示误判做最小修复。

## 审计结论摘要

后端实时、告警、B接口只读和未映射 API 已调用 eStoneII-IO 映射服务并返回 `signalName/eventName/unit/valueMeaning/alarmMeaning/eventSeverity/mappingStatus/mappingConfidence/templateVariant/needRealDataConfirm` 等字段。

本次确认前端存在显示误判：

- FSU 详情设备列表只识别小写 `mapped`，将后端 `MAPPED_CANDIDATE` 等新状态显示为未映射。
- B接口告警增强页仍用旧字段 `pointCode` 判断是否映射，而后端当前 DTO 使用 `spid/signalId/mappingStatus`。
- 未映射点位页把 D 类假设全集模板候选和真实 unmapped observation 统一计为“未映射点位”，造成误导。

已完成最小修复。

## 修改文件

- `frontend/src/utils/mappingStatus.ts`
- `frontend/src/components/common/StatusBadge.vue`
- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/alarm/AlarmCenterView.vue`
- `frontend/src/views/resource/PointMappingView.vue`
- `frontend/src/views/resource/UnmappedPointsView.vue`
- `frontend/src/views/binterface/PointCard.vue`
- `frontend/src/views/binterface/FsuStatusDetailView.vue`
- `frontend/src/views/binterface/BInterfaceAlarmView.vue`
- `frontend/src/views/binterface/BInterfaceThresholdView.vue`

## 修复内容

- 新增统一 `mappingStatus` 工具，支持 `MAPPED/CONFIRMED/VERIFIED_BY_REAL_DATA/MAPPED_CANDIDATE/HIGH/MEDIUM/TEMPLATE_ONLY/PENDING_REAL_DATA/UNMAPPED/UNKNOWN_EVENT_ID`。
- `TEMPLATE_ONLY` 显示为“模板存在，真实未确认”。
- `PENDING_REAL_DATA` 显示为“待真实数据确认”。
- `MAPPED_CANDIDATE/HIGH/MEDIUM` 显示为“候选映射”，不再误归类为未映射。
- `UNMAPPED/UNKNOWN_EVENT_ID/UNKNOWN_SIGNAL_ID/UNKNOWN_DEVICE_SIGNAL_PAIR` 才归入未映射类。
- FSU 详情设备列表改用统一状态显示。
- B接口告警增强页改用 `spid/signalId/eventId/mappingStatus/mappingConfidence/eventName/alarmMeaning`。
- 未映射页指标拆分为“待复核项 / 真实未映射 / 模板待确认”。

## 后端证据

- `RealtimeDataService` 先 DataScope，再调用 `mappingService.resolveRealtime()`。
- `AlarmRecordService` 先 DataScope，再调用 `mappingService.resolveAlarm()`。
- `BInterfaceFrontendReadController` 的 realtime、alarm、unmapped 接口均先 DataScope，再按 `fsuCode` 参数过滤。
- 未发现本次反馈相关的 DTO 漏字段或 DataScope 绕过。

## 验证结果

- `mvn -q -DskipTests compile`：通过。
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'`：186 tests，0 failures，0 errors。
- `npm run build`：通过，仅既有 Rollup/CSS/chunk size warning。

## 遗留问题

- B接口实时页仍硬编码 `51051243812345` 和电池/环境分组，建议 P1 去除。
- 告警 DTO 缺 `mappingReason/unmappedReason`，未知 EventId 在告警行内不可直接展示，只能通过 unmapped observation 排查。
- 历史实时/告警数据可能需要只读回填映射状态。
- DataScope 仍为内存过滤，后续应迁移到 Repository/SQL 层。

## 下一步建议

1. 执行 `FE-BINTERFACE-REALTIME-P1-001`，去除 B接口实时页硬编码 FSU 和分组。
2. 执行 `BE-ALARM-MAPPING-REASON-P1-001`，让告警 API 返回 mapping reason。
3. 视现场数据情况执行 `DATA-MAPPING-BACKFILL-P1-001`，对历史数据做只读映射回填。

最终口径：后端已返回映射字段，但前端部分显示逻辑误判，已完成最小修复；若核心实时数据页仍显示 `UNMAPPED`，则与后端映射状态一致，应进入真实数据观察或字典补齐任务。
