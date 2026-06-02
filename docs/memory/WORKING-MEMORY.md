# FSU-JAVA 当前工作记忆

## 最新任务：FE-REALTIME-POINT-SEMANTIC-FIX-001 站点实时数据点位/设备/测点语义修复 (2026-06-03)

本次目标是修正“站点实时数据 > 实时数据”页面中 FSU 点位、采集设备、测点/信号混用的问题。前端普通业务展示现在明确：FSU 是业务点位层，Smoke/TempHumidity/WaterLeak/Power/Battery1 是采集设备，I2C温度/湿度/烟感/水浸/市电/电池电压是测点或监控项。

本次修改:

- `frontend/src/utils/monitorAdapters.ts` 新增 `fsuPointName/deviceKey/signalKey/measurementKey/displaySignalName`，并把 `displayDevice` 注明为采集设备、`displayPointName` 注明为旧字段兼容的测点/信号。
- `summarizeRealtimePoints` 新增 `fsuPointCount/collectingDeviceCount/realtimeSignalCount`，分别统计 FSU 点位、采集设备和实时测点。
- `RealtimeDataView.vue` 将页面说明、筛选、指标卡和主表列名改为 FSU / 点位、采集设备、测点名称、当前值/状态、单位、业务状态、采集时间。
- `DashboardView.vue` 数据状态摘要同步展示实时测点、采集设备、FSU / 点位。
- `FsuStatusDetailView.vue` 设备文案调整为采集设备。
- `AlarmCenterView.vue` 普通主表“设备 / 点位”改为“设备 / 测点”，详情“点位编码”改为“测点编码”。

验证结果:

- `cd frontend && npm run build`：通过。
- `vue-tsc --noEmit`：随 build 执行，通过。
- `vite build`：通过。
- `frontend/package.json` 无独立 `type-check` / `lint` 脚本。
- 仍有既有 `@vueuse/core` Rollup 注释 warning 和 chunk size warning。

安全边界:

- 未访问真实 FSU，未执行 SET，未启 Scheduler。
- 未修改后端协议解析、DataScope、raw XML、run-once 或 SET 权限。

遗留:

- `displayPointName` 仍是历史兼容字段，后续应迁移到 `displaySignalName`。
- 建议为 `monitorAdapters.ts` 补前端单元测试。
- FSU 详情页后续可新增“测点概览”Tab。

输出:

- [审计报告](../audit/FE-REALTIME-POINT-SEMANTIC-FIX-001-realtime-point-device-signal-semantics.md)
- [后续 TODO](../tasks/FE-REALTIME-POINT-SEMANTIC-FIX-001-follow-up-todo.md)
- [工程记忆](2026-06-03-FE-REALTIME-POINT-SEMANTIC-FIX-001-realtime-point-device-signal-semantics.md)

## 上一任务：DATA-MAPPING-NAME-FIX-001 设备/点位名称未确认诊断与修复 (2026-06-02)

本次目标是排查并修复普通实时数据和告警页面仍显示“设备名称未确认 / 点位名称未确认”的问题。结论为：问题不是协议资料不足，而是后端映射结果没有完整透传到 DTO，以及前端 `monitorAdapters` 对缺名称数据过早降级为待确认。

本次修改:

- `DeviceSignalCandidateRepository` 增加按 DeviceID / DeviceCode 查询候选设备的方法。
- `EStoneIIMappingService` 候选映射命中但 Signal 字典暂缺时使用候选表 `signalName`；未知点位和 B接口2016 fallback 可按 DeviceID / DeviceCode 返回设备名，但未知 SignalId 仍保持 `UNMAPPED`。
- `BInterfaceFrontendDtos` 为 `AlarmDto` 增加 `deviceName`，为 `RealtimePointDto` 增加 `pointName`。
- `RealtimeDataService` 透传 `deviceId/deviceCode/deviceName`，并读取 `monitoring_point` 作为历史实时数据的点位名、单位、类型兜底；保留 `0407107001` 空单位，供前端显示单位待确认。
- `AlarmRecordService` 和 `BInterfaceFrontendReadController` 告警 DTO 透传 `deviceName`。
- `frontend/src/utils/monitorAdapters.ts` 普通表优先显示后端业务名称；历史数据缺名称时显示“历史设备/历史点位”；真正未知显示“待确认/待映射”；不再把 DeviceID/DeviceCode 当普通设备名称展示。
- 新增 `AlarmRecordServiceNameMappingTest`，补强 `EStoneIIMappingServiceTest` 和 `RealtimeDataServiceMappingClassificationTest`。

验证结果:

- `cd backend && mvn -q -DskipTests compile`：通过。
- `cd backend && mvn test -Dtest='EStoneIIMappingServiceTest,RealtimeDataServiceMappingClassificationTest,AlarmRecordServiceNameMappingTest'`：14 tests，0 failures，0 errors。
- `cd backend && mvn test -Dtest='*Mapping*Test,*Template*Test,*Realtime*Test,*Alarm*Test'`：188 tests，0 failures，0 errors。
- `cd backend && mvn test -Dtest='*Security*Test,*DataScope*Test'`：20 tests，0 failures，0 errors。
- `cd frontend && npm run build`：通过，仅有既有 `@vueuse/core` Rollup 注释 warning 和 chunk size warning。

安全边界:

- 未访问真实 FSU，未执行 SET，未启 Scheduler。
- 未放松 DataScope、raw XML、run-once 或 SET 权限。
- 未引入 StoneIII、`511600xx`、`ExtendField4`。

遗留:

- 历史 `realtime_data` 没有 DeviceID 字段，仍需 `DATA-MAPPING-BACKFILL-P1-001` 做只读/幂等回填。
- 长期应让 `realtime_data` 保留 DeviceID/DeviceCode，确保 `DeviceID + SignalId` 映射链路稳定。

输出:

- [审计报告](../audit/DATA-MAPPING-NAME-FIX-001-device-point-name-diagnosis-and-fix.md)
- [后续 TODO](../tasks/DATA-MAPPING-NAME-FIX-001-follow-up-todo.md)
- [工程记忆](2026-06-02-DATA-MAPPING-NAME-FIX-001-device-point-name-diagnosis-and-fix.md)

## 上一任务：FE-MONITOR-UX-P1-001 监控与告警页面信息收敛 (2026-06-02)

本次目标是收敛普通业务前端的数据监控与告警展示，建立统一监控状态模型和统一数据适配器，避免实时数据、告警中心、驾驶舱、FSU 详情各自解释后端字段。普通主视图只展示业务判断和操作所需信息，协议字段、映射字段、raw 类字段下沉到详情技术信息或内部诊断页面。

本次修改:

- 新增 `frontend/src/utils/monitorState.ts`，定义 `UnifiedDataState = normal/warning/alarm/offline/empty/stale/legacy/unmapped/parse_error/api_error/permission_denied`，并提供标签、Badge 状态、Metric 状态和异常判断。
- 新增 `frontend/src/utils/monitorAdapters.ts`，统一 `extractApiRows`、实时点归一化、实时摘要、告警归一化、告警摘要和 FSU 设备归一化。
- `RealtimeDataView.vue` 主表收敛为设备名称、点位名称、当前值/状态、单位、业务状态、采集时间；移除主表 FSU、点位类型、质量、映射状态等技术/排障列。
- `AlarmCenterView.vue` 主表收敛为告警等级、告警名称、设备/点位、告警状态、发生时间、恢复时间、操作入口；SPID/SignalID/EventID/EventSeverity/映射置信度进入详情“技术信息”折叠区。
- `DashboardView.vue` 移除 B接口报文总数和今日调用记录，改为实时数据状态、历史待回填、真实未映射的业务数据状态摘要。
- `FsuStatusDetailView.vue` 设备主表展示设备名称、业务状态、最近发现；DeviceID/DeviceCode/source/映射状态进入技术信息折叠区。
- `DataStateAlert.vue` 扩展统一业务状态提示，并把未映射提示改为联系平台管理员处理，不再指向普通菜单中已隐藏的点位映射页。

安全边界:

- 未访问真实 FSU，未执行 SET，未启 Scheduler。
- 未修改后端协议、权限、DataScope、raw XML、run-once 或 SET 安全门。
- 未恢复协议诊断、点位治理、系统审计普通入口。

验证结果:

- `cd frontend && npm run build`：通过。
- `vue-tsc --noEmit`：通过。
- `vite build`：通过。
- 仍有既有 `@vueuse/core` Rollup pure annotation warning 和 chunk size warning。

后续:

- P1：FSU 详情页补实时数据和告警记录 Tab。
- P1：站点详情页承载站点维度实时/告警二级交互。
- P1：后端补稳定 `eventSeverityLabel/mappingReason`。

输出:

- [审计报告](../audit/FE-MONITOR-UX-P1-001-monitor-and-alarm-ux-convergence.md)
- [后续 TODO](../tasks/FE-MONITOR-UX-P1-001-follow-up-todo.md)
- [工程记忆](2026-06-02-FE-MONITOR-UX-P1-001-monitor-and-alarm-ux-convergence.md)

## 上一任务：FE-ALARM-MAPPING-FIX-001 告警码表前端映射修复 (2026-06-02)

