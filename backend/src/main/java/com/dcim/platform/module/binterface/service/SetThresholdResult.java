package com.dcim.platform.module.binterface.service;

import java.util.Collections;
import java.util.List;

/**
 * SET_THRESHOLD 门限设置结果。
 *
 * <p>由 SetThresholdService 返回，包含门限设置操作的执行结果。</p>
 */
public class SetThresholdResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final int count;
    private final List<String> errors;

    private SetThresholdResult(boolean success, String resultCode, String resultDesc,
                               String fsuCode, int count, List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.fsuCode = fsuCode;
        this.count = count;
        this.errors = errors;
    }

    public static SetThresholdResult success(String fsuCode, int count) {
        return new SetThresholdResult(true, "0", "设置成功",
                fsuCode, count, List.of());
    }

    public static SetThresholdResult fail(String resultCode, String desc) {
        return new SetThresholdResult(false, resultCode, desc, null, 0, List.of(desc));
    }

    public static SetThresholdResult fail(String resultCode, String desc, String fsuCode) {
        return new SetThresholdResult(false, resultCode, desc, fsuCode, 0, List.of(desc));
    }

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getFsuCode() { return fsuCode; }
    public int getCount() { return count; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "SetThresholdResult{success=" + success + ", resultCode=" + resultCode
                + ", fsuCode=" + fsuCode + ", count=" + count + "}";
    }
}
