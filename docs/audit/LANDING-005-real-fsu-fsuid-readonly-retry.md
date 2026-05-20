# LANDING-005: 带真实 FSUID 的只读 GET_* 重试与协议对照分析

## 审计日期
2026-05-20

## 审计类型
真实设备只读重试 (FSUID=51051243812345, structured vs legacy PK_Type)

## 1. 目标
使用管理方确认的真实 FSUID `51051243812345` (StationName=1) 替换临时 fsuCode `FSU-001`，重试 5 个只读 GET_* 命令，验证 FSUID 是否为 LANDING-003/004 空响应的根因。

## 2. 实际参数

| 参数 | 值 |
|------|-----|
| serviceUrl | `http://192.168.100.100:8080/services/FSUService` |
| FSUID | `51051243812345` |
| StationName | `1` |
| 平台临时 fsuCode | `FSU-001` (仅内部标识) |

## 3. 执行命令

| 命令 | Structured (Name+Code) | Legacy (纯文本) |
|------|----------------------|-----------------|
| GET_SUINFO | Info: `<SUID>51051243812345</SUID>` | 同 |
| GET_DATA | Info: `<FSUCode>51051243812345</FSUCode>`, Name+Code 501 | 同, 纯文本 GET_DATA |
| GET_ACTIVEALARM | Info: `<SUID>51051243812345</SUID>`, Name+Code 603 | 同, 纯文本 GET_ACTIVEALARM |
| GET_SPCONFIGOPTION | Info: `<SUID>51051243812345</SUID>`, Name+Code 401 | 同, 纯文本 GET_SPCONFIGOPTION |
| GET_THRESHOLD | Info: `<FSUCode>51051243812345</FSUCode>`, Name+Code 505 | 同, 纯文本 GET_THRESHOLD |

## 4. 结果

### 4.1 Structured 格式

| 命令 | HTTP | FSU 响应 | LANDING-004 (FSU-001) |
|------|------|----------|----------------------|
| GET_SUINFO | 200 | 空 invokeReturn | 空 invokeReturn |
| GET_DATA | 200 | **SEND_ALARM** (Code=501, 空 TAlarmList) | SEND_ALARM (相同) |
| GET_ACTIVEALARM | 200 | 空 invokeReturn | 空 invokeReturn |
| GET_SPCONFIGOPTION | 200 | 空 invokeReturn | 空 invokeReturn |
| GET_THRESHOLD | 200 | 空 invokeReturn | 空 invokeReturn |

### 4.2 Legacy 格式

| 命令 | HTTP | FSU 响应 | LANDING-004 (FSU-001) |
|------|------|----------|----------------------|
| GET_SUINFO | 200 | 空 invokeReturn | 空 invokeReturn |
| GET_DATA | 200 | 空 invokeReturn | 空 invokeReturn |
| GET_ACTIVEALARM | 200 | 空 invokeReturn | 空 invokeReturn |
| GET_SPCONFIGOPTION | 200 | 空 invokeReturn | 空 invokeReturn |
| GET_THRESHOLD | 200 | 空 invokeReturn | 空 invokeReturn |

### 4.3 响应逐字节对比

- GET_DATA structured: L-005 与 L-004 **逐字节相同**（SEND_ALARM 结构完全一致）
- 所有其他命令: L-005 响应与 L-004 **完全相同**（均为空 invokeReturn）

## 5. 解析出的字段

| 字段 | 结果 |
|------|------|
| SUID | **未获取到** |
| DeviceID | **未获取到** |
| SPID | **未获取到** |
| SPName/SPType/Unit/OptionID | **未获取到** |
| AlarmLevel/AlarmFlag/AlarmDesc | **未获取到** (TAlarmList 为空) |

## 6. 关键结论

1. **FSUID 不是根因**: 使用真实 FSUID `51051243812345` 后，FSU 响应与使用临时 `FSU-001` 完全一致
2. **FSU 不校验 SUID/FSUCode**: FSU 不根据请求中的 SUID/FSUCode 判断是否响应
3. **FSU 不支持 GET_* 主动查询**: 所有 5 个命令无论 PK_Type 格式和 FSUID 均返回空
4. **Code 501 路由固化**: structured GET_DATA (Name=GET_DATA, Code=501) 不论 FSUID 如何，FSU 都将其路由到 SEND_ALARM 处理器
5. **通信层正常**: HTTP 200, WSDL RPC/encoded 封装正确, namespace 正确

## 7. 安全边界

- [x] 未执行 SET
- [x] 未启用 Scheduler
- [x] 未修改 alarm_record
- [x] 未生成正式 seed SQL
- [x] 默认 mvn test 不访问真实 FSU

## 8. 原始报文

40 个 XML 文件: `docs/landing/raw-samples/landing005-*` (5命令×2格式×4层)