用户反馈告警码表在前端未正确映射。复查确认后端告警 API 已返回 `eventName/alarmMeaning/eventSeverity/mappingStatus/mappingConfidence` 等字段，主要问题是前端仍在告警中心、驾驶舱和 B接口告警页按旧 `alarmLevel/alarmDesc` 字段做主展示、筛选和统计。

本次修改:

- 新增 `frontend/src/utils/alarmDisplay.ts`，统一 `normalizeAlarmLevel`、`normalizeAlarmStatus`、`normalizeAlarmRow`，优先使用后端 `eventSeverity/eventName/alarmMeaning`，兼容 `CRITICAL/MAJOR/MINOR/WARN/INFO`、`URGENT/IMPORTANT`、中文等级和数字码。
- `AlarmLevelTag.vue` 改为使用统一 normalizer，未知等级降级为“待确认/原值”并保留样式。
- `AlarmCenterView.vue` 主表、详情抽屉、等级筛选、状态筛选和统计全部使用 `displayAlarm*` 字段；详情保留原始 `AlarmLevel`，新增 `EventSeverity`。
- `DashboardView.vue` 最新告警和告警等级分布改为同一套告警归一化逻辑。
- `BInterfaceAlarmView.vue` 诊断页接入同一套逻辑，避免和普通告警中心口径不一致。
- `status.css` 补充未知告警等级标签样式。

安全边界:

- 未访问真实 FSU，未执行 SET，未启 Scheduler。
- 未修改后端协议、权限、DataScope、raw XML、run-once 或 SET 安全门。
- 未新增任何控制入口。

验证结果:

- `cd frontend && npm run build`：通过。
- `vue-tsc --noEmit`：通过。
- `vite build`：通过。
- 仍有既有 `@vueuse/core` Rollup pure annotation warning 和 chunk size warning。

后续:

- P1：后端输出稳定 `eventSeverityLabel` 或等价字段，减少前端对数字等级码的兼容解释。
- P1：告警 DTO 补充 `mappingReason/unmappedReason`，便于直接展示 `UNKNOWN_EVENT_ID`。

输出:

- [审计报告](../audit/FE-ALARM-MAPPING-FIX-001-alarm-dictionary-frontend-mapping.md)
- [后续 TODO](../tasks/FE-ALARM-MAPPING-FIX-001-follow-up-todo.md)
- [工程记忆](2026-06-02-FE-ALARM-MAPPING-FIX-001-alarm-dictionary-frontend-mapping.md)

## 上一任务：FE-UI-STYLE-REFIT-P1-001 浅色物联网平台视觉改造 (2026-06-01)

本次参考用户提供的浅色物联网平台 UI 风格，将前端从偏工程调试后台风格调整为浅色、轻量、卡片化的动环监控平台风格。仅修改前端 UI、布局、样式和普通业务页面展示，不改后端业务逻辑、协议逻辑、DataScope、routeGuard 或 SET 安全门。

本次修改:

- `tokens.css` / `theme.css` / `layout.css` / `status.css`：改为 `#2F80ED` 蓝色主色、浅蓝选中态、浅灰内容区、白色卡片和轻量表格/标签/筛选区；清理旧 CSS `//` 注释 warning。
- `BasicLayout.vue`：Sidebar 改白色，Header 改白色，菜单选中为浅蓝底蓝字，平台名保留“机房动环监控平台”。
- `PageHeader.vue`、`MetricCard.vue`、`FsuOnlineBadge.vue`、`StatusBadge.vue`：统一轻量组件视觉；修复 `StatusBadge` 默认中文状态标签未展示的问题。
- `FsuStatusView.vue`：FSU 管理由表格改为卡片网格，展示 FSU 编码、站点、在线/心跳、最近登录/心跳、设备数、告警数和更新时间；通信记录按钮仍仅在 `protocol:raw:view` 权限可用时显示。
- `RealtimeDataView.vue`：保留真实未映射/历史待回填/解析异常统计，主表弱化 `mappingConfidence/source/templateVariant` 等技术字段，只展示 FSU、设备、点位名称、当前值、单位、类型、质量、状态和采集时间。
- `DashboardView.vue`：改为浅色卡片式驾驶舱，增加历史待回填、站点摘要和异常 FSU 摘要。
- `AlarmCenterView.vue`：告警主表移除置信度主列，协议字段继续下沉到详情抽屉。

菜单和安全边界:

- 普通菜单仍保持监控中心、站点监控、三方授权、系统设置四组。
- 未恢复资产与点位、设备管理、机柜管理、点位字典、点位映射、未映射点位、系统审计、协议诊断、raw XML、run-once 普通入口。
- raw XML 隐藏路由仍要求 `protocol:raw:view` + elevated 角色。
- run-once 隐藏路由仍要求 `protocol:runonce:readonly` + elevated 角色。
- 未访问真实 FSU，未启 Scheduler，未新增 SET 入口。

验证结果:

- `cd frontend && npm run build`：通过。
- `vue-tsc --noEmit`：通过。
- `vite build`：通过。
- 旧 CSS `//` 注释 warning 已消除；仍有 `@vueuse/core` Rollup pure annotation warning 和既有 chunk size warning。
- 未运行后端测试，原因是本次未改后端、路由权限、route guard、DataScope 或安全拦截逻辑。

下一步:

- `FE-UI-STYLE-P1-001`: FSU 详情页补设备、机柜、点位概览 Tab。
- `FE-AUTH-P1-001`: 站点授权、FSU 授权接入真实后端 API。
- `FE-DASHBOARD-CHART-P2-001`: 驾驶舱补充图表和趋势。

输出:

- [审计报告](../audit/FE-UI-STYLE-REFIT-P1-001-light-iot-platform-style.md)
- [后续 TODO](../tasks/FE-UI-STYLE-REFIT-P1-001-follow-up-todo.md)
- [工程记忆](2026-06-01-FE-UI-STYLE-REFIT-P1-001-light-iot-platform-style.md)

## 上一任务：FE-IA-CLEANUP-P1-001 普通业务菜单收敛 (2026-06-01)

根据最新产品口径，普通业务前端菜单已收敛为:

- 监控中心: 监控主页、站点实时数据、告警中心
- 站点监控: 站点列表、FSU 管理
- 三方授权: 用户管理、角色管理、权限管理、站点授权、FSU 授权
- 系统设置: 安全设置

本次修改:

- `BasicLayout.vue` 删除普通菜单中的资产与点位、设备管理、机柜管理、点位字典、点位映射、未映射点位、协议诊断和系统审计入口。
- `router/index.ts` 保留内部页面路由，但点位治理、设备/机柜、协议诊断路由增加 elevated/admin 角色限制；三方授权路由增加管理角色限制。
- `user.ts` 新增 `isTenantAdmin`，并对齐 `admin / super_admin / platform_admin` 管理角色口径；增加新旧权限码别名兼容。
- `routeGuard.ts` 改用 `userStore.hasPermission()`，兼容后端 `fsu:view/realtime:view/alarm:view` 和旧 `binterface.*.read`。
- 新增 `SiteAuthorizationView.vue`、`FsuAuthorizationView.vue`，补齐三方授权菜单结构，当前为只读空态。

安全边界:

- 未删除后端字典、映射、unmapped observation、审计、DataScope、raw XML、run-once 或 SET 阻断能力。
- raw XML 仍要求 `protocol:raw:view` 与 elevated 角色。
- run-once 仍要求 `protocol:runonce:readonly` 与 elevated 角色。
- FSU 详情页不再因页面级 run-once 权限阻塞普通查看，但 run-once 区块仍由 `PermissionGuard` 控制。
- 未访问真实 FSU，未启 Scheduler，未新增 SET 入口。

验证结果:

- `mvn -q -DskipTests compile` 在项目根目录失败，原因是根目录无 `pom.xml`。
- `cd backend && mvn -q -DskipTests compile`：通过。
- `cd backend && mvn test -Dtest='*Security*Test,*DataScope*Test'`：20 tests 通过。
- `cd frontend && npm run build`：通过，有既有 Rollup/CSS/chunk warning。

下一步:

- `FE-IA-CLEANUP-P1-002`: 将设备、机柜、点位概览并入 FSU 详情页。
- `FE-AUTH-P1-001`: 站点授权、FSU 授权接入真实后端 API。
- `FE-INTERNAL-TOOLS-P1-001`: 规划内部工具入口，仅 elevated/admin 角色可见。

输出:

- [审计报告](../audit/FE-IA-CLEANUP-P1-001-business-menu-cleanup.md)
- [后续 TODO](../tasks/FE-IA-CLEANUP-P1-001-follow-up-todo.md)
- [工程记忆](2026-06-01-FE-IA-CLEANUP-P1-001-business-menu-cleanup.md)

## 最新任务：MAPPING-OBSERVATION-CLASSIFY-FIX-001 设备/点位观测分类修复 (2026-06-01)

根据用户截图排查前端仍显示大量“未映射”的问题，已完成分类收敛和最小修复，未发现新增 P0。

关键结论:

