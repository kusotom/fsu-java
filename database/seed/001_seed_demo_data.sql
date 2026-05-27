-- ========================================
-- 机房动环监控平台 - 最小演示数据
-- 版本: 0.0.2
-- 任务: INIT-002
-- 注意: 所有密码均为占位 hash，不可用于生产
-- ========================================

-- ========================================
-- 站点
-- ========================================
INSERT INTO site (id, site_code, site_name, site_type, region, address, longitude, latitude, contact_person, contact_phone, status, description) VALUES
(1, 'SITE-001', '朝阳区户外柜站点A', '户外柜', '北京市朝阳区', '北京市朝阳区XX路XX号', 116.443108, 39.921470, '张三', '13800001111', 'ACTIVE', '演示站点-铁塔户外柜');

SELECT setval('site_id_seq', (SELECT MAX(id) FROM site));

-- ========================================
-- 机柜
-- ========================================
INSERT INTO cabinet (id, site_id, cabinet_code, cabinet_name, cabinet_type, model, manufacturer, install_date, status) VALUES
(1, 1, 'CAB-001', '户外柜-01', '户外柜', 'ODC-2100', '华为', '2024-01-15', 'ACTIVE');

SELECT setval('cabinet_id_seq', (SELECT MAX(id) FROM cabinet));

-- ========================================
-- FSU 设备
-- ========================================
INSERT INTO fsu_device (id, site_id, cabinet_id, fsu_code, fsu_name, fsu_type, model, manufacturer, firmware_version, ip_addr, port, protocol_version, status, register_time) VALUES
(1, 1, 1, 'FSU-001', '朝阳户外柜FSU-01', '动环FSU', 'eStone II', '中兴', 'B07D07', '192.168.1.101', 8080, 'B-2016', 'ONLINE', '2024-03-01 10:00:00+08');

SELECT setval('fsu_device_id_seq', (SELECT MAX(id) FROM fsu_device));

-- ========================================
-- 监控点位
-- ========================================
INSERT INTO monitoring_point (id, fsu_id, cabinet_id, point_code, point_name, point_type, data_type, unit, value_range, precision_val, alarm_upper, alarm_lower, alarm_upper_urgent, alarm_lower_urgent, polling_interval, sort_order) VALUES
(1, 1, 1, 'TEMP-001', '机柜温度',      'AI', 'NUMBER', '°C',    '-20-80',   1, 60.0,  -5.0, 70.0,  -10.0, 60,  1),
(2, 1, 1, 'HUMI-001', '机柜湿度',      'AI', 'NUMBER', '%RH',   '0-100',    1, 90.0,  10.0, 95.0,    5.0, 60,  2),
(3, 1, 1, 'VOLT-001', '交流电压A相',   'AI', 'NUMBER', 'V',     '0-300',    1, 245.0, 198.0, 260.0, 185.0, 30,  3),
(4, 1, 1, 'DOOR-001', '柜门状态',      'DI', 'TEXT',   NULL,    NULL,       0, NULL,  NULL,  NULL,  NULL,  10,  4),
(5, 1, 1, 'WATER-001','水浸传感器',    'DI', 'TEXT',   NULL,    NULL,       0, NULL,  NULL,  NULL,  NULL,  30,  5);

SELECT setval('monitoring_point_id_seq', (SELECT MAX(id) FROM monitoring_point));

-- ========================================
-- 实时数据（当前采集值）
-- ========================================
INSERT INTO realtime_data (id, fsu_id, point_id, point_code, value_text, value_number, value_status, quality, collect_time, receive_time) VALUES
(1, 1, 1, 'TEMP-001',  NULL, 25.5, 'NORMAL', 'GOOD',  '2026-05-10 12:00:00+08', '2026-05-10 12:00:01+08'),
(2, 1, 2, 'HUMI-001',  NULL, 55.0, 'NORMAL', 'GOOD',  '2026-05-10 12:00:00+08', '2026-05-10 12:00:01+08'),
(3, 1, 3, 'VOLT-001',  NULL, 220.5,'NORMAL', 'GOOD',  '2026-05-10 12:00:00+08', '2026-05-10 12:00:01+08'),
(4, 1, 4, 'DOOR-001',  'CLOSE', NULL, 'NORMAL', 'GOOD', '2026-05-10 12:00:00+08', '2026-05-10 12:00:01+08'),
(5, 1, 5, 'WATER-001', 'DRY',   NULL, 'NORMAL', 'GOOD', '2026-05-10 12:00:00+08', '2026-05-10 12:00:01+08');

