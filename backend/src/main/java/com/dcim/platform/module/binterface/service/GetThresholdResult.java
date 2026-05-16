package com.dcim.platform.module.binterface.service;

import java.util.Collections;
import java.util.List;

/**
 * GET_THRESHOLD 门限查询结果。
 *
 * <p>由 GetThresholdService 返回，包含从 FSU 查询到的门限数据列表。</p>
 *
 * <p>与 SET_THRESHOLD 边界：GET_THRESHOLD 是只读查询，不修改 FSU 门限。</p>
 */
public class GetThresholdResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final int count;
    private final List<ThresholdValue> thresholds;
    private final List<String> errors;

    private GetThresholdResult(boolean success, String resultCode, String resultDesc,
                               String fsuCode, int count, List<ThresholdValue> thresholds,
                               List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.fsuCode = fsuCode;
        this.count = count;
        this.thresholds = thresholds;
        this.errors = errors;
    }

    // ==================== 工厂方法 ====================

    public static GetThresholdResult success(String fsuCode, List<ThresholdValue> thresholds) {
        return new GetThresholdResult(true, "0", "查询成功",
                fsuCode, thresholds.size(), thresholds, List.of());
    }

    public static GetThresholdResult fail(String resultCode, String desc) {
        return new GetThresholdResult(false, resultCode, desc, null, 0, List.of(), List.of(desc));
    }

    public static GetThresholdResult fail(String resultCode, String desc, String fsuCode) {
        return new GetThresholdResult(false, resultCode, desc, fsuCode, 0, List.of(), List.of(desc));
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getFsuCode() { return fsuCode; }
    public int getCount() { return count; }
    public List<ThresholdValue> getThresholds() { return thresholds; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "GetThresholdResult{success=" + success + ", resultCode=" + resultCode
                + ", fsuCode=" + fsuCode + ", count=" + count + "}";
    }

    // ==================== 内部模型 ====================

    /**
     * 单个信号的门限值。
     *
     * <p>字段对应 B接口协议 2016 GET_THRESHOLD 响应中的 Signal 结构。</p>
     */
    public static class ThresholdValue {
        private final String signalId;
        private final String alarmUpper;
        private final String alarmLower;
        private final String alarmUpperUrgent;
        private final String alarmLowerUrgent;

        public ThresholdValue(String signalId, String alarmUpper, String alarmLower,
                              String alarmUpperUrgent, String alarmLowerUrgent) {
            this.signalId = signalId;
            this.alarmUpper = alarmUpper;
            this.alarmLower = alarmLower;
            this.alarmUpperUrgent = alarmUpperUrgent;
            this.alarmLowerUrgent = alarmLowerUrgent;
        }

        public String getSignalId() { return signalId; }
        public String getAlarmUpper() { return alarmUpper; }
        public String getAlarmLower() { return alarmLower; }
        public String getAlarmUpperUrgent() { return alarmUpperUrgent; }
        public String getAlarmLowerUrgent() { return alarmLowerUrgent; }
    }
}
