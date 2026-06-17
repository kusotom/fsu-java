# FE-UI-STYLE-REFIT-P1-001 浅色物联网平台视觉改造审计报告

## 1. 任务背景

本次任务根据用户提供的浅色物联网平台参考风格，对当前机房动环监控平台前端进行 P1 级视觉改造。目标是弱化工程调试后台观感，形成浅色侧栏、白色 Header、浅灰内容背景、白色卡片和蓝色主色调的监控平台风格。

本次只改前端 UI、布局、组件样式和普通业务页面展示，不改后端业务逻辑、协议逻辑、DataScope、权限守卫或 SET 安全门。

## 2. 参考 UI 风格说明

参考方向采用浅色物联网平台风格：

- 浅色左侧菜单、白色顶部 Header。
- 内容区浅灰背景，业务信息以白色卡片承载。
- 蓝色主色调，菜单选中态为浅蓝背景 + 蓝色文字。
- 筛选区紧凑横向排列，表格边框更轻。
- FSU/设备类信息优先使用卡片化展示。

本次未复制 BladeX 名称、Logo、图片素材或原始布局代码。

## 3. 改造范围

改造文件集中在前端：

- `frontend/src/styles/tokens.css`
- `frontend/src/styles/theme.css`
- `frontend/src/styles/layout.css`
- `frontend/src/styles/status.css`
- `frontend/src/layouts/BasicLayout.vue`
- `frontend/src/components/PageHeader.vue`
- `frontend/src/components/SiteMonitorTabs.vue`
- `frontend/src/components/common/MetricCard.vue`
- `frontend/src/components/common/StatusBadge.vue`
- `frontend/src/components/common/FsuOnlineBadge.vue`
- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/alarm/AlarmCenterView.vue`
- `frontend/src/views/binterface/FsuStatusView.vue`

## 4. 修改前 UI 问题

- 侧栏仍使用深色背景，更接近工程调试后台。
- Header 与页面标题区较基础，缺少平台级层次感。
- FSU 管理仍为表格，设备类资产不够卡片化。
- 实时数据主表突出 `mappingConfidence`、`source` 等技术字段。
- 告警主表仍展示置信度字段，协议排查字段和业务字段区分不够清晰。
- 旧 CSS 中存在 `//` 注释，构建时产生 CSS warning。

## 5. 修改后 UI 结构

- 全局 token 改为浅色物联网平台风格：`#2F80ED` 主色、`#ECF5FF` 浅蓝选中背景、`#F5F7FA` 内容背景。
- Sidebar 改为白色，菜单选中态为浅蓝底蓝字。
- Header 改为白色，展示当前页面标题和平台副标题。
- 业务页面使用 `page-template`、`metric-row`、`app-card`、`dashboard-grid`、`fsu-card-grid` 统一布局。
- 表格、筛选区、标签、指标卡使用更轻的边框和阴影。

## 6. 菜单结构是否保持收敛

保持 FE-IA-CLEANUP-P1-001 的普通业务菜单口径：

- 监控中心：监控驾驶舱、站点实时数据、告警中心
- 站点监控：站点列表、FSU 管理
- 三方授权：用户管理、角色管理、权限管理、站点授权、FSU 授权
- 系统设置：安全设置

未恢复普通菜单：

- 资产与点位
- 设备管理
- 机柜管理
- 点位字典
- 点位映射
- 未映射点位
- 系统审计 / 登录审计 / 操作审计 / 协议审计
- 协议诊断 / raw XML / run-once

## 7. Layout 改造说明

`BasicLayout.vue` 改为浅色平台壳：

- `app-shell` 使用浅灰背景。
- `app-sidebar` 使用白色背景和浅色边框。
- `app-header` 使用白色背景、轻边框和当前路由标题。
- `activeMenu` 对 FSU 详情、站点实时数据等子路由做高亮归并。

## 8. Sidebar 改造说明

- 删除硬编码深色 `background-color/text-color/active-text-color`。
- 使用 CSS token 控制浅色菜单。
- 菜单项选中态为浅蓝底、蓝色文字和半粗字重。
- 平台名保留“机房动环监控平台”，未使用参考图品牌。

## 9. Header 改造说明

- Header 保持白底，左侧展示折叠按钮、当前页面标题和“动力环境监控与站点运维”副标题。
- 右侧保留当前时间。
- 未新增用户、权限或协议操作入口。

## 10. FSU 管理卡片化说明

`FsuStatusView.vue` 从表格改为卡片网格：

