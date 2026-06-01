# FE-P1-UI-001: 前端视觉规范与基础组件落地

**日期:** 2026-05-29
**类型:** P1 前端基础设施
**状态:** 已完成 (token + 组件落地，未大规模改页面)
**前置:** AUTH-MATRIX-VERIFY-003 (已通过)

---

## 一、执行摘要

**FE-P1-UI-001 完成。本次落地了视觉 token 系统和 12 个基础组件，未大规模修改业务页面。**

---

## 二、新增文件清单

### CSS Token (4 files)
| 文件 | 说明 |
|------|------|
| `src/styles/tokens.css` | 设计 token: 主色、状态色、告警色、文字、边框、圆角、阴影 |
| `src/styles/theme.css` | Element Plus 主题覆盖 + 协议诊断页面视觉隔离 |
| `src/styles/status.css` | 状态色系统: FSU 在线/离线/告警点、告警等级标签 |
| `src/styles/layout.css` | 布局: 页面模板、指标行、筛选区、表格、抽屉、raw XML 代码区 |

### 组件 (12 files)
| 组件 | 路径 | 类型 |
|------|------|------|
| MetricCard | `components/common/MetricCard.vue` | 新增 |
| DataStateAlert | `components/common/DataStateAlert.vue` | 新增 |
| FsuOnlineBadge | `components/common/FsuOnlineBadge.vue` | 新增 |
| AlarmLevelTag | `components/common/AlarmLevelTag.vue` | 新增 |
| FilterPanel | `components/common/FilterPanel.vue` | 新增 |
| ErrorState | `components/common/ErrorState.vue` | 新增 |
| DetailDrawer | `components/common/DetailDrawer.vue` | 新增 |
| RawXmlViewer | `components/common/RawXmlViewer.vue` | 新增 |
| AuditEventTag | `components/common/AuditEventTag.vue` | 新增 |
| ProtocolStatusCard | `components/common/ProtocolStatusCard.vue` | 新增 |
| PermissionButton | `components/auth/PermissionButton.vue` | 新增 |
| — | (StatusBadge, PageHeader, EmptyState 已有) | 已有 |

### 修改文件 (1)
| 文件 | 变更 |
|------|------|
| `src/main.ts` | +4 行 CSS import |

---

## 三、视觉规范

### 主色
- `--app-primary: #1A5FDC` (深蓝) — 替代 Element Plus 默认 `#409EFF`
- `--app-sidebar-bg: #1B2A47` (深蓝黑) — 替代 `#304156`

### 状态色
| 状态 | 色值 | 用途 |
|------|------|------|
| online | `#16A34A` | FSU在线、正常 |
| offline | `#F59E0B` | FSU离线 |
| alarm | `#DC2626` | 告警、严重 |
| warning | `#F97316` | 警告 |
| unknown | `#64748B` | 未知 |
| protocol | `#EAB308` | 协议诊断标识 |

### 告警等级
| 等级 | 色值 |
|------|------|
| CRITICAL | `#B91C1C` |
| MAJOR | `#DC2626` |
| MINOR | `#F97316` |
| WARN | `#FACC15` |
| INFO | `#2563EB` |

---

## 四、组件规格摘要

### MetricCard
- Props: title, value, unit, subtitle, trend, status, loading
- 用途: 首页驾驶舱指标卡、站点/FSU详情统计
- 状态: 已实现

### DataStateAlert
- Props: state (9种), meta (10字段)
- 用途: 统一实时数据状态口径，禁止页面各自判断
- 状态: 已实现
- 口径: api_not_found → 后端未接入, network_error → 不可达, empty → 暂无数据, ack_empty → FSU已ACK但空, parse_error → 解析失败, unmapped → 存在未映射

### PermissionButton
- Props: permissions, roles, mode (hide/disable), requireAll
- 语义: permissions=all-of, roles=any-of
- 用途: 统一按钮权限控制，替代散写的权限判断
- 状态: 已实现

### RawXmlViewer
- 深色代码区 (#1E1E1E) + 等宽字体 + 格式化/复制/下载
- 下载按钮由 PermissionButton 控制 (require view+download)
- 顶部黄色"协议诊断模式"提示条
- 状态: 已实现

### FsuOnlineBadge / AlarmLevelTag / AuditEventTag / ErrorState
- 统一状态展示组件，已实现

---

## 五、验证结果

| 检查项 | 结果 |
|--------|------|
| npm run build | **通过** (7.32s) |
| routeGuard 未破坏 | [x] — 未修改 |
| PermissionGuard 未破坏 | [x] — 未修改 |
| permissions 语义未改变 | [x] |
| raw XML / run-once 权限未放开 | [x] |
| 0.0 数值显示不受影响 | [x] — PointCard 未修改 |
| 协议诊断组件视觉隔离 | [x] — protocol-banner + protocol-page class |

---

## 六、结论

1. 本次是否改业务页面: **否** — 仅新增组件，未替换任何页面
2. 本次是否新增 token / 组件: **是** — 4 CSS + 12 组件
3. 本次是否影响权限路由: **否**
4. 本次是否影响 raw XML / run-once: **否** — RawXmlViewer 的下载按钮受 PermissionButton 控制
5. 本次是否影响 SET 安全边界: **否**
6. npm run build 结果: **通过**
7. 是否可以进入 FE-P1-LAYOUT-001: **是** — 组件基础已就绪
8. 是否仍需等待 AUTH-MATRIX-VERIFY-003: **否** — 已通过
9. 下一步建议: FE-P1-LAYOUT-001 (菜单路由重组) → FE-P1-DASHBOARD-001 (首页改版)
