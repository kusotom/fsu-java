# BACKEND-PROGRESS-SYNC：切换前端前后端进度同步

## 1. 本次目标

保存后端当前进度，准备切换到前端适配。

## 2. 当前整体进度

- 整体后端：82%~85%
- B接口协议主链路：约 90%
- 真实 FSU 只读联调：约 85%
- SET 安全体系：80%~85%
- 调度/运维能力：70%~75%
- 真实点位数据闭环：BLOCKED

## 3. 已完成能力

- B接口基础链路 (LOGIN/SEND_ALARM/GET_DATA/GET_THRESHOLD/GET_LOGININFO/GET_FTP/TIME_CHECK/GET_FSUINFO/GET_ACTIVEALARM)
- 真实 FSU RPC 调用适配 (FsuServiceRpcAdapter + RealHttpFsuServiceClient)
- /services/SCService 标准入口 (LANDING-012)
- BInterfaceMessageLog 报文日志 + 查询 + 清理 (LANDING-008~010)
- alarm_record serial_no/device_id/spid 补齐 (LANDING-008~009)
- 2016 全命令码表 + PK_Type 枚举边界 (BIF2016-P1-001/P1-002)
- GET_DATA 2016 DeviceList/TSemaphore (BIF2016-P0-001)
- SEND_ALARM 2016 字段解析 + 样本回放 (BIF2016-P0-002)
- SET 安全体系 (门禁+confirmationToken+审计)
- Scheduler 安全门禁框架 (realCallEnabled+allowedSuids+runOnceDryRun)

## 4. 真实 FSU 信息

- FSUCode：51051243812345
- StationName：1
- Endpoint：http://192.168.100.100:8080/services/FSUService
- 已知 DeviceID：51051241820004, 51051241830004, 51051241840004, 51051240700002, 51051243812345

## 5. 当前阻塞

TASK-002 阻塞：缺少每个 DeviceID 下的 SPID/SignalID 清单。

## 6. 关键技术结论

点位唯一维度必须是：FSUCode + DeviceID + SignalID。不能只按 SignalID 唯一。

## 7. 测试基线

1297 tests, 0 failures, 0 errors, 9 skipped

## 8. 后续后端任务

- POINT-MAPPING-AUDIT-001：检查点位唯一键
- 等待管理方提供 DeviceID→SPID/SignalID 映射表
- 拿到点位后恢复 TASK-002

## 9. 切换到前端

FE-TODO-001：FSU 总览与状态页
