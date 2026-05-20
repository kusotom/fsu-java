package com.dcim.platform.module.binterface.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 活动告警审计记录实体 (BIF-P4-021, BIF-P4-023)。
 *
 * <p>持久化每次定时审计的运行结果，重启不丢失。</p>
 */
@Entity
@Table(name = "active_alarm_audit_record")
public class ActiveAlarmAuditRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fsu_code", length = 64, nullable = false)
    private String fsuCode;

    @Column(name = "suid", length = 128)
    private String suid;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "query_result_code", length = 16)
    private String queryResultCode;

    @Column(name = "real_device_accessed", nullable = false)
    private boolean realDeviceAccessed;

    @Column(name = "fsu_count")
    private int fsuCount;

    @Column(name = "local_count")
    private int localCount;

    @Column(name = "matched_count")
    private int matchedCount;

    @Column(name = "fsu_only_count")
    private int fsuOnlyCount;

    @Column(name = "local_only_count")
    private int localOnlyCount;

    @Column(name = "mismatch_count")
    private int mismatchCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "summary", length = 512)
    private String summary;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "run_at", nullable = false)
    private LocalDateTime runAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }

    public String getFsuCode() { return fsuCode; }
    public void setFsuCode(String v) { this.fsuCode = v; }

    public String getSuid() { return suid; }
    public void setSuid(String v) { this.suid = v; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean v) { this.success = v; }

    public String getQueryResultCode() { return queryResultCode; }
    public void setQueryResultCode(String v) { this.queryResultCode = v; }

    public boolean isRealDeviceAccessed() { return realDeviceAccessed; }
    public void setRealDeviceAccessed(boolean v) { this.realDeviceAccessed = v; }

    public int getFsuCount() { return fsuCount; }
    public void setFsuCount(int v) { this.fsuCount = v; }

    public int getLocalCount() { return localCount; }
    public void setLocalCount(int v) { this.localCount = v; }

    public int getMatchedCount() { return matchedCount; }
    public void setMatchedCount(int v) { this.matchedCount = v; }

    public int getFsuOnlyCount() { return fsuOnlyCount; }
    public void setFsuOnlyCount(int v) { this.fsuOnlyCount = v; }

    public int getLocalOnlyCount() { return localOnlyCount; }
    public void setLocalOnlyCount(int v) { this.localOnlyCount = v; }

    public int getMismatchCount() { return mismatchCount; }
    public void setMismatchCount(int v) { this.mismatchCount = v; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }

    public String getSummary() { return summary; }
    public void setSummary(String v) { this.summary = v; }

    public String getResultJson() { return resultJson; }
    public void setResultJson(String v) { this.resultJson = v; }

    public LocalDateTime getRunAt() { return runAt; }
    public void setRunAt(LocalDateTime v) { this.runAt = v; }

    public static ActiveAlarmAuditRecordEntity from(
            String fsuCode,
            com.dcim.platform.module.binterface.service.ActiveAlarmConsistencyAuditResult result,
            String errorMessage) {
        return from(fsuCode, result, errorMessage, null);
    }

    public static ActiveAlarmAuditRecordEntity from(
            String fsuCode,
            com.dcim.platform.module.binterface.service.ActiveAlarmConsistencyAuditResult result,
            String errorMessage,
            String resultJson) {
        ActiveAlarmAuditRecordEntity e = new ActiveAlarmAuditRecordEntity();
        e.setFsuCode(fsuCode);
        e.setSuid(result != null ? result.getSuid() : null);
        e.setSuccess(result != null && result.isSuccess());
        e.setQueryResultCode(result != null ? result.getQueryResultCode() : null);
        e.setRealDeviceAccessed(result != null && result.isRealDeviceAccessed());
        e.setRunAt(LocalDateTime.now());

        if (result != null) {
            e.setFsuCount(result.getFsuCount());
            e.setLocalCount(result.getLocalCount());
            e.setMatchedCount(result.getMatchedCount());
            e.setFsuOnlyCount(result.getFsuOnlyCount());
            e.setLocalOnlyCount(result.getLocalOnlyCount());
            e.setMismatchCount(result.getMismatchCount());
            e.setSummary(result.toString());
        }

        if (errorMessage != null && !errorMessage.isEmpty()) {
            e.setErrorMessage(errorMessage);
        }

        if (resultJson != null && !resultJson.isEmpty()) {
            e.setResultJson(resultJson);
        }

        return e;
    }
}
