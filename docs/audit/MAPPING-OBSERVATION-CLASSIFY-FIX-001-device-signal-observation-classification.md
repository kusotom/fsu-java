# MAPPING-OBSERVATION-CLASSIFY-FIX-001 设备/点位观测分类修复报告

## 1. 问题现象

用户截图显示前端仍有大量“未映射”：

- 站点实时数据页：实时点位 7、有值点位 7、未映射 7，数据为 `FSU-1 / FSU-2`、设备 `-`、点位名称 `未命名点位`、来源 `UNKNOWN`、时间为 2026-05-10 / 2026-05-27。
- 点位字典页：已能看到 `0407102001 = 总电压`、`0407107001 = 后半组电压`，说明 B接口2016 标准索引已进入字典。
- 点位映射页：10 条记录均为 `SPID=-`、`模板存在，真实未确认`，这类是候选/模板记录，不是真正未映射点位。
- 未映射点位页：10 条记录来源为 `GET_LOGININFO`，`SPID=-`、`rawId=null`、`reason=missing_signal_mapping`，这是本次核心问题。

结论：截图中的主要问题不是 `MAPPED_CANDIDATE` 未识别，而是“设备级观测、历史旧数据、模板候选”和“真正未映射点位”统计口径混在了一起。

## 2. 截图证据解释

| 页面 | 现象 | 判断 |
|---|---|---|
| 站点实时数据页 | `FSU-1 / FSU-2`、设备为空、来源 `UNKNOWN` | 历史 seed/demo/legacy realtime 数据，不能作为当前真实测点未映射结论 |
| 点位字典页 | `0407102001 / 0407107001` 已显示名称 | 字典兼容层存在，问题不是字典完全缺失 |
| 点位映射页 | `SPID=-`、模板待确认 | 设备级/模板候选，不能计入真正未映射点位 |
| 未映射点位页 | `GET_LOGININFO`、无 `SignalID/SPID/rawId` | 设备发现记录，应归为 `DEVICE_ONLY` 或等待 GET_DATA |

## 3. 涉及页面

- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/resource/UnmappedPointsView.vue`
- `frontend/src/views/resource/PointMappingView.vue`
- `frontend/src/views/binterface/BInterfaceRealtimeView.vue`
- `frontend/src/utils/mappingStatus.ts`
- `frontend/src/components/common/StatusBadge.vue`

## 4. 涉及接口

- `/api/telemetry/realtime`
- `/api/b-interface/realtime-points`
- `/api/b-interface/unmapped-signals`
- `/api/b-interface/unmapped-signals?fsuCode=51051243812345`

## 5. 后端接口返回样本

本次排查过程中读取当前本地运行实例，发现运行实例尚未加载最新编译代码：

- `/api/telemetry/realtime` 返回的是旧 `RealtimeDataEntity` 字段，例如 `id/fsuId/pointCode/valueNumber`，没有完整映射 DTO 字段。
- `/api/b-interface/realtime-points?fsuCode=51051243812345` 返回 `0407102001 / 0407107001`，但 `signalName/unit/source` 为空，`mappingStatus=mapped`，与当前源码 fallback 逻辑不一致。
- `/api/b-interface/unmapped-signals?fsuCode=51051243812345` 返回 5 条 `GET_LOGININFO` 设备级记录，均无 `spid/signalId/rawId`。

样本特征：

```text
source=GET_LOGININFO
deviceId=51051241820004 / 51051241830004 / 51051241840004 / 51051240700002 / 51051243812345
spid=null
signalId=null
rawId=null
reason=missing_signal_mapping
```

这类记录只有设备身份，没有测点身份，不应进入“真正未映射点位”统计。

## 6. GET_LOGININFO 是否被错误计入未映射点位

是。修复前，`GET_LOGININFO` 返回的设备发现记录在无 `SignalID/SPID/rawId` 时仍可能以 `missing_signal_mapping` 口径进入未映射列表，并被前端统计为真实未映射点位。

本次修复：

- 后端 `UnmappedSignalObservationService.record` 对 `GET_LOGININFO` 且无测点身份的记录直接跳过，不再新增真正 unmapped observation。
- 后端未映射查询层把历史 `GET_LOGININFO` 设备级 observation 归类为 `DEVICE_ONLY` / `DEVICE_DISCOVERED_WAIT_GET_DATA`。
- 前端未映射统计排除 `DEVICE_ONLY`、`SIGNAL_PENDING`、`WAIT_GET_DATA` 和无测点身份记录。

## 7. 历史 source UNKNOWN 数据来源判断

当前 dev 库 `realtime_data` 中存在 7 条实时数据，其中：

- `FSU-001` 的 `TEMP-001/HUMI-001/VOLT-001/DOOR-001/WATER-001` 属于历史 seed/demo/legacy 数据。
- 真实 FSU `51051243812345` 有 `0407102001=54.2000`、`0407107001=0.0000`。

对 `source=UNKNOWN`、设备为空、测点编码非标准数字索引的历史旧数据，本次归类为：

```text
mappingStatus=HISTORICAL_PENDING_BACKFILL
mappingConfidence=UNKNOWN
source=legacy_realtime_data
observationType=HISTORICAL_REALTIME
```

它们不再污染当前实时页的“真实未映射”统计，历史处理留给回填任务。

## 8. 0407102001 / 0407107001 当前状态

代码链路中，`0407102001` 和 `0407107001` 已通过 B接口2016 标准信号索引 fallback 进入候选映射：

- `mappingStatus=MAPPED_CANDIDATE`
- `mappingConfidence=LOW`
- `templateVariant/source=BINTERFACE_2016_STANDARD`
- `needRealDataConfirm=true`

`0407107001 = 0.0000` 通过测试确认不会被误判为空。

如果运行页面仍显示这两个点位为旧状态，原因应优先判断为后端未重启、前端未加载新 bundle 或历史数据未回填，而不是当前映射源码失效。

## 9. 设备记录与点位记录分类口径

本次固化以下口径：

| 类型 | 判定条件 | 统计口径 |
|---|---|---|
| 真正未映射点位 | `mappingStatus=UNMAPPED` 且有 `SignalID/SPID/rawId`，不是设备级，不是历史旧数据 | 计入真实未映射 |
| 未知告警事件 | `mappingStatus=UNKNOWN_EVENT_ID` | 单独统计 |
| 候选映射 | `MAPPED_CANDIDATE/HIGH/MEDIUM/LOW` | 不计入真实未映射 |
| 模板存在但未确认 | `TEMPLATE_ONLY/PENDING_REAL_DATA` | 不计入真实未映射 |
| 设备已发现但无测点 | `GET_LOGININFO` 且无 `SignalID/SPID/rawId` | `DEVICE_ONLY`，不计入真实未映射 |
| 等待测点身份 | 无测点身份但非 GET_LOGININFO | `SIGNAL_PENDING`，不计入真实未映射 |
| 历史旧实时数据 | `source=UNKNOWN` 或旧字段且无法形成测点身份 | `HISTORICAL_PENDING_BACKFILL`，不计入真实未映射 |

## 10. 修改文件清单

后端：

- `backend/src/main/java/com/dcim/platform/module/binterface/dto/BInterfaceFrontendDtos.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/UnmappedSignalObservationService.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/service/RealtimeDataService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/test/java/com/dcim/platform/mapping/UnmappedSignalObservationServiceTest.java`
- `backend/src/test/java/com/dcim/platform/telemetry/RealtimeDataServiceMappingClassificationTest.java`
- `backend/src/test/java/com/dcim/platform/binterface/BInterfaceFrontendMappingClassificationTest.java`

前端：

- `frontend/src/utils/mappingStatus.ts`
- `frontend/src/components/common/StatusBadge.vue`
- `frontend/src/views/telemetry/RealtimeDataView.vue`
- `frontend/src/views/resource/UnmappedPointsView.vue`
- `frontend/src/views/resource/PointMappingView.vue`
- `frontend/src/views/binterface/BInterfaceRealtimeView.vue`

## 11. 测试结果

| 命令 | 结果 |
|---|---|
| `mvn -q -DskipTests compile` | 通过 |
| `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'` | 通过，193 tests |
| `mvn test -Dtest='UnmappedSignalObservationServiceTest,BInterfaceFrontendMappingClassificationTest,RealtimeDataServiceMappingClassificationTest'` | 通过，7 tests |
| `mvn test -Dtest='*SetCommandSafety*Test,*SetCommandSafetyGateTest'` | 通过，39 tests |
| `cd frontend && npm run build` | 通过，存在既有 Rollup/CSS/chunk warning |

