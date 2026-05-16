# BIF-P3-004：慢数据通道定时轮询框架

> 对应阶段：BIF-P3-004
> 完成日期：2026-05-15
> 影响范围：新增 4 个生产文件、修改 1 个配置文件、新增 4 个测试文件（38 测试）

---

## 一、本阶段目标

基于已完成的 FsuServiceClient、GET_DATA、GET_THRESHOLD 安全闭环，建立慢数据通道的定时轮询框架。提供可测试、默认关闭、Stub 模式的轮询编排能力，不允许默认访问真实设备，不允许真实批量采集，不允许真实下发控制命令。

## 二、慢数据轮询架构

```
Scheduler (SlowDataPollingScheduler, 默认 disabled)
    ↓
SlowDataPollingService.runOnce()
    ↓
FsuDeviceRepository.findAll() → 候选 FSU 列表
    ↓
isOnline() + isLoggedIn() 过滤
    ↓
GetDataService.execute() (GET_DATA 轮询)
    ↓
GetThresholdService.execute() (GET_THRESHOLD 可选)
    ↓
SlowDataPollingResult (统计/审计)
```

### 2.1 层次职责

| 层次 | 职责 | 文件 |
|------|------|------|
| Scheduler | 只触发，不写业务逻辑 | SlowDataPollingScheduler |
| PollingService | 任务编排 | SlowDataPollingService |
| FsuDeviceRepository | 提供候选 FSU 列表 | FsuDeviceRepository |
| LoginService | 在线/登录态校验 | LoginService |
| GetDataService | GET_DATA 业务 | GetDataService |
| GetThresholdService | GET_THRESHOLD 业务 | GetThresholdService |
| 结果模型 | 轮询统计 | SlowDataPollingResult |
| 配置 | 开关控制 | SlowDataPollingProperties |

### 2.2 避免的设计

- Scheduler 内不包含任何 GET_DATA / GET_THRESHOLD 逻辑
- PollingService 不直接调用 FsuServiceClient
- PollingService 不涉及 SET_THRESHOLD / SET_POINT
- PollingService 不访问真实设备

---

## 三、SlowDataPollingService 职责

| 职责 | 说明 |
|------|------|
| 查询候选 FSU | FsuDeviceRepository.findAll() |
| 跳过 OFFLINE FSU | isOnline() 检查 onlineStatus |
| 跳过未登录 FSU | loginService.isLoggedIn() 检查 |
| 执行 GET_DATA | getDataService.execute(fsuCode, null, List.of()) |
| 可选执行 GET_THRESHOLD | 仅当 poll-get-threshold=true |
| 统计计数 | scanned/skipped/success/failure/errors |
| 异常安全 | 单个 FSU 失败不影响其他，不抛未捕获异常 |

## 四、Scheduler 启用状态

**默认 disabled。** 必须同时设置以下两个配置才能启用定时调度：

```yaml
b-interface:
  slow-polling:
    enabled: true
    scheduler-enabled: true
```

Scheduler 使用 `@Scheduled(fixedDelayString = ...)` 注解，依赖 Spring `@EnableScheduling`（当前项目未全局开启，需后续按需启用）。

Scheduler 内部包含 enabled + scheduler-enabled 双重校验，即使 @Scheduled 触发，配置不允许时立即返回。

## 五、配置项说明

```yaml
b-interface:
  slow-polling:
    enabled: false              # 总开关
    scheduler-enabled: false    # 定时调度开关
    allow-real-call: false      # 是否允许真实 FSU 调用
    poll-get-data: true         # 是否轮询 GET_DATA
    poll-get-threshold: false   # 是否轮询 GET_THRESHOLD（默认关闭）
    interval-seconds: 300       # 轮询间隔（秒）
    max-fsu-per-run: 50         # 单次最大处理 FSU 数
```

## 六、GET_DATA 轮询策略

- 默认开启（poll-get-data=true）
- 对每个 ONLINE + 已登录 FSU 执行
- 当前无信号列表配置，传空 List（实际查询时 GetDataService 会返回 2003）
- **遗留问题**：需后续阶段增加每个 FSU 的信号配置

## 七、GET_THRESHOLD 轮询策略

- 默认关闭（poll-get-threshold=false）
- 需要显式开启
- 与 GET_DATA 共享相同的 FSU 过滤条件
- 单独统计 success/failure 计数

## 八、禁止 SET 类命令轮询说明

- 配置项中无任何 SET 类命令配置
- SlowDataPollingService 不依赖 SetThresholdService / SetThresholdSafetyGate
- SetThresholdCommandHandler 保持独立，不参与轮询
- SET_POINT / SET_FTP / SET_FSUREBOOT 不在任何轮询路径中