- 截图中的 `GET_LOGININFO` 记录是设备发现记录，无 `SignalID/SPID/rawId`，不应计为真正未映射点位。
- 截图中的 `FSU-1 / FSU-2`、设备为空、来源 `UNKNOWN` 的实时数据是历史 seed/demo/legacy 数据，应归类为历史待回填。
- `0407102001 / 0407107001` 已在代码链路通过 B接口2016 标准索引 fallback 进入 `MAPPED_CANDIDATE` 候选映射，不应计为真正 `UNMAPPED`。
- 运行中的本地后端曾返回旧 DTO，说明页面可能命中未重启后端或旧前端 bundle；部署后需复查。

本次修改:

- 后端 DTO 增加 `hasSignalIdentity/isDeviceOnly/legacyData/observationType`。
- `UnmappedSignalObservationService` 不再把新的 `GET_LOGININFO` 无测点身份记录写成真正 unmapped observation。
- `RealtimeDataService` 将旧实时数据归为 `HISTORICAL_PENDING_BACKFILL`。
- `BInterfaceFrontendReadController` 查询层重分类历史 observation，区分 `DEVICE_ONLY`、`SIGNAL_PENDING`、`UNMAPPED_SIGNAL` 和映射命中 observation。
- 前端 `mappingStatus.ts` 增加 `DEVICE_ONLY`、`SIGNAL_PENDING`、`WAIT_GET_DATA`、`HISTORICAL_PENDING_BACKFILL`，并用 `isTrueUnmappedPoint` 收紧统计口径。
- 实时数据页、未映射页、点位映射页、B接口实时页分别调整统计，设备记录、候选映射、模板待确认和历史旧数据不再污染真实未映射数量。

验证结果:

- `mvn -q -DskipTests compile`：通过。
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'`：193 tests 通过。
- `mvn test -Dtest='UnmappedSignalObservationServiceTest,BInterfaceFrontendMappingClassificationTest,RealtimeDataServiceMappingClassificationTest'`：7 tests 通过。
- `mvn test -Dtest='*SetCommandSafety*Test,*SetCommandSafetyGateTest'`：39 tests 通过。
- `cd frontend && npm run build`：通过，有既有 Rollup/CSS/chunk warning。

下一步:

- 重启后端并重新部署前端 bundle，确认页面命中新 DTO。
- 进入 `DATA-MAPPING-BACKFILL-P1-001`，对历史 realtime / alarm / unmapped observation 做只读/幂等回填。
- 继续 `FE-BINTERFACE-REALTIME-CLEANUP-P1-001`，清理 B接口实时页默认 FSU / 分组兼容展示。

输出:

- [审计报告](../audit/MAPPING-OBSERVATION-CLASSIFY-FIX-001-device-signal-observation-classification.md)
- [后续 TODO](../tasks/MAPPING-OBSERVATION-CLASSIFY-FIX-001-follow-up-todo.md)
- [工程记忆](2026-06-01-MAPPING-OBSERVATION-CLASSIFY-FIX-001-device-signal-observation-classification.md)

## 最新任务：DATA-MAPPING-DICTIONARY-P1-VERIFY-001 B接口2016标准信号索引兼容映射复验 (2026-06-01)

复验 `DATA-MAPPING-DICTIONARY-P1-001` 已完成，结论为通过，未发现新增 P0。B接口2016 标准信号索引 fallback 正确接入，且未破坏 eStoneII-IO 主字典优先级、前端状态展示、DataScope、安全边界和 unmapped observation 行为。

关键结论:

- `0407102001` 已进入 `MAPPED_CANDIDATE` / `LOW` / `BINTERFACE_2016_STANDARD` 候选链路，名称 `总电压`，单位 `V`。
- `0407107001` 已进入 `MAPPED_CANDIDATE` / `LOW` / `BINTERFACE_2016_STANDARD` 候选链路，名称 `后半组电压`，单位待确认，`0.0000` 正常保留。
- `needRealDataConfirm=true` 保留，未把这两个点位写成真实完全确认。
- eStoneII-IO 主字典仍优先，B接口2016 标准索引只在 eStoneII 未命中后 fallback。
- `MAPPED_CANDIDATE` 已被前端识别为候选映射，不计入真正未映射。
- DataScope 历史风险路径未回退：`devices`、`login-info`、`unmapped-signals?fsuCode=...`。
- SET 安全门测试通过，Controls 仍为禁用参考。

本次小修:

- `BInterface2016GetDataService` / `SendDataService`：若映射服务返回候选映射且 `reason=null`，即使本地 `monitoring_point` 未绑定，也不再写入 `unmapped_signal_observation`。
- 新增 GET_DATA / SEND_DATA observation 防回退测试。
- 删除前端旧 SignalID 白名单，避免 `0407102001` / `0407107001` 在前端兼容层残留。
- 实时数据表格和 B接口点位卡片增加后端 `source` / `templateVariant` 展示。

验证结果:

- `mvn -q -DskipTests compile`：通过。
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'`：189 tests 通过。
- `mvn test -Dtest='BInterface2016GetDataServiceTest,SendDataServiceTest'`：34 tests 通过。
- `mvn test -Dtest='*SetCommandSafety*Test,*SetCommandSafetyGateTest'`：39 tests 通过。
- `cd frontend && npm run build`：通过，有既有 Rollup/CSS/chunk warning。

下一步:

- 进入 `DATA-MAPPING-BACKFILL-P1-001`，对历史 realtime / alarm / unmapped observation 做只读回填。
- 另行安排 `FE-BINTERFACE-REALTIME-CLEANUP-P1-001` 清理 B接口实时页默认 FSU / 分组兼容逻辑。

## 最新任务：DATA-MAPPING-DICTIONARY-P1-001 B接口2016标准信号索引兼容映射 (2026-06-01)

本次在不改变 DATA-MAPPING-P0-002 eStoneII-IO 主字典口径的前提下，补充 B接口2016 标准信号索引兼容映射。

执行边界:

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改真实联调参数。
- 未新增前端硬编码。
- 未引入 StoneIII、`511600xx`、`ExtendField4`。
- 未放松 DataScope、权限或 SET 安全门。

查证结论:

- `0407102001`: 项目内标准 2016 索引确认为 D类机房/蓄电池组/总电压，单位 `V`。
- `0407107001`: 项目内标准 2016 索引确认为 D类机房/蓄电池组/后半组电压，但原始单位列为空，本次保持单位待确认。
- eStoneII-IO `510000xxx` 仍是当前主字典；2016 标准信号索引只作为 eStoneII 未命中后的 fallback。

本次修改:

- 新增 `backend/src/main/resources/dictionary/binterface2016/standard-signal-index.csv`，仅收录本次已查证的 `0407102001/0407107001`。
- 新增 `BInterface2016StandardSignalIndexService`，从 classpath CSV 延迟加载标准索引，source 标识为 `BINTERFACE_2016_STANDARD`。
- 修改 `EStoneIIMappingService`，实时和告警映射在 eStoneII candidate/signal/event 未命中后进入标准 2016 fallback。
- fallback 返回 `mappingStatus=MAPPED_CANDIDATE`、`mappingConfidence=LOW`、`templateVariant/source=BINTERFACE_2016_STANDARD`、`needRealDataConfirm=true`。
- 补充 `EStoneIIMappingServiceTest`，覆盖 `0407102001`、`0407107001`、`0.0000` 保留、eStoneII 优先级和未知 SignalId 仍 unmapped。

验证:

- `mvn -q -DskipTests compile` 通过。
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'` 通过，189 tests, 0 failures/errors/skipped。
- `npm run build` 通过，保留既有 Rollup/CSS/chunk warning。

结论:

- 未发现新增 P0。
- `0407102001/0407107001` 已进入待确认/候选映射链路。
- 建议下一步执行 `DATA-MAPPING-BACKFILL-P1-001`，对历史实时/告警数据做只读/幂等回填。
- `FE-BINTERFACE-REALTIME-CLEANUP-P1-001` 仍需清理 B接口实时页硬编码 FSU/分组/白名单。

输出:

- [审计报告](../audit/DATA-MAPPING-DICTIONARY-P1-001-binterface2016-standard-signal-dictionary.md)
- [后续 TODO](../tasks/DATA-MAPPING-DICTIONARY-P1-001-follow-up-todo.md)
- [工程记忆](./2026-06-01-DATA-MAPPING-DICTIONARY-P1-001-binterface2016-standard-signal-dictionary.md)

## 上一任务：REAL-DATA-OBSERVE-001 真实数据映射状态只读观察 (2026-06-01)

本次只读观察当前 dev PostgreSQL 库、日志、样本和代码，不访问真实 FSU、不执行 SET、不启 Scheduler、不改业务代码。

读取与复核:

- DATA-MAPPING-P0-002 / DATA-MAPPING-P0-VERIFY-001 / FE-MAPPING-DIAG-001 audit、TODO、memory。
- `RealtimeDataService`、`AlarmRecordService`、`BInterfaceFrontendReadController`、`EStoneIIMappingService`、`UnmappedSignalObservationService`、`DataScopeService`。
- 前端 `mappingStatus.ts`、实时数据页、告警页、点位映射页、未映射页、B接口实时页。
- `openspec/protocols/binterface-2016/dictionaries/standard-signal-dictionary-index.md`。

当前库观察:

