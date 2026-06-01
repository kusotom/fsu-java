# LANDING-009 Plan：alarm_record 落地字段幂等 DDL 补齐

> 计划版本：1.0
> 创建日期：2026-05-20
> 对应 Spec：`openspec/specs/landing-009-alarm-record-ddl-readiness.md`

---

## 1. 读取资料

| # | 文件 | 用途 |
|---|------|------|
| 1 | `database/schema/001_init_schema.sql` | 当前 alarm_record 表定义 |
| 2 | `AlarmRecordEntity.java` | 核对 Entity 字段名/类型/长度 |
| 3 | `docs/memory/WORKING-MEMORY.md` | 当前状态 |
| 4 | `openspec/specs/landing-009-alarm-record-ddl-readiness.md` | 本次规格 |

## 2. Entity ↔ Schema 核对

| Entity 字段 | @Column | Schema 当前状态 | 计划操作 |
|-------------|---------|-----------------|----------|
| `serialNo` | `name="serial_no", length=128` | ❌ 缺失 | `ADD COLUMN IF NOT EXISTS serial_no VARCHAR(128)` |
| `deviceId` | `name="device_id", length=128` | ❌ 缺失 | `ADD COLUMN IF NOT EXISTS device_id VARCHAR(128)` |
| `spid` | `name="spid", length=64` | ❌ 缺失 | `ADD COLUMN IF NOT EXISTS spid VARCHAR(64)` |

## 3. 修改计划

### 3.1 唯一修改文件

| 文件 | 操作 | 位置 |
|------|------|------|
| `database/schema/001_init_schema.sql` | **修改** | alarm_record CREATE INDEX 之后，下一个 CREATE TABLE 之前，追加 3 条 ALTER TABLE |

### 3.2 追加内容

```sql
-- LANDING-009: alarm_record 字段补齐 (serial_no/device_id 来自 LANDING-001, spid 来自 LANDING-008)
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS serial_no VARCHAR(128);
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS device_id VARCHAR(128);
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS spid VARCHAR(64);
```

### 3.3 不修改范围

- CREATE TABLE alarm_record 本身不动（避免合并风险）
- 不修改索引
- 不修改外键
- 不新增 default 值

## 4. 验证

```bash
# 1. 确认 DDL 语句存在
grep -n "ADD COLUMN IF NOT EXISTS serial_no" database/schema/001_init_schema.sql
grep -n "ADD COLUMN IF NOT EXISTS device_id" database/schema/001_init_schema.sql
grep -n "ADD COLUMN IF NOT EXISTS spid" database/schema/001_init_schema.sql

# 2. Entity ↔ DDL 字段一致性检查（人工逐项核对）
# serial_no: Entity length=128, DDL VARCHAR(128) ✅
# device_id: Entity length=128, DDL VARCHAR(128) ✅
# spid:      Entity length=64,  DDL VARCHAR(64)  ✅

# 3. 测试回归
mvn test -Dtest='*Alarm*'
mvn test -Dtest='*BInterface*'
mvn test

# 4. 文件变更确认
git diff -- database/
git status --short
```

## 5. 输出文档

| # | 文件 | 说明 |
|---|------|------|
| 1 | `openspec/specs/landing-009-alarm-record-ddl-readiness.md` | Spec（本 plan 对应） |
| 2 | `openspec/plans/landing-009-alarm-record-ddl-readiness-plan.md` | Plan（本文件） |
| 3 | `docs/audit/LANDING-009-alarm-record-ddl-readiness.md` | 审计文档 |
| 4 | `docs/memory/2026-05-20-LANDING-009-alarm-record-ddl-readiness.md` | 工程记忆 |
| 5 | `docs/memory/README.md` | 索引更新 |
| 6 | `docs/memory/WORKING-MEMORY.md` | 工作记忆更新 |

## 6. 完成条件

| # | 条件 | 状态 |
|---|------|------|
| 1 | spec 已创建 | ← 待执行 |
| 2 | plan 已创建 | ← 本文件 |
| 3 | 3 条 ALTER TABLE 已追加到 schema SQL | 待执行 |
| 4 | 字段名/类型/长度与 Entity 一致 | 待核对 |
| 5 | 全量测试无回归 | 待执行 |
| 6 | 未修改 Java 代码 | 待执行 |
| 7 | memory + audit 已写入 | 待执行 |