## 九、与 FsuServiceClient 的关系

- PollingService 不直接调用 FsuServiceClient
- 所有 FSU 调用通过 GetDataService / GetThresholdService 间接完成
- FsuServiceClient 的 Stub/Real 选择由配置 `real-call-enabled` 控制
- PollingService 的 `allow-real-call` 是额外安全层，配置独立

## 十、与 OfflineDetection 的关系

| 特性 | OfflineDetectionService | SlowDataPollingService |
|------|------------------------|----------------------|
| 职责 | 离线检测 | 慢数据轮询 |
| 触发 | 定时扫描 | 手动/定时 |
| 操作 | 置 OFFLINE | 查询 GET_DATA / GET_THRESHOLD |
| 前置依赖 | 无 | 需要 FSU 状态为 ONLINE + LOGIN |
| 边界 | 修改状态 | 读取状态 |

轮询前检查 isOnline()，利用 OfflineDetection 维护的在线状态，不直接修改状态。

## 十一、与 GET_DATA / GET_THRESHOLD / SET_THRESHOLD 的边界

- GET_DATA：轮询框架使用，只读查询
- GET_THRESHOLD：轮询框架可配置使用，只读查询
- SET_THRESHOLD：**不进入轮询框架**，独立安全门禁控制
- 查询和设置在代码层面完全隔离

## 十二、安全边界

- `allow-real-call: false` 默认值，需显式开启
- `scheduler-enabled: false` 默认值，需显式开启
- SlowDataPollingService 的 `runOnce()` 即使手动调用也默认走 Stub
- 结果模型包含 `realCallEnabled` 字段，便于审计
- 所有测试验证不访问真实网络

## 十三、测试覆盖情况

### 13.1 新增测试

| 测试文件 | 测试数 | 类型 |
|---------|--------|------|
| SlowDataPollingPropertiesTest | 8 | 单元测试（默认值 + setter） |
| SlowDataPollingServiceTest | 16 | 单元测试（成功/跳过/失败/混合/配置） |
| SlowDataPollingSchedulerTest | 4 | 单元测试（disabled/enabled/异常安全） |
| SlowDataPollingIntegrationTest | 10 | 集成测试（全链路 + 在线/离线/GET_THRESHOLD） |

**新增测试合计：38**

### 13.2 测试场景覆盖

- 默认 polling enabled=false
- 默认 scheduler-enabled=false
- 默认 allow-real-call=false
- runOnce 使用 Stub，不访问网络
- ONLINE FSU 执行 GET_DATA 成功
- OFFLINE FSU 被跳过
- 未登录 FSU 被跳过
- 无状态 FSU 被跳过
- 单个 FSU GET_DATA 失败不影响其他 FSU
- poll-get-threshold=false 时不执行 GET_THRESHOLD
- poll-get-threshold=true 时执行 GET_THRESHOLD
- maxFsuPerRun 限制生效
- 统计计数正确（scanned/skipped/success/failure）
- errors 结构化返回
- realCallEnabled 按配置报告
- Scheduler disabled/enabled 行为
- Scheduler 异常安全
- 混合 FSU 状态（ONLINE + OFFLINE + 未登录 + 失败）
- 无 FSU 返回空结果
- 所有测试不访问真实设备

### 13.3 回归测试

| 测试范围 | 测试数 | 结果 |
|---------|--------|------|
| 全量 BInterface 测试 | 558 | 全部通过 |

## 十四、本阶段未处理内容

- 真实设备轮询（enabled=false + scheduler-enabled=false）
- 生产定时任务默认启用（scheduler-enabled=false）
- 每个 FSU 的信号列表配置（当前传空列表）
- GET_DATA 信号值持久化
- 前端轮询配置页
- SET_THRESHOLD / SET_POINT 定时执行（禁止）
- 复杂任务调度平台
- 分布式锁
- @EnableScheduling 全局开启

## 十五、遗留风险

| 风险 | 等级 | 说明 |
|------|------|------|
| GET_DATA 无 signalId 无法查询 | 中 | 当前传空列表，GetDataService 返回 2003；需后续增加 FSU 信号配置 |
| @EnableScheduling 未全局开启 | 低 | Scheduler 即使 enabled 也不触发，需后续配置类补充 |
| 轮询结果未持久化 | 低 | 当前仅内存返回，可扩展审计记录 |
| FSU 地址管理未实现 | 低 | serviceUrl 传 null |
| 单线程轮询可能阻塞 | 低 | 默认单线程，大批量 FSU 时可能延迟 |

## 十六、下一步建议

**BIF-P3-005**：慢数据通道 Signal 配置 — 为每个 FSU 配置轮询的信号列表，使 GET_DATA / GET_THRESHOLD 可真正执行。
