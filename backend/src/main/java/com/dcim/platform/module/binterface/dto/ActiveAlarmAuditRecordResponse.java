package com.dcim.platform.module.binterface.dto;

import com.dcim.platform.module.binterface.entity.ActiveAlarmAuditRecordEntity;

/**
 * 活动告警审计记录响应 DTO (BIF-P4-023)。
 *
 * <p>用于 /latest /history 等只读查询接口。</p>
 */
public class ActiveAlarmAuditRecordResponse {

    private final Long id;
    private final String fsuCode;
    private final String suid;
    private final boolean success;
    private final String queryResultCode;
    private final boolean realDeviceAccessed;
    private final int fsuCount;
    private final int localCount;
    private final int matchedCount;
    private final int fsuOnlyCount;
    private final int localOnlyCount;
    private final int mismatchCount;
    private final String errorMessage;
    private final String summary;
    private final String resultJson;
    private final String runAt;

    private ActiveAlarmAuditRecordResponse(Long id, String fsuCode, String suid, boolean success,
                                            String queryResultCode, boolean realDeviceAccessed,
                                            int fsuCount, int localCount, int matchedCount,
                                            int fsuOnlyCount, int localOnlyCount, int mismatchCount,
                                            String errorMessage, String summary, String resultJson,
                                            String runAt) {
        this.id = id;
        this.fsuCode = fsuCode;
        this.suid = suid;
        this.success = success;
        this.queryResultCode = queryResultCode;
        this.realDeviceAccessed = realDeviceAccessed;
        this.fsuCount = fsuCount;
        this.localCount = localCount;
        this.matchedCount = matchedCount;
        this.fsuOnlyCount = fsuOnlyCount;
        this.localOnlyCount = localOnlyCount;
        this.mismatchCount = mismatchCount;
        this.errorMessage = errorMessage;
        this.summary = summary;
        this.resultJson = resultJson;
        this.runAt = runAt;
    }

    public static ActiveAlarmAuditRecordResponse from(ActiveAlarmAuditRecordEntity entity) {
        if (entity == null) return null;
        return new ActiveAlarmAuditRecordResponse(
                entity.getId(),
                entity.getFsuCode(),
                entity.getSuid(),
                entity.isSuccess(),
                entity.getQueryResultCode(),
                entity.isRealDeviceAccessed(),
                entity.getFsuCount(),
                entity.getLocalCount(),
                entity.getMatchedCount(),
                entity.getFsuOnlyCount(),
                entity.getLocalOnlyCount(),
                entity.getMismatchCount(),
                entity.getErrorMessage(),
                entity.getSummary(),
                entity.getResultJson(),
                entity.getRunAt() != null ? entity.getRunAt().toString() : null);
    }

    public Long getId() { return id; }
    public String getFsuCode() { return fsuCode; }
    public String getSuid() { return suid; }
    public boolean isSuccess() { return success; }
    public String getQueryResultCode() { return queryResultCode; }
    public boolean isRealDeviceAccessed() { return realDeviceAccessed; }
    public int getFsuCount() { return fsuCount; }
    public int getLocalCount() { return localCount; }
    public int getMatchedCount() { return matchedCount; }
    public int getFsuOnlyCount() { return fsuOnlyCount; }
    public int getLocalOnlyCount() { return localOnlyCount; }
    public int getMismatchCount() { return mismatchCount; }
    public String getErrorMessage() { return errorMessage; }
    public String getSummary() { return summary; }
    public String getResultJson() { return resultJson; }
    public String getRunAt() { return runAt; }
}
