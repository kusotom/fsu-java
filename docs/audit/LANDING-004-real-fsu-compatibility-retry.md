# LANDING-004: 真实 FSU 响应兼容性修复与旧版 PK_Type 查询重试

## 审计日期
2026-05-20

## 审计类型
代码修改 + 真实设备只读重试 (structured vs legacy PK_Type 对比)

## 1. 修改文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `FsuServiceRpcAdapter.java` | 修改 | unwrapResponsePayload() 增加 stripXmlDeclaration 容错 |
| `FsuServiceRpcAdapter.java` | 修改 | 新增 `public static stripXmlDeclaration()` 方法 |
| `FsuServiceRequest.java` | 修改 | 新增 `pkTypeFormat` 属性 + Builder 支持 |
| `RealHttpFsuServiceClient.java` | 修改 | call() 支持 `legacy-text` 格式强制 |
| `FsuServiceRpcAdapterTest.java` | 修改 | 新增 8 个测试 (嵌套声明容错 + 空响应) |
| `SoapMessageHandlerPkType2024Test.java` | 修改 | 新增 5 个测试 (双格式输出对比) |
| `Landing003RealFsuIntegration.java` | 重写 | 改为 @Tag("real-fsu") + @EnabledIfSystemProperty; 增加 structured/legacy 双格式重试 |

## 2. 嵌套 XML 声明容错修复

**问题**: 真实 FSU 在 `<invokeReturn>` 中返回转义的 `<?xml version="1.0"?><Response>...`。`getTextContent()` 自动反转义后，`wrapInMinimalEnvelope()` 将其嵌入另一个 XML 文档，造成嵌套声明。

**修复**: `stripXmlDeclaration()` 方法检测并剥离以 `<?xml ` 开头的 XML 声明。仅在解析副本上操作，原始 RPC 响应文件原样保存。

**测试**: 8 个新测试验证:
- stripXmlDeclaration 正确剥离声明
- 无声明文本不受影响
- null/空字符串安全
- 仅在行首检测
- 含嵌套声明的 RPC 响应正确解包
- 空 invokeReturn 正确处理
- 标准响应不受影响

## 3. PK_Type 兼容模式

**设计**: `FsuServiceRequest.pkTypeFormat` 属性，请求级隔离。
- `null` 或 `"structured"`: 默认行为，通过 AliasMapper→2024 查找 Name+Code
- `"legacy-text"`: 强制纯文本 PK_Type，跳过 2024 映射

**实现约束**:
- 默认行为不变（不设置时与修改前完全一致）
- 不影响 StubFsuServiceClient
- 不影响现有 BIF-P4 测试
- Code 值始终从 BInterfaceCommand2024 枚举获取，不硬编码

**测试**: 5 个新测试验证:
- structured 格式包含 Name+Code
- legacy 格式为纯文本 PK_Type
- 5 个允许命令的 legacy 格式均不含 Name/Code
- legacy GET_ACTIVEALARM 仍包含命令名
- structured GET_ACTIVEALARM 包含 Name+Code

## 4. 测试结果

```
mvn test -Dtest='*FsuServiceRpcAdapter*'   → 35 tests, 0 failures
mvn test -Dtest='*SoapMessageHandler*'      → 86 tests, 0 failures
mvn test -Dtest='*BInterface*'              → 116 tests, 0 failures
mvn test                                      → 1164 tests, 0 failures, 0 errors, 5 skipped
```

默认 `mvn test` 不访问真实 FSU (Landing003RealFsuIntegration 需 `-DrealFsuTest.enabled=true`)。

## 5. 真实 FSU 重试结果 (structured vs legacy)

```
mvn test -Dtest='Landing003RealFsuIntegration' -DrealFsuTest.enabled=true → 1 test, BUILD SUCCESS
```

| 命令 | Structured (Name+Code) | Legacy (纯文本) | 差异 |
|------|----------------------|-----------------|------|
| GET_SUINFO | 空 invokeReturn | 空 invokeReturn | 一致 |
| GET_DATA | **SEND_ALARM 回传** (Code 501→SEND_ALARM) | 空 invokeReturn | **不同** |
| GET_ACTIVEALARM | 空 invokeReturn | 空 invokeReturn | 一致 |
| GET_SPCONFIGOPTION | 空 invokeReturn | 空 invokeReturn | 一致 |
| GET_THRESHOLD | 空 invokeReturn | 空 invokeReturn | 一致 |

**关键发现**:
- GET_DATA structured (Code=501): FSU 将 Code 501 路由到 SEND_ALARM 处理器，返回空告警列表
- GET_DATA legacy (纯文本 "GET_DATA"): FSU 不识别此命令名，返回空响应
- 其他命令: 两种格式均返回空 invokeReturn，FSU 不支持这些出站查询
- **Nested XML declaration 容错修复生效**: structured GET_DATA 的 SEND_ALARM 回传成功解析（L-003 中此场景崩溃）

## 6. 解析出的真实字段

- **SUID**: 未获取到（所有响应为空）
- **DeviceID**: 未获取到
- **SPID**: 未获取到
- 所有业务字段仍为空白 — PK_Type 格式不是根本原因

## 7. 安全边界

- [x] 未执行任何 SET_ 命令
- [x] 未启用 Scheduler
- [x] 未修改 alarm_record 状态
- [x] 未生成正式 seed SQL
- [x] Landing003RealFsuIntegration 默认不执行
- [x] 默认 `mvn test` 不访问 192.168.100.100
- [x] pkTypeFormat 仅请求级隔离，不影响默认行为

## 8. 结论

1. **兼容性修复生效**: 嵌套 XML 声明不再导致解析崩溃
2. **PK_Type 格式不是空响应的根因**: 两种格式均返回空数据，FSU 根本不支持 GET_* 出站查询
3. **GET_DATA Code=501 路由异常**: FSU 将 Code 501 映射到 SEND_ALARM，与 2024 标准 (SEND_ALARM=601) 不一致
4. **下一步**: 需管理方确认 FSU 固件协议版本和命令支持列表
