package com.dcim.platform.module.binterface.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GET_ACTIVEALARM 活动告警查询结果 (2024 标准, Code=603)。
 *
 * <p>BIF-P4-016: SC 主动查询 FSU 当前活动告警快照。只读查询。</p>
 */
public class GetActiveAlarmResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String suid;
    private final List<ActiveAlarmItem> activeAlarms;
    private final int totalCount;
    private final int parsedCount;
    private final int invalidCount;
    private final List<String> errors;

    private GetActiveAlarmResult(boolean success, String resultCode, String resultDesc,
                                  String suid, List<ActiveAlarmItem> activeAlarms,
                                  int totalCount, int parsedCount, int invalidCount,
                                  List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.suid = suid;
        this.activeAlarms = activeAlarms != null ? List.copyOf(activeAlarms) : List.of();
        this.totalCount = totalCount;
        this.parsedCount = parsedCount;
        this.invalidCount = invalidCount;
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public static GetActiveAlarmResult success(String suid, List<ActiveAlarmItem> alarms,
                                                int total, int parsed, int invalid) {
        return new GetActiveAlarmResult(true, "0", "OK", suid, alarms, total, parsed, invalid, List.of());
    }

    public static GetActiveAlarmResult fail(String resultCode, String desc) {
        return new GetActiveAlarmResult(false, resultCode, desc, null, List.of(), 0, 0, 0, List.of(desc));
    }

    public static GetActiveAlarmResult fail(String resultCode, String desc, String suid) {
        return new GetActiveAlarmResult(false, resultCode, desc, suid, List.of(), 0, 0, 0, List.of(desc));
    }

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getSuid() { return suid; }
    public List<ActiveAlarmItem> getActiveAlarms() { return activeAlarms; }
    public int getTotalCount() { return totalCount; }
    public int getParsedCount() { return parsedCount; }
    public int getInvalidCount() { return invalidCount; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "GetActiveAlarmResult{success=" + success + ", suid=" + suid
                + ", total=" + totalCount + ", parsed=" + parsedCount + "}";
    }

    /** 单个活动告警条目。 */
    public static class ActiveAlarmItem {
        private final String serialNo;
        private final String suid;
        private final String deviceId;
        private final String spid;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final String triggerVal;
        private final String alarmLevel;
        private final String alarmFlag;
        private final String alarmDesc;
        private final String alarmFriDesc;

        public ActiveAlarmItem(String serialNo, String suid, String deviceId, String spid,
                                LocalDateTime startTime, LocalDateTime endTime, String triggerVal,
                                String alarmLevel, String alarmFlag, String alarmDesc, String alarmFriDesc) {
            this.serialNo = serialNo; this.suid = suid; this.deviceId = deviceId; this.spid = spid;
            this.startTime = startTime; this.endTime = endTime; this.triggerVal = triggerVal;
            this.alarmLevel = alarmLevel; this.alarmFlag = alarmFlag;
            this.alarmDesc = alarmDesc; this.alarmFriDesc = alarmFriDesc;
        }

        public String getSerialNo() { return serialNo; }
        public String getSuid() { return suid; }
        public String getDeviceId() { return deviceId; }
        public String getSpid() { return spid; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getTriggerVal() { return triggerVal; }
        public String getAlarmLevel() { return alarmLevel; }
        public String getAlarmFlag() { return alarmFlag; }
        public String getAlarmDesc() { return alarmDesc; }
        public String getAlarmFriDesc() { return alarmFriDesc; }
    }
}
