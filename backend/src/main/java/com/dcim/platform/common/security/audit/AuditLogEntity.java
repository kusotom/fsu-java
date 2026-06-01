package com.dcim.platform.common.security.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * BE-AUTH-P0-001: 操作审计日志实体.
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 200)
    private String resourceId;

    @Column(name = "station_id")
    private Long stationId;

    @Column(name = "fsu_code", length = 100)
    private String fsuCode;

    @Column(name = "permission_code", length = 100)
    private String permissionCode;

    @Column(name = "allowed", nullable = false)
    private boolean allowed;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestTime;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AuditLogEntity() {}

    public static AuditLogEntity create(Long userId, String username, String tenantId,
                                         String action, String resourceType, String resourceId,
                                         Long stationId, String fsuCode, String permissionCode,
                                         boolean allowed, String reason, String clientIp) {
        AuditLogEntity e = new AuditLogEntity();
        e.userId = userId; e.username = username; e.tenantId = tenantId;
        e.action = action; e.resourceType = resourceType; e.resourceId = resourceId;
        e.stationId = stationId; e.fsuCode = fsuCode; e.permissionCode = permissionCode;
        e.allowed = allowed; e.reason = reason; e.clientIp = clientIp;
        e.requestTime = LocalDateTime.now(); e.createdAt = LocalDateTime.now();
        return e;
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getTenantId() { return tenantId; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public Long getStationId() { return stationId; }
    public String getFsuCode() { return fsuCode; }
    public String getPermissionCode() { return permissionCode; }
    public boolean isAllowed() { return allowed; }
    public String getReason() { return reason; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public String getClientIp() { return clientIp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
