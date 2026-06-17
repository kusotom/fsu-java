# FE-MONITOR-UX-P1-001 监控与告警页面信息收敛

## 1. 任务背景

用户要求收敛普通业务前端的数据监控与告警展示，避免实时数据、告警中心、驾驶舱、FSU 详情各自解释接口字段。普通业务页面只展示判断与操作所需信息，协议字段、映射字段和 raw 类字段下沉到协议诊断或技术信息折叠区。

## 2. 修改前问题

- 实时数据页页面内直接归一化字段和状态，缺少统一适配器。
- 告警页主表仍显示 FSU、告警含义、告警值、映射状态等排障字段。
- 驾驶舱仍展示 B接口报文和调用记录等协议视角摘要。
- FSU 详情设备列表直接显示 DeviceID、DeviceCode、source 等技术字段。
- `DataStateAlert` 只支持旧接口状态，不支持统一业务状态模型。

## 3. 新增统一状态模型

新增 `frontend/src/utils/monitorState.ts`:

```ts
export type UnifiedDataState =
  | 'normal'
  | 'warning'
  | 'alarm'
  | 'offline'
  | 'empty'
  | 'stale'
  | 'legacy'
  | 'unmapped'
  | 'parse_error'
  | 'api_error'
  | 'permission_denied'
```

同时提供:

- `unifiedStateLabel`
- `unifiedStateBadgeStatus`
- `unifiedMetricStatus`
- `normalizeUnifiedState`
- `isBusinessAnomaly`

## 4. 新增统一数据适配器

新增 `frontend/src/utils/monitorAdapters.ts`:

- `extractApiRows(response)`：统一提取数组响应，避免页面各自处理 `res.data/data.data`。
- `normalizeRealtimePoint()` / `normalizeRealtimePoints()`：生成普通业务实时点展示对象。
- `summarizeRealtimePoints()`：统一实时数据统计。
- `normalizeBusinessAlarm()` / `normalizeBusinessAlarms()`：基于告警字典 normalizer 生成普通业务告警展示对象。
- `summarizeAlarms()`：统一告警统计。
- `normalizeFsuDevice()`：统一 FSU 设备列表展示状态。

## 5. 实时数据页收敛

修改 `frontend/src/views/telemetry/RealtimeDataView.vue`。

主表现在只展示:

- 设备名称
- 点位名称
- 当前值 / 状态
- 单位
- 业务状态
- 采集时间

从主表移除:

- FSU 列
- 点位类型列
- 质量列
- 映射状态列
- 页面内自定义归一化逻辑

统计改为使用统一适配器:

- 实时点位
- 有值点位
- 数据异常
- 真实未映射
- 离线设备

## 6. 告警中心收敛

修改 `frontend/src/views/alarm/AlarmCenterView.vue`。

主表现在只展示:

- 告警等级
- 告警名称
- 设备 / 点位
- 告警状态
- 发生时间
- 恢复时间
- 操作入口

从主表移除:

- FSU
- 告警含义
- 告警值
- 映射状态

详情抽屉中，SerialNo、DeviceID、SPID、SignalID、EventID、EventSeverity、映射状态、映射置信度等字段进入“技术信息”折叠区，默认不污染普通主视图。

## 7. 驾驶舱收敛

修改 `frontend/src/views/dashboard/DashboardView.vue`。

驾驶舱继续展示:

- FSU 在线数
- 当前告警数
- 严重告警数
- 主要告警数
- 数据异常数
- 最新告警
- 站点摘要
- 异常 FSU 摘要

移除普通驾驶舱中的协议视角摘要:

- B接口报文总数
- 今日调用记录

替换为业务数据状态摘要:

- 实时数据状态
- 历史待回填
- 真实未映射

## 8. FSU 详情收敛

修改 `frontend/src/views/binterface/FsuStatusDetailView.vue`。

设备列表主表改为:

- 设备名称
- 业务状态
- 最近发现

DeviceID、DeviceCode、source、映射状态进入“技术信息”折叠区。

## 9. 普通业务页禁用字段检查

本次收敛后，普通实时数据页、普通告警页和驾驶舱主视图不再直接展示:

- SignalId
- EventId
- SPID
- rawId / rawName
- raw XML
- templateVariant
- mappingConfidence
- needRealDataConfirm
- PK_Type / command code
- XML 结构解释

这些字段仍可在告警详情技术信息、FSU 详情技术信息或内部诊断页面中查看。

## 10. 安全边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改后端权限、DataScope、raw XML、run-once 或 SET 安全门。
- 未恢复协议诊断、点位治理、系统审计普通入口。
- 未删除后端映射、审计、权限或协议能力。

## 11. 验证结果

执行:

```bash
cd frontend
npm run build
```

结果:

- `vue-tsc --noEmit` 通过。
- `vite build` 通过。
- 仍有既有 `@vueuse/core` Rollup pure annotation warning 和 chunk size warning。

## 12. P0/P1/P2 问题清单

- P0: 未发现。
- P1: 后端建议补稳定 `eventSeverityLabel`、`mappingReason`，减少前端兼容解释。
- P1: FSU 详情页后续应补实时数据、告警记录 Tab，继续承载站点/FSU 二级交互。
- P2: 驾驶舱可继续补趋势图和数据质量图，但不得恢复协议字段主视图。

## 13. 最终结论

FE-MONITOR-UX-P1-001 完成。普通业务前端已建立统一状态模型和统一数据适配器，实时数据页、告警中心、驾驶舱和 FSU 详情主视图完成信息收敛；协议字段、映射字段和技术字段已下沉到技术信息折叠区或内部诊断页面。未修改后端协议和安全边界，npm build 通过。