SELECT setval('realtime_data_id_seq', (SELECT MAX(id) FROM realtime_data));

-- ========================================
-- 历史数据（最近几条）
-- ========================================
INSERT INTO history_data (id, fsu_id, point_id, point_code, value_text, value_number, value_status, quality, collect_time, receive_time) VALUES
(1, 1, 1, 'TEMP-001', NULL, 24.8, 'NORMAL', 'GOOD', '2026-05-10 11:00:00+08', '2026-05-10 11:00:01+08'),
(2, 1, 1, 'TEMP-001', NULL, 25.1, 'NORMAL', 'GOOD', '2026-05-10 11:30:00+08', '2026-05-10 11:30:01+08'),
(3, 1, 1, 'TEMP-001', NULL, 25.5, 'NORMAL', 'GOOD', '2026-05-10 12:00:00+08', '2026-05-10 12:00:01+08'),
(4, 1, 3, 'VOLT-001', NULL, 219.8,'NORMAL', 'GOOD', '2026-05-10 11:00:00+08', '2026-05-10 11:00:01+08'),
(5, 1, 3, 'VOLT-001', NULL, 221.0,'NORMAL', 'GOOD', '2026-05-10 11:30:00+08', '2026-05-10 11:30:01+08'),
(6, 1, 3, 'VOLT-001', NULL, 220.5,'NORMAL', 'GOOD', '2026-05-10 12:00:00+08', '2026-05-10 12:00:01+08');

SELECT setval('history_data_id_seq', (SELECT MAX(id) FROM history_data));

-- ========================================
-- 告警记录（演示用）
-- ========================================
INSERT INTO alarm_record (id, fsu_id, point_id, point_code, alarm_code, alarm_name, alarm_level, alarm_status, alarm_value, alarm_desc, occur_time) VALUES
(1, 1, 1, 'TEMP-001', 'TEMP-HIGH', '机柜温度过高', 'WARN', 'CLEARED', '62.0°C', '温度短暂超过上限60°C', '2026-05-10 08:30:00+08');

SELECT setval('alarm_record_id_seq', (SELECT MAX(id) FROM alarm_record));

-- ========================================
-- 设备心跳
-- ========================================
INSERT INTO device_heartbeat (id, fsu_id, fsu_code, heartbeat_time, status_info) VALUES
(1, 1, 'FSU-001', '2026-05-10 11:55:00+08', '正常'),
(2, 1, 'FSU-001', '2026-05-10 12:00:00+08', '正常');

SELECT setval('device_heartbeat_id_seq', (SELECT MAX(id) FROM device_heartbeat));

