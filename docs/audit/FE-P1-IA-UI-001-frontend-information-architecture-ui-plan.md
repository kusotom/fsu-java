# FE-P1-IA-UI-001: 前端信息架构重组与 UI 视觉优化方案

**日期:** 2026-05-29
**类型:** P1 前端规划
**状态:** 纯规划，不直接改代码

---

## 一、现有问题诊断

### 1.1 为什么当前架构不合理

| 问题 | 现状 | 影响 |
|------|------|------|
| **协议调试与业务混排** | B接口管理子菜单包含 6 项，与资源管理、数据监控平级 | 三方用户看到 SOAP/XML、命令矩阵、调度器等协议调试入口 |
| **B接口总览是首页** | `/b-interface` 是协议健康检查，不是监控驾驶舱 | 登录后看到"SCService: ACTIVE / SOAP: 已启用"，不像动环平台 |
| **菜单层级混乱** | "资源管理"下有站点/机柜/设备/点位，"B接口管理"下有FSU状态/报文/命令/调用/FTP | FSU 和站点分散在不同菜单，三方授权无处安放 |
| **缺少三方授权模块** | 用户/角色/权限散落在系统设置，无租户/scope 概念 | 无法授权三方用户访问特定站点和 FSU |
| **缺少审计可见性** | 操作审计无菜单入口 | 安全闭环不可见 |
| **工程调试风格 UI** | PageHeader + el-card + el-table，全局默认配色 | 像开发工具，不像生产监控平台 |
| **空状态覆盖不完整** | 大量页面显示"接口未接入"或硬编码占位数据 | 三方用户无法区分"无数据"与"无权限" |

### 1.2 现有页面审计 (28 routes, 30 views)

| 当前路由 | 当前模块 | 保留 | 移动/合并 | 说明 |
|---------|---------|------|----------|------|
| `/dashboard` | 总览 | 保留 | → 监控中心/首页 | UI 重做 |
| `/sites` | 资源管理 | 保留 | → 监控中心/站点监控 | 增强 |
| `/cabinets` | 资源管理 | 合并 | → 资产/设备管理 | 机柜纳入设备 |
| `/devices` | 资源管理 | 保留 | → 资产/FSU管理 | 增强 |
| `/points` | 资源管理 | 保留 | → 资产/点位字典 | 增强 |
| `/telemetry/realtime` | 数据监控 | 保留 | → 监控中心/实时数据 | 增强 |
| `/telemetry/history` | 数据监控 | 保留 | → 监控中心/历史数据 | UI 优化 |
| `/alarms` | 告警管理 | 保留 | → 监控中心/告警中心 | 增强 |
| `/b-interface` | B接口管理 | **下沉** | → 协议诊断/协议概览 | 三方用户隐藏 |
| `/b-interface/fsus` | B接口管理 | 移动 | → 资产/FSU管理 | 改名"FSU注册状态" |
| `/b-interface/fsus/:fsuCode` | B接口管理 | 移动 | → 资产/FSU详情 | 增强 |
| `/b-interface/realtime` | B接口管理 | 移动 | → 监控中心/实时数据 | 合并 BInterfaceRealtime |
| `/b-interface/alarms` | B接口管理 | 移动 | → 监控中心/告警中心 | 合并 BInterfaceAlarm |
| `/b-interface/thresholds` | B接口管理 | **下沉** | → 协议诊断/门限只读 | 三方隐藏 |
| `/b-interface/ftp-login-info` | B接口管理 | **下沉** | → 协议诊断/FTP参数 | 三方隐藏 |
| `/b-interface/ftp-images` | B接口管理 | **下沉** | → 协议诊断/FTP图片 | 三方隐藏 |
| `/b-interface/schedulers` | B接口管理 | **下沉** | → 协议诊断/调度器 | 三方隐藏 |
| `/b-interface/protocol-audit` | B接口管理 | **下沉** | → 协议诊断/协议治理 | 三方隐藏 |
| `/b-interface/logs` | B接口管理 | **下沉** | → 协议诊断/raw XML | 三方隐藏 |
| `/b-interface/commands` | B接口管理 | **下沉** | → 协议诊断/命令矩阵 | 三方隐藏 |
| `/b-interface/calls` | B接口管理 | **下沉** | → 协议诊断/通信记录 | 三方隐藏 |
| `/b-interface/ftp` | B接口管理 | **下沉** | → 协议诊断/FTP记录 | 三方隐藏 |
| `/system/users` | 系统管理 | 保留 | → 三方授权/用户管理 | 增强 |
| `/system/roles` | 系统管理 | 保留 | → 三方授权/角色管理 | 路由已有 |
| `/system/permissions` | 系统管理 | 保留 | → 三方授权/权限点 | 路由已有 |
| `/profile/security` | 安全 | 保留 | → 个人设置 | 不变 |
| (新增) | — | 新增 | → 三方授权/三方单位 | TENANT-001 |
| (新增) | — | 新增 | → 三方授权/站点授权 | SCOPE-001 |
| (新增) | — | 新增 | → 三方授权/FSU授权 | SCOPE-001 |
| (新增) | — | 新增 | → 系统审计/操作日志 | AUDIT-001 |

