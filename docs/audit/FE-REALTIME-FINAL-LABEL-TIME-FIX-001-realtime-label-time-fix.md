# FE-REALTIME-FINAL-LABEL-TIME-FIX-001 点位名称和采集时间格式修复

**日期**: 2026-06-03 | **类型**: 前端展示修正

## 执行摘要

FE-REALTIME-FINAL-LABEL-TIME-FIX-001 完成。

## 修复内容

### 1. FSU/点位列站点名称

前序 FE-REALTIME-STATION-NAME-FIX-001 已修复: 后端 DTO +stationName +siteName, 前端 fsuPointName 优先级 stationName > siteName > fsuName > fsuCode。

### 2. 电池测点名称细化

`normalizeSensorName()` 电池检测从笼统的 `电池电压` 细化为:

| 匹配条件 | 结果 |
|---------|------|
| 含"中点电压" | 中点电压 |
| 含"中点不平衡"/"中点"+"告警" | 电池告警 |
| 含"总电压"/"电压" | 电池电压 |
| 含"告警"/"熔丝"/"故障" | 电池告警 |
| 其他电池相关 | 待确认电池测点 |

### 3. 采集时间格式化

新增 `formatBusinessDateTime()`: `2026-05-10T12:00:00` → `2026-05-10 12:00:00`

`normalizeRealtimePoint` 的 `collectTime` 字段统一经过该函数处理。

## 验证

- `npm run build`: 通过 (0 errors)

## 修改文件

`frontend/src/utils/monitorAdapters.ts` — 电池名称细分 + formatBusinessDateTime
