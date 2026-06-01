# FE-P1-REALTIME-001: 实时数据页业务化改版

**日期:** 2026-05-29
**状态:** 已完成

---

## 一、变更摘要

将 `RealtimeDataView.vue` 从简单数据表格改造为业务化实时数据页面。

## 二、修改文件

| 文件 | 变更 |
|------|------|
| `views/telemetry/RealtimeDataView.vue` | 重写 — 业务化 |

`BInterfaceRealtimeView.vue` 保留不变 — 作为协议诊断模块中的 B接口实时数据页面。

## 三、页面结构

```
PageHeader: "实时数据" + 描述
├── DataStateAlert (9 态)
├── FilterPanel: FSU / 点位类型 / 映射状态
├── MetricCard × 4: 实时点位 | 有值 | 未映射 | 异常
├── DataStateAlert (未映射 > 0 时)
├── 数据表格: FSU | 设备 | 点位名称 | 当前值 | 单位 | 类型 | 质量 | 采集时间
└── EmptyState (无数据时)
```

## 四、使用的组件
- MetricCard, DataStateAlert, FilterPanel, EmptyState, StatusBadge, PageHeader

## 五、状态口径 (9 态)
- `api_not_found` → 后端未接入
- `network_error` → 网络异常
- `unauthorized` → 未登录
- `forbidden` → 无权限
- `server_error` → 后端异常
- `empty` → 暂无数据
- `ack_empty` → FSU ACK 但空
- `parse_error` → 解析失败
- `normal` → 正常

## 六、验证

| 检查项 | 结果 |
|--------|------|
| npm run build | **通过** |
| 是否新增后端接口 | **否** |
| 是否访问真实 FSU | **否** |
| 是否新增 raw/run-once/SET | **否** |
| DI 0/1 硬编码 | **否** — DI 类型带 "?" 提示 |
| 0.0 显示 | **是** — formatValue 正常处理 |
| CSS token | **是** |
| demo 数据 | **否** |
| BInterfaceRealtimeView 保留 | **是** — 协议诊断模块中不变 |

## 七、结论

1. 实时数据页是否已业务化: **是**
2. 是否保留状态口径: **是** — 9 态完整
3. 是否新增后端接口: **否**
4. 是否触发真实 FSU: **否**
5. 是否新增 raw/run-once/SET 入口: **否**
6. 是否使用 demo 数据: **否**
7. 是否破坏权限: **否**
8. npm run build: **通过**
9. 遗留问题: BInterfaceRealtimeView 和 RealtimeDataView 最终应合并
10. 下一步建议: **FE-P1-ALARM-001** (告警中心改版)
