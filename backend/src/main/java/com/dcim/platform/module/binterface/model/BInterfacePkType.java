package com.dcim.platform.module.binterface.model;

/**
 * B接口命令码枚举
 *
 * 对应铁塔B接口2016协议中的 PK_Type 字段
 * 后续根据协议文档逐步补充完整命令码
 *
 * TODO: BIF-P0-004 整理完整命令码映射表
 */
public enum BInterfacePkType {

    // === 2016 协议: 连接管理 ===
    LOGIN,
    LOGIN_ACK,
    LOGOUT,
    LOGOUT_ACK,

    // === 2016 协议: 快数据通道 ===
    /**
     * Non-standard: B接口2016 原文未定义 HEARTBEAT PK_Type。
     * 工程心跳应使用 GET_FSUINFO (1701/1702)。保留为兼容旧心跳实现。
     * @see openspec/protocols/binterface-2016/matrices/unknown-and-ambiguous-items.md UNKNOWN-2016-011
     */
    HEARTBEAT,
    GET_DATA,
    GET_DATA_ACK,
    SEND_DATA,
    SEND_DATA_ACK,
    SEND_ALARM,
    SEND_ALARM_ACK,

    // === 2016 协议: 慢数据通道 ===
    GET_HISDATA,              // 2016 canonical 名称
    GET_HISDATA_ACK,
    GET_HISTORY_DATA,         // @Deprecated 旧兼容别名 → GET_HISDATA
    SET_POINT,
    SET_POINT_ACK,
    GET_THRESHOLD,
    GET_THRESHOLD_ACK,
    SET_THRESHOLD,
    SET_THRESHOLD_ACK,
    TIME_CHECK,
    TIME_CHECK_ACK,
    GET_LOGININFO,
    GET_LOGININFO_ACK,
    SET_LOGININFO,
    SET_LOGININFO_ACK,
    GET_FTP,
    GET_FTP_ACK,
    SET_FTP,
    SET_FTP_ACK,
    GET_FSUINFO,
    GET_FSUINFO_ACK,
    /**
     * Non-standard: 不在 B接口2016 标准命令码表中。2024 alias 映射为 SET_RMCTRLCMD。
     * 仅保留为旧兼容，标准 2016 命令不应使用。
     */
    SET_DATA,
    SET_FSUREBOOT,
    SET_FSUREBOOT_ACK,

    // === 2024 协议: 兼容保留 ===
    GET_SUINFO,
    GET_SUFTP,
    SET_TIME,
    SET_TIME_ACK,
    GET_SPCONFIGOPTION,
    GET_SPCONFIGOPTION_ACK,
    GET_ACTIVEALARM,
    SUREADY,
    SUREADY_ACK,

    UNKNOWN
}
