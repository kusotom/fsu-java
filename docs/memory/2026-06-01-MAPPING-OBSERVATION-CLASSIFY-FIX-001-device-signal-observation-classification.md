# 2026-06-01 MAPPING-OBSERVATION-CLASSIFY-FIX-001 设备/点位观测分类修复

## 任务目标

根据用户截图排查并修复前端仍显示大量“未映射”的问题，重点收敛真正未映射点位、候选映射、模板待确认、设备发现记录、历史旧数据和未知事件的分类口径。

## 结论摘要

本次完成小范围代码修复与回归验证，未发现新增 P0。`GET_LOGININFO` 无 `SignalID/SPID/rawId` 的设备发现记录不再计为真正未映射点位；`MAPPED_CANDIDATE`、模板待确认、历史旧实时数据也不再污染前端未映射统计。

## 关键问题

- 未映射页把 `GET_LOGININFO` 设备记录作为 `missing_signal_mapping` 展示，实际这些记录没有测点身份。
- 实时数据页把 `source=UNKNOWN`、设备为空、旧字段的历史 seed/demo/legacy 数据计入未映射。
- 运行中的本地后端返回旧 DTO，说明页面可能命中未重启后端或旧前端 bundle。
- `0407102001 / 0407107001` 已在代码链路进入 B接口2016 fallback 候选映射，不应计为真正 `UNMAPPED`。

## 本次修改

后端：

- `BInterfaceFrontendDtos` 增加 `hasSignalIdentity/isDeviceOnly/legacyData/observationType`。
- `UnmappedSignalObservationService` 跳过新的 `GET_LOGININFO` 无测点身份 observation。
- `RealtimeDataService` 将旧实时数据归类为 `HISTORICAL_PENDING_BACKFILL`。
- `BInterfaceFrontendReadController` 在 unmapped 查询层重分类历史 observation，区分 `DEVICE_ONLY`、`SIGNAL_PENDING`、`UNMAPPED_SIGNAL` 和映射命中 observation。
- 新增/补充分类测试。

前端：

- `mappingStatus.ts` 增加 `DEVICE_ONLY`、`SIGNAL_PENDING`、`WAIT_GET_DATA`、`HISTORICAL_PENDING_BACKFILL`，并收紧真正未映射判定。
- 实时数据页增加“真实未映射”和“历史待回填”统计。
- 未映射页拆分“真实未映射、未知告警事件、设备待测点、模板/待确认”。
- 点位映射页不把设备级记录计入映射总数。
- B接口实时页未映射统计改用 `isTrueUnmappedPoint`。

## 验证结果

- `mvn -q -DskipTests compile`：通过。
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'`：193 tests 通过。
- `mvn test -Dtest='UnmappedSignalObservationServiceTest,BInterfaceFrontendMappingClassificationTest,RealtimeDataServiceMappingClassificationTest'`：7 tests 通过。
- `mvn test -Dtest='*SetCommandSafety*Test,*SetCommandSafetyGateTest'`：39 tests 通过。
- `cd frontend && npm run build`：通过，保留既有 Rollup/CSS/chunk warning。

## 安全边界

本次未访问真实 FSU，未启动 Scheduler，未发起 SET 写操作。DataScope 与 SET 安全门相关测试通过。

## 后续优先级

1. 重启后端并重新部署前端 bundle，确认页面命中新 DTO。
2. 进入 `DATA-MAPPING-BACKFILL-P1-001`，做历史实时、告警和 observation 的只读/幂等回填。
3. 继续执行 `FE-BINTERFACE-REALTIME-CLEANUP-P1-001`，清理 B接口实时页兼容展示。

## 输出

- [审计报告](../audit/MAPPING-OBSERVATION-CLASSIFY-FIX-001-device-signal-observation-classification.md)
- [后续 TODO](../tasks/MAPPING-OBSERVATION-CLASSIFY-FIX-001-follow-up-todo.md)

## 最终结论

MAPPING-OBSERVATION-CLASSIFY-FIX-001 完成，设备级 GET_LOGININFO 记录、候选映射、模板待确认和真正未映射点位已完成分类收敛；前端未映射统计不再被无 SPID 的设备记录和历史旧数据污染。
