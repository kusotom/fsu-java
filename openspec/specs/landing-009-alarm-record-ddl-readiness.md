# LANDING-009 Spec：alarm_record 落地字段幂等 DDL 补齐

> 规格版本：1.0
> 创建日期：2026-05-20
> 对应计划：`openspec/plans/landing-009-alarm-record-ddl-readiness-plan.md`

---

## 1. 背景

LANDING-001 在 `AlarmRecordEntity` 中新增了 `serialNo` 和 `deviceId` 字段，LANDING-008 实施阶段新增了 `spid` 字段。这三个字段已在 Entity 和 Service 层正常使用：

| Entity 字段 | @Column | 新增阶段 | 用途 |
|-------------|---------|----------|------|
| `serialNo` | `serial_no VARCHAR(128)` | LANDING-001 | B接口2016 SEND_ALARM 告警序列号 |
| `deviceId` | `device_id VARCHAR(128)` | LANDING-001 | B接口2016 设备 ID |
| `spid` | `spid VARCHAR(64)` | LANDING-008 | B接口2016 信号点 ID |

`SendAlarmService` 在 `buildAlarmEntity()` 和恢复路径中正确写入这三个字段。

**问题**: `database/schema/001_init_schema.sql` 的 `CREATE TABLE alarm_record` 未包含这三个列。JPA `ddl-auto` 可能自动补齐（取决于配置），但 schema SQL 作为权威的数据库初始化脚本，必须与 Entity 保持一致。

这是 LANDING-001 和 LANDING-008 的遗留问题。

## 2. 目标

在 `database/schema/001_init_schema.sql` 的 `alarm_record` 表定义后追加三条幂等 `ALTER TABLE ADD COLUMN IF NOT EXISTS` 语句：

```sql
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS serial_no VARCHAR(128);
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS device_id VARCHAR(128);
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS spid VARCHAR(64);
```

确保：
1. 全新部署时列已存在于 CREATE TABLE（如后续重构可合并）
2. 存量部署时幂等执行（IF NOT EXISTS 不会报错）
3. 字段名/类型/长度与 Entity @Column 定义一致

## 3. 非目标

- 不修改 Java 生产代码
- 不修改 Entity 定义
- 不修改 SendAlarmService 业务逻辑
- 不访问真实 FSU
- 不执行 SET
- 不启用 Scheduler
- 不修改 alarm_record 状态机
- 不生成正式 seed SQL
- 不重构 CREATE TABLE 语句（不合并列到 DDL）

## 4. 协议依据

本次为纯 DDL 补齐任务，不涉及 B接口协议变更。字段来源已在 LANDING-001 和 LANDING-008 中论证：

| 字段 | 协议来源 |
|------|----------|
| serial_no | B接口2016 SEND_ALARM Alarm.SerialNo |
| device_id | B接口2016 SEND_ALARM Alarm.DeviceID |
| spid | B接口2016 SEND_ALARM Alarm.SPID |

## 5. 验收标准

| # | 验收项 | 验证方式 |
|---|--------|----------|
| 1 | schema SQL 含 `ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS serial_no VARCHAR(128)` | grep |
| 2 | schema SQL 含 `ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS device_id VARCHAR(128)` | grep |
| 3 | schema SQL 含 `ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS spid VARCHAR(64)` | grep |
| 4 | Entity 字段定义与 DDL 一致（名称/类型/长度） | 逐项对照 |
| 5 | 全量测试无回归 | mvn test |
| 6 | 未修改 Java 生产代码 | git diff |
| 7 | 未访问真实 FSU | 安全边界 |
| 8 | memory + audit 已写入 | 文件存在 |

## 6. 风险

| # | 风险 | 可能性 | 影响 | 缓解 |
|---|------|--------|------|------|
| 1 | PostgreSQL < 9.6 不支持 `ADD COLUMN IF NOT EXISTS` | 极低 | 中 | 项目使用 PostgreSQL 16，完全支持 |
| 2 | 字段长度不一致（如 spid 实际需要 > 64 chars） | 低 | 低 | 与 Entity @Column(length=64) 一致，后续可按需 ALTER |
| 3 | ALTER TABLE 与 CREATE TABLE 合并导致重复 | 低 | 低 | IF NOT EXISTS 保证幂等，合并后可删除 ALTER 语句 |

## 7. 安全边界

| 边界 | 本次状态 |
|------|----------|
| 真实 FSU 访问 | **不访问** |
| Scheduler | **不启用** |
| SET 命令 | **不执行** |
| alarm_record 状态机 | **不修改** |
| Java 生产代码 | **不修改** |
| 正式 seed SQL | **不生成** |
| Codex 复审 | **暂缓** |

## 8. 已知遗留问题

1. CREATE TABLE 语句本身仍不含这三列（本次采用 ALTER TABLE 追加，不影响存量部署）
2. 后续可将 ALTER TABLE 列合并到 CREATE TABLE 中（需评估对存量部署的影响）
