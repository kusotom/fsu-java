# FE-P1-DASHBOARD-001: 首页监控驾驶舱改版

**日期:** 2026-05-29
**状态:** 已完成

---

## 一、变更摘要

将首页从"工程调试总览"改为"动力环境监控驾驶舱"。

## 二、修改文件 (1)

| 文件 | 变更 |
|------|------|
| `views/dashboard/DashboardView.vue` | 完全重写 |

## 三、使用的基础组件

| 组件 | 用途 |
|------|------|
| MetricCard | 站点总数 / FSU在线 / 当前告警 / 严重告警 |
| DataStateAlert | 连接状态 / 数据空状态 |
| FsuOnlineBadge | FSU 在线/离线状态分布 |
| AlarmLevelTag | 告警等级标签 |
| EmptyState | 无数据状态 |

## 四、页面结构

```
PageHeader: "监控驾驶舱"
├── MetricCard × 4: 站点 | FSU在线 | 告警 | 严重告警
├── FSU 状态分布 (在线/离线)
│   └── 告警分布 (CRITICAL/MAJOR/MINOR/WARN)
├── 最新告警列表 (5条)
│   └── 通信状态 (报文/调用/实时数据状态)
└── DataStateAlert (数据空状态提示)
```

## 五、验证

| 检查项 | 结果 |
|--------|------|
| npm run build | **通过** |
| 是否改动权限逻辑 | **否** |
| 是否改动后端接口 | **否** |
| 是否出现 demo 数据 | **否** — 使用真实 API, 无数据展示 EmptyState |
| 是否硬编码颜色 | **否** — 全部使用 CSS token |
| 是否引入 raw XML / run-once | **否** |
| 是否使用新组件 | **是** — MetricCard × 4, DataStateAlert, FsuOnlineBadge, AlarmLevelTag, EmptyState |

## 六、结论

首页从工程调试风格升级为监控驾驶舱。不破坏权限系统，不新增后端接口，不引入 demo 数据。