-- ========================================
-- B接口命令定义（14条命令 + UNKNOWN）
-- ========================================
INSERT INTO b_interface_command (id, command_code, command_name, direction, category, description, implemented, safe_enabled, priority) VALUES
(1,  'LOGIN',           'FSU登录认证',     'FSU_TO_SC', '快数据', 'FSU向SC发起登录认证，建立B接口会话',           FALSE, TRUE,  'P1'),
(2,  'HEARTBEAT',       'FSU心跳上报',     'FSU_TO_SC', '快数据', 'FSU定期向SC上报心跳，维持在线状态',             FALSE, TRUE,  'P1'),
(3,  'SEND_ALARM',      '告警上报',        'FSU_TO_SC', '快数据', 'FSU主动向SC上报实时告警信息',                   FALSE, TRUE,  'P1'),
(4,  'GET_DATA',        '获取监控数据',    'SC_TO_FSU', '慢数据', 'SC轮询FSU获取点位实时监控数据',                 FALSE, TRUE,  'P1'),
(5,  'GET_HISTORY_DATA','获取历史数据',    'SC_TO_FSU', '慢数据', 'SC向FSU获取指定时间段的历史采集数据',            FALSE, TRUE,  'P2'),
(6,  'SET_POINT',       '设置点位参数',    'SC_TO_FSU', '慢数据', 'SC向FSU下发点位配置参数',                       FALSE, TRUE,  'P2'),
(7,  'GET_THRESHOLD',   '获取告警阈值',    'SC_TO_FSU', '慢数据', 'SC查询FSU当前告警阈值配置',                     FALSE, TRUE,  'P2'),
(8,  'SET_THRESHOLD',   '设置告警阈值',    'SC_TO_FSU', '慢数据', 'SC向FSU下发告警阈值配置',                       FALSE, TRUE,  'P2'),
(9,  'TIME_CHECK',      '时间同步',        'SC_TO_FSU', '管理',   'SC向FSU下发标准时间用于时钟同步',               FALSE, TRUE,  'P2'),
(10, 'GET_FTP',         '获取FTP文件信息', 'SC_TO_FSU', '文件',   'SC获取FSU的FTP文件列表和传输状态',             FALSE, TRUE,  'P2'),
(11, 'GET_LOGININFO',   '获取登录信息',    'SC_TO_FSU', '管理',   'SC查询FSU当前登录状态和认证信息',               FALSE, TRUE,  'P2'),
(12, 'GET_FSUINFO',     '获取FSU信息',     'SC_TO_FSU', '管理',   'SC查询FSU设备基本信息、版本、配置',             FALSE, TRUE,  'P2'),
(13, 'SET_FSUREBOOT',   '远程重启FSU',     'SC_TO_FSU', '管理',   'SC远程重启FSU设备（安全敏感，默认禁用）',       FALSE, FALSE, 'P2'),
(14, 'SET_DATA',        '设置监控数据',    'SC_TO_FSU', '慢数据', 'SC向FSU下发控制指令或参数设置',                  FALSE, TRUE,  'P2'),
(15, 'UNKNOWN',         '未知命令',        'BOTH',      '其他',   '未识别的B接口命令码，用于兼容和排错',           TRUE,  TRUE,  'P3');

SELECT setval('b_interface_command_id_seq', (SELECT MAX(id) FROM b_interface_command));

-- ========================================
-- B接口会话（演示：当前活跃登录）
-- ========================================
INSERT INTO b_interface_session (id, fsu_id, fsu_code, session_id, auth_token, login_time, last_active_time, status, remote_addr) VALUES
(1, 1, 'FSU-001', 'SESSION-DEMO-001', 'TOKEN-PLACEHOLDER-NOT-FOR-PROD', '2026-05-10 10:00:00+08', '2026-05-10 12:00:00+08', 'ACTIVE', '192.168.1.101');

SELECT setval('b_interface_session_id_seq', (SELECT MAX(id) FROM b_interface_session));

-- ========================================
-- B接口 FSU 状态
-- ========================================
INSERT INTO b_interface_fsu_status (id, fsu_id, fsu_code, login_status, online_status, last_login_time, last_heartbeat, session_id) VALUES
(1, 1, 'FSU-001', 'LOGIN', 'ONLINE', '2026-05-10 10:00:00+08', '2026-05-10 12:00:00+08', 'SESSION-DEMO-001');

SELECT setval('b_interface_fsu_status_id_seq', (SELECT MAX(id) FROM b_interface_fsu_status));

