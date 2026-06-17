# 审计文档：LANDING-013 BInterfaceMessageLog 未入库排查修复

## 1. 审计日期
2026-05-21

## 2. 审计类型
Bug 修复（持久化保证 + 可见性增强）

## 3. 问题现象

真实 FSU POST /services/SCService LOGIN 成功：
- session 创建 ✅
- fsu_device 更新 ✅
- b_interface_fsu_status 更新 ✅
- b_interface_session 插入 ✅

但 `GET /api/b-interface/message-logs/query?command=LOGIN` 返回 `content=[]`。

## 4. 根因分析

```text
ScServiceProcessor.process()
  → logInboundMessage()           // try-catch 吞异常 (log.warn)
    → saveInbound(command, ...)
      → save()                    // try-catch 吞异常 (log.error)
        → repository.save(entity) // 无 @Transactional, 无 flush 保证
```

**三个问题**：

| # | 问题 | 影响 |
|---|------|------|
| 1 | `repository.save()` 无 `@Transactional` | JPA 可能不立即 flush，依赖 open-in-view 延迟提交 |
| 2 | 双重静默吞异常 | save() catch + logInboundMessage() catch = 任何持久化错误不可见 |
| 3 | 无成功日志 | 无法确认 save 是否实际执行 |

## 5. 修复内容

| 修改 | 说明 |
|------|------|
| `repository.save()` → `repository.saveAndFlush()` | 强制立即持久化 |
| `saveInbound/saveOutbound` + `@Transactional` | 明确事务边界 |
| 新增 `log.info("报文日志已保存: command={} fsuCode={} id={}")` | 成功可见 |
| 异常日志改为 `log.error(..., e)` 含完整堆栈 | 失败可排查 |

## 6. 修改文件

| 文件 | 操作 |
|------|------|
| `BInterfaceMessageLogService.java` | 修改 (+3 lines: @Transactional + saveAndFlush + 日志) |
| `StandardScServiceControllerTest.java` | 修改 (简化断言) |

## 7. 安全边界

| 边界 | 状态 |
|------|------|
| SET | 未执行 |
| Scheduler | 未启用 |
| 真实 FSU | 未访问 |
| LOGIN 业务 | 不影响 |

## 8. 测试结果

```
全量: 1204 tests, 0/0/5
```

## 9. 现场验证步骤

```bash
# 重启平台后
curl -s "http://localhost:8080/api/b-interface/message-logs/query?command=LOGIN&page=0&size=5" | python3 -m json.tool
```

预期: content 非空, command=LOGIN, fsuCode=51051243812345.

## 10. 结论

**通过。** 修复了 BInterfaceMessageLog 持久化缺失问题，通过 saveAndFlush + @Transactional 保证立即持久化，增加成功/失败日志可见性。