---

## 二、新的信息架构

### 2.1 一级菜单 6 项

```
🏠 监控中心          ← 业务人员日常入口
📦 资产与点位         ← 设备/点位管理
🔐 三方授权           ← 仅 SUPER_ADMIN / PLATFORM_ADMIN
🔧 协议诊断           ← 仅协议调试角色，默认收起
📋 系统审计           ← 仅 SUPER_ADMIN / PLATFORM_ADMIN
⚙ 系统设置           ← 个人/系统配置
```

### 2.2 完整菜单树

```
1. 监控中心
   ├── 首页总览           /dashboard
   ├── 站点监控           /sites
   ├── 站点详情           /sites/:id
   ├── 实时数据           /realtime
   ├── 历史数据           /history
   └── 告警中心           /alarms

2. 资产与点位
   ├── FSU 管理           /fsus           (原 b-interface/fsus)
   ├── FSU 详情           /fsus/:fsuCode
   ├── 设备管理           /devices         (合并 cabinets)
   └── 点位管理           /points          (合并 monitoring-points)
       ├── 点位字典       /points/dict
       ├── 点位映射       /points/mapping
       └── 未映射点位     /points/unmapped

3. 三方授权             ← requires SUPER_ADMIN or PLATFORM_ADMIN
   ├── 三方单位           /tenants         (新)
   ├── 用户管理           /users           (原 system/users)
   ├── 角色管理           /roles           (原 system/roles)
   ├── 权限点             /permissions     (原 system/permissions)
   ├── 站点授权           /scope/stations  (新)
   └── FSU 授权           /scope/fsus      (新)

4. 协议诊断             ← requires elevatedUser, 默认折叠
   ├── 协议概览           /protocol/overview    (原 b-interface)
   ├── 协议矩阵           /protocol/matrix      (原 b-interface/commands)
   ├── raw XML            /protocol/raw         (原 b-interface/logs)
   ├── 只读 run-once      /protocol/runonce     (原 schedulers 整合)
   ├── 通信记录           /protocol/calls       (原 b-interface/calls)
   ├── FTP 记录           /protocol/ftp         (原 b-interface/ftp)
   ├── 门限只读           /protocol/thresholds
   ├── FTP 参数           /protocol/ftp-config
   └── 错误码             /protocol/error-codes

5. 系统审计             ← requires SUPER_ADMIN or PLATFORM_ADMIN
   ├── 操作日志           /audit/logs
   ├── 权限拒绝           /audit/denied
   ├── raw 访问           /audit/raw-access
   ├── run-once 记录      /audit/runonce
   └── SET 拦截           /audit/set-blocked

6. 系统设置
   ├── 字典配置           /settings/dict
   └── 系统参数           /settings/params
```

### 2.3 旧路由 → 新路由迁移表

| 旧路由 | 新路由 | 操作 |
|--------|--------|------|
| `/dashboard` | `/dashboard` | 保留，UI重做 |
| `/sites` | `/sites` | 保留 |
| `/cabinets` | (删除) | 合并到 devices |
| `/devices` | `/devices` | 保留 |
| `/points` | `/points/dict` | 重定向 |
| `/telemetry/realtime` | `/realtime` | 移动 |
| `/telemetry/history` | `/history` | 移动 |
| `/alarms` | `/alarms` | 保留 |
| `/b-interface` | `/protocol/overview` | 移动 |
| `/b-interface/fsus` | `/fsus` | 移动 |
| `/b-interface/fsus/:fsuCode` | `/fsus/:fsuCode` | 移动 |
| `/b-interface/realtime` | (合并) | 合并到 /realtime |
| `/b-interface/alarms` | (合并) | 合并到 /alarms |
| `/b-interface/thresholds` | `/protocol/thresholds` | 下沉 |
| `/b-interface/ftp-login-info` | `/protocol/ftp-config` | 下沉 |
| `/b-interface/ftp-images` | `/protocol/ftp-images` | 下沉 |
| `/b-interface/schedulers` | `/protocol/runonce` | 下沉整合 |
| `/b-interface/protocol-audit` | `/protocol/matrix` | 下沉 |
| `/b-interface/logs` | `/protocol/raw` | 下沉 |
| `/b-interface/commands` | `/protocol/matrix` | 下沉 |
| `/b-interface/calls` | `/protocol/calls` | 下沉 |
| `/b-interface/ftp` | `/protocol/ftp` | 下沉 |
| `/system/users` | `/users` | 移动 |
| `/system/roles` | `/roles` | 移动 |
| `/system/permissions` | `/permissions` | 移动 |

