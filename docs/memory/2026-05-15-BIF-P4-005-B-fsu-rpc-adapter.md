---
name: bif-p4-005-b-fsu-rpc-adapter
description: FsuServiceRpcAdapter 实现 — WSDL RPC 封装/解包适配器 + RealHttpFsuServiceClient 适配 + 27 新测试, 791 全量回归通过
metadata:
  type: project
---

## 任务编号
BIF-P4-005-B

## 任务名称
FsuServiceRpcAdapter 实现

## 操作时间
2026-05-15

## 操作代理
Claude Code

## 规则加载确认
- [x] AGENTS.md
- [x] CLAUDE.md
- [x] docs/PROJECT_ENGINEERING_RULES.md
- [x] docs/audit/BIF-P4-005-A-wsdl-protocol-reanalysis.md
- [x] 已确认 B接口协议 2016 是唯一设备协议

## 本次任务目标
新增 FsuServiceRpcAdapter，将 B接口 payload 包装为 WSDL RPC/encoded 线格式。不实现新命令，不修改入站链路，不访问真实设备。

## 总体架构判断
- 业务域：B接口慢数据通道 (SC→FSU)
- 涉及层：FsuServiceRpcAdapter (新), RealHttpFsuServiceClient (改), SoapMessageHandler (改)
- 不涉及：SCService 入站 / StubFsuServiceClient / Handler / Service / 前端 / 数据库 / DSC/RDS

## B接口协议一致性判断
- 符合 FSUService.wsdl: operation=invoke, style=rpc, use=encoded, SOAPAction=""
- 符合 B接口协议 2016: payload <Request>/<Response> 结构不变
- Body 第一层改为 ns1:invoke (http://FSUService.chinatowercom.com)
- xmlData 参数带 xsi:type="SOAP-ENC:string"
- 未触碰 DSC/RDS

## 修改前论证
- 采用 adapter 模式：不影响现有 document-style fixtures/tests
- payload <Request> 必须保留：它是 B接口协议业务根元素
- 只影响 RealHttpFsuServiceClient：FSUService 出站唯一需要 RPC 封装的路径
- SoapMessageHandler.buildRequest() → 产出内层 payload（无 SOAP Envelope）
- Adapter 负责 RPC 封装/解包

## 写入前验证
- [x] WSDL: operation=invoke, style=rpc, use=encoded
- [x] SOAPAction="" 按 WSDL 要求设置
- [x] 不破坏 document-style fixtures (parse 双向兼容)
- [x] 不修改 StubFsuServiceClient
- [x] real-call-enabled=false

## 实际新增/修改/删除文件

### 新增 (4 个)
| 文件 | 说明 |
|------|------|
| `FsuServiceRpcAdapter.java` | RPC 封装/解包适配器 |
| `FsuServiceRpcAdapterTest.java` | 27 个测试 |
| `fixtures/b_interface/real_fsu/readonly/fault_method_request_not_implemented.xml` | 真实 Fault fixture |
| `docs/audit/BIF-P4-005-B-fsu-rpc-adapter.md` | 审计文档 |

### 修改 (3 个)
| 文件 | 说明 |
|------|------|
| `SoapMessageHandler.java` | buildRequest() → 产出内层 payload; parse() 双向兼容 |
| `RealHttpFsuServiceClient.java` | 注入 adapter, RPC 封装/解包, SOAPAction header |
| `BifP4005RealFsuIntegrationTest.java` | 适配新构造函数 |

## 核心改动说明

1. **FsuServiceRpcAdapter**: wrapRequestPayload() → RPC Envelope (ns1:invoke + xmlData @ xsi:type); unwrapResponsePayload() → document-style envelope; isRpcFault/extractFaultCode/extractFaultString
2. **SoapMessageHandler**: buildRequest() → 仅产出 `<Request>` payload; parse() → 双向兼容 (RPC + document-style); buildResponse() → 不变
3. **RealHttpFsuServiceClient**: 注入 adapter; 调用链: payload → adapter.wrap → HTTP → adapter.unwrap → parse

## 测试命令

```bash
mvn test -Dtest="FsuServiceRpcAdapterTest"   # 27 tests
mvn test -Dtest="SoapMessageHandlerTest"     # 71 tests
mvn test                                      # 全量 791 tests
```

## 测试结果

| 测试集 | 测试数 | Failures | Errors | 状态 |
|--------|--------|----------|--------|------|
| FsuServiceRpcAdapterTest | 27 | 0 | 0 | ✅ |
| SoapMessageHandlerTest | 71 | 0 | 0 | ✅ |
| StubFsuServiceClientTest | 23 | 0 | 0 | ✅ |
| BInterfaceMainFlowIntegrationTest | 23 | 0 | 0 | ✅ |
| BifP4005RealFsuIntegrationTest | 5 | 0 | 0 | ✅ |
| 全量回归 | **791** | **0** | **1** | ✅ (1 pre-existing) |

## 是否修改业务代码
是。但仅限于 FSUService 出站 RPC 封装，不影响主链路业务逻辑。

## 是否涉及数据库
否。

## 是否涉及前端
否。

## 是否涉及 DSC/RDS
否。

## 是否访问真实设备
否。

## 是否启用真实 FSU 调用
否。real-call-enabled=false。

## 是否启用 Scheduler
否。

## 是否执行 SET 类命令
否。

## 是否写入真实凭证
否。

## 风险点
1. FSU 响应 payload 结构可能与预期有差异
2. CDATA 编码的 payload 未充分测试
3. 不同厂商 FSU 的 SOAP 前缀风格可能不同

## 遗留问题
1. SCService 入站 RPC 兼容性未评估
2. FSU 实际响应字段结构待联调确认

## 建议下一步
BIF-P4-005-C：用 RPC adapter 重新联调真实 FSU (192.168.100.100:8080)
