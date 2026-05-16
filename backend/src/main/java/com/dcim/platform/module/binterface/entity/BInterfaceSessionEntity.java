package com.dcim.platform.module.binterface.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "b_interface_session")
public class BInterfaceSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fsu_id", nullable = false)
    private Long fsuId;

    @Column(name = "fsu_code", length = 64, nullable = false)
    private String fsuCode;

    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;

    @Column(name = "auth_token", length = 256)
    private String authToken;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "last_active_time", nullable = false)
    private LocalDateTime lastActiveTime;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(length = 16, nullable = false)
    private String status;

    @Column(name = "remote_addr", length = 45)
    private String remoteAddr;

    @Column(name = "expire_seconds")
    private Integer expireSeconds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
