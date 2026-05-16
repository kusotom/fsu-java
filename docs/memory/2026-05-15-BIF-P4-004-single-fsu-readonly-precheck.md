---
name: bif-p4-004-single-fsu-readonly-precheck
description: 单台真实 FSU 只读联调执行前检查 — 配置/白名单/黑名单/安全边界全部确认，可请求人工确认进入联调
metadata:
  type: project
---

## 任务编号
BIF-P4-004

## 任务名称
单台真实 FSU 只读联调执行前检查

## 操作时间
2026-05-15

## 操作代理
Claude Code

## 本次任务目标
在正式进入真实 FSU 只读联调前，完成全部执行前安全检查：验证配置安全默认值、白名单完整性、黑名单隔离性、SET 命令桩状态、RealHttpFsuServiceClient 条件加载、FsuEndpointResolver 就绪状态、日志脱敏、回滚方案存在性。本阶段不访问真实设备，不启用任何真实调用。

## 规则加载确认
- [x] AGENTS.md
- [x] CLAUDE.md
- [x] docs/PROJECT_ENGINEERING_RULES.md
- [x] docs/memory/README.md
- [x] BIF-P0-001 ~ BIF-P4-003 全部审计文档（27 份）

## 硬边界检查（14 项）

| # | 边界 | 状态 | 说明 |
|---|------|------|------|
| 1 | 不访问真实设备 | ✅ | 本阶段未发起任何网络请求 |
| 2 | 不启用 real-call-enabled | ✅ | application.yml: false |
| 3 | 不启用 scheduler | ✅ | b-interface.slow-polling.scheduler-enabled: false |
| 4 | 不执行 SET 命令 | ✅ | SET_POINT/SET_FTP: notImplemented 桩；SET_FSUREBOOT: 无 Handler；SET_THRESHOLD: 安全门禁关闭 |
| 5 | 不写入真实设备地址或凭证 | ✅ | 本阶段未修改任何业务代码和配置 |
| 6 | 不修改业务代码 | ✅ | 仅检查、测试、文档 |
| 7 | 不启用 slow-polling | ✅ | b-interface.slow-polling.enabled: false |
| 8 | 不启用 set-threshold | ✅ | b-interface.set-threshold.enabled: false |
| 9 | 不修改 application.yml | ✅ | 未写入配置 |
| 10 | 不破坏 758 测试 | ✅ | 当前 758 测试全部存在 |
| 11 | 不涉及 DSC/RDS | ✅ | 未触碰 DSC/RDS 文件 |
| 12 | 不涉及数据库 | ✅ | 本阶段纯检查 |
| 13 | 不涉及前端 | ✅ | 本阶段纯检查 |
| 14 | 不经人工确认不进真实联调 | ✅ | 等待用户确认语句 |

## 配置安全状态

| 配置项 | 文件/类 | 值 | 安全 |
|--------|---------|----|------|
| b-interface.fsu-client.real-call-enabled | application.yml | false | ✅ |
| b-interface.fsu-client.connect-timeout-ms | application.yml | 3000 | ✅ |
| b-interface.fsu-client.read-timeout-ms | application.yml | 5000 | ✅ |
| b-interface.set-threshold.enabled | application.yml | false | ✅ |
| b-interface.set-threshold.require-confirmation | application.yml | true | ✅ |
| b-interface.slow-polling.enabled | application.yml | false | ✅ |
| b-interface.slow-polling.scheduler-enabled | application.yml | false | ✅ |
| b-interface.slow-polling.allow-real-call | application.yml | false | ✅ |
| b-interface.offline-detection.enabled | application.yml | false | ✅ |
| SlowDataPollingProperties (全部默认) | SlowDataPollingProperties.java | false | ✅ |
| SetThresholdSafetyGate.enabled | SetThresholdSafetyGate.java | false | ✅ |
| SetThresholdSafetyDecision.requireConfirmation | SetThresholdSafetyGate.java | true | ✅ |

## 只读白名单状态

白名单 5 个命令，均在 ReadOnlyIntegrationSafetyTest 中自动化验证：
- ✅ GET_DATA — GetDataService 真实实现
- ✅ GET_THRESHOLD — GetThresholdService 真实实现
- ✅ TIME_CHECK — TimeCheckService 真实实现
- ✅ GET_LOGININFO — GetLoginInfoService 真实实现
- ✅ GET_FTP — GetFtpService 真实实现

