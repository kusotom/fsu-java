# DATA-MAPPING-DICTIONARY-P1-VERIFY-001 B接口2016标准信号索引兼容映射复验

## 1. 复验结论

`DATA-MAPPING-DICTIONARY-P1-001` 的核心实现通过复验。B接口2016 标准信号索引兼容层未替代 eStoneII-IO 主字典，只在 eStoneII 字典和设备候选未命中后作为 fallback 使用。

复验中发现一个小范围行为问题：`0407102001` / `0407107001` 通过 fallback 可解析为 `MAPPED_CANDIDATE`，但在 GET_DATA / SEND_DATA 入库路径中，如果本地 `monitoring_point` 尚未绑定，仍可能被写入 `unmapped_signal_observation`。该问题已做最小修复，并新增回归测试。历史 observation 不在本任务中删除，仍留给 `DATA-MAPPING-BACKFILL-P1-001` 处理。

未发现新增 P0。允许进入 `DATA-MAPPING-BACKFILL-P1-001` 做历史数据只读回填。

## 2. 读取文件清单

- `docs/audit/DATA-MAPPING-DICTIONARY-P1-001-binterface2016-standard-signal-dictionary.md`
- `docs/tasks/DATA-MAPPING-DICTIONARY-P1-001-follow-up-todo.md`
- `docs/memory/2026-06-01-DATA-MAPPING-DICTIONARY-P1-001-binterface2016-standard-signal-dictionary.md`
- `docs/audit/REAL-DATA-OBSERVE-001-real-data-mapping-observation.md`
- `docs/audit/FE-MAPPING-DIAG-001-monitoring-unmapped-diagnosis.md`
- `docs/memory/WORKING-MEMORY.md`
- `docs/memory/README.md`

## 3. 代码读取范围

- `backend/src/main/resources/dictionary/binterface2016/standard-signal-index.csv`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/BInterface2016StandardSignalIndexService.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIMappingService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/service/BInterface2016GetDataService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/service/SendDataService.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/service/AlarmRecordService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/service/SendAlarmService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/safety/SetCommandSafetyGate.java`
- `backend/src/test/java/com/dcim/platform/mapping/EStoneIIMappingServiceTest.java`
- `backend/src/test/java/com/dcim/platform/binterface/BInterface2016GetDataServiceTest.java`
- `backend/src/test/java/com/dcim/platform/binterface/SendDataServiceTest.java`
- `frontend/src/utils/mappingStatus.ts`
- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/resource/UnmappedPointsView.vue`
- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/binterface/BInterfaceRealtimeView.vue`
- `frontend/src/views/binterface/PointCard.vue`
- `frontend/src/compat/realtimeSignalFilter.ts`

## 4. 0407102001 映射复验结果

`standard-signal-index.csv` 中存在：

- `0407102001`
- 名称：`总电压`
- 单位：`V`
- 来源：`BINTERFACE_2016_STANDARD`

`EStoneIIMappingServiceTest` 覆盖该样本，返回：

- `mappingStatus=MAPPED_CANDIDATE`
- `mappingConfidence=LOW`
- `templateVariant=BINTERFACE_2016_STANDARD`
- `source=BINTERFACE_2016_STANDARD`
- `needRealDataConfirm=true`

结论：`0407102001` 不再落为真正 `UNMAPPED`，但仍是待真实确认的候选映射，不宣称已完成现场确认。

## 5. 0407107001 映射复验结果

`standard-signal-index.csv` 中存在：

- `0407107001`
- 名称：`后半组电压`
- 单位：空，保留为待确认展示
- 来源：`BINTERFACE_2016_STANDARD`

测试覆盖 `value=0.0000`，后端保留原始值，不把 `0.0000` 当空值。前端 `RealtimeDataView.vue` 使用 `??` 判断当前值，单位为空时显示 `待确认`。

结论：`0407107001` 不再落为真正 `UNMAPPED`，`0.0000` 保留正常，单位仍按待确认口径展示。

## 6. eStoneII 优先级复验结果

`EStoneIIMappingService` 当前优先级为：

1. `DeviceSignalCandidate`
2. eStoneII Signal 字典
3. B接口2016 标准信号索引 fallback
4. `UNMAPPED`

测试 `shouldPreferEStoneIISignalDictionaryBeforeStandard2016Fallback` 覆盖同 ID 情况下 eStoneII 命中优先。结论：eStoneII 主字典优先级未回退，B接口2016 标准索引没有覆盖 eStoneII 已有匹配。

未发现 StoneIII / `511600xx` / `ExtendField4` 进入当前 fallback 主流程。

## 7. B接口2016 fallback 复验结果

`BInterface2016StandardSignalIndexService` 从 `dictionary/binterface2016/standard-signal-index.csv` 读取标准索引，只提供只读查询能力。`EStoneIIMappingService` 只有在前置映射均未命中时才调用该服务。

fallback 命中时返回候选映射语义：

- `mappingStatus=MAPPED_CANDIDATE`
- `mappingConfidence=LOW`
- `templateVariant=BINTERFACE_2016_STANDARD`
- `source=BINTERFACE_2016_STANDARD`
- `needRealDataConfirm=true`

该口径符合 P1 字典兼容补强要求。

## 8. MAPPED_CANDIDATE 前端兼容结果

`frontend/src/utils/mappingStatus.ts` 已包含：

- `MAPPED_CANDIDATE` 文案：`候选映射`
- `MAPPED_CANDIDATE` 纳入 `isMappedLikeStatus`
- `isUnmappedMappingStatus` 只把 `UNMAPPED` 和 `UNKNOWN_*` 归为真正未映射

`RealtimeDataView.vue` 映射筛选包含 `MAPPED_CANDIDATE`，未映射统计使用 `isUnmappedMappingStatus`。`UnmappedPointsView.vue` 的真实未映射数量也使用同一判断。

结论：`MAPPED_CANDIDATE` 不会被前端误显示或统计为真正未映射。

## 9. LOW 置信度展示结果

`mappingConfidenceTagType` 对 `HIGH` / `MEDIUM` / `PENDING_REAL_DATA` 等有显式样式，`LOW` 当前降级为 `info` 样式并显示原始文本 `LOW`。这是可接受降级展示，不影响状态语义。

## 10. BINTERFACE_2016_STANDARD 来源展示结果

后端 DTO 已携带 `source` / `templateVariant`。本次复验增加最小前端展示：

- `RealtimeDataView.vue` 增加 `来源` 列，优先展示 `source`，其次展示 `templateVariant`
- `PointCard.vue` 增加来源展示

结论：`BINTERFACE_2016_STANDARD` 不会被误认为 eStoneII 模板来源。

## 11. unmapped observation 行为复验

复验发现并已修复：

- `BInterface2016GetDataService.recordUnmapped(...)`：fallback 命中且 `reason=null` 时不再写入 observation
- `SendDataService.recordUnmapped(...)`：同样增加映射判定，候选映射不再写入 observation

新增测试：

- `BInterface2016GetDataServiceTest.shouldNotRecordObservationWhenSignalIsMappedCandidateButPointNotBound`
- `SendDataServiceTest.shouldNotRecordObservationWhenStandard2016SignalIsMappedCandidateButPointNotBound`

注意：GET_DATA / SEND_DATA 局部处理结果仍可能把“未绑定本地 monitoring_point”的项放入当前命令结果的 `unmapped` 列表，这是点位绑定状态；但不再持久化为真正字典未识别 observation。

未知 SignalId 仍进入 `UNMAPPED`；历史 observation 未删除；`UNKNOWN_EVENT_ID` 行为未回退。

## 12. DataScope 复验

复验重点历史风险点：

- `/api/b-interface/fsus/{fsuCode}/devices`
- `/api/b-interface/fsus/{fsuCode}/login-info`
- `/api/b-interface/unmapped-signals?fsuCode=...`

当前实现仍先按 DataScope 得到授权 FSU 集合，再叠加 `fsuCode` 参数过滤。`mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'` 中 DataScope 相关测试通过。

