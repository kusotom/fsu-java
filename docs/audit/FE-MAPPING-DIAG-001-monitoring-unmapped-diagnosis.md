# FE-MAPPING-DIAG-001 监控页面“未映射”诊断报告

日期：2026-06-01  
范围：前端监控/告警/映射/未映射/B接口实时页面，后端实时/告警/B接口只读/未映射 API 和 DataScope 链路。  
边界：未访问真实 FSU，未执行 SET，未启 Scheduler，未修改后端业务代码。

## 1. 问题现象

用户反馈前端监控页面中“监控项目仍显示未映射”。本次复核目标是判断该现象来自：

- 后端真实返回 `UNMAPPED` / `UNKNOWN_EVENT_ID`；
- 前端仍使用旧字段或旧状态判断；
- 未映射页把模板候选统计为“未映射”造成误解；
- 旧数据未回填；
- B接口实时页硬编码 FSU / 分组遗留。

## 2. 已读取文件

前置文档：
- `docs/audit/DATA-MAPPING-P0-002-estoneii-dictionary-import-and-mapping.md`
- `docs/audit/DATA-MAPPING-P0-VERIFY-001-estoneii-mapping-review.md`
- `docs/tasks/DATA-MAPPING-P0-VERIFY-001-follow-up-todo.md`
- `docs/memory/WORKING-MEMORY.md`
- `docs/memory/README.md`

后端重点文件：
- `backend/src/main/java/com/dcim/platform/module/telemetry/service/RealtimeDataService.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/service/AlarmRecordService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/dto/BInterfaceFrontendDtos.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIMappingService.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIDictionaryImportService.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIITemplateDictionaryParser.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/UnmappedSignalObservationService.java`
- `backend/src/main/java/com/dcim/platform/common/security/DataScopeService.java`

前端重点文件：
- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/alarm/AlarmCenterView.vue`
- `frontend/src/views/resource/PointMappingView.vue`
- `frontend/src/views/resource/UnmappedPointsView.vue`
- `frontend/src/views/binterface/BInterfaceRealtimeView.vue`
- `frontend/src/views/binterface/BInterfaceAlarmView.vue`
- `frontend/src/views/binterface/BInterfaceThresholdView.vue`
- `frontend/src/views/binterface/FsuStatusDetailView.vue`
- `frontend/src/views/binterface/PointCard.vue`
- `frontend/src/components/common/StatusBadge.vue`
- `frontend/src/api/bInterface.ts`
- `frontend/src/api/telemetry.ts`
- `frontend/src/api/alarm.ts`

## 3. 后端接口与映射链路结论

后端实时数据链路已调用映射服务并返回映射字段。`RealtimeDataService#list()` 先 `DataScopeService.filterByFsuScope()`，再 `toDto()`；`toDto()` 调用 `mappingService.resolveRealtime()`，并写入 `signalName`、`unit`、`valueMeaning`、`mappingStatus`、`mappingConfidence`、`templateVariant`、`needRealDataConfirm` 等字段。

告警链路同样已调用 `mappingService.resolveAlarm()`，并返回 `signalName`、`eventName`、`alarmMeaning`、`eventSeverity`、`mappingStatus`、`mappingConfidence`。

B接口只读链路：
- `/api/b-interface/realtime-points`：先 DataScope，再按 `fsuCode` 参数过滤，再调用 `applyRealtimeMapping()`。
- `/api/b-interface/alarms`：先 DataScope，再按 `fsuCode` 参数过滤，再调用 `applyAlarmMapping()`。
- `/api/b-interface/unmapped-signals`：候选映射和真实 unmapped observation 均先经过 DataScope，再按 `fsuCode` 参数过滤。

结论：本次未发现后端 DTO 漏传 `mappingStatus/signalName/unit/valueMeaning`，也未发现 `/api/b-interface/unmapped-signals?fsuCode=...` 绕过 DataScope 的复发。

## 4. 后端接口返回样本

以下样本来自代码路径与已有测试断言，不来自真实 FSU 在线访问。

实时数据候选映射样本：

```json
{
  "fsuCode": "51051243812345",
  "deviceId": "51051241820004",
  "spid": "510000250",
  "signalId": "510000250",
  "signalName": "烟感",
  "unit": "",
  "value": "1",
  "valueMeaning": "有告警",
  "signalType": "DI",
  "mappingStatus": "MAPPED_CANDIDATE",
  "mappingConfidence": "HIGH",
  "templateVariant": "BOTH",
  "needRealDataConfirm": true,
  "source": "device_signal_candidate"
}
```

实时数据字典兜底样本：

```json
{
  "signalId": "510000211",
  "signalName": "I2C温度",
  "unit": "℃",
  "value": "25.5",
  "mappingStatus": "TEMPLATE_ONLY",
  "needRealDataConfirm": true,
  "source": "signal_dictionary_fallback"
}
```

告警映射样本：

```json
{
  "deviceId": "51051241820004",
  "spid": "510000250",
  "signalName": "烟感",
  "eventId": "510000250",
  "eventName": "烟感告警",
  "alarmMeaning": "烟感告警",
  "eventSeverity": "一级告警",
  "mappingStatus": "MAPPED_CANDIDATE",
  "mappingConfidence": "HIGH"
}
```

