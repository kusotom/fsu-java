# DATA-MAPPING-NAME-FIX-001 后续 TODO

## P0

无。

## P1

### DATA-MAPPING-BACKFILL-P1-001

- 问题描述：历史 `realtime_data` / `alarm_record` 中仍可能缺少 DeviceID、mappingStatus、deviceName、signalName 等新链路字段，页面只能按历史/待回填展示。
- 影响范围：普通实时数据页、告警中心、监控主页统计。
- 建议修改文件：后端新增只读/幂等回填 Service、对应测试与审计文档。
- 验收标准：历史 `0407102001 / 0407107001` 可进入 B接口2016候选映射；legacy demo 数据保持历史待回填；不访问真实 FSU；不执行 SET。
- 是否涉及后端：是。
- 是否涉及权限：否，但必须保留 DataScope 查询边界。
- 是否涉及协议安全边界：是，必须只读、不可触发真实 FSU。

### DATA-MAPPING-DEVICE-ID-P1-001

- 问题描述：`realtime_data` 实体当前没有 DeviceID/DeviceCode 字段，普通实时 API 对缺 DeviceID 的历史记录无法可靠显示设备实例名称。
- 影响范围：实时数据业务展示、设备级点位映射、站点/FSU 详情。
- 建议修改文件：`RealtimeDataEntity`、schema migration、SEND_DATA/GET_DATA 入库链路、`RealtimeDataService`、测试。
- 验收标准：新入库实时数据保留 DeviceID/DeviceCode；`DeviceID + SignalId` 可稳定返回设备名和点位名；历史数据不被误标为真实确认。
- 是否涉及后端：是。
- 是否涉及权限：否。
- 是否涉及协议安全边界：是，仅处理入库字段，不新增 SET 或轮询。

### ALARM-MAPPING-REASON-P1-001

- 问题描述：告警 DTO 仍缺少稳定 `mappingReason/unmappedReason` 字段，未知 EventId 排查依赖 observation 或技术信息。
- 影响范围：告警详情、内部诊断、后续字典补齐。
- 建议修改文件：`AlarmDto`、`AlarmRecordService`、`BInterfaceFrontendReadController`、告警前端详情。
- 验收标准：`UNKNOWN_EVENT_ID` / `UNKNOWN_SIGNAL_ID` 可在详情技术信息中明确展示，普通主表不暴露协议字段。
- 是否涉及后端：是。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

## P2

### FE-MONITOR-DETAIL-SOURCE-P2-001

- 问题描述：普通主表已收敛，但管理员详情中可进一步展示名称来源，便于判断候选映射、标准2016 fallback、历史回填的区别。
- 影响范围：实时数据详情、告警详情、FSU 详情。
- 建议修改文件：`RealtimeDataView.vue`、`AlarmCenterView.vue`、`FsuStatusDetailView.vue`。
- 验收标准：普通主表不显示协议字段；管理员技术信息折叠区可见 `source/mappingStatus/mappingConfidence`。
- 是否涉及后端：否。
- 是否涉及权限：是，应限制在管理员详情或内部诊断。
- 是否涉及协议安全边界：否。
