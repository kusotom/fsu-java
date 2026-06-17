# FE-REALTIME-POINT-SEMANTIC-FIX-001 站点实时数据页点位/设备/测点语义修复

## 1. 任务背景

用户反馈“站点实时数据 > 实时数据”页面的点位语义有误，前端疑似把 Smoke、TempHumidity、WaterLeak、Power、Battery1 等采集设备展示或统计为业务点位。

本任务仅做前端 adapter、页面字段与统计口径的小范围修复，不修改后端协议解析、安全权限、raw XML、run-once、SET 安全门、DataScope 或 Scheduler。

## 2. 读取文件清单

- `docs/PROJECT_ENGINEERING_RULES.md`
- `docs/rules/CLAUDE_PROJECT_RULES.md`
- `docs/memory/WORKING-MEMORY.md`
- `docs/memory/README.md`
- `frontend/package.json`
- `frontend/src/utils/monitorAdapters.ts`
- `frontend/src/utils/monitorState.ts`
- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/binterface/FsuStatusDetailView.vue`
- `frontend/src/views/alarm/AlarmCenterView.vue`

说明：未发现 `frontend/src/types/monitoring.ts`，本次继续在现有 `monitorAdapters.ts` 中收敛普通业务 DTO。

## 3. 代码读取范围

- 实时数据统一适配器：`monitorAdapters.ts`
- 统一状态模型：`monitorState.ts`
- 站点实时数据页：`RealtimeDataView.vue`
- 监控主页数据状态摘要：`DashboardView.vue`
- FSU 详情设备列表：`FsuStatusDetailView.vue`
- 告警中心主表和详情文案：`AlarmCenterView.vue`

## 4. 问题定位

当前主要问题不是后端协议解析，而是前端普通业务展示语义仍不够清晰：

- `displayPointName` 旧字段名容易被页面继续理解为业务点位。
- 普通实时页原先偏向“点位名称/实时点位”口径，容易把 signalName 与业务点位混用。
- 统计未区分 FSU 点位、采集设备、实时测点。
- FSU 详情页仍用“设备数/设备名称”这类宽泛文案，没有明确 Smoke、TempHumidity 等是采集设备。
- 告警中心普通主表仍有“设备 / 点位”口径，容易与本次 FSU 点位语义冲突。

## 5. 修复后的层级口径

前端普通业务页现在按以下层级展示和统计：

- FSU / 点位：`fsuName || fsuCode || stationName`
- 采集设备：`deviceName`
- 测点名称：`measurementName || signalName || pointName`
- 实时测点统计：优先按 `deviceId/deviceCode + signalId/spid/rawId` 聚合

Smoke、TempHumidity、WaterLeak、Power、Battery1 等只作为采集设备展示；I2C温度、I2C湿度、烟感、水浸状态、市电、电池电压等作为测点或监控项展示。

## 6. 修改文件清单

### `frontend/src/utils/monitorAdapters.ts`

- `BusinessRealtimePoint` 新增：
  - `fsuPointName`
  - `deviceKey`
  - `signalKey`
  - `measurementKey`
  - `displaySignalName`
- 保留 `displayPointName` 作为旧字段兼容，并添加注释说明其含义是“测点/信号”，不是采集设备或业务点位。
- `normalizeRealtimePoint` 明确分离：
  - FSU 点位名称
  - 采集设备名称
  - 测点/信号名称
- `summarizeRealtimePoints` 新增：
  - `fsuPointCount`
  - `collectingDeviceCount`
  - `realtimeSignalCount`
- 告警 adapter 的兜底文案从“待确认点位”调整为“待映射测点”。

### `frontend/src/views/telemetry/RealtimeDataView.vue`

- 页面说明改为“站点监控下的 FSU 点位、采集设备与测点实时状态”。
- 筛选项“点位类型”改为“测点类型”。
- 指标卡改为：
  - FSU / 点位
  - 采集设备
  - 实时测点
  - 数据异常
  - 真实未映射
- 主表列改为：
  - FSU / 点位
  - 采集设备
  - 测点名称
  - 当前值 / 状态
  - 单位
  - 业务状态
  - 采集时间

### `frontend/src/views/dashboard/DashboardView.vue`

- 数据状态摘要同步区分：
  - 实时测点
  - 采集设备
  - FSU / 点位
- `realtimeOk` 改为基于实时测点数判断，而不是泛化点位数量。

### `frontend/src/views/binterface/FsuStatusDetailView.vue`

- FSU 详情说明改为包含“采集设备与通信记录”。
- 指标“设备数”改为“采集设备”。
- 设备列表主列“设备名称”改为“采集设备”。

### `frontend/src/views/alarm/AlarmCenterView.vue`

- 普通主表“设备 / 点位”改为“设备 / 测点”。
- 详情区“点位编码”改为“测点编码”。

## 7. 验收项对照

- Smoke、TempHumidity、WaterLeak、Power、Battery1：展示为采集设备。
- I2C温度、I2C湿度、烟感、水浸状态、市电、电池电压：通过后端返回的 signalName/pointName 展示为测点或监控项。
- FSU：在实时页作为 `FSU / 点位` 层级展示。
- 设备数量：不再作为点位数量展示。
- 测点数量：按采集设备 + 测点身份统计。
- 历史数据：仍通过 `legacy` 状态显示为历史数据，不冒充真实映射。
- 前端未新增 SignalId 到名称、单位或 0/1 含义硬编码。

## 8. 安全边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改 raw XML、run-once、SET 权限。
- 未修改 DataScope。
- 未修改后端协议解析或 eStoneII-IO 码表。

## 9. 验证结果

- `cd frontend && npm run build`：通过。
- `vue-tsc --noEmit`：随 `npm run build` 执行，通过。
- `vite build`：通过。
- `frontend/package.json` 仅包含 `dev/build/preview`，无独立 `type-check` 或 `lint` 脚本，未额外执行。
- 既有 warning：
  - `@vueuse/core` Rollup pure annotation warning。
  - chunk size warning。
- 相关旧文案检索：
  - `设备 / 点位`
  - `点位名称`
  - `实时点位`
  - `点位类型`
  - `设备名称`
  - `待确认点位`

  在本次重点文件中未命中。

## 10. P0/P1/P2 问题清单

### P0

无新增 P0。

### P1

- `displayPointName` 仍为历史兼容字段名，建议后续在类型层逐步迁移到 `displaySignalName`。
- 后端实时数据长期应稳定返回 `fsuName/fsuCode/deviceName/signalName`，减少前端兜底。
- FSU 详情页后续可增加“测点概览”Tab，承载设备下测点数量与状态摘要。

### P2

- 为 `monitorAdapters.ts` 增加前端单元测试，覆盖 FSU 点位、采集设备、测点统计三类口径。
- Dashboard 可增加趋势或分布图，但不恢复协议字段主视图。

## 11. 后续建议

建议后续进入：

- `FE-MONITOR-ADAPTER-TEST-P1-001`：为统一监控适配器增加前端单元测试。
- `FSU-DETAIL-MEASUREMENT-TAB-P1-001`：在 FSU 详情页补“测点概览”二级展示。

## 12. 最终结论

FE-REALTIME-POINT-SEMANTIC-FIX-001 完成。站点实时数据页已修正点位/设备/测点语义：FSU 作为业务点位，Smoke/TempHumidity/WaterLeak/Power/Battery1 等作为采集设备，I2C温度/湿度/烟感/水浸/市电/电池电压等作为测点或监控项；统计口径已区分 FSU 点位数、采集设备数和实时测点数。未访问真实 FSU，未执行 SET，未启 Scheduler，未放松权限。
