# BIF-P3-005：慢数据通道 Signal 配置 / 轮询目标配置

> 对应阶段：BIF-P3-005
> 完成日期：2026-05-15
> 影响范围：新增 2 个生产文件、修改 2 个生产文件、新增 1+3 个测试文件

---

## 一、本阶段目标

基于已完成的 BIF-P3-004 慢数据轮询框架，建立慢数据轮询所需的 Signal 目标配置能力，使 GET_DATA / GET_THRESHOLD 可以获取明确的 SignalID 列表。解决 BIF-P3-004 遗留的"GET_DATA 传空 signalId 列表"问题。

## 二、当前慢数据轮询遗留问题

| 问题 | 影响 | BIF-P3-005 解决 |
|------|------|----------------|
| GET_DATA 传空 signalId 列表 → 2003 | 轮询全部失败 | 从 MonitoringPoint 获取 signalId |
| 无 FSU 信号配置 | 无法执行轮询 | 新增 SignalPollingTargetService |
| 无 target 未统计 | 无法区分"无配置"和"失败" | 新增 skippedNoSignalCount |

## 三、Signal 目标配置架构

```
SlowDataPollingService
    ↓
SignalPollingTargetService.findTargets(fsuCode, commandType)
    ↓
FsuDeviceRepository.findByFsuCode(fsuCode) → fsuId
    ↓
MonitoringPointRepository.findByFsuId(fsuId) → 监控点列表
    ↓
filter status=ACTIVE → map pointCode → signalId
    ↓
List<SignalPollingTarget>
```

### 3.1 复用现有模型

| 现有模型 | 用途 | 字段映射 |
|---------|------|---------|
| FsuDeviceEntity | FSU 标识 | fsuCode → id |
| MonitoringPointEntity | 信号配置 | pointCode → SignalID, status → enabled |

不新增任何数据库表。

## 四、SignalPollingTargetService 职责

| 职责 | 说明 |
|------|------|
| 按 FSU 查询目标 | findTargets(fsuCode, commandType) |
| 按命令类型过滤 | 仅支持 GET_DATA / GET_THRESHOLD，其他返回空 |
| 状态过滤 | 仅返回 status=ACTIVE |
| 异常安全 | 所有异常返回空列表，不抛出 |
| 空值安全 | null/空 fsuCode → 空列表 |

## 五、SlowDataPollingService 改造说明

### 5.1 构造注入变更

新增 `SignalPollingTargetService` 第六个构造函数参数。

### 5.2 runOnce 流程变更

```
原流程：
  ONLINE + loggedIn → GET_DATA (List.of()) → 必然 2003

新流程：
  ONLINE + loggedIn → 查询 GET_DATA targets
    ├─ 无 target → skippedNoSignalCount++
    └─ 有 target → extractSignalIds → GET_DATA(signalIds)
  poll-get-threshold=true → 查询 GET_THRESHOLD targets
    ├─ 无 target → 跳过
    └─ 有 target → GET_THRESHOLD(signalIds)
```

### 5.3 SlowDataPollingResult 变更

新增 `skippedNoSignalCount` 字段，记录"ONLINE + 已登录但无信号配置"的 FSU 数量。

## 六、GET_DATA 目标配置策略

- 查询 SignalPollingTargetService.findTargets(fsuCode, GET_DATA)
- 从 MonitoringPoint 表获取所有 status=ACTIVE 的点位
- 使用 pointCode 作为 SignalID
- 不支持按 commandType 区分（同一点位可同时用于 GET_DATA 和 GET_THRESHOLD）

## 七、GET_THRESHOLD 目标配置策略

- 仅当 poll-get-threshold=true 时查询
- 查询 SignalPollingTargetService.findTargets(fsuCode, GET_THRESHOLD)
- 与 GET_DATA 使用相同的 monitoring_point 配置
- 单独统计 success/failure

## 八、禁止 SET 类命令进入轮询说明

- SignalPollingTargetService.findTargets() 对 SET_THRESHOLD 等命令返回空列表
- SlowDataPollingService 不引用 SetThresholdService / SetThresholdSafetyGate
- 无 SET 类命令的配置路径

## 九、Stub / 内存配置与未来数据库配置边界

| 当前实现 | 未来方向 |
|---------|---------|
| 复用 MonitoringPoint 表 | 可新增独立的 polling_target 表 |
| pointCode = SignalID | 可增加 signalId 映射字段 |
| status = ACTIVE 过滤 | 可增加 polling_enabled 独立开关 |
| fsuId 关联 | 可增加 fsuCode 直接关联 |

## 十、测试覆盖情况

### 10.1 新增/修改测试

| 测试文件 | 测试数 | 类型 |
|---------|--------|------|
| SignalPollingTargetServiceTest | 12 | 单元测试（GET_DATA/GET_THRESHOLD/空/disabled/边界） |
| SlowDataPollingServiceTest (扩展) | +1 | 新增无 target 跳过测试（现 17 测试） |

### 10.2 测试场景覆盖

- findTargets 返回 GET_DATA targets
- findTargets 返回 GET_THRESHOLD targets
- 无监控点时返回空列表
- FSU 不存在返回空列表
- null/空 fsuCode 返回空列表
- disabled 监控点被过滤
- SET_THRESHOLD 等命令返回空列表
- hasTargets 正确判断
- FSU 间数据隔离
- SlowDataPollingService 无 target 时跳过并统计 skippedNoSignalCount
- GET_DATA 不再传空 signalId 列表

### 10.3 回归测试

| 测试范围 | 测试数 | 结果 |
|---------|--------|------|
| 全量 BInterface 测试 | 571 | 全部通过 |

## 十一、本阶段未处理内容

- 前端配置页面（通过 MonitoringPointService CRUD 管理）
- 独立的 polling_target 配置表
- 按 commandType 区分信号（GET_DATA vs GET_THRESHOLD 用同一套点位）
- 信号自动发现
- 批量导入导出

## 十二、遗留风险

| 风险 | 等级 | 说明 |
|------|------|------|
| MonitoringPointEntity.status 语义不确定 | 低 | 当前以 "ACTIVE" 过滤，需与上游确认 |
| pointCode = SignalID 隐式映射 | 低 | 如需差异化映射需后续加表 |
| 无 commandType 区分 | 低 | GET_DATA 和 GET_THRESHOLD 用同一批信号 |
| 无前端管理页面 | 低 | 可通过 MonitoringPointService API 管理 |

## 十三、下一步建议

**BIF-P4-001**：慢数据通道启用确认 — 评估产出、确认配置、考虑是否在开发环境启用轮询。
