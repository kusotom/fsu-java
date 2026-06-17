# FE-ALARM-MAPPING-FIX-001 告警码表前端映射修复

## 任务目标

修复前端告警码表展示不正确的问题，使告警中心、监控驾驶舱和 B接口告警增强页优先消费后端返回的 `eventName/alarmMeaning/eventSeverity` 等映射结果，而不是继续只按旧 `alarmLevel/alarmDesc` 字段展示。

## 审计结论摘要

后端告警映射链路已返回告警字典字段，主要问题在前端字段消费和显示口径不统一。本次完成小范围前端修复，未修改后端协议逻辑、DataScope、权限或 SET 安全边界。

## 关键问题

- `AlarmCenterView.vue` 仍按 `alarmLevel` 统计、筛选和显示告警等级。
- `AlarmLevelTag.vue` 只识别少量英文等级码，不能兼容 `URGENT/IMPORTANT`、中文等级和数字码。
- 驾驶舱最新告警未优先展示 `eventName/alarmMeaning`。
- B接口告警增强页和普通告警中心使用不同等级显示口径。

## 修改内容

- 新增 `frontend/src/utils/alarmDisplay.ts`，统一告警等级、状态、名称、含义、时间和值展示归一化。
- `AlarmLevelTag.vue` 改为使用 `normalizeAlarmLevel()`。
- `AlarmCenterView.vue` 主表、详情、筛选和统计使用归一化后的展示字段。
- `DashboardView.vue` 最新告警和告警等级分布使用同一套归一化逻辑。
- `BInterfaceAlarmView.vue` 接入同一套归一化逻辑。
- `status.css` 补充未知告警等级标签样式。

## 安全边界

- 未访问真实 FSU。
- 未执行 SET。
- 未启 Scheduler。
- 未修改后端权限、DataScope、raw XML、run-once 或 SET 安全门。
- 未新增任何控制入口。

## 验证结果

- `cd frontend && npm run build`：通过。
- `vue-tsc --noEmit`：通过。
- `vite build`：通过。
- 仍有既有 `@vueuse/core` Rollup pure annotation warning 和 chunk size warning。

## 后续优先级

1. P1：后端输出稳定 `eventSeverityLabel` 或等价字段，减少前端对数字等级码的兼容解释。
2. P1：告警 DTO 补充 `mappingReason/unmappedReason`。
3. P2：告警中心增加按映射状态和事件来源筛选。

## 输出

- [审计报告](../audit/FE-ALARM-MAPPING-FIX-001-alarm-dictionary-frontend-mapping.md)
- [后续 TODO](../tasks/FE-ALARM-MAPPING-FIX-001-follow-up-todo.md)