-- ========================================
-- B接口报文日志（演示：心跳报文）
-- ========================================
INSERT INTO b_interface_message_log (id, direction, command_code, fsu_id, fsu_code, message_type, pk_type, raw_message, status) VALUES
(1, 'FSU_TO_SC', 'HEARTBEAT', 1, 'FSU-001', 'REQUEST', 'HEARTBEAT',
'<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Request>
      <PK_Type>HEARTBEAT</PK_Type>
      <Info>
        <FSUCode>FSU-001</FSUCode>
        <Timestamp>2026-05-10T12:00:00+08:00</Timestamp>
      </Info>
      <xmlData>
        <CPU>35</CPU>
        <Memory>62</Memory>
        <Temperature>42</Temperature>
      </xmlData>
    </Request>
  </soap:Body>
</soap:Envelope>', 'PROCESSED');

SELECT setval('b_interface_message_log_id_seq', (SELECT MAX(id) FROM b_interface_message_log));

-- ========================================
-- SC 调用 FSUService 记录（演示）
-- ========================================
INSERT INTO b_interface_call_record (id, fsu_id, fsu_code, command_code, call_type, request_body, response_body, status, duration_ms, call_time) VALUES
(1, 1, 'FSU-001', 'GET_DATA', 'REQUEST', '<SOAP GET_DATA 请求占位>', NULL, 'INIT', NULL, '2026-05-10 12:00:05+08');

SELECT setval('b_interface_call_record_id_seq', (SELECT MAX(id) FROM b_interface_call_record));

-- ========================================
-- FTP 传输记录（空演示）
-- ========================================
INSERT INTO ftp_transfer_record (id, fsu_id, fsu_code, file_name, file_type, file_size, direction, transfer_status) VALUES
(1, 1, 'FSU-001', 'alarm_snapshot_20260510.jpg', 'IMAGE', 245760, 'UPLOAD', 'SUCCESS');

SELECT setval('ftp_transfer_record_id_seq', (SELECT MAX(id) FROM ftp_transfer_record));

-- ========================================
-- 用户与角色
-- ========================================
INSERT INTO role (id, role_code, role_name, description) VALUES
(1, 'ADMIN',   '管理员',   '系统管理员，拥有全部权限'),
(2, 'OPERATOR','运维人员', '日常监控和运维操作'),
(3, 'VIEWER',  '只读用户', '仅可查看监控数据和告警');

SELECT setval('role_id_seq', (SELECT MAX(id) FROM role));

INSERT INTO user_account (id, username, password_hash, display_name, email, phone, status) VALUES
(1, 'admin',    '$2a$10$PLACEHOLDER_HASH_NOT_FOR_PRODUCTION_USE_00', '系统管理员', 'admin@dcim.local', '13800000000', 'ACTIVE'),
(2, 'operator', '$2a$10$PLACEHOLDER_HASH_NOT_FOR_PRODUCTION_USE_01', '运维工程师', 'operator@dcim.local', '13800000001', 'ACTIVE'),
(3, 'viewer',   '$2a$10$PLACEHOLDER_HASH_NOT_FOR_PRODUCTION_USE_02', '只读用户',   'viewer@dcim.local',  '13800000002', 'ACTIVE');

SELECT setval('user_account_id_seq', (SELECT MAX(id) FROM user_account));

INSERT INTO user_role (id, user_id, role_id) VALUES
(1, 1, 1),  -- admin -> ADMIN
(2, 2, 2),  -- operator -> OPERATOR
(3, 3, 3);  -- viewer -> VIEWER

SELECT setval('user_role_id_seq', (SELECT MAX(id) FROM user_role));

-- BACKEND-FE-API-001: 角色权限 seed（幂等 UPDATE）
UPDATE role SET permissions = 'binterface.read,binterface.fsu.read,binterface.realtime.read,binterface.alarm.read,binterface.threshold.read,binterface.ftp.read,binterface.ftp_image.read,binterface.scheduler.read,binterface.protocol_audit.read,audit.read' WHERE role_code = 'VIEWER';
UPDATE role SET permissions = 'binterface.read,binterface.fsu.read,binterface.realtime.read,binterface.alarm.read,binterface.threshold.read,binterface.ftp.read,binterface.ftp_image.read,binterface.scheduler.read,binterface.protocol_audit.read,binterface.dry_run,binterface.mock,binterface.get.run,scheduler.read,audit.read' WHERE role_code = 'OPERATOR';
UPDATE role SET permissions = 'binterface.read,binterface.fsu.read,binterface.realtime.read,binterface.alarm.read,binterface.threshold.read,binterface.ftp.read,binterface.ftp_image.read,binterface.scheduler.read,binterface.protocol_audit.read,binterface.dry_run,binterface.mock,binterface.get.run,scheduler.read,scheduler.manage,user.read,role.read,permission.read,audit.read' WHERE role_code = 'ADMIN';
