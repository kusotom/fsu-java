# DATA-MAPPING-DICTIONARY-P1-VERIFY-001 后续 TODO

## P0

无新增 P0。

## P1

### DATA-MAPPING-BACKFILL-P1-001

- 问题描述：历史 realtime / alarm / unmapped observation 仍可能保留旧映射状态，不能通过本次新 fallback 自动改写。
- 影响范围：实时数据、告警数据、未映射 observation 历史展示。
- 建议修改文件：后续新增只读回填 Service / Task，复用 `EStoneIIMappingService`。
- 验收标准：`0407102001` / `0407107001` 历史数据进入候选映射或待确认状态；历史 observation 不被误当作新增真正未映射。
- 是否涉及后端：是。
- 是否涉及权限：是，回填结果查询仍需 DataScope。
- 是否涉及协议安全边界：是，只读回填，不访问真实 FSU，不发起写操作。

### FE-BINTERFACE-REALTIME-CLEANUP-P1-001

- 问题描述：B接口实时页仍保留默认 FSU / 分组兼容逻辑。
- 影响范围：协议诊断实时页展示口径。
- 建议修改文件：`frontend/src/views/binterface/BInterfaceRealtimeView.vue`、`frontend/src/compat/realtimeSignalFilter.ts`。
- 验收标准：页面不依赖硬编码默认 FSU / 分组，统一展示后端 `signalName`、`unit`、`valueMeaning`、`mappingStatus`、`mappingConfidence`、`source`。
- 是否涉及后端：否。
- 是否涉及权限：是，仍需保留现有路由和 API 权限控制。
- 是否涉及协议安全边界：是，普通实时展示不得触发真实 FSU 请求。

## P2

### FE-MAPPING-STATUS-LABEL-P2-001

- 问题描述：`LOW` 置信度目前以前端原始文本展示，语义可读性一般。
- 影响范围：实时数据、点位卡片、告警或映射相关页面。
- 建议修改文件：`frontend/src/utils/mappingStatus.ts`。
- 验收标准：`LOW` 展示为“低置信候选”或等价中文文案，不影响后端枚举。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

### BINTERFACE2016-STANDARD-DICT-P2-001

- 问题描述：当前标准2016 fallback 仅补充真实历史样本中的两个索引。
- 影响范围：后续真实 FSU 被动上报中可能出现更多标准2016索引。
- 建议修改文件：`backend/src/main/resources/dictionary/binterface2016/standard-signal-index.csv` 及对应测试。
- 验收标准：新增索引必须有项目内协议依据；无法确认单位或含义时必须保留待确认口径。
- 是否涉及后端：是。
- 是否涉及权限：否。
- 是否涉及协议安全边界：是，仍只能作为只读字典 fallback。
