# 审计文档：LANDING-010 BInterfaceMessageLog 日志清理与查询增强

## 1. 审计日期
2026-05-20

## 2. 审计类型
查询增强 + 日志清理（零协议变更）

## 3. 审计目标

审核 BInterfaceMessageLog 的查询增强和清理能力是否正确实现，确认安全边界。

## 4. 背景

LANDING-008 实现了报文日志保存能力，但查询仅支持非分页的 findByFsuCode/findByCommand，且无清理能力。真实联调前需要补齐分页查询和手动清理能力。

## 5. 修改范围

### 5.1 生产代码 (3 文件)

| 文件 | 变更 |
|------|------|
| `BInterfaceMessageLogRepository.java` | +4 分页查询, +1 deleteByCreatedAtBefore |
| `BInterfaceMessageLogService.java` | +query(), +cleanBefore(), +cleanOlderThanDays() |
| `BInterfaceMessageLogController.java` | 重写: 保留 GET / + GET /{id}, +GET /query, +DELETE /cleanup |

### 5.2 测试代码 (2 文件)

| 文件 | 变更 |
|------|------|
| `BInterfaceMessageLogServiceTest.java` | +12 tests (23 total) |
| `BInterfaceMessageLogControllerTest.java` | **新增** 10 tests |

## 6. 接口兼容性

| 原端点 | 方法 | 行为 |
|--------|------|------|
| `GET /api/b-interface/message-logs` | list() | 保留，标记 @Deprecated |
| `GET /api/b-interface/message-logs/{id}` | getById() | 保留，不变 |

| 新端点 | 方法 | 说明 |
|--------|------|------|
| `GET /api/b-interface/message-logs/query?page=0&size=20&direction=INBOUND&command=SEND_ALARM&fsuCode=xxx&messageType=SOAP` | query() | 分页多条件查询 |
| `DELETE /api/b-interface/message-logs/cleanup?olderThanDays=30` | cleanupByDays() | 手动清理 (days>0) |

## 7. 查询增强

| 能力 | 实现 |
|------|------|
| 分页 | Pageable (默认 page=0, size=20, max size=100) |
| direction 过滤 | findByDirectionOrderByCreatedAtDesc |
| command 过滤 | findByCommandOrderByCreatedAtDesc |
| fsuCode 过滤 | findByFsuCodeOrderByCreatedAtDesc |
| messageType 过滤 | findByMessageTypeOrderByCreatedAtDesc |
| 无条件查询 | findAll(Pageable) |
| 查询失败 | 返回空 Page，不抛异常 |

## 8. 清理策略

| 能力 | 实现 |
|------|------|
| 按时间清理 | cleanBefore(LocalDateTime cutoff) |
| 按天数清理 | cleanOlderThanDays(int days) |
| 参数校验 | days <= 0 → IllegalArgumentException |
| 清理范围 | 仅 binterface_message_log |
| 自动清理 | 无 |
| Scheduler | 不涉及 |
| 清理失败 | 返回 0，不抛异常 |

## 9. 安全边界

| 边界 | 状态 |
|------|------|
| 真实 FSU | 未访问 |
| SET | 未执行 |
| Scheduler | 未启用 |
| alarm_record 状态机 | 未修改 |
| 业务数据 | 未触碰 (仅 binterface_message_log) |
| 自动清理 | 未实现 |
| 旧接口 | 保持兼容 |
| 分页上限 | size ≤ 100 |

## 10. 测试结果

```
*BInterfaceMessageLog*:  33 tests, 0/0/0 (Service 23 + Controller 10)
*BInterface*:           149 tests, 0/0/0 (127 → 149, +22)
全量:                  1199 tests, 0/0/5 (1177 → 1199, +22)
```

## 11. 结论

**通过。** 查询增强和清理能力实现完整、安全：

- 分页查询 + 多条件过滤就绪
- 清理仅删日志表，不影响业务数据
- 旧接口保持兼容
- 全量测试 1199/0/0/5 无回归
- 未访问真实 FSU，未执行 SET，未启用 Scheduler
