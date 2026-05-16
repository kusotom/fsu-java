package com.dcim.platform.module.binterface.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "b_interface_fsu_status")
public class BInterfaceFsuStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fsu_id", nullable = false)
    private Long fsuId;

    @Column(name = "fsu_code", length = 64, nullable = false)
    private String fsuCode;

    @Column(name = "login_status", length = 16, nullable = false)
    private String loginStatus;

    @Column(name = "online_status", length = 16, nullable = false)
    private String onlineStatus;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "last_logout_time")
    private LocalDateTime lastLogoutTime;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "heartbeat_miss_count")
    private Integer heartbeatMissCount;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "status_detail", columnDefinition = "TEXT")
    private String statusDetail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
