# 数据库目录

## 目录结构

- `schema/` - 数据库表结构初始化脚本
  - `001_init_schema.sql` — **核心 DDL（17 张表，含索引、约束、触发器）**
- `seed/` - 演示数据
  - `001_seed_demo_data.sql` — **最小演示数据（1站点/1机柜/1FSU/5点位）**
- `migration/` - 后续版本迁移脚本（待 INIT-003 后启用）

## 表清单

### 资源管理
- `site` - 站点表
- `cabinet` - 机柜表
- `fsu_device` - FSU设备表
- `monitoring_point` - 监控点位表

### 数据监控
- `realtime_data` - 实时数据表
- `history_data` - 历史数据表

### 告警管理
- `alarm_record` - 告警记录表
- `device_heartbeat` - 设备心跳表

### B接口
- `b_interface_message_log` - B接口SOAP/XML报文日志表
- `b_interface_command` - B接口命令定义表（15条命令，含安全开关）
- `b_interface_session` - B接口会话/登录状态表
- `b_interface_fsu_status` - B接口FSU状态表
- `b_interface_call_record` - SC调用FSUService记录表
- `ftp_transfer_record` - FTP文件/图片传输记录表

### 系统管理
- `user_account` - 用户表
- `role` - 角色表
- `user_role` - 用户角色关联表

## 表间关系

详见 `../docs/database/core-model-design.md`

## 执行方式

```bash
# 方式一：通过 psql
psql -h localhost -U dcim -d dcim_platform -f schema/001_init_schema.sql
psql -h localhost -U dcim -d dcim_platform -f seed/001_seed_demo_data.sql

# 方式二：通过 docker exec
docker exec -i dcim-postgres psql -U dcim -d dcim_platform < schema/001_init_schema.sql
docker exec -i dcim-postgres psql -U dcim -d dcim_platform < seed/001_seed_demo_data.sql
```

## 注意事项

- SET_FSUREBOOT 命令 `safe_enabled = FALSE`，应用层需检查此字段
- 所有密码为 bcrypt 占位字符串，不可用于生产
- 外键无 CASCADE，删除需按逆序手动清理
