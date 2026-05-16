package com.dcim.platform.module.telemetry.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "device_heartbeat")
public class DeviceHeartbeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fsu_id", nullable = false)
    private Long fsuId;

    @Column(name = "fsu_code", length = 64, nullable = false)
    private String fsuCode;

    @Column(name = "heartbeat_time", nullable = false)
    private LocalDateTime heartbeatTime;

    @Column(name = "status_info", length = 256)
    private String statusInfo;

    @Column(name = "source_message_id")
    private Long sourceMessageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