未命中样本：

```json
{
  "signalId": "999999999",
  "mappingStatus": "UNMAPPED",
  "mappingConfidence": "UNKNOWN",
  "reason": "UNKNOWN_SIGNAL_ID",
  "source": "unmapped_observation"
}
```

## 5. mappingStatus 分布统计

静态候选映射 CSV 分布：

- `HIGH`: 4 条。
- `MEDIUM`: 6 条。
- `PENDING_REAL_DATA`: 13 条。

按当前 parser 生成的候选 `mappingStatus`：

- `MAPPED_CANDIDATE`: 4 条（HIGH）。
- `PENDING_REAL_DATA`: 19 条（MEDIUM + PENDING_REAL_DATA）。

因此，如果页面把 `PENDING_REAL_DATA`、`MAPPED_CANDIDATE` 或 `HIGH/MEDIUM` 显示成“未映射”，属于前端显示误判，不是后端真实未映射。

## 6. 页面逐项结论

| 页面 | 接口 | 结论 |
| --- | --- | --- |
| 站点实时数据 | `/api/telemetry/realtime` | 正常消费后端 `signalName/unit/valueMeaning/mappingStatus`。已统一状态工具，`UNMAPPED` 才计入未映射。 |
| 告警中心 | `/api/alarms` | 正常消费后端 `eventName/alarmMeaning/eventSeverity/mappingStatus`。已统一状态工具。 |
| 点位映射 | `/api/b-interface/unmapped-signals` | 展示 D 类假设全集候选和真实 unmapped observation；不是所有行都代表真实未映射。 |
| 未映射点位 | `/api/b-interface/unmapped-signals` | 原指标“未映射点位=总行数”会把候选模板也算作未映射，已修正为“待复核项/真实未映射/模板待确认”。 |
| 协议诊断 / B接口实时页 | `/api/b-interface/realtime-points?fsuCode=51051243812345` | 消费后端映射结果，但仍有硬编码 FSU 和电池/环境分组，属于 P1/P2 遗留，不是本次主要误判根因。 |
| FSU 详情 / 设备列表 | `/api/b-interface/fsus/{fsuCode}/devices` | 发现明确显示 bug：仅小写 `mapped` 被认为已映射，`MAPPED_CANDIDATE` 被显示成未映射。已最小修复。 |
| B接口告警增强页 | `/api/b-interface/alarms` | 发现明确旧字段 bug：用 `pointCode` 判断是否已映射，但后端 DTO 使用 `spid/signalId/mappingStatus`。已最小修复。 |
| B接口门限只读页 | `/api/b-interface/thresholds` + 设备候选 | 真实门限接口未接入，原文案“点位映射表未导入”和小写状态统计误导。已修正文案和状态展示。 |
| 首页监控驾驶舱 | 多接口聚合 | 当前不展示映射状态分布，未发现“未映射”误判；最新告警仍可后续优先展示 `eventName/alarmMeaning`。 |

## 7. 前端实际显示逻辑问题

本次确认的前端误判：

1. `FsuStatusDetailView.vue` 旧逻辑：`scope.row.mappingStatus === 'mapped' ? 'mapped' : 'unmapped'`。后端返回 `MAPPED_CANDIDATE` 时会被误判为 `unmapped`。
2. `BInterfaceAlarmView.vue` 旧逻辑：`!a.pointCode` 统计未映射，`row.pointCode ? 'mapped' : 'unmapped'` 控制 badge。后端当前返回 `spid/signalId/mappingStatus`，不保证有 `pointCode`。
3. `UnmappedPointsView.vue` 旧指标：`未映射点位 = rawData.length`，会把 D 类假设全集候选、`PENDING_REAL_DATA` 和真实 `UNMAPPED` 混成一个数量。
4. 多页面各自维护 mappingStatus 文案，缺少 `MAPPED/CONFIRMED/HIGH/MEDIUM/UNKNOWN_EVENT_ID` 兼容。

## 8. 本次最小修复

新增：
- `frontend/src/utils/mappingStatus.ts`

