# BIF-P4-001：慢数据通道启用确认 / 真实联调前安全验收

> 对应阶段：BIF-P4-001
> 完成日期：2026-05-15
> 影响范围：纯安全验收审计，不修改业务代码

---

## 一、本阶段目标

对 BIF-P3-001 ~ BIF-P3-007 已完成的所有慢数据通道能力进行总体验收，确认是否具备进入真实 FSU 联调的条件。本阶段不启用真实设备调用，不实现新命令，不修改前端。

## 二、P3 阶段能力总览

| 阶段 | 内容 | 状态 |
|------|------|------|
| BIF-P3-001 | FSU SOAP 客户端基础设施 + GET_DATA 闭环 | ✅ 完成 |
| BIF-P3-002 | GET_THRESHOLD 门限查询闭环 | ✅ 完成 |
| BIF-P3-003 | SET_THRESHOLD 门限设置闭环（4 层安全） | ✅ 完成 |
| BIF-P3-004 | 慢数据通道定时轮询框架 | ✅ 完成 |
| BIF-P3-005 | Signal 轮询目标配置 | ✅ 完成 |
| BIF-P3-006 | TIME_CHECK 时间同步闭环 | ✅ 完成 |
| BIF-P3-007 | GET_LOGININFO / GET_FTP 查询类闭环 | ✅ 完成 |

## 三、当前慢数据通道架构

```
SC Platform
  ├── CommandDispatcher (路由 13 个 PK_Type)
  │   ├── GET_DATA          → GetDataCommandHandler (真实) → GetDataService → FsuServiceClient
  │   ├── GET_THRESHOLD     → GetThresholdCommandHandler (真实) → GetThresholdService → FsuServiceClient
  │   ├── SET_THRESHOLD     → SetThresholdCommandHandler (真实) → SafetyGate → AuditService → SetThresholdService
  │   ├── TIME_CHECK        → TimeCheckCommandHandler (真实) → TimeCheckService → FsuServiceClient
  │   ├── GET_LOGININFO     → GetLoginInfoCommandHandler (真实) → GetLoginInfoService → FsuServiceClient
  │   ├── GET_FTP           → GetFtpCommandHandler (真实) → GetFtpService → FsuServiceClient
  │   ├── SET_POINT         → SetPointCommandHandler (桩: notImplemented)
  │   ├── SET_FTP           → SetFtpCommandHandler (桩: notImplemented)
  │   └── ... (UNKNOWN → UnknownCommandHandler)
  │
  ├── SlowDataPollingService (默认 disabled)
  │   ├── SignalPollingTargetService → MonitoringPoint
  │   └── 仅 GET_DATA / GET_THRESHOLD（无 SET 类）
  │
  ├── FsuServiceClient
  │   ├── StubFsuServiceClient (默认, matchIfMissing=true)
  │   └── RealHttpFsuServiceClient (需 real-call-enabled=true)
  │
  └── OfflineDetectionService (默认 disabled)
```

## 四、已实现命令清单（6 个闭环）

| 命令 | 类型 | Handler | Service | Result | Stub 支持 |
|------|------|---------|---------|--------|----------|
| GET_DATA | 只读查询 | ✅ 真实 | ✅ GetDataService | ✅ GetDataResult | ✅ 5 信号 |
| GET_THRESHOLD | 只读查询 | ✅ 真实 | ✅ GetThresholdService | ✅ GetThresholdResult | ✅ 3 信号 |
| TIME_CHECK | 只读查询 | ✅ 真实 | ✅ TimeCheckService | ✅ TimeCheckResult | ✅ 固定时间 |
| GET_LOGININFO | 只读查询 | ✅ 真实 | ✅ GetLoginInfoService | ✅ GetLoginInfoResult | ✅ 登录状态 |
| GET_FTP | 只读查询 | ✅ 真实 | ✅ GetFtpService | ✅ GetFtpResult | ✅ FTP 配置 |
| SET_THRESHOLD | 配置下发 | ✅ 真实+门禁 | ✅ SetThresholdService | ✅ SetThresholdResult | ✅ 固定响应 |

## 五、未实现命令清单（3 个高风险）

| 命令 | 风险 | Handler 状态 | StubFsuServiceClient 支持 |
|------|------|-------------|--------------------------|
| SET_POINT | 极高 | notImplemented 桩 | ❌ 不支持 |
| SET_FTP | 高 | notImplemented 桩 | ❌ 不支持 |
| SET_FSUREBOOT | 极高 | ❌ 无 Handler 类 | ❌ 不支持 |

