# LANDING-012 Spec：新增 /services/SCService B接口标准兼容接收入口

> 规格版本：1.0
> 创建日期：2026-05-21
> 对应计划：`openspec/plans/landing-012-standard-scservice-path-plan.md`

---

## 1. 背景

现场 tcpdump 确认真实 FSU 已按 B接口标准路径向平台发起 HTTP/SOAP 上报：

```
POST /services/SCService HTTP/1.1
Host: 192.168.100.123:8080
User-Agent: gSOAP/2.8
Content-Type: text/xml; charset=utf-8
SOAPAction: ""
```

请求内容为真实 LOGIN（PK_Type=LOGIN, Code=101），包含 FsuId=51051243812345 和 5 个 DeviceID。

但当前项目只有 `POST /api/b-interface/sc-service`，`/services/SCService` 无映射，导致 Spring 返回 404 → GlobalExceptionHandler 将其转为 JSON ApiResponse，FSU 无法收到合法 SOAP ACK。

## 2. 目标

新增标准兼容入口 `POST /services/SCService`：
1. 复用现有 SCService 处理逻辑（解析→分发→响应→日志）
2. 返回 SOAP XML 响应（非 JSON）
3. 入站报文保存到 BInterfaceMessageLog
4. `/api/b-interface/sc-service` 保持兼容不删除

## 3. 非目标

- 不删除 `/api/b-interface/sc-service`
- 不改变 SOAP 解析/分发/响应逻辑
- 不修改 FSUService 出站调用
- 不执行 SET
- 不启用 Scheduler
- 不主动访问真实 FSU
- 不影响 WSDL GET 端点
- 不大范围重构

## 4. 验收标准

| # | 验收项 |
|---|--------|
| 1 | `POST /services/SCService` 返回 SOAP XML（非 JSON） |
| 2 | `POST /api/b-interface/sc-service` 仍可用 |
| 3 | 两个入口对同一请求返回一致 SOAP ACK |
| 4 | 两个入口均保存入站报文到 BInterfaceMessageLog |
| 5 | 全量测试 0 failures, 0 errors |
| 6 | 未执行 SET / Scheduler / 真实 FSU 访问 |

## 5. 安全边界

| 边界 | 状态 |
|------|------|
| SET 命令 | **禁止** |
| Scheduler | **禁止** |
| 主动访问真实 FSU | **禁止** |
| alarm_record 状态机 | **不修改** |
| `/api/b-interface/sc-service` | **保留兼容** |
| WSDL GET | **不影响** |
| Codex 复审 | **暂缓** |

## 6. 风险

| # | 风险 | 缓解 |
|---|------|------|
| 1 | `@RequestMapping("/services")` 与其他路径冲突 | 仅新增 POST /SCService，不影响其他 |
| 2 | GlobalExceptionHandler 拦截异常返回 JSON | Controller 内部 try-catch 所有异常，不抛出 |
| 3 | 提取 Processor 破坏现有测试 | 最小提取，保持测试兼容 |
