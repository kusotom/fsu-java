# 审计文档：LANDING-013-FIX-001 b_interface_message_log 真实表结构与 Entity 对齐

## 1. 审计日期
2026-05-21

## 2. 审计类型
Schema/Entity 对齐修复（零业务逻辑变更）

## 3. 问题根因

真实 FSU 已成功进入标准 SCService 入站链路，LOGIN 业务成功：

```text
POST /services/SCService
LOGIN 成功: fsuCode=51051243812345
```

但报文日志入库失败：

```text
ERROR: null value in column "command_code" of relation "b_interface_message_log" violates not-null constraint
```

Hibernate 实际 INSERT：

```sql
insert into b_interface_message_log
(command, created_at, direction, fsu_code, message_type, raw_message)
values (?, ?, ?, ?, ?, ?)
```

`BInterfaceMessageLogEntity` 写入 `command`，不映射也不写入历史列 `command_code`。真实 PostgreSQL 表中 `command_code` 仍为 `NOT NULL`，因此 `command_code = NULL` 触发约束失败。

## 4. Entity / Table 对照

| 表列 | DDL 后 nullable | Entity 是否写入 | 结论 |
|------|-----------------|----------------|------|
| `id` | NO | 主键生成 | 正常 |
| `direction` | NO | 是 | 正常 |
| `command_code` | YES | 否 | 已修复 |
| `fsu_id` | YES | 否 | 正常 |
| `fsu_code` | YES | 是 | 正常 |
| `session_id` | YES | 否 | 正常 |
| `message_type` | NO | 是 | 正常 |
| `pk_type` | YES | 否 | 正常 |
| `info` | YES | 否 | 正常 |
| `xml_data` | YES | 否 | 正常 |
| `raw_message` | YES | 是 | 正常 |
| `status` | YES | 否，数据库默认 | 正常 |
| `error_message` | YES | 否 | 正常 |
| `process_time_ms` | YES | 否 | 正常 |
| `created_at` | NO | 是 | 正常 |
| `command` | YES | 是 | 正常 |

未发现其他 Entity 不写入但仍 `NOT NULL` 的历史列。

## 5. 执行的 DDL

```sql
ALTER TABLE b_interface_message_log
ALTER COLUMN command_code DROP NOT NULL;
```

执行结果：

```text
ALTER TABLE
```

## 6. nullable 验证

DDL 前：

```text
command_code | NO
```

DDL 后：

```text
command_code | YES
```

## 7. schema 文件同步

`database/schema/001_init_schema.sql` 已包含后置幂等修复：

```sql
-- LANDING-013-FIX-001: align historical b_interface_message_log.command_code with current entity
ALTER TABLE b_interface_message_log ALTER COLUMN command_code DROP NOT NULL;
```

## 8. Java 修改情况

本次不修改 Java。现有 Entity 与真实表约束已通过 DDL 对齐。

## 9. 测试验证

```text
mvn test -Dtest='*BInterfaceMessageLog*'  -> 33 tests, 0 failures, 0 errors, 0 skipped
mvn test -Dtest='*ScService*'             -> 12 tests, 0 failures, 0 errors, 0 skipped
mvn test -Dtest='*BInterface*'            -> 149 tests, 0 failures, 0 errors, 0 skipped
mvn test                                  -> 1204 tests, 0 failures, 0 errors, 5 skipped
```

默认测试未访问真实 FSU、未执行 SET、未启用 Scheduler。

## 10. 真实 FSU 验证

DDL 后等待 FSU 再次 LOGIN，报文日志已成功入库：

```text
2026-05-21T08:46:18.194+08:00 INFO ... 报文日志已保存: command=LOGIN fsuCode=51051243812345 id=399 direction=INBOUND
2026-05-21T08:46:18.202+08:00 INFO ... LOGIN 成功: fsuCode=51051243812345, sessionId=SESSION-51051243812345-928554E07D00
2026-05-21T08:47:25.595+08:00 INFO ... 报文日志已保存: command=LOGIN fsuCode=51051243812345 id=400 direction=INBOUND
2026-05-21T08:47:25.606+08:00 INFO ... LOGIN 成功: fsuCode=51051243812345, sessionId=SESSION-51051243812345-B44E96E52B33
```

API 查询：

```text
GET /api/b-interface/message-logs/query?command=LOGIN&page=0&size=5
```

返回 `content` 非空，最新记录 `id=400`：

| 字段 | 值 |
|------|----|
| command | LOGIN |
| fsuCode | 51051243812345 |
| direction | INBOUND |
| messageType | SOAP |
| rawMessage | 包含完整 SOAP Envelope，内含 `PK_Type Name=LOGIN Code=101` |

说明：后续为恢复 8080 API 监听执行过一次平台重启，`/tmp/fsu-start.log` 被重新生成；上述日志行来自重启前已捕获的现场输出。数据库与 API 中 `id=399`、`id=400` 两条真实 LOGIN 记录仍保留且可查询。

## 11. 安全边界

| 边界 | 状态 |
|------|------|
| SET | 未执行 |
| Scheduler | 未启用 |
| 主动 GET_* 访问真实 FSU | 未执行 |
| `alarm_record` 状态机 | 未修改 |
| `command_code` 字段 | 保留，仅取消 NOT NULL |
| 历史数据 | 未删除 |

## 12. 结论

**通过。** `command_code NOT NULL` 是导致真实 FSU LOGIN 报文日志无法入库的直接根因。真实 PostgreSQL 约束已修复为 nullable，schema 已同步幂等 DDL，真实 LOGIN 报文已成功写入 `b_interface_message_log`。