- `realtime_data` 7条、`alarm_record` 1条、`monitoring_point` 15条、`b_interface_message_log` 4695条。
- 当前 dev DB 没有 `estoneii_signal_dictionary`、`estoneii_event_dictionary`、`estoneii_control_reference`、`device_signal_candidate`、`unmapped_signal_observation` 表。
- 真实 FSU `51051243812345` 只有两条历史实时数据: `0407102001=54.2000`、`0407107001=0.0000`。
- `0407102001/0407107001` 已在 B接口2016标准信号字典索引中，但不在 DATA-MAPPING-P0-002 的 eStoneII `510000xxx` 主字典内。
- `FSU-001` 的 5条实时数据和 1条告警为 legacy/demo 编码。

结论:

- 当前剩余未映射主要来自后端真实 `UNMAPPED`，不是前端误判。
- 未发现新增 P0。
- 需要 `DATA-MAPPING-DICTIONARY-P1-001` 补充标准2016 `0407/0418` 系列字典边界。
- 需要 `DATA-MAPPING-BACKFILL-P1-001` 做历史实时/告警回填。
- 需要 `REAL-DATA-PASSIVE-OBSERVE-P1-001` 被动观察新真实上报。
- 需要 `FE-BINTERFACE-REALTIME-CLEANUP-P1-001` 清理B接口实时页硬编码 FSU/分组/白名单。

验证:

- `mvn -q -DskipTests compile` 通过。
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'` 通过，186 tests, 0 failures/errors/skipped。
- `npm run build` 通过，保留既有 Rollup/CSS/chunk warning。

输出:

- [观察报告](../audit/REAL-DATA-OBSERVE-001-real-data-mapping-observation.md)
- [后续 TODO](../tasks/REAL-DATA-OBSERVE-001-follow-up-todo.md)
- [工程记忆](./2026-06-01-REAL-DATA-OBSERVE-001-real-data-mapping-observation.md)

## 上一任务：FE-MAPPING-DIAG-001 监控页面未映射诊断 (2026-06-01)

FE-MAPPING-DIAG-001 完成，结论：**后端映射字段已返回，但部分前端页面存在显示误判，已完成最小修复。**

本次执行边界：
- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改后端业务代码。
- 仅对明确的前端显示误判做最小修复。

诊断结论：
- 后端实时、告警、B接口只读和未映射 API 已调用 eStoneII-IO 映射服务。
- `/api/telemetry/realtime`、`/api/alarms`、`/api/b-interface/realtime-points`、`/api/b-interface/alarms`、`/api/b-interface/unmapped-signals` 均能返回映射字段；未发现本次反馈相关的 DTO 漏字段或 DataScope 绕过。
- FSU 详情设备列表只识别小写 `mapped`，导致 `MAPPED_CANDIDATE` 被显示为未映射。
- B接口告警增强页仍用旧字段 `pointCode` 判断映射，导致后端已返回 `spid/signalId/mappingStatus` 时显示误判。
- 未映射点位页把 D 类假设全集候选和真实 unmapped observation 统一计为“未映射点位”，造成误导。

本次修复：
- 新增 `frontend/src/utils/mappingStatus.ts`，统一 `MAPPED/CONFIRMED/VERIFIED_BY_REAL_DATA/MAPPED_CANDIDATE/HIGH/MEDIUM/TEMPLATE_ONLY/PENDING_REAL_DATA/UNMAPPED/UNKNOWN_EVENT_ID` 展示口径。
- `TEMPLATE_ONLY` 显示为“模板存在，真实未确认”；`PENDING_REAL_DATA` 显示为“待真实数据确认”；`MAPPED_CANDIDATE/HIGH/MEDIUM` 显示为“候选映射”。
- 修复 `FsuStatusDetailView.vue`、`BInterfaceAlarmView.vue`、`BInterfaceThresholdView.vue` 的旧状态/旧字段误判。
- 调整 `UnmappedPointsView.vue` 指标为“待复核项 / 真实未映射 / 模板待确认”。
- 核心实时、告警、映射、未映射、PointCard 复用统一状态工具。

验证：
- `mvn -q -DskipTests compile` 通过。
- `mvn test -Dtest='*Mapping*Test,*Realtime*Test,*Alarm*Test,*DataScope*Test'`: 186 tests，0 failures。
- `npm run build` 通过，仅既有 Rollup/CSS/chunk size warning。

遗留：
- B接口实时页仍硬编码 FSU 和电池/环境分组，需 P1 去除。
- 告警 DTO 缺 `mappingReason/unmappedReason`，未知 EventId 在告警行内不可直接展示。
- 历史实时/告警数据可能需要只读映射回填。
- DataScope 仍为内存过滤，后续应迁移到 Repository/SQL 层。

输出：
- [诊断报告](../audit/FE-MAPPING-DIAG-001-monitoring-unmapped-diagnosis.md)
- [后续 TODO](../tasks/FE-MAPPING-DIAG-001-follow-up-todo.md)
- [工程记忆](./2026-06-01-FE-MAPPING-DIAG-001-monitoring-unmapped-diagnosis.md)

下一步：若核心实时数据页仍显示 `UNMAPPED`，按后端真实映射未命中处理，进入 REAL-DATA-OBSERVE-001 或字典补齐；同时建议优先执行 B接口实时页去硬编码和告警 mapping reason 补充。

## 最新任务：DATA-MAPPING-P0-VERIFY-001 eStoneII-IO 映射闭环复验 (2026-06-01)

DATA-MAPPING-P0-VERIFY-001 完成，结论：**DATA-MAPPING-P0-002 在本次小范围修正后通过 P0 复验。**

本次执行边界：
- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未引入 StoneIII、`511600xx`、`ExtendField4`。
- 未把 B接口2024 混入当前 eStoneII-IO / B接口2016 主线。

复验结论：
- eStoneII-IO 为当前唯一 P0 映射依据；生产代码和字典资源未发现 StoneIII、`511600xx`、`ExtendField4` 进入主流程。
- Signal 字典、Event 告警字典、Controls 禁用参考、DeviceSignalCandidate、Unmapped observation 基础闭环通过。
- 前端实时、告警、映射、未映射、B接口实时页面消费后端映射结果；`0.0` 正常展示；DI 未硬编码 0/1 含义；单位缺失有待确认提示。
- SET/Controls 安全边界通过，未发现 Controls 可点击入口，SET 安全门测试通过。

本次小范围修正：
- `BInterfaceFrontendReadController#getDevices` 增加 DataScope 校验，候选设备不再对空结果回退全量字典。
- `/login-info` 不再返回固定默认 FSU，改为从当前 DataScope 可见 FSU 取值。
- `EStoneIIMappingService#resolveAlarm` 保留显式未知 EventId，并返回 `UNKNOWN_EVENT_ID` reason。
- `SendAlarmService` 对 `mapped.reason()!=null` 写入 unmapped observation。
- `UnmappedSignalObservationService` 聚合键增加 `rawId`，避免同一 Signal 下不同未知 EventId 被合并。
- `UnmappedSignalDto` 和前端未映射页补充 `rawId/rawName`。

验证：
- `mvn -q -DskipTests compile` 最终通过。
- `mvn -q test -Dtest='com.dcim.platform.mapping.*Test'`: 10 tests，0 failures。
- `mvn test -Dtest='*Security*Test,*DataScope*Test'`: 20 tests，0 failures。
- `mvn test -Dtest='*Template*Test,*Mapping*Test,*Realtime*Test,*Alarm*Test'`: 177 tests，0 failures。
- `mvn test -Dtest='*CommandHandlerTest,*SetCommandSafety*Test,*SetCommandSafetyGateTest'`: 201 tests，0 failures。
- `npm run build` 通过，仅既有 CSS 注释、Rollup PURE 注释和 chunk size warning。

遗留：
- `unmapped_signal_observation` 缺独立 `eventId` 列，当前以 `rawId` 承载未知 EventId。
- DataScope 仍是内存过滤，后续应迁移到 Repository/SQL 层。
- 字典表缺显式 migration。
- 候选映射和未映射观测缺 `tenantId/stationId`。
- `frontend/src/compat/realtimeSignalFilter.ts` 仍保留临时 FSU 常量。
- B接口实时页设备分组仍需后端驱动。

输出：
- [复验报告](../audit/DATA-MAPPING-P0-VERIFY-001-estoneii-mapping-review.md)
- [后续 TODO](../tasks/DATA-MAPPING-P0-VERIFY-001-follow-up-todo.md)
- [工程记忆](./2026-06-01-DATA-MAPPING-P0-VERIFY-001-estoneii-mapping-review.md)

下一步：可以进入 FE-P1 页面建设和三方授权页面规划；建议优先补 SQL 层 DataScope、显式 migration、tenant/station 字段和映射/未映射 API 拆分。

## 上一任务：DATA-MAPPING-P0-002 eStoneII-IO 标准码表导入与实时/告警映射闭环 (2026-05-31)

DATA-MAPPING-P0-002 完成，结论：**eStoneII-IO 标准码表已导入，实时数据与告警已具备基于 DeviceID + SignalId/SPID 的候选映射能力；未真实返回的模板点位仍按 `TEMPLATE_ONLY` 或 `PENDING_REAL_DATA` 管理。**

