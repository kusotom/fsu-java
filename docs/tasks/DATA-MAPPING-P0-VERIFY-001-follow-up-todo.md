# DATA-MAPPING-P0-VERIFY-001 后续 TODO

## P0

当前未保留开放 P0。本次复验中发现的两个 P0 小风险已修正:

- `DATA-MAPPING-P0-VERIFY-001-FIX-001`: `/api/b-interface/fsus/{fsuCode}/devices` 增加 DataScope 校验，候选设备不再回退全量字典。
- `DATA-MAPPING-P0-VERIFY-001-FIX-002`: 告警显式 EventId 未命中时记录 `UNKNOWN_EVENT_ID` unmapped observation。

## P1

| 任务编号 | 问题描述 | 影响范围 | 建议修改文件 | 验收标准 | 涉及后端 | 涉及权限 | 涉及协议安全边界 |
|---|---|---|---|---|---|---|---|
| DATA-MAPPING-P1-006 | `unmapped_signal_observation` 以 `rawId` 承载未知 EventId，缺独立 `eventId` 字段 | 告警排障、厂家对照 | `UnmappedSignalObservationEntity`、repository、DTO、migration、前端未映射页 | 未知 EventId/SPID/SignalId 可分别查询和展示；历史 rawId 兼容 | 是 | 是 | 否 |
| DATA-MAPPING-P1-007 | BInterfaceFrontendReadController 聚合只读接口缺专门 Controller 测试覆盖 DataScope 参数路径 | 三方隔离回归 | 新增 `BInterfaceFrontendReadControllerDataScopeTest` | 覆盖 devices/login-info/unmapped/realtime/alarms 带参和不带参的授权/未授权场景 | 是 | 是 | 否 |
| DATA-MAPPING-P1-008 | 新增字典表仍依赖 Hibernate `ddl-auto=update` | 生产部署、回滚 | `backend/src/main/resources/db/migration/**` 或项目 schema 目录 | Signal/Event/Control/Candidate/Unmapped 表、索引、唯一键有显式 migration | 是 | 否 | 否 |
| DATA-MAPPING-P1-009 | DataScope 仍是内存过滤 | 分页、排序、性能、越权总数风险 | mapping/realtime/alarm repositories 与 services | 数据范围在 Repository/SQL 层过滤，不先查全量再过滤 | 是 | 是 | 否 |
| DATA-MAPPING-P1-010 | 候选映射和未映射观测缺 `tenantId/stationId` | 三方授权和站点隔离 | mapping entities/repositories/services | 字段补齐并由 FSU/站点关系写入；可按 tenant/station/fsu 三层过滤 | 是 | 是 | 否 |

## P2

| 任务编号 | 问题描述 | 影响范围 | 建议修改文件 | 验收标准 | 涉及后端 | 涉及权限 | 涉及协议安全边界 |
|---|---|---|---|---|---|---|---|
| DATA-MAPPING-P2-005 | `frontend/src/compat/realtimeSignalFilter.ts` 仍保留临时 FSU 常量 | 前端上下文、站点切换 | `BInterfaceRealtimeView.vue`、路由参数、站点/FSU选择器 | B接口实时页 FSU 来自路由或站点上下文，不依赖硬编码常量 | 否 | 是 | 否 |
| DATA-MAPPING-P2-006 | B接口实时页设备分组仍是静态蓄电池/环境布局 | 设备展示准确性 | `BInterfaceRealtimeView.vue`、后端 DTO | 设备组完全由后端 `deviceName/deviceType` 驱动 | 可选 | 否 | 否 |
| DATA-MAPPING-P2-007 | 未映射 API 同时返回模板候选和真实未映射观测 | 页面语义 | `BInterfaceFrontendReadController`、新增 mapping controller/API | 拆成 candidate list 和 observation list，前端分别展示 | 是 | 是 | 否 |
| DATA-MAPPING-P2-008 | `VERIFIED_BY_REAL_DATA` 写回流程未闭环 | 点位可信度 | `SendDataService`、`BInterface2016GetDataService`、`SendAlarmService` | 真实返回 DeviceID+SignalId 后可审计化更新 verified 状态 | 是 | 是 | 否 |

## P3

| 任务编号 | 问题描述 | 影响范围 | 建议修改文件 | 验收标准 | 涉及后端 | 涉及权限 | 涉及协议安全边界 |
|---|---|---|---|---|---|---|---|
| DATA-MAPPING-P3-004 | 映射状态和置信度标签逻辑在多个前端页面重复 | 维护性 | `frontend/src/utils`、公共组件 | 统一 `mappingStatusLabel/confidenceTagType` | 否 | 否 | 否 |
| DATA-MAPPING-P3-005 | 字典导入为 lazy import，可观测性不足 | 运维体验 | `EStoneIIDictionaryImportService`、管理端点 | 可查询导入状态、导入时间、资源版本 | 是 | 是 | 否 |
| DATA-MAPPING-P3-006 | 前端 build 存在既有 CSS `//` 注释 warning 和 chunk size warning | 构建洁净度 | `frontend/src/styles/**`、Vite chunk 配置 | build warning 清零或形成明确忽略清单 | 否 | 否 | 否 |
