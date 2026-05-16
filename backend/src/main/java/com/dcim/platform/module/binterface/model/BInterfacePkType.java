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

    LOGIN,
    HEARTBEAT,
    GET_DATA,
    SEND_DATA,
    SEND_ALARM,
    GET_HISTORY_DATA,
    SET_POINT,
    GET_THRESHOLD,
    SET_THRESHOLD,
    TIME_CHECK,
    GET_FTP,
    SET_FTP,
    GET_LOGININFO,
    GET_FSUINFO,
    SET_DATA,
    SET_FSUREBOOT,
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
