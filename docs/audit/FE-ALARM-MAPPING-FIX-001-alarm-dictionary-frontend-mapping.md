# FE-ALARM-MAPPING-FIX-001 告警码表前端映射修复

## 1. 背景

用户反馈“告警码表在前端没有正确映射，而且前端逻辑还有点问题”。本次只检查和修复前端展示逻辑，不修改后端业务、协议解析、DataScope、SET 安全门或 Scheduler。

## 2. 问题定位

后端 `/api/alarms` 与 `/api/b-interface/alarms` 已返回告警字典映射字段:

- `eventName`
- `alarmMeaning`
- `eventSeverity`
- `mappingStatus`
- `mappingConfidence`
- `templateVariant`
- `needRealDataConfirm`

前端问题主要在:

- `AlarmCenterView.vue` 主表、筛选、统计仍主要按 `alarmLevel` 判断，未把 `eventSeverity` 纳入告警码表展示口径。
- `AlarmLevelTag.vue` 只识别 `CRITICAL/MAJOR/MINOR/WARN`，不兼容 `URGENT/IMPORTANT`、中文等级、数字码等码表值。
- 驾驶舱最新告警优先使用 `alarmDesc/alarmName`，未优先展示后端 `eventName/alarmMeaning`。
- B接口告警增强页仍使用 `URGENT/IMPORTANT` 旧展示口径，和业务告警中心不一致。

## 3. 修改范围

- 新增 `frontend/src/utils/alarmDisplay.ts`
- 修改 `frontend/src/components/common/AlarmLevelTag.vue`
- 修改 `frontend/src/styles/status.css`
- 修改 `frontend/src/views/alarm/AlarmCenterView.vue`
- 修改 `frontend/src/views/dashboard/DashboardView.vue`
- 修改 `frontend/src/views/binterface/BInterfaceAlarmView.vue`

## 4. 修复内容

新增统一告警展示 normalizer:

- `normalizeAlarmLevel(alarmLevel, eventSeverity)`：优先使用后端事件字典字段 `eventSeverity`，兼容 `CRITICAL/MAJOR/MINOR/WARN/INFO`、`URGENT/IMPORTANT`、中文等级和数字码。
- `normalizeAlarmStatus(status, alarmFlag)`：兼容 `ACTIVE/NEW/CONFIRMED/RECOVERED/CLEARED/CLOSED/BEGIN/END/0/1` 等状态。
- `normalizeAlarmRow(row)`：统一生成 `displayAlarmName/displayAlarmMeaning/displayAlarmLevel/displayAlarmStatus/displayOccurTime/displayRecoverTime/displayAlarmValue`。

页面修复:

- 告警中心主表优先展示 `eventName/alarmMeaning`，等级使用归一化标签。
- 告警中心筛选和统计改为使用 `displayAlarmLevel/displayAlarmStatus`。
- 告警详情保留原始 `AlarmLevel`，并新增展示 `EventSeverity`，便于协议排障。
- 驾驶舱最新告警和等级分布改为消费同一套归一化逻辑。
- B接口告警增强页接入同一套归一化逻辑，避免内部诊断页和业务页口径不一致。
- `AlarmLevelTag` 支持未知等级降级展示，不再把码表外值显示为空或无样式。

## 5. 安全边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改后端权限、DataScope、raw XML、run-once 或 SET 安全门。
- 未新增任何控制入口。
- 未在前端硬编码具体告警点位名称、单位或 0/1 含义。

## 6. 验证结果

执行:

```bash
cd frontend
npm run build
```

结果:

- `vue-tsc --noEmit` 通过。
- `vite build` 通过。
- 仍有既有 `@vueuse/core` Rollup pure annotation warning 和 chunk size warning。
- 未运行后端测试，原因是本次只改前端展示逻辑，未改后端、权限、DataScope 或协议执行链路。

## 7. 遗留问题

- eStoneII 模板原始 `EventSeverity` 可能是数字码；当前前端做展示兼容，但建议后端后续统一输出稳定中文等级字段，避免多个前端入口重复解释。
- 告警 DTO 仍建议补充 `mappingReason/unmappedReason`，让 `UNKNOWN_EVENT_ID` 在告警行中更直接可见。

## 8. 结论

FE-ALARM-MAPPING-FIX-001 完成。前端告警中心、监控驾驶舱和 B接口告警增强页已统一消费后端告警字典映射结果，告警名称、含义、等级、状态、时间和值展示已收敛到统一 normalizer；未修改后端协议和安全边界，npm build 通过。
