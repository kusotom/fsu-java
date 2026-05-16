package com.dcim.platform.module.telemetry.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "realtime_data")
public class RealtimeDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fsu_id", nullable = false)
    private Long fsuId;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    @Column(name = "point_code", length = 64, nullable = false)
    private String pointCode;

    @Column(name = "value_text", columnDefinition = "TEXT")
    private String valueText;

    @Column(name = "value_number", precision = 16, scale = 4)
    private BigDecimal valueNumber;

    @Column(name = "value_status", length = 16)
    private String valueStatus;

    @Column(length = 8)
    private String quality;

    @Column(name = "collect_time", nullable = false)
    private LocalDateTime collectTime;

    @Column(name = "receive_time", nullable = false)
    private LocalDateTime receiveTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
