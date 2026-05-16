---
name: bif-p4-001-slow-channel-readiness
description: 慢数据通道启用确认/真实联调前安全验收，配置审计、命令覆盖检查、敏感字段脱敏检查、安全验收测试、联调前检查清单
metadata:
  type: project
---

## 任务编号
BIF-P4-001

## 任务名称
慢数据通道启用确认 / 真实联调前安全验收

## 操作时间
2026-05-15

## 操作代理
Claude Code

## 本次任务目标
对 BIF-P3-001 ~ BIF-P3-007 已完成的慢数据通道能力进行总体验收，确认是否具备进入真实 FSU 联调的条件。本阶段只做安全验收、配置审计、开关检查、风险清单、联调前检查清单和必要的测试补强。

## 规则加载确认
- [x] AGENTS.md
- [x] CLAUDE.md
- [x] docs/PROJECT_ENGINEERING_RULES.md（文件不存在，未加载）
- [x] docs/memory/README.md
- [x] BIF-P1-005 ~ BIF-P3-007 全部 11 份审计文档

## 总体架构判断
```
FsuServiceClient (接口)
  → StubFsuServiceClient (默认) / RealHttpFsuServiceClient (需启用)
  → GET_DATA / GET_THRESHOLD / SET_THRESHOLD / TIME_CHECK / GET_LOGININFO / GET_FTP
  → SlowDataPollingService (默认 disabled)
  → SignalPollingTargetService (MonitoringPoint 复用)
  → OfflineDetectionService (默认 disabled)
```

## B接口协议一致性判断
审计确认所有已实现命令均符合 B接口协议 2016 SOAP/XMLData 标准，未引入私有协议，高风险 SET 类命令保持禁用。

## 修改前论证
本阶段纯安全验收 + 文档输出：
- 为什么现在可以做 P4 启用确认：P3 全部 7 个阶段（001~007）已完成
- 为什么不能真实访问设备：未授权、未配置、未建立联调环境
- 为什么先做配置安全审计：确保默认值安全、无密码硬编码、SET 类禁止
- 为什么必须确认敏感字段脱敏：GetFtpResult.username 需掩码输出
- 为什么必须确认 SET_POINT/SET_FTP/SET_FSUREBOOT 仍禁用：高风险命令不可进入轮询

## 写入前验证
- [x] application.yml 配置安全检查
- [x] CommandHandler 实现状态审计
- [x] StubFsuServiceClient 默认激活确认
- [x] RealHttpFsuServiceClient 默认禁用确认
- [x] SET_THRESHOLD 安全门禁确认
- [x] SET_POINT/SET_FTP/SET_FSUREBOOT 禁用状态确认
- [x] 敏感字段脱敏检查（GetFtpResult.getMaskedUsername）
- [x] SlowDataPollingService 不引用 SET 类服务
- [x] 无硬编码 FSU 凭证

## 实际新增/修改/删除文件

### 新增（4 个）
| 文件 | 说明 |
|------|------|
| `backend/src/test/.../SlowChannelReadinessAuditTest.java` | 19 个安全验收测试 |
| `docs/audit/BIF-P4-001-slow-channel-readiness.md` | 慢数据通道启用确认审计文档 |
| `docs/audit/BIF-P4-001-real-fsu-integration-checklist.md` | 真实 FSU 联调前检查清单 |
| `docs/memory/2026-05-15-BIF-P4-001-slow-channel-readiness.md` | 操作记忆文件 |

### 修改
| 文件 | 说明 |
|------|------|
| `docs/memory/README.md` | 新增 BIF-P4-001 索引条目 |

## 核心改动说明
1. **SlowChannelReadinessAuditTest (19 测试)**：覆盖配置默认值、SET 类命令禁用、敏感字段脱敏、Stub 不访问网络、轮询不引用 SET 类服务
2. **BIF-P4-001-slow-channel-readiness.md**：18 节安全验收审计文档
3. **BIF-P4-001-real-fsu-integration-checklist.md**：9 节联调前检查清单

## 执行的测试命令
```
mvn test -Dtest="com.dcim.platform.binterface.**"
```

## 测试结果
```
Tests run: 711, Failures: 0, Errors: 0, Skipped: 0
```
较 BIF-P3-007 的 692 测试新增 19 个安全验收测试。

## 是否修改业务代码
否。仅新增测试和文档。

## 是否涉及数据库
否。

## 是否涉及前端
否。

## 是否涉及 DSC/RDS
否。

## 是否访问真实设备
否。所有测试使用 Stub/Mock。

## 是否启用真实 FSU 调用
否。real-call-enabled 默认 false。

## 是否启用 Scheduler
否。scheduler-enabled 默认 false。

## 是否执行 SET 类命令
否。SET_POINT/SET_FTP 仍为 notImplemented 桩，SET_FSUREBOOT 无 Handler。

## 是否输出明文敏感信息
否。GetFtpResult.toString() 使用 getMaskedUsername()，GetLoginInfoResult 无 password 字段。

## 是否建议进入真实联调
有条件建议。见审计文档第 17 节。

## 风险点
| 风险 | 等级 | 说明 |
|------|------|------|
| FSU 地址管理未实现 | 中 | serviceUrl 传 null，联调前必须解决 |
| 审计数据内存存储 | 中 | 重启丢失，需持久化 |
| @EnableScheduling 未全局开启 | 低 | 即使 enabled=true 也不触发 |
| SET_FSUREBOOT 无 Handler | 低 | 路由到 UnknownCommandHandler |

## 遗留问题
1. FSU 地址发现逻辑未实现，真实联调前必须补充
2. 审计数据未持久化到数据库
3. @EnableScheduling 未全局开启

## 下一步建议
1. 补充 FSU 地址发现逻辑（FsuDeviceEntity endpoint 字段 + Service 层查询）
2. 制定联调计划（联调 FSU、范围、步骤、回滚方案）
3. 真实联调执行（先 GET 类命令，后 SET_THRESHOLD）
4. SET_POINT/SET_FTP/SET_FSUREBOOT 需单独安全评估

## Git diff 摘要
无业务代码修改，仅新增测试和文档。

## Git status 摘要
```
A  backend/src/test/.../SlowChannelReadinessAuditTest.java
A  docs/audit/BIF-P4-001-slow-channel-readiness.md
A  docs/audit/BIF-P4-001-real-fsu-integration-checklist.md
A  docs/memory/2026-05-15-BIF-P4-001-slow-channel-readiness.md
M  docs/memory/README.md
```
