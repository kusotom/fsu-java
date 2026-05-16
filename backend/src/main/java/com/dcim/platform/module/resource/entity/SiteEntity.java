package com.dcim.platform.module.resource.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "site")
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_code", length = 64, nullable = false)
    private String siteCode;

    @Column(name = "site_name", length = 128, nullable = false)
    private String siteName;

    @Column(name = "site_type", length = 32)
    private String siteType;

    @Column(length = 64)
    private String region;

    @Column(length = 256)
    private String address;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(name = "contact_person", length = 64)
    private String contactPerson;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(length = 16, nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
