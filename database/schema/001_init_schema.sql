-- ========================================
-- 机房动环监控平台 - 数据库核心 DDL
-- 版本: 0.0.2
-- 协议: 中国铁塔 B接口 2016
-- 数据库: PostgreSQL 16
-- 任务: INIT-002
-- ========================================

-- ========================================
-- 清理旧表（逆序删除，避免外键冲突）
-- ========================================
DROP TABLE IF EXISTS user_role CASCADE;
DROP TABLE IF EXISTS ftp_transfer_record CASCADE;
DROP TABLE IF EXISTS b_interface_call_record CASCADE;
DROP TABLE IF EXISTS b_interface_message_log CASCADE;
DROP TABLE IF EXISTS b_interface_fsu_status CASCADE;
DROP TABLE IF EXISTS b_interface_session CASCADE;
DROP TABLE IF EXISTS b_interface_command CASCADE;
DROP TABLE IF EXISTS device_heartbeat CASCADE;
DROP TABLE IF EXISTS alarm_record CASCADE;
DROP TABLE IF EXISTS history_data CASCADE;
DROP TABLE IF EXISTS realtime_data CASCADE;
DROP TABLE IF EXISTS monitoring_point CASCADE;
DROP TABLE IF EXISTS cabinet CASCADE;
DROP TABLE IF EXISTS fsu_device CASCADE;
DROP TABLE IF EXISTS user_account CASCADE;
DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS site CASCADE;

-- ========================================
-- Phase 1: 无外键依赖的基础表
-- ========================================

