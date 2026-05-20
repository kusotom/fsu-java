# 真实点位表接收检查清单

## 1. 文件完整性检查

| # | 检查项 | 要求 | 通过 |
|---|--------|------|------|
| 1.1 | SUConfigInstance.xml | FSU 监控点实例配置表已提供 | [ ] |
| 1.2 | StdSPDic.xml | 标准化监控点全表已提供（或说明版本） | [ ] |
| 1.3 | StdSPConfigOptionDic.xml | 标准化监控点配置方案全表已提供（或说明版本） | [ ] |
| 1.4 | FSU serviceUrl | 每个 FSU 的真实 SOAP 端点已提供 | [ ] |
| 1.5 | 协议版本说明 | 明确标注使用的 B接口版本（2016 / 2024） | [ ] |
| 1.6 | 配置文件格式 | XML 格式正确，可被标准 XML 解析器读取 | [ ] |

## 2. 必填字段检查

| # | 检查项 | 要求 | 通过 |
|---|--------|------|------|
| 2.1 | SU ID | `<SU ID="">` 不为空，每个 FSU 有唯一标识 | [ ] |
| 2.2 | DeviceID | `<Device DeviceID="">` 不为空 | [ ] |
| 2.3 | SPID | `<Signal SPID="">` 不为空 | [ ] |
| 2.4 | SPName | `<Signal SPName="">` 不为空 | [ ] |
| 2.5 | SPType | `<Signal SPType="">` 不为空 | [ ] |
| 2.6 | OptionID | `<Signal OptionID="">` 不为空（关联配置方案） | [ ] |
| 2.7 | 唯一性 | DeviceID + SPID 在同一 SU ID 下唯一 | [ ] |
| 2.8 | DeviceHLType | 设备高级类型可识别 | [ ] |

## 3. 告警字段检查

| # | 检查项 | 要求 | 通过 |
|---|--------|------|------|
| 3.1 | AlarmMeanings | 告警点位有 AlarmMeanings 或能映射告警含义 | [ ] |
| 3.2 | AlarmLevel 映射 | 告警点位能映射到 AlarmLevel（URGENT/IMPORTANT/WARN/INFO） | [ ] |
| 3.3 | AlarmFlag 定义 | BEGIN=告警产生, END=告警恢复 语义明确 | [ ] |
| 3.4 | TAlarm.SerialNo | 明确 SerialNo 由 FSU 稳定生成，每个告警实例唯一 | [ ] |
| 3.5 | 告警门限参考 | 有 ConfigOption.AlarmThreshold 或现场门限值 | [ ] |

## 4. SET 安全检查

| # | 检查项 | 要求 | 通过 |
|---|--------|------|------|
| 4.1 | 可控点位清单 | 明确哪些点位可被 SET（遥控遥调） | [ ] |
| 4.2 | SET_POINT 授权 | 是否允许 SET_POINT，需现场书面授权 | [ ] |
| 4.3 | SET_THRESHOLD 授权 | 是否允许 SET_THRESHOLD，需现场书面授权 | [ ] |
| 4.4 | SET_SCIP 授权 | 是否允许 SET_SCIP，需现场书面授权 | [ ] |
| 4.5 | 默认安全策略 | setSafetyLevel=forbidden（仅授权后才放行） | [ ] |
| 4.6 | 二次确认 | confirmationRequired=true（默认要求确认） | [ ] |

## 5. seed SQL 生成前检查

| # | 检查项 | 要求 | 通过 |
|---|--------|------|------|
| 5.1 | 目标数据库确认 | 明确是 dev / staging / production 环境 | [ ] |
| 5.2 | 不覆盖生产数据 | 确认 seed SQL 不会覆盖已有生产告警数据 | [ ] |
| 5.3 | 字段映射确认 | 按 REAL-POINT-TABLE-MAPPING.md 映射，字段名对齐 | [ ] |
| 5.4 | nullable 策略 | 协议能提供的字段不可空，平台生成字段允许为空 | [ ] |
| 5.5 | 历史数据兼容 | 旧数据 serial_no/device_id 为空不报错 | [ ] |
| 5.6 | serviceUrl 范围 | 确认 serviceUrl 是否区分内外网 | [ ] |
| 5.7 | 事务边界 | 大批量导入需确认事务策略 | [ ] |

## 6. 真实联调前检查

| # | 检查项 | 要求 | 通过 |
|---|--------|------|------|
| 6.1 | serviceUrl 可达 | 从 SC 服务器能 ping/telnet FSU IP:port | [ ] |
| 6.2 | real-call-enabled | 仍默认 false，联调时显式开启单 FSU | [ ] |
| 6.3 | Scheduler 禁止 | active-alarm-audit.scheduler-enabled=false（联调期不走定时） | [ ] |
| 6.4 | SET 禁止 | 联调期 SET 默认 forbidden（手动审批后逐条放行） | [ ] |
| 6.5 | 单 FSU 先行 | 先对一个 FSU 做只读联调（GET_DATA/GET_ACTIVEALARM） | [ ] |
| 6.6 | 日志开启 | 联调期间 BInterface 报文日志开启 | [ ] |
| 6.7 | 回滚方案 | 如联调异常，有方案回退到 Stub 模式 | [ ] |

## 7. 验收签字

| 角色 | 检查项 | 签字 | 日期 |
|------|--------|------|------|
| FSU 管理方 | 提供完整配置文件 | ________ | ____ |
| 平台开发 | 字段映射确认 | ________ | ____ |
| 现场运维 | serviceUrl / SET 授权确认 | ________ | ____ |
| 安全审核 | SET 安全策略确认 | ________ | ____ |
