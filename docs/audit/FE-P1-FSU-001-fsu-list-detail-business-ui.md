# FE-P1-FSU-001: FSU 列表与详情页业务化改版

**日期:** 2026-05-29
**状态:** 已完成

---

## 一、修改文件

| 文件 | 变更 |
|------|------|
| `views/binterface/FsuStatusView.vue` | 重写 — 业务化FSU列表 |
| `views/binterface/FsuStatusDetailView.vue` | 重写 — 业务化FSU详情+Tabs |

## 二、FSU 列表页

```
PageHeader + DataStateAlert (7态)
├── MetricCard ×4: FSU总数 | 在线 | 离线 | 告警
├── FilterPanel: 在线状态 · FSU编码
├── 表格: FSU编码 | 在线状态(FsuOnlineBadge) | 登录 | 登录时间 | 心跳 | 丢失 | 详情
└── EmptyState
```

## 三、FSU 详情页

```
PageHeader + DataStateAlert
├── MetricCard ×3: 在线 | 心跳丢失 | 设备数
├── Tabs: 基础信息 | 运行状态 | 设备列表 | 通信记录
├── 基础信息: FSU编码/状态/登录/心跳/会话 (el-descriptions)
├── 运行状态: CPU/MEM 仪表盘 + EmptyState
├── 设备列表: DeviceID/来源/DeviceCode/映射状态
├── 通信记录: LOGIN/HEARTBEAT 时间线
└── GET_FSUINFO run-once (PermissionGuard保护, 协议诊断模式)
```

## 四、使用组件
`MetricCard`×7, `DataStateAlert`, `FilterPanel`, `EmptyState`, `FsuOnlineBadge`, `StatusBadge`, `PermissionGuard`, `PageHeader`

## 五、验证

| 检查项 | 结果 |
|--------|------|
| npm run build | **通过** |
| raw/run-once/SET | **仅** GET_FSUINFO run-once (PermissionGuard保护) |
| 新增后端接口 | **否** |
| 访问真实 FSU | **否** |
| demo 数据 | **否** |
| 权限破坏 | **否** |

## 六、结论

1. FSU页面已业务化: **是** — 列表+详情Tabs
2. 新增后端接口: **否**
3. 访问真实FSU/GET_DATA: **否**
4. raw/run-once/SET入口: 仅保护内的 GET_FSUINFO run-once
5. demo数据: **否**
6. 权限: 未破坏
7. npm run build: **通过**
8. 下一步: **FE-P1-MAPPING-001** (点位映射)