本次执行边界：
- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未引入 StoneIII、`511600xx`、`ExtendField4`。
- 未把 B接口2024 混入当前 2016 主线。
- Controls 只作为禁用参考，前端无可点击控制入口。

实现摘要：
- 新增 `backend/src/main/resources/dictionary/estoneii/**`，落地 eStoneII-IO 默认/有中点/无中点模板 XML 和 23 条候选映射 CSV。
- 新增 mapping 模块: Signal/Event/Control/Candidate/Unmapped 5 类实体、仓库和服务。
- Signal 字典: WITH_MIDPOINT 39，NO_MIDPOINT 37；独立解析每个 Signal 的 `SignalMeanings`。
- Event 字典: WITH_MIDPOINT 13，NO_MIDPOINT 11；告警支持 SPID/EventId/SignalId 多路径映射。
- Controls: 4 条入库为 `enabledForControl=false`、`controlAccess=disabled`、`source=template_reference_only`。
- Candidate: HIGH 4、MEDIUM 6、PENDING_REAL_DATA 13；HIGH 为 `MAPPED_CANDIDATE`，其余保留真实数据确认。
- 实时/告警 API 返回 `signalName/eventName/unit/valueMeaning/alarmMeaning/eventSeverity/mappingStatus/mappingConfidence/templateVariant/needRealDataConfirm`。
- `SendDataService`、`BInterface2016GetDataService`、`SendAlarmService` 未匹配时写入 unmapped observation。
- 修正 `/api/b-interface/unmapped-signals?fsuCode=...` 参数路径 DataScope 绕过风险：先 DataScope，再按参数过滤。
- 前端实时、告警、映射、未映射、B接口实时页消费后端映射结果，不再硬编码 SignalName/EventName/单位/0-1 含义。

验证：
- `mvn -q -DskipTests compile` 通过。
- `mvn -q test -Dtest='com.dcim.platform.mapping.*Test'`: 8 tests，0 failures。
- `mvn test -Dtest='*Security*Test,*DataScope*Test'`: 20 tests，0 failures。
- `mvn test -Dtest='*Template*Test,*Mapping*Test,*Realtime*Test,*Alarm*Test'`: 176 tests，0 failures。
- `mvn test -Dtest='*CommandHandlerTest,*SetCommandSafety*Test,*SetCommandSafetyGateTest'`: 201 tests，0 failures。
- `npm run build` 通过，仅既有 CSS 注释和 chunk size 警告。

遗留：
- 当前 schema 依赖 Hibernate `ddl-auto=update`，生产化前需补显式 migration。
- DataScope 仍为内存过滤最小闭环，后续应迁移到 Repository/SQL 层。
- `VERIFIED_BY_REAL_DATA` 写回流程未完整实现。
- 候选映射和未映射观测缺 `tenantId/stationId` 字段。
- 未映射 API 后续应拆分候选映射和真实未映射观测。

输出：
- [审计报告](../audit/DATA-MAPPING-P0-002-estoneii-dictionary-import-and-mapping.md)
- [后续 TODO](../tasks/DATA-MAPPING-P0-002-follow-up-todo.md)
- [工程记忆](./2026-05-31-DATA-MAPPING-P0-002-estoneii-dictionary-import-and-mapping.md)

## 最新任务：AUTH-MATRIX-VERIFY-002 后端 P0 修复后权限矩阵复验 (2026-05-29)

AUTH-MATRIX-VERIFY-002 完成，结论：**未通过，仍存在 P0**。本次只做复验和文档输出，未修改业务代码，未访问真实 FSU，未执行 SET，未启 Scheduler。

通过项：
- `SecurityContextFilter` + `AuthInterceptor` + `@RequirePermission` 基础链路存在。
- 真实 HTTP 401/403 已由 `ResponseEntity` 保证。
- adminLike 对 raw/run-once/SET 敏感权限的绕过已受限，仅 `super_admin` 可绕过。
- run-once 端点要求 `protocol:runonce:readonly`，当前 REST 未暴露 SET。
- SET 安全门默认拒绝仍成立。
- 前端 raw/run-once guard、实时状态口径、`0.0` 展示、DI 不硬编码和未知单位提示通过。

P0 遗留：
- raw XML 下载端点未设置 `requireAll=true`，`protocol:raw:download` 未形成独立强制条件。
- message-log/call-record 按 ID 查看和下载未做 `fsuScope` 校验。
- `/api/b-interface` 聚合读取 Controller 仍直接 `findAll()`，绕过 DataScope。
- 多个业务资源 `getById()` 可绕过列表过滤直接返回实体。
- `HistoryDataService`、`DeviceHeartbeatService`、`FsuDeviceService` 等仍存在未过滤全量查询。
- `site:view/fsu:view/user:view/role:view` 类级权限覆盖 POST/PUT/DELETE，查看权限可执行写操作。

验证：
- 指定后端测试：217 tests，0 failures。
- `npm run build`：通过。
- 全量 `mvn test`：1450 tests，4 failures，24 skipped；4 个失败集中在 `CommandResultTest` 和 `FsuServiceRpcAdapterTest`，属于协议 ResultCode/SOAP ENC 旧问题，不是本次鉴权修复直接失败。
- 缺少用户要求的 `AuthHttpStatusTest`、`AdminLikeBypassTest`、`DataScopeIntegrationTest`、`AuditLogWiringTest`。

结论：后端 API 权限矩阵、raw XML、三方数据隔离、AuditLog 最小闭环均未通过；run-once 保护面通过；SET 仍被拒绝。P0 全部闭合前，不进入 FE-P1 页面建设，不进入三方授权页面实现；可仅做字段模型草案。

## 上一任务：BE-AUTH-P0-FIX-001 后端鉴权与数据隔离 P0 闭环修复 (2026-05-29)

BE-AUTH-P0-FIX-001 完成。AUTH-MATRIX-VERIFY-001 发现的 4 项 P0 全部闭合:
- FIX-001: 21 Controller @RequirePermission 全量核查通过, 3 白名单排除
- FIX-002: GlobalExceptionHandler → ResponseEntity 显式 HTTP 401/403
- FIX-005: canBypassPermissionCheck 仅 super_admin 可绕过敏感权限
- FIX-003: 6 Service DataScope 接入 + TokenStore→SecurityContextFilter scope 链路
- FIX-004: 5 类审计事件全部接入 (raw XML view/download, run-once, SET blocked, permission denied)
- 测试: 217 security tests 0 failures, 162 CommandHandler 0 failures (此前 6 failures 修复), 全量 1450 tests (4 预存协议 failures)
- 本次新增修改 8 生产文件 + 1 测试重建 + 5 测试适配

建议进入 AUTH-MATRIX-VERIFY-002 复验。P0 闭合，复验后可决策 FE-P1 页面建设。

## 上一任务：AUTH-MATRIX-VERIFY-001 前后端权限矩阵与三方授权安全闭环验证 (2026-05-29)

AUTH-MATRIX-VERIFY-001 完成。前后端权限矩阵联合验证结论：**5/8 通过，4 项 P0 遗留，安全闭环尚未成立，不可进入三方授权生产化。**

通过项：
- 前端权限矩阵: routeGuard + PermissionGuard + 菜单过滤三层一致，permissions=all-of, roles=any-of, AND
- run-once 保护: 前后端权限一致，仅 GET_FSUINFO + GET_DATA，无 SET
- SET 命令拒绝: 安全门禁 + 前端 PermissionGuard + run-once 不含 SET
- 前端状态口径: 9 态精确分类，meta 缺失优雅降级
- 测试: 55 tests 0/0/0 (Security + Safety + ReadOnly + RunOnce)，npm run build 通过

P0 遗留 (5 项):
- BE-AUTH-P0-001-FIX-001: 9 Controller 未加 @RequirePermission 默认放行
- BE-AUTH-P0-001-FIX-002: GlobalExceptionHandler 返回 HTTP 200 而非 401/403
- BE-AUTH-P0-001-FIX-003: DataScopeService 零调用点，数据隔离不成立
- BE-AUTH-P0-001-FIX-004: raw XML/run-once/SET 审计未接入 AuditLogService
- BE-AUTH-P0-001-FIX-005: isAdminLike() 允许 admin 绕过 raw/run-once

执行顺序: FIX-002 → FIX-001 → FIX-005 → FIX-003 → FIX-004
完成后执行 AUTH-MATRIX-VERIFY-002 重新验证。
P0 全部闭合前，不进入 FE-P1 页面建设和三方授权页面规划。

## 上一任务：CODEX-BE-AUTH-P0-001 后端鉴权、权限点、数据隔离和审计复审 (2026-05-29)

复审 `BE-AUTH-P0-001` 完成。本次只写审计报告和 memory，未修改业务代码，未访问真实 FSU，未执行 SET，未启 Scheduler。

结论：`BE-AUTH-P0-001` 未通过复审。

已完成基础骨架：
- `SecurityContextFilter` 可提取 Bearer token 并建立 `RequestContext`，请求结束后清理 ThreadLocal。
- `AuthInterceptor` 可识别类级/方法级 `@RequirePermission`。
- raw XML、run-once、用户/角色、告警、实时、站点、FSU 部分 Controller 已加权限注解。
- SET 安全门、只读白名单、run-once probe 定向测试通过。
- `AuditLogEntity` / `AuditLogService` 已存在。

