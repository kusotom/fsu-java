# DATA-MAPPING-P0-002 后续 TODO

## P0

当前未保留开放 P0。已在本任务中修正 `/api/b-interface/unmapped-signals?fsuCode=...` 参数绕过 DataScope 的问题，并通过编译与映射/DataScope测试。

## P1

| 任务编号 | 问题描述 | 影响范围 | 建议修改文件 | 验收标准 | 涉及后端 | 涉及权限 | 涉及协议安全边界 |
|---|---|---|---|---|---|---|---|
| DATA-MAPPING-P1-001 | 新增 5 张字典/观测表依赖 Hibernate `ddl-auto=update`，缺显式 migration | 生产部署、回滚、审计 | `backend/src/main/resources/db/migration/**` 或项目后续 schema 目录 | 新增表、唯一键、索引均有幂等 DDL；dev/prod 环境不依赖自动建表 | 是 | 否 | 否 |
| DATA-MAPPING-P1-002 | DataScope 仍是内存过滤 | 三方数据隔离、分页、排序、性能 | mapping/realtime/alarm repositories 与 services | 候选映射、未映射、实时、告警均在 Repository/SQL 层按 fsuScope/stationScope 过滤 | 是 | 是 | 否 |
| DATA-MAPPING-P1-003 | `VERIFIED_BY_REAL_DATA` 仅预留，真实确认写回未闭环 | 点位落地可信度 | `EStoneIIMappingService`、`SendDataService`、`BInterface2016GetDataService`、`SendAlarmService` | 真实返回 DeviceID+SignalId 后可把候选从 `MAPPED_CANDIDATE/PENDING_REAL_DATA` 更新为 `VERIFIED_BY_REAL_DATA`，并保留审计 | 是 | 是 | 否 |
| DATA-MAPPING-P1-004 | 未映射 API 同时返回模板候选和真实未映射观测 | 映射页面语义 | `BInterfaceFrontendReadController`、新增 mapping controller/API | 提供 `/mapping/candidates` 与 `/mapping/unmapped-observations` 两类接口，前端页面不混淆 | 是 | 是 | 否 |
| DATA-MAPPING-P1-005 | 候选映射和未映射观测缺 `tenantId/stationId` | 三方授权和站点隔离 | mapping entities/repositories/services | 字段补齐并由 FSU/站点关系写入；DataScope 可按 tenant/station/fsu 三层过滤 | 是 | 是 | 否 |

## P2

| 任务编号 | 问题描述 | 影响范围 | 建议修改文件 | 验收标准 | 涉及后端 | 涉及权限 | 涉及协议安全边界 |
|---|---|---|---|---|---|---|---|
| DATA-MAPPING-P2-001 | 字典导入为首次访问 lazy import | 可观测性、启动一致性 | `EStoneIIDictionaryImportService`、启动 runner 或管理端点 | 启动时或管理员显式导入；导入结果可查询；重复导入幂等 | 是 | 是 | 否 |
| DATA-MAPPING-P2-002 | 前端 B接口实时页仍粗分蓄电池/环境分组 | 交互体验 | `BInterfaceRealtimeView.vue`、后端 realtime DTO | 设备分组由后端 `deviceName/deviceType` 驱动，不写死设备类型 | 是 | 否 | 否 |
| DATA-MAPPING-P2-003 | 点位映射缺人工确认、忽略、导出厂家清单流程 | 工程落地 | mapping API + `PointMappingView.vue` / `UnmappedPointsView.vue` | 支持确认/忽略/导出未映射清单，并记录操作审计 | 是 | 是 | 否 |
| DATA-MAPPING-P2-004 | Event 字典映射已返回，但告警详情页缺事件字典专项视图 | 告警排障 | `AlarmCenterView.vue`、告警详情组件 | 告警详情展示 EventId、EventName、SignalName、SignalMeanings、EventSeverity 和映射来源 | 否 | 否 | 否 |

## P3

| 任务编号 | 问题描述 | 影响范围 | 建议修改文件 | 验收标准 | 涉及后端 | 涉及权限 | 涉及协议安全边界 |
|---|---|---|---|---|---|---|---|
| DATA-MAPPING-P3-001 | 前端状态/置信度标签逻辑在多个页面重复 | 维护性 | `frontend/src/utils` 或公共组件 | 统一 `mappingStatusLabel/confidenceTagType` 工具，页面无重复定义 | 否 | 否 | 否 |
| DATA-MAPPING-P3-002 | 字典页面缺模板版本筛选和统计 | 体验 | 后续字典管理页 | 可按 `WITH_MIDPOINT/NO_MIDPOINT/BOTH`、置信度、是否派生筛选 | 可选 | 否 | 否 |
| DATA-MAPPING-P3-003 | 未映射列表最近发现指标暂未联动 | 体验 | `UnmappedPointsView.vue` | 指标卡展示最大 `lastSeenAt`，空值文案明确 | 否 | 否 | 否 |
