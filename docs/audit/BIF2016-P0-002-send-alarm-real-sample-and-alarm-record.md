# BIF2016-P0-002 SEND_ALARM 真实告警接收验证

## 1. 日期
2026-05-21

## 2. 修改

SendAlarmService 增强:
- 2016 字段解析: AlarmFlag, DeviceCode, ID(→spid备选), FSUID
- ID→SignalID 备选 (2016 TAlarm 用 ID 替代 SignalID)
- AlarmFlag 作为 AlarmType 备选恢复判断
- DeviceCode 写入 alarmDesc 补充

## 3. 样本回放测试

SendAlarm2016SampleTest: 8 tests
- 单条/多条 TAlarm, 缺 SerialNo, AlarmFlag 恢复, SPID 兼容, 未映射

## 4. ACK 约定

SCService 入站 ACK 使用同名 PK_Type=SEND_ALARM（非 SEND_ALARM_ACK）。这是 SCService 协议约定。

## 5. 测试

1287 tests, 0/0/9.

## 6. 真实现场

SEND_ALARM 未触发。平台样本回放已闭环，真实验收待现场告警条件具备。
