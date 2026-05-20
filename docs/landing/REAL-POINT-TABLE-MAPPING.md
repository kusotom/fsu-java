# 真实点位表映射规范 — B接口 2024 附录到平台字段

## 1. 参考资料

- B接口 2024 附录 E.2: `SUConfigInstance.xml` — FSU 监控点实例配置
- B接口 2024 附录 E.3: `StdSPDic.xml` — 标准化监控点全表
- B接口 2024 附录 E.4: `StdSPConfigOptionDic.xml` — 标准化监控点配置方案全表
- B接口 2024 第 4.2 节: `TAlarm` — 告警事件结构
- 附录 E.15: `SEND_ALARM` — 告警上报 XML 样例

## 2. 三类配置文件关系

```
StdSPDic.xml
    │ 定义标准设备类型 (DeviceType) 和标准监控点 (Signal) 字典
    │ 字段: TypeID, DeviceHLType, DeviceTypeName, SPID, SPName, SPType, Unit
    │
    ↓ 关联: TypeID = TypeID, SPID = SPID

StdSPConfigOptionDic.xml
    │ 定义每个标准监控点的可选配置方案
    │ 字段: OptionID, AlarmLevel, AlarmThreshold, StartDelay, EndDelay,
    │       Period, AbsoluteVal, RelativeVal, Hysteresis
    │
    ↓ 关联: SPID = SPID, OptionID = OptionID

SUConfigInstance.xml
    │ 某个 FSU 实际采用的设备、点位和 OptionID
    │ 字段: SU.ID, Device.DeviceID/DeviceName/DeviceHLType,
    │       Signal.SPID/SPType/OptionID
    │
    ↓ 平台导入

fsu_device / device / point / alarm 表
```

## 3. SUConfigInstance → 平台映射

| XML 属性 | 平台字段 | 类型 | 说明 |
|----------|----------|------|------|
| `<SU ID="">` | `fsu_device.fsu_code` / suid | VARCHAR(64) | FSU 唯一标识 |
| `<Device DeviceID="">` | 设备表 `device_id` | VARCHAR(128) | 广义设备编码 |
| `<Device DeviceName="">` | 设备表名称字段 | VARCHAR(128) | 设备名称 |
| `<Device Description="">` | 设备表描述字段 | TEXT | 设备描述（可选） |
| `<Device DeviceHLType="">` | 设备表 `device_hl_type` | VARCHAR(8) | 设备高级类型 |
| `<Signal SPID="">` | `alarm_record.point_code` / spid | VARCHAR(64) | 标准监控点 ID |
| `<Signal SPName="">` | 点位表名称字段 | VARCHAR(128) | 监控点名称 |
| `<Signal SPType="">` | 点位表 `sp_type` | VARCHAR(8) | 监控点类型 |
| `<Signal AlarmMeanings="">` | 点位表 `alarm_meanings` | TEXT | 告警含义描述 |
| `<Signal NormalMeanings="">` | 点位表 `normal_meanings` | TEXT | 正常含义描述 |
| `<Signal Unit="">` | 点位表 `unit` | VARCHAR(32) | 单位 |
| `<Signal OptionID="">` | 点位表 `option_id` / 配置关联 | VARCHAR(16) | 配置方案 ID |

### 平台落地专属字段（协议无法提供，平台生成）

| 平台字段 | 说明 | 来源 |
|----------|------|------|
| `local_device_id` (BIGINT) | 平台自增主键 | 平台生成 |
| `local_point_id` (BIGINT) | 平台自增主键 | 平台生成 |
| `service_url` | FSU SOAP 端点 | **现场提供** |
| `enabled` | 是否启用 | 平台默认 true |
| `settable` | 是否允许控制 | **现场确认** |
| `set_safety_level` | SET 安全级别 | 平台默认 forbidden |
| `confirmation_required` | 是否需要二次确认 | 平台默认 true |

## 4. StdSPDic → 标准设备类型 / 点位字典映射

| XML 属性 | 平台含义 | 说明 |
|----------|----------|------|
| `StdSPDic.Name` | 字典名称 | 如 "XX 标准化监控点全表" |
| `StdSPDic.Version` | 字典版本 | 如 "1.0" |
| `DeviceType.TypeID` | 设备类型编码 | 如 "020" |
| `DeviceType.DeviceHLType` | 设备高级类型 | 1=交流配电, 2=低压配电, 3=机房, ... |
| `DeviceType.DeviceTypeName` | 设备类型名称 | 如 "低压配电系统" |
| `Signal.SPID` | 标准监控点 ID | 如 "230200050010" |
| `Signal.SPName` | 监控点名称 | 如 "线电压Uab" |
| `Signal.SPType` | 监控点类型 | 3=模拟量, ... |
| `Signal.AlarmMeanings` | 告警含义 | 如 "过高/过低" |
| `Signal.NormalMeanings` | 正常含义 | 如 "正常范围" |
| `Signal.Unit` | 单位 | 如 "V" / "℃" |

**注意**: StdSPDic 中 Signal 不含 OptionID。OptionID 只在 SUConfigInstance 和 StdSPConfigOptionDic 中出现。

