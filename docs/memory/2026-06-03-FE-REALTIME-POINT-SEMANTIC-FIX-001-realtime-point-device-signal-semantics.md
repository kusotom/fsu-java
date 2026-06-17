# FE-REALTIME-POINT-SEMANTIC-FIX-001 站点实时数据页点位/设备/测点语义修复

## 本次目标

修复站点实时数据页把 FSU 点位、采集设备、测点/信号混用的问题。普通业务前端必须表达清楚：FSU 是业务点位层，Smoke/TempHumidity/WaterLeak/Power/Battery1 是采集设备，I2C温度/湿度/烟感/水浸/市电/电池电压是测点或监控项。

## 本次修改

- `frontend/src/utils/monitorAdapters.ts`
  - 新增 `fsuPointName/deviceKey/signalKey/measurementKey/displaySignalName`。
  - `displayDevice` 明确为采集设备。
  - `displayPointName` 保留为旧字段兼容，并注明其含义是测点/信号。
  - 实时摘要新增 `fsuPointCount/collectingDeviceCount/realtimeSignalCount`。
  - 告警兜底文案由“待确认点位”改为“待映射测点”。
- `frontend/src/views/telemetry/RealtimeDataView.vue`
  - 页面说明、筛选项、指标卡和主表列名全部改为 FSU 点位 / 采集设备 / 测点口径。
  - 指标卡从泛化点位数改为 FSU / 点位、采集设备、实时测点、数据异常、真实未映射。
- `frontend/src/views/dashboard/DashboardView.vue`
  - 数据状态摘要同步展示实时测点、采集设备、FSU / 点位。
- `frontend/src/views/binterface/FsuStatusDetailView.vue`
  - FSU 详情页设备相关文案改为采集设备。
- `frontend/src/views/alarm/AlarmCenterView.vue`
  - 普通主表“设备 / 点位”改为“设备 / 测点”，详情“点位编码”改为“测点编码”。

## 验证结果

- `cd frontend && npm run build`：通过。
- `vue-tsc --noEmit`：随 build 执行，通过。
- `vite build`：通过。
- `frontend/package.json` 无独立 `type-check` / `lint` 脚本，未额外执行。
- 既有 warning：`@vueuse/core` Rollup 注释 warning 和 chunk size warning。
- 重点文件中检索 `设备 / 点位`、`点位名称`、`实时点位`、`点位类型`、`设备名称`、`待确认点位` 未命中。

## 安全边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改后端协议解析、raw XML、run-once、SET 安全门或 DataScope。

## 遗留问题

- `displayPointName` 仍为历史兼容字段，后续应逐步迁移到 `displaySignalName`。
- 建议为 `monitorAdapters.ts` 增加前端单元测试。
- FSU 详情页后续可增加“测点概览”Tab。

## 输出

- [审计报告](../audit/FE-REALTIME-POINT-SEMANTIC-FIX-001-realtime-point-device-signal-semantics.md)
- [后续 TODO](../tasks/FE-REALTIME-POINT-SEMANTIC-FIX-001-follow-up-todo.md)
- [工程记忆](2026-06-03-FE-REALTIME-POINT-SEMANTIC-FIX-001-realtime-point-device-signal-semantics.md)
