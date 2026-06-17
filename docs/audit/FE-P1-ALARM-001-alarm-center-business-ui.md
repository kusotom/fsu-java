# FE-P1-ALARM-001: 告警中心业务化改版

**日期:** 2026-05-29
**状态:** 已完成

---

## 一、变更摘要

将 `AlarmCenterView.vue` 从简单数据表格改为完整告警中心。

## 二、修改文件

| 文件 | 变更 |
|------|------|
| `views/alarm/AlarmCenterView.vue` | 重写 — 业务化告警中心 |
| `components/common/DetailDrawer.vue` | 修复 v-model → model-value |

## 三、页面结构

```
PageHeader + DataStateAlert (7态)
├── Tabs: 当前告警 | 历史告警
├── MetricCard × 4: 当前告警 | 严重 | 主要 | 次要
├── FilterPanel: 告警等级 · 告警状态 · 关键字
├── 告警表格: FSU | 告警名称 | 等级 | 状态 | 告警值 | 发生时间 | 恢复时间 | 操作
├── EmptyState (无告警 / 筛选无结果)
└── DetailDrawer:
    ├── 业务信息 (8 字段)
    ├── 协议字段 (SEND_ALARM, 8 字段)
    └── 系统信息 (3 字段)
```

## 四、使用的组件
`MetricCard`×4, `DataStateAlert`, `FilterPanel`, `EmptyState`, `DetailDrawer`, `AlarmLevelTag`, `StatusBadge`, `PageHeader`

## 五、等级映射
CRITICAL→一级(红), MAJOR→二级(红橙), MINOR→三级(橙), WARN→四级(黄), INFO→提示(蓝)

## 六、验证

| 检查项 | 结果 |
|--------|------|
| npm run build | **通过** |
| 原始告警数据保留 | **是** — 全部协议字段在详情抽屉 |
| 协议字段下沉 | **是** — 独立"协议字段"分组 |
| raw XML / run-once / SET | **否** — 无入口 |
| demo 数据 | **否** |
| 新增后端接口 | **否** |
| 访问真实 FSU | **否** |

## 七、结论

1. 告警中心是否已业务化: **是**
2. 当前告警/历史告警区分: **是** — Tabs 切换
3. 新增后端接口: **否**
4. 访问真实 FSU: **否**
5. raw/run-once/SET: **否**
6. demo 数据: **否**
7. 破坏权限: **否**
8. npm run build: **通过**
9. 后续: **FE-P1-FSU-001** (FSU 列表与详情)
