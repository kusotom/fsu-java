# 数据库核心模型设计

## 概述

本文档描述机房动环监控平台（中国铁塔 B 接口 2016）的 PostgreSQL 数据库核心模型设计。

### 协议背景

- SC（Supervisory Center）与 FSU（Field Supervision Unit）通过 B 接口互联
- 数据流：WebService + SOAP + XML
- 文件传输：FTP
- 快数据通道（FSU → SC）：LOGIN、HEARTBEAT、SEND_ALARM
- 慢数据通道（SC → FSU）：GET_DATA、SET_DATA、SET_FSUREBOOT
- 报文结构：Request / Response / PK_Type / Info / xmlData

## 表分组

### 资源管理（4 表）

| 表 | 用途 | 核心字段 |
|----|------|----------|
| `site` | 站点/站址信息 | site_code, site_name, site_type, region, 经纬度 |
| `cabinet` | 机柜物理资产 | cabinet_code, cabinet_type, model, manufacturer, FK→site |
| `fsu_device` | FSU 动环采集设备 | fsu_code, fsu_type, ip_addr, protocol_version, FK→site/cabinet |
| `monitoring_point` | 监控点位定义 | point_code, point_type(AI/DI/DO/PI), unit, 告警阈值, FK→fsu_device |

### 数据采集（3 表）

| 表 | 用途 | 核心字段 |
|----|------|----------|
| `realtime_data` | 点位最新值（每点一行） | value_text/value_number, value_status, collect_time, FK→point(UK) |
| `history_data` | 点位历史采集数据 | value_text/value_number, collect_time, FK→point, UK(fsu_id,point_code,collect_time) |
| `device_heartbeat` | FSU 心跳记录 | heartbeat_time, status_info, FK→fsu_device |

### 告警管理（1 表）

| 表 | 用途 | 核心字段 |
|----|------|----------|
| `alarm_record` | 告警生命周期管理 | alarm_level(URGENT/IMPORTANT/WARN/INFO), alarm_status(ACTIVE/CONFIRMED/CLEARED/IGNORED), occur/confirm/clear_time |

### B接口核心（6 表）

| 表 | 用途 | 核心字段 |
|----|------|----------|
| `b_interface_command` | B接口命令清单与安全开关 | command_code, direction, implemented, safe_enabled |
| `b_interface_message_log` | SOAP/XML 报文日志 | direction, command_code, pk_type, info, xml_data, raw_message(TEXT) |
| `b_interface_session` | FSU 登录会话 | session_id(UK), auth_token, login/logout_time, FK→fsu_device |
| `b_interface_fsu_status` | FSU B接口在线状态 | login_status, online_status, last_heartbeat, FK→fsu_device(UK) |
| `b_interface_call_record` | SC→FSU 调用记录 | command_code, request/response_body(TEXT), duration_ms, retry_count |
| `ftp_transfer_record` | FTP 文件传输记录 | file_name, file_type, direction, transfer_status, checksum |

### 系统管理（2 表 + 1 关联表）

| 表 | 用途 | 核心字段 |
|----|------|----------|
| `user_account` | 用户账户 | username(UK), password_hash, status |
| `role` | 角色定义 | role_code(UK), role_name |
| `user_role` | 用户-角色关联 | UK(user_id, role_id) |

## 表关系图

```
site ──┬── cabinet ──┬── monitoring_point ──┬── realtime_data (UK)
       │              │                       ├── history_data
       │              │                       └── alarm_record
       │              │
       │              └── fsu_device ──┬── device_heartbeat
       │                               ├── b_interface_session
       │                               ├── b_interface_fsu_status (UK)
       │                               ├── b_interface_message_log
       │                               ├── b_interface_call_record
       │                               └── ftp_transfer_record
       │
user_account ──┬── user_role ──┬── role
               │               │
               └── alarm_record (confirm_user_id)
```

## B接口数据流与表关联

```
快数据（FSU→SC）：
  FSU 上报 LOGIN/HEARTBEAT/SEND_ALARM
    → SOAP 报文到达 SCService
    → 记录到 b_interface_message_log (raw_message TEXT)
    → LOGIN → b_interface_session
    → HEARTBEAT → device_heartbeat + b_interface_fsu_status
    → SEND_ALARM → alarm_record

慢数据（SC→FSU）：
  SC 发起 GET_DATA/SET_DATA/GET_HISTORY_DATA
    → 调用记录到 b_interface_call_record
    → 返回数据 → realtime_data (upsert) + history_data (insert)
```

## 安全设计

- `b_interface_command.safe_enabled` 控制各命令的安全开关
- `SET_FSUREBOOT` 的 `safe_enabled` 默认 `FALSE`，防止远程重启 FSU
- `password_hash` 使用 bcrypt，演示数据仅放占位字符串
- `auth_token` 在演示数据中为占位值，生产环境需 JWT/Session Token
