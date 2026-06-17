# 05 — 设备编码表

## 1. 设备标识字段

| 字段 | 说明 | 长度建议 | 协议位置 |
|------|------|---------|---------|
| FSUCode | FSU 设备编码 | VARCHAR(64) | 所有请求 Info |
| FSUID | FSU 唯一标识 | VARCHAR(128) | LOGIN Info |
| DeviceID | 设备 ID | VARCHAR(128) | LOGIN Info, GET_DATA |
| StationName | 站名 | VARCHAR(64) | 设备配置 |

## 2. 当前真实设备

| 字段 | 值 |
|------|-----|
| FSUCode | 51051243812345 |
| FSUID | 51051243812345 |
| StationName | 1 |
| IP | 192.168.100.100 |
| MAC | 00:09:F5:E0:8D:B7 |
| Version | 21.1.HQ.FSU.WD.AA44.R |

## 3. 已知 DeviceID

| # | DeviceID |
|---|----------|
| 1 | 51051241820004 |
| 2 | 51051241830004 |
| 3 | 51051241840004 |
| 4 | 51051240700002 |
| 5 | 51051243812345 |

来源: LANDING-006 GET_LOGININFO (Code=1501) 返回。

## 4. SPID/SignalID

当前无映射数据。管理方未提供 DeviceID→SPID 映射表。monitoring_point 表为空。

## 5. 平台 Entity 映射

| 协议字段 | Entity | 表列 |
|---------|--------|------|
| FSUCode | `FsuDeviceEntity.fsuCode` | `fsu_device.fsu_code` |
| FSUID | 未独立存储 | — |
| DeviceID | `AlarmRecordEntity.deviceId` | `alarm_record.device_id` |
| SPID | `AlarmRecordEntity.spid` | `alarm_record.spid` |
