# FE-REALTIME-TABLE-FINAL-FIX-001 后续任务

**日期**: 2026-06-03 | **状态**: 已完成

## 已完成

- [x] FSU 列 span-method 合并相同 FSU
- [x] 删除单位列和业务状态列
- [x] displayValueWithUnit 合并值+单位+状态
- [x] normalizeSensorName 增加 signalId/pointCode fallback
- [x] 异常测点排除 legacy 历史数据
- [x] npm run build 通过

## P2 建议

- 如果后端 EStoneII 映射返回更多 signalName，前端的 normalizeSensorName 会自动收敛未匹配的名称
- 分页功能可在数据量大时添加
