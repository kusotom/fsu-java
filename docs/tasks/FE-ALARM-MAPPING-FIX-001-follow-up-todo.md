# FE-ALARM-MAPPING-FIX-001 后续 TODO

| 优先级 | 任务编号 | 问题描述 | 影响范围 | 建议修改文件 | 验收标准 | 是否涉及后端 | 是否涉及权限 | 是否涉及协议安全边界 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P1 | FE-ALARM-MAPPING-P1-001 | 后端 `eventSeverity` 原始数字码仍需统一成稳定中文等级字段，当前前端只做兼容展示 | 告警中心、驾驶舱、B接口告警页 | `EStoneIIMappingService`、`AlarmRecordService`、`BInterfaceFrontendReadController` | API 返回 `eventSeverityLabel` 或等价字段，前端不再需要解释 eStoneII 数字等级 | 是 | 否 | 否 |
| P1 | FE-ALARM-MAPPING-P1-002 | 告警 DTO 缺少 `mappingReason/unmappedReason`，未知 EventId 在告警行里不够直观 | 告警排障、UNKNOWN_EVENT_ID 显示 | `BInterfaceFrontendDtos.AlarmDto`、告警映射服务、告警页面 | `UNKNOWN_EVENT_ID` 可在告警列表和详情中直接显示原因 | 是 | 否 | 否 |
| P2 | FE-ALARM-MAPPING-P2-001 | 告警中心缺少按映射状态和事件来源筛选 | 告警运维体验 | `AlarmCenterView.vue` | 可按已映射、候选映射、未知事件、待确认筛选告警 | 否 | 否 | 否 |
| P2 | FE-ALARM-MAPPING-P2-002 | 告警详情缺少 Event 字典专项区 | 告警排障体验 | `AlarmCenterView.vue` 或详情组件 | 详情展示 EventId、EventName、SignalName、EventSeverity、AlarmMeaning、映射来源和置信度 | 否 | 否 | 否 |
