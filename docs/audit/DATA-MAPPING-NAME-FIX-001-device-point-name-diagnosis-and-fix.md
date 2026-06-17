# DATA-MAPPING-NAME-FIX-001 设备名称/点位名称未确认诊断与修复

日期：2026-06-02

## 1. 任务背景

FE-MONITOR-UX-P1-001 完成后，普通实时数据和告警页面已做信息收敛，但页面仍出现“设备名称未确认 / 点位名称未确认”一类文案。协议口径确认当前主线仍是 eStoneII-IO `510000xxx` SignalId，B接口2016 标准索引 `0407102001 / 0407107001` 仅作为 fallback 候选映射；StoneIII、`511600xx`、`ExtendField4` 不进入当前主逻辑。

本次目标是排查名称没有正确展示的真实原因，并在不改变协议、安全和权限边界的前提下做小范围修复。

## 2. 执行边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改 raw XML、run-once、SET 安全门、权限模型或 DataScope 规则。
- 未引入 StoneIII、`511600xx`、`ExtendField4`。
- 未在前端页面硬编码 SignalId 到名称、单位或 0/1 含义。

## 3. 读取文件清单

- `docs/PROJECT_ENGINEERING_RULES.md`
- `docs/rules/CLAUDE_PROJECT_RULES.md`
- `docs/memory/WORKING-MEMORY.md`
- `docs/memory/README.md`
- `docs/audit/DATA-MAPPING-DICTIONARY-P1-001-binterface2016-standard-signal-dictionary.md`
- `docs/audit/REAL-DATA-OBSERVE-001-real-data-mapping-observation.md`
- `docs/audit/FE-MAPPING-DIAG-001-monitoring-unmapped-diagnosis.md`
- `docs/audit/FE-MONITOR-UX-P1-001-monitor-and-alarm-ux-convergence.md`

## 4. 代码读取范围

- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIMappingService.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/repository/DeviceSignalCandidateRepository.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/service/RealtimeDataService.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/service/AlarmRecordService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/dto/BInterfaceFrontendDtos.java`
- `frontend/src/utils/monitorAdapters.ts`

## 5. 问题原因

本次确认问题不是协议资料缺失，而是名称字段在后端 DTO 和前端适配层之间没有完整透传。

主要问题：

1. `EStoneIIMappingService` 在候选映射命中但 Signal 字典缺失时，没有使用 `DeviceSignalCandidate.signalName` 兜底，可能导致候选点位已有名称但结果 `signalName=null`。
2. `EStoneIIMappingService` 对未知 SignalId 即使 DeviceID 已在现场候选清单中，也没有返回设备名称，导致未知点位无法显示 `TempHumidity/Smoke/...` 这类设备名。
3. `RealtimeDataService` 没有把 `mapped.deviceId/deviceCode/deviceName` 写入 `RealtimePointDto`。
4. `RealtimeDataService` 没有读取 `monitoring_point.pointName/unit/pointType` 给历史实时数据兜底，旧数据只能落为待确认。
5. `AlarmRecordService` 和 B接口告警只读 DTO 未透传 `deviceName`。
6. `monitorAdapters.ts` 在后端没有 `deviceName` 时会过早显示“待确认”，并且可能把 `deviceCode` 当普通业务设备名称展示。

## 6. 修复方案

后端修复：

- `DeviceSignalCandidateRepository` 增加按 DeviceID / DeviceCode 查询候选设备的方法。
- `EStoneIIMappingService`：
  - 候选映射命中时优先使用 Signal 字典名称，缺失时使用候选表 `signalName`。
  - 标准 2016 fallback 和未知点位都可按 DeviceID / DeviceCode 返回设备名。
  - 未知 SignalId 仍保持 `UNMAPPED`，不冒充已映射点位。
- `RealtimeDataService`：
  - `RealtimePointDto` 透传 `deviceId/deviceCode/deviceName`。
  - 增加 `pointName` 字段。
  - 使用 `monitoring_point` 作为历史实时数据的名称、单位、类型兜底。
  - 保留 `0407107001` 空单位，前端继续显示单位待确认。
- `AlarmRecordService` 和 `BInterfaceFrontendReadController`：
  - 告警 DTO 透传 `deviceName`。

前端修复：

- `monitorAdapters.ts`：
  - 普通实时页优先展示后端 `deviceName` 与 `pointName/signalName`。
  - 历史数据缺名称时显示“历史设备 / 历史点位”。
  - 真正未映射显示“待映射点位”。
  - 其他待确认场景显示“待确认设备 / 待确认点位”。
  - 不在普通业务表中用 DeviceID/DeviceCode 冒充设备名称。

## 7. 标准样例复核

已通过单元测试覆盖：

| 样例 | 修复后结果 |
|---|---|
| `deviceId=51051241830004`, `signalId=510000211` | 返回 `deviceName=TempHumidity`, `pointName/signalName=I2C温度`, `unit=℃`, `mappingStatus=MAPPED_CANDIDATE` |
| `deviceId=51051241820004`, `signalId=510000250` | 返回 `deviceName=Smoke`, `signalName=烟感`, `valueMeaning=有告警` |
| `deviceId=51051241830004`, `signalId=999999999` | 返回 `deviceName=TempHumidity`，但 `mappingStatus=UNMAPPED`，点位仍为待映射 |
| `signalId=0407107001`, `value=0.0000` | 返回 `pointName/signalName=后半组电压`，值 `0.0000` 保留，单位为空供前端显示待确认 |
| legacy `TEMP-001` | 返回 `pointName=机柜温度`, `unit=℃`, `mappingStatus=HISTORICAL_PENDING_BACKFILL` |
| 告警 `Smoke + 510000250` | 返回 `deviceName=Smoke`, `signalName=烟感`, `eventName=烟感告警`, `eventSeverity=一级告警` |

## 8. 历史数据说明

当前 `realtime_data` 实体本身没有 DeviceID 字段。对已经入库且缺 DeviceID 的历史实时数据，后端不能可靠推断真实设备实例，只能：

- 用 `monitoring_point` 显示点位名称、单位、类型；
- 用 `legacy_realtime_data` / `HISTORICAL_PENDING_BACKFILL` 标记历史待回填；
- 不宣称这些历史数据已经完成真实现场映射。

因此历史数据仍建议进入 `DATA-MAPPING-BACKFILL-P1-001` 做只读/幂等回填。

## 9. 修改文件清单

- `backend/src/main/java/com/dcim/platform/module/mapping/repository/DeviceSignalCandidateRepository.java`
- `backend/src/main/java/com/dcim/platform/module/mapping/service/EStoneIIMappingService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/dto/BInterfaceFrontendDtos.java`
- `backend/src/main/java/com/dcim/platform/module/telemetry/service/RealtimeDataService.java`
- `backend/src/main/java/com/dcim/platform/module/alarm/service/AlarmRecordService.java`
- `backend/src/main/java/com/dcim/platform/module/binterface/controller/BInterfaceFrontendReadController.java`
- `backend/src/test/java/com/dcim/platform/mapping/EStoneIIMappingServiceTest.java`
- `backend/src/test/java/com/dcim/platform/telemetry/RealtimeDataServiceMappingClassificationTest.java`
- `backend/src/test/java/com/dcim/platform/alarm/AlarmRecordServiceNameMappingTest.java`
- `frontend/src/utils/monitorAdapters.ts`

## 10. 测试结果

已执行：

```bash
cd backend
mvn -q -DskipTests compile
mvn test -Dtest='EStoneIIMappingServiceTest,RealtimeDataServiceMappingClassificationTest,AlarmRecordServiceNameMappingTest'
mvn test -Dtest='*Mapping*Test,*Template*Test,*Realtime*Test,*Alarm*Test'
mvn test -Dtest='*Security*Test,*DataScope*Test'

