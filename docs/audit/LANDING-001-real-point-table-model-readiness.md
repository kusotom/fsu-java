# LANDING-001: 真实点位表接入前的数据模型补强

## 1. 时间

2026-05-20

## 2. 策略变更

用户已放弃单纯 Demo 阶段。当前转向项目真实落地，为 FSU 管理方提供的真实 SUID/DeviceID/SPID 点位表做准备。

## 3. Codex 复审状态

**Codex 复审暂缓。**

- BIF-P4-FIX-001 / BIF-P4-021 / BIF-P4-022 / BIF-P4-023 Codex 复审暂缓；
- 本阶段基于 Claude 当前结果继续推进；
- 后续仍需 Codex 集中复审。

## 4. 目标

补齐 `alarm_record` 的 `serialNo`/`deviceId` 字段和 `fsu_device` 的 `serviceUrl` 字段，为后续接入 FSU 管理方真实点位表做准备。

## 5. 边界

| 边界 | 说明 |
|------|------|
| 不访问真实 FSU | 不访问 192.168.100.100 |
| 不启用 Scheduler | 默认 false |
| 不执行 SET | 不调用任何 SET_ 命令 |
| 不改变状态机 | alarm_record 告警新增/恢复/确认逻辑不变 |
| 不做 Demo | 无 mock 数据/页面/脚本 |

## 6. 数据模型补强

### 6.1 AlarmRecordEntity

| 新增字段 | 列名 | 类型 | 说明 |
|----------|------|------|------|
| serialNo | serial_no | VARCHAR(128) NULL | B接口告警 SerialNo |
| deviceId | device_id | VARCHAR(128) NULL | B接口 DeviceID |

### 6.2 FsuDeviceEntity

| 新增字段 | 列名 | 类型 | 说明 |
|----------|------|------|------|
| serviceUrl | service_url | VARCHAR(512) NULL | FSU SOAP 端点地址 |

### 6.3 AlarmRecordRepository

新增查询方法：
- `findByFsuIdAndSerialNo` — SerialNo 精确匹配
- `findByFsuIdAndDeviceIdAndPointCodeAndAlarmStatus` — DeviceID+SPID 匹配
- `findByFsuIdAndAlarmStatus` — FSU 活动告警查询

## 7. 匹配逻辑增强

### LocalActiveAlarmSnapshotService
- `toSnapshot()` 从 `entity.getSerialNo()` / `entity.getDeviceId()` 读取真值
- 旧数据字段为 null 时兼容：降级到原有匹配逻辑

### SendAlarmService
- `parseAlarmItem()` 新增解析 SerialNo/DeviceID/SPID
- `buildAlarmEntity()` 写入 serialNo/deviceId 到实体
- `processOneAlarm()` 恢复路径兼容补写字段（仅 null 时回填）

### ActiveAlarmDiffService
- 无需修改：已支持 serialNo 优先 + deviceId/spid 降级

## 8. SQL Schema

```sql
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS serial_no VARCHAR(128);
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS device_id VARCHAR(128);
ALTER TABLE fsu_device ADD COLUMN IF NOT EXISTS service_url VARCHAR(512);
```

## 9. 安全边界确认

- [x] 不访问真实 FSU
- [x] 不启用 Scheduler
- [x] 不执行 SET
- [x] 不改变 alarm_record 状态机
- [x] 新增字段 nullable 兼容旧数据
- [x] 无 Demo 内容

## 10. 测试基线

- 全量: 1151 tests, 0 failures, 0 errors, 5 skipped
- LANDING-001 新增: 20 tests
- 相关测试: 250 tests (Landing001 + ActiveAlarm + SendAlarm + BInterface) 全部通过

## 11. 修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `alarm/entity/AlarmRecordEntity.java` | 修改 | 新增 serialNo, deviceId |
| `resource/entity/FsuDeviceEntity.java` | 修改 | 新增 serviceUrl |
| `alarm/repository/AlarmRecordRepository.java` | 修改 | 新增 3 个查询方法 |
| `binterface/service/LocalActiveAlarmSnapshotService.java` | 修改 | toSnapshot 读取真值 |
| `binterface/service/SendAlarmService.java` | 修改 | 解析+写入 SerialNo/DeviceID/SPID |
| `database/schema/001_init_schema.sql` | 修改 | ALTER TABLE 新增列 |
| `test/.../Landing001ModelReadinessTest.java` | 新增 | 20 个测试 |
| `test/.../BInterfaceMainFlowIntegrationTest.java` | 修改 | Stub 补方法 |
| `test/.../SendAlarmServiceTest.java` | 修改 | Stub 补方法 |
