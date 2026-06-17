# LANDING-013 Spec：真实 FSU LOGIN 成功但 BInterfaceMessageLog 未入库排查修复

> 规格版本：1.0 | 创建日期：2026-05-21

## 1. 背景

真实 FSU 通过 POST /services/SCService LOGIN 成功（session 创建、fsu_device 更新、b_interface_fsu_status 更新），但 `GET /api/b-interface/message-logs/query?command=LOGIN` 返回 `content=[]`。

## 2. 根因分析

代码追踪确认：

```
ScServiceProcessor.process()
  → logInboundMessage()           // 内层 try-catch，异常被 warn 吞掉
    → saveInbound(command, fsuCode, rawMessage)
      → save()                    // 内层 try-catch，异常被 error 吞掉
        → repository.save(entity) // 无 @Transactional，可能未 flush
```

**双重静默吞异常**：
1. `save()` 的 catch 块吞掉所有 JPA 异常（仅 log.error）
2. `logInboundMessage()` 的 catch 块再次吞掉异常（仅 log.warn）

**无事务边界**：`BInterfaceMessageLogService` 和 `ScServiceProcessor` 均无 `@Transactional`，`repository.save()` 可能不立即 flush。

**缺少成功日志**：save 成功时无日志输出，无法确认是否执行。

## 3. 目标

1. 修复 save 不持久化的问题
2. 增加可见性（成功/失败日志）
3. 确保 saveAndFlush 立即持久化
4. 新增集成测试覆盖真实 LOGIN SOAP 样本入库

## 4. 非目标

- 不修改 ScServiceProcessor 处理流程
- 不影响 LOGIN 业务
- 不影响 SOAP ACK

## 5. 验收标准

| # | 验收项 |
|---|--------|
| 1 | saveInbound 调用 repository.saveAndFlush() |
| 2 | save() 成功时输出 info 日志 |
| 3 | 测试验证真实 LOGIN SOAP 入库后 command=LOGIN |
| 4 | query?command=LOGIN 可查到数据 |
| 5 | 全量测试 0/0/5 |

## 6. 安全边界

SET/Scheduler/GET_*/alarm_record: 不涉及。Codex 复审：暂缓。
