# LANDING-012 Plan：新增 /services/SCService B接口标准兼容接收入口

> 计划版本：1.0
> 创建日期：2026-05-21
> 对应 Spec：`openspec/specs/landing-012-standard-scservice-path.md`

---

## 1. 读取资料

| # | 文件 | 用途 |
|---|------|------|
| 1 | `ScServiceController.java` | 现有处理逻辑 |
| 2 | `GlobalExceptionHandler.java` | JSON 异常处理（需避开） |
| 3 | `SoapMessageHandler.java` | SOAP 构建 |
| 4 | `BInterfaceMessageLogService.java` | 报文日志 |

## 2. 设计

提取共享处理逻辑 → 新增 StandardScServiceController：

```
ScServiceProcessor (@Service, new)
  └─ process(requestBody) → String (SOAP XML)
      ├─ SoapMessageHandler.parse()
      ├─ BInterfaceMessageLogService.saveInbound()
      ├─ CommandDispatcher.dispatch()
      └─ SoapMessageHandler.buildResponse()

ScServiceController (@RestController, /api/b-interface)
  └─ POST /sc-service → processor.process(requestBody)

StandardScServiceController (@RestController, /services)
  └─ POST /SCService → processor.process(requestBody)
```

## 3. 实现步骤

### Step 1: 提取 ScServiceProcessor

从 `ScServiceController.handleScService()` 提取核心处理逻辑（parse→log→dispatch→buildResponse）到新 `@Service ScServiceProcessor`。

### Step 2: 重构 ScServiceController

委托给 `ScServiceProcessor.process()`，保持接口签名不变。

### Step 3: 新增 StandardScServiceController

```java
@RestController
@RequestMapping("/services")
public class StandardScServiceController {
    @PostMapping(value = "/SCService",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = MediaType.TEXT_XML_VALUE)
    public String handleScService(@RequestBody(required = false) String requestBody) {
        return processor.process(requestBody);
    }
}
```

### Step 4: 测试

- 新增 `StandardScServiceControllerTest`
- 扩展 `ScServiceControllerTest`（如存在）

## 4. 验证

```bash
# 1. 用真实 LOGIN SOAP 样本测试新入口
curl -i -X POST "http://localhost:8080/services/SCService" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H 'SOAPAction: ""' \
  -d '<soap:Envelope>...</soap:Envelope>'

# 2. 本地代码测试
mvn test -Dtest='*ScService*'
mvn test -Dtest='*BInterfaceMessageLog*'
mvn test -Dtest='*BInterface*'
mvn test
```

## 5. 输出文档

| # | 文件 |
|---|------|
| 1 | `ScServiceProcessor.java` (new) |
| 2 | `ScServiceController.java` (refactor) |
| 3 | `StandardScServiceController.java` (new) |
| 4 | `StandardScServiceControllerTest.java` (new) |
| 5 | `docs/audit/LANDING-012-*.md` |
| 6 | `docs/memory/2026-05-21-LANDING-012-*.md` |
| 7 | `docs/memory/README.md` (update) |
| 8 | `docs/memory/WORKING-MEMORY.md` (update) |
| 9 | `docs/landing/PASSIVE-REPORTING-REAL-CALL-REPORT.md` (update) |

## 6. 完成条件

| # | 条件 |
|---|------|
| 1 | POST /services/SCService 返回 SOAP XML |
| 2 | POST /api/b-interface/sc-service 保持兼容 |
| 3 | BInterfaceMessageLog 记录两个入口报文 |
| 4 | GlobalExceptionHandler 不拦截新入口异常 |
| 5 | 全量测试 0/0/5+ |
| 6 | 未执行 SET / Scheduler |