## 六、查询类命令安全状态

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 默认走 Stub | ✅ | matchIfMissing=true |
| 需登录态校验 | ✅ | LoginService.isLoggedIn() |
| 不修改 FSU 状态 | ✅ | 只读查询 |
| 不访问真实网络 | ✅ | Stub 模式 |
| 不执行 SET 类命令 | ✅ | 代码隔离 |
| GET_FTP username 脱敏 | ✅ | getMaskedUsername() + toString 脱敏 |
| GET_LOGININFO 无密码 | ✅ | 无 password 字段 |
| 无明文凭证硬编码 | ✅ | 仅测试 IP |

## 七、高风险命令安全状态

| 检查项 | SET_THRESHOLD | SET_POINT | SET_FTP | SET_FSUREBOOT |
|--------|--------------|-----------|---------|---------------|
| 安全门禁 | ✅ enabled=false | N/A（桩） | N/A（桩） | N/A（无 Handler） |
| 二次确认 | ✅ require-confirmation=true | N/A | N/A | N/A |
| 操作审计 | ✅ SetThresholdAuditService | N/A | N/A | N/A |
| 代码隔离 | ✅ 不进入轮询 | ✅ 桩 | ✅ 桩 | ✅ 无 Handler |
| StubFsuClient 支持 | ✅ | ❌ | ❌ | ❌ |

## 八、配置默认值检查

| 配置项 | 默认值 | 安全 | 说明 |
|--------|--------|------|------|
| b-interface.fsu-client.real-call-enabled | false | ✅ 安全 | Stub 默认激活 |
| b-interface.slow-polling.enabled | false | ✅ 安全 | 轮询总开关关闭 |
| b-interface.slow-polling.scheduler-enabled | false | ✅ 安全 | 定时调度关闭 |
| b-interface.slow-polling.allow-real-call | false | ✅ 安全 | 即使启用轮询也走 Stub |
| b-interface.set-threshold.enabled | false | ✅ 安全 | SET 类命令默认禁止 |
| b-interface.set-threshold.require-confirmation | true | ✅ 安全 | 默认要求二次确认 |
| b-interface.offline-detection.enabled | false | ✅ 安全 | 离线检测默认关闭 |

## 九、Stub / Real 调用边界

| 特性 | StubFsuServiceClient | RealHttpFsuServiceClient |
|------|---------------------|-------------------------|
| 默认激活 | ✅ (matchIfMissing=true) | ❌ (havingValue="true") |
| 网络访问 | ❌ 否 | ✅ 是 |
| 适用场景 | 开发/测试 | 真实联调/生产 |
| 配置值 | false / 未配置 | true |
| 初始化日志 | INFO | WARN（明确提示） |
| 异常处理 | 返回结构化响应 | 返回结构化响应 |
| SET_POINT/SET_FTP/SET_FSUREBOOT | ❌ 不支持 | ❌ 代码注释声明不支持 |

## 十、Scheduler 启用状态

| 组件 | 启用状态 | 风险 |
|------|---------|------|
| SlowDataPollingScheduler | disabled（scheduler-enabled=false） | 低 |
| @EnableScheduling 全局 | 未开启 | 低 |
| OfflineDetectionService | disabled（enabled=false） | 低 |

## 十一、SET 类命令门禁状态

| 门禁层 | SET_THRESHOLD | 说明 |
|--------|--------------|------|
| enabled 开关 | ✅ enabled=false 默认关闭 | 第一道门禁 |
| require-confirmation | ✅ require-confirmation=true | 第二道门禁 |
| 操作审计 | ✅ SetThresholdAuditService | 审计记录 |
| Handler 编排 | ✅ SetThresholdCommandHandler | 组合门禁+审计+业务 |
| 不进入轮询 | ✅ SlowDataPollingService 无 SET_THRESHOLD 路径 | 代码隔离 |

## 十二、敏感字段脱敏状态

| 模型 | 敏感字段 | 脱敏方式 | 状态 |
|------|---------|---------|------|
| GetFtpResult | username | getMaskedUsername() + toString 用掩码 | ✅ 已脱敏 |
| GetFtpService | username | 日志记录首字符+**** | ✅ 已脱敏 |
| GetLoginInfoResult | sessionId | toString 不包含 | ✅ 无密码字段 |
| 所有 Result | password | 不在模型字段中 | ✅ 不存储 |
| application.yml | FSU IP/密码 | 无硬编码 | ✅ 仅测试 IP |
| StubFsuServiceClient | 设备地址 | 仅 192.168.1.200 测试 IP | ✅ 无真实地址 |

