package com.dcim.platform.module.mapping.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "estoneii_event_dictionary",
        uniqueConstraints = @UniqueConstraint(name = "uk_estoneii_event_id", columnNames = "event_id"))
public class EStoneIIEventDictionaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "event_name", length = 128, nullable = false)
    private String eventName;

    @Column(name = "signal_id", length = 64)
    private String signalId;

    @Column(name = "start_expression", columnDefinition = "TEXT")
    private String startExpression;

    @Column(name = "event_category", length = 32)
    private String eventCategory;

    @Column(name = "event_severity", length = 32)
    private String eventSeverity;

    @Column(name = "start_operation", length = 32)
    private String startOperation;

    @Column(name = "start_compare_value", length = 64)
    private String startCompareValue;

    @Column(columnDefinition = "TEXT")
    private String meanings;

    @Column(name = "base_type_id", length = 64)
    private String baseTypeId;

    @Column(name = "template_variant", length = 32, nullable = false)
    private String templateVariant;

    @Column(name = "need_real_data_confirm", nullable = false)
    private Boolean needRealDataConfirm;

    @Column(name = "source_file", length = 512)
    private String sourceFile;

    @Column(nullable = false)
    private Boolean enable;

    @Column(nullable = false)
    private Boolean visible;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
