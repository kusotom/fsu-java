# LANDING-010 Spec：BInterfaceMessageLog 日志清理与查询增强

> 规格版本：1.0
> 创建日期：2026-05-20
> 对应计划：`openspec/plans/landing-010-binterface-message-log-retention-plan.md`

---

## 1. 背景

LANDING-008 实现了 `BInterfaceMessageLogService` 的入站报文保存能力（saveInbound/saveOutbound），但查询能力较基础（仅 findByFsuCode/findByCommand 列表查询），且缺少日志清理策略。

当前 `BInterfaceMessageLogRepository` 只有两个查询方法，不支持分页、时间范围、direction/messageType 过滤。`BInterfaceMessageLogController` 只有 `GET /list` 和 `GET /{id}` 两个端点。

真实联调前需要补齐：
- 分页查询（避免大报文列表 OOM）
- 多条件过滤（direction/command/fsuCode/messageType/时间范围）
- 日志清理能力（手动触发，不自动）

## 2. 目标

1. **Repository 增强**：新增分页查询方法（按 direction/command/fsuCode/messageType）和 `deleteByCreatedAtBefore`
2. **Service 增强**：新增 `query()` 分页查询、`cleanBefore()` 按时间清理、`cleanOlderThanDays()` 按天数清理
3. **Controller 增强**：保留现有 `GET /list` 和 `GET /{id}`，新增分页查询 GET 端点 + 清理 DELETE 端点
4. **清理安全**：必须显式调用，不自动执行，不触碰业务数据

## 3. 非目标

- 不访问真实 FSU
- 不执行 SET
- 不启用 Scheduler
- 不修改 alarm_record 状态机
- 不删除 alarm_record / telemetry / monitoring_point 数据
- 不自动定时清理日志
- 不大范围重构 BInterfaceMessageLogQueryService（保留兼容）
- 不新增 DTO 楼层（保持 Entity 直接返回）
- 不改变 SCService ACK 链路

## 4. 验收标准

| # | 验收项 |
|---|--------|
| 1 | Repository 支持分页查询（direction/command/fsuCode/messageType） |
| 2 | Repository 支持 `deleteByCreatedAtBefore` |
| 3 | Service `query(...)` 支持分页 + 多条件过滤 |
| 4 | Service `cleanBefore(cutoff)` 只删除日志表 |
| 5 | Service `cleanOlderThanDays(days)` days>0，否则抛异常 |
| 6 | Controller 保留现有 GET / 和 GET /{id}（向后兼容） |
| 7 | Controller 新增分页查询 GET 端点 |
| 8 | Controller 新增 DELETE 清理端点 |
| 9 | 查询/清理失败不影响 SCService ACK |
| 10 | 全量测试：0 failures, 0 errors |
| 11 | 默认不访问真实 FSU，不执行 SET，不启用 Scheduler |
| 12 | memory + audit 已写入 |

## 5. 安全边界

| 边界 | 状态 |
|------|------|
| 真实 FSU 访问 | **否** |
| SET 命令 | **否** |
| Scheduler | **否** |
| alarm_record 状态机 | **否** |
| 自动清理 | **否** — 只支持手动调用 |
| 业务数据删除 | **否** — 仅 binterface_message_log |
| Java 生产代码修改范围 | Repository + Service + Controller（3 文件） |
| Codex 复审 | **暂缓** |

## 6. 风险

| # | 风险 | 可能性 | 缓解 |
|---|------|--------|------|
| 1 | 大报文查询慢 | 中 | 分页限制 size<=100；rawMessage 已在 Entity 中 |
| 2 | 清理误删 | 低 | deleteByCreatedAtBefore 仅删日志表，不级联 |
| 3 | olderThanDays <= 0 | 低 | 参数校验抛 IllegalArgumentException |
| 4 | 与 BInterfaceMessageLogQueryService 冲突 | 低 | 新方法加在 BInterfaceMessageLogService，QueryService 保留不变 |
