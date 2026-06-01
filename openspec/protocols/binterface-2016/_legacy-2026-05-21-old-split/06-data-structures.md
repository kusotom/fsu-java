# 06 — 数据结构定义

## 1. TAlarm（告警）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| SignalID | String | 是 | 关联信号量 ID |
| AlarmCode | String | 是 | 告警代码 |
| AlarmName | String | 否 | 告警名称 |
| AlarmLevel | String | 是 | 告警级别（WARN/CRITICAL） |
| AlarmValue | String | 否 | 触发告警时的值 |
| AlarmDesc | String | 否 | 告警描述 |
| AlarmType | Integer | 否 | 0=产生, 1=恢复 |
| SerialNo | String | 否 | 告警序列号 |
| DeviceID | String | 否 | 设备 ID |
| SPID | String | 否 | 信号点 ID |

平台 Entity: `AlarmRecordEntity` → `alarm_record`

## 2. TSemaphore（信号量）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ID | String | 是 | 信号量标识 |
| Code | String | 否 | 信号量编码 |
| MeasuredVal | String | 否 | 测量值 |
| Status | String | 否 | 状态 |
| Time | DateTime | 否 | 采集时间 |

## 3. TThreshold（门限）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| SignalID | String | 是 | 信号量 ID |
| AlarmUpper | Double | 否 | 告警上限 |
| AlarmLower | Double | 否 | 告警下限 |
| AlarmUpperUrgent | Double | 否 | 严重告警上限 |
| AlarmLowerUrgent | Double | 否 | 严重告警下限 |

## 4. TFSUStatus（FSU运行状态）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| CPUUsage | Decimal | 否 | CPU 使用率（%） |
| MEMUsage | Decimal | 否 | 内存使用率（%） |

来源: LANDING-006 GET_FSUINFO_ACK 实测。协议手册 §6.13 xmlData 结构标为"待确认"。

平台使用: `BInterface2016GetFsuInfoService` 从 xmlData items 提取。

## 5. DeviceInfo（设备信息）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Manufacturer | String | 否 | 设备厂商 |
| Model | String | 否 | 设备型号 |
| FirmwareVersion | String | 否 | 固件版本 |
| HardwareVersion | String | 否 | 硬件版本 |
| MacAddr | String | 否 | MAC 地址 |
| IPAddr | String | 否 | IP 地址 |

用于 LOGIN 请求 xmlData。

## 6. LoginInfo（登录信息）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| FSUCode | String | 是 | FSU 设备编码 |
| LoginStatus | String | 是 | LOGIN/LOGOUT |
| OnlineStatus | String | 是 | ONLINE/OFFLINE |
| SessionID | String | 是 | 当前会话 ID |
| LoginTime | DateTime | 是 | 登录时间 |
| LastHeartbeat | DateTime | 否 | 最后心跳时间 |

用于 GET_LOGININFO 响应 xmlData。

## 7. FTPConfig（FTP配置）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Host | String | 是 | FTP 服务器地址 |
| Port | Integer | 是 | FTP 端口 |
| Username | String | 是 | 用户名 |
| Password | String | 否 | 密码 |
| PassiveMode | Boolean | 否 | 被动模式 |
| BasePath | String | 否 | 基础路径 |
