# FE-REALTIME-SENSOR-NAME-SOURCE-FIX-001 修复实时数据测点名称仍显示"历史测点"问题

**日期**: 2026-06-03 | **类型**: 前端 adapter 修复 | **前置**: FE-REALTIME-TABLE-FINAL-FIX-001

## 执行摘要

FE-REALTIME-SENSOR-NAME-SOURCE-FIX-001 完成。测点名称不再全部显示"历史测点"。

## 根因分析

### 数据链路追踪

1. **数据库 seed 数据**: `realtime_data` 表 `point_code` = `'TEMP-001'`, `'HUMI-001'`, `'VOLT-001'`, `'DOOR-001'`, `'WATER-001'` (非数字编码)

2. **后端 RealtimeDataService.toDto()**:
   - `signalId = e.getPointCode()` = `'TEMP-001'`
   - `EStoneIIMappingService.resolveRealtime()` → 字典中无 'TEMP-001' 匹配 → 返回 `UNMAPPED`, `signalName=null`
   - `isLegacyRealtimePointCode('TEMP-001')` → 非 `\d+` → `true` → 设置 `mappingStatus="HISTORICAL_PENDING_BACKFILL"`

3. **前端收到**: `{signalId: "TEMP-001", signalName: null, pointName: null, mappingStatus: "HISTORICAL_PENDING_BACKFILL", source: "legacy_realtime_data"}`

4. **原 normalizeSensorName()**:
   - `rawName` = '' (signalName/pointName 均为 null)
   - `fallbackId` = `'TEMP-001'`
   - `nameToNormalize` = `'TEMP-001'`
   - 所有文本匹配规则（`includes('温度')` 等）均不命中
   - 返回 `''`

5. **最终 sensorName**: `'' || '历史测点'` → **'历史测点'**

## 修复内容

### 1. normalizeSensorName 重写

**文件**: `frontend/src/utils/monitorAdapters.ts`

**新增功能**:

a) **`isGenericLegacyName()` 过滤**: "历史测点"/"待映射测点"/"点位名称未确认" 等通用兜底名不作为有效源名称。当 rawName 是通用兜底名时, 自动尝试用 signalId/pointCode 替代。

b) **描述性编码前缀匹配** (大小写不敏感):

| 前缀 | 映射 |
|------|------|
| TEMP-, 温度 | 温度 |
| HUMI-, 湿度 | 湿度 |
| VOLT-, 电压 | 电压 |
| DOOR-, 门磁, 门 | 门磁/状态 |
| WATER-, 水浸, 漏水 | 水浸/状态 |
| SMOKE-, 烟感, 烟雾 | 烟感 |
| BAT-, 电池 | 电池电压 |
| CURR-, 电流 | 电流 |
| POW-, 市电, AC | 市电 |

c) **纯数字 signalId (B接口2016)**: 按 device_type_code 子串推断类型 (`06`=开关电源, `07`=蓄电池, `18`=环境)

d) **rawName 有效名称保留**: 非通用兜底名的原始名称作为最后 fallback

### 2. sensorName fallback 优化

当 `normalizeSensorName` 返回空时, 如果存在 signalKey (signalId/pointCode), 显示 `历史测点(TEMP-001)` 而非仅 `历史测点`。

### 3. 种子数据映射结果

| pointCode | 新 sensorName |
|-----------|--------------|
| TEMP-001 | **温度** |
| HUMI-001 | **湿度** |
| VOLT-001 | **电压** |
| DOOR-001 | **门磁/状态** |
| WATER-001 | **水浸/状态** |

## 验证

- `npm run build`: **通过** (0 errors)

## 不涉及

- 未修改后端协议解析/数据库/权限
- 未访问真实 FSU
- 未硬编码行号
- 未在 Vue 页面写 SignalId 映射表

## 修改文件

| 文件 | 改动 |
|------|------|
| `frontend/src/utils/monitorAdapters.ts` | normalizeSensorName 重写 + sensorName fallback 优化 |
