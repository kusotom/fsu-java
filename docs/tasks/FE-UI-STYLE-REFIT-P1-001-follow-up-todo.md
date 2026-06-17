# FE-UI-STYLE-REFIT-P1-001 后续 TODO

## P0

无新增 P0。

## P1

### FE-UI-STYLE-P1-001

- 问题描述：FSU 详情页仍以现有结构为主，设备列表、机柜信息、点位概览尚未完整归并为详情 Tab。
- 影响范围：FSU 管理、站点监控、资产信息查看。
- 建议修改文件：`frontend/src/views/binterface/FsuStatusDetailView.vue`、设备/机柜相关页面。
- 验收标准：FSU 详情页可在不暴露独立普通菜单的前提下查看设备列表、机柜信息、点位概览和通信摘要。
- 是否涉及后端：否，优先复用现有接口。
- 是否涉及权限：是，通信和协议信息仍需 elevated/raw 权限。
- 是否涉及协议安全边界：是，不能新增 run-once 或 SET。

### FE-AUTH-P1-001

- 问题描述：站点授权、FSU 授权页面当前为轻量占位或静态展示，尚未接入真实授权 API。
- 影响范围：三方授权、租户站点隔离、FSU 数据隔离。
- 建议修改文件：`frontend/src/views/system/SiteAuthorizationView.vue`、`frontend/src/views/system/FsuAuthorizationView.vue`、授权 API 封装。
- 验收标准：平台管理角色可配置站点/FSU 授权，普通监控用户和三方用户不可见三方授权菜单。
- 是否涉及后端：是。
- 是否涉及权限：是。
- 是否涉及协议安全边界：否。

## P2

### FE-DASHBOARD-CHART-P2-001

- 问题描述：驾驶舱已完成卡片化，但图表和趋势能力仍偏弱。
- 影响范围：监控驾驶舱、运营观察。
- 建议修改文件：`frontend/src/views/dashboard/DashboardView.vue`、`frontend/src/components/charts/BaseChart.vue`。
- 验收标准：补充 FSU 状态分布、告警等级分布、数据质量趋势图，且页面在无数据时保持空态清晰。
- 是否涉及后端：可能涉及聚合 API。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

### FE-RESPONSIVE-P2-001

- 问题描述：当前已补基础响应式网格，但移动端细节仍需截图验证。
- 影响范围：驾驶舱、FSU 管理、实时数据、告警中心。
- 建议修改文件：全局样式和核心业务页面。
- 验收标准：桌面和移动端页面无文字溢出、卡片重叠、按钮挤压。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

## P3

### FE-THEME-P3-001

- 问题描述：可选增加浅色主题配置项和用户偏好记忆。
- 影响范围：用户体验。
- 建议修改文件：`frontend/src/stores/app.ts`、全局样式。
- 验收标准：用户可切换视觉密度或主题偏好，刷新后保留。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。
