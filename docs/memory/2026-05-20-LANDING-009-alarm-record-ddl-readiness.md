# 工程记忆：LANDING-009 alarm_record 落地字段幂等 DDL 补齐

## 1. 时间
2026-05-20

## 2. 背景

LANDING-001 新增了 serialNo/deviceId 两个 Entity 字段，LANDING-008 新增了 spid 字段。这三个字段在 Entity 和 SendAlarmService 中已正常使用，但 `database/schema/001_init_schema.sql` 的 alarm_record 表定义未包含它们。

LANDING-001 曾在 schema SQL 末尾追加了 serial_no/device_id 的 ALTER TABLE（距离表定义较远），spid 从未补齐。

## 3. 本次目标

1. 在 alarm_record 表定义区域集中补齐 3 条幂等 ALTER TABLE
2. 移除 LANDING-001 旧位置重复的 serial_no/device_id
3. 确保 Entity ↔ Schema 字段名/类型/长度一致
4. 全量测试无回归

## 4. 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `database/schema/001_init_schema.sql` | 修改 | +3 ALTER TABLE 近表定义, -2 移除旧位置重复 |
| `openspec/specs/landing-009-*.md` | 新增 | Spec |
| `openspec/plans/landing-009-*.md` | 新增 | Plan |
| `docs/audit/LANDING-009-*.md` | 新增 | 审计文档 |
| `docs/memory/2026-05-20-LANDING-009-*.md` | 新增 | 本文件 |
| `docs/memory/README.md` | 修改 | 索引更新 |
| `docs/memory/WORKING-MEMORY.md` | 修改 | 当前阶段更新 |

## 5. 关键决策

1. **DDL 位置**: 放在 alarm_record 表定义区域（CREATE INDEX 之后，下一个 CREATE TABLE 之前），而非 schema 文件末尾。便于维护者理解表结构。
2. **幂等性**: 全部使用 `IF NOT EXISTS`，PostgreSQL 16 原生支持。
3. **合并旧 DDL**: LANDING-001 在文件末尾追加的 serial_no/device_id ALTER TABLE 已移除，避免分散和重复。
4. **不合并到 CREATE TABLE**: 保持 CREATE TABLE 语句不变，ALTER TABLE 追加方式对存量部署更安全。

## 6. Entity ↔ Schema 对齐

| Entity 字段 | @Column | DDL | 一致 |
|-------------|---------|-----|------|
| serialNo | serial_no VARCHAR(128) | serial_no VARCHAR(128) | ✅ |
| deviceId | device_id VARCHAR(128) | device_id VARCHAR(128) | ✅ |
| spid | spid VARCHAR(64) | spid VARCHAR(64) | ✅ |

## 7. B接口协议依据

三个字段均来源于 B接口2016 SEND_ALARM 告警上报的 XML 字段：
- SerialNo → serial_no
- DeviceID → device_id
- SPID → spid

## 8. 安全边界

| 边界 | 本次状态 |
|------|----------|
| 修改 Java 生产代码 | **否** |
| 访问真实 FSU | **否** |
| 启用 Scheduler | **否** |
| 执行 SET | **否** |
| 修改 alarm_record 状态机 | **否** |
| 生成正式 seed SQL | **否** |
| Codex 复审 | 暂缓 |

## 9. 测试结果

```
*Alarm*:      142 tests, 0 failures, 0 errors
*BInterface*: 127 tests, 0 failures, 0 errors  
全量:        1177 tests, 0 failures, 0 errors, 5 skipped
```

基线保持。

## 10. 遗留问题

无新增遗留问题。

## 11. 下一步建议

1. 管理方提供 DeviceID/SPID 映射表 → 导入 monitoring_point
2. FSU 侧配置 SC 上报目标后启动被动上报联调
3. 为 18 个已有 B接口命令补充 openspec/specs/ 规格文档
4. 提交 Codex 集中复审
