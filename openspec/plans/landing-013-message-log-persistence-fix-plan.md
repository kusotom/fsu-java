# LANDING-013 Plan

## 1. 修复

BInterfaceMessageLogService.save():
- `repository.save(entity)` → `repository.saveAndFlush(entity)`
- 成功时 `log.info("报文日志已保存: direction={} command={} fsuCode={}")`
- 保留 try-catch（不抛异常，不阻塞 ACK）

## 2. 测试

ScServiceProcessorTest (单元测试，注入 stub 依赖):
- 真实 LOGIN SOAP 样本入库后 command=LOGIN, fsuCode=51051243812345
- save 时 rawMessage 包含完整 SOAP Envelope

StandardScServiceControllerTest 增强:
- shouldSaveLoginMessageLog 验证 messageLogRepository 可查到

## 3. 验证

```bash
mvn test -Dtest='*ScService*'
mvn test -Dtest='*BInterfaceMessageLog*'
mvn test -Dtest='*BInterface*'
mvn test
```

## 4. 文档

audit + memory + 索引更新。
