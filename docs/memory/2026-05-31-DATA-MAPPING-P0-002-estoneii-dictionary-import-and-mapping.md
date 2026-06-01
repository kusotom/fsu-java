# DATA-MAPPING-P0-002 eStoneII-IO 标准码表导入与映射闭环

## 本次目标

实现 eStoneII-IO 标准码表导入，并建立实时数据与告警数据的字典映射最小闭环。要求前端消费后端映射结果，不硬编码 SignalName/EventName/单位/0-1 含义；Controls 只能作为禁用参考；不访问真实 FSU，不执行 SET，不启 Scheduler。

## 结论摘要

eStoneII-IO 标准码表已导入，实时数据与告警已具备基于 DeviceID + SignalId/SPID 的候选映射能力；未真实返回的模板点位仍按 `TEMPLATE_ONLY` 或 `PENDING_REAL_DATA` 管理。

不得声明现场 FSU 所有实时点位已完全确认，也不得声明 D 类全部点位已真实返回。Controls 不可用。

## 关键实现

- 新增 `backend/src/main/resources/dictionary/estoneii/**`，保存 eStoneII-IO 三份模板 XML 和 23 条候选映射 CSV。
- 新增 mapping 模块实体、仓库和服务:
  - Signal 字典
  - Event 告警字典
  - Controls 禁用参考
  - DeviceSignalCandidate
  - UnmappedSignalObservation
- `EStoneIITemplateDictionaryParser` 解析:
  - WITH_MIDPOINT: 39 Signal + 13 Event
  - NO_MIDPOINT: 37 Signal + 11 Event
  - Controls: 4
  - Candidates: HIGH 4 / MEDIUM 6 / PENDING_REAL_DATA 13
- `EStoneIIMappingService` 支持实时和告警映射:
  - `FSUID + DeviceID + SignalId`
  - `FSUID + DeviceCode + SignalId`
  - `DeviceID + SignalId`
  - `DeviceCode + SignalId`
  - `SignalId` 字典兜底
- `SendDataService`、`BInterface2016GetDataService`、`SendAlarmService` 在未匹配时记录 unmapped observation。
- `/api/telemetry/realtime`、`/api/alarms`、B接口 realtime/alarms/unmapped API 返回映射增强字段。
- 前端实时、告警、点位映射、未映射、B接口实时页面改为展示后端返回的名称、单位、值含义、映射状态和置信度。

## 发现的关键问题

- 原运行态表无法承载模板版本、置信度、值含义、Event 字典和 Controls 禁用参考，因此采用新增最小字典表方案。
- `/api/b-interface/unmapped-signals?fsuCode=...` 初始实现存在参数路径绕过 DataScope 的风险，已修正为先 DataScope 后按参数过滤。
- 项目当前使用 Hibernate `ddl-auto=update`，未引入显式 migration；生产化前需补 DDL。
- `VERIFIED_BY_REAL_DATA` 字段已预留，但真实数据确认写回尚未闭环。

## 安全边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未引入 StoneIII、`511600xx`、`ExtendField4`。
- 未把 B接口2024 混入 2016 主线。
- Controls 入库为 `enabledForControl=false`、`controlAccess=disabled`、`source=template_reference_only`。
- 前端未新增 Controls 可点击入口。

## 验证结果

已执行:

- `mvn -q -DskipTests compile`: 通过。
- `mvn -q test -Dtest='com.dcim.platform.mapping.*Test'`: 8 tests，0 failures。
- `mvn test -Dtest='*Security*Test,*DataScope*Test'`: 20 tests，0 failures。
- `mvn test -Dtest='*Template*Test,*Mapping*Test,*Realtime*Test,*Alarm*Test'`: 176 tests，0 failures。
- `mvn test -Dtest='*CommandHandlerTest,*SetCommandSafety*Test,*SetCommandSafetyGateTest'`: 201 tests，0 failures。
- `npm run build`: 通过；仅有既有 CSS 注释和 chunk size 警告。

未执行全量 `mvn test`: 当前任务限定不访问真实 FSU，且项目历史已存在全量协议类 pre-existing failures。本次验证边界聚焦映射、权限、DataScope、SET 安全和前端 build。

## 后续优先级

P1:

- 补显式 migration / DDL。
- 把 DataScope 从内存过滤迁移到 Repository/SQL 层。
- 建立 `VERIFIED_BY_REAL_DATA` 写回流程。
- 拆分候选映射和真实未映射观测 API。
- 候选映射/未映射补 `tenantId/stationId`。

P2:

- 前端按后端 `deviceName/deviceType` 动态分组。
- 增加人工确认、忽略、导出厂家清单和操作审计。

## 输出文件

- [审计报告](../audit/DATA-MAPPING-P0-002-estoneii-dictionary-import-and-mapping.md)
- [后续 TODO](../tasks/DATA-MAPPING-P0-002-follow-up-todo.md)

## 未修改说明

本次未访问真实 FSU，未执行 SET，未启 Scheduler，未删除 raw XML，未清空 message_log，未改 SET 安全门，未放松权限。
