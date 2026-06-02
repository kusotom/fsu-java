# FE-MONITOR-UX-P1-001 后续 TODO

| 优先级 | 任务编号 | 问题描述 | 影响范围 | 建议修改文件 | 验收标准 | 是否涉及后端 | 是否涉及权限 | 是否涉及协议安全边界 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P1 | FE-MONITOR-UX-P1-002 | FSU 详情页仍缺实时数据、告警记录 Tab | 站点/FSU 二级交互闭环 | `FsuStatusDetailView.vue`、实时/告警 API 适配器 | FSU 详情可查看该 FSU 下实时点位和告警记录，主表仍隐藏协议字段 | 否 | 否 | 否 |
| P1 | FE-MONITOR-UX-P1-003 | 后端未输出稳定 `eventSeverityLabel/mappingReason`，前端仍需兼容解释部分码值 | 告警展示一致性 | `EStoneIIMappingService`、`AlarmRecordService`、告警 DTO | 告警 API 返回稳定中文等级和映射原因，前端只展示后端语义 | 是 | 否 | 否 |
| P1 | FE-MONITOR-UX-P1-004 | 站点详情页尚未承载站点维度实时/告警二级交互 | 站点监控业务闭环 | `SiteManagementView.vue` 或新增站点详情页 | 从站点进入站点实时数据、当前告警、历史告警和 FSU 列表 | 可能 | 是 | 否 |
| P2 | FE-MONITOR-UX-P2-001 | 驾驶舱缺少趋势图和数据质量图 | 监控体验 | `DashboardView.vue` | 增加 FSU 在线趋势、告警趋势、数据异常趋势，且不展示协议字段 | 否 | 否 | 否 |
| P2 | FE-MONITOR-UX-P2-002 | 前端缺少权限矩阵 UI 自动化验证 | 回归质量 | 前端测试目录、route/menu 测试 | 覆盖普通用户/三方用户不可见协议诊断、点位治理、系统审计 | 否 | 是 | 是 |