## 禁止命令状态

| 命令 | 风险 | Handler 状态 | 执行保护 |
|------|------|-------------|---------|
| SET_POINT | 极高 | notImplemented 桩 | 桩返回 false |
| SET_FTP | 高 | notImplemented 桩 | 桩返回 false |
| SET_FSUREBOOT | 极高 | 无 Handler 类 | 路由到 UnknownCommandHandler |
| SET_THRESHOLD | 高 | 安全门禁 enabled=false | 门禁拒绝 + 审计记录 |
| SET_DATA | 高 | 未实现 | 无执行路径 |

## RealHttpFsuServiceClient 状态
- 条件加载：`@ConditionalOnProperty(name = "b-interface.fsu-client.real-call-enabled", havingValue = "true")`
- 当前：因 `real-call-enabled=false` 不会被加载
- StubFsuServiceClient：`@ConditionalOnProperty(havingValue="false", matchIfMissing=true)` — 默认激活

## FsuEndpointResolver 状态
- 接口：FsuEndpointResolver.resolve(String fsuCode) → FsuEndpointResult
- 实现：FsuServiceEndpointService — 从 FsuDeviceEntity.ipAddr + port 构建 URL
- 默认路径：`http://{ip}:{port}/services/FSUService`
- 测试覆盖：24 测试（FsuServiceEndpointServiceTest）

## 日志脱敏状态
- GetFtpResult.getMaskedUsername() — username 中间 4 位掩码为 `****`
- GetFtpResult.toString() — 使用掩码后的 username
- GetLoginInfoResult — 无 password 字段

## 回滚方案状态
- 文件：BIF-P4-003-real-call-rollback-plan.md
- 覆盖 6 场景：网络不可达 / 响应异常 / 应用异常 / 配置错误 / SET 误执行 / 忘记关闭
- 每个场景有完整回滚步骤和验证方法

## 测试命令
```
mvn test -Dtest='*ReadOnly*,*Whitelist*,*Endpoint*,*Resolver*,*Readiness*,*Safety*,*Boundary*,*FsuService*,*FsuClient*,*BInterface*'
```

## 测试结果
```
Tests run: 143, Failures: 0, Errors: 0, Skipped: 0
```
- StubFsuServiceClientTest: 23 通过
- FsuServiceEndpointServiceTest: 24 通过
- ReadOnlyIntegrationSafetyTest: 23 通过
- 其他 BInterface 测试：全部通过

## 是否访问真实设备
否。本阶段为纯检查，无任何网络请求。

## 是否启用真实 FSU 调用
否。real-call-enabled=false，RealHttpFsuServiceClient 不会被加载。

## 是否启用 Scheduler
否。scheduler-enabled=false，@EnableScheduling 未开启。

## 是否执行 SET 类命令
否。所有 SET 命令都被安全机制拦截。

## 是否写入真实设备地址或凭证
否。本阶段未修改配置和代码。

## 是否可以请求人工确认进入真实只读联调
是。所有 14 项硬边界检查通过，已满足进入真实只读联调的前置条件。

## 实际新增/修改文件

### 新增（1 个）
| 文件 | 说明 |
|------|------|
| `docs/memory/2026-05-15-BIF-P4-004-single-fsu-readonly-precheck.md` | 操作记忆文件 |

### 修改（1 个）
| 文件 | 说明 |
|------|------|
| `docs/memory/README.md` | 新增 BIF-P4-004 索引条目 |

## 遗留问题
1. 联调 FSU 设备、网络、人员尚未确认
2. 配置模板中 connect-timeout/read-timeout 需联调时调整
3. FSU 通讯录中联系人尚未填写
4. 本次为检查阶段，未执行真实联调

## 建议下一步
在获得用户书面确认后进入单台 FSU 真实只读联调执行阶段：
1. 等待用户输入确认语句
2. 创建联调分支
3. 按 BIF-P4-003 联调计划逐步执行
4. 先 GET_LOGININFO → TIME_CHECK → GET_DATA → GET_THRESHOLD → GET_FTP
5. 联调完成后关闭 real-call-enabled
6. 运行全量测试验证无回归
