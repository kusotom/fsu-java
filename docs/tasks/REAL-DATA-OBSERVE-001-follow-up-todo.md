# REAL-DATA-OBSERVE-001 后续 TODO

## P0

无新增 P0。

## P1

### DATA-MAPPING-DICTIONARY-P1-001

- 问题描述: 当前真实 FSU `51051243812345` 的 `realtime_data` 出现 `0407102001/0407107001`，这两项已在 B接口2016 标准信号字典索引中，但不在 DATA-MAPPING-P0-002 的 eStoneII `510000xxx` 主映射字典中。
- 影响范围: 核心实时数据页、B接口实时页、未映射统计、后续告警 EventId 映射。
- 建议修改文件: `backend/src/main/java/com/dcim/platform/module/mapping/**`、`backend/src/main/resources/dictionary/**`、`openspec/protocols/binterface-2016/dictionaries/standard-signal-dictionary-index.md` 对应实现输入。
- 验收标准: `0407102001` 可返回“总电压/V”，`0407107001` 可返回“后半组电压/单位待确认”；不把这些标准 2016 SignalId 强行改写为 eStoneII `510000xxx` 已确认点位；DI 0/1 语义仍按点位独立含义处理。
- 是否涉及后端: 是。
- 是否涉及权限: 否，字典全局可读；设备实例数据仍需 DataScope。
- 是否涉及协议安全边界: 是，必须保持 B接口2016 主线，不引入 SET。

### DATA-MAPPING-BACKFILL-P1-001

- 问题描述: 当前历史 `realtime_data/alarm_record` 早于 DATA-MAPPING-P0-002，且当前 dev DB 没有 observation 表内容，历史记录未形成映射状态和 unmapped observation 回填。
- 影响范围: 页面未映射统计、历史告警、历史实时数据、排障证据链。
- 建议修改文件: 新增只读/幂等 backfill service 或管理脚本，涉及 `backend/src/main/java/com/dcim/platform/module/mapping/**`、`backend/src/test/java/com/dcim/platform/mapping/**`。
- 验收标准: backfill 不访问 FSU、不执行 SET；对 `FSU-001` legacy/demo 与真实 FSU `51051243812345` 分开标记；重复执行不制造重复 observation。
- 是否涉及后端: 是。
- 是否涉及权限: 是，若提供 API 必须管理员权限并写审计。
- 是否涉及协议安全边界: 是，只允许本地历史数据回填。

### REAL-DATA-PASSIVE-OBSERVE-P1-001

- 问题描述: 当前库中 `b_interface_message_log` 未观察到 GET_DATA/SEND_ALARM 结构化样本，无法统计新的真实 EventId/SPID 分布。
- 影响范围: 未知事件统计、真实点位覆盖、字典补齐优先级。
- 建议修改文件: 原则上不改业务代码；如需增强只读观察报表，涉及 `docs/audit/**` 或只读查询脚本。
- 验收标准: 不启 Scheduler、不主动轮询、不访问真实 FSU；只观察已有被动上报和日志。
- 是否涉及后端: 可选。
- 是否涉及权限: 否。
- 是否涉及协议安全边界: 是，必须只读。

### FE-BINTERFACE-REALTIME-CLEANUP-P1-001

- 问题描述: `BInterfaceRealtimeView.vue` 和 `frontend/src/compat/realtimeSignalFilter.ts` 仍保留硬编码 FSU、分组和 8 个 SignalID 白名单兼容逻辑。
- 影响范围: B接口实时页展示、用户对“未映射/候选/模板”的理解。
- 建议修改文件: `frontend/src/views/binterface/BInterfaceRealtimeView.vue`、`frontend/src/compat/realtimeSignalFilter.ts`、`frontend/src/api/bInterface.ts`。
- 验收标准: 页面按后端返回的 `fsuCode/deviceId/signalName/mappingStatus/mappingConfidence` 动态分组；删除临时白名单，不硬编码点位名称、单位、0/1 含义。
- 是否涉及后端: 可能需要后端补充 deviceId/source 字段。
- 是否涉及权限: 否。
- 是否涉及协议安全边界: 是，不触发 GET_DATA run-once，不新增 SET。

## P2

- `realtime_data` 缺 DeviceID，设备级候选映射无法命中。后续应在入库链路保存 `deviceId/deviceCode/sourceMessageId`，但需要先设计兼容迁移。
- 当前 dev DB 未出现 eStoneII 字典表，建议部署流程明确 schema/migration 状态，而不是依赖人工判断应用是否已启动新版本。

## P3

- 后续可增加只读统计看板，将 mappingStatus 分布、UNKNOWN_EVENT_ID、UNMAPPED TopN 展示给协议调试员。
