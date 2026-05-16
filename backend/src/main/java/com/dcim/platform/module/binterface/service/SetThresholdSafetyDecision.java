package com.dcim.platform.module.binterface.service;

import java.util.ArrayList;
import java.util.List;

/**
 * SET_THRESHOLD 安全门禁决策。
 *
 * <p>由 {@link SetThresholdSafetyGate} 返回，判断是否允许执行 SET_THRESHOLD 操作。</p>
 */
public class SetThresholdSafetyDecision {

    private final boolean allowed;
    private final String resultCode;
    private final String resultDesc;
    private final List<String> reasons;

    private SetThresholdSafetyDecision(boolean allowed, String resultCode,
                                       String resultDesc, List<String> reasons) {
        this.allowed = allowed;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.reasons = reasons;
    }

    public static SetThresholdSafetyDecision allow() {
        return new SetThresholdSafetyDecision(true, "0", "安全门禁通过", List.of());
    }

    public static SetThresholdSafetyDecision deny(String resultCode, String reason) {
        List<String> reasons = new ArrayList<>();
        reasons.add(reason);
        return new SetThresholdSafetyDecision(false, resultCode, reason, reasons);
    }

    public static SetThresholdSafetyDecision deny(String resultCode, String reason, List<String> extraReasons) {
        List<String> reasons = new ArrayList<>();
        reasons.add(reason);
        if (extraReasons != null) reasons.addAll(extraReasons);
        return new SetThresholdSafetyDecision(false, resultCode, reason, reasons);
    }

    public boolean isAllowed() { return allowed; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public List<String> getReasons() { return reasons; }

    @Override
    public String toString() {
        return "SetThresholdSafetyDecision{allowed=" + allowed
                + ", resultCode=" + resultCode + "}";
    }
}
