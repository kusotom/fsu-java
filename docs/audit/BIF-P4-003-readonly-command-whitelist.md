# BIF-P4-003：只读命令白名单 / 禁止命令黑名单

> 对应阶段：BIF-P4-003
> 完成日期：2026-05-15
> 状态：计划阶段，不修改业务代码

---

## 一、范围说明

本白名单/黑名单适用于 **单台真实 FSU 只读联调**。白名单中的命令可在本次联调中执行，黑名单中的命令无论任何情况均不得在本次联调中执行。

## 二、只读命令白名单

### 2.1 白名单列表

| PK_Type | Handler 状态 | Service 状态 | 说明 |
|---------|-------------|-------------|------|
| GET_DATA | ✅ 真实实现 | ✅ GetDataService | 查询 FSU 实时监控数据 |
| GET_THRESHOLD | ✅ 真实实现 | ✅ GetThresholdService | 查询 FSU 告警门限 |
| TIME_CHECK | ✅ 真实实现 | ✅ TimeCheckService | 时间同步校验 |
| GET_LOGININFO | ✅ 真实实现 | ✅ GetLoginInfoService | 查询 FSU 登录/在线状态 |
| GET_FTP | ✅ 真实实现 | ✅ GetFtpService | 查询 FSU FTP 配置（只读） |

### 2.2 白名单准入标准

每条白名单命令必须满足以下所有条件：

1. **只读**：不修改 FSU 任何配置或状态
2. **已实现**：Handler + Service 均为真实实现（非 notImplemented 桩）
3. **已测试**：有完整的单元测试和集成测试覆盖
4. **安全**：不涉及敏感凭证明文传输
5. **Stub 支持**：StubFsuServiceClient 有对应实现

### 2.3 白名单命令详情

#### GET_DATA
- **用途**：SC 主动向 FSU 查询实时监控数据
- **方向**：SC→FSU
- **B接口标准**：Info 含 FSUCode，xmlData 含 SignalID 列表
- **响应**：SignalID + Value + Quality + Status + CollectTime
- **安全边界**：不调用 SendDataService/SendAlarmService，不走入库

#### GET_THRESHOLD
- **用途**：SC 主动向 FSU 查询告警门限参数
- **方向**：SC→FSU
- **B接口标准**：Info 含 FSUCode，xmlData 含 SignalID 列表
- **响应**：SignalID + AlarmUpper + AlarmLower + AlarmUpperUrgent + AlarmLowerUrgent
- **安全边界**：不调用 SetThresholdService，不修改门限

#### TIME_CHECK
- **用途**：SC 主动向 FSU 发起时间同步校验
- **方向**：SC→FSU
- **B接口标准**：Info 含 FSUCode + StandardTime
- **响应**：FSUTime（ISO8601）
- **安全边界**：不修改 FSU 时间，不修改系统时间

#### GET_LOGININFO
- **用途**：SC 主动向 FSU 查询登录状态和在线信息
- **方向**：SC→FSU
- **B接口标准**：Info 含 FSUCode
- **响应**：LoginStatus + OnlineStatus + SessionID + LoginTime + LastHeartbeat
- **安全边界**：无 password 字段，不调用 LoginService

#### GET_FTP
- **用途**：SC 主动向 FSU 查询 FTP 配置参数
- **方向**：SC→FSU
- **B接口标准**：Info 含 FSUCode + 可选 FileType
- **响应**：Host + Port + Username + PassiveMode + BasePath
- **安全边界**：Username 脱敏输出（getMaskedUsername），不调用 SetFtpService

## 三、禁止命令黑名单

### 3.1 黑名单列表

| PK_Type | 风险等级 | 禁止原因 | Handler 状态 |
|---------|---------|---------|-------------|
| SET_POINT | 极高 | 遥控/遥调，可能直接控制物理设备 | notImplemented 桩 |
| SET_FTP | 高 | 修改 FTP 配置可能使 FSU 失联 | notImplemented 桩 |
| SET_FSUREBOOT | 极高 | 远程重启 FSU，需完整审批流程 | 无 Handler 类 |
| SET_THRESHOLD | 高 | 修改告警门限，本阶段只读 | 安全门禁默认关闭 |
| SET_DATA | 高 | 设置监控参数 | 未实现 |
| GET_HISTORY_DATA | 中 | 历史数据查询量大，本阶段不涉及 | 未实现 |
| GET_FSUINFO | 中 | 设备信息查询，本阶段不涉及 | 未实现 |

### 3.2 黑名单项详细说明

#### SET_POINT
- **风险**：极高——直接控制物理设备（开/关/遥调）
- **当前状态**：SetPointCommandHandler 为 notImplemented 桩
- **StubFsuServiceClient**：不支持
- **恢复条件**：需单独安全评估 + 完整审批

#### SET_FTP
- **风险**：高——修改 FTP 配置可能导致 FSU 断连
- **当前状态**：SetFtpCommandHandler 为 notImplemented 桩
- **StubFsuServiceClient**：不支持
- **恢复条件**：需单独安全评估

#### SET_FSUREBOOT
- **风险**：极高——远程重启 FSU，影响设备可用性
- **当前状态**：无对应 Handler 类，路由到 UnknownCommandHandler
- **StubFsuServiceClient**：不支持
- **恢复条件**：需完整审批流程 + 变更窗口

#### SET_THRESHOLD
- **风险**：高——本阶段仅执行只读查询
- **当前状态**：安全门禁默认关闭（enabled=false）
- **门禁配置**：require-confirmation=true（即使开启也需二次确认）
- **恢复条件**：本阶段过去后，经授权可在第二阶段联调

### 3.3 额外禁止操作

| 操作 | 禁止原因 |
|------|---------|
| 启用 slow-polling.enabled=true | 自动轮询可能触发大量请求 |
| 启用 scheduler-enabled=true | 定时任务不可控 |
| 启用 allow-real-call=true | 轮询发起真实调用风险 |
| 批量联调多个 FSU | 故障影响面不可控 |
| 修改生产环境配置 | 影响现网设备 |
| 硬编码设备凭证 | 安全违规 |

## 四、白名单/黑名单验证

### 4.1 自动化验证

`ReadOnlyIntegrationSafetyTest` 在编译期验证：

- 白名单包含且仅包含 5 个命令
- 白名单不含任何 SET_ 命令
- 黑名单不含任何 GET_ 命令
- 白名单与黑名单无交集

### 4.2 人工验证

- 确认白名单命令 Handler 均为真实实现
- 确认黑名单命令 Handler 均为 notImplemented/不存在
- 确认无遗漏命令

## 五、异常处理

| 场景 | 处理方式 |
|------|---------|
| 联调中误操作黑名单命令 | CommandDispatcher 路由到 notImplemented 桩，返回错误 |
| 配置误开启 SET_THRESHOLD | 安全门禁 require-confirmation=true 拦截 |
| 配置误开启 slow-polling | scheduler-enabled=false + @EnableScheduling 未开启 |
| 真实 FSU 无响应 | RealHttpFsuServiceClient 返回结构化 error 响应 |
