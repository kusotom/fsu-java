package com.dcim.platform.module.alarm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alarm_record")
public class AlarmRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fsu_id", nullable = false)
    private Long fsuId;

    @Column(name = "point_id")
    private Long pointId;

    @Column(name = "point_code", length = 64)
    private String pointCode;

    @Column(name = "serial_no", length = 128)
    private String serialNo;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "alarm_code", length = 32)
    private String alarmCode;

    @Column(name = "alarm_name", length = 128)
    private String alarmName;

    @Column(name = "alarm_level", length = 16, nullable = false)
    private String alarmLevel;

    @Column(name = "alarm_status", length = 16, nullable = false)
    private String alarmStatus;

    @Column(name = "alarm_value", length = 64)
    private String alarmValue;

    @Column(name = "alarm_desc", columnDefinition = "TEXT")
    private String alarmDesc;

    @Column(name = "occur_time", nullable = false)
    private LocalDateTime occurTime;

    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    @Column(name = "clear_time")
    private LocalDateTime clearTime;

    @Column(name = "confirm_user_id")
    private Long confirmUserId;

    @Column(name = "cleared_by", length = 16)
    private String clearedBy;

    @Column(name = "source_message_id")
    private Long sourceMessageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
