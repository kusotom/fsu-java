package com.dcim.platform.module.mapping.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "estoneii_signal_dictionary",
        uniqueConstraints = @UniqueConstraint(name = "uk_estoneii_signal_id", columnNames = "signal_id"))
public class EStoneIISignalDictionaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "signal_id", length = 64, nullable = false)
    private String signalId;

    @Column(name = "signal_name", length = 128, nullable = false)
    private String signalName;

    @Column(name = "signal_category", length = 32)
    private String signalCategory;

    @Column(name = "signal_type", length = 32)
    private String signalType;

    @Column(name = "channel_no", length = 32)
    private String channelNo;

    @Column(name = "channel_type", length = 32)
    private String channelType;

    @Column(length = 32)
    private String unit;

    @Column(name = "base_type_id", length = 64)
    private String baseTypeId;

    @Column(name = "signal_meanings_raw", columnDefinition = "TEXT")
    private String signalMeaningsRaw;

    @Column(name = "signal_meanings_json", columnDefinition = "TEXT")
    private String signalMeaningsJson;

    @Column(columnDefinition = "TEXT")
    private String expression;

    @Column(name = "display_index")
    private Integer displayIndex;

    @Column(name = "template_variant", length = 32, nullable = false)
    private String templateVariant;

    @Column(name = "mapping_confidence", length = 32, nullable = false)
    private String mappingConfidence;

    @Column(name = "need_real_data_confirm", nullable = false)
    private Boolean needRealDataConfirm;

    @Column(nullable = false)
    private Boolean derived;

    @Column(nullable = false)
    private Boolean enable;

    @Column(nullable = false)
    private Boolean visible;

    @Column(name = "source_file", length = 512)
    private String sourceFile;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