结论：DataScope 未发现回退。

## 13. SET 安全复验

未访问真实 FSU，未启动 Scheduler，未发起任何 SET 调用。`SetCommandSafetyGate` 仍默认拦截高风险写操作，Controls 仍仅作为禁用参考。

`mvn test -Dtest='*SetCommandSafety*Test,*SetCommandSafetyGateTest'` 通过 39 个测试，确认安全门未回退。

## 14. 前端硬编码检查

执行 `rg -n "0407102001|0407107001|总电压|后半组电压" frontend/src -S` 未发现前端硬编码。

本次删除 `frontend/src/compat/realtimeSignalFilter.ts` 中未使用的旧 SignalID 白名单，避免 `0407102001` / `0407107001` 在前端兼容层残留。

仍存在已知 P1：`BInterfaceRealtimeView` 保留默认 FSU / 分组相关兼容逻辑，应由 `FE-BINTERFACE-REALTIME-CLEANUP-P1-001` 清理。该问题不影响本次标准信号名称、单位和值含义的后端映射链路。

## 15. 测试结果

- `mvn -q -DskipTests compile`：通过
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'`：通过，189 tests，0 failures，0 errors，0 skipped
- `mvn test -Dtest='BInterface2016GetDataServiceTest,SendDataServiceTest'`：通过，34 tests，0 failures，0 errors，0 skipped
- `mvn test -Dtest='*SetCommandSafety*Test,*SetCommandSafetyGateTest'`：通过，39 tests，0 failures，0 errors，0 skipped
- `cd frontend && npm run build`：通过

前端 build 存在既有 warning：

- `@vueuse/core` 的 Rollup pure annotation warning
- CSS 中 `//` 注释 warning
- chunk size warning

这些 warning 与本次 B接口2016 标准信号 fallback 复验无关。

## 16. P0 / P1 / P2 问题清单

### P0

无新增 P0。

### P1

- `DATA-MAPPING-BACKFILL-P1-001`：历史 realtime / alarm / unmapped observation 只读回填，避免旧数据继续按旧状态展示。
- `FE-BINTERFACE-REALTIME-CLEANUP-P1-001`：清理 B接口实时页默认 FSU / 分组兼容逻辑，统一走后端返回字段。

### P2

- `LOW` 置信度可增加更明确的前端中文说明，例如“低置信候选”。
- 标准2016 fallback 字典可继续扩展，但必须保留 eStoneII 主字典优先级。

## 17. 是否允许进入 DATA-MAPPING-BACKFILL-P1-001

允许。当前标准2016 fallback 对新解析链路生效，候选映射不会再作为真正未映射 observation 写入。历史记录仍未回填，下一步应进入 `DATA-MAPPING-BACKFILL-P1-001` 做只读回填。

## 18. 最终结论

DATA-MAPPING-DICTIONARY-P1-VERIFY-001 复验通过，B接口2016 标准信号索引兼容映射未发现 P0，可进入 DATA-MAPPING-BACKFILL-P1-001 做历史数据只读回填。
