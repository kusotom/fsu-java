# 审计文档：LANDING-012 新增 /services/SCService B接口标准兼容接收入口

## 1. 审计日期
2026-05-21

## 2. 审计类型
新增 HTTP 入口（零协议变更，复用现有处理逻辑）

## 3. 审计目标

审校 `/services/SCService` POST 入口是否正确复用现有处理链路，是否返回 SOAP XML（非 JSON），旧入口是否保持兼容。

## 4. tcpdump 真实证据

```
POST /services/SCService HTTP/1.1
Host: 192.168.100.123:8080
User-Agent: gSOAP/2.8
Content-Type: text/xml; charset=utf-8
SOAPAction: ""
```

FSU gSOAP 客户端以 B接口 WSDL 声明的标准路径 `/services/SCService` 发起上报。平台缺少此映射。

## 5. 问题根因

| 项目 | 状态 |
|------|------|
| FSU 请求路径 | POST /services/SCService |
| 平台已有 POST 路径 | POST /api/b-interface/sc-service |
| /services/SCService POST 映射 | **无** |
| 无映射行为 | Spring 404 → GlobalExceptionHandler 返回 JSON ApiResponse |
| FSU 期望 | SOAP XML ACK |
| **根因** | 平台缺少 /services/SCService POST 映射 |

## 6. 修改范围

### 6.1 新增文件

| 文件 | 说明 |
|------|------|
| `ScServiceProcessor.java` | 提取核心 SOAP 处理逻辑（parse→log→dispatch→buildResponse），供多个入口复用 |
| `StandardScServiceController.java` | `@RequestMapping("/services")` + `@PostMapping("/SCService")`，委托 ScServiceProcessor |
| `StandardScServiceControllerTest.java` | 5 tests: 真实 LOGIN SOAP 验证，两入口一致性，非 JSON 断言，旧入口兼容，报文日志 |

### 6.2 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `ScServiceController.java` | 重构 | 委托 ScServiceProcessor，接口签名不变 |

## 7. 架构

```
ScServiceProcessor (@Service)
  └─ process(requestBody) → String (SOAP XML)
      ├─ SoapMessageHandler.parse()
      ├─ BInterfaceMessageLogService.saveInbound() (best-effort)
      ├─ CommandDispatcher.dispatch()
      └─ SoapMessageHandler.buildResponse() / buildFault()

POST /api/b-interface/sc-service  → ScServiceController  → processor.process()
POST /services/SCService          → StandardScServiceController → processor.process()
```

## 8. 关键设计决策

| 决策 | 理由 |
|------|------|
| 提取 Processor 而非复制代码 | 两个入口共享同一套处理逻辑 |
| Processor 内 try-catch 所有异常 | 防止 GlobalExceptionHandler 返回 JSON |
| 使用 @RestController + produces=TEXT_XML | 确保 Content-Type 为 text/xml |
| 不修改旧 ScServiceController 接口签名 | 保持向后兼容 |

## 9. 安全边界

| 边界 | 状态 |
|------|------|
| SET 命令 | 未执行 |
| Scheduler | 未启用 |
| 真实 FSU 访问 | 未访问 |
| `/api/b-interface/sc-service` | 保留兼容 |
| WSDL GET | 不影响 |
| JSON 响应 | `/services/SCService` 不返回 JSON |

## 10. 测试结果

```
*ScService*:              12 tests, 0/0/0 (7 RpcCompatibility + 5 StandardScService)
*BInterfaceMessageLog*:   33 tests, PASS
*BInterface*:            154 tests, PASS
全量:                   1204 tests, 0/0/5
contextLoads:            PASS
```

## 11. 结论

**通过。** `/services/SCService` 标准入口已新增：
- 复用现有 ScServiceProcessor 处理逻辑
- 返回 SOAP XML（非 JSON）
- `/api/b-interface/sc-service` 保持兼容
- 全量测试 1204/0/0/5 无回归
- 不执行 SET，不启用 Scheduler
