# POINT-MAPPING-AUDIT-001: B接口点位模型三层结构审计

> 日期: 2026-05-21 | 类型: 只读审计 | 修改: 0

## 1. 审计目标

确认平台是否按 **FSU + Device + Signal** 三层结构建模，避免只按 SignalID 建唯一约束。

## 2. 审计结论

**存在 P0 风险**: `monitoring_point` 和 `realtime_data` 缺少 `device_id`，不同 DeviceID 下相同 SignalID 会被错误合并。

## 3. 表结构审计

| 表/实体 | 有 fsu_id | 有 device_id | 有 signal_id/spid | 唯一键 | 风险 |
|---------|:--:|:--:|:--:|------|:--:|
| `monitoring_point` | ✅ | ❌ | ✅ (point_code) | `UNIQUE(fsu_id, point_code)` | **P0** — 缺少 device_id |
| `realtime_data` | ✅ | ❌ | ✅ (point_code) | `UNIQUE(point_id)` | **P0** — 缺少 device_id |
| `alarm_record` | ✅ | ✅ | ✅ (point_code, spid) | 无 | ✅ LANDING-008 已补 |
| `device_heartbeat` | — | ✅ | — | — | — |
| `history_data` | ✅ | ❌ | ✅ (point_code) | `UNIQUE(fsu_id, point_code, collect_time)` | P2 — 缺少 device_id |

## 4. 服务逻辑审计

| 服务 | 保留 DeviceID | 保留 SignalID | 风险 |
|------|:--:|:--:|------|
| `SendAlarmService` | ✅ | ✅ | ✅ LANDING-008 已实现 |
| `SendDataService` | ❌ | ✅ (SignalID) | **P0** — 无法区分同 SignalID 不同 DeviceID |
| `GetDataService` (2024) | ❌ | ✅ (SignalID) | P1 — 2024 版本不处理 DeviceList |
| `BInterface2016GetDataService` | ✅ (请求) | ✅ | ✅ BIF2016-P0-001 已实现 |
| `GetThresholdService` | ❌ | ✅ | P1 — 待检查 |

## 5. Repository 查询审计

| Repository | 查询方法 | 缺失 device_id? |
|-----------|---------|:--:|
| `MonitoringPointRepository` | `findByFsuIdAndPointCode(fsuId, pointCode)` | **是** |
| `RealtimeDataRepository` | `findByPointId(pointId)` | 依赖 point_id FK |

## 6. seed SQL / 模板审计

| 文件 | 按 DeviceID 分组 | 状态 |
|------|:--:|------|
| `REAL-STD-SP-DIC-TEMPLATE.csv` | 无 | 仅表头 |
| `REAL-SUCONFIGINSTANCE-TEMPLATE.csv` | 有 DeviceID 字段 | 仅表头 |
| `real-fsu-point-seed-template.csv` | 有 deviceId 列 | TASK-001 生成, 待填 |

## 7. 发现的问题

### P0

| # | 问题 | 影响 | 涉及文件 |
|---|------|------|---------|
| 1 | `monitoring_point` 无 `device_id` | 同 FSU 下不同 DeviceID 的相同 SignalID 被合并 | `MonitoringPointEntity`, schema, Repository |
| 2 | `realtime_data` 无 `device_id` | 无法定位数据来源设备 | `RealtimeDataEntity`, schema |
| 3 | `SendDataService` 不处理 DeviceID | FSU 上报 SEND_DATA 时无法区分设备 | `SendDataService.java` |

### P1

| # | 问题 | 涉及文件 |
|---|------|---------|
| 1 | `GetDataService` 2024 版本不解析 Device.Id/Code | `GetDataService.java` |
| 2 | `GetThresholdService` 待确认真实数据 | `GetThresholdService.java` |

## 8. 修复建议

| 优先级 | 行动 | 任务编号建议 |
|--------|------|------------|
| P0 | `monitoring_point` 新增 `device_id` 列 | POINT-FIX-001 |
| P0 | `realtime_data` 新增 `device_id` 列 | POINT-FIX-002 |
| P0 | `SendDataService` 增加 DeviceID 解析 | POINT-FIX-003 |
| P1 | `GetDataService` 增加 Device.Id/Code 解析 | 已部分在 BIF2016-P0-001 |

## 9. 安全确认

- ✅ 未访问真实 FSU
- ✅ 未执行 SET
- ✅ 未启 Scheduler
- ✅ 未修改数据库
- ✅ 未删除数据
