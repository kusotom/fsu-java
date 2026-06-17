# FE-P1-DASHBOARD-002: 首页业务指标与点位映射状态优化

## 目标

在 FE-P1-DASHBOARD-001 基础上进行二次优化，强化首页业务风险展示，将未映射点位、异常 FSU、点位映射状态纳入驾驶舱视图。

## 范围

- 只改 `DashboardView.vue`
- 小幅增强 `MetricCard.vue`（添加可选的 `to` prop）
- 新增 `frontend/src/utils/dashboardNormalizers.ts`
- 不新增后端接口，不改路由/权限/CSS token

## 布局方案

```
第一行: MetricCard × 4
  [站点总数] [FSU 在线率] [当前告警] [未映射点位]
  → /sites   → /b-interface/fsus → /alarms → /unmapped

第二行: 风险状态区
  [严重告警] [主要告警] [离线FSU] [心跳丢失] [ACK空数据] [解析异常]

第三行: 两列布局
  左1/2: 最新告警列表
  右1/2: 异常FSU列表

第四行: 两列布局
  左2/3: 未映射点位 Top 列表
  右1/3: 点位映射状态分布

底部: DataStateAlert (空状态提示)
```

## MetricCard 增强

```ts
to?: string | RouteLocationRaw  // 可选，有值时卡片可点击跳转
```

不传 `to` 时行为不变（向后兼容）。

## 数据规范化 (dashboardNormalizers.ts)

```ts
normalizeDashboardMetrics(sites, fsus, alarms, unmapped) → DashboardMetrics
normalizeUnmappedTopList(unmappedSignals, limit=5) → UnmappedSummary[]
normalizeAbnormalFsuList(fsus, limit=5) → AbnormalFsu[]
normalizeMappingDistribution(unmappedCount, mappedCount, total?) → MappingDistribution
resolveDashboardErrorState(errors: Record<string, any>) → DataState
```

## 数据来源

| 数据 | API |
|------|-----|
| 站点数 | `getSites()` |
| FSU 列表/在线状态 | `getBInterfaceFsus()` |
| 告警列表 | `getBInterfaceAlarms({ alarmStatus: 'ACTIVE' })` |
| 未映射点位 | `getBInterfaceUnmappedSignals()` |
| 消息日志(通信状态) | `queryBInterfaceMessageLogs({ size: 1 })` |

## 错误处理

区分 401/403/404/5xx/network error，每类错误有对应的 DataStateAlert state。

## 禁止事项

- 不访问真实 FSU，不触发 GET_DATA
- 不新增 raw XML / run-once / SET 入口
- 不使用 demo 数据
- 不修改路由/权限/CSS token
- MetricCard 旧用法不受影响
- 首页不做映射操作、标记忽略、导出清单
