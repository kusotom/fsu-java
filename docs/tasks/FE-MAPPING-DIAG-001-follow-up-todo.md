# FE-MAPPING-DIAG-001 后续 TODO

日期：2026-06-01  
结论：本次未发现新增 P0；已完成前端误判的最小修复。以下任务用于后续收敛。

## P0

无新增 P0。

## P1

### FE-BINTERFACE-REALTIME-P1-001 去除 B接口实时页硬编码 FSU 和分组

- 问题描述：`BInterfaceRealtimeView.vue` 仍固定使用 `51051243812345`，并按电池/环境硬编码分组。
- 影响范围：多站点、多 FSU 场景下可能看不到当前授权 FSU 的实时数据。
- 建议修改文件：`frontend/src/views/binterface/BInterfaceRealtimeView.vue`、`frontend/src/api/bInterface.ts`，必要时补后端设备分组 DTO。
- 验收标准：页面支持按站点/FSU 选择；分组来自后端设备/映射结果；无前端硬编码 FSU。
- 是否涉及后端：可能涉及。
- 是否涉及权限：是，必须遵守 DataScope。
- 是否涉及协议安全边界：否，仍只读。

### BE-ALARM-MAPPING-REASON-P1-001 告警 DTO 补充 mappingReason

- 问题描述：未知 EventId 会进入 unmapped observation，但告警列表 DTO 未直接返回 `reason`，前端不能在告警行展示 `UNKNOWN_EVENT_ID`。
- 影响范围：告警排障效率和 UNKNOWN_EVENT_ID 可见性。
- 建议修改文件：`BInterfaceFrontendDtos.AlarmDto`、`AlarmRecordService`、`BInterfaceFrontendReadController`、相关前端告警页面。
- 验收标准：告警 API 返回 `mappingReason` 或 `unmappedReason`；前端可显示“未知告警事件”。
- 是否涉及后端：是。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

### DATA-MAPPING-BACKFILL-P1-001 历史实时/告警映射状态只读回填

- 问题描述：DATA-MAPPING-P0-002 前产生的历史记录可能缺少 `signalId/spid/rawId` 或映射字段。
- 影响范围：历史记录和旧页面可能继续显示未映射。
- 建议修改文件：新增后端只读回填 service/test，必要时新增管理端手动触发接口，默认禁用自动任务。
- 验收标准：历史数据可在不访问真实 FSU、不改 raw XML 的前提下重跑映射；回填前后有审计记录。
- 是否涉及后端：是。
- 是否涉及权限：是，需要管理员权限和审计。
- 是否涉及协议安全边界：是，必须保证不访问真实 FSU、不启 Scheduler。

## P2

### FE-UNMAPPED-FILTER-P2-001 未映射页增加状态筛选

- 问题描述：未映射页同时展示真实 unmapped observation 和 D 类模板候选。
- 影响范围：现场排障时需要快速区分真实未映射、模板候选、待真实确认。
- 建议修改文件：`frontend/src/views/resource/UnmappedPointsView.vue`。
- 验收标准：支持“真实未映射 / 模板候选 / 待真实确认 / 全部”筛选。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

### FE-DASHBOARD-MAPPING-P2-001 首页告警摘要优先展示字典名称

- 问题描述：首页最近告警仍主要用 `alarmDesc/alarmName`，未优先展示 `eventName/alarmMeaning`。
- 影响范围：首页业务可读性。
- 建议修改文件：`frontend/src/views/dashboard/DashboardView.vue`。
- 验收标准：最近告警优先显示 `eventName`，其次 `alarmMeaning/alarmDesc`。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

### FE-MAPPING-STATUS-P2-001 持续收敛 mappingStatus 展示工具

- 问题描述：本次已新增 `frontend/src/utils/mappingStatus.ts`，后续新页面应复用，避免重复状态表。
- 影响范围：前端状态口径一致性。
- 建议修改文件：所有新增实时/告警/映射页面。
- 验收标准：新增页面不再自定义互相冲突的 mappingStatus 文案。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。
