# BIF-P2-002：慢数据通道（SC→FSU）审计与优先级规划

> 对应阶段：BIF-P2-002
> 完成日期：2026-05-14
> 影响范围：仅只读审计，不修改业务代码

---

## 一、本阶段目标

对所有 SC→FSU 方向的慢数据通道命令进行全面审计，评估当前实现状态、分类风险等级、确定开发优先级，为 P3 阶段的实施提供决策依据。

---

## 二、慢数据通道总览

### 2.1 协议定义

B接口协议 2016 定义了两类消息通道：

| 通道 | 方向 | 触发方 | 用途 | 状态 |
|------|------|--------|------|------|
| 快数据通道 | FSU→SC | FSU 主动上报 | 登录/心跳/实时数据/告警 | BIF-P1 已完成 |
| 慢数据通道 | SC→FSU | SC 轮询/触发 | 参数查询/控制/配置 | BIF-P2 待实现 |

慢数据命令由 SC（平台）发起，经 FSUService 调用发送请求到 FSU，FSU 处理后返回响应。SC 侧需要有触发入口（UI/定时任务）和 FSU 通信客户端。

### 2.2 完整命令清单

| # | PK_Type | 方向 | Handler | Fixture | 说明 |
|---|---------|------|---------|---------|------|
| 1 | GET_DATA | SC→FSU | 桩 | xmldata + soap | 轮询 FSU 监控数据 |
| 2 | GET_THRESHOLD | SC→FSU | 桩 | xmldata + soap | 读取 FSU 告警阈值 |
| 3 | SET_THRESHOLD | SC→FSU | 桩 | xmldata + soap | 设置 FSU 告警阈值 |
| 4 | SET_POINT | SC→FSU | 桩 | xmldata + soap | 遥控/遥调命令 |
| 5 | GET_FTP | SC→FSU | 桩 | xmldata + soap | 获取 FTP 配置 |
| 6 | SET_FTP | SC→FSU | 桩 | xmldata + soap | 设置 FTP 参数 |
| 7 | GET_LOGININFO | SC→FSU | 桩 | xmldata + soap | 查询 FSU 登录状态 |
| 8 | TIME_CHECK | SC→FSU | 桩 | xmldata + soap | 时间同步校验 |
| 9 | GET_HISTORY_DATA | SC→FSU | ❌ 无 | ❌ 无 | 历史数据查询 |
| 10 | GET_FSUINFO | SC→FSU | ❌ 无 | ❌ 无 | 获取 FSU 设备信息 |
| 11 | SET_DATA | SC→FSU | ❌ 无 | ❌ 无 | 设置监控参数 |
| 12 | SET_FSUREBOOT | SC→FSU | ❌ 无 | ❌ 无 | 远程重启 FSU |

### 2.3 当前实现状态

所有 8 个已注册的 Handler 均为**纯桩**：

```java
@Override
public CommandResult handle(CommandContext context) {
    return CommandResult.notImplemented(BInterfacePkType.XXX);
    // 返回 resultCode="1", implemented=false
}
```

底层基础设施已就绪：

| 组件 | 状态 | 说明 |
|------|------|------|
| CommandDispatcher | 完成 | 支持任意 PK_Type 路由 |
| XmlDataParser/Builder | 完成 | 可解析/构造所有命令的 xmlData |
| SoapMessageHandler | 完成 | 可构造标准 SOAP 响应 |
| FsuServiceClient | 桩 | 需真实 HTTP/SOAP 客户端实现 |
| FsuServiceClientStub | 桩 | 空方法实现 |
| 登录态校验 | 完成 | LoginService.isLoggedIn() 可用 |

---

## 三、风险分类

### 3.1 高风险命令（禁止真实下发）

| 命令 | 风险 | 说明 |
|------|------|------|
| **SET_POINT** | ⚠ 极高 | 遥控/遥调，直接控制物理设备（断路器、调压器等）。一旦误下发可能导致设备损坏或安全事故 |
| **SET_FSUREBOOT** | ⚠ 极高 | 远程重启 FSU，可能导致监控中断、数据丢失。必须有二次确认和操作审计 |
| **SET_FTP** | ⚠ 高 | 修改 FSU 网络配置参数（IP、端口、密码）。若配置错误会直接导致 FSU 失联 |
| **SET_THRESHOLD** | ⚠ 中高 | 修改告警阈值，误设置会导致漏报或误报 |

### 3.2 中等风险命令

| 命令 | 风险 | 说明 |
|------|------|------|
| **SET_DATA** | 中 | 设置 FSU 监控参数，影响采集行为 |
| **TIME_CHECK** | 低 | 时间同步，异常不影响核心功能 |

