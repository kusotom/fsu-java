# REAL-DATA-OBSERVE-001 真实数据映射状态只读观察

## 1. 任务背景

本任务在 `DATA-MAPPING-P0-002`、`DATA-MAPPING-P0-VERIFY-001`、`FE-MAPPING-DIAG-001` 之后执行，只读观察当前数据库、日志、样本和代码，确认实时数据、告警数据、未映射 observation 的映射状态分布。

本次没有访问真实 FSU，没有执行 SET，没有启用 Scheduler，没有修改真实联调参数。

## 2. 执行边界

- 允许: 读取 PostgreSQL 当前库、读取文档、读取代码、执行编译和测试。
- 禁止: 真实 FSU 请求、SET、Scheduler、业务代码大改、把 `TEMPLATE_ONLY/PENDING_REAL_DATA` 强行改成已确认。
- 本次业务代码修改: 无。
- 本次文档修改: 本报告、TODO、memory 和 memory 索引。

## 3. 读取文件清单

- `docs/audit/DATA-MAPPING-P0-002-estoneii-dictionary-import-and-mapping.md`
- `docs/audit/DATA-MAPPING-P0-VERIFY-001-estoneii-mapping-review.md`
- `docs/audit/FE-MAPPING-DIAG-001-monitoring-unmapped-diagnosis.md`
- `docs/tasks/DATA-MAPPING-P0-VERIFY-001-follow-up-todo.md`
- `docs/tasks/FE-MAPPING-DIAG-001-follow-up-todo.md`
- `docs/memory/WORKING-MEMORY.md`
- `docs/memory/README.md`
- `openspec/protocols/binterface-2016/dictionaries/standard-signal-dictionary-index.md`
- `openspec/protocols/binterface-2016/profiles/emerson-2016.md`
- `openspec/protocols/binterface-2016/matrices/field-mapping-matrix.md`

## 4. 读取代码范围

