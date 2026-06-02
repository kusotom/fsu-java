# FE-REALTIME-POINT-SEMANTIC-FIX-001 后续 TODO

## P0

无。

## P1

### FE-MONITOR-ADAPTER-TEST-P1-001

- 问题描述：当前 `monitorAdapters.ts` 已统一 FSU 点位、采集设备、实时测点口径，但缺少前端单元测试保护。
- 影响范围：实时数据页、监控主页、告警中心、FSU 详情页。
- 建议修改文件：`frontend/src/utils/monitorAdapters.ts`，新增对应测试文件。
- 验收标准：覆盖 `fsuPointCount`、`collectingDeviceCount`、`realtimeSignalCount`、legacy、unmapped、0.0000 保留等场景。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

### FSU-DETAIL-MEASUREMENT-TAB-P1-001

- 问题描述：FSU 详情页目前只把设备列表语义修正为采集设备，尚未提供“采集设备下测点概览”的二级展示。
- 影响范围：FSU 详情页。
- 建议修改文件：`frontend/src/views/binterface/FsuStatusDetailView.vue`，必要时接入现有实时数据 API。
- 验收标准：FSU 详情可按采集设备分组展示测点名称、当前值、单位、业务状态和采集时间；不展示 raw XML/run-once/SET 普通入口。
- 是否涉及后端：可能不涉及；如 API 不支持按 FSU 查询测点，则需后端只读查询增强。
- 是否涉及权限：按现有 FSU / realtime 查看权限。
- 是否涉及协议安全边界：否。

### FE-MONITOR-TYPE-CLEANUP-P1-001

- 问题描述：`displayPointName` 仍作为兼容字段存在，虽然已注明其代表测点/信号，但命名仍可能误导后续开发。
- 影响范围：统一适配器和旧页面兼容。
- 建议修改文件：`frontend/src/utils/monitorAdapters.ts` 及使用旧字段的页面。
- 验收标准：普通业务页面统一使用 `displaySignalName`，旧字段只保留在兼容层或完全移除。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

## P2

### FE-DASHBOARD-SEMANTIC-CHART-P2-001

- 问题描述：监控主页已区分实时测点、采集设备和 FSU 点位，但仍是纯数字摘要。
- 影响范围：监控主页。
- 建议修改文件：`frontend/src/views/dashboard/DashboardView.vue`。
- 验收标准：增加轻量分布图或分组摘要，仍不恢复 SignalId/SPID/raw XML 等协议字段主视图。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。
