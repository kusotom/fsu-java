# FE-MONITOR-UX-P1-001 监控与告警页面信息收敛

## 任务目标

建立统一前端监控状态模型和统一数据适配器，收敛普通业务前端实时数据、告警中心、驾驶舱和 FSU 详情的展示口径。普通主视图只展示业务判断需要的信息，协议字段和映射字段下沉到技术信息折叠区或内部诊断页面。

## 本次结论

任务完成，未发现新增 P0。前端新增统一状态模型与统一数据适配器，实时数据页、告警中心、驾驶舱和 FSU 详情已完成主视图信息收敛。未修改后端业务逻辑、协议逻辑、权限、DataScope、raw XML、run-once 或 SET 安全门。

## 修改内容

- 新增 `frontend/src/utils/monitorState.ts`：
  - `UnifiedDataState`
  - `unifiedStateLabel`
  - `unifiedStateBadgeStatus`
  - `unifiedMetricStatus`
  - `normalizeUnifiedState`
  - `isBusinessAnomaly`
- 新增 `frontend/src/utils/monitorAdapters.ts`：
  - `extractApiRows`
  - `normalizeRealtimePoint(s)`
  - `summarizeRealtimePoints`
  - `normalizeBusinessAlarm(s)`
  - `summarizeAlarms`
  - `normalizeFsuDevice`
- `RealtimeDataView.vue`：主表收敛为设备名称、点位名称、当前值/状态、单位、业务状态、采集时间。
- `AlarmCenterView.vue`：主表收敛为告警等级、告警名称、设备/点位、告警状态、发生时间、恢复时间、操作入口；协议字段进入技术信息折叠区。
- `DashboardView.vue`：移除 B接口报文总数和调用记录，改为实时数据状态、历史待回填、真实未映射摘要。
- `FsuStatusDetailView.vue`：设备主表展示设备名称、业务状态、最近发现，DeviceID/DeviceCode/source 进入技术信息折叠区。
- `DataStateAlert.vue`：扩展统一业务状态展示。

## 安全边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改后端权限、DataScope、raw XML、run-once 或 SET 安全门。
- 未恢复协议诊断、点位治理、系统审计普通入口。

## 验证结果

- `cd frontend && npm run build`：通过。
- `vue-tsc --noEmit`：通过。
- `vite build`：通过。
- 仍有既有 `@vueuse/core` Rollup pure annotation warning 和 chunk size warning。

## 后续优先级

1. P1：FSU 详情页补实时数据和告警记录 Tab。
2. P1：站点详情页承载站点维度实时/告警二级交互。
3. P1：后端补稳定 `eventSeverityLabel/mappingReason`。
4. P2：驾驶舱补趋势图，但不恢复协议字段主视图。

## 输出

- [审计报告](../audit/FE-MONITOR-UX-P1-001-monitor-and-alarm-ux-convergence.md)
- [后续 TODO](../tasks/FE-MONITOR-UX-P1-001-follow-up-todo.md)