-- 1. 站点表
CREATE TABLE site (
    id              BIGSERIAL PRIMARY KEY,
    site_code       VARCHAR(64)  NOT NULL,
    site_name       VARCHAR(128) NOT NULL,
    site_type       VARCHAR(32),                    -- 铁塔站点/机房/户外柜
    region          VARCHAR(64),
    address         VARCHAR(256),
    longitude       DECIMAL(10, 6),
    latitude        DECIMAL(10, 6),
    contact_person  VARCHAR(64),
    contact_phone   VARCHAR(32),
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/INACTIVE/MAINTENANCE
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 2. 机柜表
CREATE TABLE cabinet (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT       NOT NULL,
    cabinet_code    VARCHAR(64)  NOT NULL,
    cabinet_name    VARCHAR(128) NOT NULL,
    cabinet_type    VARCHAR(32),                    -- 户外柜/室内柜/综合柜
    model           VARCHAR(64),
    manufacturer    VARCHAR(64),
    install_date    DATE,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/INACTIVE/MAINTENANCE
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_cabinet_code      UNIQUE (cabinet_code),
    CONSTRAINT fk_cabinet_site      FOREIGN KEY (site_id) REFERENCES site(id)
);

-- 3. FSU 设备表
CREATE TABLE fsu_device (
    id              BIGSERIAL PRIMARY KEY,
    site_id         BIGINT       NOT NULL,
    cabinet_id      BIGINT,
    fsu_code        VARCHAR(64)  NOT NULL,
    fsu_name        VARCHAR(128) NOT NULL,
    fsu_type        VARCHAR(32),                    -- 动环FSU/智能FSU
    model           VARCHAR(64),
    manufacturer    VARCHAR(64),
    firmware_version VARCHAR(32),
    ip_addr         VARCHAR(45),                    -- 支持 IPv6
    port            INTEGER,
    mac_addr        VARCHAR(24),
    protocol_version VARCHAR(16) DEFAULT 'B-2016',
    status          VARCHAR(16)  NOT NULL DEFAULT 'OFFLINE', -- ONLINE/OFFLINE/MAINTENANCE/UNREGISTERED
    register_time   TIMESTAMPTZ,
    last_online_time TIMESTAMPTZ,
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_fsu_code           UNIQUE (fsu_code),
    CONSTRAINT fk_fsu_device_site    FOREIGN KEY (site_id)    REFERENCES site(id),
    CONSTRAINT fk_fsu_device_cabinet FOREIGN KEY (cabinet_id) REFERENCES cabinet(id)
);

-- 4. 角色表
CREATE TABLE role (
    id              BIGSERIAL PRIMARY KEY,
    role_code       VARCHAR(32)  NOT NULL,
    role_name       VARCHAR(64)  NOT NULL,
    description     VARCHAR(256),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_role_code UNIQUE (role_code)
);

-- 5. 用户表
CREATE TABLE user_account (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(256) NOT NULL,
    display_name    VARCHAR(64),
    email           VARCHAR(128),
    phone           VARCHAR(32),
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/DISABLED/LOCKED
    last_login_time TIMESTAMPTZ,
    description     VARCHAR(256),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_email    UNIQUE (email)
);

-- 6. 用户角色关联表
CREATE TABLE user_role (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    role_id         BIGINT       NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_user_role          UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_role_user     FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_user_role_role     FOREIGN KEY (role_id) REFERENCES role(id)
);

-- ========================================
-- Phase 2: B接口命令定义（基础数据表）
-- ========================================

-- 7. B接口命令定义表
CREATE TABLE b_interface_command (
    id              BIGSERIAL PRIMARY KEY,
    command_code    VARCHAR(32)  NOT NULL,           -- PK_Type
    command_name    VARCHAR(64)  NOT NULL,
    direction       VARCHAR(16)  NOT NULL,           -- FSU_TO_SC / SC_TO_FSU
    category        VARCHAR(32),                     -- 快数据/慢数据/文件/管理
    description     VARCHAR(256),
    implemented     BOOLEAN      NOT NULL DEFAULT FALSE,  -- 是否已实现
    safe_enabled    BOOLEAN      NOT NULL DEFAULT TRUE,   -- 安全开关
    priority        VARCHAR(8)   DEFAULT 'P2',       -- P1/P2/P3
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_bic_command_code UNIQUE (command_code),
    CONSTRAINT chk_bic_direction   CHECK (direction IN ('FSU_TO_SC', 'SC_TO_FSU'))
);

-- ========================================
-- Phase 3: B接口报文日志（需在历史/告警/心跳之前创建）
-- ========================================

-- 8. B接口报文日志表
CREATE TABLE b_interface_message_log (
    id              BIGSERIAL PRIMARY KEY,
    direction       VARCHAR(16)  NOT NULL,           -- SC_TO_FSU / FSU_TO_SC
    command_code    VARCHAR(32)  NOT NULL,           -- 对应 b_interface_command.command_code
    fsu_id          BIGINT,
    fsu_code        VARCHAR(64),
    session_id      VARCHAR(64),                     -- 会话关联
    message_type    VARCHAR(16)  NOT NULL,           -- REQUEST / RESPONSE / FAULT
    pk_type         VARCHAR(32),
    info            TEXT,                            -- Info 字段原文
    xml_data        TEXT,                            -- xmlData 字段原文
    raw_message     TEXT,                            -- 完整 SOAP/XML 报文
    status          VARCHAR(16)  DEFAULT 'RECEIVED', -- RECEIVED/PROCESSED/FAILED
    error_message   TEXT,
    process_time_ms INTEGER,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_bmsg_fsu       FOREIGN KEY (fsu_id) REFERENCES fsu_device(id)
);

CREATE INDEX idx_bmsg_fsu_time     ON b_interface_message_log(fsu_id, created_at);
CREATE INDEX idx_bmsg_command      ON b_interface_message_log(command_code, created_at);
CREATE INDEX idx_bmsg_direction    ON b_interface_message_log(direction, created_at);
CREATE INDEX idx_bmsg_session      ON b_interface_message_log(session_id);
CREATE INDEX idx_bmsg_status       ON b_interface_message_log(status, created_at);

-- ========================================
-- Phase 4: 监控点位
-- ========================================

-- 9. 监控点位表
CREATE TABLE monitoring_point (
    id              BIGSERIAL PRIMARY KEY,
    fsu_id          BIGINT       NOT NULL,
    cabinet_id      BIGINT,
    point_code      VARCHAR(64)  NOT NULL,
    point_name      VARCHAR(128) NOT NULL,
    point_type      VARCHAR(32)  NOT NULL,           -- AI/DI/DO/PI/ACC
    data_type       VARCHAR(16)  NOT NULL DEFAULT 'NUMBER', -- NUMBER/TEXT/BOOLEAN
    unit            VARCHAR(16),
    value_range     VARCHAR(64),                     -- 量程如 "0-100"
    precision_val   INTEGER      DEFAULT 2,          -- 精度
    alarm_upper     DECIMAL(12, 4),
    alarm_lower     DECIMAL(12, 4),
    alarm_upper_urgent DECIMAL(12, 4),
    alarm_lower_urgent DECIMAL(12, 4),
    polling_interval INTEGER     DEFAULT 300,        -- 轮询间隔(秒)
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    sort_order      INTEGER      DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_point_fsu_code     UNIQUE (fsu_id, point_code),
    CONSTRAINT fk_point_fsu          FOREIGN KEY (fsu_id)     REFERENCES fsu_device(id),
    CONSTRAINT fk_point_cabinet      FOREIGN KEY (cabinet_id) REFERENCES cabinet(id)
);

-- ========================================
-- Phase 5: 数据采集与告警
-- ========================================

-- 10. 实时数据表
CREATE TABLE realtime_data (
    id              BIGSERIAL PRIMARY KEY,
    fsu_id          BIGINT       NOT NULL,
    point_id        BIGINT       NOT NULL,
    point_code      VARCHAR(64)  NOT NULL,
    value_text      TEXT,
    value_number    DECIMAL(16, 4),
    value_status    VARCHAR(16)  DEFAULT 'NORMAL',   -- NORMAL/LOW_ALARM/HIGH_ALARM/OFFLINE/INVALID
    quality         VARCHAR(8)   DEFAULT 'GOOD',     -- GOOD/BAD/UNCERTAIN
    collect_time    TIMESTAMPTZ  NOT NULL,
    receive_time    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_realtime_point    UNIQUE (point_id),
    CONSTRAINT fk_realtime_fsu      FOREIGN KEY (fsu_id)   REFERENCES fsu_device(id),
    CONSTRAINT fk_realtime_point_fk FOREIGN KEY (point_id) REFERENCES monitoring_point(id)
);

-- 11. 历史数据表
CREATE TABLE history_data (
    id               BIGSERIAL PRIMARY KEY,
    fsu_id           BIGINT       NOT NULL,
    point_id         BIGINT       NOT NULL,
    point_code       VARCHAR(64)  NOT NULL,
    value_text       TEXT,
    value_number     DECIMAL(16, 4),
    value_status     VARCHAR(16)  DEFAULT 'NORMAL',
    quality          VARCHAR(8)   DEFAULT 'GOOD',
    collect_time     TIMESTAMPTZ  NOT NULL,
    receive_time     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    source_message_id BIGINT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_history_duplicate     UNIQUE (fsu_id, point_code, collect_time),
    CONSTRAINT fk_history_fsu           FOREIGN KEY (fsu_id)   REFERENCES fsu_device(id),
    CONSTRAINT fk_history_point         FOREIGN KEY (point_id) REFERENCES monitoring_point(id),
    CONSTRAINT fk_history_source_msg    FOREIGN KEY (source_message_id) REFERENCES b_interface_message_log(id)
);

CREATE INDEX idx_history_data_point_time ON history_data(point_id, collect_time);
CREATE INDEX idx_history_data_fsu_time    ON history_data(fsu_id, collect_time);
CREATE INDEX idx_history_data_collect     ON history_data(collect_time);

-- 12. 告警记录表
CREATE TABLE alarm_record (
    id              BIGSERIAL PRIMARY KEY,
    fsu_id          BIGINT       NOT NULL,
    point_id        BIGINT,
    point_code      VARCHAR(64),
    alarm_code      VARCHAR(32),
    alarm_name      VARCHAR(128),
    alarm_level     VARCHAR(16)  NOT NULL DEFAULT 'WARN',  -- URGENT/IMPORTANT/WARN/INFO
    alarm_status    VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/CONFIRMED/CLEARED/IGNORED
    alarm_value     VARCHAR(64),
    alarm_desc      TEXT,
    occur_time      TIMESTAMPTZ  NOT NULL,
    confirm_time    TIMESTAMPTZ,
    clear_time      TIMESTAMPTZ,
    confirm_user_id BIGINT,
    cleared_by      VARCHAR(16)  DEFAULT 'AUTO',           -- AUTO/MANUAL
    source_message_id BIGINT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_alarm_fsu          FOREIGN KEY (fsu_id)           REFERENCES fsu_device(id),
    CONSTRAINT fk_alarm_point        FOREIGN KEY (point_id)         REFERENCES monitoring_point(id),
    CONSTRAINT fk_alarm_confirm_user FOREIGN KEY (confirm_user_id)  REFERENCES user_account(id),
    CONSTRAINT fk_alarm_source_msg   FOREIGN KEY (source_message_id) REFERENCES b_interface_message_log(id)
);

CREATE INDEX idx_alarm_record_fsu_time    ON alarm_record(fsu_id, occur_time);
CREATE INDEX idx_alarm_record_status      ON alarm_record(alarm_status, occur_time);
CREATE INDEX idx_alarm_record_level_time  ON alarm_record(alarm_level, occur_time);

-- LANDING-009: alarm_record 字段补齐
-- serial_no/device_id 来自 LANDING-001 (B接口2016 SEND_ALARM), spid 来自 LANDING-008
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS serial_no VARCHAR(128);
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS device_id VARCHAR(128);
ALTER TABLE alarm_record ADD COLUMN IF NOT EXISTS spid VARCHAR(64);

-- 13. 设备心跳表
CREATE TABLE device_heartbeat (
    id              BIGSERIAL PRIMARY KEY,
    fsu_id          BIGINT       NOT NULL,
    fsu_code        VARCHAR(64)  NOT NULL,
    heartbeat_time  TIMESTAMPTZ  NOT NULL,
    status_info     VARCHAR(256),
    source_message_id BIGINT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_heartbeat_fsu        FOREIGN KEY (fsu_id)   REFERENCES fsu_device(id),
    CONSTRAINT fk_heartbeat_source_msg FOREIGN KEY (source_message_id) REFERENCES b_interface_message_log(id)
);

CREATE INDEX idx_heartbeat_fsu_time ON device_heartbeat(fsu_id, heartbeat_time);

-- ========================================
-- Phase 6: 其余 B接口相关表
-- ========================================

-- 14. B接口会话/登录状态表
CREATE TABLE b_interface_session (
    id               BIGSERIAL PRIMARY KEY,
    fsu_id           BIGINT       NOT NULL,
    fsu_code         VARCHAR(64)  NOT NULL,
    session_id       VARCHAR(64)  NOT NULL,
    auth_token       VARCHAR(256),
    login_time       TIMESTAMPTZ  NOT NULL,
    last_active_time TIMESTAMPTZ  NOT NULL,
    logout_time      TIMESTAMPTZ,
    status           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/EXPIRED/LOGOUT
    remote_addr      VARCHAR(45),
    expire_seconds   INTEGER      DEFAULT 3600,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_bis_session_id UNIQUE (session_id),
    CONSTRAINT fk_bis_fsu        FOREIGN KEY (fsu_id) REFERENCES fsu_device(id)
);

CREATE INDEX idx_bis_fsu_active ON b_interface_session(fsu_id, status);

-- 15. B接口 FSU 状态表
CREATE TABLE b_interface_fsu_status (
    id              BIGSERIAL PRIMARY KEY,
    fsu_id          BIGINT       NOT NULL,
    fsu_code        VARCHAR(64)  NOT NULL,
    login_status    VARCHAR(16)  NOT NULL DEFAULT 'LOGOUT',  -- LOGIN/LOGOUT/REJECTED
    online_status   VARCHAR(16)  NOT NULL DEFAULT 'OFFLINE', -- ONLINE/OFFLINE/UNSTABLE
    last_login_time TIMESTAMPTZ,
    last_logout_time TIMESTAMPTZ,
    last_heartbeat  TIMESTAMPTZ,
    heartbeat_miss_count INTEGER  DEFAULT 0,
    session_id      VARCHAR(64),
    status_detail   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_bifs_fsu       UNIQUE (fsu_id),
    CONSTRAINT fk_bifs_fsu       FOREIGN KEY (fsu_id) REFERENCES fsu_device(id)
);

-- 16. SC 调用 FSUService 记录表
CREATE TABLE b_interface_call_record (
    id              BIGSERIAL PRIMARY KEY,
    fsu_id          BIGINT       NOT NULL,
    fsu_code        VARCHAR(64)  NOT NULL,
    command_code    VARCHAR(32)  NOT NULL,
    call_type       VARCHAR(16)  NOT NULL,           -- REQUEST / RESPONSE / RETRY
    request_body    TEXT,
    response_body   TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'INIT',  -- INIT/SUCCESS/FAILED/TIMEOUT
    error_message   TEXT,
    duration_ms     INTEGER,
    retry_count     INTEGER      DEFAULT 0,
    call_time       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    source_message_id BIGINT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_bicr_fsu        FOREIGN KEY (fsu_id)   REFERENCES fsu_device(id),
    CONSTRAINT fk_bicr_source_msg FOREIGN KEY (source_message_id) REFERENCES b_interface_message_log(id)
);

CREATE INDEX idx_bicr_fsu_time    ON b_interface_call_record(fsu_id, call_time);
CREATE INDEX idx_bicr_command     ON b_interface_call_record(command_code, call_time);
CREATE INDEX idx_bicr_status      ON b_interface_call_record(status, call_time);

-- 17. FTP 文件/图片传输记录表
CREATE TABLE ftp_transfer_record (
    id              BIGSERIAL PRIMARY KEY,
    fsu_id          BIGINT,
    fsu_code        VARCHAR(64),
    file_name       VARCHAR(256) NOT NULL,
    file_type       VARCHAR(32)  NOT NULL,           -- IMAGE/LOG/CONFIG/OTHER
    file_size       BIGINT,
    remote_path     VARCHAR(512),
    local_path      VARCHAR(512),
    direction       VARCHAR(16)  NOT NULL,           -- UPLOAD/DOWNLOAD
    transfer_status VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- PENDING/TRANSFERRING/SUCCESS/FAILED
    checksum        VARCHAR(128),
    error_message   TEXT,
    transfer_time   TIMESTAMPTZ,
    completed_time  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_ftp_fsu FOREIGN KEY (fsu_id) REFERENCES fsu_device(id)
);

CREATE INDEX idx_ftp_fsu_time  ON ftp_transfer_record(fsu_id, transfer_time);
CREATE INDEX idx_ftp_status    ON ftp_transfer_record(transfer_status, created_at);

-- ========================================
-- 创建 updated_at 自动更新触发器
-- ========================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

COMMENT ON FUNCTION update_updated_at_column() IS '自动更新 updated_at 字段为当前时间';

-- 为所有包含 updated_at 的表创建触发器
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOR tbl IN
        SELECT table_name FROM information_schema.columns
        WHERE table_schema = 'public'
          AND column_name = 'updated_at'
          AND table_name NOT LIKE 'pg_%'
          AND table_name NOT LIKE 'sql_%'
    LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()',
            tbl, tbl
        );
    END LOOP;
END $$;

-- ============================================================
-- LANDING-013-FIX-001: align historical b_interface_message_log.command_code with current entity
-- 原因: Entity 映射到 command，但真实表有 command_code NOT NULL 历史约束
--        Entity 不写入 command_code → INSERT 触发 not-null violation
ALTER TABLE b_interface_message_log ALTER COLUMN command_code DROP NOT NULL;

-- LANDING-001: 真实点位表接入前数据模型补强
-- ============================================================

-- alarm_record 字段补齐已移至表定义区域 (LANDING-009 合并)
-- fsu_device 新增 service_url 字段
ALTER TABLE fsu_device ADD COLUMN IF NOT EXISTS service_url VARCHAR(512);
