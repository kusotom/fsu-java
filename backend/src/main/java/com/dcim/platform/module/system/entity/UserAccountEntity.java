package com.dcim.platform.module.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_account")
public class UserAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String username;

    @Column(name = "password_hash", length = 256, nullable = false)
    private String passwordHash;

    @Column(name = "display_name", length = 64)
    private String displayName;

    @Column(length = 128)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(length = 16, nullable = false)
    private String status;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(length = 256)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
