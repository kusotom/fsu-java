# FE-REALTIME-TABLE-FINAL-FIX-001 站点实时数据表格最终展示口径修正

**日期**: 2026-06-03 | **类型**: 前端展示修正 | **前置**: FE-REALTIME-BUSINESS-DISPLAY-FIX-001

## 执行摘要

FE-REALTIME-TABLE-FINAL-FIX-001 完成。表格已按业务口径收敛为 4 列, FSU 列按相同 FSU 合并展示。

## 修改内容

### 1. FSU/点位列合并 (span-method)

`RealtimeDataView.vue` 新增 `fsuSpanMethod` 函数, 对连续相同 `fsuPointName` 的行合并第一列单元格。相同 FSU 只显示一次。

### 2. 删除"单位"列和"业务状态"列

表格从 6 列精简为 4 列: `FSU/点位 | 测点名称 | 当前值/状态 | 采集时间`

- 单位合并到 `当前值/状态` 列 (如 `25.50 ℃`, `55 %RH`)
- 业务状态以轻量标签附加到值后 (如 `25.50 待确认`)
- 正常状态不附加标签

### 3. displayValueWithUnit 统一字段

`monitorAdapters.ts` 新增 `buildDisplayValueWithUnit()` 函数, 生成合并列:
- 数值型: `25.50 ℃` 或 `25.50 待确认` (单位待确认时)
- 文本型(CLOSE/DRY): 不加单位, 仅附加状态标签
- 非正常状态附加 `· {状态标签}` 后缀

### 4. 传感器名称映射增强

`normalizeSensorName()` 增加 fallback: 当 signalName/pointName 为空时, 使用 signalId/pointCode/spid/rawId 作为标识进行匹配。新增 `门磁/状态`、`市电`、`状态` 收敛规则。

### 5. 异常测点统计修正

`summarizeRealtimePoints().anomaly` 排除 legacy 历史数据, 避免历史待回填数据计入"异常测点"。

### 6. DashboardView 同步

移除 `采集设备` 统计行 (上一轮已做), `真实未映射` → `待映射`。

## 最终表格列

| 列 | 宽度 | 说明 |
|----|------|------|
| FSU / 点位 | min 150px | span-method 合并相同 FSU |
| 测点名称 | min 160px | sensorName, 业务传感器名称 |
| 当前值 / 状态 | min 180px | displayValueWithUnit, 值+单位+轻量状态 |
| 采集时间 | 170px | 格式化采集时间 |

## 验证

- `npm run build`: 通过 (vue-tsc + vite build, 0 errors)

## 修改文件

| 文件 | 改动 |
|------|------|
| `frontend/src/utils/monitorAdapters.ts` | displayValueWithUnit, buildDisplayValueWithUnit, normalizeSensorName 增强, anomaly 排除 legacy |
| `frontend/src/views/telemetry/RealtimeDataView.vue` | span-method, 移除单位列/业务状态列, 4 列布局 |
| `frontend/src/views/dashboard/DashboardView.vue` | 移除采集设备统计 (上一轮) |

## 安全边界

- [x] 不访问真实 FSU
- [x] 不执行 SET
- [x] 不启 Scheduler
- [x] 不修改后端
- [x] 不在页面硬编码 SignalId 映射
- [x] 不把设备当成点位