测试覆盖：

- `GET_LOGININFO` 无测点身份记录不计为真正未映射。
- 未知 SignalId 且有测点身份时仍计为 `UNMAPPED`。
- `0407102001 / 0407107001` 不计为真正 `UNMAPPED`。
- `0.0000` 正常保留。
- `source UNKNOWN` 且旧字段无法形成当前测点身份时归类为历史待回填。
- DataScope 与 SET 安全测试未回退。

## 12. P0/P1/P2 问题清单

### P0

未发现新增 P0。

### P1

- `DATA-MAPPING-BACKFILL-P1-001`：历史 realtime / alarm / unmapped observation 仍需只读、幂等回填。
- 当前运行中的本地服务需要重启或重新部署，确保页面命中新 DTO 和新 bundle。
- `FE-BINTERFACE-REALTIME-CLEANUP-P1-001`：B接口实时页仍建议继续清理默认 FSU / 分组兼容逻辑。

### P2

- 未映射/设备待测点/历史待回填可增加趋势和按设备聚合。
- Dashboard 可进一步拆分“设备待测点”和“真实未映射点位”的统计卡片。

## 13. 是否允许进入 DATA-MAPPING-BACKFILL-P1-001

允许。当前分类口径已经收敛，设备级 `GET_LOGININFO`、候选映射、模板待确认和历史旧数据不再被计入真正未映射点位。下一步可以对历史实时、告警和 observation 做只读、幂等回填。

## 14. 后续建议

1. 重启后端并重新部署前端 bundle，确认页面接口返回已加载本次修改。
2. 执行 `DATA-MAPPING-BACKFILL-P1-001`，回填历史状态，不删除历史 observation。
3. 执行 `FE-BINTERFACE-REALTIME-CLEANUP-P1-001`，继续清理 B接口实时页兼容展示。
4. 对被动观察的新 `UNMAPPED / UNKNOWN_EVENT_ID` 样本进入字典补齐流程。

## 15. 最终结论

MAPPING-OBSERVATION-CLASSIFY-FIX-001 完成，设备级 GET_LOGININFO 记录、候选映射、模板待确认和真正未映射点位已完成分类收敛；前端未映射统计不再被无 SPID 的设备记录和历史旧数据污染。
