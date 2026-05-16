# 索引与约束设计

## 唯一约束

| 表 | 约束名 | 字段 | 说明 |
|----|--------|------|------|
| cabinet | `uk_cabinet_code` | cabinet_code | 机柜编码全局唯一 |
| fsu_device | `uk_fsu_code` | fsu_code | FSU设备编码全局唯一 |
| monitoring_point | `uk_point_fsu_code` | (fsu_id, point_code) | 同一FSU下点位编码唯一 |
| realtime_data | `uk_realtime_point` | point_id | 每个点位仅一条实时记录 |
| history_data | `uk_history_duplicate` | (fsu_id, point_code, collect_time) | 防止重复采集或重放 |
| b_interface_command | `uk_bic_command_code` | command_code | 命令码唯一 |
| b_interface_session | `uk_bis_session_id` | session_id | 会话ID唯一 |
| b_interface_fsu_status | `uk_bifs_fsu` | fsu_id | 每个FSU仅一条B接口状态 |
| user_account | `uk_user_username` | username | 用户名唯一 |
| user_account | `uk_user_email` | email | 邮箱唯一 |
| role | `uk_role_code` | role_code | 角色编码唯一 |
| user_role | `uk_user_role` | (user_id, role_id) | 防止重复授权 |

## 索引

| 表 | 索引名 | 字段 | 用途 |
|----|--------|------|------|
| history_data | `idx_history_data_point_time` | (point_id, collect_time) | 按点位查询历史曲线 |
| history_data | `idx_history_data_fsu_time` | (fsu_id, collect_time) | 按FSU查询全部历史 |
| history_data | `idx_history_data_collect` | (collect_time) | 按时间范围批量查询 |
| alarm_record | `idx_alarm_record_fsu_time` | (fsu_id, occur_time) | 按FSU查询告警时间线 |
| alarm_record | `idx_alarm_record_status` | (alarm_status, occur_time) | 查询活跃/已清除告警 |
| alarm_record | `idx_alarm_record_level_time` | (alarm_level, occur_time) | 按严重等级过滤 |
| device_heartbeat | `idx_heartbeat_fsu_time` | (fsu_id, heartbeat_time) | 按FSU查询心跳历史 |
| b_interface_message_log | `idx_bmsg_fsu_time` | (fsu_id, created_at) | 按FSU查询报文日志 |
| b_interface_message_log | `idx_bmsg_command` | (command_code, created_at) | 按命令类型查询报文 |
| b_interface_message_log | `idx_bmsg_direction` | (direction, created_at) | 按方向过滤报文 |
| b_interface_message_log | `idx_bmsg_session` | (session_id) | 按会话关联报文 |
| b_interface_message_log | `idx_bmsg_status` | (status, created_at) | 按处理状态查询 |
| b_interface_session | `idx_bis_fsu_active` | (fsu_id, status) | 查询FSU当前活跃会话 |
| b_interface_call_record | `idx_bicr_fsu_time` | (fsu_id, call_time) | 按FSU查询调用历史 |
| b_interface_call_record | `idx_bicr_command` | (command_code, call_time) | 按命令查询调用历史 |
| b_interface_call_record | `idx_bicr_status` | (status, call_time) | 按状态查询调用记录 |
| ftp_transfer_record | `idx_ftp_fsu_time` | (fsu_id, transfer_time) | 按FSU查询FTP历史 |
| ftp_transfer_record | `idx_ftp_status` | (transfer_status, created_at) | 按传输状态查询 |

## CHECK 约束

| 表 | 字段 | CHECK | 说明 |
|----|------|-------|------|
| b_interface_command | direction | IN ('FSU_TO_SC', 'SC_TO_FSU') | 限制方向值 |

## 触发器

| 触发器 | 作用域 | 功能 |
|--------|--------|------|
| `trg_*_updated_at` | 所有含 `updated_at` 的表 | `BEFORE UPDATE` 自动设置 `updated_at = now()` |

## B接口数据幂等与去重设计

- `history_data`: UK (fsu_id, point_code, collect_time) 防止同一FSU同一点位同一时刻的重复写入
- `realtime_data`: UK (point_id) 确保每个点位只有最新一条实时数据，使用 UPSERT 更新
- `b_interface_message_log`: 不设报文级唯一约束，允许重放和重试
- `b_interface_session`: UK (session_id) 防止会话ID冲突

## 安全约束

- `b_interface_command.safe_enabled` 字段默认为 TRUE
- `SET_FSUREBOOT` 记录的 `safe_enabled = FALSE`，通过应用层读取该字段阻止执行
- 无 CASCADE 级联删除，所有外键使用默认的 NO ACTION，防止误删扩散
