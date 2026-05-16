package com.dcim.platform.module.binterface.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FSU 活动告警 vs 本地 alarm_record 差异核对结果 (BIF-P4-018)。
 *
 * <p>只读审计，不修改本地告警状态。</p>
 */
public class ActiveAlarmDiffResult {

    private final boolean success;
    private final String suid;
    private final int fsuCount;
    private final int localCount;
    private final int matchedCount;
    private final int fsuOnlyCount;
    private final int localOnlyCount;
    private final int mismatchCount;
    private final List<DiffItem> items;
    private final List<String> warnings;
    private final List<String> errors;

    private ActiveAlarmDiffResult(boolean success, String suid, int fsuCount, int localCount,
                                   int matched, int fsuOnly, int localOnly, int mismatch,
                                   List<DiffItem> items, List<String> warnings, List<String> errors) {
        this.success = success; this.suid = suid;
        this.fsuCount = fsuCount; this.localCount = localCount;
        this.matchedCount = matched; this.fsuOnlyCount = fsuOnly;
        this.localOnlyCount = localOnly; this.mismatchCount = mismatch;
        this.items = items != null ? List.copyOf(items) : List.of();
        this.warnings = warnings != null ? List.copyOf(warnings) : List.of();
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public static ActiveAlarmDiffResult empty(String suid) {
        return new ActiveAlarmDiffResult(true, suid, 0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of());
    }

    public static ActiveAlarmDiffResult of(String suid, int fsuCount, int localCount,
                                            int matched, int fsuOnly, int localOnly, int mismatch,
                                            List<DiffItem> items, List<String> warnings) {
        return new ActiveAlarmDiffResult(true, suid, fsuCount, localCount, matched, fsuOnly, localOnly, mismatch, items, warnings, List.of());
    }

    public static ActiveAlarmDiffResult fail(String suid, String error) {
        return new ActiveAlarmDiffResult(false, suid, 0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of(error));
    }

    public boolean isSuccess() { return success; }
    public String getSuid() { return suid; }
    public int getFsuCount() { return fsuCount; }
    public int getLocalCount() { return localCount; }
    public int getMatchedCount() { return matchedCount; }
    public int getFsuOnlyCount() { return fsuOnlyCount; }
    public int getLocalOnlyCount() { return localOnlyCount; }
    public int getMismatchCount() { return mismatchCount; }
    public List<DiffItem> getItems() { return items; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getErrors() { return errors; }
    public boolean hasAlarmInconsistency() { return fsuOnlyCount > 0 || localOnlyCount > 0 || mismatchCount > 0; }

    @Override
    public String toString() {
        return "AlarmDiff{suid=" + suid + " fsu=" + fsuCount + " local=" + localCount
                + " matched=" + matchedCount + " fsuOnly=" + fsuOnlyCount
                + " localOnly=" + localOnlyCount + " mismatch=" + mismatchCount + "}";
    }

    /** 单条差异记录。 */
    public enum DiffType { MATCHED, FSU_ONLY, LOCAL_ONLY, FIELD_MISMATCH, INVALID }

    public static class DiffItem {
        private final DiffType type;
        private final String serialNo;
        private final String deviceId;
        private final String spid;
        private final String alarmLevel;
        private final String alarmFlag;
        private final String fsuSummary;
        private final String localSummary;
        private final List<String> mismatchFields;
        private final String suggestion;

        public DiffItem(DiffType type, String serialNo, String deviceId, String spid,
                         String alarmLevel, String alarmFlag, String fsuSummary, String localSummary,
                         List<String> mismatchFields, String suggestion) {
            this.type = type; this.serialNo = serialNo; this.deviceId = deviceId; this.spid = spid;
            this.alarmLevel = alarmLevel; this.alarmFlag = alarmFlag;
            this.fsuSummary = fsuSummary; this.localSummary = localSummary;
            this.mismatchFields = mismatchFields != null ? List.copyOf(mismatchFields) : List.of();
            this.suggestion = suggestion;
        }

        public DiffType getType() { return type; }
        public String getSerialNo() { return serialNo; }
        public String getDeviceId() { return deviceId; }
        public String getSpid() { return spid; }
        public String getAlarmLevel() { return alarmLevel; }
        public String getAlarmFlag() { return alarmFlag; }
        public String getFsuSummary() { return fsuSummary; }
        public String getLocalSummary() { return localSummary; }
        public List<String> getMismatchFields() { return mismatchFields; }
        public String getSuggestion() { return suggestion; }
    }
}
