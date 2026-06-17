package com.dcim.platform.module.mapping.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "device_signal_candidate",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_signal_candidate",
                columnNames = {"fsu_id", "device_id", "signal_id"}))
public class DeviceSignalCandidateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fsu_id", length = 64, nullable = false)
    private String fsuId;

    @Column(name = "device_id", length = 128, nullable = false)
    private String deviceId;

    @Column(name = "device_code", length = 128)
    private String deviceCode;

    @Column(name = "device_name", length = 128)
    private String deviceName;

    @Column(name = "tower_category_id", length = 32)
    private String towerCategoryId;

    @Column(name = "tower_device_type", length = 128)
    private String towerDeviceType;

    @Column(name = "emerson_device_type_id", length = 32)
    private String emersonDeviceTypeId;

    @Column(name = "signal_id", length = 64, nullable = false)
    private String signalId;

    @Column(name = "signal_name", length = 128)
    private String signalName;

    @Column(length = 32, nullable = false)
    private String confidence;

    @Column(name = "template_variant", length = 32)
    private String templateVariant;

    @Column(name = "need_real_data_confirm", nullable = false)
    private Boolean needRealDataConfirm;

    @Column(name = "mapping_source", length = 128, nullable = false)
    private String mappingSource;

    @Column(name = "verified_by_real_data", nullable = false)
    private Boolean verifiedByRealData;

    @Column(name = "mapping_status", length = 32, nullable = false)
    private String mappingStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