---

## 三、角色菜单矩阵

| 一级菜单 | SUPER_ADMIN | PLATFORM_ADMIN | PROTOCOL_DEBUGGER | THIRD_PARTY_ADMIN | THIRD_PARTY_OPS | READONLY_VIEWER |
|---------|-------------|---------------|-------------------|-------------------|-----------------|-----------------|
| 监控中心 | ● | ● | ● | ● | ● | ● |
| 资产与点位 | ● | ● | ● | ● | ● | ◐ FSU只读 |
| 三方授权 | ● | ● | ○ | ○ | ○ | ○ |
| 协议诊断 | ● | ● | ● | ○ | ○ | ○ |
| 系统审计 | ● | ● | ○ | ○ | ○ | ○ |
| 系统设置 | ● | ● | ○ | ○ | ○ | ○ |

● 全部可见  ◐ 部分可见  ○ 不可见

---

## 四、UI 视觉规范

### 4.1 主色
- **主色**: `#1A5FDC` (深蓝，稳重可靠) — 替代当前 Element Plus 默认 `#409EFF`
- **辅助色**: `#0EA882` (翠绿，在线/正常)
- **背景色**: `#F0F2F5` (浅灰) → 主内容区 / `#FFFFFF` → 卡片
- **侧边栏**: `#1B2A47` (深蓝黑) — 替代当前 `#304156`

### 4.2 状态色
| 状态 | 颜色 | 用途 |
|------|------|------|
| 成功/在线 | `#0EA882` | 在线、正常、已确认 |
| 警告 | `#E6A23C` | 离线、未映射、待确认 |
| 危险/告警 | `#F56C6C` | 严重告警、FSU异常、拒绝 |
| 信息 | `#909399` | 未知、待确认、空状态 |

### 4.3 告警等级颜色 (B接口标准)
| 等级 | 颜色 | 标签 |
|------|------|------|
| 一级 (紧急) | `#FF0000` | 红色闪烁 |
| 二级 (重要) | `#FF6600` | 橙色 |
| 三级 (一般) | `#FFCC00` | 黄色 |
| 四级 (提示) | `#3399FF` | 蓝色 |

### 4.4 FSU 在线/离线/异常
- **在线**: 绿色圆点 + "在线" + 最后心跳时间
- **离线**: 灰色圆点 + "离线" + 最后心跳时间 + 心跳丢失次数
- **异常**: 红色圆点 + "异常" + 会话过期/心跳超时

### 4.5 数据状态提示
| 状态 | 样式 |
|------|------|
| 无数据 | 灰色 EmptyState |
| 无权限 | 橙色 403 提示 |
| 加载中 | 骨架屏 |
| 加载失败 | 红色 ErrorState + 重试按钮 |
| 部分数据 | 黄色 Alert + "部分数据不可用" |

### 4.6 卡片 (MetricCard)
- 白色背景、圆角 8px、阴影 0 2px 8px rgba(0,0,0,0.06)
- 指标值: 32px bold, 主色
- 指标标签: 14px, 灰色
- 趋势箭头: 绿色↑ / 红色↓

### 4.7 表格
- 斑马纹, hover 高亮
- 表头: 加粗, #606266
- 操作列右对齐, 图标按钮优先

### 4.8 筛选区 (FilterPanel)
- 卡片包裹, 单行排列
- el-input/el-select 紧凑模式 (size="small")
- "查询" + "重置" 按钮

### 4.9 详情抽屉 (DetailDrawer)
- 右侧滑出, 宽度 600px
- 上部: 标题 + 元数据
- 中部: Tabs (基础信息/设备列表/告警/审计)
- 底部: 操作按钮

