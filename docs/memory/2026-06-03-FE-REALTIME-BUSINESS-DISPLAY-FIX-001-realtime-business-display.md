# FE-REALTIME-BUSINESS-DISPLAY-FIX-001 站点实时数据页业务展示口径修正

## 1. 任务目标
修正站点实时数据页，去除采集设备列，将测点名称收敛为业务传感器名称（温度/湿度/电压/烟感等），调整统计口径。

## 2. 架构判断
仅前端展示层: `monitorAdapters.ts` + `RealtimeDataView.vue` + `DashboardView.vue`。不涉及后端协议解析、安全权限、数据库。

## 3. 协议一致性判断
不涉及 B接口协议。展示名收敛为纯前端行为。

## 4. 修改前论证
当前页面显示 "采集设备" 列和过于技术化的测点名称（如 I2C温度、电池1总电压），对普通业务用户无意义。需收敛为业务友好的传感器名称。

## 5. 写入前验证
- 代码审查确认 adapter 集中处理，页面不散落判断
- 告警页和协议诊断页未受影响

## 6. 实际修改文件
- `frontend/src/utils/monitorAdapters.ts` — normalizeSensorName, sensorName, 移除collectingDeviceCount
- `frontend/src/views/telemetry/RealtimeDataView.vue` — 移除采集设备列, 更新指标卡片和文案
- `frontend/src/views/dashboard/DashboardView.vue` — 移除采集设备统计行, 文案

## 7. 核心改动
- 新增 normalizeSensorName() 收敛技术名→业务名
- 表格移除采集设备列
- 指标卡片移除采集设备/真实未映射, 改为 FSU点位/实时测点/异常测点/待映射

## 8. 测试结果
npm run build 通过 (0 errors)。前端无 test 脚本。

## 9. 风险
低风险，仅前端展示变更。

## 10. 遗留问题
无。

## 11. 下一步建议
继续 DATA-MAPPING-P0-001 点位映射后端闭环。

## 12. git diff 摘要
3 文件修改: monitorAdapters.ts (+30/-5), RealtimeDataView.vue (+5/-10), DashboardView.vue (+2/-5)

## 13. git status 摘要
Modified: frontend/src/utils/monitorAdapters.ts, frontend/src/views/telemetry/RealtimeDataView.vue, frontend/src/views/dashboard/DashboardView.vue
New: docs/audit/FE-REALTIME-BUSINESS-DISPLAY-FIX-001-*.md, docs/tasks/FE-REALTIME-BUSINESS-DISPLAY-FIX-001-*.md