- `backend/src/main/java/com/dcim/platform/module/telemetry/service/RealtimeDataService.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/service/AlarmRecordService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/dto/BInterfaceFrontendDtos.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIMappingService.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/UnmappedSignalObservationService.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/entity/UnmappedSignalObservationEntity.java`
- `backend/src/main/java/com/dcim/platform/common/security/DataScopeService.java`
- `frontend/src/utils/mappingStatus.ts`
- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/alarm/AlarmCenterView.vue`
- `frontend/src/views/resource/PointMappingView.vue`
- `frontend/src/views/resource/UnmappedPointsView.vue`
- `frontend/src/views/binterface/BInterfaceRealtimeView.vue`
- `frontend/src/compat/realtimeSignalFilter.ts`

## 5. 数据源说明

当前只读数据库为 `dcim_platform`，容器 `dcim-postgres`，用户 `dcim`。观察时间为 `2026-06-01 14:27 CST` 左右。

当前库表数量观察:

| 表 | 数量 |
|---|---:|
| `fsu_device` | 2 |
| `monitoring_point` | 15 |
| `realtime_data` | 7 |
| `alarm_record` | 1 |
| `b_interface_message_log` | 4695 |
| `b_interface_fsu_status` | 2 |

关键数据库事实:

- 当前 dev 库没有 `estoneii_signal_dictionary`、`estoneii_event_dictionary`、`estoneii_control_reference`、`device_signal_candidate`、`unmapped_signal_observation` 表。
- 因此当前库不能直接统计 eStoneII 字典表或 observation 表内容。
- 源码中存在 eStoneII 字典实体、导入逻辑和 observation 实体；当前 DB 缺表更像是部署/启动状态未应用到 dev 库，或当前业务数据早于 DATA-MAPPING-P0-002 生成。

## 6. 实时数据 mappingStatus 分布

按当前源码映射逻辑，`RealtimeDataService` 使用 `point_code` 同时作为 `spid/signalId` 调用 `EStoneIIMappingService.resolveRealtime(fsuCode, null, null, pointCode, pointCode, value)`。由于当前 `realtime_data` 没有 DeviceID，设备级候选映射不会命中；只有 eStoneII `510000xxx` Signal 字典兜底会返回 `TEMPLATE_ONLY`。

当前库实时数据没有 `510000xxx` 点位，因此按 eStoneII 主线推断 API 映射状态为:

| 分类 | 数量 | 说明 |
|---|---:|---|
| 已映射 `MAPPED/CONFIRMED` | 0 | 无真实确认映射 |
| 候选映射 `HIGH/MEDIUM/MAPPED_CANDIDATE` | 0 | 实时表缺 DeviceID，且无 `510000xxx` |
| 模板存在 `TEMPLATE_ONLY` | 0 | 当前 point_code 不在 eStoneII Signal 字典 |
| 待真实确认 `PENDING_REAL_DATA` | 0 | 当前实时表无 candidate 命中 |
| 真正未映射 `UNMAPPED` | 7 | 2 条真实 FSU 标准 2016 点位 + 5 条 legacy/demo 点位 |

按 `fsuCode` 分布:

| FSU | 代码桶 | 数量 |
|---|---|---:|
| `51051243812345` | `STANDARD_2016_INDEXED_NOT_ESTONEII` | 2 |
| `FSU-001` | `LEGACY_OR_DEMO_CODE` | 5 |

当前实时点位样本:

| FSU | point_code | 值 | 当前库 point_name | 单位 | 判断 |
|---|---|---:|---|---|---|
| `51051243812345` | `0407102001` | `54.2000` | 总电压 | V | B接口2016标准字典已索引，但不在 eStoneII `510000xxx` 主线 |
| `51051243812345` | `0407107001` | `0.0000` | 后半组电压 | 空 | B接口2016标准字典已索引，单位待确认，不在 eStoneII 主线 |
| `FSU-001` | `TEMP-001` | `25.5000` | 机柜温度 | °C | legacy/demo |
| `FSU-001` | `HUMI-001` | `55.0000` | 机柜湿度 | %RH | legacy/demo |
| `FSU-001` | `VOLT-001` | `220.5000` | 交流电压A相 | V | legacy/demo |
| `FSU-001` | `DOOR-001` | `CLOSE` | 柜门状态 | 空 | legacy/demo |
| `FSU-001` | `WATER-001` | `DRY` | 水浸传感器 | 空 | legacy/demo |

## 7. 告警数据 mappingStatus 分布

当前 `alarm_record` 只有 1 条，属于 `FSU-001` legacy/demo 数据:

| FSU | point_code | alarm_code | alarm_name | status | 判断 |
|---|---|---|---|---|---|
| `FSU-001` | `TEMP-001` | `TEMP-HIGH` | 机柜温度过高 | `CLEARED` | eStoneII Event 字典无法命中，按源码为 `UNMAPPED` + `UNKNOWN_EVENT_ID` |

告警统计:

| 分类 | 数量 |
|---|---:|
| 告警总数 | 1 |
| 已映射告警 | 0 |
| `UNKNOWN_EVENT_ID` | 1 |
| `UNMAPPED` | 1 |

当前库没有 `51051243812345` 的真实 SEND_ALARM 告警记录。

## 8. unmapped observation 分布

当前 dev 库不存在 `unmapped_signal_observation` 表，因此无法统计 observation 总数、Top 20、最近出现等数据库内容。

源码链路结论:

- `BInterface2016GetDataService` 在未命中映射时调用 `UnmappedSignalObservationService.record(...)`。
- `SendAlarmService` 在 `!mapped.mapped()` 或 `mapped.reason()!=null` 时记录 unmapped alarm。
- `UnmappedSignalObservationService` 使用 `fsuId/deviceId/effectiveSignalId/rawId/sourceCommand/reason` 聚合更新，不是无限插入同类重复记录。

当前 DB 缺表意味着当前库尚未承载 DATA-MAPPING-P0-002 的 observation 持久化结果。原因更可能是历史数据早于该任务，或当前 dev 库未由新版本应用启动生成 schema。

## 9. UNKNOWN_EVENT_ID 样本

| FSU | deviceId | spid | signalId | eventId/alarmCode | 出现次数 | 判断 |
|---|---|---|---|---|---:|---|
| `FSU-001` | 空 | 空 | `TEMP-001` | `TEMP-HIGH` | 1 | legacy/demo 告警码，不属于 eStoneII Event |

当前未观察到真实 FSU `51051243812345` 的 `UNKNOWN_EVENT_ID`。

## 10. UNMAPPED 样本

| 来源 | FSU | rawId/signalId | 原因 |
|---|---|---|---|
| `realtime_data` | `51051243812345` | `0407102001` | 真实 FSU 标准 2016 点位，已在 openspec 标准信号字典索引中，但不在 eStoneII `510000xxx` 字典 |
| `realtime_data` | `51051243812345` | `0407107001` | 同上，且单位按标准索引为“单位待确认” |
| `realtime_data` | `FSU-001` | `TEMP-001/HUMI-001/VOLT-001/DOOR-001/WATER-001` | legacy/demo 数据 |
| `alarm_record` | `FSU-001` | `TEMP-HIGH` | legacy/demo 告警码 |

结论: 当前页面剩余“未映射”如果来自核心实时数据页，主要是后端真实返回 `UNMAPPED`，不是 FE-MAPPING-DIAG-001 已修复的前端误判。

## 11. TEMPLATE_ONLY / PENDING_REAL_DATA 样本

当前数据库实时/告警表没有 `TEMPLATE_ONLY` 或 `PENDING_REAL_DATA` 样本。

eStoneII 候选资源存在:

- `device-signal-candidates.csv`: 23 条候选，`HIGH=4`、`MEDIUM=6`、`PENDING_REAL_DATA=13`。
- WITH_MIDPOINT/NO_MIDPOINT 模板仍是 D 类假设全集候选，不代表 FSU 已真实返回全集。

这些候选不应计入真正未映射。前端 `mappingStatus.ts` 已区分:

- `TEMPLATE_ONLY`: 模板存在，真实未确认。
- `PENDING_REAL_DATA`: 待真实数据确认。
- `UNMAPPED`: 未映射。

## 12. 字典覆盖分析

当前 DATA-MAPPING-P0-002 的 eStoneII 字典主线只覆盖 `510000xxx` SignalId。当前 dev 库真实 FSU 记录出现的是:

- `0407102001`: 标准 B接口2016 D类机房/蓄电池组/总电压，单位 V；见 `openspec/protocols/binterface-2016/dictionaries/standard-signal-dictionary-index.md`。
- `0407107001`: 标准 B接口2016 D类机房/蓄电池组/后半组电压，单位待确认；不得凭“电压”二字补造 V。

因此当前缺口不是前端显示问题，而是“真实 FSU 返回的标准 2016 `0407/0418` 系列 SignalID 未进入当前 eStoneII 映射服务主字典”。建议进入 `DATA-MAPPING-DICTIONARY-P1-001`，补充标准 2016 字典与 eStoneII `510000xxx` 字典的关系边界。

## 13. 历史数据影响判断

存在历史/legacy 数据影响:

- `FSU-001` 的 5 条实时数据和 1 条告警明显是 demo/legacy 编码，不代表真实 FSU。
- `51051243812345` 的 2 条实时数据来自 2026-05-27 真实只读样本链路，发生在 DATA-MAPPING-P0-002 之前。
- 当前 `unmapped_signal_observation` 表缺失，说明这些历史数据没有 observation 回填。

因此需要 `DATA-MAPPING-BACKFILL-P1-001`，但回填必须先明确字典策略: 标准 2016 `0407/0418` 系列应按标准字典映射，不能强行改成 eStoneII `510000xxx` 已确认点位。

## 14. 前端误判是否仍存在

未发现新的前端误判:

- `frontend/src/utils/mappingStatus.ts` 已区分 `MAPPED/CONFIRMED/HIGH/MEDIUM/TEMPLATE_ONLY/PENDING_REAL_DATA/UNMAPPED/UNKNOWN_EVENT_ID`。
- `RealtimeDataView.vue` 的 `0.0000` 通过 `??` 与空值判断展示，不会把 0.0 当空。
- `UnmappedPointsView.vue` 的“真实未映射”仅统计 `UNMAPPED/UNKNOWN_*`，不会把 `TEMPLATE_ONLY/PENDING_REAL_DATA` 计入真实未映射。

仍存在 P1 展示遗留:

- `BInterfaceRealtimeView.vue` 标题、默认 FSU 和分组仍硬编码 `51051243812345`、蓄电池组/环境分组。
- `frontend/src/compat/realtimeSignalFilter.ts` 保留 8 个 `0407/0418` 白名单，文件已标注临时兼容。

## 15. DataScope 是否仍安全

源码复核:

- `RealtimeDataService#list/getById` 先用 `DataScopeService.filterByFsuScope` 过滤，再 DTO 映射。
- `AlarmRecordService#list/getById/listByStatus` 先过滤 DataScope，再映射。
- `BInterfaceFrontendReadController#getRealtimePoints` 先 DataScope 过滤，再按 `fsuCode` 参数收敛。
- `BInterfaceFrontendReadController#getUnmappedSignals` 对 candidates 和 observations 均先 DataScope 过滤，再按 `fsuCode` 参数收敛。
- `DataScopeIntegrationTest` 在本次测试集中通过。

