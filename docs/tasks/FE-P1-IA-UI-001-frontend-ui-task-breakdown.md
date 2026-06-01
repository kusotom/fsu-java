# FE-P1-IA-UI-001: 前端 UI 改造任务分解

**日期:** 2026-05-29
**来源:** FE-P1-IA-UI-001 信息架构与 UI 规划

---

## P1A: 基础架构 + 核心页面 (优先)

### FE-P1-IA-002: 菜单与路由重组
- **文件**: `router/index.ts`, `layouts/BasicLayout.vue`
- **内容**: 6 模块菜单, 28 路由迁移, 旧路由重定向
- **风险**: 路由变更影响所有页面入口, 需全量回归测试
- **验收**: npm run build + 所有路由可访问

### FE-P1-UI-001: 基础 UI 组件与视觉规范
- **新增组件**: MetricCard, FilterPanel, DetailDrawer, RawXmlViewer, ErrorState, AlarmLevelTag, FsuOnlineBadge, AuditEventTag
- **增强组件**: StatusBadge, ApiErrorAlert→DataStateAlert, PermissionButton, EmptyState
- **CSS 变量**: 主色/状态色/告警色全局变量
- **验收**: Storybook 级组件展示

### FE-P1-DASHBOARD-001: 首页总览改版
- **文件**: `views/dashboard/DashboardView.vue` (重写)
- **内容**: MetricCard 行 + ECharts 在线率 + 双列表 + 待处理
- **API**: sites, fsus, alarms, message-logs
- **验收**: 6 指标卡 + 趋势图 + 列表正确展示

### FE-P1-SITE-001: 站点监控页
- **文件**: `views/sites/SiteListView.vue` + `SiteDetailView.vue`
- **内容**: 站点卡片网格 + 详情抽屉 (含 FSU 列表/告警/实时值)
- **API**: sites, fsus/{fsuCode}/devices, alarms?fsuCode=

---

## P1B: 业务闭环 (次优先)

### FE-P1-REALTIME-001: 实时数据统一
- 合并 `telemetry/RealtimeDataView.vue` + `binterface/BInterfaceRealtimeView.vue`
- 删除 compat/realtimeSignalFilter.ts (如后端字段已补齐)

### FE-P1-ALARM-001: 告警中心改版
- 告警列表 + 确认/清除 + 告警详情

### FE-P1-MAPPING-001: 点位映射
- 点位字典 + 映射管理 + 未映射点位

### FE-P1-TENANT-001: 三方授权
- 租户管理 + 用户/角色/权限 + scope 授权

### FE-P1-AUDIT-001: 系统审计
- 审计日志 + 5 种事件筛选

### FE-P1-PROTOCOL-001: 协议诊断下沉
- 9 个协议页面 UI 优化 + 视觉隔离
