# 审计文档：LANDING-009 alarm_record 落地字段幂等 DDL 补齐

## 1. 审计日期
2026-05-20

## 2. 审计类型
Schema DDL 补齐 (零业务逻辑变更)

## 3. 审计目标

审核 `database/schema/001_init_schema.sql` 的 alarm_record 表是否补齐了 Entity 已使用但 Schema 缺失的三个字段。

## 4. 背景

| 字段 | Entity | 新增阶段 | Schema 原状态 |
|------|--------|----------|---------------|
| serial_no | `AlarmRecordEntity.serialNo` `@Column(length=128)` | LANDING-001 | ❌ 缺失 |
| device_id | `AlarmRecordEntity.deviceId` `@Column(length=128)` | LANDING-001 | ❌ 缺失 |
| spid | `AlarmRecordEntity.spid` `@Column(length=64)` | LANDING-008 | ❌ 缺失 |

LANDING-001 曾在 schema SQL 末尾追加了 serial_no/device_id 的 ALTER TABLE 语句（line 458-459），本次将其移至 alarm_record 表定义区域并与 spid 合并（line 305-308），避免分散重复。

## 5. 修改范围

### 5.1 修改文件

| 文件 | 操作 | 行数 |
|------|------|------|
| `database/schema/001_init_schema.sql` | 修改 | +5 (新增 3 ALTER TABLE + 2 注释), -4 (移除旧位置重复 serial_no/device_id) |

### 5.2 追加内容 (line 304-308)

```sql
-- LANDING-009: alarm_record 字段补齐
-- serial_no/device_id 来自 LANDING-001 (B接口2016 SEND_ALARM), spid 来自 LANDING-008
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS serial_no VARCHAR(128);
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS device_id VARCHAR(128);
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS spid VARCHAR(64);
```

### 5.3 未修改范围

- Java 生产代码: 0 修改
- Entity 定义: 不变
- CREATE TABLE alarm_record: 不变
- 索引/外键/其他表: 不变
- 正式 seed SQL: 不变

## 6. Entity / Schema 对齐检查

| Entity 字段 | @Column | DDL | 一致? |
|-------------|---------|-----|-------|
| `serialNo` | `name="serial_no", length=128` | `serial_no VARCHAR(128)` | ✅ |
| `deviceId` | `name="device_id", length=128` | `device_id VARCHAR(128)` | ✅ |
| `spid` | `name="spid", length=64` | `spid VARCHAR(64)` | ✅ |

## 7. 幂等性检查

- PostgreSQL 16 支持 `ADD COLUMN IF NOT EXISTS` ✅
- 重复执行不会报错 ✅
- 旧位置 (LANDING-001) 的重复 serial_no/device_id 已移除 ✅

## 8. 安全边界

| 边界 | 状态 |
|------|------|
| Java 生产代码 | 未修改 |
| 真实 FSU 访问 | 未访问 |
| SET 命令 | 未执行 |
| Scheduler | 未启用 |
| alarm_record 状态机 | 未修改 |
| 正式 seed SQL | 未生成 |
| Codex 复审 | 暂缓 (累积项) |

## 9. 测试结果

```
*Alarm*:      142 tests, 0 failures, 0 errors
*BInterface*: 127 tests, 0 failures, 0 errors
全量:        1177 tests, 0 failures, 0 errors, 5 skipped
```

基线保持，无回归。

## 10. 验证

```bash
$ grep -n "serial_no\|device_id\|spid" database/schema/001_init_schema.sql
305:-- serial_no/device_id 来自 LANDING-001 (B接口2016 SEND_ALARM), spid 来自 LANDING-008
306:ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS serial_no VARCHAR(128);
307:ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS device_id VARCHAR(128);
308:ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS spid VARCHAR(64);
```

3 列全部存在，无重复，位置在 alarm_record 表定义区域（紧接索引之后）。

## 11. 结论

**通过。** alarm_record 表落地字段已补齐。3 个 ALTER TABLE 语句：
- 幂等（IF NOT EXISTS）
- 字段名/类型/长度与 Entity 完全一致
- 已合并 LANDING-001 旧位置的重复语句
- 全量测试 1177/0/0/5 无回归
- 未修改 Java 生产代码