- 展示 FSU 编码、站点名称、在线状态、心跳状态、最近登录时间、最近心跳时间、设备数量、告警数量、最后更新时间。
- 卡片使用蓝色设备图标，不使用外部图片素材。
- 普通操作为详情、设备列表、刷新状态。
- 通信记录按钮仅在 `protocol:raw:view` 权限可用时显示，未新增普通用户 run-once 或 SET 操作。

## 11. 实时数据页改造说明

`RealtimeDataView.vue` 保留当前正确统计口径：

- 实时点位
- 有值点位
- 真实未映射
- 历史待回填
- 解析异常

主表弱化技术字段：

- 保留 FSU、设备、点位名称、当前值、单位、类型、质量、状态、采集时间。
- `valueMeaning` 下沉到当前值下方。
- `mappingConfidence`、`templateVariant`、`source` 不再作为普通主列突出展示。
- `legacy_realtime_data` 以“历史数据”标签显示，不突出为真实未映射。

## 12. 驾驶舱改造说明

`DashboardView.vue` 改为浅色卡片式驾驶舱：

- 第一行指标：站点总数、FSU 在线、当前告警、严重告警、历史待回填。
- 卡片区：FSU 状态分布、告警等级分布、最新告警、站点摘要、异常 FSU 摘要、通信状态。
- 不恢复点位字典、点位映射或未映射入口。

## 13. 告警中心改造说明

`AlarmCenterView.vue` 改为轻量表格：

- 当前告警 / 历史告警 Tab 保留。
- 增加设备列，告警等级使用统一标签。
- 主表保留映射状态，但移除置信度主列。
- SPID、EventID、SignalID、RawXml ID 等协议字段继续下沉到详情抽屉。

## 14. 权限影响分析

本次未修改 `router/index.ts`、`routeGuard.ts`、`permissions.ts` 或后端权限逻辑。

复查结果：

- 三方授权仍由 `userStore.isTenantAdmin` 控制菜单可见性。
- 点位治理、协议诊断、系统审计仍不在普通菜单。
- raw XML 隐藏路由仍要求 `protocol:raw:view` 和 elevated 角色。
- run-once 隐藏路由仍要求 `protocol:runonce:readonly` 和 elevated 角色。
- 未新增 SET、Controls 或真实 FSU 写操作入口。

## 15. 修改文件清单

- `frontend/src/styles/tokens.css`
- `frontend/src/styles/theme.css`
- `frontend/src/styles/layout.css`
- `frontend/src/styles/status.css`
- `frontend/src/layouts/BasicLayout.vue`
- `frontend/src/components/PageHeader.vue`
- `frontend/src/components/SiteMonitorTabs.vue`
- `frontend/src/components/common/MetricCard.vue`
- `frontend/src/components/common/StatusBadge.vue`
- `frontend/src/components/common/FsuOnlineBadge.vue`
- `frontend/src/views/dashboard/DashboardView.vue`
- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/alarm/AlarmCenterView.vue`
- `frontend/src/views/binterface/FsuStatusView.vue`

## 16. 测试结果

已执行：

```bash
cd frontend
npm run build
```

结果：通过。

说明：

- `vue-tsc --noEmit` 通过。
- `vite build` 通过。
- 旧 CSS `//` 注释 warning 已消除。
- 仍有来自 `@vueuse/core` 的 Rollup pure annotation warning 和既有 chunk size warning，非本次改造引入的阻断问题。

未执行后端测试，原因：本次未修改后端代码、路由权限、route guard、DataScope 或安全拦截逻辑。

## 17. P0 / P1 / P2 问题清单

P0：无新增。

P1：

- FSU 详情页仍可继续增强设备列表、机柜信息、点位概览等 Tab 的业务化展示。
- 三方授权页面当前仍为轻量占位或静态展示，后续需接入真实授权 API。

P2：

- 可引入 ECharts 做 FSU 状态、告警等级、数据质量趋势图。
- 可进一步做表格/卡片视图切换、用户偏好记忆和响应式细节优化。

## 18. 后续建议

- 进入 `FE-IA-CLEANUP-P1-002`，将设备、机柜、点位概览逐步归并到 FSU 详情页。
- 进入 `FE-AUTH-P1-001`，让站点授权和 FSU 授权接入真实后端 API。
- 进入 `FE-DASHBOARD-CHART-P2-001`，补充驾驶舱图表与趋势。

## 19. 最终结论

FE-UI-STYLE-REFIT-P1-001 完成，前端已调整为浅色物联网平台风格，菜单结构保持收敛，FSU 管理、实时数据、告警中心和驾驶舱完成统一视觉改造；未恢复点位治理、系统审计或协议诊断普通入口，安全边界未回退。
