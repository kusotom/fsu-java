package com.dcim.platform.module.resource.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cabinet")
public class CabinetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "cabinet_code", length = 64, nullable = false)
    private String cabinetCode;

    @Column(name = "cabinet_name", length = 128, nullable = false)
    private String cabinetName;

    @Column(name = "cabinet_type", length = 32)
    private String cabinetType;

    @Column(length = 64)
    private String model;

    @Column(length = 64)
    private String manufacturer;

    @Column(name = "install_date")
    private LocalDate installDate;

    @Column(length = 16, nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
