# LANDING-004 真实 FSU 响应兼容性修复与 PK_Type 重试

## 任务目标
修复 LANDING-003 发现的 2 个协议兼容性问题，并使用 structured/legacy 双格式重试真实 FSU 只读查询。

## 架构判断
- 修改层: FSU 通信层 (FsuServiceRpcAdapter, RealHttpFsuServiceClient, FsuServiceRequest)
- 不涉及: 业务服务层、SC 入站、StubFsuServiceClient、Scheduler、SET 安全体系

## 协议一致性判断
- 兼容性修复符合 WSDL RPC/encoded 规范
- PK_Type 双格式符合 B-2016/B-2024 双协议支持
- Code 值始终从 BInterfaceCommand2024 枚举获取，不硬编码

## 修改前论证
见 LANDING-004 修改前论证报告（8 条论证项，用户已确认）。

## 写入前验证
- stripXmlDeclaration: 仅剥离行首 XML 声明，不影响标准 XML
- pkTypeFormat: 默认 null/空值时行为完全不变
- 真实 FSU 测试类: @Tag("real-fsu") + @EnabledIfSystemProperty，默认不执行

## 实际修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `FsuServiceRpcAdapter.java` | 修改 | +stripXmlDeclaration() 方法; unwrapResponsePayload 调用剥离 |
| `FsuServiceRequest.java` | 修改 | +pkTypeFormat 属性 + Builder |
| `RealHttpFsuServiceClient.java` | 修改 | call() 支持 legacy-text 格式 |
| `FsuServiceRpcAdapterTest.java` | 修改 | +8 测试 (35 total) |
| `SoapMessageHandlerPkType2024Test.java` | 修改 | +5 测试 (12 total) |
| `Landing003RealFsuIntegration.java` | 修改 | structured/legacy 双格式重试 + 安全注解 |
| `docs/audit/LANDING-004-real-fsu-compatibility-retry.md` | 新增 | 审计报告 |
| `docs/landing/raw-samples/landing004-*` | 新增 | 40 个 XML 原始报文 (5命令×2格式×4层) |

## 核心改动
1. **嵌套 XML 声明容错**: `stripXmlDeclaration()` 在解析前剥离行首 `<?xml ...?>`
2. **PK_Type 兼容模式**: 请求级 `pkTypeFormat` 属性 (null=structured, "legacy-text"=纯文本)
3. **默认行为不变**: 不设置 pkTypeFormat 时与修改前完全一致

## 测试命令和结果
```
mvn test -Dtest='*FsuServiceRpcAdapter*'   → 35 tests PASS
mvn test -Dtest='*SoapMessageHandler*'      → 86 tests PASS
mvn test -Dtest='*BInterface*'              → 116 tests PASS
mvn test                                      → 1164 tests, 0 failures, 0 errors, 5 skipped
mvn test -Dtest='Landing003RealFsuIntegration' -DrealFsuTest.enabled=true → 1 test PASS (10 real calls)
```

## 风险
- pkTypeFormat 字符串匹配（非枚举），拼写错误不报编译错 → 已文档化可接受值
- FSU 仍不支持 GET_* 查询 → 需管理方确认

## 遗留问题
1. FSU 对所有 GET_* 命令返回空响应（无论 PK_Type 格式）
2. GET_DATA Code=501 被 FSU 路由到 SEND_ALARM
3. 真实 SUID/DeviceID/SPID 仍未知

## 下一步建议
1. 向管理方确认 FSU 固件协议版本和支持的命令列表
2. 确认 FSU Code 分配表（与 2024 标准可能不一致）
3. 提交 Codex 集中复审 (BIF-P4-FIX-001 ~ LANDING-004)
