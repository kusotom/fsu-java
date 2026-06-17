package com.dcim.platform.module.mapping.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "unmapped_signal_observation")
public class UnmappedSignalObservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fsu_id", length = 64)
    private String fsuId;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "device_code", length = 128)
    private String deviceCode;

    @Column(length = 64)
    private String spid;

    @Column(name = "signal_id", length = 64)
    private String signalId;

    @Column(name = "raw_id", length = 128)
    private String rawId;

    @Column(name = "raw_name", length = 128)
    private String rawName;

    @Column(name = "raw_value", columnDefinition = "TEXT")
    private String rawValue;

    @Column(length = 32)
    private String unit;

    @Column(name = "source_command", length = 64)
    private String sourceCommand;

    @Column(name = "message_log_id")
    private Long messageLogId;

    @Column(name = "raw_sample_id", length = 128)
    private String rawSampleId;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "seen_count", nullable = false)
    private Integer seenCount;

    @Column(length = 64, nullable = false)
    private String reason;
}