## 十三、测试覆盖状态

| 测试类别 | 测试数 | 状态 |
|---------|--------|------|
| FsuServiceClient 测试 | 23 (StubFsuServiceClientTest) | ✅ 通过 |
| GET_DATA 测试 | 16+14+8=38 | ✅ 通过 |
| GET_THRESHOLD 测试 | 15+14+8=37 + Stub 扩展 | ✅ 通过 |
| SET_THRESHOLD 测试 | 16+6+7+15+10=54 + Stub 扩展 | ✅ 通过 |
| TIME_CHECK 测试 | 20+19=39 | ✅ 通过 |
| GET_LOGININFO 测试 | 13+11+11=35 | ✅ 通过 |
| GET_FTP 测试 | 17+20+11=48 | ✅ 通过 |
| SlowDataPolling 测试 | 8+17+4+10=39 | ✅ 通过 |
| SignalPollingTarget 测试 | 12+1=13 | ✅ 通过 |
| 安全验收测试 | 21 (SlowChannelReadinessAuditTest) | ✅ 新增 |
| **全量 B-interface 测试** | **692 → 713** | **✅ 全部通过** |

## 十四、真实联调前检查清单

见独立文件 `BIF-P4-001-real-fsu-integration-checklist.md`

## 十五、禁止项清单

1. ❌ 不允许访问真实设备（未授权时）
2. ❌ 不允许启用 real-call-enabled=true（未批准时）
3. ❌ 不允许启用 slow-polling.enabled=true（未批准时）
4. ❌ 不允许启用 scheduler-enabled=true（未批准时）
5. ❌ 不允许启用 allow-real-call=true（未批准时）
6. ❌ 不允许启用 set-threshold.enabled=true（未批准时）
7. ❌ 不允许执行 SET_POINT / SET_FTP / SET_FSUREBOOT 真实下发
8. ❌ 不允许绕过安全门禁进行 SET_THRESHOLD 操作
9. ❌ 不允许在日志中输出明文密码或敏感凭证
10. ❌ 不允许在前端显示 FSU FTP 明文密码
11. ❌ 不允许新增非 B接口协议入口
12. ❌ 不允许破坏现有 713 个 B-interface 测试

## 十六、风险清单

| 风险 | 等级 | 缓解措施 |
|------|------|---------|
| 开发环境误启用真实调用 | 中 | real-call-enabled 默认 false，需显式配置 |
| SET_THRESHOLD 配置错误仍可手动开启 | 中 | enabled=false 默认关闭，需配置+重启 |
| FSU 地址管理未实现 (serviceUrl=null) | 中 | 真实联调前必须补充 FSU endpoint 来源 |
| SlowDataPollingService 允许 real call 配置检查不足 | 低 | allow-real-call=Stub 属性 + real-call-enabled=FsuClient 属性分离 |
| 审计数据内存存储重启丢失 | 中 | 后续需持久化到数据库 |
| @EnableScheduling 未全局开启 | 低 | Scheduler 即使 enabled=true 也不触发 |
| SET_FSUREBOOT 无 Handler（路由到 Unknown） | 低 | 返回 error，不会误执行 |

## 十七、是否建议进入真实联调

**有条件建议。**

满足条件后可以进入真实联调：
1. ✅ 所有 6 个查询类命令已完成 Stub 闭环
2. ✅ 安全门禁已配置到位
3. ✅ 高风险命令保持禁用
4. ✅ 敏感字段已脱敏
5. ✅ 配置默认值安全
6. ⚠️ **需补充：FSU 地址管理** — 当前所有 Service 层传 null serviceUrl，真实联调时必须传入 FSU endpoint
7. ⚠️ **需确认：联调环境** — 明确联调 FSU 的 IP/Port/FSUService endpoint
8. ⚠️ **需确认：人员授权** — 明确哪些人员有权操作

## 十八、下一步建议

1. **联调前准备**：补充 FSU 地址发现逻辑（从 FsuDeviceEntity 获取 FSU endpoint）
2. **联调计划制定**：确认联调 FSU、联调范围、联调步骤、回滚方案
3. **真实联调执行**：先执行 GET 类命令联调，确认通讯正常
4. **SET_THRESHOLD 联调**：只在 GET 类命令通过后，且经单独授权后
5. **SET_POINT/SET_FTP/SET_FSUREBOOT**：当前严格禁止，需单独安全评估