### 3.3 低风险命令（只读）

| 命令 | 风险 | 说明 |
|------|------|------|
| **GET_DATA** | 低 | 只读轮询，不修改 FSU 状态 |
| **GET_THRESHOLD** | 低 | 只读查询 |
| **GET_FTP** | 低 | 只读查询 |
| **GET_LOGININFO** | 低 | 只读查询 |
| **GET_HISTORY_DATA** | 低 | 只读查询 |
| **GET_FSUINFO** | 低 | 只读查询 |

---

## 四、优先级评估

### 4.1 优先级矩阵

| 优先级 | 命令 | 理由 |
|--------|------|------|
| **P0** | GET_DATA | 核心监控功能，轮询 FSU 采集数据 |
| **P1** | GET_THRESHOLD, SET_THRESHOLD | 告警阈值管理，与 SEND_ALARM 配合 |
| **P1** | TIME_CHECK | 时间同步，确保 FSU 采集时间准确 |
| **P2** | GET_LOGININFO | 查询 FSU 在线状态，与离线检测配合 |
| **P2** | GET_FTP, SET_FTP | FTP 文件传输配置 |
| **P3** | SET_POINT | 遥控，高风险需谨慎设计 |
| **P3** | GET_HISTORY_DATA, GET_FSUINFO, SET_DATA | 低频使用 |
| **P4** | SET_FSUREBOOT | 极高风险，需完整的安全审批流程 |

### 4.2 依赖关系

```
P0: GET_DATA
     ├── 前置依赖: LoginService（已就绪）
     ├── 前置依赖: FsuServiceClient HTTP 客户端（需实现）
     └── 前置依赖: 前端展示页面（需确认）

P1: GET_THRESHOLD → SET_THRESHOLD
     ├── 前置依赖: monitoring_point 告警阈值字段（需确认 DB 字段）
     └── 前置依赖: 阈值管理 UI（需确认优先级）

P1: TIME_CHECK
     └── 前置依赖: FsuServiceClient（需实现）

P2: GET_LOGININFO
     └── 前置依赖: 无（可直接基于 FsuStatusEntity 实现服务端）

P3: SET_POINT
     ├── 前置依赖: 操作确认 UI
     ├── 前置依赖: 操作审计日志
     └── 前置依赖: ⛔ 二次确认门禁
```

---

## 五、架构分析

### 5.1 慢数据命令的双面性质

每个慢数据命令在 SC 侧涉及两个角色：

```
Operator/UI → CommandDispatcher → SlowDataHandler
                                         ↓
                                  FsuServiceClient
                                         ↓
                                  HTTP SOAP → FSU 设备
                                         ↓
                                  FSU 响应解析
                                         ↓
                                  CommandResult ← 结果返回调用方
```

1. **接收处理端（FSU→SC 响应）**：已通过 CommandDispatcher + Handler 注册机制就绪
2. **主动调用端（SC→FSU 请求）**：FsuServiceClient 仍为全桩，需实现真实 SOAP/HTTP 调用

### 5.2 FsuServiceClient 当前状态

```java
public interface FsuServiceClient {
    void login(String fsuCode);                    // 桩
    void heartbeat(String fsuCode);                // 桩
    Object getData(String fsuCode, String pointIds); // 桩，返回 null
    void setData(String fsuCode, String pointId, String value); // 桩
    void reboot(String fsuCode);                   // 桩
}
```

缺少：
- HTTP SOAP 客户端依赖（Spring WS / OkHttp + SOAP）
- FSU 地址管理（如何知道 FSU 的 endpoint URL？）
- 超时/重试策略
- 响应解析层

### 5.3 路由与指令流

CommandDispatcher 通过 Map<BInterfacePkType, CommandHandler> 路由，所有 Handler 已注册为 @Component。当前已注册 16 个 Handler（11 个 PK_Type + 5 个待补充的 UNKNOWN 等）。

注意：GET_HISTORY_DATA / GET_FSUINFO / SET_DATA / SET_FSUREBOOT 尚未创建 Handler 类，虽在 BInterfacePkType 枚举中定义，但未被 Spring 扫描注入。

---

## 六、Fixtures 覆盖

### xmldata 目录（24 个文件）

