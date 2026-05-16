package com.dcim.platform.module.binterface.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "b_interface_command")
public class BInterfaceCommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_code", length = 32, nullable = false)
    private String commandCode;

    @Column(name = "command_name", length = 64, nullable = false)
    private String commandName;

    @Column(length = 16, nullable = false)
    private String direction;

    @Column(length = 32)
    private String category;

    @Column(length = 256)
    private String description;

    @Column(nullable = false)
    private Boolean implemented;

    @Column(name = "safe_enabled", nullable = false)
    private Boolean safeEnabled;

    @Column(length = 8)
    private String priority;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