## 5. StdSPConfigOptionDic → 门限/配置模板映射

| XML 属性 | 平台含义 | 说明 |
|----------|----------|------|
| `StdSPConfigOptionDic.Name` | 配置方案表名称 | |
| `StdSPConfigOptionDic.Version` | 配置方案表版本 | |
| `Option.OptionID` | 配置方案编号 | 关联到 SUConfigInstance.Signal.OptionID |
| `Option.AlarmLevel` | 告警级别 | URGENT / IMPORTANT / WARN / INFO |
| `Option.AlarmThreshold` | 告警门限 | 如 "420.0" |
| `Option.StartDelay` | 告警产生延时 秒 | |
| `Option.EndDelay` | 告警结束延时 秒 | |
| `Option.Period` | 存储周期 秒 | |
| `Option.AbsoluteVal` | 变化绝对阈值 | |
| `Option.RelativeVal` | 变化百分比阈值 | |
| `Option.Hysteresis` | 回差 | |

### GET_SPCONFIGOPTION 查询规则（协议原文）

- **DeviceID 填全 9**：返回该 FSU 全部设备的所有监控点配置模板
- **SPID 填全 9**：返回某 DeviceID 下全部监控点的配置模板
- **两者均非全 9**：返回指定 DeviceID + SPID 的单个配置模板

## 6. TAlarm → alarm_record 映射

| TAlarm XML 元素 | alarm_record 字段 | 说明 |
|-----------------|-------------------|------|
| `<SerialNo>` | `serial_no` VARCHAR(128) | **告警实例标识**，非静态点位表字段。由 FSU 稳定生成 |
| `<SUID>` | `fsu_device.fsu_code` | FSU / 站点标识 |
| `<DeviceID>` | `device_id` VARCHAR(128) | 广义设备编码 |
| `<SPID>` | `point_code` VARCHAR(64) | 监控点 ID |
| `<StartTime>` | `occur_time` TIMESTAMPTZ | 告警发生时间 |
| `<EndTime>` | `clear_time` TIMESTAMPTZ | 告警结束时间（恢复时写入） |
| `<TriggerVal>` | `alarm_value` VARCHAR(64) | 告警触发值 |
| `<AlarmLevel>` | `alarm_level` VARCHAR(16) | 告警级别 |
| `<AlarmFlag>` | `alarm_status` VARCHAR(16) | BEGIN→ACTIVE, END→RECOVERED |
| `<AlarmDesc>` | `alarm_desc` TEXT | 告警标准文本 (≤ 200 字节) |
| `<AlarmFriDesc>` | 可存入 alarm_desc | 告警详细描述 (≤ 200 字节) |

**关键区分**：
- `SerialNo` 是**告警实例字段**，每个告警事件一个唯一值，不属于静态点位表。
- 静态点位表（SUConfigInstance）不包含 SerialNo。SerialNo 由 FSU 在 SEND_ALARM / GET_ACTIVEALARM 时动态生成。

## 7. 活动告警匹配规则（LANDING-001 已实现）

```
优先级 1: SerialNo 精确匹配
    FSU 告警.SerialNo == alarm_record.serial_no → matched

优先级 2: DeviceID + SPID + AlarmFlag 组合匹配
    FSU 告警.DeviceID + SPID + AlarmFlag == alarm_record.device_id + point_code + alarm_status
    → matched（降级）

优先级 3: 旧逻辑兼容
    当 serial_no 和 device_id 都为空时，使用原始 pointCode+alarmCode+alarmStatus 匹配
```

## 8. 字段来源分类

### 8.1 协议能确定（字段结构、XML 标签、属性名、配置关系）

- SUID 字段名、XML `<SU ID="">` 结构
- DeviceID、DeviceName、Description、DeviceHLType XML 属性
- SPID、SPName、SPType、AlarmMeanings、NormalMeanings、Unit 属性
- OptionID、AlarmLevel、AlarmThreshold、StartDelay、EndDelay、Period、AbsoluteVal、RelativeVal、Hysteresis
- TAlarm 完整字段结构
- TConfigOption Type/SPID/OptionID 结构
- GET_SPCONFIGOPTION 全 9 查询规则

### 8.2 现场必须提供（真实值）

- 真实 SUID（FSU 标识）
- 真实 DeviceID（每个设备的实际编号）
- 真实 SPID（每个监控点的实际编号）
- 真实 FSU serviceUrl（`http://{ip}:{port}/services/FSUService`）
- 真实设备名称（DeviceName）
- 真实点位名称（SPName）
- 真实配置方案 OptionID
- 真实门限参数（AlarmThreshold 等）
- 哪些点位可以 SET（settable）
- 是否允许 SET_POINT / SET_THRESHOLD / SET_SCIP
- 协议版本确认（B-2016 / B-2024）

### 8.3 平台生成（落地字段）

- localDeviceId（数据库自增主键）
- localPointId（数据库自增主键）
- enabled（默认 true）
- setSafetyLevel（默认 forbidden）
- confirmationRequired（默认 true）
- alarmEnabled（默认 true）
- created_at / updated_at（自动时间戳）
