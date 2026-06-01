# 2026-06-01 DATA-MAPPING-DICTIONARY-P1-VERIFY-001 B接口2016标准信号索引兼容映射复验

## 任务目标

复验 `DATA-MAPPING-DICTIONARY-P1-001` 的实现结果，确认 B接口2016 标准信号索引 fallback 正确接入，并且没有破坏 eStoneII 主字典优先级、前端状态展示、DataScope、安全边界和 unmapped observation 行为。

## 审计结论摘要

复验通过，未发现新增 P0。`0407102001` 和 `0407107001` 已进入 `MAPPED_CANDIDATE` / `LOW` / `BINTERFACE_2016_STANDARD` 候选映射链路，且 `needRealDataConfirm=true`。eStoneII-IO 主字典仍优先，B接口2016 标准索引仅作为 fallback。

## 关键发现

1. `0407102001` 映射为 `总电压`，单位 `V`。
2. `0407107001` 映射为 `后半组电压`，单位为空并由前端显示待确认，`0.0000` 未被误判为空。
3. `MAPPED_CANDIDATE` 已被前端识别为候选映射，不计入真正未映射。
4. 复验发现 GET_DATA / SEND_DATA 在本地 `monitoring_point` 未绑定时可能仍写入 unmapped observation，已做最小修复。
5. 未知 SignalId 仍进入 `UNMAPPED`；历史 observation 未删除。
6. DataScope 和 SET 安全门未发现回退。

## 修改说明

本次为审计复验中的小范围修复：

- `backend/src/main/java/com/dcim/platform/module/binterface/service/BInterface2016GetDataService.java`：候选映射命中且无 reason 时不再写入 observation。
- `backend/src/main/java/com/dcim/platform/module/binterface/service/SendDataService.java`：同样增加映射判定，避免标准2016候选被持久化为真正未映射。
- `backend/src/test/java/com/dcim/platform/binterface/BInterface2016GetDataServiceTest.java`：新增 GET_DATA observation 防回退测试。
- `backend/src/test/java/com/dcim/platform/binterface/SendDataServiceTest.java`：新增 SEND_DATA observation 防回退测试。
- `frontend/src/compat/realtimeSignalFilter.ts`：删除未使用的旧 SignalID 白名单，避免前端残留 `0407102001` / `0407107001` 映射逻辑。
- `frontend/src/views/telemetry/RealtimeDataView.vue`、`frontend/src/views/binterface/PointCard.vue`：展示后端返回的 `source` / `templateVariant`。

## 验证结果

- `mvn -q -DskipTests compile`：通过
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'`：189 tests 通过
- `mvn test -Dtest='BInterface2016GetDataServiceTest,SendDataServiceTest'`：34 tests 通过
- `mvn test -Dtest='*SetCommandSafety*Test,*SetCommandSafetyGateTest'`：39 tests 通过
- `cd frontend && npm run build`：通过，只有既有 Rollup/CSS/chunk warning

## 安全边界

本次未访问真实 FSU，未启动 Scheduler，未发起任何 SET 调用。Controls 仍为禁用参考。DataScope 参数过滤路径未发现回退。

## 后续优先级

1. `DATA-MAPPING-BACKFILL-P1-001`：对历史实时、告警和 observation 做只读回填。
2. `FE-BINTERFACE-REALTIME-CLEANUP-P1-001`：清理 B接口实时页默认 FSU / 分组兼容逻辑。
3. `BINTERFACE2016-STANDARD-DICT-P2-001`：按真实被动观察继续补充标准2016索引，但必须保留 eStoneII 主字典优先级。

## 最终结论

DATA-MAPPING-DICTIONARY-P1-VERIFY-001 复验通过，B接口2016 标准信号索引兼容映射未发现 P0，可进入 DATA-MAPPING-BACKFILL-P1-001 做历史数据只读回填。
