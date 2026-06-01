# DATA-MAPPING-DICTIONARY-P1-001 B接口2016标准信号索引兼容映射

## 1. 任务背景

`REAL-DATA-OBSERVE-001` 只读观察确认，当前真实 FSU `51051243812345` 的历史实时数据中存在两条标准 2016 信号索引:

| FSU | SignalId/SPID/rawId | 值 | 观察结论 |
|---|---|---:|---|
| `51051243812345` | `0407102001` | `54.2000` | B接口2016 标准信号索引，未在 eStoneII-IO `510000xxx` 主字典中 |
| `51051243812345` | `0407107001` | `0.0000` | B接口2016 标准信号索引，未在 eStoneII-IO `510000xxx` 主字典中 |

本任务目标是在不替代 DATA-MAPPING-P0-002 eStoneII-IO 主字典的前提下，增加 B接口2016 标准信号索引兼容层，使这两个历史真实点位进入待确认/候选映射链路，不再无解释地落为普通 `UNMAPPED`。

## 2. 执行边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改真实联调参数。
- 未新增前端硬编码 SignalName、unit 或 0/1 含义。
- 未引入 StoneIII、`511600xx`、`ExtendField4` 作为当前主逻辑。
- 未放松 DataScope、权限或 SET 安全门。

## 3. 读取文件清单

- `docs/audit/REAL-DATA-OBSERVE-001-real-data-mapping-observation.md`
- `docs/tasks/REAL-DATA-OBSERVE-001-follow-up-todo.md`
- `docs/audit/DATA-MAPPING-P0-002-estoneii-dictionary-import-and-mapping.md`
- `docs/audit/DATA-MAPPING-P0-VERIFY-001-estoneii-mapping-review.md`
- `docs/audit/FE-MAPPING-DIAG-001-monitoring-unmapped-diagnosis.md`
- `docs/memory/WORKING-MEMORY.md`
- `docs/memory/README.md`
- `openspec/protocols/binterface-2016/dictionaries/standard-signal-dictionary-index.md`
- `openspec/protocols/binterface-2016/matrices/dictionary-code-matrix.md`
- `docs/audit/SPEC-TEST-P0-001-real-fsu-run-once.md`

## 4. 读取代码范围

- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIMappingService.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIMappingResult.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIDictionaryImportService.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIITemplateDictionaryParser.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/service/RealtimeDataService.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/service/AlarmRecordService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/test/java/com/dcim/platform/mapping/EStoneIIMappingServiceTest.java`
- `frontend/src/utils/mappingStatus.ts`
- `frontend/src/views/telemetry/RealtimeDataView.vue`

## 5. `0407102001` 标准含义查证结果

查证依据:

- `openspec/protocols/binterface-2016/dictionaries/standard-signal-dictionary-index.md`: `SPEC-2016-DICT-SIGNAL-0407102001`，SignalId `0407102001`，名称 `总电压`，设备逻辑 `07 蓄电池组`，类型 `遥测 AI`，单位 `V`。
- `openspec/protocols/binterface-2016/matrices/dictionary-code-matrix.md`: `SPEC-2016-DICT-XLSX-SIGNAL-0256`，D类机房/蓄电池组/总电压，单位 `V`，信号解释为电池总电压。
- `docs/audit/SPEC-TEST-P0-001-real-fsu-run-once.md`: 2026-05-27 只读样本中该点返回 `53.9`，未执行 SET、未启 Scheduler。

实现口径:

- `signalName = 总电压`
- `unit = V`
- `signalCategory = AI/模拟量`
- `signalType = AI`
- `source = BINTERFACE_2016_STANDARD`
- `mappingStatus = MAPPED_CANDIDATE`
- `mappingConfidence = LOW`
- `needRealDataConfirm = true`

## 6. `0407107001` 标准含义查证结果

查证依据:

- `openspec/protocols/binterface-2016/dictionaries/standard-signal-dictionary-index.md`: `SPEC-2016-DICT-SIGNAL-0407107001`，SignalId `0407107001`，名称 `后半组电压`，设备逻辑 `07 蓄电池组`，类型 `遥测 AI`，单位列为“单位待确认”。
- `openspec/protocols/binterface-2016/matrices/dictionary-code-matrix.md`: `SPEC-2016-DICT-XLSX-SIGNAL-0268`，D类机房/蓄电池组/后半组电压，Excel 行 70 原始单位未给出。
- `docs/audit/SPEC-TEST-P0-001-real-fsu-run-once.md`: 只读样本中该点返回 `0.0`，报告明确单位未在字典行给出。

实现口径:

- `signalName = 后半组电压`
- `unit = ""`，前端现有实时数据页会按空单位显示“待确认”
- `signalCategory = AI/模拟量`
- `signalType = AI`
- `source = BINTERFACE_2016_STANDARD`
- `mappingStatus = MAPPED_CANDIDATE`
- `mappingConfidence = LOW`
- `needRealDataConfirm = true`

## 7. 字典兼容方案

新增轻量资源:

- `backend/src/main/resources/dictionary/binterface2016/standard-signal-index.csv`

当前只纳入本次有可靠项目内依据的两条标准索引:

| SignalId | SignalName | Unit | Source |
|---|---|---|---|
| `0407102001` | 总电压 | `V` | B接口2016 standard signal index |
| `0407107001` | 后半组电压 | 空，单位待确认 | B接口2016 standard signal index |

新增服务:

- `BInterface2016StandardSignalIndexService`
- 从 classpath CSV 延迟加载标准索引。
- 暴露 `findBySignalId(String signalId)`。
- 固定 source 标识为 `BINTERFACE_2016_STANDARD`。

映射服务调整:

- `EStoneIIMappingService.resolveRealtime(...)` 在 eStoneII 主字典与候选映射均未命中后，进入 2016 标准索引 fallback。
- `EStoneIIMappingService.resolveAlarm(...)` 在 eStoneII Signal/Event 均未命中后，也可用标准 SignalId 作为信号侧候选映射；如果显式 EventId 未命中，仍保留 `reason = UNKNOWN_EVENT_ID`。

## 8. 映射优先级说明

最终优先级:

1. eStoneII-IO DeviceSignalCandidate 精确候选。
2. eStoneII-IO Signal 字典兜底。
3. B接口2016 标准信号索引兼容命中。
4. `unmapped_observation`。

关键边界:

- B接口2016 fallback 不覆盖 eStoneII 精确命中。
- B接口2016 fallback 不把标准索引改写成 eStoneII `510000xxx`。
- `0407102001/0407107001` 返回候选映射和低置信度，保留真实数据确认字段。
- 未知 SignalId 仍返回 `UNMAPPED`，不会被通配兜底。

## 9. 修改文件清单

- `backend/src/main/resources/dictionary/binterface2016/standard-signal-index.csv`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/BInterface2016StandardSignalIndexService.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIMappingService.java`
- `backend/src/test/java/com/dcim/platform/mapping/EStoneIIMappingServiceTest.java`
- `docs/audit/DATA-MAPPING-DICTIONARY-P1-001-binterface2016-standard-signal-dictionary.md`
- `docs/tasks/DATA-MAPPING-DICTIONARY-P1-001-follow-up-todo.md`
- `docs/memory/2026-06-01-DATA-MAPPING-DICTIONARY-P1-001-binterface2016-standard-signal-dictionary.md`
- `docs/memory/README.md`
- `docs/memory/WORKING-MEMORY.md`