修改：
- `frontend/src/components/common/StatusBadge.vue`
- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/alarm/AlarmCenterView.vue`
- `frontend/src/views/resource/PointMappingView.vue`
- `frontend/src/views/resource/UnmappedPointsView.vue`
- `frontend/src/views/binterface/PointCard.vue`
- `frontend/src/views/binterface/FsuStatusDetailView.vue`
- `frontend/src/views/binterface/BInterfaceAlarmView.vue`
- `frontend/src/views/binterface/BInterfaceThresholdView.vue`

修复内容：
- 统一 `MAPPED/CONFIRMED/VERIFIED_BY_REAL_DATA/MAPPED_CANDIDATE/HIGH/MEDIUM/TEMPLATE_ONLY/PENDING_REAL_DATA/UNMAPPED/UNKNOWN_EVENT_ID` 展示口径。
- `TEMPLATE_ONLY` 显示为“模板存在，真实未确认”。
- `PENDING_REAL_DATA` 显示为“待真实数据确认”。
- `MAPPED_CANDIDATE/HIGH/MEDIUM` 显示为“候选映射”，不再归为未映射。
- `UNMAPPED/UNKNOWN_EVENT_ID/UNKNOWN_SIGNAL_ID/UNKNOWN_DEVICE_SIGNAL_PAIR` 才归入未映射类。
- B接口告警页改为消费 `spid/signalId/eventId/mappingStatus/mappingConfidence/eventName/alarmMeaning`。
- FSU 详情设备列表改为按统一 mappingStatus 显示。
- 未映射页指标拆分为“待复核项 / 真实未映射 / 模板待确认”。

## 9. 是否真实未映射

结论是“混合原因”：

- 核心实时数据、告警中心如果显示 `UNMAPPED`，与后端映射状态一致，属于真实未命中或字典待补齐。
- FSU 详情设备列表、B接口告警增强页存在前端误判，已完成最小修复。
- 未映射点位页包含 D 类假设全集候选是设计行为，不等于真实 FSU 返回未映射；本次已通过指标文案区分。

## 10. 是否旧数据遗留

本次未连接生产库查询历史记录，因此不能断言当前页面中的每条未映射是否来自 DATA-MAPPING-P0-002 前的旧数据。代码层面可见旧记录如果缺 `signalId/spid/rawId`，仍可能需要回填。

建议后续任务：`DATA-MAPPING-BACKFILL-P1-001`，对历史实时/告警记录重新跑只读映射回填，不访问真实 FSU，不改写原始报文。

## 11. unmapped / UNKNOWN_EVENT_ID 处理

`UnmappedSignalObservationService` 会按 `fsuId/deviceId/signalId/rawId/sourceCommand/reason` 聚合未映射观察，包含 `firstSeenAt/lastSeenAt/seenCount`。

注意：告警 DTO 当前没有显式返回 `reason` 字段。已知未知 EventId 会写入 unmapped observation，但告警列表行本身主要展示 `mappingStatus`。建议 P1 补充 `mappingReason` 或 `unmappedReason` 给告警 DTO，便于前端在告警行直接提示 `UNKNOWN_EVENT_ID`。

## 12. DataScope 复核

复核结果：

- `/api/telemetry/realtime`：Service 层 `filterByFsuScope()`。
- `/api/alarms`：Service 层 `filterByFsuScope()`。
- `/api/b-interface/realtime-points`：Controller 先 DataScope，后 `fsuCode` 参数过滤。
- `/api/b-interface/alarms`：Controller 先 DataScope，后 `fsuCode` 参数过滤。
- `/api/b-interface/unmapped-signals`：候选和 observation 都先 DataScope，后 `fsuCode` 参数过滤。

未发现本次反馈相关的 DataScope 绕过。既有 P1 遗留仍是内存过滤，应迁移到 Repository/SQL 层。

## 13. B接口实时页硬编码遗留

`BInterfaceRealtimeView.vue` 仍使用 `HARDCODED_REALTIME_FSU_CODE=51051243812345`，并按电池/环境分组展示。该页面已消费后端 `signalName/unit/valueMeaning/mappingStatus`，但硬编码 FSU 和分组会影响多 FSU、多站点场景下的准确性。

本次未做结构性改造，建议进入 `FE-BINTERFACE-REALTIME-P1-001`：按站点/FSU 选择器和后端设备分组驱动 B接口实时页。

## 14. 测试结果

已执行：

- `mvn -q -DskipTests compile`：通过。
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'`：186 tests，0 failures，0 errors。
- `npm run build`：通过。仅保留既有 Rollup PURE 注释、CSS `//` 注释、chunk size warning。

未执行：

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未跑全量 `mvn test`，因为本任务聚焦 mapping/realtime/alarm/DataScope 与前端 build；指定后端测试已覆盖本次诊断范围。

## 15. P0 / P1 / P2 问题清单

P0：无新增 P0。

P1：
- B接口实时页仍硬编码 FSU 和设备分组，不适合多站点/多 FSU 监控。
- 告警 DTO 缺 `mappingReason/unmappedReason`，未知 EventId 只能通过 unmapped observation 页排查。
- 历史实时/告警数据如果产生于映射导入前，可能需要只读回填映射状态。

P2：
- 首页最近告警可优先展示 `eventName/alarmMeaning`。
- mappingStatus 工具已新增，但后续可继续统一更多页面的状态筛选项。
- 未映射页可增加“只看真实未映射 / 只看模板候选 / 只看待真实确认”筛选。

## 16. 最终结论

本次确认前端确实存在显示误判：后端已返回映射字段，但部分页面仍用旧字段或小写状态判断，导致 `MAPPED_CANDIDATE/PENDING_REAL_DATA` 被显示为“未映射”。已完成最小修复。

核心结论：

> 后端已返回映射字段，但前端部分显示逻辑误判，已完成最小修复；若核心实时数据页仍显示 `UNMAPPED`，则与后端映射状态一致，应进入 REAL-DATA-OBSERVE-001 或字典补齐任务。

不得将当前结论解释为“真实 FSU 点位全部确认”。D 类模板候选仍按候选或待真实确认管理。
