# DATA-MAPPING-DICTIONARY-P1-001 后续 TODO

## P0

无新增 P0。

## P1

### DATA-MAPPING-BACKFILL-P1-001

- 问题描述: 本次已补充 B接口2016 标准信号索引运行时 fallback，但历史 `realtime_data/alarm_record` 可能仍缺少统一映射状态、source 和 observation 聚合结果。
- 影响范围: 核心实时数据页、历史告警、未映射统计、协议排障报表。
- 建议修改文件: `backend/src/main/java/com/dcim/platform/module/mapping/**`、`backend/src/test/java/com/dcim/platform/mapping/**`，如提供管理 API 需放在受权限保护的系统/诊断端点。
- 验收标准: 只读/幂等回填；不访问真实 FSU；不执行 SET；`0407102001` 回填为 B接口2016 标准候选映射，`0407107001` 保留单位待确认；未知 SignalId 仍进入 unmapped observation。
- 是否涉及后端: 是。
- 是否涉及权限: 是，如提供 API 必须管理员或协议调试权限，并写操作审计。
- 是否涉及协议安全边界: 是，必须只处理本地历史数据。

### FE-BINTERFACE-REALTIME-CLEANUP-P1-001

- 问题描述: B接口实时页仍保留硬编码 FSU、分组和临时 SignalID 白名单兼容逻辑，容易让用户误解映射来源。
- 影响范围: B接口实时页、现场排障展示、未映射/候选/模板状态理解。
- 建议修改文件: `frontend/src/views/binterface/BInterfaceRealtimeView.vue`、`frontend/src/compat/realtimeSignalFilter.ts`、`frontend/src/api/bInterface.ts`。
- 验收标准: 页面按后端返回的 `fsuCode/deviceId/signalName/unit/valueMeaning/mappingStatus/mappingConfidence/source` 动态展示；删除临时白名单；不硬编码名称、单位、0/1 含义；不触发 GET_DATA run-once。
- 是否涉及后端: 可能需要补充 `source/deviceId` 字段。
- 是否涉及权限: 否。
- 是否涉及协议安全边界: 是，不能新增 SET 或主动轮询入口。

### DATA-MAPPING-DICTIONARY-P1-002

- 问题描述: 当前标准 2016 兼容资源仅收录 `0407102001/0407107001` 两条已查证且已出现在历史真实数据中的点位；后续若观察到新的标准 SignalId/EventId，需要按证据补齐。
- 影响范围: 实时数据、告警映射、未知事件统计。
- 建议修改文件: `backend/src/main/resources/dictionary/binterface2016/standard-signal-index.csv`、`backend/src/main/java/com/dcim/platform/module/mapping/service/BInterface2016StandardSignalIndexService.java`、对应测试。
- 验收标准: 每条新增字典项都能追溯到项目内协议文档、标准索引或真实只读样本；单位不明确时保持待确认；不得引入 StoneIII、`511600xx`、`ExtendField4`。
- 是否涉及后端: 是。
- 是否涉及权限: 否。
- 是否涉及协议安全边界: 是，只补字典展示，不新增控制命令。

## P2

### REALTIME-DEVICEID-PERSIST-P2-001

- 问题描述: 当前 `realtime_data` 缺 DeviceID 时，设备级候选映射无法命中，只能按 SignalId 字典或标准索引兜底。
- 影响范围: DeviceID + SignalId 精确候选映射、未映射收敛、设备级统计。
- 建议修改文件: `backend/src/main/java/com/dcim/platform/module/telemetry/**`、`backend/src/main/java/com/dcim/platform/module/binterface/service/SendDataService.java`、migration/schema 文件。
- 验收标准: 入库链路保存 `deviceId/deviceCode/sourceMessageId`；历史数据兼容；DataScope 不回退。
- 是否涉及后端: 是。
- 是否涉及权限: 否。
- 是否涉及协议安全边界: 否，但不得访问真实 FSU 或启 Scheduler。

## P3

- 可增加标准 2016 字典索引只读查看页，仅面向协议调试员展示 source、单位确认状态和最后命中样本。
