# 工程记忆：LANDING-012 新增 /services/SCService B接口标准兼容接收入口

## 1. 时间
2026-05-21

## 2. 背景

现场 tcpdump 确认真实 FSU 以 gSOAP 客户端按 B接口 WSDL 标准路径 `POST /services/SCService` 向平台发起上报。平台只有 `POST /api/b-interface/sc-service`，导致返回 JSON 而非 SOAP ACK。

## 3. 本次目标

新增标准兼容入口 `POST /services/SCService`：
1. 复用现有 ScServiceProcessor 处理逻辑
2. 返回 SOAP XML（非 JSON）
3. 保持 `/api/b-interface/sc-service` 兼容

## 4. 修改文件

### 新增

| 文件 | 说明 |
|------|------|
| `ScServiceProcessor.java` | 核心 SOAP 处理（parse→log→dispatch→buildResponse） |
| `StandardScServiceController.java` | @RequestMapping("/services") + @PostMapping("/SCService") |
| `StandardScServiceControllerTest.java` | 5 tests (Spring Boot + MockMvc) |

### 修改

| 文件 | 操作 |
|------|------|
| `ScServiceController.java` | 重构：委托 ScServiceProcessor |
| `docs/memory/README.md` | 索引更新 |
| `docs/memory/WORKING-MEMORY.md` | 当前阶段更新 |

## 5. 关键决策

1. **提取 Processor 而非复制代码**: 两个入口共享 `ScServiceProcessor.process()`
2. **Processor 内 try-catch**: 防止 GlobalExceptionHandler 返回 JSON
3. **`@RestController` + `produces=TEXT_XML`**: 确保 SOAP XML Content-Type
4. **旧入口兼容**: ScServiceController 接口签名和路径不变

## 6. 测试结果

```
*ScService*:              12 tests, PASS (7 Rpc + 5 StandardScService)
*BInterfaceMessageLog*:   33 tests, PASS
*BInterface*:            154 tests, PASS
全量:                   1204 tests, 0/0/5
contextLoads:            PASS
```

- 不访问真实 FSU
- 不执行 SET
- 不启用 Scheduler

## 7. 真实 FSU 样本验证

测试中使用 tcpdump 真实 LOGIN SOAP 样本（FsuId=51051243812345, 5 DeviceIDs, MacId），验证：
- HTTP 200 + Content-Type: text/xml
- 响应包含 SOAP Envelope（非 JSON）
- 两入口返回一致 SOAP 格式
- BInterfaceMessageLog 记录入站报文

## 8. 遗留问题

1. FSU 未在 fsu_device 注册 → LOGIN ACK 返回 FSU 未注册错误（需现场在数据库注册后重试）
2. Codex 复审暂缓

## 9. 下一步建议

1. **立即**: 现场在 fsu_device 表注册 51051243812345 后重启平台
2. 观察 FSU 重新 LOGIN → 应收到成功 ACK (SessionID)
3. 后续观察 HEARTBEAT / SEND_DATA / SEND_ALARM 上报
4. 提交 Codex 集中复审
