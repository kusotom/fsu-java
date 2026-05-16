# 数据库表清单

## 总览

| 序号 | 表名 | 中文名 | 分组 | 主键 | 外键数 | 唯一约束数 |
|------|------|--------|------|------|--------|-----------|
| 1 | `site` | 站点表 | 资源管理 | id (BIGSERIAL) | 0 | 0 |
| 2 | `cabinet` | 机柜表 | 资源管理 | id (BIGSERIAL) | 1 (site) | 1 (cabinet_code) |
| 3 | `fsu_device` | FSU设备表 | 资源管理 | id (BIGSERIAL) | 2 (site, cabinet) | 1 (fsu_code) |
| 4 | `monitoring_point` | 监控点位表 | 资源管理 | id (BIGSERIAL) | 2 (fsu_device, cabinet) | 1 (fsu_id, point_code) |
| 5 | `realtime_data` | 实时数据表 | 数据采集 | id (BIGSERIAL) | 2 (fsu_device, point) | 1 (point_id) |
| 6 | `history_data` | 历史数据表 | 数据采集 | id (BIGSERIAL) | 3 (fsu_device, point, message_log) | 1 (fsu_id, point_code, collect_time) |
| 7 | `alarm_record` | 告警记录表 | 告警管理 | id (BIGSERIAL) | 4 | 0 |
| 8 | `device_heartbeat` | 设备心跳表 | 数据采集 | id (BIGSERIAL) | 2 | 0 |
| 9 | `b_interface_command` | B接口命令定义表 | B接口 | id (BIGSERIAL) | 0 | 1 (command_code) |
| 10 | `b_interface_message_log` | B接口报文日志表 | B接口 | id (BIGSERIAL) | 1 (fsu_device) | 0 |
| 11 | `b_interface_session` | B接口会话表 | B接口 | id (BIGSERIAL) | 1 (fsu_device) | 1 (session_id) |
| 12 | `b_interface_fsu_status` | B接口FSU状态表 | B接口 | id (BIGSERIAL) | 1 (fsu_device) | 1 (fsu_id) |
| 13 | `b_interface_call_record` | SC调用记录表 | B接口 | id (BIGSERIAL) | 2 | 0 |
| 14 | `ftp_transfer_record` | FTP传输记录表 | B接口 | id (BIGSERIAL) | 1 (fsu_device) | 0 |
| 15 | `user_account` | 用户表 | 系统管理 | id (BIGSERIAL) | 0 | 2 (username, email) |
| 16 | `role` | 角色表 | 系统管理 | id (BIGSERIAL) | 0 | 1 (role_code) |
| 17 | `user_role` | 用户角色关联表 | 系统管理 | id (BIGSERIAL) | 2 (user, role) | 1 (user_id, role_id) |

**总计：17 张表，其中资源管理 4 张、数据采集 3 张、告警管理 1 张、B接口 6 张、系统管理 3 张。**

## 表列详情

### 通用字段

所有核心表包含：
- `id` BIGSERIAL PRIMARY KEY
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
- `updated_at` TIMESTAMPTZ NOT NULL DEFAULT now()（b_interface_command、history_data、device_heartbeat、user_role 除外）

### B接口表特殊说明

b_interface_message_log:
- `raw_message` TEXT — 完整 SOAP/XML 原始报文
- `info` TEXT — Info 字段内容
- `xml_data` TEXT — xmlData 字段内容
- `pk_type` VARCHAR(32) — 对应 b_interface_command.command_code

b_interface_command:
- `safe_enabled` BOOLEAN — 安全开关，SET_FSUREBOOT 默认为 FALSE
- `implemented` BOOLEAN — 实现状态跟踪

b_interface_call_record:
- `request_body` / `response_body` TEXT — SOAP 请求/响应报文
- `retry_count` INTEGER — 重试次数
- `duration_ms` INTEGER — 调用耗时

## 外键依赖顺序

```
创建顺序（无外键 → 有外键）：
  site, role, user_account, b_interface_command
    → cabinet (FK→site)
    → fsu_device (FK→site, cabinet)
    → user_role (FK→user_account, role)
    → monitoring_point (FK→fsu_device, cabinet)
    → b_interface_message_log (FK→fsu_device)
    → realtime_data (FK→fsu_device, point)
    → history_data (FK→fsu_device, point, message_log)
    → alarm_record (FK→fsu_device, point, user, message_log)
    → device_heartbeat (FK→fsu_device, message_log)
    → b_interface_session (FK→fsu_device)
    → b_interface_fsu_status (FK→fsu_device)
    → b_interface_call_record (FK→fsu_device, message_log)
    → ftp_transfer_record (FK→fsu_device)

删除顺序：与创建顺序严格相反
```
