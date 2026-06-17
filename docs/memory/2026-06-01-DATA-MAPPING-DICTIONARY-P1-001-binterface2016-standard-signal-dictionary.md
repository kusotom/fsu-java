# DATA-MAPPING-DICTIONARY-P1-001 B接口2016标准信号索引兼容映射

## 本次目标

在不改变 DATA-MAPPING-P0-002 eStoneII-IO 主字典口径的前提下，补充 B接口2016 标准信号索引兼容映射，使真实 FSU 历史返回的 `0407102001`、`0407107001` 不再无解释地落为普通 `UNMAPPED`。

## 执行边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改真实联调参数。
- 未新增前端硬编码。
- 未引入 StoneIII、`511600xx`、`ExtendField4`。
- 未修改权限、DataScope 或 SET 安全门。

## 读取依据

- `REAL-DATA-OBSERVE-001` audit/TODO。
- `DATA-MAPPING-P0-002`、`DATA-MAPPING-P0-VERIFY-001`、`FE-MAPPING-DIAG-001` audit。
- `docs/memory/WORKING-MEMORY.md`、`docs/memory/README.md`。
- `openspec/protocols/binterface-2016/dictionaries/standard-signal-dictionary-index.md`。
- `openspec/protocols/binterface-2016/matrices/dictionary-code-matrix.md`。
- `docs/audit/SPEC-TEST-P0-001-real-fsu-run-once.md`。

## 关键结论

- `0407102001` 已在项目内标准 2016 索引中查证为 D类机房/蓄电池组/总电压，单位 `V`。
- `0407107001` 已查证为 D类机房/蓄电池组/后半组电压，但原始单位列为空，本次保持单位待确认。
- eStoneII-IO `510000xxx` 仍是当前主字典；2016 标准信号索引只作为 fallback。
- fallback 返回 `mappingStatus=MAPPED_CANDIDATE`、`mappingConfidence=LOW`、`templateVariant/source=BINTERFACE_2016_STANDARD`、`needRealDataConfirm=true`。
- 未知 SignalId 仍进入 `UNMAPPED`，不会被标准索引通配兜底。

## 修改文件

- `backend/src/main/resources/dictionary/binterface2016/standard-signal-index.csv`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/BInterface2016StandardSignalIndexService.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIMappingService.java`
- `backend/src/test/java/com/dcim/platform/mapping/EStoneIIMappingServiceTest.java`
- `docs/audit/DATA-MAPPING-DICTIONARY-P1-001-binterface2016-standard-signal-dictionary.md`
- `docs/tasks/DATA-MAPPING-DICTIONARY-P1-001-follow-up-todo.md`
- `docs/memory/2026-06-01-DATA-MAPPING-DICTIONARY-P1-001-binterface2016-standard-signal-dictionary.md`
- `docs/memory/README.md`
- `docs/memory/WORKING-MEMORY.md`

## 测试结果

- `mvn -q -DskipTests compile`: 通过。
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'`: 通过，189 tests, 0 failures, 0 errors, 0 skipped。
- `npm run build`: 通过，保留既有 Rollup/CSS/chunk size warning。

## 遗留与下一步

- 建议执行 `DATA-MAPPING-BACKFILL-P1-001`，对历史实时/告警数据做只读/幂等映射回填。
- 建议执行 `FE-BINTERFACE-REALTIME-CLEANUP-P1-001`，清理 B接口实时页硬编码 FSU、分组和临时白名单。
- 后续若观察到新的标准 SignalId/EventId，应按项目内标准文档或只读样本证据补充字典，不得臆造单位或含义。
