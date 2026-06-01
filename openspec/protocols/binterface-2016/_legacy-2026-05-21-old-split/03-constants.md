# 03 — 常量和 ResultCode

## 1. ResultCode 定义

| ResultCode | 含义 | 说明 |
|-----------|------|------|
| 0 | SUCCESS | 成功 |
| 1 | FAILURE | 通用失败 |
| 1001 | INVALID_SESSION | 会话无效或已过期 |
| 1002 | INVALID_FSU | FSU 未注册或认证失败 |
| 2001 | INVALID_PARAMETER | 参数错误 |
| 5001 | INTERNAL_ERROR | 服务器内部错误 |

## 2. 使用规范

- 所有响应必须包含 ResultCode
- ResultCode=0 表示成功，非 0 值应附带说明
- 各命令可定义更具体的业务错误码

## 3. 平台错误码扩展

| ResultCode | 含义 | 来源 |
|-----------|------|------|
| 2003 | 无有效响应数据 | `BInterface2016GetFsuInfoService` |
| 2004 | 响应缺少 CPUUsage/MEMUsage | `BInterface2016GetFsuInfoService` |
| 2010 | ACK Code 不匹配 | `BInterface2016GetFsuInfoService` |

## 4. 会话相关常量

| 常量 | 值 | 说明 |
|------|-----|------|
| SESSION_PREFIX | `SESSION-` | 会话ID前缀 |
| DEFAULT_EXPIRE_SECONDS | 3600 | 默认会话过期时间（秒） |

## 5. 方向常量

| 常量 | 值 | 说明 |
|------|-----|------|
| DIRECTION_INBOUND | `INBOUND` | FSU→SC |
| DIRECTION_OUTBOUND | `OUTBOUND` | SC→FSU |
| MESSAGE_TYPE_SOAP | `SOAP` | SOAP报文 |

## 6. 告警常量

| 常量 | 值 | 说明 |
|------|-----|------|
| ALARM_TYPE_GENERATE | `0` | 告警产生 |
| ALARM_TYPE_RECOVER | `1` | 告警恢复 |
| ALARM_STATUS_ACTIVE | `ACTIVE` | 活跃告警 |
| ALARM_STATUS_RECOVERED | `RECOVERED` | 已恢复 |

## 7. FSU 状态常量

| 常量 | 值 | 说明 |
|------|-----|------|
| ONLINE_STATUS_ONLINE | `ONLINE` | 在线 |
| ONLINE_STATUS_OFFLINE | `OFFLINE` | 离线 |
| LOGIN_STATUS_LOGIN | `LOGIN` | 已登录 |
