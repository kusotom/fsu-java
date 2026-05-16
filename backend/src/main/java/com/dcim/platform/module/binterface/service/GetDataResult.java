package com.dcim.platform.module.binterface.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GET_DATA 命令查询结果。
 *
 * <p>由 GetDataService 返回，包含从 FSU 查询到的实时信号数据列表。</p>
 *
 * <p>与 SEND_DATA 的 {@link SendDataResult} 区别：</p>
 * <ul>
 *   <li>GET_DATA 是 SC 主动查询 FSU 的结果，只读</li>
 *   <li>SEND_DATA 是 FSU 主动上报到 SC 的结果，含持久化统计</li>
 * </ul>
 */
public class GetDataResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final int count;
    private final List<SignalValue> signals;
    private final List<String> errors;

    private GetDataResult(boolean success, String resultCode, String resultDesc,
                          String fsuCode, int count, List<SignalValue> signals,
                          List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.fsuCode = fsuCode;
        this.count = count;
        this.signals = signals;
        this.errors = errors;
    }

    // ==================== 工厂方法 ====================

    public static GetDataResult success(String fsuCode, List<SignalValue> signals) {
        return new GetDataResult(true, "0", "查询成功",
                fsuCode, signals.size(), signals, List.of());
    }

    public static GetDataResult fail(String resultCode, String desc) {
        return new GetDataResult(false, resultCode, desc, null, 0, List.of(), List.of(desc));
    }

    public static GetDataResult fail(String resultCode, String desc, String fsuCode) {
        return new GetDataResult(false, resultCode, desc, fsuCode, 0, List.of(), List.of(desc));
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getFsuCode() { return fsuCode; }
    public int getCount() { return count; }
    public List<SignalValue> getSignals() { return signals; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "GetDataResult{success=" + success + ", resultCode=" + resultCode
                + ", fsuCode=" + fsuCode + ", count=" + count + "}";
    }

    // ==================== 内部模型 ====================

    /**
     * 单个信号值。
     */
    public static class SignalValue {
        private final String signalId;
        private final String value;
        private final String quality;
        private final String status;
        private final String collectTime;

        public SignalValue(String signalId, String value, String quality,
                           String status, String collectTime) {
            this.signalId = signalId;
            this.value = value;
            this.quality = quality;
            this.status = status;
            this.collectTime = collectTime;
        }

        public String getSignalId() { return signalId; }
        public String getValue() { return value; }
        public String getQuality() { return quality; }
        public String getStatus() { return status; }
        public String getCollectTime() { return collectTime; }
    }
}