### 4.10 协议诊断视觉区分
- 协议诊断所有页面加顶部黄色提示条: "协议诊断模式 — 仅限授权人员使用"
- 代码块/XML 深色背景 (#1E1E1E) + 等宽字体
- 与业务页面明确的视觉隔离

---

## 五、首页总览 UI 规划

### 页面定位
监控驾驶舱，B接口动环平台运行态势一屏总览。

### UI 区块 (从上到下)
```
┌────────────────────────────────────────────┐
│ PageHeader: "动环监控平台" + 最后刷新时间    │
├────────────────────────────────────────────┤
│ [站点总数 6]  [FSU在线 4/6]  [当前告警 3]   │  ← MetricCard 行
│              [严重告警 1]  [数据更新 2m前]   │
├────────────────────────────────────────────┤
│ FSU 在线率趋势 (ECharts 折线)               │  ← 72h FSU 在线率
├────────────────────┬───────────────────────┤
│ 最新告警列表 (5条) │ 最新通信事件 (5条)      │  ← 双列表
│ - 温度过高 WARN    │ - LOGIN FSU-001 OK    │
│ - 门限超限 CRITICAL│ - HEARTBEAT FSU-002 OK│
├────────────────────┴───────────────────────┤
│ 待处理问题 (0)                              │
└────────────────────────────────────────────┘
```

### 权限: `dashboard:view`
### 数据: sites API + fsus API + alarms API + message-logs API
### 刷新: 手动 + 自动轮询(可选)

---

## 六、复用组件规划

| 组件 | 用途 | Props | 已有/新增 |
|------|------|-------|----------|
| StatusBadge | 通用状态徽章 | status, label, size | **已有** |
| MetricCard | 监控指标卡片 | label, value, trend, color | 新增 |
| AlarmLevelTag | 告警等级标签 | level (1-4) | 新增 |
| FsuOnlineBadge | FSU 在线状态 | onlineStatus, lastHeartbeat | 新增 |
| DataStateAlert | 数据状态提示 | state (empty/error/partial) | **已有** ApiErrorAlert 增强 |
| PermissionButton | 权限控制按钮 | permissions, roles | **已有** PermissionGuard 增强 |
| ScopeSelector | 站点/FSU 范围选择 | type (station/fsu), modelValue | 新增 |
| PageHeader | 页面标题 | title, description | **已有** |
| FilterPanel | 筛选面板 | filters, onSearch, onReset | 新增 |
| DetailDrawer | 详情抽屉 | visible, title, width | 新增 |
| EmptyState | 空状态 | type, title, description | **已有** |
| ErrorState | 错误状态 | error, onRetry | 新增 |
| RawXmlViewer | XML 查看器 | xml, readonly | 新增 |
| AuditEventTag | 审计事件标签 | action (allowed/denied) | 新增 |

---

## 七、开发任务拆分 (10 tasks)

| 序号 | 任务 | 说明 | 优先级 |
|------|------|------|--------|
| FE-P1-IA-002 | 菜单与路由重组 | 6 模块菜单 + 路由迁移 + 重定向 | P1A |
| FE-P1-UI-001 | 基础 UI 组件 | 15 组件 + 视觉规范落地 | P1A |
| FE-P1-DASHBOARD-001 | 首页总览改版 | 监控驾驶舱 | P1A |
| FE-P1-SITE-001 | 站点监控页改版 | 站点列表+详情 | P1A |
| FE-P1-REALTIME-001 | 实时数据页改版 | 合并两处实时数据 | P1B |
| FE-P1-ALARM-001 | 告警中心改版 | 告警确认/清除闭环 | P1B |
| FE-P1-MAPPING-001 | 点位映射 | 映射管理+未映射点位 | P1B |
| FE-P1-TENANT-001 | 三方授权页面 | 租户+用户+角色+scope | P1B |
| FE-P1-AUDIT-001 | 系统审计页面 | 操作日志+拒绝+raw+SET | P1B |
| FE-P1-PROTOCOL-001 | 协议诊断下沉 | raw XML/run-once 等 9 页下沉 | P1B |

---

## 八、结论

1. **当前不合理**: 协议调试与业务监控混排, 菜单层级混乱, 缺少三方授权模块
2. **新架构**: 监控中心 | 资产点位 | 三方授权 | 协议诊断 | 系统审计 | 系统设置 — 6 模块清晰分离
3. **协议诊断下沉**: 9 个 B接口子页面移到"协议诊断"模块, 默认折叠, 仅 elevated user 可见
4. **三方授权独立**: 用户/角色/权限点 + 租户 + scope 授权形成独立模块
5. **系统审计独立**: 操作日志 + 权限拒绝 + raw 访问 + run-once + SET 拦截
6. **UI 升级**: 从工程调试风格 (#409EFF 默认主题 + el-card 堆叠) 升级为监控平台风格 (深蓝主色 + MetricCard + ECharts + 骨架屏)
7. **优先做**: IA-002 (菜单路由) → UI-001 (组件) → DASHBOARD-001 (首页) → SITE-001 (站点)
8. **暂缓**: AUDIT-001 (依赖后端审计 API), TENANT-001 (依赖租户 API)
9. **先做组件**: MetricCard, FilterPanel, DetailDrawer, RawXmlViewer, ErrorState
10. **可以等 VERIFY-003 通过后进入实现**: 是 — 本方案为规划文档, 不涉及代码修改
