# 工程记忆：LANDING-010 BInterfaceMessageLog 日志清理与查询增强

## 1. 时间
2026-05-20

## 2. 背景

LANDING-008 实现了报文日志保存（saveInbound/saveOutbound），但查询仅支持非分页列表，且无日志清理能力。真实联调前需要补齐分页查询和多条件过滤，以及手动日志清理。

## 3. 本次目标

1. Repository 新增分页查询（direction/command/fsuCode/messageType）和 deleteByCreatedAtBefore
2. Service 新增 query() / cleanBefore() / cleanOlderThanDays()
3. Controller 新增分页查询 GET /query 和清理 DELETE /cleanup 端点
4. 保留旧接口 GET / 和 GET /{id} 向后兼容

## 4. 修改文件

| 文件 | 操作 | 新增测试 |
|------|------|----------|
| `BInterfaceMessageLogRepository.java` | 修改 (+5 方法) | — |
| `BInterfaceMessageLogService.java` | 修改 (+3 方法) | — |
| `BInterfaceMessageLogController.java` | 重写 (+2 端点, 旧兼容) | — |
| `BInterfaceMessageLogServiceTest.java` | 扩展 | +12 (23 total) |
| `BInterfaceMessageLogControllerTest.java` | **新增** | 10 |

## 5. 关键决策

1. **分页上限**: size ≤ 100，防止大报文列表 OOM
2. **无 DTO 层**: 本阶段保持 Entity 直接返回（后续可优化列表返回摘要 + rawMessageLength）
3. **清理仅手动**: 不实现 @Scheduled 自动清理
4. **旧接口兼容**: list() 标记 @Deprecated 建议使用 query()，但保留可用
5. **query() 单条件过滤**: 按优先级匹配 direction > command > fsuCode > messageType > 无条件
6. **safe-save 不变**: saveInbound/saveOutbound 的 try-catch 不抛异常机制保持

## 6. 测试结果

```
*BInterfaceMessageLog*:  33 tests, 0 failures, 0 errors (Service 23 + Controller 10)
*BInterface*:           149 tests, 0 failures, 0 errors
全量:                  1199 tests, 0 failures, 0 errors, 5 skipped
```

- 不访问真实 FSU (StubFsuServiceClient)
- 不执行 SET
- 不启用 Scheduler

## 7. API 变更

| 端点 | 方法 | 说明 |
|------|------|------|
| `GET /api/b-interface/message-logs` | list() | 保留 (@Deprecated) |
| `GET /api/b-interface/message-logs/{id}` | getById() | 保留 |
| `GET /api/b-interface/message-logs/query` | query() | **新增** 分页多条件查询 |
| `DELETE /api/b-interface/message-logs/cleanup` | cleanupByDays() | **新增** 按天数清理 |

## 8. 遗留问题

1. 列表查询返回完整 rawMessage（建议后续加 DTO 返回摘要 + rawMessageLength）
2. 多条件组合查询未实现（当前为单条件优先级匹配）
3. 时间范围查询未在 Repository 层实现（Service query() 不支持 startTime/endTime）
4. Codex 复审暂缓

## 9. 下一步建议

1. 后续可优化列表返回 DTO（id/direction/command/fsuCode/messageType/createdAt/rawMessageLength）
2. 管理方提供 DeviceID/SPID 映射表 → 导入 monitoring_point
3. FSU 侧配置 SC 上报目标后启动被动上报联调
4. 提交 Codex 集中复审