未修改前端业务代码。

## 10. 新增或修改测试清单

修改 `EStoneIIMappingServiceTest`，新增/补强:

- `resolvesRealtimeByBInterface2016StandardSignalIndexFallback`
  - 验证 `0407102001` 返回 `总电压/V`、`MAPPED_CANDIDATE`、`LOW`、`BINTERFACE_2016_STANDARD`。
- `preservesZeroValueAndUnknownUnitForBInterface2016StandardFallback`
  - 验证 `0407107001` 返回 `后半组电压`，单位保持空，`0.0000` 不丢失。
- `eStoneIISignalDictionaryKeepsPriorityOverBInterface2016Fallback`
  - 验证 eStoneII Signal 字典命中时不会被 2016 fallback 覆盖。
- 既有 `unresolvedRealtimeSignalBecomesUnmapped`
  - 继续验证未知 SignalId 仍进入 `UNMAPPED`。

## 11. 验证结果

执行命令:

```bash
mvn -q -DskipTests compile
```

结果: 通过。

```bash
mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'
```

结果: 通过，`189 tests, 0 failures, 0 errors, 0 skipped`。

```bash
cd frontend
npm run build
```

结果: 通过。保留既有 Rollup pure annotation、CSS `//` 注释、chunk size warning，未出现新增构建失败。

## 12. 是否新增 P0

未发现新增 P0。

安全边界复核:

- 未新增 SET/Controls 入口。
- 未修改 SetCommandSafetyGate。
- 未访问真实 FSU。
- 未启 Scheduler。
- 未放松 raw XML、run-once 或 DataScope 权限。
- B接口2016 fallback 只用于字典展示候选，不改变采集命令链路。

## 13. P1/P2 遗留

P1:

- 历史 `realtime_data/alarm_record` 仍需执行只读/幂等映射回填，否则旧数据可能仍保留旧展示或缺少 observation 聚合。
- B接口实时页仍有硬编码 FSU/分组/白名单兼容逻辑，应进入 `FE-BINTERFACE-REALTIME-CLEANUP-P1-001`。
- 标准 2016 字典兼容层当前只纳入本次已查证的两条真实历史点位，后续如出现新的标准 SignalId/EventId，应按同一证据口径扩展。

P2:

- `realtime_data` 当前缺 DeviceID 时，设备级候选映射仍无法命中；后续可在入库链路保存 `deviceId/deviceCode/sourceMessageId`。
- 可增加只读映射状态统计 API，减少后续人工 SQL 观察成本。

## 14. 是否建议进入历史回填

建议进入 `DATA-MAPPING-BACKFILL-P1-001`。

理由:

- 本次补充的是运行时映射 fallback，已解决两个标准 2016 历史点位无解释落为普通 `UNMAPPED` 的问题。
- 既有历史实时/告警记录如果已经被旧链路统计或写入 observation，仍需要只读/幂等回填来统一历史映射状态。
- 回填必须只处理本地历史数据，不触发真实 FSU 请求，不执行 SET。

## 15. 最终结论

DATA-MAPPING-DICTIONARY-P1-001 完成。B接口2016 标准信号索引兼容映射已补充，真实历史点位 0407102001 / 0407107001 可进入待确认或候选映射链路。建议下一步执行 DATA-MAPPING-BACKFILL-P1-001。
