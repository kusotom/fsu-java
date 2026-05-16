package com.dcim.platform.module.binterface.service;

import java.util.List;

/**
 * GET_ACTIVEALARM + diff 编排审计结果 (BIF-P4-020)。
 *
 * <p>只读审计，不修改本地告警状态。</p>
 */
public class ActiveAlarmConsistencyAuditResult {

    private final boolean success;
    private final String suid;
    private final boolean fsuQueryExecuted;
    private final boolean realDeviceAccessed;
    private final String queryResultCode;
    private final int fsuCount;
    private final int localCount;
    private final int matchedCount;
    private final int fsuOnlyCount;
    private final int localOnlyCount;
    private final int mismatchCount;
    private final ActiveAlarmDiffResult diffResult;
    private final List<String> warnings;
    private final List<String> errors;

    private ActiveAlarmConsistencyAuditResult(boolean success, String suid,
                                               boolean fsuQueryExecuted, boolean realDeviceAccessed,
                                               String queryResultCode, int fsuCount, int localCount,
                                               int matched, int fsuOnly, int localOnly, int mismatch,
                                               ActiveAlarmDiffResult diffResult,
                                               List<String> warnings, List<String> errors) {
        this.success = success; this.suid = suid;
        this.fsuQueryExecuted = fsuQueryExecuted; this.realDeviceAccessed = realDeviceAccessed;
        this.queryResultCode = queryResultCode;
        this.fsuCount = fsuCount; this.localCount = localCount;
        this.matchedCount = matched; this.fsuOnlyCount = fsuOnly;
        this.localOnlyCount = localOnly; this.mismatchCount = mismatch;
        this.diffResult = diffResult;
        this.warnings = warnings != null ? List.copyOf(warnings) : List.of();
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public static ActiveAlarmConsistencyAuditResult fromDiff(ActiveAlarmDiffResult diff,
                                                               boolean fsuQueryExecuted,
                                                               boolean realDeviceAccessed,
                                                               String queryResultCode) {
        return new ActiveAlarmConsistencyAuditResult(
                diff.isSuccess(), diff.getSuid(), fsuQueryExecuted, realDeviceAccessed,
                queryResultCode, diff.getFsuCount(), diff.getLocalCount(),
                diff.getMatchedCount(), diff.getFsuOnlyCount(),
                diff.getLocalOnlyCount(), diff.getMismatchCount(),
                diff, diff.getWarnings(), diff.getErrors());
    }

    public static ActiveAlarmConsistencyAuditResult fsuQueryFailed(String suid,
                                                                     String resultCode, String error) {
        return new ActiveAlarmConsistencyAuditResult(false, suid, true, false, resultCode,
                0, 0, 0, 0, 0, 0, null, List.of(), List.of(error));
    }

    public static ActiveAlarmConsistencyAuditResult invalid(String error) {
        return new ActiveAlarmConsistencyAuditResult(false, null, false, false, null,
                0, 0, 0, 0, 0, 0, null, List.of(), List.of(error));
    }

    public boolean isSuccess() { return success; }
    public String getSuid() { return suid; }
    public boolean isFsuQueryExecuted() { return fsuQueryExecuted; }
    public boolean isRealDeviceAccessed() { return realDeviceAccessed; }
    public String getQueryResultCode() { return queryResultCode; }
    public int getFsuCount() { return fsuCount; }
    public int getLocalCount() { return localCount; }
    public int getMatchedCount() { return matchedCount; }
    public int getFsuOnlyCount() { return fsuOnlyCount; }
    public int getLocalOnlyCount() { return localOnlyCount; }
    public int getMismatchCount() { return mismatchCount; }
    public ActiveAlarmDiffResult getDiffResult() { return diffResult; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getErrors() { return errors; }
    public boolean hasInconsistency() { return fsuOnlyCount > 0 || localOnlyCount > 0 || mismatchCount > 0; }

    @Override public String toString() {
        return "ConsistencyAudit{suid=" + suid + " fsu=" + fsuCount + " local=" + localCount
                + " matched=" + matchedCount + " fsuOnly=" + fsuOnlyCount
                + " localOnly=" + localOnlyCount + " mismatch=" + mismatchCount + "}";
    }
}
