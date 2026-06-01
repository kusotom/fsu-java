# 工程记忆：LANDING-013 BInterfaceMessageLog 未入库排查修复

## 1. 时间
2026-05-21

## 2. 背景

真实 FSU LOGIN 成功但 BInterfaceMessageLog 查询为空。

## 3. 根因

`BInterfaceMessageLogService.save()`:
- 使用 `repository.save()` 无 `@Transactional`，JPA 不保证立即 flush
- 双重 try-catch 静默吞异常（save() + logInboundMessage()）
- 无成功日志，无法确认执行

## 4. 修复

| 变更 | 说明 |
|------|------|
| `save()` → `saveAndFlush()` | 强制立即持久化 |
| `saveInbound/saveOutbound` + `@Transactional` | 明确事务边界 |
| 新增 `log.info("报文日志已保存: command={} fsuCode={} id={}")` | 成功可见 |
| 错误日志含完整 stacktrace | 失败可排查 |

## 5. 测试

1204 tests, 0/0/5 — 基线保持。

## 6. 现场验证

重启后 FSU LOGIN → 查询 `GET /api/b-interface/message-logs/query?command=LOGIN` → content 非空。

## 7. 遗留

- 建议在 `save()` 中使用 `REQUIRES_NEW` 确保独立事务（当前 @Transactional 使用默认 REQUIRED）。现场验证后如仍有问题再升级。