本次直接 SQL 读取绕过应用权限，仅用于审计统计，不代表用户可见范围。

## 16. 是否发现 P0

未发现新增 P0。

需要注意的非 P0 风险:

- 当前 dev DB 尚未出现 eStoneII 字典表和 observation 表。如果当前运行后端也连接该库且未重启到新代码，前端会看不到 DATA-MAPPING-P0-002 的完整运行态效果。这属于部署/schema 应用与历史数据回填问题，建议按 P1 处理。

## 17. 测试结果

执行命令:

```bash
cd /home/tom/桌面/FSU/fsu-platform-java/backend
mvn -q -DskipTests compile
mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'

cd /home/tom/桌面/FSU/fsu-platform-java/frontend
npm run build
```

结果:

- `mvn -q -DskipTests compile`: PASS。
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'`: PASS，`Tests run: 186, Failures: 0, Errors: 0, Skipped: 0`。
- `npm run build`: PASS。

既有 warning:

- Maven 测试中有用例模拟网络/DB异常并打印 stack trace，但测试结果为 0 failure/0 error。
- Vite/Rollup 有既有 `#__PURE__` 注释 warning、CSS `//` 注释 warning、chunk size warning。

## 18. P0/P1/P2 问题清单

P0: 无。

P1:

- 真实 FSU 当前出现 `0407102001/0407107001`，未进入 eStoneII `510000xxx` 主映射字典，核心实时数据会按后端真实 `UNMAPPED` 展示。
- 当前 dev DB 缺少 eStoneII 字典表和 `unmapped_signal_observation` 表，需要确认部署/启动/schema 应用状态。
- 历史 realtime/alarm 数据未回填 mappingStatus 和 observation。
- B接口实时页仍有硬编码 FSU/分组/白名单兼容逻辑。

