package com.dcim.platform.module.binterface.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "b_interface_call_record")
public class BInterfaceCallRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fsu_id", nullable = false)
    private Long fsuId;

    @Column(name = "fsu_code", length = 64, nullable = false)
    private String fsuCode;

    @Column(name = "command_code", length = 32, nullable = false)
    private String commandCode;

    @Column(name = "call_type", length = 16, nullable = false)
    private String callType;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(length = 16, nullable = false)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "call_time", nullable = false)
    private LocalDateTime callTime;

    @Column(name = "source_message_id")
    private Long sourceMessageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