P0 遗留：
- 未加 `@RequirePermission` 的敏感 Controller 默认放行，包括 FSU 设备、点位、机柜、历史遥测、心跳、协议命令、session、FTP 记录、registration context、active-alarm-audit。
- `GlobalExceptionHandler` 当前返回 `ApiResponse.fail(401/403, ...)`，没有真实 HTTP 401/403 状态码保证。
- `DataScopeService` 没有业务调用点，Token 不含 `tenantId/stationScope/fsuScope`，三方数据隔离不成立。
- raw XML 下载权限只定义未执行，raw XML 查看/下载无 scope 过滤和审计。
- `admin` 被 `isAdminLike()` 视为通用绕过角色，和种子数据中 admin 不含 raw/run-once 的语义冲突。
- `AuditLogService.logRawXmlAccess/logRawXmlDownload/logRunOnce/logSetCommandBlocked` 无调用点。

验证：
- `mvn test -Dtest=SecurityInfrastructureTest`：9 tests，0 failures。
- `mvn test -Dtest='SecurityInfrastructureTest,BInterface2016SetCommandSafetyTest,ReadOnlyIntegrationSafetyTest,BInterface2016ReadOnlyRunOnceProbeIntegrationTest'`：55 tests，0 failures。
- `mvn test -Dtest='*CommandHandlerTest'`：162 tests，6 failures，失败集中在 Heartbeat/SEND_DATA/SEND_ALARM 成功路径断言；因此未继续跑全量 `mvn test`。

输出：
- [复审报告](../audit/CODEX-BE-AUTH-P0-001-backend-auth-review.md)
- [工程记忆](./2026-05-29-CODEX-BE-AUTH-P0-001-backend-auth-review.md)

下一步：优先做 `BE-AUTH-P0-001-FIX-001`，修默认放行、真实 HTTP 401/403、DataScope 接入、raw 下载权限、审计接线和 adminLike 绕过。FE/BE 权限矩阵联合验证和 FE-P1 页面建设应在后端 P0 修复后进行。

## 上一任务：FE-P0-RECTIFY-002 前端P0状态口径与权限语义小修 (2026-05-29)

FE-P0-RECTIFY-002 完成。CODEX-FE-P0-RECTIFY-001 复审遗留的 5 项 P0 问题全部闭合：
- 状态口径: 9 种精确分类 (api_404/network_error/http_401/http_403/http_5xx/empty/fsu_ack_empty/fsu_no_ack/parse_error/unmapped)
- 实时 meta: 10 字段补齐 (realDeviceAccessed/ackReceived/emptyData/valuesReturned/unmappedCount/lastCollectTime/lastGetDataTime/parseError/errorCode/errorMessage)
- 权限语义: permissions=all-of, roles=any-of, 两者=AND。2 路由+1 PermissionGuard 受影响
- dist 产物: 保留 (项目规范为提交 dist)
- 验证: `npm run build` 通过 (0 errors)

未访问真实 FSU，未执行 SET，未启 Scheduler，未改 B接口2016 主线。

遗留：
- BE-AUTH-P0-001 后端鉴权闭环已完成初版，但已被 CODEX-BE-AUTH-P0-001 复审判定仍有 P0 遗留，不能视为生产闭环
- 后端 `/b-interface/realtime-points` 需返回 meta 字段前端才能真实驱动
- 建议进入 AUTH-MATRIX-VERIFY-001 权限矩阵验证

## 上一任务：BE-AUTH-P0-001 后端鉴权、权限点、数据隔离和操作审计最小闭环 (2026-05-29)

完成后端最小安全闭环，不引入Spring Security，不修改entity。采用Filter(SecurityContextFilter提取Bearer token→TokenStore→RequestContext) + Interceptor(AuthInterceptor检查@RequirePermission注解) + DataScopeService(内存scope过滤) + AuditLogEntity(JPA持久化审计)。10个Controller加@RequirePermission保护，6角色种子数据。新增12文件+修改12文件+1测试类(9 tests)。105 tests 0/0/0 (含96回归)。未访问真实FSU，未启Scheduler，未执行SET。详细见 [审计报告](../audit/BE-AUTH-P0-001-backend-auth-data-scope-audit.md)。

## 上一任务：CODEX-FE-P0-RECTIFY-001 前端P0权限与状态口径复审 (2026-05-29)

复审 `FE-P0-RECTIFY-001` 完成。本次只写审计报告和 memory，未修改前后端业务代码，未访问真实 FSU，未执行 SET，未启 Scheduler。

结论：`FE-P0-RECTIFY-001` 未完全通过复审。

通过项：
- `npm run build` 通过。
- 业务路由基本补齐 `requiresAuth`。
- raw XML 路由和按钮增加前端权限控制。
- run-once 占位区块增加 `protocol:runonce:readonly`。
- 普通实时数据页没有调用真实 GET_DATA probe。
- 8 个 SignalID 白名单集中到 `compat/realtimeSignalFilter.ts`。
- `PointCard` 仍正确展示 `0.0`，DI 未硬编码 0/1，未知单位显示“单位待确认”。
- 未发现 SET 类可点击执行入口。

P0 遗留：
- 后端鉴权、数据范围和 raw XML API 保护仍缺失，是三方授权生产阻塞项。
- 多处“后端/接口未接入”误报仍存在：`FsuStatusDetailView.vue`、`BInterfaceSchedulerView.vue`、`BInterfaceThresholdView.vue`、`api/auth.ts` 等。
- 实时数据对 API 404/500 没有明确区分“API 不存在/后端未接入”，仍泛化为 `LOAD_FAILED`。

P1 遗留：
- `routeGuard` 和 `PermissionGuard` 多权限数组使用 OR 语义，不能表达“同时需要”。
- `/b-interface/realtime-points` 后端未返回 `realDeviceAccessed/ackReceived/valuesReturned/emptyData/parseError/unmappedCount` meta，前端 ACK 空数据等状态无法真实驱动。
- `compat/realtimeSignalFilter.ts` 仍含 `fsuId === 2`，只可作为临时兼容。
- `role.read` 与 `role.manage` 权限点口径不一致。

验证：
- `npm run build` 通过，`vue-tsc --noEmit` 和 `vite build` 均成功。
- 前端无 `test` 脚本，未执行测试。

新增文档：
- `docs/audit/CODEX-FE-P0-RECTIFY-001-frontend-p0-review.md`
- `docs/memory/2026-05-29-CODEX-FE-P0-RECTIFY-001-frontend-p0-review.md`

下一步：立即进入 `BE-AUTH-P0-001`；暂缓 FE-P1 页面建设；补一个前端 P0 小修修正“未接入”误报和 API 状态分类。

## 最新任务：FE-P0-RECTIFY-001 前端P0安全与状态口径整改 (2026-05-29)

FE-ARCH-AUDIT-001 确认前端存在 6 项 P0 风险。本次整改 15 文件（修改 14 + 新增 1），未做大规橫重构：路由 requiresAuth 全覆盖 + raw XML/run-once 权限加固 + 菜单过滤 + 硬编码白名单 compat 封装 + 状态 6 态口径 + 3 新权限点 + 3 新角色。`npm run build` 通过 (0 errors)。详细见 [审计报告](../audit/FE-P0-RECTIFY-001-frontend-p0-security-and-status-fix.md) 和 [BE-AUTH-P0-001 TODO](../tasks/BE-AUTH-P0-001-backend-auth-and-data-scope-todo.md)。

## 上一任务：SPEC-DICT-P0-001 B接口2016字典码表全量分析 (2026-05-27)

按用户“全量分析，不可跳过、省略、精简、编造”的要求，补齐 B接口2016 主协议 docx 与设备信号字典 xlsx 的字典码表矩阵。新增 `openspec/protocols/binterface-2016/matrices/dictionary-code-matrix.md`，共 1114 条，覆盖协议常量、枚举、设备编码表、PK_Type/命令码/ACK码、LOGIN相关枚举、FTP图片规则、告警文本枚举、工程ResultCode、Emerson实测差异、xlsx说明、局站类型编码、设备/系统类型编码、设备编码、设备信号字典。新增 `unknown-dictionary-items.md`，记录 12 条待确认项，包括 EVENT_LENGTH 重复、设备编码229重复、EnumDeviceType预留范围冲突、GET_HISDATA_ACK方向冲突、ResultCode非2016标准字段、LOGIN Version/Vervion不一致、PaSCword大小写异常、TAlarm DEVICEICODE_LEN拼写异常、xlsx SignalID `0316005001` 重复。未修改 Java 生产代码、SQL schema、前端页面；未访问真实FSU；未启动Scheduler；未执行SET类命令。

补充实测：2026-05-27 20:34 按用户要求读取蓄电池电压，执行只读 `BInterface2016ReadOnlyRealFsuRunOnceTest#getData_readOnly_knownSensorStatus`。真实FSU返回 `GET_DATA_ACK / 402`、`Result=1`，蓄电池组设备 `51051240700002` 下 `TSemaphore Id=0407102001` 返回 `MeasuredVal=53.9`、`Status=0`。按字典矩阵 `SPEC-2016-DICT-XLSX-SIGNAL-0256`，该点为D类机房蓄电池组总电压，单位V。raw样本前缀：`SPEC-TEST-P0-001-GET_DATA_SENSOR_STATUS-51051243812345-20260527-203401-*`。未执行SET，未启动Scheduler，未写业务数据库。

