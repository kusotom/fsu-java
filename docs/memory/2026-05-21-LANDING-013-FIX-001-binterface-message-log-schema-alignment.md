# 工程记忆：LANDING-013-FIX-001 b_interface_message_log Entity/Table 对齐

## 1. 时间
2026-05-21

## 2. 背景

真实 FSU 已通过 `POST /services/SCService` 进入平台 B接口处理链路，LOGIN 业务成功，但报文日志保存失败：

```text
ERROR: null value in column "command_code" of relation "b_interface_message_log" violates not-null constraint
```

## 3. 本次目标

执行最小数据库修复：

```sql
ALTER TABLE b_interface_message_log
ALTER COLUMN command_code DROP NOT NULL;
```

同时确认真实表结构、schema 文件、测试和真实 FSU LOGIN 日志入库结果。

## 4. 架构判断

本次属于 B接口报文日志持久化表结构对齐，不改变协议解析、命令分发、LOGIN 业务、SET、Scheduler 或告警状态机。`BInterfaceMessageLogEntity` 当前写入 `command`，不写入历史列 `command_code`，因此通过 DDL 放宽历史列约束是最小修复。

## 5. 协议一致性判断

不修改 B接口 SOAP/XMLData、PK_Type、FsuCode、DeviceID、SignalID 或 ResultCode。真实入站报文仍按 B接口 2016 LOGIN：`PK_Type Name=LOGIN Code=101` 处理。

## 6. 实际修改文件

| 文件 | 操作 |
|------|------|
| `database/schema/001_init_schema.sql` | 规范 LANDING-013-FIX-001 幂等 DDL 注释 |
| `docs/audit/LANDING-013-FIX-001-binterface-message-log-schema-alignment.md` | 补充现场 DDL、测试、真实 FSU 验证结果 |
| `docs/memory/2026-05-21-LANDING-013-FIX-001-binterface-message-log-schema-alignment.md` | 写入本次执行记忆 |
| `docs/memory/README.md` | 更新索引摘要 |
| `docs/memory/WORKING-MEMORY.md` | 更新当前阶段与验证结论 |

## 7. 数据库执行结果

DDL 前：

```text
command_code | NO
```

执行：

```text
ALTER TABLE
```

DDL 后：

```text
command_code | YES
```

完整列核对后，未发现其他 Entity 不写入但仍 `NOT NULL` 的历史列。

## 8. 关键决策

1. 不删除 `command_code` 字段，保留历史兼容。
2. 不修改 Java，避免扩大业务逻辑范围。
3. 通过真实 DB DDL + schema 后置幂等 DDL 双重对齐。
4. 等待 FSU 自然重登验证入站 LOGIN，不主动 GET_* 访问真实 FSU。

## 9. 测试结果

```text
mvn test -Dtest='*BInterfaceMessageLog*'  -> 33 tests, 0 failures, 0 errors, 0 skipped
mvn test -Dtest='*ScService*'             -> 12 tests, 0 failures, 0 errors, 0 skipped
mvn test -Dtest='*BInterface*'            -> 149 tests, 0 failures, 0 errors, 0 skipped
mvn test                                  -> 1204 tests, 0 failures, 0 errors, 5 skipped
```

## 10. 真实 FSU 验证

DDL 后等待 FSU 再次 LOGIN，平台日志出现：

```text
2026-05-21T08:46:18.194+08:00 INFO ... 报文日志已保存: command=LOGIN fsuCode=51051243812345 id=399 direction=INBOUND
2026-05-21T08:46:18.202+08:00 INFO ... LOGIN 成功: fsuCode=51051243812345, sessionId=SESSION-51051243812345-928554E07D00
2026-05-21T08:47:25.595+08:00 INFO ... 报文日志已保存: command=LOGIN fsuCode=51051243812345 id=400 direction=INBOUND
2026-05-21T08:47:25.606+08:00 INFO ... LOGIN 成功: fsuCode=51051243812345, sessionId=SESSION-51051243812345-B44E96E52B33
```

API 查询 `GET /api/b-interface/message-logs/query?command=LOGIN&page=0&size=5` 返回 `content` 非空：

| 字段 | 验证结果 |
|------|----------|
| command | LOGIN |
| fsuCode | 51051243812345 |
| direction | INBOUND |
| messageType | SOAP |
| rawMessage | 完整 SOAP Envelope，包含 LOGIN Code=101 与 DeviceList |

后续为恢复 8080 API 监听执行过一次平台重启，`/tmp/fsu-start.log` 被重新生成；当前验收以 API/DB 保留的 `id=399`、`id=400` 真实 LOGIN 记录为准。

## 11. 安全边界

- 未执行 SET。
- 未启用 Scheduler。
- 未主动执行 GET_* 真实 FSU 调用。
- 未修改 `alarm_record` 状态机。
- 未删除 `command_code` 字段。
- 未删除历史数据。
- 未修改 `LoginService` 业务语义。

## 12. git diff 摘要

- `database/schema/001_init_schema.sql`：LANDING-013-FIX-001 后置 DDL 注释规范为任务指定文本，保留 `ALTER COLUMN command_code DROP NOT NULL`。
- `docs/audit/...LANDING-013-FIX-001...md`：补充现场 DDL、nullable 验证、测试结果和真实 FSU 入库证据。
- `docs/memory/...LANDING-013-FIX-001...md`：补充本次执行记忆、测试和安全边界。

## 13. 遗留问题

1. 当前测试使用 dev profile，会连接本地 PostgreSQL；这是既有项目测试配置，本次未调整。
2. `database/schema/001_init_schema.sql` 的初始表定义仍保留历史 `command_code` 列，依靠后置幂等 DDL 放宽约束；本次按最小修复未重构历史表定义。

## 14. 下一步建议

1. 后续单独评估测试 profile 隔离，避免集成测试写入 dev PostgreSQL。
2. 后续可评估将 `b_interface_message_log` 初始化表定义整体整理为当前 Entity + 查询需求一致的结构，但不应并入本次热修。