| 类型 | Request | Response | 覆盖 |
|------|---------|----------|------|
| LOGIN | ✓ | ✓ | BIF-P1 已使用 |
| HEARTBEAT | ✓ | ✓ | BIF-P1 已使用 |
| SEND_DATA | ✓ | ✓ | BIF-P1 已使用 |
| SEND_ALARM | ✓ | ✓ | BIF-P1 已使用 |
| GET_DATA | ✓ | ✓ | 未使用 |
| GET_THRESHOLD | ✓ | ✓ | 未使用 |
| SET_THRESHOLD | ✓ | ✓ | 未使用 |
| SET_POINT | ✓ | ✓ | 未使用 |
| GET_FTP | ✓ | ✓ | 未使用 |
| SET_FTP | ✓ | ✓ | 未使用 |
| GET_LOGININFO | ✓ | ✓ | 未使用 |
| TIME_CHECK | ✓ | ✓ | 未使用 |

### soap/fsu_service 目录（14 个文件）

GET_DATA, GET_THRESHOLD, SET_THRESHOLD, SET_POINT, GET_FTP, SET_FTP, GET_LOGININFO, TIME_CHECK 各有完整的请求/响应 SOAP 报文。

缺失：
- GET_HISTORY_DATA: xmldata + soap 均无
- GET_FSUINFO: xmldata + soap 均无
- SET_DATA: xmldata + soap 均无
- SET_FSUREBOOT: xmldata + soap 均无

---

## 七、结论与建议

### 立即可行（无需新增 Handler）

| 命令 | 实现方案 | 预估工作量 |
|------|---------|-----------|
| **GET_LOGININFO** | 直接在 Handler 中查询 BInterfaceFsuStatusEntity，无需调用 FSU | ~2 天（Handler + Service + Test） |
| **TIME_CHECK** | 返回 SC 服务器当前时间，无需调用 FSU | ~1 天 |

### 需先完成 FsuServiceClient

| 命令 | 前置条件 | 预估工作量 |
|------|---------|-----------|
| **GET_DATA** | FsuServiceClient HTTP 客户端 + FSU 地址管理 | ~5 天 |
| **GET_THRESHOLD** | FsuServiceClient 完成后 | ~3 天 |
| **SET_THRESHOLD** | GET_THRESHOLD 完成后 | ~3 天 |

### 高风险需门禁

| 命令 | 门禁要求 | 预估工作量 |
|------|---------|-----------|
| **SET_POINT** | 二次确认 UI + 操作审计 + 操作人鉴权 | ~5 天 |
| **SET_FSUREBOOT** | 同上 + 审批流程 | ~5 天 |

### 不推荐优先实现

| 命令 | 理由 |
|------|------|
| **GET_HISTORY_DATA** | 需 FSU 支持历史数据存储，依赖 FSU 固件能力 |
| **GET_FSUINFO** | FSU 设备信息应在注册时采集，非轮询 |
| **SET_DATA** | 与 SET_POINT/SET_THRESHOLD 功能重叠 |

---

## 八、慢数据命令优先级总表

| 优先级 | 命令 | 风险 | 前置依赖 | 建议阶段 |
|--------|------|------|---------|---------|
| P0 | GET_DATA | 低 | FsuServiceClient HTTP | BIF-P3-001 |
| P1 | GET_THRESHOLD | 低 | FsuServiceClient | BIF-P3-002 |
| P1 | SET_THRESHOLD | 中高 | GET_THRESHOLD + 校验 | BIF-P3-002 |
| P1 | TIME_CHECK | 低 | 无 | BIF-P3-003 |
| P2 | GET_LOGININFO | 低 | 无 | BIF-P3-003 |
| P2 | GET_FTP | 低 | FsuServiceClient | BIF-P3-004 |
| P2 | SET_FTP | 高 | GET_FTP + 确认门禁 | BIF-P3-004 |
| P3 | SET_POINT | 极高 | UI + 审计 + 门禁 | BIF-P3-005 |
| P3 | GET_HISTORY_DATA | 低 | FSU 固件调研 | BIF-P4-001 |
| P3 | GET_FSUINFO | 低 | 无 | BIF-P4-001 |
| P3 | SET_DATA | 中 | 与 SET_POINT 合并评估 | BIF-P4-001 |
| P4 | SET_FSUREBOOT | 极高 | 完整审批流程 | BIF-P4-002 |

---

## 九、未覆盖风险

| 风险 | 说明 | 等级 |
|------|------|------|
| FSU 地址未知 | 当前 FsuDeviceEntity 可能缺少 FSU 网络地址字段 | 高 |
| FSU 离线不可达 | 慢数据命令无法发送到离线 FSU | 中 |
| HTTP SOAP 客户端未选型 | Spring WebServiceTemplate / OkHttp + 手动 SOAP | 中 |
| 超时/重试未设计 | FSU 在网络不稳定场景下可能超时 | 中 |
| 命令并发冲突 | 多个慢数据命令同时下发到同一 FSU | 低 |
| 操作审计缺失 | SET_POINT 等控制命令无审计日志 | 高（高风险命令） |
