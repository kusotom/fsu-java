# FE-REALTIME-VALUE-DISPLAY-FIX-001 修正实时数据当前值/状态列显示口径

**日期**: 2026-06-03 | **类型**: 前端展示修正

## 执行摘要

FE-REALTIME-VALUE-DISPLAY-FIX-001 完成。当前值/状态列不再把"历史待回填"拼入主值文本。

## 根因

`buildDisplayValueWithUnit()` 对非 normal/empty 状态附加 `· ${unifiedStateLabel(state)}`。legacy 数据的 state='legacy', `unifiedStateLabel('legacy')`='历史待回填', 导致每行显示 `25.50 · 历史待回填`。

## 修复

### 1. buildDisplayValueWithUnit 简化

移除状态文本拼接。函数签名改为 `(value, unit, sensorName)`:

- 文本型值 (CLOSE/DRY): 返回原值, 不加单位
- 数值型: `值 + 单位`, 单位来源: 后端 unit > sensorName 推断 > 无单位
- 不再附加状态标签

### 2. inferUnitBySensorName 新增

按 sensorName 推断展示单位兜底:

| sensorName 包含 | 单位 |
|----------------|------|
| 温度 | ℃ |
| 湿度 | %RH |
| 电压/电池 | V |
| 电流 | A |
| 功率 | W |

### 3. 种子数据验证

| pointCode | 值 | sensorName | 结果 |
|-----------|-----|------------|------|
| TEMP-001 | 25.5 | 温度 | **25.50 ℃** |
| HUMI-001 | 55 | 湿度 | **55 %RH** |
| VOLT-001 | 220.5 | 电压 | **220.50 V** |
| DOOR-001 | CLOSE | 门磁/状态 | **CLOSE** |
| WATER-001 | DRY | 水浸/状态 | **DRY** |

## 验证

- `npm run build`: 通过 (0 errors)

## 修改文件

| 文件 | 改动 |
|------|------|
| `frontend/src/utils/monitorAdapters.ts` | buildDisplayValueWithUnit 简化, inferUnitBySensorName 新增, 调用点适配 |

## 安全边界

- [x] 不访问真实 FSU
- [x] 不修改后端
- [x] 不恢复已删除的列
- [x] 不破坏 FSU 合并