P2:

- 当前数据库 `realtime_data` 缺 DeviceID，导致设备级候选映射无法命中，只能依赖 SignalId 字典兜底。
- `b_interface_message_log` 当前库未观察到 GET_DATA/SEND_ALARM 结构化样本，现有 4695 条主要为 LOGIN/UNKNOWN，后续需要被动观察新的真实上报或只读样本入库。

## 19. 后续任务建议

建议进入:

- `DATA-MAPPING-DICTIONARY-P1-001`: 补充真实 FSU 返回的标准 2016 `0407/0418` 系列 SignalId/EventId/rawId 字典，并定义与 eStoneII `510000xxx` 字典的边界和优先级。
- `DATA-MAPPING-BACKFILL-P1-001`: 在字典策略明确后，对历史 `realtime_data/alarm_record` 做映射状态回填和 observation 补录。
- `REAL-DATA-PASSIVE-OBSERVE-P1-001`: 不启 Scheduler、不执行 SET，仅被动观察新的真实 FSU 上报/日志入库情况。
- `FE-BINTERFACE-REALTIME-CLEANUP-P1-001`: 清理 B接口实时页硬编码 FSU、分组和白名单兼容逻辑。

## 20. 最终结论

REAL-DATA-OBSERVE-001 完成。当前剩余未映射状态已完成只读观察和分类，未发现新增 P0。后续应根据样本分布进入字典补齐、历史回填或被动观察任务。

当前剩余 `UNMAPPED / UNKNOWN_EVENT_ID` 来自后端真实映射未命中，不属于 FE-MAPPING-DIAG-001 已修复的前端误判范围。

当前部分未映射来自历史数据未回填，不代表 DATA-MAPPING-P0-002 当前链路失效。

不得把本次只读观察扩大解释为现场点位全量确认、生产可用、三方生产开放或 SET 可用。
