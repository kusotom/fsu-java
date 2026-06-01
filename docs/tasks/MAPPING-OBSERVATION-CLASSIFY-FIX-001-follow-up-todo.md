# MAPPING-OBSERVATION-CLASSIFY-FIX-001 后续 TODO

## P0

无新增 P0。

## P1

### MAPPING-OBSERVATION-P1-001 重启/部署后复查接口返回

- 问题描述：排查时本地运行实例仍返回旧 DTO，页面可能命中未重启后端或旧前端 bundle。
- 影响范围：实时数据页、B接口实时页、未映射点位页。
- 建议修改文件：无业务代码修改；部署脚本或运行环境按实际情况处理。
- 验收标准：`/api/telemetry/realtime`、`/api/b-interface/realtime-points`、`/api/b-interface/unmapped-signals` 返回 `mappingStatus/mappingConfidence/source/observationType/hasSignalIdentity/isDeviceOnly` 等新字段。
- 是否涉及后端：是。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

### DATA-MAPPING-BACKFILL-P1-001 历史映射状态只读回填

- 问题描述：历史 realtime / alarm / unmapped observation 仍可能保存旧状态或旧 reason。
- 影响范围：历史统计、未映射页、Dashboard 摘要。
- 建议修改文件：新增只读/幂等回填 service、migration 或任务脚本，具体按后续任务设计。
- 验收标准：历史 `0407102001 / 0407107001` 进入候选映射状态；历史 `GET_LOGININFO` 设备记录归入设备待测点；未知点位仍保留为真正 `UNMAPPED`。
- 是否涉及后端：是。
- 是否涉及权限：是，必须遵守 DataScope。
- 是否涉及协议安全边界：是，不访问真实 FSU，不触发写命令。

### FE-BINTERFACE-REALTIME-CLEANUP-P1-001 B接口实时页兼容展示清理

- 问题描述：B接口实时页仍有默认 FSU / 分组兼容逻辑遗留。
- 影响范围：协议诊断下的 B接口实时页面。
- 建议修改文件：`frontend/src/views/binterface/BInterfaceRealtimeView.vue` 及相关 API normalizer。
- 验收标准：页面完全消费后端映射结果和分类字段，不使用前端点位白名单或硬编码分组推断业务名称。
- 是否涉及后端：否。
- 是否涉及权限：是，保留现有页面权限。
- 是否涉及协议安全边界：是，不触发 SET，不主动访问真实 FSU。

## P2

### MAPPING-OBSERVATION-P2-001 分类统计体验增强

- 问题描述：当前已区分真正未映射、未知事件、设备待测点、历史待回填，但 Dashboard 和列表筛选还可增强。
- 影响范围：Dashboard、实时数据页、未映射点位页。
- 建议修改文件：`frontend/src/views/DashboardView.vue`、`frontend/src/views/telemetry/RealtimeDataView.vue`、`frontend/src/views/resource/UnmappedPointsView.vue`。
- 验收标准：可按分类筛选和查看趋势，统计口径与 `mappingStatus.ts` 一致。
- 是否涉及后端：可选。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。

## P3

### MAPPING-OBSERVATION-P3-001 展示文案优化

- 问题描述：设备待测点、历史待回填、模板待确认可补充更细的运维说明。
- 影响范围：前端展示文案。
- 建议修改文件：状态字典和页面提示组件。
- 验收标准：用户能从页面区分“设备已发现但未返回测点”和“真正未映射点位”。
- 是否涉及后端：否。
- 是否涉及权限：否。
- 是否涉及协议安全边界：否。
