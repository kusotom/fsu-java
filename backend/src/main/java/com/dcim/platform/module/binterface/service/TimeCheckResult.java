package com.dcim.platform.module.binterface.service;

import java.util.List;

/**
 * TIME_CHECK 时间同步校验结果。
 *
 * <p>由 TimeCheckService 返回，包含 SC 向 FSU 查询时间同步的结果。
 * StandardTime 为 SC 端参考时间，FSUTime 为 FSU 端返回时间。</p>
 */
public class TimeCheckResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final String fsuTime;
    private final String standardTime;
    private final List<String> errors;

    private TimeCheckResult(boolean success, String resultCode, String resultDesc,
                            String fsuCode, String fsuTime, String standardTime,
                            List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.fsuCode = fsuCode;
        this.fsuTime = fsuTime;
        this.standardTime = standardTime;
        this.errors = errors;
    }

    // ==================== 工厂方法 ====================

    public static TimeCheckResult success(String fsuCode, String fsuTime, String standardTime) {
        return new TimeCheckResult(true, "0", "时间同步成功",
                fsuCode, fsuTime, standardTime, List.of());
    }

    public static TimeCheckResult fail(String resultCode, String desc) {
        return new TimeCheckResult(false, resultCode, desc, null, null, null, List.of(desc));
    }

    public static TimeCheckResult fail(String resultCode, String desc, String fsuCode) {
        return new TimeCheckResult(false, resultCode, desc, fsuCode, null, null, List.of(desc));
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getFsuCode() { return fsuCode; }
    public String getFsuTime() { return fsuTime; }
    public String getStandardTime() { return standardTime; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "TimeCheckResult{success=" + success + ", resultCode=" + resultCode
                + ", fsuCode=" + fsuCode + ", fsuTime=" + fsuTime + "}";
    }
}
