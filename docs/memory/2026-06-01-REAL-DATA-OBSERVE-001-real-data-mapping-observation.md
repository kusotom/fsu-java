# 2026-06-01 REAL-DATA-OBSERVE-001 真实数据映射状态只读观察

## 本次目标

在 DATA-MAPPING-P0-002、DATA-MAPPING-P0-VERIFY-001、FE-MAPPING-DIAG-001 之后，只读观察当前数据库、日志、样本和代码，确认实时数据、告警数据、未映射 observation 的映射状态分布。

## 执行边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改真实联调参数。
- 未修改业务代码。

## 审计结论摘要

当前 dev 库中:

- `realtime_data`: 7 条。
- `alarm_record`: 1 条。
- `b_interface_message_log`: 4695 条，主要为 LOGIN/UNKNOWN。
- 真实 FSU `51051243812345` 只有 2 条实时数据: `0407102001=54.2000`、`0407107001=0.0000`。
- 当前 dev 库没有 `estoneii_signal_dictionary`、`estoneii_event_dictionary`、`estoneii_control_reference`、`device_signal_candidate`、`unmapped_signal_observation` 表。

按当前源码 eStoneII 映射主线，实时数据中 7 条均会落入 `UNMAPPED`，因为当前库没有 `510000xxx` SignalId。真实 FSU 的 `0407102001/0407107001` 已在 B接口2016 标准信号字典索引中，但不在 DATA-MAPPING-P0-002 的 eStoneII `510000xxx` 主字典内。

## 发现的关键问题

1. 当前页面剩余未映射主要来自后端真实 `UNMAPPED`，不是 FE-MAPPING-DIAG-001 已修复的前端误判。
2. `0407102001/0407107001` 是真实 FSU 历史只读样本入库点位，属于标准 2016 字典补齐范围。
3. `FSU-001` 的 5 条实时数据和 1 条告警是 legacy/demo 编码，应与真实 FSU 数据分开统计。
4. 当前 dev DB 缺 eStoneII 字典表和 observation 表，说明当前库没有 DATA-MAPPING-P0-002 的完整运行态持久化结果。
5. `BInterfaceRealtimeView.vue` 仍有硬编码 FSU/分组，`realtimeSignalFilter.ts` 仍有 8 个 SignalID 临时白名单。

## 状态分布

实时数据:

- 已映射: 0。
- 候选映射: 0。
- 模板存在但未真实确认: 0。
- 待真实数据确认: 0。
- 真正未映射: 7。

告警数据:

- 总数: 1。
- `UNKNOWN_EVENT_ID`: 1，样本为 `FSU-001 / TEMP-HIGH`。

未映射 observation:

- 当前 dev DB 表不存在，无法统计历史 observation。

## 后续优先级

P1:

- `DATA-MAPPING-DICTIONARY-P1-001`: 补充真实 FSU 返回的标准 2016 `0407/0418` 系列 SignalId/EventId/rawId 字典，并定义与 eStoneII `510000xxx` 字典的边界。
- `DATA-MAPPING-BACKFILL-P1-001`: 字典策略确定后，对历史 `realtime_data/alarm_record` 做映射状态和 observation 回填。
- `REAL-DATA-PASSIVE-OBSERVE-P1-001`: 只读被动观察新的真实 FSU 上报数据。
- `FE-BINTERFACE-REALTIME-CLEANUP-P1-001`: 清理 B接口实时页硬编码 FSU/分组/白名单。

## 验证结果

```bash
cd /home/tom/桌面/FSU/fsu-platform-java/backend
mvn -q -DskipTests compile
mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'

cd /home/tom/桌面/FSU/fsu-platform-java/frontend
npm run build
```

结果:

- compile PASS。
- 后端测试 PASS，`Tests run: 186, Failures: 0, Errors: 0, Skipped: 0`。
- 前端 build PASS，有既有 Rollup/CSS/chunk size warning。

## 下一步建议

REAL-DATA-OBSERVE-001 完成。当前剩余未映射状态已完成只读观察和分类，未发现新增 P0。后续应根据样本分布进入字典补齐、历史回填或被动观察任务。