## 上一任务：SPEC-TEST-P0-001 真实FSU只读Run-Once (2026-05-26)

按用户要求完成真实FSU只读测试：新增 `BInterface2016ReadOnlyRealFsuRunOnceTest`，默认跳过，仅 `-DrealFsuTest.enabled=true` 执行。已访问真实FSU `http://192.168.100.100:8080/services/FSUService`，执行GET_DATA、GET_LOGININFO、GET_FTP、GET_FSUINFO、GET_THRESHOLD；另补跑 `Landing015RealFsuIntegrationTest` 验证服务层主动GET_FSUINFO状态读取。未执行SET、未启动Scheduler、未写业务数据库。结果：GET_DATA 401/402 `Result=1` 空DeviceList；GET_LOGININFO 1501/1502返回SCIP和4个Device但缺`Result`；GET_FTP 1601/1602 `Result=1`且返回FTP账号字段；GET_FSUINFO 1701/1702返回CPU/MEM但`Result=0`；GET_THRESHOLD 1901/1902 `Result=1`但未返回TThreshold明细。传感器状态专项GET_DATA显式Id查询返回8个TSemaphore，MeasuredVal均为0或0.0，因缺厂家点位表不能可靠标注电压/电流/温湿度名称。Emerson差异登记为 `[SPEC-2016-PROFILE-EMERSON-004]`、`[SPEC-2016-PROFILE-EMERSON-005]`、`[SPEC-2016-PROFILE-EMERSON-006]`，不得覆盖标准2016 `SUCCESS=1`。

# 工作记忆 — 当前会话状态

> 最后更新：2026-05-27 (SPEC-DICT-P0-001: B接口2016字典码表全量分析)
> 目的：会话重启后快速恢复上下文

---

## 当前阶段

**当前阶段：B接口前端运营观察控制台基本闭合。** 前端 ~90-93%, 后端 ~84-87%, 项目整体 ~85-88%. 测试基线：**1297 tests, 0/0/9**。Auth/权限系统前后端闭环。B接口8+5页全部真实API接入。阻塞：DeviceID→SPID/SignalID映射表缺失。下一阶段：DATA-MAPPING-001 点位映射规划。

### CODEX-AUDIT-2016-FULL-001 结论摘要

| 优先级 | 结论 |
|------|------|
| P0 | GET_DATA 未支持 Emerson DeviceList/TSemaphore，SEND_ALARM 无真实样本验收，raw-samples 缺真实 SOAP 文件 |
| P1 | BInterfaceCommand2016 未覆盖 15 命令+ACK，BInterfacePkType 缺 LOGOUT/SET_LOGININFO，存在 2024 alias 误用风险 |
| P2 | LOGOUT、GET_HISDATA、SET_LOGININFO 未实现；SET_POINT、SET_FTP 仅 stub；TIME_CHECK 未真实验证 |
| P3 | 项目内 docs/PROJECT_ENGINEERING_RULES.md 缺失，SPEC-2016 memory 索引指向不存在文件 |
| 安全 | SET/Scheduler/real-call 默认关闭，未发现本次需立即修改代码的安全事故；`TIME_CHECK` 被 legacy SET 名称集合包含需后续澄清 |

### 真实 FSU 联调结论 (截至 LANDING-008)

| 假说 | 验证结果 |
|------|----------|
| PK_Type 格式不匹配 | ❌ 排除 — structured/legacy 行为一致 |
| FSUID/FSUCode 不被识别 | ❌ 排除 — 51051243812345 与 FSU-001 结果完全相同 |
| FSU 不支持 GET_* 出站查询 | ✅ **确认** — FSU 对所有 GET_* 命令返回空响应 |
| Code=501 路由异常 | ✅ **确认** — structured GET_DATA Code=501→SEND_ALARM |
| 2016 Code 兼容性 | ✅ **突破** — 2016 Code(GET_DATA=401/GET_LOGININFO=1501)获取真实数据: CPU 14.95%/MEM 62.84%/4 DeviceIDs |
| GET_DATA 真实 DeviceID 查询 | ✅ **完成** — 4策略空响应, FSU 当前无测量数据 |
| 被动上报接收链路 | ✅ **核对完成** — SCService 4命令链路全部就绪 |

## 项目规则

1. B接口协议是唯一设备采集/上报协议
2. 禁止重新引入 DSC/RDS
3. 每次代码操作必须先论证、再验证、后写入
4. 每次任务完成必须写入 docs/memory 工程记忆
5. Claude / Codex / AI Agent 每次启动必须加载项目规范
6. SET 类命令必须经过安全门禁、confirmationToken、审计和人工确认

---

## 真实设备联调状态

| 项目 | 值 |
|------|-----|
| IP | 192.168.100.100 |
| Port | 8080 |
| Endpoint | `/services/FSUService` |
| 完整地址 | `http://192.168.100.100:8080/services/FSUService` |
| 临时 fsuCode | FSU-001 (不等同于真实 SUID) |

### LANDING-003 (已完成)
- 6 个只读命令 HTTP 200 全部可达
- FSU 对所有 GET_* 返回空 `<invokeReturn/>`
- GET_DATA 返回 SEND_ALARM 结构 + 嵌套 XML 声明
- 24 个原始 XML 报文已保存

### LANDING-004 (已完成)
- 嵌套 XML 声明容错修复: `FsuServiceRpcAdapter.stripXmlDeclaration()`
- PK_Type 兼容模式: `FsuServiceRequest.pkTypeFormat` (structured/legacy-text)
- Structured vs Legacy 重试: 10 次真实调用, 40 个新 XML 报文
- **结论**: PK_Type 格式不是根因，FSU 不支持 GET_* 出站查询
- GET_DATA Code=501 被 FSU 路由到 SEND_ALARM (与 2024 标准 SEND_ALARM=601 不一致)

### 安全边界 (保持)
- 未执行任何 SET 类命令
- 未启用 Scheduler
- 未修改 alarm_record 状态
- 未生成正式 seed SQL
- 真实 FSU 测试默认不执行 (`@Tag("real-fsu")` + `@EnabledIfSystemProperty`)

---

## 协议基线 (PROTOCOL-BASELINE-001, 2026-05-21)

| 版本 | 定位 | 说明 |
|------|------|------|
| **B接口2016** | **当前主开发依据** | 真实 FSU 51051243812345 已验证 |
| B接口2024 | 未来升级版本 / 兼容层 | 保留实现，非当前默认依据 |

2016 关键码表: LOGIN=101, GET_DATA=401→402, GET_LOGININFO=1501→1502, GET_FSUINFO=1701→1702, GET_FTP=1601→1602, SEND_ALARM=501。

---

## 2024/2016 协议联调进度

| 阶段 | 命令/内容 | Code | 状态 |
|------|-----------|------|------|
| LANDING-003 | 真实 FSU 只读联调 | — | ✅ |
| LANDING-004 | 兼容修复 + PK_Type 重试 | — | ✅ |
| LANDING-005 | 真实 FSUID 只读重试 | — | ✅ |
| LANDING-006 | B接口2016 码表兼容重试 | 401/1501 | ✅ |
| LANDING-007 | GET_DATA 真实 DeviceID 查询 | 401/402 | ✅ |
| LANDING-008 | 被动上报接收联调准备 (文档+实施) | 101/501 | ✅ |
| OPENSPEC-001 | OpenSpec 规格驱动层接入 | — | ✅ |
| OPENSPEC-CLI-001 | OpenSpec CLI 安装与校验 | — | ✅ |
| SPEC-INIT-001 | OpenSpec + Superpowers 工作流初始化 | — | ✅ |
| LANDING-009 | alarm_record 落地字段幂等 DDL 补齐 | — | ✅ |
| LANDING-010 | BInterfaceMessageLog 日志清理与查询增强 | — | ✅ |
| LANDING-011 | FSU → SCService 被动上报真实联调 | — | ✅ (操作指引) |
| LANDING-012 | /services/SCService 标准入口 | — | ✅ |
| LANDING-013 | BInterfaceMessageLog 持久化修复 | — | ✅ |
| LANDING-013-FIX-001 | Schema/Entity command_code 对齐 | — | ✅ (真实 LOGIN 日志 id=399/400) |
| LANDING-014 | 连续上报观察与入库验证 | — | ✅ (观察指引) |
| LANDING-015 | 2016 GET_FSUINFO 心跳轮询 | Code=1701 | ✅ |

---

## 测试规模

| 阶段 | 测试数 | Failures |
|------|--------|----------|
| LANDING-003 后 | 1151 | 0 (5 skipped) |
| LANDING-004 后 | **1164** | **0** (5 skipped) |
| LANDING-012 后 | **1204** | **0** (5 skipped) |

---

## 当前阻塞

