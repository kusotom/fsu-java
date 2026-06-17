package com.dcim.platform.module.mapping.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "estoneii_control_reference",
        uniqueConstraints = @UniqueConstraint(name = "uk_estoneii_control_id", columnNames = "command_id"))
public class EStoneIIControlReferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_id", length = 64, nullable = false)
    private String commandId;

    @Column(name = "command_name", length = 128, nullable = false)
    private String commandName;

    @Column(name = "command_category", length = 32)
    private String commandCategory;

    @Column(name = "command_severity", length = 32)
    private String commandSeverity;

    @Column(name = "cmd_token", length = 64)
    private String cmdToken;

    @Column(name = "signal_id", length = 64)
    private String signalId;

    @Column(name = "parameter_name", length = 128)
    private String parameterName;

    @Column(name = "parameter_value", length = 128)
    private String parameterValue;

    @Column(columnDefinition = "TEXT")
    private String meanings;

    @Column(name = "source_file", length = 512)
    private String sourceFile;

    @Column(name = "enabled_for_control", nullable = false)
    private Boolean enabledForControl;

    @Column(name = "control_access", length = 32, nullable = false)
    private String controlAccess;

    @Column(length = 64, nullable = false)
    private String source;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
