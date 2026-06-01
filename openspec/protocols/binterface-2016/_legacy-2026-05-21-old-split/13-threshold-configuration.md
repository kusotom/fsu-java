# 13 — 门限值配置

## 1. 协议定位

B接口2016 支持 SC 查询和设置 FSU 告警门限值。

## 2. 相关命令

| 命令 | Code | 方向 | 说明 |
|------|------|------|------|
| GET_THRESHOLD | 1901 | SC→FSU | 查询告警门限 |
| SET_THRESHOLD | 2001 | SC→FSU | 设置告警门限 |

## 3. 门限数据结构 (TThreshold)

| 字段 | 类型 | 说明 |
|------|------|------|
| SignalID | String | 信号量 ID |
| AlarmUpper | Double | 告警上限 |
| AlarmLower | Double | 告警下限 |
| AlarmUpperUrgent | Double | 严重告警上限 |
| AlarmLowerUrgent | Double | 严重告警下限 |

## 4. 当前项目实现

| 能力 | 状态 | 说明 |
|------|------|------|
| GET_THRESHOLD Service | ✅ 已实现 | 2024 格式 |
| SET_THRESHOLD Service | ✅ 已实现 | 安全门禁保护 |
| 2016 Code 对齐 | ❌ 未对齐 | BInterfaceCommand2016 缺少 GET_THRESHOLD=1901, SET_THRESHOLD=2001 |

## 5. 安全边界

SET_THRESHOLD 属于 SET 类命令，需安全门禁:
- `set-threshold.enabled: false` (默认)
- `require-confirmation: true`
- 禁止默认执行
