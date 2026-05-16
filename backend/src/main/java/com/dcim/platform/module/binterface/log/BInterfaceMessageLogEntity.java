package com.dcim.platform.module.binterface.log;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * B接口SOAP/XML报文日志实体
 *
 * 记录B接口通信中的所有SOAP/XML报文，用于调试和审计
 *
 * TODO: BIF-P0-005 整理SOAP报文日志规范后完善字段
 */
@Data
@Entity
@Table(name = "b_interface_message_log")
public class BInterfaceMessageLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 方向: SC->FSU 或 FSU->SC */
    private String direction;

    /** 命令码: LOGIN, HEARTBEAT, GET_DATA, SEND_ALARM */
    private String command;

    /** FSU设备编码 */
    private String fsuCode;

    /** 消息类型: Request / Response */
    private String messageType;

    /** 原始报文XML */
    @Column(columnDefinition = "TEXT")
    private String rawMessage;

    /** 记录时间 */
    private LocalDateTime createdAt;
}