cd frontend
npm run build
```

结果：

- compile：通过。
- 名称映射定向测试：14 tests，0 failures，0 errors。
- Mapping/Template/Realtime/Alarm 回归：188 tests，0 failures，0 errors。
- Security/DataScope：20 tests，0 failures，0 errors。
- 前端 build：通过。

既有 warning：

- Maven 测试中部分用例主动模拟网络/DB异常并打印堆栈，测试结果仍为 0 failure/0 error。
- Vite/Rollup 保留既有 `@vueuse/core` pure annotation warning 和 chunk size warning。

## 11. 安全边界影响

- DataScope 测试通过，未回退。
- 未放松 raw XML / run-once / SET 权限。
- 未新增任何 Controls 或 SET 入口。
- 未访问真实 FSU。
- 未启 Scheduler。

## 12. P0/P1/P2 问题清单

P0：无。

P1：

- 历史实时数据缺 DeviceID，仍需 `DATA-MAPPING-BACKFILL-P1-001` 做只读/幂等回填。
- `realtime_data` 长期应补充 DeviceID/DeviceCode 或通过 point_id 绑定设备实例，否则普通实时 API 只能显示点位名，不能可靠显示设备实例名。
- 告警 DTO 可后续补充稳定 `mappingReason/unmappedReason`，减少前端排障解释。

P2：

- 前端可在详情折叠区展示“名称来源：候选映射 / 标准2016 fallback / 历史回填”，但普通主表不应展示协议字段。

## 13. 最终结论

DATA-MAPPING-NAME-FIX-001 完成。设备名称/点位名称未确认问题已定位并修复：当前后端可基于真实 DeviceID 与 eStoneII-IO `510000xxx` SignalId 返回设备名称、点位名称、单位和值含义；前端普通实时数据和告警页面通过 `monitorAdapters` 统一消费后端映射结果。历史旧数据仍按 legacy/待回填处理，不冒充真实已映射点位。未访问真实 FSU，未执行 SET，未启 Scheduler，未放松权限和 DataScope。
