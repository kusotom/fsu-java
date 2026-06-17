# FE-REALTIME-BUSINESS-DISPLAY-FIX-001 后续任务

**日期**: 2026-06-03 | **状态**: 已完成，以下为建议

## 已完成

- [x] 删除采集设备列
- [x] 测点名称收敛为业务传感器名称
- [x] 统计卡片移除采集设备
- [x] 页面文案更新
- [x] dashboard 同步
- [x] npm run build 通过

## P2 建议

- 如果后端返回更多 SignalId → signalName 映射，`normalizeSensorName` 会自动收敛未匹配名称
- FSU/点位按 FSU 分组合并展示（rowspan/分组标题行）可作为后续 UI 增强
