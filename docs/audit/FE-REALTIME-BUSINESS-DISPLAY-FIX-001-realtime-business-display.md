# FE-REALTIME-BUSINESS-DISPLAY-FIX-001 站点实时数据页业务展示口径修正

**日期**: 2026-06-03
**类型**: 前端展示修正
**范围**: 仅前端 RealtimeDataView + DashboardView + monitorAdapters

## 执行摘要

**FE-REALTIME-BUSINESS-DISPLAY-FIX-001 完成。** 站点实时数据页已按业务口径收敛。

## 修改内容

### 1. 删除"采集设备"列

**文件**: `RealtimeDataView.vue`

主表移除 `采集设备` 列（原 `displayDevice` 列）。采集设备信息（Smoke/TempHumidity/WaterLeak/Power/Battery1）不再作为普通表格列展示，仅保留在 `BusinessRealtimePoint.displayDevice` 技术字段中供诊断页使用。

### 2. 测点名称收敛为业务传感器名称

**文件**: `monitorAdapters.ts`

新增 `normalizeSensorName()` 函数，将技术名收敛为业务名：

| 技术名 | 业务名 |
|--------|--------|
| I2C温度 | 温度 |
| I2C湿度 | 湿度 |
| 烟感/烟雾 | 烟感 |
| 水浸/漏水 | 水浸 |
| 市电A路/交流A相 | 市电A路 |
| 市电B路/交流B相 | 市电B路 |
| 电池总电压 | 电池电压 |
| 中点电压 | 中点电压 |
| 通讯状态/通信状态 | 通信状态 |

未匹配的名称保留原始值。该函数只做展示收敛，不做协议映射。

`BusinessRealtimePoint` 新增 `sensorName` 字段，页面使用 `sensorName` 代替 `displaySignalName`。

### 3. 统计口径调整

**RealtimeDataView** 指标卡片:
- 移除: `采集设备`
- 保留: `FSU 点位`, `实时测点`, `异常测点`, `待映射`

**DashboardView**:
- 移除: `采集设备` 统计行
- 文案: `真实未映射` → `待映射`

**`summarizeRealtimePoints()`**: 移除 `collectingDeviceCount` 输出字段。

### 4. 页面文案

- 副标题: `站点监控下的 FSU 点位、传感器与实时状态`
- 表格列: `FSU / 点位 | 测点名称 | 当前值/状态 | 单位 | 业务状态 | 采集时间`

### 5. adapter 集中处理

所有展示名收敛逻辑集中在 `monitorAdapters.ts`:
- `normalizeSensorName()` — 业务传感器名称收敛
- `normalizeRealtimePoint()` — 统一数据归一化
- `summarizeRealtimePoints()` — 统一统计口径

页面 (`RealtimeDataView.vue`) 只消费 adapter 输出，不散落判断逻辑。

## 未修改

- 协议诊断页 (`BInterfaceRealtimeView.vue`, `FsuStatusDetailView.vue`) — 保留采集设备显示
- 告警页 (`AlarmCenterView.vue`) — 保留设备+测点组合显示
- `BusinessRealtimePoint.displayDevice` — 保留技术字段供诊断使用

## 验证

- `npm run build`: **通过** (vue-tsc + vite build, 0 errors)
- 前端无 `test` / `type-check` / `lint` 脚本

## 安全边界

- [x] 不修改后端协议解析
- [x] 不修改 eStoneII-IO 标准码表
- [x] 不访问真实 FSU
- [x] 不执行 SET
- [x] 不启 Scheduler
- [x] 不放松权限
- [x] 不把采集设备当成点位
- [x] 不在页面硬编码 SignalId 映射表
- [x] 不把历史数据冒充真实已确认点位

## 修改文件

| 文件 | 操作 |
|------|------|
| `frontend/src/utils/monitorAdapters.ts` | 修改 — normalizeSensorName, sensorName, 移除collectingDeviceCount |
| `frontend/src/views/telemetry/RealtimeDataView.vue` | 修改 — 移除采集设备列, 指标卡片, 文案 |
| `frontend/src/views/dashboard/DashboardView.vue` | 修改 — 移除采集设备统计, 文案 |
