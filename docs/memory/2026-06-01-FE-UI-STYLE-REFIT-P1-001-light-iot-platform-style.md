# 2026-06-01 FE-UI-STYLE-REFIT-P1-001 浅色物联网平台视觉改造

## 本次任务目标

参考用户提供的浅色物联网平台 UI 风格，将机房动环监控平台前端从偏工程调试后台风格调整为浅色、轻量、卡片化的监控平台风格。

执行边界：

- 只改前端 UI、布局、样式、组件展示和菜单视觉。
- 不改后端业务逻辑。
- 不改协议逻辑。
- 不改 DataScope。
- 不访问真实 FSU。
- 不执行 SET。
- 不启 Scheduler。

## 修改前论证

当前 FE-IA-CLEANUP-P1-001 已完成普通菜单收敛，但视觉层仍有几个问题：

- 侧栏使用深色背景，与用户参考的浅色物联网平台不一致。
- FSU 管理仍是表格形态，设备资产感不足。
- 实时数据主表仍突出 `mappingConfidence/source` 等技术字段。
- 告警中心主表仍展示置信度字段，普通用户视角不够业务化。
- 旧 CSS 中存在 `//` 注释，构建阶段会产生 CSS warning。

因此本次采用前端样式和普通页面展示层的最小改造，不改路由权限和后端安全边界。

## 修改范围

修改文件：

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

新增文档：

- `docs/audit/FE-UI-STYLE-REFIT-P1-001-light-iot-platform-style.md`
- `docs/tasks/FE-UI-STYLE-REFIT-P1-001-follow-up-todo.md`
- `docs/memory/2026-06-01-FE-UI-STYLE-REFIT-P1-001-light-iot-platform-style.md`

## 关键实现

- 全局 token 调整为 `#2F80ED` 主色、浅蓝选中背景、浅灰内容背景和白色卡片。
- `BasicLayout` 改为白色 Sidebar、白色 Header，菜单选中为浅蓝底蓝字。
- `PageHeader`、`MetricCard`、`FsuOnlineBadge`、`StatusBadge` 统一轻量样式。
- 修复 `StatusBadge` 默认状态未显示计算后中文标签的问题。
- `FsuStatusView` 改为 FSU 卡片网格，展示在线、心跳、登录、设备数、告警数和更新时间。
- `RealtimeDataView` 主表保留 FSU、设备、点位名称、当前值、单位、类型、质量、状态、采集时间，弱化置信度和 source。
- `DashboardView` 改为卡片式驾驶舱，加入历史待回填、站点摘要、异常 FSU 摘要。
- `AlarmCenterView` 去掉置信度主列，协议字段继续留在详情抽屉。

## 菜单和权限结论

普通业务菜单仍保持收敛：

- 监控中心：监控驾驶舱、站点实时数据、告警中心
- 站点监控：站点列表、FSU 管理
- 三方授权：用户管理、角色管理、权限管理、站点授权、FSU 授权
- 系统设置：安全设置

未恢复：

- 资产与点位
- 设备管理
- 机柜管理
- 点位字典
- 点位映射
- 未映射点位
- 系统审计
- 协议诊断
- raw XML
- run-once

raw XML 和 run-once 仍只存在于隐藏内部路由，并保留原权限要求。FSU 管理卡片中的“通信记录”仅在 `protocol:raw:view` 权限可用时显示。

## 验证结果

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
- 仍有 `@vueuse/core` Rollup pure annotation warning 和既有 chunk size warning，非本次阻断问题。

未执行后端测试，原因是本次未修改后端、路由权限、route guard、DataScope 或安全拦截逻辑。

## 后续优先级

P1：

- `FE-UI-STYLE-P1-001`：FSU 详情页补设备、机柜、点位概览 Tab。
- `FE-AUTH-P1-001`：站点授权、FSU 授权接入真实后端 API。

P2：

- `FE-DASHBOARD-CHART-P2-001`：驾驶舱补充图表和趋势。
- `FE-RESPONSIVE-P2-001`：桌面/移动端截图复核。

## 最终结论

FE-UI-STYLE-REFIT-P1-001 完成，前端已调整为浅色物联网平台风格，菜单结构保持收敛，FSU 管理、实时数据、告警中心和驾驶舱完成统一视觉改造；未恢复点位治理、系统审计或协议诊断普通入口，安全边界未回退。