- FSU 不支持 GET_* 出站查询（所有 GET_* 返回空 DeviceList）
- FSU Code 分配表与 2024 标准不一致 (501→SEND_ALARM 而非 GET_DATA)
- 真实 SUID 未知
- 管理方未提供 DeviceID/SPID 点位表
- BInterfaceMessageLogService 待实现（报文日志）→ ✅ **已实现且真实 LOGIN 已入库**
- alarm_record 缺少 spid 列 → ✅ **已补齐 (Entity)**
- alarm_record 落地 DDL → ✅ **已补齐 (LANDING-009)**
- 父目录 `/home/tom/桌面/FSU/docs/` 存在过时规则副本
- Codex 集中复审暂缓 (累积: BIF-P4-FIX-001 ~ LANDING-008)

## 下一步建议

## 当前阻塞

TASK-002 阻塞：缺少真实 DeviceID → SPID/SignalID 映射表。

## 下一阶段：前端适配

FE-TODO-001：FSU 总览与状态页 → 后续 FE-TODO-002~011

## 后续后端任务（等待点位数据后恢复）

- POINT-MAPPING-AUDIT-001：检查点位唯一键
- 等待管理方提供 DeviceID→SPID/SignalID 映射表
- 拿到点位后恢复 TASK-002 (GET_DATA/GET_THRESHOLD 真实联调)

## OpenSpec + Superpowers 规格驱动层

OpenSpec + Superpowers 工作流已初始化（SPEC-INIT-001）。后续涉及协议/数据库/SET/Scheduler/告警/前端的任务，必须先创建 spec 和 plan，再进入编码。

### 目录结构

```
openspec/
├── README.md              ← 使用说明
├── project.md             ← 项目上下文（技术栈/协议/设备/安全配置/阶段）
├── specs/                 ← 已有能力的规格文档
│   └── landing-008-passive-reporting-readiness.md
├── plans/                 ← 执行计划文档
│   └── landing-008-passive-reporting-readiness-plan.md
├── changes/               ← 进行中的变更提案（需用户确认）
└── archive/               ← 已完成并归档的变更
```

### 四类文件对应关系

| 类型 | 位置 | 含义 | 写入时机 |
|------|------|------|----------|
| **spec** | `openspec/specs/` | 意图、边界、验收标准 | 任务开始前 |
| **plan** | `openspec/plans/` | 执行步骤、验证命令 | 任务开始前 |
| **memory** | `docs/memory/` | 执行结果、关键决策 | 任务完成后 |
| **audit** | `docs/audit/` | 审计结论、协议一致性 | 完成后或审计时 |

### 规则文件

- 最高规则：`docs/rules/CLAUDE_PROJECT_RULES.md`
- 规格驱动层规则：`docs/rules/CLAUDE_OPENSPEC_RULES.md`
- 项目上下文：`openspec/project.md`
- 强制场景：10 项（详见 CLAUDE_OPENSPEC_RULES.md 第3节）

### OpenSpec CLI

| 项目 | 值 |
|------|-----|
| CLI 版本 | **1.3.1** |
| CLI 路径 | `/home/tom/.npm-global/bin/openspec` |
| Node.js | v24.15.0 |
| 项目识别 | `openspec list` / `list --specs` 正常 |
| `openspec init` | 未执行（手动创建结构） |

---

## AI-FLOW-001-REV2 工程流系统 (2026-05-24)

6-Agent 分工模型已正式写入项目工程流系统。

### Agent 分工

| 组 | Agent | 实现者 | 职责 |
|----|-------|--------|------|
| GPT | GPT-Architect | — | 总规划/架构决策/阶段路线/任务拆分 |
| GPT | GPT-Protocol-Agent | GPT-Architect | B接口2016 协议专家 |
| Claude | Claude-Developer | — | 默认主开发/前后端/dry-run |
| Claude | Claude-Memory-Agent | Claude-Developer | 记忆/audit/WORKING-MEMORY/context-pack |
| Codex | Codex-Reviewer | — | 默认审计/代码审计/安全边界检查 |
| Codex | Codex-Deploy-Agent | Codex-Reviewer | 部署运维/Ubuntu/PostgreSQL/Nginx |

### 工程流配置文件

| 路径 | 说明 |
|------|------|
| `.agent/agents.yml` | 6 Agent 定义+权限 |
| `.agent/policies.yml` | 策略/默认Agent/所有权/禁止操作/闭环条件 |
| `.agent/workflow.yml` | 7 状态工作流 + 3 阶段门禁 |
| `.agent/locks.yml` | 任务锁管理 |
| `.agent/current-task.yml` | 当前任务追踪 |
| `.agent/tasks/DATA-MAPPING-006.yml` | DATA-MAPPING-006 预置 (PLANNED) |
| `docs/ai-team/` (10 文件) | AI Team 角色/权限/工作流/安全门禁/检查清单/模板/决策日志 |
| `tools/agentflow/agentflow.py` | **工程流 CLI MVP** — status/start/check/finish/release/pack/decision |

### agentflow.py CLI (AI-FLOW-002 + AI-FLOW-002-FIX-001, 2026-05-24)

**标准入口:** `cd /home/tom/桌面/FSU && python3 tools/agentflow/agentflow.py <command>`

**7 命令:**
- `status` — 当前任务/锁/任务列表
- `start <ID> --agent <A>` — 启动 (PLANNED/APPROVED → IN_PROGRESS) + 记录 baseline
- `check <ID>` — required_outputs/forbidden_paths/actions/memory/audit (forbidden_paths 命中 → exit 1)
- `finish <ID>` — 完成 (IN_PROGRESS → SELF_TESTED, 阻止 forbidden_paths)
- `release <ID>` — 释放锁
- `pack <ID> --for gpt` — 生成 context-pack
- `decision <ID> --from <F>` — GPT 决策 (PASS → COMPLETED)

**安全门禁 (AI-FLOW-002-FIX-001):**
- baseline 机制: start 记录 git diff 快照, check 用 current - baseline 过滤历史 dirty diff
- forbidden_paths 命中 → check 返回非 0 退出码
- finish 阻止 forbidden_paths 违规

典型使用：
```bash
cd /home/tom/桌面/FSU
python3 tools/agentflow/agentflow.py status
python3 tools/agentflow/agentflow.py start AI-FLOW-002-FIX-001 --agent Claude-Developer
python3 tools/agentflow/agentflow.py check AI-FLOW-002-FIX-001
python3 tools/agentflow/agentflow.py pack AI-FLOW-002-FIX-001 --for gpt
python3 tools/agentflow/agentflow.py finish AI-FLOW-002-FIX-001
python3 tools/agentflow/agentflow.py release AI-FLOW-002-FIX-001
```

### 关键规则

- GPT 负责规划、架构、协议判断和阶段决策，不直接改本地代码
- Claude 是默认主开发 Agent，同时负责 Memory-Agent
- Codex 是默认审计 Agent，同时负责 Deploy-Agent
- 协议专家由 GPT 扮演，部署运维由 Codex 扮演，记忆审计维护由 Claude 扮演
- 所有任务通过 `.agent/tasks/*.yml` 管理
- 任务闭环前必须更新 memory/audit/WORKING-MEMORY

## SPEC-TEST-P0-001 B接口2016 标准主命令测试建设 (2026-05-26)

本次只新增测试、fixture、audit/memory，不修改生产业务代码、SQL或前端。

新增测试覆盖：

- `BInterface2016StandardCommandMatrixTest`
- `BInterface2016ResultSemanticsTest`
- `BInterface2016XmlSampleReplayTest`
- `BInterface2016ScServiceCommandTest`
- `BInterface2016FsuServiceReadOnlyCommandTest`
- `BInterface2016SetCommandSafetyTest`
- `BInterface2016EmersonProfileCompatibilityTest`

新增 fixture：

- `backend/src/test/resources/fixtures/b_interface_2016_standard/`：18个标准2016样本
- `backend/src/test/resources/fixtures/b_interface_2016_emerson/`：Emerson GET_DATA空DeviceList样本

验证命令：

```bash
cd /home/tom/桌面/FSU/fsu-platform-java/backend
mvn test -Dtest='*BInterface2016*'
```

结果：编译通过，测试失败，`Tests run: 110, Failures: 6, Errors: 0, Skipped: 0`。

失败点为当前实现与 SPEC-P0-001 的差异：

- `PkTypeDescriptor` 用2024码表判定2016 `GET_DATA/401` 不一致
- `CommandResult.success()` 仍输出 `ResultCode=0`，标准2016应为 `Result=1`
- SCService ACK未输出 `LOGIN_ACK/102` 的标准Name+Code结构
- `GET_LOGININFO`、`GET_FTP`、`GET_THRESHOLD`、`TIME_CHECK` 出站只读服务未显式使用 `legacy-2016`
- SET安全门未识别 `SET_LOGININFO`
- `SET_THRESHOLD`、`SET_LOGININFO` 未标记高风险

安全边界：未执行SET、未启用真实FSU、未启动Scheduler。运行现有 `BInterface2016ReadOnlyRunOnceProbeIntegrationTest` 时写入了 `backend/docs/landing/raw-samples/*20260526-224049.xml`，属于既有测试副作用。

## 项目根目录

`/home/tom/桌面/FSU/fsu-platform-java/`
