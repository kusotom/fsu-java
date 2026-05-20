package com.dcim.platform.module.resource.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "fsu_device")
public class FsuDeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "cabinet_id")
    private Long cabinetId;

    @Column(name = "fsu_code", length = 64, nullable = false)
    private String fsuCode;

    @Column(name = "fsu_name", length = 128, nullable = false)
    private String fsuName;

    @Column(name = "fsu_type", length = 32)
    private String fsuType;

    @Column(length = 64)
    private String model;

    @Column(length = 64)
    private String manufacturer;

    @Column(name = "firmware_version", length = 32)
    private String firmwareVersion;

    @Column(name = "ip_addr", length = 45)
    private String ipAddr;

    private Integer port;

    @Column(name = "mac_addr", length = 24)
    private String macAddr;

    @Column(name = "protocol_version", length = 16)
    private String protocolVersion;

    @Column(name = "service_url", length = 512)
    private String serviceUrl;

    @Column(length = 16, nullable = false)
    private String status;

    @Column(name = "register_time")
    private LocalDateTime registerTime;

    @Column(name = "last_online_time")
    private LocalDateTime lastOnlineTime;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
