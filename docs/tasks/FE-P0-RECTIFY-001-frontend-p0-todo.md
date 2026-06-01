# FE-P0-RECTIFY-001: 前端 P0 后续任务

**日期:** 2026-05-29

## 待完成

### 1. 删除 compat 层 (等后端字段)
- 文件: `frontend/src/compat/realtimeSignalFilter.ts`
- 条件: 后端 `/api/b-interface/realtime-points` 返回 `mappingStatus`、`dataSource`、`isDemo` 字段
- 操作: 删除 compat 文件，`BInterfaceRealtimeView.vue` 改为后端字段过滤

### 2. 完善 run-once UI
- 当前均为占位，待后端 REST run-once controller 实现后
- `FsuStatusDetailView.vue` GET_FSUINFO run-once
- `BInterfaceSchedulerView.vue` run-once 卡片
- `BInterfaceThresholdView.vue` GET_THRESHOLD run-once

### 3. 动态权限同步
- 当前权限点 source 均为 `frontend-planning`
- 待后端权限 API 就绪后，改为从后端拉取并同步

### 4. CallRecordView raw 内容
- `BInterfaceCallRecord` 类型含 `requestBody`/`responseBody`
- 当前 CallRecordView 只展示元数据，未展示 raw 内容
- 如后端返回完整报文体，需同步加 raw XML 权限保护

### 5. DI 0/1 语义
- 当前未硬编码 DI 0/1
- 后续如有 DI 点位展示需求，需在协议层统一转换，不允许前端硬编码
