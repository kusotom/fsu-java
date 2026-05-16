package com.dcim.platform.module.binterface.service;

import java.util.ArrayList;
import java.util.List;

/**
 * SEND_ALARM 告警上报处理结果。
 *
 * 由 SendAlarmService 返回，供 SendAlarmCommandHandler 构造 CommandResult。
 */
public class SendAlarmResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final int acceptedCount;
    private final int recoveredCount;
    private final int rejectedCount;
    private final List<String> errors;
    private final List<Long> alarmIds;

    private SendAlarmResult(boolean success, String resultCode, String resultDesc,
                            String fsuCode, int acceptedCount, int recoveredCount,
                            int rejectedCount, List<String> errors, List<Long> alarmIds) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.fsuCode = fsuCode;
        this.acceptedCount = acceptedCount;
        this.recoveredCount = recoveredCount;
        this.rejectedCount = rejectedCount;
        this.errors = errors;
        this.alarmIds = alarmIds;
    }

    // ==================== 工厂方法 ====================

    public static SendAlarmResult success(String fsuCode, int accepted, int recovered,
                                          int rejected, List<Long> alarmIds) {
        return new SendAlarmResult(true, "0", "告警上报成功",
                fsuCode, accepted, recovered, rejected, List.of(), alarmIds);
    }

    public static SendAlarmResult fail(String resultCode, String desc) {
        return new SendAlarmResult(false, resultCode, desc,
                null, 0, 0, 0, List.of(desc), List.of());
    }

    public static SendAlarmResult fail(String resultCode, String desc, String fsuCode) {
        return new SendAlarmResult(false, resultCode, desc,
                fsuCode, 0, 0, 0, List.of(desc), List.of());
    }

    public static SendAlarmResult partial(String fsuCode, int accepted, int recovered,
                                          int rejected, List<String> errors, List<Long> alarmIds) {
        return new SendAlarmResult(true, "0", "告警上报完成（部分失败）",
                fsuCode, accepted, recovered, rejected, errors, alarmIds);
    }

    // ==================== Getter ====================

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getFsuCode() { return fsuCode; }
    public int getAcceptedCount() { return acceptedCount; }
    public int getRecoveredCount() { return recoveredCount; }
    public int getRejectedCount() { return rejectedCount; }
    public List<String> getErrors() { return errors; }
    public List<Long> getAlarmIds() { return alarmIds; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "SendAlarmResult{success=" + success + ", resultCode=" + resultCode
                + ", fsuCode=" + fsuCode + ", accepted=" + acceptedCount
                + ", recovered=" + recoveredCount + ", rejected=" + rejectedCount + "}";
    }
}
