package com.dcim.platform.module.resource.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "monitoring_point")
public class MonitoringPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fsu_id", nullable = false)
    private Long fsuId;

    @Column(name = "cabinet_id")
    private Long cabinetId;

    @Column(name = "point_code", length = 64, nullable = false)
    private String pointCode;

    @Column(name = "point_name", length = 128, nullable = false)
    private String pointName;

    @Column(name = "point_type", length = 32, nullable = false)
    private String pointType;

    @Column(name = "data_type", length = 16, nullable = false)
    private String dataType;

    @Column(length = 16)
    private String unit;

    @Column(name = "value_range", length = 64)
    private String valueRange;

    @Column(name = "precision_val")
    private Integer precisionVal;

    @Column(name = "alarm_upper", precision = 12, scale = 4)
    private BigDecimal alarmUpper;

    @Column(name = "alarm_lower", precision = 12, scale = 4)
    private BigDecimal alarmLower;

    @Column(name = "alarm_upper_urgent", precision = 12, scale = 4)
    private BigDecimal alarmUpperUrgent;

    @Column(name = "alarm_lower_urgent", precision = 12, scale = 4)
    private BigDecimal alarmLowerUrgent;

    @Column(name = "polling_interval")
    private Integer pollingInterval;

    @Column(length = 16, nullable = false)
    private String status;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
